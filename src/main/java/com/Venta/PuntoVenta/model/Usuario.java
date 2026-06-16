package com.Venta.PuntoVenta.model;

import java.time.OffsetDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true, length = 60)
    private String username;
 
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
 
    @Column(unique = true, length = 120)
    private String email;
 
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;
 
    @Column(nullable = false)
    private boolean activo = true;
 
    @Column(name = "ultimo_acceso")
    private OffsetDateTime ultimoAcceso;
 
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;
 
    @Column(nullable = false)
    private boolean bloqueado = false;
 
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();
 
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();
 
    @PreUpdate
    public void preUpdate() {
        this.actualizadoEn = OffsetDateTime.now();
    }
}
