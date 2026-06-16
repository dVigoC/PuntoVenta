package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.model.Empleado;
import com.Venta.PuntoVenta.service.EmpleadoServices;
import com.Venta.PuntoVenta.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/empleados")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoServices service;

    // ── Constantes ────────────────────────────────────────────────────────────
    private static final int TAMANO_PAGINA = 10;

    // ── GET /empleados ─────────────────────────────────────────────────────────
    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "")       String busqueda,
            @RequestParam(defaultValue = "")       String cargo,
            @RequestParam(defaultValue = "activos") String estado,   // ← nuevo, por defecto muestra activos
            @RequestParam(defaultValue = "0")      int    pagina,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Page<Empleado> page = service.listar(busqueda, cargo, estado, pagina, TAMANO_PAGINA);
        Map<String, Long> stats = service.stats();

        model.addAttribute("empleados",      page.getContent());
        model.addAttribute("page",           page);
        model.addAttribute("busqueda",       busqueda);
        model.addAttribute("cargo",          cargo);
        model.addAttribute("estado",         estado);              // ← nuevo
        model.addAttribute("cargos",         service.cargos());
        model.addAttribute("totalEmpleados", stats.get("total"));
        model.addAttribute("activos",        stats.get("activos"));
        model.addAttribute("inactivos",      stats.get("inactivos"));
        model.addAttribute("currentUri",     "/empleados");
        model.addAttribute("title",          "Empleados");
        model.addAttribute("usuario",        userDetails);

        return "empleados/lista";
    }

    // ── POST /empleados/crear ─────────────────────────────────────────────────
    @PostMapping("/crear")
    public String crear(
            @RequestParam                                     String     nombres,
            @RequestParam                                     String     apellidos,
            @RequestParam                                     String     dni,
            @RequestParam(required = false)                   String     telefono,
            @RequestParam(required = false)                   String     direccion,
            @RequestParam(required = false)                   String     email,
            @RequestParam                                     String     cargo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaIngreso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCese,
            @RequestParam(defaultValue = "false")             boolean    activo,
            RedirectAttributes redir) {

        try {
            Empleado e = Empleado.builder()
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .dni(dni)
                    .telefono(telefono)
                    .direccion(direccion)
                    .email(email)
                    .cargo(cargo)
                    .fechaIngreso(fechaIngreso)
                    .fechaCese(fechaCese)
                    .activo(activo)
                    .build();

            service.crear(e);
            redir.addFlashAttribute("flashMensaje", "Empleado creado correctamente.");
            redir.addFlashAttribute("flashTipo",    "success");
        } catch (IllegalArgumentException ex) {
            redir.addFlashAttribute("flashMensaje", ex.getMessage());
            redir.addFlashAttribute("flashTipo",    "error");
        }

        return "redirect:/empleados";
    }

    // ── POST /empleados/{id}/editar ───────────────────────────────────────────
    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable                                     Long       id,
            @RequestParam                                     String     nombres,
            @RequestParam                                     String     apellidos,
            @RequestParam                                     String     dni,
            @RequestParam(required = false)                   String     telefono,
            @RequestParam(required = false)                   String     direccion,
            @RequestParam(required = false)                   String     email,
            @RequestParam                                     String     cargo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaIngreso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCese,
            @RequestParam(defaultValue = "false")             boolean    activo,
            RedirectAttributes redir) {

        try {
            Empleado datos = Empleado.builder()
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .dni(dni)
                    .telefono(telefono)
                    .direccion(direccion)
                    .email(email)
                    .cargo(cargo)
                    .fechaIngreso(fechaIngreso)
                    .fechaCese(fechaCese)
                    .activo(activo)
                    .build();

            service.actualizar(id, datos);
            redir.addFlashAttribute("flashMensaje", "Empleado actualizado correctamente.");
            redir.addFlashAttribute("flashTipo",    "success");
        } catch (IllegalArgumentException ex) {
            redir.addFlashAttribute("flashMensaje", ex.getMessage());
            redir.addFlashAttribute("flashTipo",    "error");
        }

        return "redirect:/empleados";
    }

    // ── POST /empleados/{id}/desactivar ───────────────────────────────────────
    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes redir) {
        try {
            service.desactivar(id);
            redir.addFlashAttribute("flashMensaje", "Empleado desactivado correctamente.");
            redir.addFlashAttribute("flashTipo",    "success");
        } catch (IllegalArgumentException ex) {
            redir.addFlashAttribute("flashMensaje", ex.getMessage());
            redir.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/empleados";
    }

    // ── API: verificar DNI único ──────────────────────────────────────────────
    @GetMapping("/api/verificar-dni")
    @ResponseBody
    public ResponseEntity<Boolean> verificarDni(
            @RequestParam                   String dni,
            @RequestParam(required = false) Long   excluirId) {

        boolean existe = (excluirId != null)
                ? service.existeDniParaOtro(dni, excluirId)
                : service.existeDni(dni);
        return ResponseEntity.ok(existe);
    }

    // ── API: verificar Email único ────────────────────────────────────────────
    @GetMapping("/api/verificar-email")
    @ResponseBody
    public ResponseEntity<Boolean> verificarEmail(
            @RequestParam                   String email,
            @RequestParam(required = false) Long   excluirId) {

        boolean existe = (excluirId != null)
                ? service.existeEmailParaOtro(email, excluirId)
                : service.existeEmail(email);
        return ResponseEntity.ok(existe);
    }
}