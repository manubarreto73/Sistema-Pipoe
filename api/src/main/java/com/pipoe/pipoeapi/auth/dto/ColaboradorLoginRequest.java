package com.pipoe.pipoeapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ColaboradorLoginRequest {

    /**
     * El código del proyecto (PIPOE-7K2F), no su nombre. El nombre servía de identificador sin
     * serlo: es único distinguiendo mayúsculas, así que dos proyectos podían llamarse igual a
     * ojos de una persona, y además el dueño podía cambiarlo y dejar afuera a todo su equipo.
     */
    @NotBlank(message = "El código del proyecto es obligatorio")
    private String codigoProyecto;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Ese email no parece válido")
    private String email;

    @NotBlank
    private String password;
}
