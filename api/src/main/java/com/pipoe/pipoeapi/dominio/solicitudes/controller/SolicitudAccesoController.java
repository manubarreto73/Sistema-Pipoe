package com.pipoe.pipoeapi.dominio.solicitudes.controller;

import static com.pipoe.pipoeapi.parametros.Constantes.PAGE_SIZE;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.solicitudes.dtos.SolicitudAccesoResponse;
import com.pipoe.pipoeapi.dominio.solicitudes.dtos.request.RegisterSolicitudAccesoRequest;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.EstadoSolicitud;
import com.pipoe.pipoeapi.dominio.solicitudes.services.SolicitudAccesoService;
import com.pipoe.pipoeapi.utils.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/solicitudes-acceso")
@RequiredArgsConstructor
public class SolicitudAccesoController {

    private final SolicitudAccesoService solicitudAccesoService;
    private final RequestUtils requestUtils;

    /** Público (ver SecurityConfig). Rate limit por IP en el servicio. */
    @PostMapping
    public ResponseEntity<SolicitudAccesoResponse> create(
        @Valid @RequestBody RegisterSolicitudAccesoRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(solicitudAccesoService.create(request, requestUtils.clientIp(httpRequest)));
    }

    /** `texto` busca en nombre, apellidos, email e institución. `desde` y `hasta` son días. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<SolicitudAccesoResponse>> getAll(
        @RequestParam(required = false) EstadoSolicitud estado,
        @RequestParam(required = false) String texto,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("fechaSolicitud").descending());
        return ResponseEntity.ok(
            solicitudAccesoService.getAll(estado, texto, desde, hasta, pageable)
        );
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitudAccesoResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudAccesoService.aprobar(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SolicitudAccesoResponse> rechazar(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudAccesoService.rechazar(id));
    }
}
