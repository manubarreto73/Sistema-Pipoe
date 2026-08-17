package com.pipoe.pipoeapi.dominio.colaboradores.dtos;

import java.util.Comparator;
import java.util.List;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ColaboradorResponse {

    private Long id;
    private String nombre;
    private String email;
    private Long proyectoId;
    private String proyectoNombre;
    /** Siempre las 5 fases, ordenadas. */
    private List<PermisoResponse> permisos;

    public static ColaboradorResponse from(Colaborador colaborador) {
        return ColaboradorResponse.builder()
            .id(colaborador.getId())
            .nombre(colaborador.getNombre())
            .email(colaborador.getEmail())
            .proyectoId(colaborador.getProyecto().getId())
            .proyectoNombre(colaborador.getProyecto().getNombre())
            .permisos(colaborador.getPermisos().stream()
                .sorted(Comparator.comparingInt(permiso -> permiso.getFase().getOrden()))
                .map(PermisoResponse::from)
                .toList())
            .build();
    }
}
