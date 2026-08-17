package com.pipoe.pipoeapi.dominio.colaboradores.dtos.request;

import java.util.List;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Reemplaza los permisos del colaborador. Se manda el mapa completo de las 5 fases, no un
 * parche: así el resultado no depende del orden en que se aplicaron los cambios.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ActualizarPermisosRequest {

    @NotEmpty(message = "Hay que enviar los permisos de las 5 fases")
    @Valid
    private List<PermisoFaseRequest> permisos;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class PermisoFaseRequest {

        @NotNull(message = "La fase es obligatoria")
        private Fase fase;

        @NotNull(message = "El nivel es obligatorio")
        private NivelPermiso nivel;
    }
}
