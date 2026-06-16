package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.model.Empresa;
import com.Venta.PuntoVenta.repository.EmpresaRepository;
import com.Venta.PuntoVenta.security.CustomUserDetails;
import com.Venta.PuntoVenta.service.GraficasService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/graficas")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class GraficasController {

    private final GraficasService    graficasService;
    private final EmpresaRepository  empresaRepository;

    // ══════════════════════════════════════════════════════════════════
    // VISTA PRINCIPAL
    // ══════════════════════════════════════════════════════════════════
    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails userDetails,
                        HttpServletRequest request,
                        Model model) {

        if (userDetails == null) return "redirect:/login";

        Empresa empresa = empresaRepository.findFirstByActivoTrue().orElse(null);

        model.addAttribute("empresa",    empresa);
        model.addAttribute("usuario",    userDetails);
        model.addAttribute("currentUri", request.getRequestURI());

        return "graficas/lista";
    }

    // ══════════════════════════════════════════════════════════════════
    // API JSON — llamadas asíncronas desde Chart.js
    // ══════════════════════════════════════════════════════════════════

    /** Ventas totales agrupadas por mes (últimos 12 meses). */
    @GetMapping("/api/ventas-por-mes")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ventasPorMes() {
        return ResponseEntity.ok(graficasService.getVentasPorMes());
    }

    /** Heatmap: día de semana × hora del día (últimos 90 días). */
    @GetMapping("/api/heatmap-trafico")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> heatmap() {
        return ResponseEntity.ok(graficasService.getHeatmap());
    }

    /** Top 10 productos más vendidos (últimos 30 días). */
    @GetMapping("/api/top-productos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> topProductos() {
        return ResponseEntity.ok(graficasService.getTopProductos());
    }

    /** Ventas por categoría – gráfica de dona (últimos 30 días). */
    @GetMapping("/api/ventas-por-categoria")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ventasPorCategoria() {
        return ResponseEntity.ok(graficasService.getVentasPorCategoria());
    }

    /** Ticket promedio diario y conteo de pedidos (últimos 30 días). */
    @GetMapping("/api/ticket-promedio")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ticketPromedio() {
        return ResponseEntity.ok(graficasService.getTicketPromedioPorDia());
    }

    /** Estado de pedidos del día – dona compacta. */
    @GetMapping("/api/pedidos-estado-hoy")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pedidosEstadoHoy() {
        return ResponseEntity.ok(graficasService.getPedidosPorEstadoHoy());
    }

    /** Comparativa ventas: semana actual vs semana anterior. */
    @GetMapping("/api/comparativa-semanas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comparativaSemanas() {
        return ResponseEntity.ok(graficasService.getComparativaSemanas());
    }

    /** KPIs: mes actual vs mes anterior con variaciones porcentuales. */
    @GetMapping("/api/kpis")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kpis() {
        return ResponseEntity.ok(graficasService.getKpis());
    }
}