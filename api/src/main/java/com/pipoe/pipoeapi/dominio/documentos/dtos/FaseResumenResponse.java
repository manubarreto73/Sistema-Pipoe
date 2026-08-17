package com.pipoe.pipoeapi.dominio.documentos.dtos;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import lombok.*;

/** Una fase vista desde un proyecto: cuánto se avanzó y qué puede hacer quien mira. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class FaseResumenResponse {

    private Fase fase;
    private String nombre;
    private String ideaCentral;
    private Integer orden;

    /** Nombre del entregable de la fase ("Plan de promoción"). */
    private String producto;
    private Boolean productoCompletado;
    /** El producto sólo se habilita cuando todos los pasos de la fase están completos. */
    private Boolean productoHabilitado;

    /** No incluye al producto: son los pasos del despliegue. */
    private Integer totalPasos;
    private Integer pasosCompletados;

    private NivelPermiso nivel;
}
