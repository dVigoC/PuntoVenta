package com.Venta.PuntoVenta.repository;

import com.Venta.PuntoVenta.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByActivoTrueOrderByOrdenDisplayAscNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    //VISTA PEDIDO
    
}