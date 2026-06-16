package com.Venta.PuntoVenta.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.Venta.PuntoVenta.model.Usuario;

public class CustomUserDetails implements UserDetails {
 
    private final Usuario usuario;
 
    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
    }
 
    public Usuario getUsuario() {
        return usuario;
    }
 
    public Long getId() {
        return usuario.getId();
    }
 
    public String getNombreCompleto() {
        return usuario.getNombreCompleto();
    }
 
    public String getRolNombre() {
        return usuario.getRol().getNombre();
    }
 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
    }
 
    @Override
    public String getPassword() {
        return usuario.getPasswordHash();
    }
 
    @Override
    public String getUsername() {
        return usuario.getUsername();
    }
 
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
 
    @Override
    public boolean isAccountNonLocked() {
        return !usuario.isBloqueado();
    }
 
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
 
    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}