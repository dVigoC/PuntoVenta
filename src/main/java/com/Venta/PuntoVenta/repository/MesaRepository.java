package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Mesa;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    // JOIN FETCH a: carga el área en la misma consulta, evitando
    // LazyInitializationException cuando Thymeleaf accede a m.area.nombre
    @Query(value = """
                SELECT m FROM Mesa m
                JOIN FETCH m.area a
                WHERE (:busqueda = ''
                       OR LOWER(CAST(m.numero AS string)) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                       OR LOWER(a.nombre)                 LIKE LOWER(CONCAT('%', :busqueda, '%')))
                  AND (:areaId IS NULL OR a.id     = :areaId)
                  AND (:estado IS NULL OR m.estado = :estado)
            """,
            countQuery = """
                SELECT COUNT(m) FROM Mesa m
                JOIN m.area a
                WHERE (:busqueda = ''
                       OR LOWER(CAST(m.numero AS string)) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                       OR LOWER(a.nombre)                 LIKE LOWER(CONCAT('%', :busqueda, '%')))
                  AND (:areaId IS NULL OR a.id     = :areaId)
                  AND (:estado IS NULL OR m.estado = :estado)
            """)
    Page<Mesa> buscar(
            @Param("busqueda") String  busqueda,
            @Param("areaId")   Long    areaId,
            @Param("estado")   String  estado,
            Pageable pageable);

    // Conteos para las stats
    long countByActivoTrue();
    long countByEstadoAndActivoTrue(String estado);

    // Unicidad por área + número
    boolean existsByAreaIdAndNumero(Long areaId, String numero);

    // Para enriquecer cantidadMesas en AreaService
    long countByAreaId(Long areaId);

    //reserva
    // Agregar estos métodos al MesaRepository existente:

    /** Mesas en los estados dados y activas (para el selector de reservas) */
    List<Mesa> findByEstadoInAndActivoTrue(List<String> estados);

    /** Todas las mesas activas ordenadas (para edición) */
    @Query("SELECT m FROM Mesa m JOIN FETCH m.area WHERE m.activo = true ORDER BY m.area.nombre ASC, m.numero ASC")
    List<Mesa> findByActivoTrueOrderByAreaNombreAscNumeroAsc();

    //Vista pedidos
    @Query("""
        SELECT DISTINCT m FROM Mesa m
        JOIN FETCH m.area
        WHERE m.activo = true
          AND (
              m.estado = 'LIBRE'
              OR (
                  m.estado = 'RESERVADA'
                  AND EXISTS (
                      SELECT r FROM Reserva r
                      WHERE r.mesa = m
                        AND r.estado = 'ACTIVA'
                  )
              )
          )
        ORDER BY m.area.nombre ASC, m.numero ASC
        """)
    List<Mesa> findMesasDisponiblesParaPedido();
}