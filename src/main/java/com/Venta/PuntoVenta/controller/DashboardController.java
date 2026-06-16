package com.Venta.PuntoVenta.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.Venta.PuntoVenta.model.Empresa;
import com.Venta.PuntoVenta.repository.EmpresaRepository;
import com.Venta.PuntoVenta.repository.PedidoRepository;
import com.Venta.PuntoVenta.security.CustomUserDetails;
import com.Venta.PuntoVenta.service.MesaService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {
 
    private final EmpresaRepository empresaRepository;
    private final MesaService mesaService;
    private final PedidoRepository pedidoRepository;
 
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model,
                        HttpServletRequest request) {

        
        // Si por alguna razón la sesión se pierde, mandarlo al login en vez de crashear
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        // 1. Definir el rango de tiempo para "Hoy" (00:00:00 hasta las 23:59:59)
        // Usamos ZoneOffset.of("-05:00") que corresponde a la hora de Perú/Bogotá
        OffsetDateTime inicioHoy = LocalDate.now().atStartOfDay().atOffset(ZoneOffset.of("-05:00"));
        OffsetDateTime finHoy = LocalDate.now().atTime(LocalTime.MAX).atOffset(ZoneOffset.of("-05:00"));

        // 2. Calcular las métricas del día
        long pedidosAbiertosHoy = pedidoRepository.contarPedidosRealizadosHoy(inicioHoy, finHoy);
        BigDecimal ventasHoy = pedidoRepository.sumarTotalVentasHoy(inicioHoy, finHoy);

        model.addAttribute("pedidosActivos", pedidoRepository.findActivosConMesaYArea());
        // Agregar después de las otras métricas
        model.addAttribute("pedidosEnCocina", pedidoRepository.contarPedidosEnCocina());        
        model.addAttribute("mesasOcupadas", mesaService.contarPorEstado("OCUPADA"));
        model.addAttribute("pedidosAbiertosHoy", pedidosAbiertosHoy);
        model.addAttribute("ventasHoy", ventasHoy);

        Empresa empresa = empresaRepository.findFirstByActivoTrue().orElse(null);
        model.addAttribute("empresa", empresa);
        model.addAttribute("usuario", userDetails);
        model.addAttribute("currentUri", request.getRequestURI());
        return "dashboard";
    }
 
    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}
