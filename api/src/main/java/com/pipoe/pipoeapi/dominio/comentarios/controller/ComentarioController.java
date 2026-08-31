package com.pipoe.pipoeapi.dominio.comentarios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.comentarios.dtos.ComentarioResponse;
import com.pipoe.pipoeapi.dominio.comentarios.dtos.request.RegisterComentarioRequest;
import com.pipoe.pipoeapi.dominio.comentarios.services.ComentarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Los comentarios de un paso. Sin @PreAuthorize de clase, por lo mismo que PipoeController:
 * acá entran el dueño, los colaboradores y la administradora, y quién puede qué lo resuelve
 * AccesoFaseService fase por fase.
 *
 * Cuelga de la ruta del paso —y no de una ruta propia con el id del documento— porque el
 * documento es un detalle interno: se materializa solo y el front nunca conoce su id.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/pasos/{pasoId}/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    @GetMapping
    public ResponseEntity<List<ComentarioResponse>> listar(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(comentarioService.listar(proyectoId, pasoId, principal));
    }

    @PostMapping
    public ResponseEntity<ComentarioResponse> crear(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @Valid @RequestBody RegisterComentarioRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(comentarioService.crear(proyectoId, pasoId, request, principal));
    }

    @DeleteMapping("/{comentarioId}")
    public ResponseEntity<Void> eliminar(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @PathVariable Long comentarioId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        comentarioService.eliminar(proyectoId, pasoId, comentarioId, principal);
        return ResponseEntity.noContent().build();
    }
}
