package com.Venta.PuntoVenta.service;

import com.Venta.PuntoVenta.dto.UsuarioDTO;
import com.Venta.PuntoVenta.exception.ResourceNotFoundException;
import com.Venta.PuntoVenta.model.Rol;
import com.Venta.PuntoVenta.model.Usuario;
import com.Venta.PuntoVenta.repository.RolRepository;
import com.Venta.PuntoVenta.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository     rolRepo;
    private final PasswordEncoder   passwordEncoder;

    public Page<Usuario> listar(String busqueda, int pagina, int tam) {
        String term = StringUtils.hasText(busqueda) ? busqueda.trim() : null;
        return usuarioRepo.buscar(term, PageRequest.of(pagina, tam));
    }

    public long contarActivos()    { return usuarioRepo.countByActivoTrue(); }
    public long contarInactivos() { return usuarioRepo.countByActivoFalse(); }

    public Usuario obtenerPorId(Long id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + id));
    }

    @Transactional
    public Usuario crear(UsuarioDTO dto) {
        validarUsername(dto.getUsername(), null);
        validarEmail(dto.getEmail(), null);

        if (!StringUtils.hasText(dto.getPassword()))
            throw new IllegalArgumentException("La contraseña es obligatoria al crear un usuario");

        // Validación de longitud mínima de contraseña (antes estaba solo en el DTO con @Size
        // pero sin min en edición; aquí lo garantizamos en creación)
        if (dto.getPassword().length() < 6)
            throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres");

        Rol rol = rolRepo.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        Usuario u = new Usuario();
        u.setUsername(dto.getUsername().trim());
        u.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        u.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        u.setNombreCompleto(dto.getNombreCompleto().trim());
        u.setRol(rol);
        u.setActivo(dto.isActivo());
        return usuarioRepo.save(u);
    }

    @Transactional
    public Usuario actualizar(Long id, UsuarioDTO dto) {
        Usuario u = obtenerPorId(id);
        validarUsername(dto.getUsername(), id);
        validarEmail(dto.getEmail(), id);

        Rol rol = rolRepo.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        u.setUsername(dto.getUsername().trim());
        u.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
        u.setNombreCompleto(dto.getNombreCompleto().trim());
        u.setRol(rol);
        u.setActivo(dto.isActivo());

        // Solo actualizar contraseña si viene con contenido y longitud válida
        if (StringUtils.hasText(dto.getPassword())) {
            if (dto.getPassword().length() < 6)
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres");
            u.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        return usuarioRepo.save(u);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario u = obtenerPorId(id);
        u.setActivo(false);
        usuarioRepo.save(u);
    }

    @Transactional
    public void desbloquear(Long id) {
        Usuario u = obtenerPorId(id);
        u.setBloqueado(false);
        u.setIntentosFallidos(0);
        usuarioRepo.save(u);
    }

    public List<Rol> listarRolesActivos() { return rolRepo.findByActivoTrue(); }

    // ── Validaciones ─────────────────────────────────────────────────────────

    private void validarUsername(String username, Long excluirId) {
        usuarioRepo.findByUsernameIgnoreCase(username).ifPresent(u -> {
            if (!u.getId().equals(excluirId))
                throw new IllegalArgumentException("El username «" + username + "» ya está en uso");
        });
    }

    /**
     * CORRECCIÓN: el original usaba findByUsernameIgnoreCase(email) en vez de
     * buscar por email, así nunca detectaba duplicados de email al editar.
     */
    private void validarEmail(String email, Long excluirId) {
        if (!StringUtils.hasText(email)) return;
        usuarioRepo.findByEmailIgnoreCase(email).ifPresent(u -> {
            if (!u.getId().equals(excluirId))
                throw new IllegalArgumentException("El email «" + email + "» ya está registrado");
        });
    }
}