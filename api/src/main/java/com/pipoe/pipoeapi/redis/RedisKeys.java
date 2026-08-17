package com.pipoe.pipoeapi.redis;

public enum RedisKeys {
    reset_password,
    login_attempts,
    solicitudes_acceso,
    blocked_ips,
    /** Bloqueo por cuenta: frena el ataque distribuido que el contador por IP no ve. */
    blocked_cuentas,
    refresh,
    /** Refresh token ya consumido. Que aparezca otra vez significa que alguien lo robó. */
    refresh_usado,
    blacklist,
}
