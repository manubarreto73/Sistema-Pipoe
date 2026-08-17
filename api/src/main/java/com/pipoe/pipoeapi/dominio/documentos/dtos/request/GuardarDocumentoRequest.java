package com.pipoe.pipoeapi.dominio.documentos.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GuardarDocumentoRequest {

    /**
     * HTML del editor. Puede venir vacío: borrar todo es una edición válida.
     *
     * El tope existe porque cada guardado además escribe una copia completa en el historial:
     * sin él, unos pocos `PUT` grandes llenan el disco. 200 KB de HTML son unas 60 carillas
     * de texto, muy por encima de la carilla que pide el modelo.
     */
    @NotNull
    @Size(max = 200_000, message = "El documento es demasiado largo")
    private String contenido;

    /** La versión sobre la que se editó. Si ya no es la actual, la API responde 409. */
    @NotNull(message = "Falta la versión del documento")
    private Integer version;
}
