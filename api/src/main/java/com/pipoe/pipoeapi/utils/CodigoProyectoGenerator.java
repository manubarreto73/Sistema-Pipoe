package com.pipoe.pipoeapi.utils;

import java.security.SecureRandom;

/**
 * El código con el que un colaborador entra a su proyecto: PIPOE-7K2F.
 *
 * Reemplaza al nombre del proyecto en el login. El nombre servía como identificador pero no lo
 * era: es único con distinción de mayúsculas —"Proyecto Aldea" y "proyecto aldea" conviven— y
 * además el dueño puede cambiarlo, dejando afuera a todos sus colaboradores de golpe.
 *
 * El alfabeto excluye 0/O y 1/I/L a propósito: este código se dicta por teléfono y se copia de
 * un mail, y son los pares que se confunden al leerlos.
 */
public class CodigoProyectoGenerator {

    private static final String PREFIJO = "PIPOE-";
    private static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LARGO = 4;

    private static final SecureRandom RANDOM = new SecureRandom();

    private CodigoProyectoGenerator() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIJO.length() + LARGO).append(PREFIJO);

        for (int i = 0; i < LARGO; i++)
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));

        return sb.toString();
    }

    /** Lo que la persona escribe en el login: se acepta con o sin prefijo, y en cualquier caja. */
    public static String normalizar(String codigo) {
        if (codigo == null) return null;

        String limpio = codigo.trim().toUpperCase(java.util.Locale.ROOT).replace(" ", "");

        return limpio.startsWith(PREFIJO) ? limpio : PREFIJO + limpio;
    }
}
