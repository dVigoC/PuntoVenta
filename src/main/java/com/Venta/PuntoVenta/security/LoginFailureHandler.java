package com.Venta.PuntoVenta.security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.Venta.PuntoVenta.model.Usuario;
import com.Venta.PuntoVenta.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {
 
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private final UsuarioRepository usuarioRepository;
 
    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
 
        String username = request.getParameter("username");
        if (username != null) {
            Optional<Usuario> optUsuario = usuarioRepository.findByUsernameIgnoreCase(username.trim());
            optUsuario.ifPresent(usuario -> {
                int intentos = usuario.getIntentosFallidos() + 1;
                usuario.setIntentosFallidos(intentos);
                if (intentos >= MAX_FAILED_ATTEMPTS) {
                    usuario.setBloqueado(true);
                    log.warn("Cuenta bloqueada por múltiples intentos fallidos: {}", username);
                }
                usuarioRepository.save(usuario);
            });
        }
 
        log.warn("Login fallido: usuario={}, ip={}", username, request.getRemoteAddr());
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}