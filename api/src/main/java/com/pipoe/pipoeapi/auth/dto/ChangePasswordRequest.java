package com.pipoe.pipoeapi.auth.dto;

import com.pipoe.pipoeapi.parametros.Constantes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Pattern(regexp = Constantes.PASSWORD_REGEX, message = Constantes.PASSWORD_MENSAJE)
    private String newPassword;
}
