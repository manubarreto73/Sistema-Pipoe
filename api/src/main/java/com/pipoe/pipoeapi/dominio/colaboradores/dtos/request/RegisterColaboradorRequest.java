package com.pipoe.pipoeapi.dominio.colaboradores.dtos.request;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterColaboradorRequest {

    @NotBlank
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 150, message = "El email no puede exceder los 150 caracteres")
    private String email;

    public Colaborador toEntity() {
        return Colaborador.builder()
            .nombre(this.nombre)
            .email(this.email)
            .build();
    }
}
