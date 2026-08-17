package com.pipoe.pipoeapi.parametros.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pipoe.pipoeapi.parametros.dtos.ConfiguracionResponse;
import com.pipoe.pipoeapi.parametros.dtos.request.ActualizarParametrosRequest;
import com.pipoe.pipoeapi.parametros.service.ParametrosService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
public class ParametrosController {

    private final ParametrosService parametrosService;

    /**
     * Sólo de lectura y para cualquier sesión: son límites de UI, no datos sensibles.
     * La API los sigue aplicando por su cuenta al crear proyectos y colaboradores.
     */
    @GetMapping
    public ResponseEntity<ConfiguracionResponse> configuracion() {
        return ResponseEntity.ok(ConfiguracionResponse.from(parametrosService.getConfiguracion()));
    }

    /** Los cupos los fija la dueña de la aplicación desde su pantalla de ajustes. */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionResponse> actualizar(
        @Valid @RequestBody ActualizarParametrosRequest request
    ) {
        return ResponseEntity.ok(
            ConfiguracionResponse.from(parametrosService.actualizarConfiguracion(request.toEntity()))
        );
    }
}
