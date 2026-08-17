package com.pipoe.pipoeapi.dominio.solicitudes.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.solicitudes.dtos.SolicitudAccesoResponse;
import com.pipoe.pipoeapi.dominio.solicitudes.dtos.request.RegisterSolicitudAccesoRequest;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.EstadoSolicitud;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.SolicitudAcceso;
import com.pipoe.pipoeapi.dominio.solicitudes.repositories.SolicitudAccesoRepository;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.service.UsuarioService;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;
import com.pipoe.pipoeapi.redis.RedisKeys;
import com.pipoe.pipoeapi.redis.RedisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolicitudAccesoService {

    private final SolicitudAccesoRepository solicitudAccesoRepository;
    private final UsuarioService usuarioService;
    private final RedisService redisService;

    @Value("${security.rate-limit.max-solicitudes-acceso:3}")
    private int maxSolicitudesPorIp;

    @Value("${security.rate-limit.solicitudes-window-minutes:60}")
    private int ventanaSolicitudesMinutos;

    public SolicitudAcceso findById(Long id) {
        return solicitudAccesoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con id: " + id));
    }

    /**
     * `desde` y `hasta` son días, no instantes: se traducen a "desde las 00:00 de ese día" y
     * "antes de las 00:00 del día siguiente", para que filtrar por un mismo día en los dos
     * extremos devuelva las solicitudes de ese día y no un rango vacío.
     */
    public Page<SolicitudAccesoResponse> getAll(
        EstadoSolicitud estado, String texto, LocalDate desde, LocalDate hasta, Pageable pageable
    ) {
        return solicitudAccesoRepository.buscar(
            estado,
            patron(texto),
            desde == null ? null : desde.atStartOfDay(),
            hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
            pageable
        ).map(SolicitudAccesoResponse::from);
    }

    /**
     * Texto de búsqueda a patrón LIKE.
     *
     * Los comodines que escriba la persona se **descartan** en vez de escaparse: Hibernate emite
     * el LIKE con `ESCAPE ''`, o sea sin carácter de escape, así que escaparlos no funcionaría.
     * Y buscar un `%` literal en un nombre propio no es un caso real.
     */
    private String patron(String texto) {
        if (texto == null || texto.isBlank()) return null;

        String limpio = texto.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[%_\\\\]", "");

        return limpio.isEmpty() ? null : "%" + limpio + "%";
    }

    @Transactional
    public SolicitudAccesoResponse create(RegisterSolicitudAccesoRequest request, String ip) {
        validarRateLimit(ip);

        // Mismo mensaje para "ya pediste" y "ya sos usuario": no le decimos al visitante
        // en cuál de los dos casos cayó, pero sí le damos una respuesta útil.
        if (solicitudAccesoRepository.existsByEmailAndEstado(request.getEmail(), EstadoSolicitud.PENDIENTE)
            || usuarioService.existsByEmail(request.getEmail()))
            throw new BusinessException("Ya recibimos una solicitud con ese email. Te vamos a contactar.");

        SolicitudAcceso solicitud = request.toEntity();

        solicitudAccesoRepository.save(solicitud);
        return SolicitudAccesoResponse.from(solicitud);
    }

    /** Crea el Usuario y le manda la clave generada por mail (lo hace UsuarioService.register). */
    @Transactional
    public SolicitudAccesoResponse aprobar(Long id) {
        SolicitudAcceso solicitud = findById(id);
        validarPendiente(solicitud);

        usuarioService.register(solicitud.getEmail(), solicitud.nombreCompleto(), Role.USER);

        return resolver(solicitud, EstadoSolicitud.APROBADA);
    }

    /** El rechazo es silencioso: no se le manda ningún mail al solicitante. */
    @Transactional
    public SolicitudAccesoResponse rechazar(Long id) {
        SolicitudAcceso solicitud = findById(id);
        validarPendiente(solicitud);

        return resolver(solicitud, EstadoSolicitud.RECHAZADA);
    }

    private SolicitudAccesoResponse resolver(SolicitudAcceso solicitud, EstadoSolicitud estado) {
        solicitud.setEstado(estado);
        solicitud.setFechaResolucion(LocalDateTime.now());

        solicitudAccesoRepository.save(solicitud);
        return SolicitudAccesoResponse.from(solicitud);
    }

    private void validarPendiente(SolicitudAcceso solicitud) {
        if (solicitud.getEstado() != EstadoSolicitud.PENDIENTE)
            throw new BusinessException("La solicitud ya fue resuelta");
    }

    private void validarRateLimit(String ip) {
        Long enviadas = redisService.increment(
            RedisKeys.solicitudes_acceso + ":" + ip, ventanaSolicitudesMinutos
        );

        if (enviadas != null && enviadas > maxSolicitudesPorIp)
            throw new BusinessException("Demasiadas solicitudes desde esta conexión. Probá de nuevo más tarde.");
    }
}
