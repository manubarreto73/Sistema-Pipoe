package com.pipoe.pipoeapi.parametros;

public class Constantes {

    public static final int PAGE_SIZE = 30;

    public static final int MAX_INTENTOS_LOGIN = 5;
    public static final int TIEMPO_INTENTOS = 5;
    public static final int TIEMPO_BLOQUEO_IP = 30;

    /**
     * Al menos una mayúscula, al menos un número y 8 caracteres.
     * Lo cumplen tanto las claves que elige el usuario como las que genera PasswordGenerator.
     */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-zA-Z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MENSAJE =
        "La contraseña debe tener al menos 8 caracteres, incluir letras y números, y al menos una mayúscula";
}
