package com.pipoe.pipoeapi.dominio.proyectos.dtos.request;

import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterProyectoRequest {

    @NotBlank
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String nombre;

    public Proyecto toEntity() {
        return Proyecto.builder()
            .nombre(this.nombre)
            .build();
    }
}
