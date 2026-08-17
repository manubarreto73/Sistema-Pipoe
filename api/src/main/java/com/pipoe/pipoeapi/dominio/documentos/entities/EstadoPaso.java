package com.pipoe.pipoeapi.dominio.documentos.entities;

/**
 * Estado de un paso dentro de un proyecto. No se guarda: se deriva del documento, así no puede
 * quedar desincronizado con el contenido real.
 */
public enum EstadoPaso {
    /** Nunca se escribió nada. */
    PENDIENTE,
    /** Tiene contenido pero no está marcado como terminado. */
    EN_PROGRESO,
    /** El equipo lo dio por terminado. Se puede seguir editando igual. */
    COMPLETADO
}
