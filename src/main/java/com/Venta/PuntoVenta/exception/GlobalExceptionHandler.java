package com.Venta.PuntoVenta.exception;

import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
 
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }
 
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        model.addAttribute("message", "No tiene permisos para realizar esta acción.");
        return "error/403";
    }
 
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception ex, HttpServletRequest request, Model model) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("error", ex.getMessage());
            return ResponseEntity.status(500).body(body);
        }
        
        model.addAttribute("mensaje", "Ocurrió un error inesperado.");
        return "error/500";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleResourceNotFound(NoResourceFoundException ex) {
        // Esto responde un 404 limpio al navegador sin disparar la pantalla de error 500
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}