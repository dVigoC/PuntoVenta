package com.Venta.PuntoVenta.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Venta.PuntoVenta.model.Empresa;
import com.Venta.PuntoVenta.repository.EmpresaRepository;

import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthController {
 
    private final EmpresaRepository empresaRepository;
 
    @GetMapping("/login")
    public String loginPage(Authentication authentication,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String expired,
                            Model model) {
        // Redirect if already authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
 
        Empresa empresa = empresaRepository.findFirstByActivoTrue().orElse(null);
        model.addAttribute("empresa", empresa);
 
        if (error != null) {
            model.addAttribute("errorMsg", "Usuario o contraseña incorrectos. Verifique sus credenciales.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Ha cerrado sesión correctamente.");
        }
        if (expired != null) {
            model.addAttribute("errorMsg", "Su sesión ha expirado. Por favor inicie sesión nuevamente.");
        }
 
        return "auth/login";
    }
}
