package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.dto.DetalleItemDTO;
import com.Venta.PuntoVenta.model.ComandaCocina;
import com.Venta.PuntoVenta.model.Pedido;
import com.Venta.PuntoVenta.repository.CategoriaRepository;
import com.Venta.PuntoVenta.repository.EmpresaRepository;
import com.Venta.PuntoVenta.repository.MesaRepository;
import com.Venta.PuntoVenta.repository.ProductoRepository;
import com.Venta.PuntoVenta.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService      pedidoService;
    private final MesaRepository     mesaRepo;
    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
     private final EmpresaRepository   empresaRepo;

    private static final int PAGE_SIZE = 10;

    // =========================================================================
    // LISTADO PRINCIPAL
    // =========================================================================

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "")  String busqueda,
            @RequestParam(defaultValue = "")  String estado,
            @RequestParam(defaultValue = "0") int    pagina,
            Model model) {

        PageRequest pageable = PageRequest.of(
                pagina, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "creadoEn"));

        Page<Pedido> page = pedidoService.buscar(busqueda, estado, pageable);

        // Stats
        model.addAttribute("totalActivos",  pedidoService.contarActivos());
        model.addAttribute("abiertos",      pedidoService.contarPorEstado("ABIERTO"));
        model.addAttribute("enCocina",      pedidoService.contarPorEstado("EN_COCINA"));
        model.addAttribute("servidos",      pedidoService.contarPorEstado("SERVIDO"));
        model.addAttribute("cobrados",      pedidoService.contarPorEstado("COBRADO"));
        model.addAttribute("anulados",      pedidoService.contarPorEstado("ANULADO"));

        // Mesas disponibles para crear pedido: LIBRE + RESERVADA (con reserva ACTIVA)
        model.addAttribute("mesasDisponibles",
                mesaRepo.findMesasDisponiblesParaPedido());

        // Productos activos y disponibles agrupados por categoría (para el modal de items)
        model.addAttribute("categorias",   categoriaRepo.findByActivoTrueOrderByOrdenDisplayAscNombreAsc());
        model.addAttribute("productos",    productoRepo.findByActivoTrueAndDisponibleTrueOrderByNombreAsc());

        // Tabla
        model.addAttribute("pedidos",   page.getContent());
        model.addAttribute("page",      page);
        model.addAttribute("busqueda",  busqueda);
        model.addAttribute("estado",    estado);

        return "pedidos/lista";
    }

    // =========================================================================
    // DETALLE (vista completa con ítems)
    // =========================================================================

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model,
                          RedirectAttributes ra) {

        return pedidoService.findConDetalles(id).map(pedido -> {
            model.addAttribute("pedido", pedido);
            // Productos disponibles para agregar más ítems
            model.addAttribute("categorias",  categoriaRepo.findByActivoTrueOrderByOrdenDisplayAscNombreAsc());
            model.addAttribute("productos",   productoRepo.findByActivoTrueAndDisponibleTrueOrderByNombreAsc());
            return "pedidos/detalle";
        }).orElseGet(() -> {
            ra.addFlashAttribute("flashMensaje", "Pedido no encontrado.");
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/pedidos";
        });
    }

    // =========================================================================
    // CREAR PEDIDO
    // =========================================================================

    @PostMapping("/crear")
    public String crear(
            @RequestParam Long    mesaId,
            @RequestParam List<Long>    productoIds,
            @RequestParam List<Integer> cantidades,
            @RequestParam(required = false) List<String> notas,
            RedirectAttributes ra) {

        try {
            List<DetalleItemDTO> items = buildItems(productoIds, cantidades, notas);
            if (items.isEmpty()) throw new IllegalArgumentException("Debe agregar al menos un ítem.");
            Pedido p = pedidoService.crear(mesaId, items);
            ra.addFlashAttribute("flashMensaje", "Pedido «" + p.getNumeroPedido() + "» creado correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
            return "redirect:/pedidos/" + p.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/pedidos";
        }
    }

    // =========================================================================
    // AGREGAR ÍTEMS A PEDIDO EXISTENTE
    // =========================================================================

    @PostMapping("/{id}/agregar-items")
    public String agregarItems(
            @PathVariable Long id,
            @RequestParam List<Long>    productoIds,
            @RequestParam List<Integer> cantidades,
            @RequestParam(required = false) List<String> notas,
            RedirectAttributes ra) {

        try {
            List<DetalleItemDTO> items = buildItems(productoIds, cantidades, notas);
            if (items.isEmpty()) throw new IllegalArgumentException("Debe seleccionar al menos un producto.");
            pedidoService.agregarItems(id, items);
            ra.addFlashAttribute("flashMensaje", "Ítems agregados correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/pedidos/" + id;
    }

    // =========================================================================
    // EDITAR ÍTEM (cantidad + nota)
    // =========================================================================

    @PostMapping("/items/{itemId}/editar")
    public String editarItem(
            @PathVariable Long    itemId,
            @RequestParam int     cantidad,
            @RequestParam(required = false) String notaCocina,
            @RequestParam Long    pedidoId,
            RedirectAttributes ra) {

        try {
            pedidoService.editarItem(itemId, cantidad, notaCocina);
            ra.addFlashAttribute("flashMensaje", "Ítem actualizado.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/pedidos/" + pedidoId;
    }

    // =========================================================================
    // ANULAR ÍTEM
    // =========================================================================

    @PostMapping("/items/{itemId}/anular")
    public String anularItem(
            @PathVariable Long itemId,
            @RequestParam Long pedidoId,
            RedirectAttributes ra) {

        try {
            pedidoService.anularItem(itemId);
            ra.addFlashAttribute("flashMensaje", "Ítem anulado correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/pedidos/" + pedidoId;
    }

    // =========================================================================
    // CAMBIAR ESTADO DEL PEDIDO
    // =========================================================================

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long   id,
            @RequestParam String nuevoEstado,
            RedirectAttributes ra) {

        try {
            pedidoService.cambiarEstado(id, nuevoEstado);
            ra.addFlashAttribute("flashMensaje", "Estado cambiado a «" + nuevoEstado + "».");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/pedidos/" + id;
    }

    // =========================================================================
    // ANULAR PEDIDO
    // =========================================================================

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.anular(id);
            ra.addFlashAttribute("flashMensaje", "Pedido anulado correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/pedidos";
    }

    // =========================================================================
    // COMANDA DE COCINA — GENERAR + VISTA DE IMPRESIÓN
    // =========================================================================
 
    /**
     * POST /pedidos/{id}/comanda-cocina
     * Genera la comanda con los ítems pendientes y redirige a la vista de impresión.
     */
    @PostMapping("/{id}/comanda-cocina")
    public String generarComandaCocina(@PathVariable Long id, RedirectAttributes ra) {
        try {
            ComandaCocina comanda = pedidoService.generarComandaCocina(id);
            // Redirigir directo a la vista de impresión de la comanda
            return "redirect:/pedidos/comanda/" + comanda.getId() + "/imprimir";
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/pedidos/" + id;
        }
    }
 
    /**
     * GET /pedidos/comanda/{comandaId}/imprimir
     * Vista de impresión de la comanda de cocina.
     */
    @GetMapping("/comanda/{comandaId}/imprimir")
    public String imprimirComandaCocina(@PathVariable Long comandaId, Model model,
                                        RedirectAttributes ra) {
        return pedidoService.findComandaConDetalles(comandaId).map(comanda -> {
            model.addAttribute("comanda",  comanda);
            model.addAttribute("pedido",   comanda.getPedido());
            model.addAttribute("empresa",  empresaRepo.findFirstByActivoTrue().orElse(null));
            return "pedidos/comanda-cocina-print";
        }).orElseGet(() -> {
            ra.addFlashAttribute("flashMensaje", "Comanda no encontrada.");
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/pedidos";
        });
    }
 
    /**
     * POST /pedidos/comanda/{comandaId}/marcar-impresa
     * Marca la comanda como impresa (llamado por AJAX desde la vista de impresión).
     */
    @PostMapping("/comanda/{comandaId}/marcar-impresa")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> marcarComandaImpresa(
            @PathVariable Long comandaId) {
        try {
            pedidoService.marcarComandaImpresa(comandaId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", e.getMessage()));
        }
    }
 
    // =========================================================================
    // TICKET DE COBRO — VISTA DE IMPRESIÓN
    // =========================================================================
 
    /**
     * GET /pedidos/{id}/ticket
     * Vista de impresión del ticket de venta (para el cliente).
     * Disponible desde que el pedido está en SERVIDO o COBRADO.
     */
    @GetMapping("/{id}/ticket")
    public String imprimirTicket(@PathVariable Long id, Model model, RedirectAttributes ra) {
        return pedidoService.findConDetalles(id).map(pedido -> {
            if ("ANULADO".equals(pedido.getEstado())) {
                ra.addFlashAttribute("flashMensaje", "No se puede imprimir ticket de un pedido anulado.");
                ra.addFlashAttribute("flashTipo", "error");
                return "redirect:/pedidos/" + id;
            }
            model.addAttribute("pedido",  pedido);
            model.addAttribute("empresa", empresaRepo.findFirstByActivoTrue().orElse(null));
            return "pedidos/ticket-cobro-print";
        }).orElseGet(() -> {
            ra.addFlashAttribute("flashMensaje", "Pedido no encontrado.");
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/pedidos";
        });
    }

    // =========================================================================
    // API: precio de un producto (AJAX)
    // =========================================================================

    @GetMapping("/api/producto/{id}/precio")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPrecio(@PathVariable Long id) {
        return productoRepo.findById(id)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "precio",  p.getPrecio(),
                        "nombre",  p.getNombre(),
                        "activo",  p.getActivo(),
                        "disponible", p.getDisponible()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================================================================
    // API: pedido activo de una mesa (AJAX — para verificar antes de crear)
    // =========================================================================

    @GetMapping("/api/mesa/{mesaId}/pedido-activo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pedidoActivoDeMesa(@PathVariable Long mesaId) {
        return pedidoService.pedidoActivoDeMesa(mesaId)
                .map(p -> ResponseEntity.ok(Map.<String, Object>of(
                        "existe",        true,
                        "pedidoId",      p.getId(),
                        "numeroPedido",  p.getNumeroPedido(),
                        "estado",        p.getEstado()
                )))
                .orElse(ResponseEntity.ok(Map.of("existe", false)));
    }

    // =========================================================================
    // PRIVADOS
    // =========================================================================

    private List<DetalleItemDTO> buildItems(List<Long> productoIds,
                                            List<Integer> cantidades,
                                            List<String> notas) {
        List<DetalleItemDTO> items = new ArrayList<>();
        for (int i = 0; i < productoIds.size(); i++) {
            Long pid = productoIds.get(i);
            if (pid == null || pid == 0) continue;
            int cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : 1;
            if (cant <= 0) continue;
            String nota = (notas != null && i < notas.size()) ? notas.get(i) : null;

            DetalleItemDTO dto = new DetalleItemDTO();
            dto.setProductoId(pid);
            dto.setCantidad(cant);
            dto.setNotaCocina(nota);
            items.add(dto);
        }
        return items;
    }
}