package com.Venta.PuntoVenta.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class ProductoDTO {

    private Long id;

    @NotNull(message = "La categoría es obligatoria")
    private Long categoriaId;

    @Size(max = 30, message = "El código no puede superar 30 caracteres")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 1000)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    private BigDecimal precio;

    private Boolean disponible = true;
    private Boolean activo     = true;
}