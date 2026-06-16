package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.dto.UsuarioDTO;
import com.Venta.PuntoVenta.model.Usuario;
import com.Venta.PuntoVenta.service.RolService;
import com.Venta.PuntoVenta.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final RolService     rolService;       // ← agregado para listar TODOS los roles
    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String busqueda,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        Page<Usuario> page = usuarioService.listar(busqueda, pagina, PAGE_SIZE);

        model.addAttribute("usuarios",        page.getContent());
        model.addAttribute("page",            page);
        model.addAttribute("busqueda",        busqueda);
        // Roles activos para el select de usuarios
        model.addAttribute("roles",           usuarioService.listarRolesActivos());
        // Todos los roles (activos e inactivos) para el modal de gestión de roles
        model.addAttribute("todosLosRoles",   rolService.listarTodos());
        model.addAttribute("activosCount",    usuarioService.contarActivos());
        model.addAttribute("inactivosCount", usuarioService.contarInactivos());
        model.addAttribute("title",           "Usuarios");

        return "usuarios/lista";
    }

    @PostMapping("/crear")
    public String crear(
            @Valid @ModelAttribute("usuarioDTO") UsuarioDTO dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "Revisa los campos del formulario.");
            return "redirect:/usuarios";
        }
        try {
            usuarioService.crear(dto);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Usuario creado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @Valid @ModelAttribute("usuarioDTO") UsuarioDTO dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "Revisa los campos del formulario.");
            return "redirect:/usuarios";
        }
        try {
            usuarioService.actualizar(id, dto);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Usuario actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            usuarioService.eliminar(id);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Usuario desactivado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "No se pudo desactivar el usuario.");
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/desbloquear")
    public String desbloquear(@PathVariable Long id, RedirectAttributes ra) {
        usuarioService.desbloquear(id);
        ra.addFlashAttribute("flashTipo",    "success");
        ra.addFlashAttribute("flashMensaje", "Usuario desbloqueado correctamente.");
        return "redirect:/usuarios";
    }
}