package com.pipoe.pipoeapi.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Genera las claves iniciales de usuarios y colaboradores.
 *
 * La clave generada tiene que cumplir la misma política que se le exige al usuario cuando
 * la cambia (Constantes.PASSWORD_REGEX): elegir caracteres al azar de un pool no alcanza,
 * porque nada garantiza que salga una mayúscula o un número. Por eso se siembra un carácter
 * obligatorio de cada clase y recién después se completa y se mezcla.
 */
public class PasswordGenerator {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String NUMEROS = "23456789";
    private static final String SIMBOLOS = "!@#$%";
    private static final String TODOS = MAYUSCULAS + MINUSCULAS + NUMEROS + SIMBOLOS;

    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    public static String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);

        chars.add(random(MAYUSCULAS));
        chars.add(random(MINUSCULAS));
        chars.add(random(NUMEROS));
        chars.add(random(SIMBOLOS));

        while (chars.size() < LENGTH)
            chars.add(random(TODOS));

        Collections.shuffle(chars, RANDOM);

        StringBuilder sb = new StringBuilder(LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private static char random(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }
}
