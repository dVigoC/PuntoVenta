package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Area;
import com.Venta.PuntoVenta.repository.AreaRepository;
import com.Venta.PuntoVenta.repository.MesaRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;
    private final MesaRepository  mesaRepository;

    public List<Area> listarActivas() {
        return areaRepository.findByActivoTrueOrderByNombreAsc();
    }

    public List<Area> listarTodas() {
        List<Area> areas = areaRepository.findAllByOrderByNombreAsc();
        areas.forEach(a -> a.setCantidadMesas(mesaRepository.countByAreaId(a.getId())));
        return areas;
    }

    @Transactional
    public Area crear(String nombre, String descripcion, boolean activo) {
        if (areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new IllegalArgumentException("Ya existe un área con el nombre «" + nombre + "».");
        }
        Area area = new Area();
        area.setNombre(nombre);
        area.setDescripcion(descripcion);
        area.setActivo(activo);
        return areaRepository.save(area);
    }

    @Transactional
    public Area editar(Long id, String nombre, String descripcion, boolean activo) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada"));

        // Verificar unicidad excluyendo la actual
        if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new IllegalArgumentException("Ya existe un área con el nombre «" + nombre + "».");
        }

        area.setNombre(nombre);
        area.setDescripcion(descripcion);
        area.setActivo(activo);
        return areaRepository.save(area);
    }

    @Transactional
    public void desactivar(Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada"));
        area.setActivo(false);
        areaRepository.save(area);
    }
}