package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.dto.DetalleItemDTO;
import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.ComandaCocina;
import com.Venta.PuntoVenta.model.DetalleComandaCocina;
import com.Venta.PuntoVenta.model.DetallePedido;
import com.Venta.PuntoVenta.model.Mesa;
import com.Venta.PuntoVenta.model.Pedido;
import com.Venta.PuntoVenta.model.Producto;
import com.Venta.PuntoVenta.repository.ComandaCocinaRepository;
import com.Venta.PuntoVenta.repository.DetalleComandaCocinaRepository;
import com.Venta.PuntoVenta.repository.DetallePedidoRepository;
import com.Venta.PuntoVenta.repository.MesaRepository;
import com.Venta.PuntoVenta.repository.PedidoRepository;
import com.Venta.PuntoVenta.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository       pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final MesaRepository          mesaRepository;
    private final ProductoRepository      productoRepository;
    private final ComandaCocinaRepository       comandaRepository;
    private final DetalleComandaCocinaRepository detalleComandaRepository;

    private static final BigDecimal IGV_PORCENTAJE = new BigDecimal("18.00");

    // =========================================================================
    // LISTADO / CONSULTAS
    // =========================================================================

    public Page<Pedido> buscar(String busqueda, String estado, Pageable pageable) {
        String bus = (busqueda == null) ? "" : busqueda.trim();
        String est = (estado   == null || estado.isBlank()) ? "" : estado.trim();
        return pedidoRepository.buscar(bus, est, pageable);
    }

    public long contarPorEstado(String estado) {
        return pedidoRepository.countByEstado(estado);
    }

    public long contarActivos() {
        return pedidoRepository.countByEstadoIn(List.of("ABIERTO", "EN_COCINA", "SERVIDO"));
    }

    /**
     * Retorna el pedido completo con sus ítems (JOIN FETCH).
     */
    public Optional<Pedido> findConDetalles(Long id) {
        return pedidoRepository.findByIdConDetalles(id);
    }

    /**
     * Si la mesa ya tiene un pedido activo lo retorna; si no, retorna vacío.
     */
    public Optional<Pedido> pedidoActivoDeMesa(Long mesaId) {
        return pedidoRepository.findActivoByMesaId(mesaId);
    }

    // =========================================================================
    // CREAR PEDIDO
    // =========================================================================

    @Transactional
    public Pedido crear(Long mesaId, List<DetalleItemDTO> items) {

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

        // Validación: la mesa no puede tener ya un pedido activo
        pedidoRepository.findActivoByMesaId(mesaId).ifPresent(p -> {
            throw new IllegalStateException(
                    "La mesa ya tiene el pedido activo «" + p.getNumeroPedido() + "». " +
                    "Usa la opción «Agregar ítems» para añadir más platos.");
        });

        // La BD valida RESERVADA con estado PENDIENTE/CONFIRMADA mediante trigger;
        // aquí solo dejamos pasar LIBRE y RESERVADA con reserva ACTIVA.
        if ("INACTIVA".equals(mesa.getEstado())) {
            throw new IllegalStateException("La mesa está inactiva y no puede recibir pedidos.");
        }

        Pedido pedido = new Pedido();
        pedido.setMesa(mesa);
        pedido.setEstado("ABIERTO");
        // El trigger de BD genera el numero_pedido automáticamente
        // Guardamos primero para obtener el ID
        pedido = pedidoRepository.save(pedido);

         //  Refrescar para que Hibernate lea el numero_pedido generado por el trigger
        pedidoRepository.flush();
        pedido = pedidoRepository.findById(pedido.getId()).orElseThrow();

        // Agregar los ítems iniciales
        for (DetalleItemDTO dto : items) {
            agregarItemAlPedido(pedido, dto);
        }

        recalcularTotales(pedido);
        return pedidoRepository.save(pedido);
    }

    // =========================================================================
    // AGREGAR ÍTEMS A UN PEDIDO EXISTENTE
    // =========================================================================

    @Transactional
    public Pedido agregarItems(Long pedidoId, List<DetalleItemDTO> items) {

        Pedido pedido = pedidoRepository.findByIdConDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if (List.of("COBRADO", "ANULADO").contains(pedido.getEstado())) {
            throw new IllegalStateException("No se pueden agregar ítems a un pedido " + pedido.getEstado());
        }

        for (DetalleItemDTO dto : items) {
            agregarItemAlPedido(pedido, dto);
        }

        // ── FIX: si el pedido estaba SERVIDO, revertir a EN_COCINA ──────────────
        // Los ítems nuevos estarán PENDIENTES hasta que se envíe comanda,
        // pero el pedido debe volver a EN_COCINA para que el flujo sea consistente.
        if ("SERVIDO".equals(pedido.getEstado())) {
            pedido.setEstado("EN_COCINA");
        }

        recalcularTotales(pedido);
        return pedidoRepository.save(pedido);
    }

    // =========================================================================
    // EDITAR CANTIDAD / NOTA DE UN ÍTEM
    // =========================================================================

    @Transactional
    public DetallePedido editarItem(Long itemId, int nuevaCantidad, String notaCocina) {

        DetallePedido item = detalleRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        if ("ANULADO".equals(item.getEstadoItem())) {
            throw new IllegalStateException("No se puede editar un ítem anulado.");
        }
        if (nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0.");
        }

        item.setCantidad(nuevaCantidad);
        item.setNotaCocina(notaCocina);
        item.setSubtotal(item.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(nuevaCantidad))
                .subtract(item.getDescuentoItem()));

        DetallePedido saved = detalleRepository.save(item);

        // Recalcular totales del pedido padre
        Pedido pedido = pedidoRepository.findByIdConDetalles(item.getPedido().getId()).orElseThrow();
        recalcularTotales(pedido);
        pedidoRepository.save(pedido);

        return saved;
    }

    // =========================================================================
    // ANULAR ÍTEM
    // =========================================================================

    @Transactional
    public void anularItem(Long itemId) {
        DetallePedido item = detalleRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        if ("ANULADO".equals(item.getEstadoItem())) return;

        detalleRepository.anularItem(itemId);

        // Recalcular totales
        Pedido pedido = pedidoRepository.findByIdConDetalles(item.getPedido().getId()).orElseThrow();
        recalcularTotales(pedido);
        pedidoRepository.save(pedido);
    }

    // =========================================================================
    // CAMBIAR ESTADO DEL PEDIDO
    // =========================================================================

    @Transactional
    public Pedido cambiarEstado(Long pedidoId, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findByIdConDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
    
        validarTransicionEstado(pedido.getEstado(), nuevoEstado);
    
        // Bloqueo especial: no se puede pasar a SERVIDO si hay ítems activos sin servir
        if ("SERVIDO".equals(nuevoEstado)) {
            List<DetallePedido> sinServir = pedido.getDetalles().stream()
                    .filter(d -> !"ANULADO".equals(d.getEstadoItem()))
                    .filter(d -> !"SERVIDO".equals(d.getEstadoItem()))
                    .toList();
    
            if (!sinServir.isEmpty()) {
                String nombres = sinServir.stream()
                        .map(d -> "«" + d.getProducto().getNombre() + "» (" + d.getEstadoItem() + ")")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                throw new IllegalStateException(
                        "No se puede marcar como SERVIDO. Ítems pendientes: " + nombres);
            }
        }
    
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    // =========================================================================
    // ANULAR PEDIDO COMPLETO
    // =========================================================================

    @Transactional
    public void anular(Long pedidoId) {
        Pedido pedido = pedidoRepository.findByIdConDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));

        if ("COBRADO".equals(pedido.getEstado())) {
            throw new IllegalStateException("No se puede anular un pedido ya cobrado.");
        }

        pedido.setEstado("ANULADO");
        pedido.getDetalles().forEach(d -> d.setEstadoItem("ANULADO"));
        pedidoRepository.save(pedido);
        // El trigger de BD libera la mesa automáticamente
    }

    // =========================================================================
    // COMANDA DE COCINA — CREAR Y REGISTRAR
    // =========================================================================
 
    /**
     * Genera una comanda de cocina con los ítems PENDIENTES del pedido que
     * aún no tienen comanda_ref asignada.
     * Devuelve la ComandaCocina persistida (con su número generado por el trigger).
     */
    @Transactional
    public ComandaCocina generarComandaCocina(Long pedidoId) {
 
        Pedido pedido = pedidoRepository.findByIdConDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
 
        if (List.of("COBRADO", "ANULADO").contains(pedido.getEstado())) {
            throw new IllegalStateException(
                    "No se puede generar comanda para un pedido " + pedido.getEstado());
        }
 
        // Ítems PENDIENTES sin comanda_ref asignada = los "nuevos" de este envío
        List<DetallePedido> itemsNuevos = pedido.getDetalles().stream()
                .filter(d -> "PENDIENTE".equals(d.getEstadoItem())
                          && (d.getComandaRef() == null || d.getComandaRef().isBlank()))
                .toList();
 
        if (itemsNuevos.isEmpty()) {
            throw new IllegalStateException(
                    "No hay ítems pendientes sin enviar a cocina en este pedido.");
        }
 
        long nroEnvio = comandaRepository.countByPedidoId(pedidoId) + 1;
 
        // Creamos la comanda (el trigger de BD generará numero_comanda)
        ComandaCocina comanda = new ComandaCocina();
        comanda.setPedido(pedido);
        comanda.setNroEnvio((int) nroEnvio);
        comanda.setEstado("ENVIADA");
        comanda.setImpresa(false);
        comanda.setImpresaAuto(false);
        comanda = comandaRepository.save(comanda);
 
        // Refrescar para leer el numero_comanda generado por el trigger
        comandaRepository.flush();
        comanda = comandaRepository.findById(comanda.getId()).orElseThrow();
 
        String refComanda = comanda.getNumeroComanda();
 
        // Vincular ítems a esta comanda
        for (DetallePedido item : itemsNuevos) {
            DetalleComandaCocina dc = new DetalleComandaCocina();
            dc.setComanda(comanda);
            dc.setDetallePedido(item);
            dc.setCantidad(item.getCantidad());
            dc.setNotaCocina(item.getNotaCocina());
            comanda.getDetalles().add(dc);
 
            // Marcar el ítem con la referencia de la comanda y cambiar estado
            item.setComandaRef(refComanda);
            item.setEstadoItem("EN_PREP");
            detalleRepository.save(item);
        }
 
        // Cambiar estado del pedido a EN_COCINA si aún está ABIERTO
        if ("ABIERTO".equals(pedido.getEstado())) {
            pedido.setEstado("EN_COCINA");
            pedidoRepository.save(pedido);
        }
 
        return comandaRepository.save(comanda);
    }
 
    /**
     * Retorna la comanda con todos sus detalles para imprimir.
     */
    public Optional<ComandaCocina> findComandaConDetalles(Long comandaId) {
        return comandaRepository.findByIdConDetalles(comandaId);
    }
 
    /**
     * Retorna todas las comandas de un pedido.
     */
    public List<ComandaCocina> findComandasDePedido(Long pedidoId) {
        return comandaRepository.findByPedidoIdConDetalles(pedidoId);
    }
 
    /**
     * Marca la comanda como impresa.
     */
    @Transactional
    public void marcarComandaImpresa(Long comandaId) {
        ComandaCocina comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada"));
        comanda.setImpresa(true);
        comandaRepository.save(comanda);
    }

    // =========================================================================
    // PRIVADOS
    // =========================================================================

    private void agregarItemAlPedido(Pedido pedido, DetalleItemDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + dto.getProductoId()));

        if (!Boolean.TRUE.equals(producto.getDisponible())) {
            throw new IllegalStateException("El producto «" + producto.getNombre() + "» no está disponible.");
        }

        BigDecimal precio    = producto.getPrecio();
        BigDecimal cantidad  = BigDecimal.valueOf(dto.getCantidad());
        BigDecimal subtotal  = precio.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);

        DetallePedido item = new DetallePedido();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(dto.getCantidad());
        item.setPrecioUnitario(precio);
        item.setDescuentoItem(BigDecimal.ZERO);
        item.setSubtotal(subtotal);
        item.setNotaCocina(dto.getNotaCocina());
        item.setEstadoItem("PENDIENTE");

        pedido.getDetalles().add(item);
    }

    private void recalcularTotales(Pedido pedido) {
        BigDecimal subtotal = pedido.getDetalles().stream()
                .filter(d -> !"ANULADO".equals(d.getEstadoItem()))
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal igv   = subtotal.multiply(IGV_PORCENTAJE)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(igv);

        pedido.setSubtotal(subtotal);
        pedido.setIgv(igv);
        pedido.setTotal(total);
    }

    private void validarTransicionEstado(String actual, String nuevo) {
        boolean valido = switch (actual) {
            case "ABIERTO"   -> List.of("EN_COCINA", "ANULADO").contains(nuevo);
            case "EN_COCINA" -> List.of("SERVIDO",   "ANULADO").contains(nuevo);
            case "SERVIDO"   -> List.of("COBRADO", "EN_COCINA",   "ANULADO").contains(nuevo);
            default          -> false;
        };
        if (!valido) {
            throw new IllegalStateException(
                    "No se puede cambiar de «" + actual + "» a «" + nuevo + "».");
        }
    }
}