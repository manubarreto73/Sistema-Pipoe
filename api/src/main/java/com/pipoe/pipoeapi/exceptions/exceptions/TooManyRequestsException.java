package com.pipoe.pipoeapi.exceptions.exceptions;

/**
 * El cliente chocó contra un freno de ritmo: intentos de login o solicitudes de acceso.
 *
 * Va aparte de BusinessException porque el código importa. Un 400 le dice al frontend "lo que
 * mandaste está mal" y quien lo lee corrige los datos y vuelve a probar, que es exactamente lo
 * que no hay que hacer acá: no hay nada que corregir, hay que esperar. El 429 es el código que
 * significa eso y el único que un cliente automático interpreta bien.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
