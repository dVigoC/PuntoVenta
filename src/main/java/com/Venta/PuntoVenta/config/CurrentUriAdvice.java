package com.Venta.PuntoVenta.config;

import com.Venta.PuntoVenta.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class CurrentUriAdvice {

    @ModelAttribute
    public void addAttributes(HttpServletRequest request, Model model) {

        // ── URI actual ────────────────────────────────────────────────
        String uri = request.getRequestURI();
        if (uri == null || "/error".equals(uri)) {
            uri = "";
        }
        model.addAttribute("currentUri", uri);

        // ── Usuario autenticado ───────────────────────────────────────
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {

            model.addAttribute("usuario", new UsuarioInfo(
                userDetails.getNombreCompleto(),
                userDetails.getRolNombre()
            ));
        }
    }

    public record UsuarioInfo(String nombreCompleto, String rolNombre) {}
}