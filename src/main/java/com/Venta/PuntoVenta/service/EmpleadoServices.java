package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.model.Empleado;
import com.Venta.PuntoVenta.repository.EmpleadoRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpleadoServices {

    private final EmpleadoRepository repo;

    // ── Listado paginado ──────────────────────────────────────────────────────
    public Page<Empleado> listar(String busqueda, String cargo, String estado, int pagina, int tamano) {
        Pageable pageable = PageRequest.of(pagina, tamano);
        String b = (busqueda != null && !busqueda.isBlank()) ? busqueda.trim() : null;
        String c = (cargo    != null && !cargo.isBlank())    ? cargo.trim()    : null;

        return switch (estado) {
            case "activos"   -> repo.buscarActivos(b, c, pageable);
            case "inactivos" -> repo.buscarPorEstado(b, c, false, pageable);
            default          -> repo.buscarTodos(b, c, pageable);
        };
    }

    // ── Cargos para filtro (todos, no solo activos) ───────────────────────────
    public List<String> cargos() {
        return repo.findCargosActivos();   // puedes dejarlo igual o crear findCargos() sin filtro
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    public Map<String, Long> stats() {
        long activos   = repo.countByActivoTrue();
        long inactivos = repo.countByActivoFalse();
        return Map.of(
            "activos",   activos,
            "inactivos", inactivos,
            "total",     activos + inactivos
        );
    }

    

    // ── Buscar por ID ─────────────────────────────────────────────────────────
    public Empleado buscarPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado: " + id));
    }

    // ── Verificaciones de unicidad ────────────────────────────────────────────
    public boolean existeDni(String dni) {
        return repo.existsByDni(dni.trim().toUpperCase());
    }

    public boolean existeDniParaOtro(String dni, Long id) {
        return repo.existsByDniAndIdNot(dni.trim().toUpperCase(), id);
    }

    public boolean existeEmail(String email) {
        if (email == null || email.isBlank()) return false;
        return repo.existsByEmail(email.trim().toLowerCase());
    }

    public boolean existeEmailParaOtro(String email, Long id) {
        if (email == null || email.isBlank()) return false;
        return repo.existsByEmailAndIdNot(email.trim().toLowerCase(), id);
    }

    // ── Crear ─────────────────────────────────────────────────────────────────
    @Transactional
    public Empleado crear(Empleado empleado) {
        normalizarCampos(empleado);
        validarNuevo(empleado);
        return repo.save(empleado);
    }

    // ── Actualizar ────────────────────────────────────────────────────────────
    @Transactional
    public Empleado actualizar(Long id, Empleado datos) {
        Empleado existente = buscarPorId(id);
        normalizarCampos(datos);
        validarActualizacion(datos, id);

        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setDni(datos.getDni());
        existente.setTelefono(datos.getTelefono());
        existente.setDireccion(datos.getDireccion());
        existente.setEmail(datos.getEmail());
        existente.setCargo(datos.getCargo());
        existente.setFechaIngreso(datos.getFechaIngreso());
        existente.setFechaCese(datos.getFechaCese());
        existente.setActivo(datos.getActivo());

        return repo.save(existente);
    }

    // ── Desactivar (soft delete) ──────────────────────────────────────────────
    @Transactional
    public void desactivar(Long id) {
        Empleado e = buscarPorId(id);
        e.setActivo(false);
        e.setFechaCese(LocalDate.now());
        repo.save(e);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────
    private void normalizarCampos(Empleado e) {
        if (e.getNombres()   != null) e.setNombres(capitalize(e.getNombres().trim()));
        if (e.getApellidos() != null) e.setApellidos(capitalize(e.getApellidos().trim()));
        if (e.getDni()       != null) e.setDni(e.getDni().trim().toUpperCase());
        if (e.getCargo()     != null) e.setCargo(e.getCargo().trim().toUpperCase());
        if (e.getEmail()     != null && !e.getEmail().isBlank())
            e.setEmail(e.getEmail().trim().toLowerCase());
        else
            e.setEmail(null);
        if (e.getTelefono()  != null && e.getTelefono().isBlank()) e.setTelefono(null);
        if (e.getDireccion() != null && e.getDireccion().isBlank()) e.setDireccion(null);
        if (e.getFechaIngreso() == null) e.setFechaIngreso(LocalDate.now());
    }

    private void validarNuevo(Empleado e) {
        if (repo.existsByDni(e.getDni()))
            throw new IllegalArgumentException("El DNI " + e.getDni() + " ya está registrado.");
        if (e.getEmail() != null && repo.existsByEmail(e.getEmail()))
            throw new IllegalArgumentException("El correo " + e.getEmail() + " ya está registrado.");
    }

    private void validarActualizacion(Empleado e, Long id) {
        if (repo.existsByDniAndIdNot(e.getDni(), id))
            throw new IllegalArgumentException("El DNI " + e.getDni() + " ya está registrado.");
        if (e.getEmail() != null && repo.existsByEmailAndIdNot(e.getEmail(), id))
            throw new IllegalArgumentException("El correo " + e.getEmail() + " ya está registrado.");
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        String[] words = s.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)))
                                .append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}