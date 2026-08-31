package com.pipoe.pipoeapi.dominio.proyectos.dtos;

import java.util.Map;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import lombok.*;

/**
 * Un proyecto visto desde el panel de la administradora: lo justo para decidir en cuál entrar.
 *
 * No lleva nada del contenido del proyecto. Quien administra el sistema ve que un proyecto
 * existe, de quién es y cuánto avanzó; para leer lo que escribieron tiene que entrar, y ahí
 * queda con permiso de comentario y nada más.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AdminProyectoResponse {

    private Long id;
    private String nombre;
    private String codigo;

    private Long duenioId;
    private String duenio;

    private int colaboradores;

    /** Una entrada por cada una de las 5 fases. */
    private Map<Fase, EstadoFase> fases;

    /** Las 5 fases completas. Es el filtro "terminado" del listado. */
    private boolean terminado;
}
