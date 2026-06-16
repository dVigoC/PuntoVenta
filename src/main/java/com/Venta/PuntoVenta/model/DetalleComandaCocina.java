package com.Venta.PuntoVenta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detalle_comanda_cocina")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DetalleComandaCocina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private ComandaCocina comanda;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "detalle_pedido_id", nullable = false)
    private DetallePedido detallePedido;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "nota_cocina", length = 255)
    private String notaCocina;
}