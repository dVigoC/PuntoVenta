package com.Venta.PuntoVenta.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.Venta.PuntoVenta.model.Usuario;
import com.Venta.PuntoVenta.repository.UsuarioRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
 
    private final UsuarioRepository usuarioRepository;
 
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
 
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Usuario usuario = userDetails.getUsuario();
 
        // Reset failed attempts and update last access de forma segura
        if (usuarioRepository != null && usuario != null) {
            usuarioRepository.findById(usuario.getId()).ifPresent(u -> {
                u.setIntentosFallidos(0);
                u.setUltimoAcceso(OffsetDateTime.now());
                usuarioRepository.save(u);
            });
        }
 
        log.info("Login exitoso: usuario={}, ip={}",
                usuario != null ? usuario.getUsername() : "Desconocido",
                request.getRemoteAddr());
 
        // Establecemos el destino por defecto y dejamos que Spring Security maneje la redirección de forma limpia
        setDefaultTargetUrl("/dashboard");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}