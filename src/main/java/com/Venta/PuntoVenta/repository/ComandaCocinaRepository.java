package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.ComandaCocina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;


public interface ComandaCocinaRepository extends JpaRepository<ComandaCocina, Long> {

    /** Todas las comandas de un pedido, ordenadas por número de envío */
    @Query("""
           SELECT c FROM ComandaCocina c
           LEFT JOIN FETCH c.detalles d
           LEFT JOIN FETCH d.detallePedido dp
           LEFT JOIN FETCH dp.producto
           WHERE c.pedido.id = :pedidoId
           ORDER BY c.nroEnvio ASC
           """)
    List<ComandaCocina> findByPedidoIdConDetalles(@Param("pedidoId") Long pedidoId);

    /** Número de envíos ya realizados para un pedido (para calcular nroEnvio) */
    long countByPedidoId(Long pedidoId);

    /** Buscar comanda con sus detalles por ID */
    @Query("""
           SELECT c FROM ComandaCocina c
           LEFT JOIN FETCH c.detalles d
           LEFT JOIN FETCH d.detallePedido dp
           LEFT JOIN FETCH dp.producto
           LEFT JOIN FETCH c.pedido p
           LEFT JOIN FETCH p.mesa m
           LEFT JOIN FETCH m.area
           WHERE c.id = :id
           """)
    Optional<ComandaCocina> findByIdConDetalles(@Param("id") Long id);
}