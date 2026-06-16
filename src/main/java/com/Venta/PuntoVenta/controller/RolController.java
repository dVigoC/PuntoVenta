package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.dto.RolDTO;
import com.Venta.PuntoVenta.service.RolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/roles")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    @PostMapping("/crear")
    public String crear(
            @Valid @ModelAttribute("rolDTO") RolDTO dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "Revisa los campos del formulario de rol.");
            return "redirect:/usuarios";
        }
        try {
            rolService.crear(dto);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Rol creado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @Valid @ModelAttribute("rolDTO") RolDTO dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "Revisa los campos del formulario de rol.");
            return "redirect:/usuarios";
        }
        try {
            rolService.actualizar(id, dto);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Rol actualizado correctamente.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            rolService.desactivar(id);
            ra.addFlashAttribute("flashTipo",    "success");
            ra.addFlashAttribute("flashMensaje", "Rol desactivado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("flashTipo",    "error");
            ra.addFlashAttribute("flashMensaje", "No se pudo desactivar el rol.");
        }
        return "redirect:/usuarios";
    }
}