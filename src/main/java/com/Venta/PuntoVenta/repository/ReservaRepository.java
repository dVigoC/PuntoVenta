package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.OffsetDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // ── Búsqueda paginada por nombre cliente y/o estado ──────────────────────
    @Query("""
        SELECT r FROM Reserva r
        JOIN FETCH r.mesa m
        JOIN FETCH m.area
        WHERE (:busqueda IS NULL OR :busqueda = ''
               OR LOWER(r.nombreCliente) LIKE LOWER(CONCAT('%', :busqueda, '%')))
          AND (:estado   IS NULL OR :estado   = '' OR r.estado = :estado)
        ORDER BY r.fechaReserva DESC
        """)
    Page<Reserva> buscar(
            @Param("busqueda") String busqueda,
            @Param("estado")   String estado,
            Pageable pageable);

    // ── Verificar nombre de cliente único (ignorar la reserva actual al editar)
    boolean existsByNombreClienteIgnoreCaseAndIdNot(String nombreCliente, Long id);

    boolean existsByNombreClienteIgnoreCase(String nombreCliente);

    // ── Reservas activas de una mesa ──────────────────────────────────────────
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.mesa.id = :mesaId
          AND r.estado IN ('PENDIENTE', 'CONFIRMADA', 'ACTIVA')
        """)
    List<Reserva> findActivasByMesaId(@Param("mesaId") Long mesaId);

    // JPQL-compatible: buscamos las que ya deberían ser NO_SHOW
    @Query("""
        SELECT r FROM Reserva r
        WHERE r.estado IN ('PENDIENTE', 'CONFIRMADA')
          AND r.fechaReserva < :limite
        """)
    List<Reserva> findCandidatasNoShow(@Param("limite") OffsetDateTime limite);
}