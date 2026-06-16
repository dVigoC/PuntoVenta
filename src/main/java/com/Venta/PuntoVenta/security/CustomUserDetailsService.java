package com.Venta.PuntoVenta.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Venta.PuntoVenta.model.Usuario;
import com.Venta.PuntoVenta.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
 
    private final UsuarioRepository usuarioRepository;
 
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Sanitize input to prevent injection
        String cleanUsername = username != null ? username.trim() : "";
 
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(cleanUsername)
                .orElseThrow(() -> {
                    log.warn("Intento de login con usuario inexistente: {}", cleanUsername);
                    return new UsernameNotFoundException("Credenciales inválidas");
                });
 
        return new CustomUserDetails(usuario);
    }
}
