package com.pipoe.pipoeapi.dominio.colaboradores.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.colaboradores.dtos.ColaboradorResponse;
import com.pipoe.pipoeapi.dominio.colaboradores.dtos.request.ActualizarPermisosRequest;
import com.pipoe.pipoeapi.dominio.colaboradores.dtos.request.RegisterColaboradorRequest;
import com.pipoe.pipoeapi.dominio.colaboradores.services.ColaboradorService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Los colaboradores los gestiona el dueño del proyecto, que siempre es un USER. */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}/colaboradores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ColaboradorController {

    private final ColaboradorService colaboradorService;

    @GetMapping
    public ResponseEntity<List<ColaboradorResponse>> listar(
        @PathVariable Long proyectoId,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(colaboradorService.listar(proyectoId, usuario));
    }

    @PostMapping
    public ResponseEntity<ColaboradorResponse> create(
        @PathVariable Long proyectoId,
        @Valid @RequestBody RegisterColaboradorRequest request,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(colaboradorService.create(proyectoId, request, usuario));
    }

    @PutMapping("/{colaboradorId}/permisos")
    public ResponseEntity<ColaboradorResponse> actualizarPermisos(
        @PathVariable Long proyectoId,
        @PathVariable Long colaboradorId,
        @Valid @RequestBody ActualizarPermisosRequest request,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(
            colaboradorService.actualizarPermisos(proyectoId, colaboradorId, request, usuario)
        );
    }

    /** Baja lógica: el colaborador deja de poder entrar, pero su fila sobrevive. */
    @DeleteMapping("/{colaboradorId}")
    public ResponseEntity<Void> eliminar(
        @PathVariable Long proyectoId,
        @PathVariable Long colaboradorId,
        @AuthenticationPrincipal Usuario usuario
    ) {
        colaboradorService.eliminar(proyectoId, colaboradorId, usuario);
        return ResponseEntity.noContent().build();
    }
}
