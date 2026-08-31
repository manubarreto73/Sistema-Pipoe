package com.pipoe.pipoeapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ese email no parece válido")
    private String email;

    @NotBlank
    private String password;
}
