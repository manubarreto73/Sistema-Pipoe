package com.pipoe.pipoeapi.dominio.pasos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.pasos.dtos.PasoCatalogoResponse;
import com.pipoe.pipoeapi.dominio.pasos.dtos.request.ActualizarPasoRequest;
import com.pipoe.pipoeapi.dominio.pasos.services.PasoCatalogoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El catálogo del modelo PipoE. Lo edita el ADMIN: es contenido de la metodología, no de un
 * proyecto. Los usuarios lo leen a través del detalle de cada paso.
 */
@RestController
@RequestMapping("/api/catalogo/pasos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PasoCatalogoController {

    private final PasoCatalogoService pasoCatalogoService;

    @GetMapping
    public ResponseEntity<List<PasoCatalogoResponse>> listar() {
        return ResponseEntity.ok(pasoCatalogoService.listar());
    }

    @PutMapping("/{pasoId}")
    public ResponseEntity<PasoCatalogoResponse> actualizar(
        @PathVariable Long pasoId,
        @Valid @RequestBody ActualizarPasoRequest request
    ) {
        return ResponseEntity.ok(pasoCatalogoService.actualizar(pasoId, request));
    }
}
