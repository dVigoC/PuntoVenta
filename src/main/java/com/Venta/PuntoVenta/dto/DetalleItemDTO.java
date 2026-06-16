package com.Venta.PuntoVenta.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
public class DetalleItemDTO {

    private Long      productoId;
    private Integer   cantidad;
    private BigDecimal precioUnitario;   // puede venir del front o recalcularse en el service
    private String    notaCocina;
}