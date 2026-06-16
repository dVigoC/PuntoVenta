package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Empleado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    // ── Búsqueda paginada con filtros ─────────────────────────────────────────
    @Query("""
        SELECT e FROM Empleado e
        WHERE e.activo = true
          AND (
            :busqueda IS NULL OR :busqueda = ''
            OR LOWER(e.nombres)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(e.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(e.dni)       LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(e.cargo)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
          )
          AND (:cargo IS NULL OR :cargo = '' OR LOWER(e.cargo) = LOWER(:cargo))
        ORDER BY e.apellidos ASC, e.nombres ASC
        """)
    Page<Empleado> buscarActivos(
            @Param("busqueda") String busqueda,
            @Param("cargo")    String cargo,
            Pageable pageable);

            
    // ── Todos (activos e inactivos) — para estadísticas ───────────────────────
    long countByActivoTrue();
    long countByActivoFalse();

    // ── Verificaciones de unicidad ────────────────────────────────────────────
    boolean existsByDni(String dni);
    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    // ── Búsqueda por DNI ──────────────────────────────────────────────────────
    Optional<Empleado> findByDni(String dni);

    // ── Lista de cargos distintos (para filtro) ───────────────────────────────
    @Query("SELECT DISTINCT e.cargo FROM Empleado e WHERE e.activo = true ORDER BY e.cargo ASC")
    java.util.List<String> findCargosActivos();

    // ── Todos (activos e inactivos) con filtros ───────────────────────────────
@Query("""
    SELECT e FROM Empleado e
    WHERE (
        :busqueda IS NULL OR :busqueda = ''
        OR LOWER(e.nombres)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.dni)       LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.cargo)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
    )
    AND (:cargo IS NULL OR :cargo = '' OR LOWER(e.cargo) = LOWER(:cargo))
    ORDER BY e.apellidos ASC, e.nombres ASC
    """)
Page<Empleado> buscarTodos(
        @Param("busqueda") String busqueda,
        @Param("cargo")    String cargo,
        Pageable pageable);

// ── Por estado (activo=true o false) con filtros ──────────────────────────
@Query("""
    SELECT e FROM Empleado e
    WHERE e.activo = :activo
      AND (
        :busqueda IS NULL OR :busqueda = ''
        OR LOWER(e.nombres)   LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.dni)       LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.cargo)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
        OR LOWER(e.email)     LIKE LOWER(CONCAT('%', :busqueda, '%'))
      )
      AND (:cargo IS NULL OR :cargo = '' OR LOWER(e.cargo) = LOWER(:cargo))
    ORDER BY e.apellidos ASC, e.nombres ASC
    """)
Page<Empleado> buscarPorEstado(
        @Param("busqueda") String busqueda,
        @Param("cargo")    String cargo,
        @Param("activo")   boolean activo,
        Pageable pageable);
}