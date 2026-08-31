package com.pipoe.pipoeapi.dominio.proyectos.dtos;

/**
 * Cuánto avanzó una fase, para el indicador en colores del listado de la administradora.
 *
 * Tres estados y no dos: "completa o no" escondería la diferencia entre un proyecto que ni
 * arrancó una fase y otro al que le falta sólo el producto, que es justamente lo que se quiere
 * ver de un vistazo al recorrer la lista.
 */
public enum EstadoFase {
    /** Ningún paso de la fase está completado. */
    SIN_EMPEZAR,
    /** Hay pasos completados, pero el producto de la fase todavía no. */
    EN_PROGRESO,
    /** El producto de la fase está completado. Es lo que cierra la fase. */
    COMPLETA
}
