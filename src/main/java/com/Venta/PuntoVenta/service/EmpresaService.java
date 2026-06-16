package com.Venta.PuntoVenta.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Venta.PuntoVenta.model.Empresa;
import com.Venta.PuntoVenta.repository.EmpresaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpresaService {

    private final EmpresaRepository empresaRepository;

    public Optional<Empresa> obtenerEmpresaActiva() {
        return empresaRepository.findFirstByActivoTrue();
    }

    public boolean existeEmpresaActiva() {
        return empresaRepository.findFirstByActivoTrue().isPresent();
    }

    @Transactional
    public Empresa crear(Empresa empresa) {
        empresa.setActivo(true);
        return empresaRepository.save(empresa);
    }

    @Transactional
    public Empresa editar(Long id, Empresa datos) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        // Campos editables
        empresa.setNombre(datos.getNombre());
        empresa.setDireccion(datos.getDireccion());
        empresa.setTelefono(datos.getTelefono());
        empresa.setEmail(datos.getEmail());
        empresa.setLogoUrl(datos.getLogoUrl());
        empresa.setMoneda(datos.getMoneda());
        empresa.setPorcentajeIgv(datos.getPorcentajeIgv());
        empresa.setPieFactura(datos.getPieFactura());
        empresa.setDesarrollador(datos.getDesarrollador());

        // RUC NO se edita — campo bloqueado

        return empresaRepository.save(empresa);
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa empresa = empresaRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
        empresa.setActivo(false);
        empresaRepository.save(empresa);
    }
}