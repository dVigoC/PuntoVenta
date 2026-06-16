package com.Venta.PuntoVenta.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reservas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    private Mesa mesa;

    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String nombreCliente;

    @Column(name = "telefono_cliente", length = 9)
    private String telefonoCliente;

    @Column(name = "fecha_reserva", nullable = false)
    private OffsetDateTime fechaReserva;

    @Column(name = "tolerancia_minutos", nullable = false)
    private int toleranciaMinutos = 15;

    @Column(nullable = false)
    private int personas = 1;

    /**
     * PENDIENTE | CONFIRMADA | ACTIVA | COMPLETADA | CANCELADA | NO_SHOW
     */
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}