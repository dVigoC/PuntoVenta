package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    // ── Todos los ítems de un pedido (con producto para evitar N+1) ──────────
    @Query("""
            SELECT d FROM DetallePedido d
            JOIN FETCH d.producto p
            JOIN FETCH p.categoria
            WHERE d.pedido.id = :pedidoId
            ORDER BY d.creadoEn ASC
            """)
    List<DetallePedido> findByPedidoIdConProducto(@Param("pedidoId") Long pedidoId);

    // ── Ítems activos (no anulados) de un pedido ─────────────────────────────
    @Query("""
            SELECT d FROM DetallePedido d
            JOIN FETCH d.producto p
            WHERE d.pedido.id = :pedidoId
              AND d.estadoItem <> 'ANULADO'
            ORDER BY d.creadoEn ASC
            """)
    List<DetallePedido> findActivosByPedidoId(@Param("pedidoId") Long pedidoId);

    // ── Anular un ítem ────────────────────────────────────────────────────────
    @Modifying
    @Query("UPDATE DetallePedido d SET d.estadoItem = 'ANULADO' WHERE d.id = :id")
    void anularItem(@Param("id") Long id);
}