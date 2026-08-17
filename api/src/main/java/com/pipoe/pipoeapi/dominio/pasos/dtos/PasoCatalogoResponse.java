package com.pipoe.pipoeapi.dominio.pasos.dtos;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;

import lombok.*;

/** Un paso del catálogo, tal como lo edita la dueña. Sin nada de ningún proyecto. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PasoCatalogoResponse {

    private Long id;
    private Fase fase;
    private String faseNombre;
    private Integer orden;
    private String titulo;
    private String tituloCorto;
    private String explicacion;
    private String ejemplo;
    private Boolean esProducto;
    /** Para que la pantalla de admin muestre cuánto falta cargar. */
    private Boolean contenidoCargado;

    public static PasoCatalogoResponse from(Paso paso) {
        return PasoCatalogoResponse.builder()
            .id(paso.getId())
            .fase(paso.getFase())
            .faseNombre(paso.getFase().getNombre())
            .orden(paso.getOrden())
            .titulo(paso.getTitulo())
            .tituloCorto(paso.getTituloCorto())
            .explicacion(paso.getExplicacion())
            .ejemplo(paso.getEjemplo())
            .esProducto(paso.isEsProducto())
            .contenidoCargado(!paso.getExplicacion().isBlank() && !paso.getEjemplo().isBlank())
            .build();
    }
}
