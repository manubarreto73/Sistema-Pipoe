package com.pipoe.pipoeapi.dominio.pasos.entities;

/**
 * Las cinco fases del modelo PipoE.
 *
 * No son secuenciales: se puede avanzar en cualquiera sin haber terminado las otras. El orden
 * es sólo el de presentación. Vive en código y no en una tabla porque es el modelo mismo:
 * no se agregan ni se sacan fases sin cambiar la aplicación.
 */
public enum Fase {

    PROMOCION("Promoción", "Compromiso para la acción", 1),
    INDAGACION("Indagación", "Estado de situación", 2),
    PROGRAMACION("Programación", "Capacidad de respuesta", 3),
    ORGANIZACION("Organización", "Capacidad ejecutiva", 4),
    EVALUACION("Evaluación", "Valoración del sentido", 5);

    private final String nombre;
    private final String ideaCentral;
    private final int orden;

    Fase(String nombre, String ideaCentral, int orden) {
        this.nombre = nombre;
        this.ideaCentral = ideaCentral;
        this.orden = orden;
    }

    public String getNombre() { return nombre; }
    public String getIdeaCentral() { return ideaCentral; }
    public int getOrden() { return orden; }
}
