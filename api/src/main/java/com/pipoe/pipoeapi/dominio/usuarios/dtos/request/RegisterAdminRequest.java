package com.pipoe.pipoeapi.dominio.usuarios.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Alta de otra cuenta con permisos de administración.
 *
 * Pide el nombre además del correo porque `nombre_completo` es obligatorio en la base y porque
 * es con lo que se la saluda en el mail donde viaja su contraseña. Quien reciba la cuenta puede
 * cambiarlo después desde su perfil.
 */
@Data
public class RegisterAdminRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ese email no parece válido")
    @Size(max = 150, message = "El email no puede exceder los 150 caracteres")
    private String email;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    private String nombreCompleto;
}
