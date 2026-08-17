package com.pipoe.pipoeapi.dominio.landing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.landing.dtos.TextoLandingResponse;
import com.pipoe.pipoeapi.dominio.landing.dtos.request.ActualizarTextoRequest;
import com.pipoe.pipoeapi.dominio.landing.entities.ClaveTexto;
import com.pipoe.pipoeapi.dominio.landing.services.TextoLandingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Los textos de la portada. El GET es público —lo consume la portada misma— y la edición es
 * de ADMIN, que es la cuenta desde la que Arlette mantiene el contenido del sitio.
 */
@RestController
@RequestMapping("/api/landing/textos")
@RequiredArgsConstructor
public class TextoLandingController {

    private final TextoLandingService textoLandingService;

    @GetMapping
    public ResponseEntity<List<TextoLandingResponse>> listar() {
        return ResponseEntity.ok(textoLandingService.listar());
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TextoLandingResponse> actualizar(
        @PathVariable ClaveTexto clave,
        @Valid @RequestBody ActualizarTextoRequest request
    ) {
        return ResponseEntity.ok(textoLandingService.actualizar(clave, request));
    }
}
