package com.pipoe.pipoeapi.auth.dto;

import org.springframework.security.core.userdetails.UserDetails;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;
import com.pipoe.pipoeapi.dominio.colaboradores.security.ColaboradorPrincipal;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Identidad de la sesión, sea de un usuario o de un colaborador.
 * Nunca expone la contraseña: es lo que se devuelve en /me y dentro de LoginResponse.
 */
@Data
@AllArgsConstructor
public class SesionResponse {

    private String type;            // USUARIO | COLABORADOR
    private Long id;
    private String email;
    private String nombreCompleto;
    private Role role;              // null para colaboradores
    private Long proyectoId;        // null para usuarios
    private String proyectoNombre;  // null para usuarios

    public static SesionResponse from(Usuario usuario) {
        return new SesionResponse(
            "USUARIO",
            usuario.getId(),
            usuario.getEmail(),
            usuario.getNombreCompleto(),
            usuario.getRole(),
            null,
            null
        );
    }

    public static SesionResponse from(Colaborador colaborador) {
        return new SesionResponse(
            "COLABORADOR",
            colaborador.getId(),
            colaborador.getEmail(),
            colaborador.getNombre(),
            null,
            colaborador.getProyecto().getId(),
            colaborador.getProyecto().getNombre()
        );
    }

    public static SesionResponse from(UserDetails principal) {
        if (principal instanceof ColaboradorPrincipal colaborador)
            return from(colaborador.getColaborador());

        if (principal instanceof Usuario usuario)
            return from(usuario);

        throw new IllegalArgumentException("Tipo de sesión desconocido: " + principal.getClass().getName());
    }
}
