package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.config.ZonaHorariaConfig;
import com.Venta.PuntoVenta.model.Mesa;
import com.Venta.PuntoVenta.model.Reserva;
import com.Venta.PuntoVenta.repository.MesaRepository;
import com.Venta.PuntoVenta.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaService {

    private final ReservaRepository reservaRepo;
    private final MesaRepository    mesaRepo;

    // ── Estados "activos" de reserva que bloquean la mesa ────────────────────
    private static final List<String> ESTADOS_ACTIVOS =
            List.of("PENDIENTE", "CONFIRMADA", "ACTIVA");

    // ── Estados "cerrados" de reserva ────────────────────────────────────────
    private static final List<String> ESTADOS_CERRADOS =
            List.of("COMPLETADA", "CANCELADA", "NO_SHOW");

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    public Page<Reserva> buscar(String busqueda, String estado, Pageable pageable) {
        return reservaRepo.buscar(
                (busqueda == null ? "" : busqueda.trim()),
                (estado   == null ? "" : estado.trim()),
                pageable);
    }

    public Reserva obtener(Long id) {
        return reservaRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada: " + id));
    }

    public long contarPorEstado(String estado) {
        return reservaRepo.buscar("", estado, Pageable.unpaged()).getTotalElements();
    }

    // =========================================================================
    // CREAR
    // =========================================================================

    @Transactional
    public Reserva crear(
            Long           mesaId,
            String         nombreCliente,
            String         telefono,
            OffsetDateTime fechaReserva,
            int            toleranciaMinutos,
            int            personas,
            String         observaciones) {

        validarNombreUnico(nombreCliente, null);

        Mesa mesa = mesaRepo.findById(mesaId)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));

        if ("OCUPADA".equals(mesa.getEstado())) {
            throw new IllegalStateException("La mesa está OCUPADA y no puede reservarse.");
        }
        if ("INACTIVA".equals(mesa.getEstado())) {
            throw new IllegalStateException("La mesa está INACTIVA.");
        }
        if ("RESERVADA".equals(mesa.getEstado())) {
            throw new IllegalStateException("La mesa ya tiene una reserva activa.");
        }

        Reserva r = new Reserva();
        r.setMesa(mesa);
        r.setNombreCliente(nombreCliente.trim());
        r.setTelefonoCliente(telefono);
        r.setFechaReserva(fechaReserva);
        r.setToleranciaMinutos(toleranciaMinutos > 0 ? toleranciaMinutos : 15);
        r.setPersonas(personas > 0 ? personas : 1);
        r.setObservaciones(observaciones);
        r.setEstado("PENDIENTE");

        Reserva guardada = reservaRepo.save(r);

        // Cambiar estado de mesa a RESERVADA
        mesa.setEstado("RESERVADA");
        mesaRepo.save(mesa);

        return guardada;
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    @Transactional
    public Reserva editar(
            Long           id,
            Long           mesaId,
            String         nombreCliente,
            String         telefono,
            OffsetDateTime fechaReserva,
            int            toleranciaMinutos,
            int            personas,
            String         observaciones,
            String         estado) {

        Reserva r = obtener(id);
        validarNombreUnico(nombreCliente, id);

        String estadoAnterior = r.getEstado();
        Long   mesaAnteriorId = r.getMesa().getId();

        // ── Cambio de mesa ────────────────────────────────────────────────────
        if (!mesaAnteriorId.equals(mesaId)) {
            Mesa mesaNueva = mesaRepo.findById(mesaId)
                    .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));

            if ("OCUPADA".equals(mesaNueva.getEstado())) {
                throw new IllegalStateException("La nueva mesa está OCUPADA.");
            }
            if ("RESERVADA".equals(mesaNueva.getEstado())) {
                throw new IllegalStateException("La nueva mesa ya está RESERVADA.");
            }

            // Liberar mesa anterior si ya no tiene reservas activas
            liberarMesaSiCorresponde(mesaAnteriorId, id);

            // Reservar nueva mesa
            mesaNueva.setEstado("RESERVADA");
            mesaRepo.save(mesaNueva);
            r.setMesa(mesaNueva);
        }

        r.setNombreCliente(nombreCliente.trim());
        r.setTelefonoCliente(telefono);
        r.setFechaReserva(fechaReserva);
        r.setToleranciaMinutos(toleranciaMinutos > 0 ? toleranciaMinutos : 15);
        r.setPersonas(personas > 0 ? personas : 1);
        r.setObservaciones(observaciones);

        // ── Cambio de estado ──────────────────────────────────────────────────
        if (estado != null && !estado.equals(estadoAnterior)) {
            r.setEstado(estado);
            aplicarCambioEstadoMesa(r.getMesa().getId(), estadoAnterior, estado, id);
        }

        return reservaRepo.save(r);
    }

    // =========================================================================
    // CAMBIAR ESTADO rápido (desde botones de la tabla)
    // =========================================================================

    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        Reserva r = obtener(id);
        String estadoAnterior = r.getEstado();
        r.setEstado(nuevoEstado);
        reservaRepo.save(r);
        aplicarCambioEstadoMesa(r.getMesa().getId(), estadoAnterior, nuevoEstado, id);
    }

    // =========================================================================
    // ELIMINAR
    // =========================================================================

    @Transactional
    public void eliminar(Long id) {
        Reserva r = obtener(id);

        // Si la reserva aún está "activa" debemos liberar la mesa
        if (ESTADOS_ACTIVOS.contains(r.getEstado())) {
            liberarMesaSiCorresponde(r.getMesa().getId(), id);
        }

        reservaRepo.deleteById(id);
    }

    // =========================================================================
    // SCHEDULER: procesar NO_SHOW cada minuto
    // =========================================================================

    @Scheduled(fixedDelay = 60_000)   // cada 60 segundos
    @Transactional
    public void procesarNoShow() {
        // Buscamos reservas cuya fecha_reserva + tolerancia_minutos ya pasó
        // Usamos un límite conservador: la más pequeña tolerancia es 1 minuto,
        // pero filtramos de forma segura con fecha < ahora - 0 y evaluamos por ítem.
        OffsetDateTime ahora = OffsetDateTime.now(ZonaHorariaConfig.ZONA_PERU);

        // Candidatas: estado PENDIENTE/CONFIRMADA y fecha en el pasado
        List<Reserva> candidatas = reservaRepo.findCandidatasNoShow(ahora);

        for (Reserva r : candidatas) {
            OffsetDateTime limite = r.getFechaReserva()
                .atZoneSameInstant(ZoneId.of("America/Lima"))
                .toOffsetDateTime()
                .plusMinutes(r.getToleranciaMinutos());

            if (ahora.isAfter(limite)) {
                log.info("NO_SHOW procesado: reserva {} cliente '{}'",
                        r.getId(), r.getNombreCliente());
                r.setEstado("NO_SHOW");
                reservaRepo.save(r);
                liberarMesaSiCorresponde(r.getMesa().getId(), r.getId());
            }
        }
    }

    // =========================================================================
    // VALIDACIONES Y HELPERS PRIVADOS
    // =========================================================================

    private void validarNombreUnico(String nombre, Long idActual) {
        boolean duplicado = (idActual == null)
                ? reservaRepo.existsByNombreClienteIgnoreCase(nombre.trim())
                : reservaRepo.existsByNombreClienteIgnoreCaseAndIdNot(nombre.trim(), idActual);

        if (duplicado) {
            throw new IllegalArgumentException(
                    "El nombre «" + nombre + "» ya está registrado en otra reserva. " +
                    "Usa un nombre diferente o un seudónimo.");
        }
    }

    /**
     * Según el cambio de estado de la reserva, actualiza el estado de la mesa.
     */
    private void aplicarCambioEstadoMesa(Long mesaId, String estadoAnterior,
                                          String estadoNuevo, Long reservaId) {
        if (ESTADOS_CERRADOS.contains(estadoNuevo)) {
            liberarMesaSiCorresponde(mesaId, reservaId);
        } else if ("ACTIVA".equals(estadoNuevo)) {
            // Cliente llegó; la mesa permanece RESERVADA hasta que se cree el pedido
            mesaRepo.findById(mesaId).ifPresent(m -> {
                m.setEstado("RESERVADA");
                mesaRepo.save(m);
            });
        }
    }

    /**
     * Libera la mesa (pone LIBRE) solo si ya no tiene otras reservas activas
     * (excluyendo la reserva con id = excludeReservaId).
     */
    private void liberarMesaSiCorresponde(Long mesaId, Long excludeReservaId) {
        boolean hayOtrasActivas = reservaRepo.findActivasByMesaId(mesaId)
                .stream()
                .anyMatch(rv -> !rv.getId().equals(excludeReservaId));

        if (!hayOtrasActivas) {
            mesaRepo.findById(mesaId).ifPresent(m -> {
                // Solo liberamos si no hay un pedido abierto (el estado puede ser OCUPADA)
                if (!"OCUPADA".equals(m.getEstado())) {
                    m.setEstado("LIBRE");
                    mesaRepo.save(m);
                }
            });
        }
    }


    // Agregar estos métodos al ReservaService existente:

    public boolean existeNombre(String nombre) {
        return reservaRepo.existsByNombreClienteIgnoreCase(nombre.trim());
    }

    public boolean existeNombreExcluyendo(String nombre, Long excludeId) {
        return reservaRepo.existsByNombreClienteIgnoreCaseAndIdNot(nombre.trim(), excludeId);
    }
}