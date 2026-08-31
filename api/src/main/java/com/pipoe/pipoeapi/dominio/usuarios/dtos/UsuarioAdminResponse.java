package com.pipoe.pipoeapi.dominio.usuarios.dtos;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import lombok.*;

/**
 * Un usuario visto desde el panel de administración.
 *
 * No lleva la contraseña ni ningún dato del contenido de sus proyectos: sirve para decidir a
 * quién dar de baja y a quién no, no para mirar lo que la gente escribió.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UsuarioAdminResponse {

    private Long id;
    private String email;
    private String nombreCompleto;
    private Role role;

    /** false es una baja lógica: la cuenta no entra, pero lo que hizo sigue estando. */
    private boolean activo;

    /** Null si la cuenta nunca inició sesión. */
    private LocalDateTime ultimoAcceso;

    private long proyectos;

    public static UsuarioAdminResponse from(Usuario usuario, long proyectos) {
        return UsuarioAdminResponse.builder()
            .id(usuario.getId())
            .email(usuario.getEmail())
            .nombreCompleto(usuario.getNombreCompleto())
            .role(usuario.getRole())
            .activo(usuario.isEnabled())
            .ultimoAcceso(usuario.getUltimoAcceso())
            .proyectos(proyectos)
            .build();
    }
}
