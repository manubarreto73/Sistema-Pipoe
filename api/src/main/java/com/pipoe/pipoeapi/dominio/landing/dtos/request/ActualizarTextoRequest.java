package com.pipoe.pipoeapi.dominio.landing.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ActualizarTextoRequest {

    @NotBlank(message = "El texto no puede quedar vacío")
    @Size(max = 20000, message = "El texto no puede exceder los 20000 caracteres")
    private String contenido;
}
