package com.Venta.PuntoVenta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;


import com.Venta.PuntoVenta.model.Empresa;


public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findFirstByActivoTrue();
}