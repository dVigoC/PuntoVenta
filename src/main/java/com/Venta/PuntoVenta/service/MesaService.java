package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Area;
import com.Venta.PuntoVenta.model.Mesa;
import com.Venta.PuntoVenta.repository.AreaRepository;
import com.Venta.PuntoVenta.repository.MesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository  mesaRepository;
    private final AreaRepository   areaRepository;

    // ── Búsqueda paginada ─────────────────────────────────────────────────────
    public Page<Mesa> buscar(String busqueda, Long areaId, String estado, Pageable pageable) {
        return mesaRepository.buscar(
                busqueda != null ? busqueda.trim() : "",
                areaId,
                (estado != null && !estado.isBlank()) ? estado : null,
                pageable);
    }

    // ── Conteos para stats ────────────────────────────────────────────────────
    public long contarTotal() {
        return mesaRepository.countByActivoTrue();
    }

    public long contarPorEstado(String estado) {
        return mesaRepository.countByEstadoAndActivoTrue(estado);
    }

    // ── Crear ─────────────────────────────────────────────────────────────────
    @Transactional
    public Mesa crear(String numero, Long areaId, String estado, boolean activo) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada"));

        if (mesaRepository.existsByAreaIdAndNumero(areaId, numero)) {
            throw new IllegalArgumentException(
                    "Ya existe una mesa con el número " + numero + " en el área «" + area.getNombre() + "».");
        }

        Mesa mesa = new Mesa();
        mesa.setNumero(numero);
        mesa.setArea(area);
        mesa.setEstado(estado != null ? estado : "LIBRE");
        mesa.setActivo(activo);
        return mesaRepository.save(mesa);
    }

    // ── Editar ────────────────────────────────────────────────────────────────
    @Transactional
    public Mesa editar(Long id, Long areaId, String estado, boolean activo) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));

        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Área no encontrada"));

        mesa.setArea(area);
        mesa.setEstado(estado != null ? estado : "LIBRE");
        mesa.setActivo(activo);
        return mesaRepository.save(mesa);
    }

    // ── Desactivar ────────────────────────────────────────────────────────────
    @Transactional
    public void desactivar(Long id) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada"));
        mesa.setActivo(false);
        mesa.setEstado("INACTIVA");
        mesaRepository.save(mesa);
    }
}