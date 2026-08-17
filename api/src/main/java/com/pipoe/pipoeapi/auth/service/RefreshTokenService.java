package com.pipoe.pipoeapi.auth.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.redis.RedisKeys;
import com.pipoe.pipoeapi.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Los refresh token, con rotación y **detección de reuso**.
 *
 * Cada token se usa una sola vez: al canjearlo se emite uno nuevo y el viejo queda marcado como
 * consumido. Que un token consumido vuelva a aparecer sólo puede significar una cosa —alguien
 * tiene una copia que no debería—, así que en ese caso se cierran todas las sesiones de esa
 * persona y se la obliga a volver a autenticarse.
 *
 * Es la recomendación del OAuth 2.0 Security BCP y es la única forma que tiene el sistema de
 * enterarse de un robo de sesión: sin esto, el ladrón usa el token, el dueño legítimo queda
 * deslogueado sin entender por qué, y nadie se entera de nada.
 *
 * Todos los tokens de una misma sesión comparten una **familia**, que es lo que permite
 * revocarlos juntos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    @Value("${security.jwt.refresh-expiration-days:7}")
    private int refreshExpirationDays;

    /**
     * Cuánto se recuerda un token ya canjeado. Tiene que cubrir de sobra la vida del token que
     * lo reemplazó, o el reuso pasaría desapercibido por olvido.
     */
    private static final long MEMORIA_USADOS_MINUTOS = 60L * 24 * 30;

    private final RedisService redisService;

    public String create(String username) {
        return emitir(username, UUID.randomUUID().toString());
    }

    public String validateAndRotate(String token, String newToken) {
        String valor = redisService.getAndDelete(key(token));

        if (valor == null) {
            detectarReuso(token);
            throw new BusinessException("Refresh token inválido o expirado");
        }

        Sesion sesion = Sesion.parse(valor);

        // Se recuerda que este token ya fue canjeado, con su familia, para reconocerlo si
        // vuelve a aparecer.
        redisService.set(usado(token), sesion.familia(), MEMORIA_USADOS_MINUTOS);
        redisService.set(key(newToken), sesion.serializar(), ttlMinutes());
        // El nuevo token queda registrado en su familia, para poder revocarla entera.
        redisService.setAdd(familia(sesion.familia()), newToken, ttlMinutes());

        return sesion.subject();
    }

    public void revoke(String token) {
        String valor = redisService.getAndDelete(key(token));
        if (valor != null) revocarFamilia(Sesion.parse(valor).familia());
    }

    // ------------------------------------------------------------------ interno

    private String emitir(String subject, String familia) {
        String token = UUID.randomUUID().toString();

        redisService.set(key(token), new Sesion(subject, familia).serializar(), ttlMinutes());
        redisService.setAdd(familia(familia), token, ttlMinutes());

        return token;
    }

    /**
     * Llegó un token que no está vigente. Si además figura entre los ya canjeados, no es un
     * token vencido: es una copia en manos de alguien más.
     */
    private void detectarReuso(String token) {
        String familia = redisService.get(usado(token));
        if (familia == null) return;

        log.warn("SEGURIDAD reuso_refresh_token familia={} — se revocan todas sus sesiones",
            familia);

        revocarFamilia(familia);
    }

    private void revocarFamilia(String familia) {
        for (String token : redisService.setMembers(familia(familia)))
            redisService.delete(key(token));

        redisService.delete(familia(familia));
    }

    /** Lo que se guarda contra el token: a quién pertenece y de qué sesión viene. */
    private record Sesion(String subject, String familia) {
        String serializar() {
            return familia + "|" + subject;
        }

        static Sesion parse(String valor) {
            int corte = valor.indexOf('|');
            // Sin separador: es un token emitido antes de que existieran las familias. Se le
            // arma una propia para que el resto del flujo funcione igual.
            if (corte < 0) return new Sesion(valor, UUID.randomUUID().toString());

            return new Sesion(valor.substring(corte + 1), valor.substring(0, corte));
        }
    }

    private String key(String token) {
        return RedisKeys.refresh + ":" + token;
    }

    private String usado(String token) {
        return RedisKeys.refresh_usado + ":" + token;
    }

    /** Conjunto con todos los token vivos de una misma sesión. */
    private String familia(String familia) {
        return RedisKeys.refresh + ":familia:" + familia;
    }

    private long ttlMinutes() {
        return (long) refreshExpirationDays * 24 * 60;
    }
}
