package com.pipoe.pipoeapi.dominio.landing.dtos;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.landing.entities.ClaveTexto;
import com.pipoe.pipoeapi.dominio.landing.entities.TextoLanding;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TextoLandingResponse {

    private ClaveTexto clave;
    private String contenido;
    /** Rótulo y ayuda para la pantalla de edición; el visitante de la portada los ignora. */
    private String etiqueta;
    private String ayuda;
    /** PLANO | RICO: decide si se edita con un campo o con el editor de texto. */
    private String tipo;
    private Integer orden;
    private LocalDateTime actualizadoEn;

    public static TextoLandingResponse from(TextoLanding texto) {
        ClaveTexto clave = texto.getClave();

        return TextoLandingResponse.builder()
            .clave(clave)
            .contenido(texto.getContenido())
            .etiqueta(clave.getEtiqueta())
            .ayuda(clave.getAyuda())
            .tipo(clave.getTipo().name())
            .orden(clave.getOrden())
            .actualizadoEn(texto.getActualizadoEn())
            .build();
    }
}
