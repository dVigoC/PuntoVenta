package com.Venta.PuntoVenta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RolDTO {
    private Long id;
 
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;
 
    @Size(max = 200, message = "La descripción no puede superar 200 caracteres")
    private String descripcion;
 
    private boolean activo = true;
 
    // ── Getters / Setters ────────────────────────────────────────────────────
 
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
 
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
 
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
 
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
