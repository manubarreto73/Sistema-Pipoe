package com.pipoe.pipoeapi.auth.dto;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String email;
    private String nombreCompleto;
    private Role role;

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(), usuario.getEmail(), usuario.getNombreCompleto(), usuario.getRole()
        );
    }
}
