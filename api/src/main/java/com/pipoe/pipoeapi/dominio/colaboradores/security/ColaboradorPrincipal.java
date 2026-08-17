package com.pipoe.pipoeapi.dominio.colaboradores.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;

import lombok.Getter;

@Getter
public class ColaboradorPrincipal implements UserDetails {

    private final Colaborador colaborador;

    public ColaboradorPrincipal(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public Long getProyectoId() {
        return colaborador.getProyecto().getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_COLABORADOR"));
    }

    @Override
    public String getPassword() { return colaborador.getPassword(); }

    // El subject del JWT de colaborador es su id, no el email (el email no es único globalmente).
    @Override
    public String getUsername() { return String.valueOf(colaborador.getId()); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    // Al dar de baja a un colaborador, su token sigue existiendo hasta que venza: el filtro
    // mira esto para cortar la sesión en la request siguiente y no sólo en el próximo login.
    @Override
    public boolean isEnabled() { return colaborador.isActivo(); }
}
