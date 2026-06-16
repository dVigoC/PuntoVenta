package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.dto.CategoriaDTO;
import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Categoria;
import com.Venta.PuntoVenta.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivoTrueOrderByOrdenDisplayAscNombreAsc();
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    @Transactional
    public Categoria crear(CategoriaDTO dto) {
        if (categoriaRepository.existsByNombreIgnoreCase(dto.getNombre().trim())) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        Categoria cat = new Categoria();
        mapearDesdeDTO(cat, dto);
        return categoriaRepository.save(cat);
    }

    @Transactional
    public Categoria editar(Long id, CategoriaDTO dto) {
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(dto.getNombre().trim(), id)) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }
        mapearDesdeDTO(cat, dto);
        return categoriaRepository.save(cat);
    }

    @Transactional
    public void desactivar(Long id) {
        Categoria cat = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        cat.setActivo(false);
        categoriaRepository.save(cat);
    }

    private void mapearDesdeDTO(Categoria cat, CategoriaDTO dto) {
        cat.setNombre(dto.getNombre().trim().toUpperCase());
        cat.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        cat.setOrdenDisplay(dto.getOrdenDisplay() != null ? dto.getOrdenDisplay() : 0);
        cat.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
    }
}