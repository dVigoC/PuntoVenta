package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // ── Búsqueda paginada con JOIN FETCH para evitar LazyInitializationException ──
    @Query(value = """
            SELECT p FROM Pedido p
            JOIN FETCH p.mesa m
            JOIN FETCH m.area
            WHERE (:busqueda IS NULL OR :busqueda = ''
                   OR LOWER(p.numeroPedido) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(m.numero)       LIKE LOWER(CONCAT('%', :busqueda, '%')))
              AND (:estado IS NULL OR :estado = '' OR p.estado = :estado)
            """,
            countQuery = """
            SELECT COUNT(p) FROM Pedido p
            JOIN p.mesa m
            WHERE (:busqueda IS NULL OR :busqueda = ''
                   OR LOWER(p.numeroPedido) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(m.numero)       LIKE LOWER(CONCAT('%', :busqueda, '%')))
              AND (:estado IS NULL OR :estado = '' OR p.estado = :estado)
            """)
    Page<Pedido> buscar(
            @Param("busqueda") String busqueda,
            @Param("estado")   String estado,
            Pageable pageable);

    // ── Pedido activo de una mesa (solo puede haber UNO gracias al índice parcial) ──
    @Query("""
            SELECT p FROM Pedido p
            JOIN FETCH p.mesa m
            JOIN FETCH m.area
            WHERE p.mesa.id = :mesaId
              AND p.estado NOT IN ('COBRADO', 'ANULADO')
            """)
    Optional<Pedido> findActivoByMesaId(@Param("mesaId") Long mesaId);

    // ── Conteos para stats ────────────────────────────────────────────────────
    long countByEstado(String estado);

    long countByEstadoIn(List<String> estados);

    // ── Pedido completo con detalles (para la pantalla de detalle / edición) ──
    @Query("""
            SELECT DISTINCT p FROM Pedido p
            JOIN FETCH p.mesa m
            JOIN FETCH m.area
            LEFT JOIN FETCH p.detalles d
            LEFT JOIN FETCH d.producto pr
            LEFT JOIN FETCH pr.categoria
            WHERE p.id = :id
            """)
    Optional<Pedido> findByIdConDetalles(@Param("id") Long id);

    //vista dashboard
    // Contar pedidos que NO estén anulados y que pertenezcan al día de hoy (puedes ajustar los estados según desees)
    // Según tu modelo, los estados son: ABIERTO | EN_COCINA | SERVIDO | COBRADO | ANULADO
    // Si quieres contar TODOS los pedidos activos realizados hoy (excepto anulados):
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado IN ('ABIERTO', 'EN_COCINA', 'SERVIDO', 'COBRADO') AND p.creadoEn BETWEEN :inicio AND :fin")
    long contarPedidosRealizadosHoy(@Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin);

    // Sumar el total de las ventas (Pedidos en estado 'COBRADO') creados hoy
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.estado = 'COBRADO' AND p.creadoEn BETWEEN :inicio AND :fin")
    BigDecimal sumarTotalVentasHoy(@Param("inicio") OffsetDateTime inicio, @Param("fin") OffsetDateTime fin);

    //vista cocina

    // Agrega este método al PedidoRepository.java existente

        @Query("""
                SELECT DISTINCT p FROM Pedido p
                JOIN FETCH p.mesa m
                JOIN FETCH m.area
                LEFT JOIN FETCH p.detalles d
                LEFT JOIN FETCH d.producto pr
                LEFT JOIN FETCH pr.categoria
                WHERE p.estado = 'EN_COCINA'
                ORDER BY p.creadoEn ASC
                """)
        List<Pedido> findEnCocinaConDetalles();

        /**
         * Pedidos que tienen al menos un ítem activo EN_PREP o LISTO.
         * Incluye pedidos en cualquier estado (EN_COCINA, SERVIDO, etc.)
         * para capturar ítems nuevos agregados a pedidos ya servidos.
         */
        @Query("""
                SELECT DISTINCT p FROM Pedido p
                JOIN FETCH p.mesa m
                JOIN FETCH m.area
                LEFT JOIN FETCH p.detalles d
                LEFT JOIN FETCH d.producto pr
                LEFT JOIN FETCH pr.categoria
                WHERE EXISTS (
                SELECT 1 FROM DetallePedido dp
                WHERE dp.pedido = p
                AND dp.estadoItem IN ('EN_PREP', 'LISTO')
                )
                ORDER BY p.creadoEn ASC
                """)
        List<Pedido> findConItemsActivosEnCocina();

        //vista dashboard

        // ── Pedidos activos con mesa y área (para el dashboard) ──────────────────
        @Query("""
                SELECT p FROM Pedido p
                JOIN FETCH p.mesa m
                JOIN FETCH m.area a
                WHERE p.estado NOT IN ('COBRADO', 'ANULADO')
                ORDER BY a.nombre ASC, m.numero ASC
                """)
        List<Pedido> findActivosConMesaYArea();

        @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado = 'EN_COCINA'")
        long contarPedidosEnCocina();
}