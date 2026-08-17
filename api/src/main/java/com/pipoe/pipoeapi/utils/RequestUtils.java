package com.pipoe.pipoeapi.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * De dónde viene un pedido.
 *
 * `X-Forwarded-For` lo puede mandar **cualquiera**: es una cabecera HTTP común y corriente. Si
 * se le cree a ciegas, quien quiera adivinar contraseñas manda una IP distinta en cada intento
 * y el bloqueo por intentos fallidos no se activa nunca.
 *
 * Por eso sólo se lee cuando `security.rate-limit.trust-proxy` está en `true`, que hay que
 * activar a mano al desplegar detrás de un proxy que **pise** esa cabecera con la IP real de
 * la conexión (`proxy_set_header X-Forwarded-For $remote_addr;` en nginx). Corriendo sin
 * proxy —como en desarrollo—, la cabecera se ignora y se usa la IP del socket, que no se
 * puede falsificar.
 */
@Component
public class RequestUtils {

    @Value("${security.rate-limit.trust-proxy:false}")
    private boolean confiarEnProxy;

    public String clientIp(HttpServletRequest request) {
        if (!confiarEnProxy) return request.getRemoteAddr();

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();

        return request.getRemoteAddr();
    }
}
