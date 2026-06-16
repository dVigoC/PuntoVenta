package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.service.CocinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cocina")
@RequiredArgsConstructor
public class CocinaController {

    private final CocinaService cocinaService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pedidos", cocinaService.listarPedidosEnCocina());
        return "cocina/lista";
    }

    @PostMapping("/items/{itemId}/estado")
    public String cambiarEstado(
            @PathVariable Long itemId,
            @RequestParam String nuevoEstado,
            RedirectAttributes ra) {

        try {
            String msg = cocinaService.cambiarEstadoItem(itemId, nuevoEstado);
            ra.addFlashAttribute("flashMensaje", msg);
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }

        return "redirect:/cocina";
    }
}