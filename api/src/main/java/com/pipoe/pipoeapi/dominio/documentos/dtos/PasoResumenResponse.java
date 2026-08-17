package com.pipoe.pipoeapi.dominio.documentos.dtos;

import com.pipoe.pipoeapi.dominio.documentos.entities.EstadoPaso;

import lombok.*;

/** Un paso en el diagrama de flujo de la fase. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PasoResumenResponse {

    private Long pasoId;
    private Integer orden;
    private String tituloCorto;
    private String titulo;
    private Boolean esProducto;
    private EstadoPaso estado;

    /**
     * Si hoy se puede marcar como completado. Falso cuando el paso anterior está vacío, o
     * cuando es el producto y la fase todavía tiene pasos sin completar.
     */
    private Boolean puedeCompletarse;
    /** Por qué no se puede completar, para mostrarlo en la UI sin repetir la regla. */
    private String motivoBloqueo;
}
