package com.pipoe.pipoeapi.dominio.colaboradores.entities;

/**
 * Qué puede hacer un colaborador dentro de una fase del pipoe.
 *
 * Están ordenados de menos a más permisivo. Todavía no hay contenido dentro del proyecto sobre
 * el cual aplicarlos: por ahora sólo se guardan y se editan.
 */
public enum NivelPermiso {
    LECTURA,
    COMENTARIOS,
    EDICION
}
