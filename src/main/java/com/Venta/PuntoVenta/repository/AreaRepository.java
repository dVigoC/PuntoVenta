package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findByActivoTrueOrderByNombreAsc();
    List<Area> findAllByOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}