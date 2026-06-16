package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Producto;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query(
      value = """
      SELECT p FROM Producto p
      LEFT JOIN FETCH p.categoria c
      WHERE (:busqueda IS NULL OR LENGTH(:busqueda) = 0 
            OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
        AND (:categoriaId IS NULL OR c.id = :categoriaId)
      ORDER BY c.ordenDisplay ASC, c.nombre ASC, p.nombre ASC
      """,
      countQuery = """
      SELECT COUNT(p) FROM Producto p
      LEFT JOIN p.categoria c
      WHERE (:busqueda IS NULL OR LENGTH(:busqueda) = 0 
            OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(c.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
        AND (:categoriaId IS NULL OR c.id = :categoriaId)
      """
  )
  Page<Producto> buscar(@Param("busqueda")   String busqueda,
                        @Param("categoriaId") Long categoriaId,
                        Pageable pageable);

    long countByActivoTrue();
    long countByDisponibleTrueAndActivoTrue();
    long countByDisponibleFalse();

    boolean existsByCodigoAndIdNot(String codigo, Long id);
    boolean existsByCodigo(String codigo);

    //VISTA PEDIDO

    // ── Para el selector de productos en pedidos (solo activos y disponibles) ──
    @Query("""
           SELECT p FROM Producto p
           JOIN FETCH p.categoria c
           WHERE p.activo = true AND p.disponible = true
           ORDER BY c.ordenDisplay ASC, c.nombre ASC, p.nombre ASC
           """)
    List<Producto> findByActivoTrueAndDisponibleTrueOrderByNombreAsc();
}