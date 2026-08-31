package com.pipoe.pipoeapi.dominio.usuarios.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Boolean y no boolean: sin mandar el campo se rechaza, en vez de tomarse como false. */
@Data
public class CambiarActivoRequest {

    @NotNull(message = "Hay que indicar si la cuenta queda activa")
    private Boolean activo;
}
