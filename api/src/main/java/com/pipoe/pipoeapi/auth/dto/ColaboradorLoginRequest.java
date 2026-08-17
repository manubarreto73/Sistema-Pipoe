package com.pipoe.pipoeapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColaboradorLoginRequest {
    @NotBlank
    private String nombreProyecto;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String password;
}
