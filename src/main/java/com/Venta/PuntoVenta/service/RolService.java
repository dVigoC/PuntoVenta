package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.dto.RolDTO;
import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Rol;
import com.Venta.PuntoVenta.repository.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RolService {

    private final RolRepository rolRepo;

    public List<Rol> listarTodos() {
        return rolRepo.findAll();
    }

    public List<Rol> listarActivos() {
        return rolRepo.findByActivoTrue();
    }

    public Rol obtenerPorId(Long id) {
        return rolRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado: " + id));
    }

    @Transactional
    public Rol crear(RolDTO dto) {
        validarNombre(dto.getNombre(), null);

        Rol rol = new Rol();
        rol.setNombre(dto.getNombre().trim().toUpperCase());
        rol.setDescripcion(StringUtils.hasText(dto.getDescripcion()) ? dto.getDescripcion().trim() : null);
        rol.setActivo(dto.isActivo());
        return rolRepo.save(rol);
    }

    @Transactional
    public Rol actualizar(Long id, RolDTO dto) {
        Rol rol = obtenerPorId(id);
        validarNombre(dto.getNombre(), id);

        rol.setNombre(dto.getNombre().trim().toUpperCase());
        rol.setDescripcion(StringUtils.hasText(dto.getDescripcion()) ? dto.getDescripcion().trim() : null);
        rol.setActivo(dto.isActivo());
        return rolRepo.save(rol);
    }

    @Transactional
    public void desactivar(Long id) {
        Rol rol = obtenerPorId(id);
        rol.setActivo(false);
        rolRepo.save(rol);
    }

    private void validarNombre(String nombre, Long excluirId) {
        rolRepo.findByNombreIgnoreCase(nombre).ifPresent(r -> {
            if (!r.getId().equals(excluirId))
                throw new IllegalArgumentException("El rol «" + nombre.toUpperCase() + "» ya existe");
        });
    }
}