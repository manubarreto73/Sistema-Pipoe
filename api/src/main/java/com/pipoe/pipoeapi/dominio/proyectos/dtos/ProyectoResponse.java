package com.pipoe.pipoeapi.dominio.proyectos.dtos;

import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProyectoResponse {

    private Long id;
    private String nombre;
    /** El dueño se lo pasa a sus colaboradores: es lo que escriben para entrar. */
    private String codigo;
    private Long usuarioId;
    private String usuarioNombreCompleto;

    public static ProyectoResponse from(Proyecto proyecto) {
        return ProyectoResponse.builder()
            .id(proyecto.getId())
            .nombre(proyecto.getNombre())
            .codigo(proyecto.getCodigo())
            .usuarioId(proyecto.getUsuario().getId())
            .usuarioNombreCompleto(proyecto.getUsuario().getNombreCompleto())
            .build();
    }
}
