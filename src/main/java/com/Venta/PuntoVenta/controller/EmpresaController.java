package com.Venta.PuntoVenta.controller;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Venta.PuntoVenta.model.Empresa;
import com.Venta.PuntoVenta.service.EmpresaService;
import com.Venta.PuntoVenta.service.SupabaseStorageService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;
    private final SupabaseStorageService storageService;

    // ── GET /empresa — ADMIN y CAJERO pueden ver ──────────────────
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public String ver(Model model) {
        Optional<Empresa> empresa = empresaService.obtenerEmpresaActiva();
        model.addAttribute("empresa", empresa.orElse(null));
        model.addAttribute("existeEmpresa", empresa.isPresent());
        model.addAttribute("currentUri", "/empresa");
        return "empresa/lista";
    }

    // ── POST /empresa/crear — solo ADMIN ──────────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/crear")
    public String crear(
            @RequestParam String nombre,
            @RequestParam(required = false) String ruc,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile logoFile,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(defaultValue = "PEN") String moneda,
            @RequestParam(defaultValue = "18.00") BigDecimal porcentajeIgv,
            @RequestParam(required = false, defaultValue = "Gracias por su visita") String pieFactura,
            @RequestParam(required = false, defaultValue = "Desarrollado por TuEmpresa Dev") String desarrollador,
            RedirectAttributes ra) {

        if (empresaService.existeEmpresaActiva()) {
            ra.addFlashAttribute("flashMensaje", "Ya existe una empresa registrada.");
            ra.addFlashAttribute("flashTipo", "error");
            return "redirect:/empresa";
        }

        try {

            // Archivo tiene prioridad sobre URL manual
            String urlFinal = resolverUrlLogo(logoFile, logoUrl);

            Empresa e = new Empresa();
            e.setNombre(nombre.trim());
            e.setRuc(ruc != null ? ruc.trim() : null);
            e.setDireccion(direccion);
            e.setTelefono(telefono);
            e.setEmail(email);
            e.setLogoUrl(logoUrl);
            e.setLogoUrl(urlFinal);
            e.setMoneda(moneda);
            e.setPorcentajeIgv(porcentajeIgv);
            e.setPieFactura(pieFactura);
            e.setDesarrollador(desarrollador);

            empresaService.crear(e);
            ra.addFlashAttribute("flashMensaje", "Empresa registrada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error al registrar la empresa: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }

        return "redirect:/empresa";
    }

    // ── POST /empresa/{id}/editar — solo ADMIN ────────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) MultipartFile logoFile,
            @RequestParam(required = false) String logoUrl,
            @RequestParam(defaultValue = "PEN") String moneda,
            @RequestParam(defaultValue = "18.00") BigDecimal porcentajeIgv,
            @RequestParam(required = false) String pieFactura,
            @RequestParam(required = false) String desarrollador,
            RedirectAttributes ra) {

        try {
            // Obtener empresa actual para conservar logo si no se sube uno nuevo
            Empresa empresaActual = empresaService.obtenerEmpresaActiva()
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

            // Resolver logo:
            // 1) Si sube archivo nuevo → subir y usar esa URL
            // 2) Si escribe URL manual (y no vacía) → usar esa URL
            // 3) Si ambos vacíos → conservar el logo actual
            String urlFinal;
            if (logoFile != null && !logoFile.isEmpty()) {
                urlFinal = storageService.subirLogo(logoFile);
            } else if (logoUrl != null && !logoUrl.isBlank()) {
                urlFinal = logoUrl.trim();
            } else {
                // Conservar el logo que ya tenía
                urlFinal = empresaActual.getLogoUrl();
            }

            Empresa datos = new Empresa();
            datos.setNombre(nombre.trim());
            datos.setDireccion(direccion);
            datos.setTelefono(telefono);
            datos.setEmail(email);
            datos.setLogoUrl(logoUrl);
            datos.setLogoUrl(urlFinal); 
            datos.setMoneda(moneda);
            datos.setPorcentajeIgv(porcentajeIgv);
            datos.setPieFactura(pieFactura);
            datos.setDesarrollador(desarrollador);

            empresaService.editar(id, datos);
            ra.addFlashAttribute("flashMensaje", "Empresa actualizada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error al actualizar: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }

        return "redirect:/empresa";
    }

    // ── POST /empresa/{id}/eliminar — solo ADMIN ──────────────────
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            empresaService.eliminar(id);
            ra.addFlashAttribute("flashMensaje", "Empresa eliminada. Ya puede registrar una nueva.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error al eliminar: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/empresa";
    }

    // ── Método auxiliar: resuelve qué URL de logo usar ────────────
    // Usado solo en CREAR (en editar la lógica está inline
    // porque necesita acceder al logo actual de la BD)
    private String resolverUrlLogo(MultipartFile logoFile, String logoUrl) throws Exception {
        if (logoFile != null && !logoFile.isEmpty()) {
            return storageService.subirLogo(logoFile);
        }
        if (logoUrl != null && !logoUrl.isBlank()) {
            return logoUrl.trim();
        }
        return null; // Sin logo
    }
}