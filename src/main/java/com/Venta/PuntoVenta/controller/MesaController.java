package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.model.Mesa;
import com.Venta.PuntoVenta.service.AreaService;
import com.Venta.PuntoVenta.service.MesaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;
    private final AreaService areaService;

    private static final int PAGE_SIZE = 10;

    // ─────────────────────────────────────────────────────────────────────────
    // LISTADO PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "")  String busqueda,
            @RequestParam(required = false)   Long   areaId,
            @RequestParam(defaultValue = "")  String estado,
            @RequestParam(defaultValue = "0") int    pagina,
            Model model) {

        Sort sort = Sort.by(
                Sort.Order.asc("area.nombre"),
                Sort.Order.asc("numero")
        );
        PageRequest pageable = PageRequest.of(pagina, PAGE_SIZE, sort);

        Page<Mesa> page = mesaService.buscar(busqueda, areaId, estado, pageable);

        // Stats
        model.addAttribute("totalMesas",      mesaService.contarTotal());
        model.addAttribute("mesasLibres",     mesaService.contarPorEstado("LIBRE"));
        model.addAttribute("mesasOcupadas",   mesaService.contarPorEstado("OCUPADA"));
        model.addAttribute("mesasReservadas", mesaService.contarPorEstado("RESERVADA"));

        // Datos para selects
        model.addAttribute("areas",      areaService.listarActivas());
        model.addAttribute("todasAreas", areaService.listarTodas());

        // Tabla y filtros
        model.addAttribute("mesas",    page.getContent());
        model.addAttribute("page",     page);
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("areaId",   areaId);
        model.addAttribute("estado",   estado);

        return "mesas/lista";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR MESA
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/crear")
    public String crear(
            @RequestParam                           String  numero,
            @RequestParam                           Long    areaId,
            @RequestParam(defaultValue = "LIBRE")   String  estado,
            @RequestParam(defaultValue = "false")   boolean activo,
            RedirectAttributes ra) {
        try {
            mesaService.crear(numero.trim(), areaId, estado, activo);
            ra.addFlashAttribute("flashMensaje", "Mesa " + numero + " creada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EDITAR MESA
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable                           Long    id,
            @RequestParam                           Long    areaId,
            @RequestParam(defaultValue = "LIBRE")   String  estado,
            @RequestParam(defaultValue = "false")   boolean activo,
            RedirectAttributes ra) {
        try {
            mesaService.editar(id, areaId, estado, activo);
            ra.addFlashAttribute("flashMensaje", "Mesa actualizada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESACTIVAR MESA
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            mesaService.desactivar(id);
            ra.addFlashAttribute("flashMensaje", "Mesa desactivada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁREAS — CREAR
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/areas/crear")
    public String crearArea(
            @RequestParam                           String  nombre,
            @RequestParam(required = false)         String  descripcion,
            @RequestParam(defaultValue = "false")   boolean activo,
            RedirectAttributes ra) {
        try {
            areaService.crear(nombre.trim().toUpperCase(), descripcion, activo);
            ra.addFlashAttribute("flashMensaje", "Área «" + nombre + "» creada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁREAS — EDITAR
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/areas/{id}/editar")
    public String editarArea(
            @PathVariable                           Long    id,
            @RequestParam                           String  nombre,
            @RequestParam(required = false)         String  descripcion,
            @RequestParam(defaultValue = "false")   boolean activo,
            RedirectAttributes ra) {
        try {
            areaService.editar(id, nombre.trim().toUpperCase(), descripcion, activo);
            ra.addFlashAttribute("flashMensaje", "Área actualizada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁREAS — DESACTIVAR
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/areas/{id}/desactivar")
    public String desactivarArea(@PathVariable Long id, RedirectAttributes ra) {
        try {
            areaService.desactivar(id);
            ra.addFlashAttribute("flashMensaje", "Área desactivada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/mesas";
    }
}