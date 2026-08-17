package com.pipoe.pipoeapi.parametros.dtos;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.parametros.entities.Parametros;

import lombok.*;

/**
 * Límites que el frontend necesita para armar la UI (cuántos proyectos quedan, cuántas fases
 * mostrar). Los máximos salen de la tabla `parametros`; la cantidad de fases es fija.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ConfiguracionResponse {

    private Integer maxProyectosPorUsuario;
    private Integer maxColaboradoresPorProyecto;
    private Integer cantidadFases;

    public static ConfiguracionResponse from(Parametros parametros) {
        return ConfiguracionResponse.builder()
            .maxProyectosPorUsuario(parametros.getMaxProyectosPorUsuario())
            .maxColaboradoresPorProyecto(parametros.getMaxColaboradoresPorProyecto())
            .cantidadFases(Fase.values().length)
            .build();
    }
}
