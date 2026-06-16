package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.DetallePedido;
import com.Venta.PuntoVenta.model.Pedido;
import com.Venta.PuntoVenta.repository.DetallePedidoRepository;
import com.Venta.PuntoVenta.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CocinaService {

    private final PedidoRepository       pedidoRepository;
    private final DetallePedidoRepository detalleRepository;

    // =========================================================================
    // PEDIDOS EN COCINA
    // =========================================================================

    public List<Pedido> listarPedidosEnCocina() {
        // Ahora: busca por ítems activos, no por estado del pedido
        return pedidoRepository.findConItemsActivosEnCocina();
    }

    // =========================================================================
    // CAMBIAR ESTADO DE UN ÍTEM
    // =========================================================================

    @Transactional
    public String cambiarEstadoItem(Long itemId, String nuevoEstado) {

        DetallePedido item = detalleRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        if ("ANULADO".equals(item.getEstadoItem())) {
            throw new IllegalStateException("No se puede cambiar el estado de un ítem anulado.");
        }

        validarTransicion(item.getEstadoItem(), nuevoEstado);

        item.setEstadoItem(nuevoEstado);
        detalleRepository.save(item);

        // Si todos los ítems activos del pedido están SERVIDOS → pedido pasa a SERVIDO
        Pedido pedido = pedidoRepository.findByIdConDetalles(item.getPedido().getId())
                .orElseThrow();

        String extra = "";
        if ("SERVIDO".equals(nuevoEstado)) {
            boolean todoServido = pedido.getDetalles().stream()
                    .filter(d -> !"ANULADO".equals(d.getEstadoItem()))
                    .allMatch(d -> "SERVIDO".equals(d.getEstadoItem()));

            if (todoServido) {
                pedido.setEstado("SERVIDO");
                pedidoRepository.save(pedido);
                extra = " El pedido " + pedido.getNumeroPedido() + " fue marcado como SERVIDO automáticamente.";
            }
        }

        String nombreEstado = switch (nuevoEstado) {
            case "LISTO"   -> "Listo";
            case "SERVIDO" -> "Servido";
            default        -> nuevoEstado;
        };

        return "«" + item.getProducto().getNombre() + "» marcado como " + nombreEstado + "." + extra;
    }

    // =========================================================================
    // PRIVADO
    // =========================================================================

    private void validarTransicion(String actual, String nuevo) {
        boolean valido = switch (actual) {
            case "EN_PREP" -> List.of("LISTO", "SERVIDO").contains(nuevo);
            case "LISTO"   -> "SERVIDO".equals(nuevo);
            default        -> false;
        };
        if (!valido) {
            throw new IllegalStateException(
                    "Transición no permitida: «" + actual + "» → «" + nuevo + "».");
        }
    }
}