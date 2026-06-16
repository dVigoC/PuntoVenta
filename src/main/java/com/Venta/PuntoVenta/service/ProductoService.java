package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.dto.ProductoDTO;
import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Categoria;
import com.Venta.PuntoVenta.model.Producto;
import com.Venta.PuntoVenta.repository.CategoriaRepository;
import com.Venta.PuntoVenta.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository  productoRepository;
    private final CategoriaRepository categoriaRepository;

    public Page<Producto> listar(String busqueda, Long categoriaId, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        // DESPUÉS — nunca pasar null, pasar string vacío y ajustar la query
        String bus = (busqueda == null || busqueda.isBlank()) ? "" : busqueda.trim();
        return productoRepository.buscar(bus, categoriaId, pageable);
    }

    public long contarTotal()       { return productoRepository.countByActivoTrue(); }
    public long contarDisponibles() { return productoRepository.countByDisponibleTrueAndActivoTrue(); }
    public long contarNoDisp()      { return productoRepository.countByDisponibleFalse(); }

    @Transactional
    public Producto crear(ProductoDTO dto) {
        validarCodigo(dto.getCodigo(), null);
        Categoria cat = obtenerCategoria(dto.getCategoriaId());
        Producto p = new Producto();
        mapearDesdeDTO(p, dto, cat);
        return productoRepository.save(p);
    }

    @Transactional
    public Producto editar(Long id, ProductoDTO dto) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        validarCodigo(dto.getCodigo(), id);
        Categoria cat = obtenerCategoria(dto.getCategoriaId());
        mapearDesdeDTO(p, dto, cat);
        return productoRepository.save(p);
    }

    @Transactional
    public void desactivar(Long id) {

        productoRepository.deleteById(id);

    }

    // ── privados ──────────────────────────────────────────────────────────────

    private Categoria obtenerCategoria(Long catId) {
        return categoriaRepository.findById(catId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }

    private void validarCodigo(String codigo, Long idActual) {
        if (codigo == null || codigo.isBlank()) return;
        boolean existe = (idActual == null)
                ? productoRepository.existsByCodigo(codigo.trim())
                : productoRepository.existsByCodigoAndIdNot(codigo.trim(), idActual);
        if (existe) throw new IllegalArgumentException("Ya existe un producto con ese código");
    }

    private void mapearDesdeDTO(Producto p, ProductoDTO dto, Categoria cat) {
        p.setCategoria(cat);
        p.setCodigo(dto.getCodigo() != null && !dto.getCodigo().isBlank()
                ? dto.getCodigo().trim().toUpperCase() : null);
        p.setNombre(dto.getNombre().trim());
        p.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        p.setPrecio(dto.getPrecio());
        p.setDisponible(dto.getDisponible() != null ? dto.getDisponible() : true);
        p.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
    }



    // ── Métodos de validación pública para la API MODULO PRODUCTOS─────────────────────────

    /**
     * Verifica si un código ya existe para cualquier producto activo.
     * Útil al momento de CREAR un nuevo producto.
     */
    public boolean codigoExiste(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        return productoRepository.existsByCodigo(codigo.trim());
    }

    /**
     * Verifica si un código ya existe en otro producto diferente al que se está editando.
     * Útil al momento de EDITAR para evitar auto-bloquearse con su propio código.
     */
    public boolean existeCodigoExcluyendoId(String codigo, Long id) {
        if (codigo == null || codigo.isBlank() || id == null) {
            return false;
        }
        return productoRepository.existsByCodigoAndIdNot(codigo.trim(), id);
    }
}