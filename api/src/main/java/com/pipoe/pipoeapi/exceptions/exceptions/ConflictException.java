package com.pipoe.pipoeapi.exceptions.exceptions;

/** Choque de escrituras concurrentes: el cliente guardó sobre una versión que ya quedó vieja. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
