package com.pipoe.pipoeapi.dominio.pasos.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Lo que la dueña puede cambiar del catálogo. El título y la fase no se tocan: son el modelo
 * PipoE y cambiarlos por pantalla sería editar la metodología sin dejar rastro.
 */
@Data
public class ActualizarPasoRequest {

    @NotNull(message = "La explicación es obligatoria")
    private String explicacion;

    @NotNull(message = "El ejemplo es obligatorio")
    private String ejemplo;

    /** La etiqueta del diagrama de flujo, donde el título completo no entra. */
    @NotBlank(message = "El título corto es obligatorio")
    @Size(max = 60, message = "El título corto no puede exceder los 60 caracteres")
    private String tituloCorto;
}
