package com.pipoe.pipoeapi.dominio.colaboradores.dtos;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.ColaboradorPermiso;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PermisoResponse {

    private Fase fase;
    /** Nombre legible de la fase, para no repetir el diccionario en el frontend. */
    private String faseNombre;
    private NivelPermiso nivel;

    public static PermisoResponse from(ColaboradorPermiso permiso) {
        return new PermisoResponse(
            permiso.getFase(), permiso.getFase().getNombre(), permiso.getNivel()
        );
    }
}
