package com.Venta.PuntoVenta.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.Venta.PuntoVenta.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    // CORRECCIÓN: agregado para que validarEmail() funcione correctamente
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByActivoTrue();

    long countByActivoFalse();  // ← agrega esta línea
    long countByBloqueadoTrue();

    /**
     * CORRECCIÓN FINAL: native query con cast explícito a ::text para Supabase/PgBouncer.
     *
     * El problema era que Hibernate enviaba los parámetros como bytea en lugar de text
     * al usar JPQL con LOWER+CONCAT en PostgreSQL vía Supabase (PgBouncer transaction mode).
     * PostgreSQL no tiene lower(bytea), solo lower(text), por eso fallaba.
     *
     * Solución: volver a native query pero con cast explícito ::text en cada columna
     * y CAST(:busqueda AS TEXT) en el parámetro, forzando el tipo correcto.
     *
     * El mapeo de Rol funciona porque FetchType.EAGER en la entidad hace que
     * Hibernate emita un segundo SELECT por el rol tras cargar usuarios, lo cual
     * es correcto y seguro con paginación pequeña (PAGE_SIZE=10).
     */
    @Query(value = """
        SELECT u.*
        FROM usuarios u
        JOIN roles r ON r.id = u.rol_id
        WHERE (CAST(:busqueda AS TEXT) IS NULL
               OR LOWER(u.username::text)        LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%')
               OR LOWER(u.nombre_completo::text)  LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%'))
        ORDER BY u.creado_en DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM usuarios u
        WHERE (CAST(:busqueda AS TEXT) IS NULL
               OR LOWER(u.username::text)        LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%')
               OR LOWER(u.nombre_completo::text)  LIKE LOWER('%' || CAST(:busqueda AS TEXT) || '%'))
        """,
        nativeQuery = true)
    Page<Usuario> buscar(@Param("busqueda") String busqueda, Pageable pageable);
}