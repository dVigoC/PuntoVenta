package com.Venta.PuntoVenta.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.Venta.PuntoVenta.model.Rol;


public interface RolRepository extends JpaRepository<Rol, Long> {
    
    List<Rol> findByActivoTrue();
 
    // Necesario para validar nombres duplicados en RolService
    Optional<Rol> findByNombreIgnoreCase(String nombre);
}
 
