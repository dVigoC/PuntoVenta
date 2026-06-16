package com.Venta.PuntoVenta.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empresa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Empresa {
    
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, length = 150)
    private String nombre;
 
    @Column(unique = true, length = 20)
    private String ruc;
 
    @Column(length = 255)
    private String direccion;
 
    @Column(length = 20)
    private String telefono;
 
    @Column(length = 100)
    private String email;
 
    @Column(name = "logo_url")
    private String logoUrl;
 
    @Column(nullable = false, length = 10)
    private String moneda = "PEN";
 
    @Column(name = "porcentaje_igv", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIgv = new BigDecimal("18.00");
 
    @Column(name = "pie_factura", length = 255)
    private String pieFactura = "Gracias por su visita";
 
    @Column(length = 150)
    private String desarrollador = "Desarrollado por TuEmpresa Dev";
 
    @Column(nullable = false)
    private boolean activo = true;
 
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();
 
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();
 
    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = OffsetDateTime.now();
    }
}
