package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.dto.CategoriaDTO;
import com.Venta.PuntoVenta.dto.ProductoDTO;
import com.Venta.PuntoVenta.service.CategoriaService;
import com.Venta.PuntoVenta.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProductoController {

    private final ProductoService  productoService;
    private final CategoriaService categoriaService;

    private static final int TAMANO_PAGINA = 10;

    // ── Listado ───────────────────────────────────────────────────────────────
    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "0")  int    pagina,
            @RequestParam(required = false)    String busqueda,
            @RequestParam(required = false)    Long   categoriaId,
            Model model) {

        var page = productoService.listar(busqueda, categoriaId, pagina, TAMANO_PAGINA);

        model.addAttribute("page",        page);
        model.addAttribute("productos",   page.getContent());
        model.addAttribute("busqueda",    busqueda);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("categorias",  categoriaService.listarActivas());
        model.addAttribute("todasCategorias", categoriaService.listarTodas());
        model.addAttribute("totalProductos",  productoService.contarTotal());
        model.addAttribute("disponibles",     productoService.contarDisponibles());
        model.addAttribute("noDisponibles",   productoService.contarNoDisp());
        return "productos/lista";
    }

    // ── Crear producto ────────────────────────────────────────────────────────
    @PostMapping("/crear")
    public String crear(@Valid @ModelAttribute ProductoDTO dto,
                        @RequestParam(value = "disponible", required = false) String dispParam,
                        @RequestParam(value = "activo",     required = false) String activoParam,
                        RedirectAttributes ra) {

        dto.setDisponible("true".equals(dispParam));
        dto.setActivo("true".equals(activoParam));
        try {
            productoService.crear(dto);
            ra.addFlashAttribute("flashMensaje", "Producto creado correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // ── Editar producto ───────────────────────────────────────────────────────
    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute ProductoDTO dto,
                         @RequestParam(value = "disponible", required = false) String dispParam,
                         @RequestParam(value = "activo",     required = false) String activoParam,
                         RedirectAttributes ra) {
        dto.setDisponible("true".equals(dispParam));
        dto.setActivo("true".equals(activoParam));

        try {
            productoService.editar(id, dto);
            ra.addFlashAttribute("flashMensaje", "Producto actualizado correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // ── Eliminar (desactivar) producto ────────────────────────────────────────
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            productoService.desactivar(id);
            ra.addFlashAttribute("flashMensaje", "Producto desactivado correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // ── Crear categoría ───────────────────────────────────────────────────────
    @PostMapping("/categorias/crear")
    public String crearCategoria(@Valid @ModelAttribute CategoriaDTO dto,
                                 RedirectAttributes ra) {
        try {
            categoriaService.crear(dto);
            ra.addFlashAttribute("flashMensaje", "Categoría creada correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // ── Editar categoría ──────────────────────────────────────────────────────
    @PostMapping("/categorias/{id}/editar")
    public String editarCategoria(@PathVariable Long id,
                                  @Valid @ModelAttribute CategoriaDTO dto,
                                  RedirectAttributes ra) {
        try {
            categoriaService.editar(id, dto);
            ra.addFlashAttribute("flashMensaje", "Categoría actualizada correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // ── Desactivar categoría ──────────────────────────────────────────────────
    @PostMapping("/categorias/{id}/desactivar")
    public String desactivarCategoria(@PathVariable Long id, RedirectAttributes ra) {
        try {
            categoriaService.desactivar(id);
            ra.addFlashAttribute("flashMensaje", "Categoría desactivada correctamente");
            ra.addFlashAttribute("flashTipo",    "success");
        } catch (Exception ex) {
            ra.addFlashAttribute("flashMensaje", "Error: " + ex.getMessage());
            ra.addFlashAttribute("flashTipo",    "error");
        }
        return "redirect:/productos";
    }

    // Endpoint para verificar si el código existe (llamado desde el frontend)
    @GetMapping("/api/verificar-codigo")
    @ResponseBody
    public boolean verificarCodigo(@RequestParam String codigo, 
                                @RequestParam(required = false) Long id) {
        if (id == null) return productoService.codigoExiste(codigo);
        return productoService.existeCodigoExcluyendoId(codigo, id);
    }
}