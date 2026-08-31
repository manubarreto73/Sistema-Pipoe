package com.pipoe.pipoeapi.utils;

import java.util.Locale;

/**
 * Normalización del email, en un solo lugar.
 *
 * Un correo no distingue mayúsculas: "Ana@Correo.com" y "ana@correo.com" son el mismo buzón.
 * La base, en cambio, compara VARCHAR carácter por carácter, así que sin normalizar al escribir
 * las dos formas conviven como identidades distintas. Eso no era teórico: dejaba mandar dos
 * solicitudes de acceso con el mismo correo, y al aprobarlas creaba dos usuarios para la misma
 * persona, cada uno con su clave, sin ningún error a la vista.
 *
 * Se aplica en todas las altas y en todas las búsquedas por email. La migración V12 agrega
 * además un CHECK en la base, para que un alta que se olvide de pasar por acá falle en vez de
 * volver a abrir el agujero en silencio.
 */
public class Emails {

    private Emails() {}

    /** Null entra, null sale: la obligatoriedad la exigen las anotaciones del DTO, no esto. */
    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
