package com.pipoe.pipoeapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Sirve para los dos tipos de sesión. El tope de 100 es el de `colaboradores.nombre`, la más
 * corta de las dos columnas.
 */
@Data
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombreCompleto;
}
