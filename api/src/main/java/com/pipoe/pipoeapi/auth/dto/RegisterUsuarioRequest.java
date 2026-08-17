package com.pipoe.pipoeapi.auth.dto;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUsuarioRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String nombreCompleto;

    private Role role;
}
