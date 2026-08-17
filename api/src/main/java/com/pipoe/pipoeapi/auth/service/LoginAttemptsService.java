package com.pipoe.pipoeapi.auth.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pipoe.pipoeapi.redis.RedisKeys;
import com.pipoe.pipoeapi.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Freno a los intentos de adivinar contraseñas.
 *
 * Cuenta por dos claves a la vez, porque frenan ataques distintos:
 *
 * - **Por IP**: alguien probando muchas contraseñas contra muchas cuentas desde un lugar.
 * - **Por cuenta**: alguien probando la misma lista de contraseñas filtradas contra una cuenta
 *   concreta desde muchas IPs. El contador por IP no ve nada de eso, porque cada IP hace un
 *   intento solo. Es el ataque más común contra un sitio público.
 *
 * El de cuenta es más tolerante que el de IP: bloquear una cuenta es algo que un atacante puede
 * usar para dejar afuera a su dueño, así que conviene que cueste más llegar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptsService {

    @Value("${security.rate-limit.max-login-attempts:5}")
    private int maxIntentos;

    @Value("${security.rate-limit.max-login-attempts-cuenta:10}")
    private int maxIntentosCuenta;

    @Value("${security.rate-limit.attempts-window-minutes:5}")
    private int ventanaMinutos;

    @Value("${security.rate-limit.block-duration-minutes:30}")
    private int bloqueoMinutos;

    private final RedisService redisService;

    /** Registra un intento fallido, tanto contra la IP como contra la cuenta apuntada. */
    public void registrarIntento(String ip, String cuenta) {
        contarYBloquear(RedisKeys.login_attempts + ":ip:" + ip, claveBloqueoIp(ip),
            maxIntentos, "ip", ip);

        if (cuenta != null && !cuenta.isBlank())
            contarYBloquear(RedisKeys.login_attempts + ":cuenta:" + normalizar(cuenta),
                claveBloqueoCuenta(cuenta), maxIntentosCuenta, "cuenta", normalizar(cuenta));
    }

    public void limpiarIntentos(String ip, String cuenta) {
        redisService.delete(RedisKeys.login_attempts + ":ip:" + ip);

        if (cuenta != null && !cuenta.isBlank())
            redisService.delete(RedisKeys.login_attempts + ":cuenta:" + normalizar(cuenta));
    }

    public boolean estaBloqueada(String ip) {
        return existe(claveBloqueoIp(ip));
    }

    public boolean cuentaBloqueada(String cuenta) {
        return cuenta != null && !cuenta.isBlank() && existe(claveBloqueoCuenta(cuenta));
    }

    private void contarYBloquear(String claveContador, String claveBloqueo, int tope,
                                 String tipo, String valor) {
        Long intentos = redisService.increment(claveContador, ventanaMinutos);

        if (intentos != null && intentos >= tope) {
            redisService.set(claveBloqueo, "1", bloqueoMinutos);
            // Este renglón es la señal que después mira un fail2ban o una alerta.
            log.warn("SEGURIDAD bloqueo={} valor={} intentos={} minutos={}",
                tipo, valor, intentos, bloqueoMinutos);
        }
    }

    private boolean existe(String clave) {
        return Boolean.TRUE.equals(redisService.exists(clave));
    }

    private String claveBloqueoIp(String ip) {
        return RedisKeys.blocked_ips + ":" + ip;
    }

    private String claveBloqueoCuenta(String cuenta) {
        return RedisKeys.blocked_cuentas + ":" + normalizar(cuenta);
    }

    /** El email no distingue mayúsculas: sin esto, alternarlas evadiría el contador. */
    private String normalizar(String cuenta) {
        return cuenta.trim().toLowerCase(Locale.ROOT);
    }
}
