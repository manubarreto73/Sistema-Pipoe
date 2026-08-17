package com.pipoe.pipoeapi.dominio.documentos.dtos;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.documentos.entities.EstadoPaso;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import lombok.*;

/** Todo lo que necesita la pantalla de un paso: consigna, documento y con qué permiso se entra. */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PasoDetalleResponse {

    private Long pasoId;
    private Fase fase;
    private String faseNombre;
    private Integer orden;
    private String titulo;
    private String tituloCorto;
    private Boolean esProducto;

    /** Vacíos hasta que la dueña los cargue desde la pantalla de admin. */
    private String explicacion;
    private String ejemplo;

    private String contenido;
    /** La que hay que devolver al guardar; si no coincide, la API responde 409. */
    private Integer version;
    private EstadoPaso estado;
    private Boolean puedeCompletarse;
    private String motivoBloqueo;

    private LocalDateTime actualizadoEn;
    private String actualizadoPor;

    private NivelPermiso nivel;
    private Boolean puedeEditar;

    /** Nombre de otra persona con el documento abierto, o null. */
    private String editandoOtro;
}
