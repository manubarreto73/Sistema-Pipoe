package com.pipoe.pipoeapi.exceptions.handlers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.TransientObjectException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.TooManyRequestsException;
import com.pipoe.pipoeapi.exceptions.exceptions.ConflictException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(401, "Credenciales inválidas", LocalDateTime.now()));
    }

    /**
     * Un 403 aislado es normal —una pantalla que pide algo que esa sesión no puede—, pero
     * muchos seguidos son alguien tanteando qué puede tocar. Queda registrado para poder verlo.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("SEGURIDAD acceso_denegado motivo={}", e.getMessage());

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, "Acción no autorizada", LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        e.getBindingResult().getFieldErrors()
            .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, errores, LocalDateTime.now()));
    }

    /**
     * Body ilegible o imposible de mapear: JSON mal formado, o un valor que no existe en un
     * enum (por ejemplo un nivel de permiso inventado). Falla antes de llegar a @Valid, así
     * que sin esto caía en el handler genérico y devolvía 500 por un error del cliente.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBodyIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, "El cuerpo de la petición no es válido", LocalDateTime.now()));
    }

    /**
     * Un valor de la URL que no se puede convertir al tipo esperado: una fase o una clave de
     * texto que no existen. Es un pedido mal armado, no una falla del servidor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(
                400, "Valor no válido para '" + e.getName() + "'", LocalDateTime.now()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(400, e.getMessage(), LocalDateTime.now()));
    }

    /**
     * Método equivocado sobre una ruta que existe. Sin este manejador caía en el catch-all y
     * salía como 500, o sea "algo se rompió del lado del servidor" cuando en realidad el
     * pedido estaba mal formado. Cuesta un rato largo de depuración averiguar eso.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(new ErrorResponse(405, "Método no permitido en esta ruta", LocalDateTime.now()));
    }

    /**
     * Freno de ritmo. El 429 es contrato con el frontend, que lo muestra tal cual en vez de
     * pedirle a la persona que revise lo que escribió: no hay nada mal en los datos.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse(429, e.getMessage(), LocalDateTime.now()));
    }

    /**
     * Dos personas guardaron el mismo documento sobre la misma base. El 409 es contrato con
     * el frontend: es lo que dispara el aviso de "recarga para ver la última versión".
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(409, e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(TransientObjectException.class)
    public ResponseEntity<ErrorResponse> handleTransientObject(TransientObjectException e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "Error interno del servidor", LocalDateTime.now()));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleRedisFailure(RedisConnectionFailureException e) {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ErrorResponse(503, "Servicio de sesión no disponible", LocalDateTime.now()));
    }

    /**
     * Al cliente se le responde siempre lo mismo, sin detalles: un mensaje de excepción puede
     * revelar rutas, consultas o versiones de librerías. Pero **al log va todo**, con la traza
     * completa: sin eso, un 500 en producción es indiagnosticable y además ningún detector de
     * ataques tiene con qué darse cuenta de que algo raro está pasando.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e) {
        log.error("Error no controlado", e);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "Error interno del servidor", LocalDateTime.now()));
    }
}
