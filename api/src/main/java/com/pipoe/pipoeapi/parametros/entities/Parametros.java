package com.pipoe.pipoeapi.parametros.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametros")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Parametros {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parametro_id")
    private Long id;

    @Column(name = "max_proyectos_por_usuario", nullable = false)
    private Integer maxProyectosPorUsuario;

    @Column(name = "max_colaboradores_por_proyecto", nullable = false)
    private Integer maxColaboradoresPorProyecto;
}
