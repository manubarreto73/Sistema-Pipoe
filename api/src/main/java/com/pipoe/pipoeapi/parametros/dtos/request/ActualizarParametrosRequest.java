package com.pipoe.pipoeapi.parametros.dtos.request;

import com.pipoe.pipoeapi.parametros.entities.Parametros;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ActualizarParametrosRequest {

    /**
     * Los topes son de sentido común, no técnicos: bajar el cupo a cero dejaría la app
     * inutilizable, y ponerlo en miles no cambia nada salvo esconder un error de tipeo.
     */
    @NotNull(message = "Indica el máximo de proyectos por usuario")
    @Min(value = 1, message = "Tiene que ser al menos 1")
    @Max(value = 100, message = "No puede pasar de 100")
    private Integer maxProyectosPorUsuario;

    @NotNull(message = "Indica el máximo de colaboradores por proyecto")
    @Min(value = 1, message = "Tiene que ser al menos 1")
    @Max(value = 100, message = "No puede pasar de 100")
    private Integer maxColaboradoresPorProyecto;

    public Parametros toEntity() {
        return Parametros.builder()
            .maxProyectosPorUsuario(this.maxProyectosPorUsuario)
            .maxColaboradoresPorProyecto(this.maxColaboradoresPorProyecto)
            .build();
    }
}
