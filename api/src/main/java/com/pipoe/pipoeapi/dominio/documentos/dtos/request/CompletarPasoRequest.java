package com.pipoe.pipoeapi.dominio.documentos.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompletarPasoRequest {

    /** false para volver a marcarlo como en progreso. */
    @NotNull
    private Boolean completado;
}
