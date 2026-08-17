package com.pipoe.pipoeapi.dominio.landing.entities;

import lombok.Getter;

/**
 * Los huecos de texto de la portada pública.
 *
 * Es un enum y no una lista libre a propósito: la portada tiene una estructura fija —encabezado,
 * descripción, modelo, biografía— y lo que se edita son las palabras, no el armado de la página.
 * Agregar una sección es agregar un valor acá y su lugar en el frontend.
 */
@Getter
public enum ClaveTexto {

    HERO_TITULO(
        "Título principal", "Lo primero que se lee al entrar.", Tipo.PLANO, 1),
    HERO_SUBTITULO(
        "Bajada del título", "La frase que acompaña al título.", Tipo.PLANO, 2),

    DESCRIPCION_TITULO(
        "Descripción · título", "Encabezado de la primera sección.", Tipo.PLANO, 3),
    DESCRIPCION_CUERPO(
        "Descripción · texto", "Qué es el Modelo PipoE y de dónde viene.", Tipo.RICO, 4),

    MODELO_TITULO(
        "El modelo · título", "Encabezado de la segunda sección.", Tipo.PLANO, 5),
    MODELO_CUERPO(
        "El modelo · texto", "Cómo se entiende la planificación y cuáles son sus componentes.",
        Tipo.RICO, 6),

    BIOGRAFIA_TITULO(
        "Biografía · título", "Encabezado de la sección de la autora.", Tipo.PLANO, 7),
    BIOGRAFIA_CUERPO(
        "Biografía · texto", "La biografía. Al lado se muestra la foto.", Tipo.RICO, 8);

    /** Cómo se edita: una línea suelta o un texto con formato. */
    public enum Tipo { PLANO, RICO }

    private final String etiqueta;
    private final String ayuda;
    private final Tipo tipo;
    private final int orden;

    ClaveTexto(String etiqueta, String ayuda, Tipo tipo, int orden) {
        this.etiqueta = etiqueta;
        this.ayuda = ayuda;
        this.tipo = tipo;
        this.orden = orden;
    }
}
