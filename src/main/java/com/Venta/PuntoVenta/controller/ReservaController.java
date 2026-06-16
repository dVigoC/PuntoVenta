package com.Venta.PuntoVenta.controller;

import com.Venta.PuntoVenta.config.ZonaHorariaConfig;
import com.Venta.PuntoVenta.model.Reserva;
import com.Venta.PuntoVenta.repository.MesaRepository;
import com.Venta.PuntoVenta.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService  reservaService;
    private final MesaRepository  mesaRepo;

    private static final int PAGE_SIZE = 10;

    // =========================================================================
    // LISTADO PRINCIPAL
    // =========================================================================

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "")  String busqueda,
            @RequestParam(defaultValue = "")  String estado,
            @RequestParam(defaultValue = "0") int    pagina,
            Model model) {

        PageRequest pageable = PageRequest.of(
                pagina, PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "creadoEn"));

        Page<Reserva> page = reservaService.buscar(busqueda, estado, pageable);

        model.addAttribute("totalReservas",  page.getTotalElements());
        model.addAttribute("pendientes",     reservaService.contarPorEstado("PENDIENTE"));
        model.addAttribute("confirmadas",    reservaService.contarPorEstado("CONFIRMADA"));
        model.addAttribute("activas",        reservaService.contarPorEstado("ACTIVA"));
        model.addAttribute("completadas",    reservaService.contarPorEstado("COMPLETADA"));
        model.addAttribute("noShow",         reservaService.contarPorEstado("NO_SHOW"));

        model.addAttribute("mesasDisponibles",
                mesaRepo.findByEstadoInAndActivoTrue(
                        java.util.List.of("LIBRE", "RESERVADA")));

        model.addAttribute("todasMesas",
                mesaRepo.findByActivoTrueOrderByAreaNombreAscNumeroAsc());

        model.addAttribute("reservas",  page.getContent());
        model.addAttribute("page",      page);
        model.addAttribute("busqueda",  busqueda);
        model.addAttribute("estado",    estado);

        return "reserva/lista";
    }

    // =========================================================================
    // CREAR
    // =========================================================================

    @PostMapping("/crear")
    public String crear(
            @RequestParam                   Long   mesaId,
            @RequestParam                   String nombreCliente,
            @RequestParam(required = false) String telefonoCliente,
            @RequestParam                   String fechaReserva,
            @RequestParam(defaultValue = "15") int  toleranciaMinutos,
            @RequestParam(defaultValue = "1")  int  personas,
            @RequestParam(required = false) String observaciones,
            RedirectAttributes ra) {

        try {

            log.info("fechaReserva recibida: '{}'", fechaReserva);
            OffsetDateTime fecha = LocalDateTime.parse(fechaReserva)
                                   .atZone(ZonaHorariaConfig.ZONA_PERU)
                                   .toOffsetDateTime();
            reservaService.crear(mesaId, nombreCliente, telefonoCliente,
                    fecha, toleranciaMinutos, personas, observaciones);
            ra.addFlashAttribute("flashMensaje", "Reserva para «" + nombreCliente + "» creada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/reservas";
    }

    // =========================================================================
    // EDITAR
    // =========================================================================

    @PostMapping("/{id}/editar")
    public String editar(
            @PathVariable                   Long   id,
            @RequestParam                   Long   mesaId,
            @RequestParam                   String nombreCliente,
            @RequestParam(required = false) String telefonoCliente,
            @RequestParam                   String fechaReserva,
            @RequestParam(defaultValue = "15") int  toleranciaMinutos,
            @RequestParam(defaultValue = "1")  int  personas,
            @RequestParam(required = false) String observaciones,
            @RequestParam(required = false) String estado,
            RedirectAttributes ra) {

        try {
            OffsetDateTime fecha = LocalDateTime.parse(fechaReserva)
                                   .atZone(ZonaHorariaConfig.ZONA_PERU)
                                   .toOffsetDateTime();
            reservaService.editar(id, mesaId, nombreCliente, telefonoCliente,
                    fecha, toleranciaMinutos, personas, observaciones, estado);
            ra.addFlashAttribute("flashMensaje", "Reserva actualizada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/reservas";
    }

    // =========================================================================
    // CAMBIO RÁPIDO DE ESTADO
    // =========================================================================

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable Long   id,
            @RequestParam String nuevoEstado,
            RedirectAttributes ra) {

        try {
            reservaService.cambiarEstado(id, nuevoEstado);
            ra.addFlashAttribute("flashMensaje", "Estado cambiado a «" + nuevoEstado + "».");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/reservas";
    }

    // =========================================================================
    // ELIMINAR
    // =========================================================================

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            reservaService.eliminar(id);
            ra.addFlashAttribute("flashMensaje", "Reserva eliminada correctamente.");
            ra.addFlashAttribute("flashTipo", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("flashMensaje", "Error: " + e.getMessage());
            ra.addFlashAttribute("flashTipo", "error");
        }
        return "redirect:/reservas";
    }

    // =========================================================================
    // API: verificar nombre único (AJAX)
    // =========================================================================

    @GetMapping("/verificar-nombre")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> verificarNombre(
            @RequestParam                   String nombre,
            @RequestParam(required = false) Long   excludeId) {

        boolean existe = (excludeId == null)
                ? reservaService.existeNombre(nombre)
                : reservaService.existeNombreExcluyendo(nombre, excludeId);

        return ResponseEntity.ok(Map.of("existe", existe));
    }
}