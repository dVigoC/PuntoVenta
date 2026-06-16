package com.Venta.PuntoVenta.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comandas_cocina")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ComandaCocina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /**
     * Generado por trigger BD: COM-YYYYMMDD-NNNN
     * insertable=false, updatable=false para que Hibernate no lo sobrescriba.
     */
    @Column(name = "numero_comanda", nullable = false, unique = true, length = 30,
            insertable = false, updatable = false)
    private String numeroComanda;

    /** Número de envío dentro del mismo pedido: 1ra comanda, 2da, etc. */
    @Column(name = "nro_envio", nullable = false)
    private Integer nroEnvio = 1;

    @Column(nullable = false)
    private Boolean impresa = false;

    @Column(name = "impresa_auto", nullable = false)
    private Boolean impresaAuto = false;

    @Column(name = "impresora_destino", length = 100)
    private String impresoraDestino;

    /**
     * ENVIADA | EN_PREP | LISTA | ENTREGADA
     */
    @Column(nullable = false, length = 20)
    private String estado = "ENVIADA";

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DetalleComandaCocina> detalles = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;
}