package com.Venta.PuntoVenta.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "mesas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"area_id", "numero"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // EAGER: carga el área junto con la mesa en la misma consulta.
    // LAZY + Thymeleaf causa LazyInitializationException porque la sesión
    // de Hibernate ya está cerrada cuando el template intenta acceder al área.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(nullable = false, length = 20)
    private String estado = "LIBRE";   // LIBRE | OCUPADA | RESERVADA | INACTIVA

    @Column(nullable = false)
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}