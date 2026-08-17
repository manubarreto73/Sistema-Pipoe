package com.pipoe.pipoeapi.redis;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void set(String key, String value, long ttlMinutos) {
        redisTemplate.opsForValue().set(key, value, ttlMinutos, TimeUnit.MINUTES);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    public Long increment(String key, long ttlMinutos) {
        Long intentos = redisTemplate.opsForValue().increment(key);
        if (intentos == 1)
            redisTemplate.expire(key, ttlMinutos, TimeUnit.MINUTES);
        return intentos;
    }

    public String getAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    /**
     * Hash con vencimiento. Sirve para lo que necesita varios valores bajo una misma clave,
     * como la presencia (quiénes tienen abierto un paso), sin recurrir a KEYS con patrón,
     * que recorre todo el keyspace.
     */
    public void hashSet(String key, String field, String value, long ttlMinutos) {
        redisTemplate.opsForHash().put(key, field, value);
        redisTemplate.expire(key, ttlMinutos, TimeUnit.MINUTES);
    }

    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Conjunto con vencimiento. Sirve para agrupar claves que después hay que borrar juntas
     * —los refresh token de una misma sesión—, sin recurrir a KEYS con patrón.
     */
    public void setAdd(String key, String value, long ttlMinutos) {
        redisTemplate.opsForSet().add(key, value);
        redisTemplate.expire(key, ttlMinutos, TimeUnit.MINUTES);
    }

    public Set<String> setMembers(String key) {
        Set<String> miembros = redisTemplate.opsForSet().members(key);
        return miembros == null ? Set.of() : miembros;
    }
}
