package com.pipoe.pipoeapi.dominio.proyectos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.proyectos.dtos.ProyectoResponse;
import com.pipoe.pipoeapi.dominio.proyectos.dtos.request.RegisterProyectoRequest;
import com.pipoe.pipoeapi.dominio.proyectos.services.ProyectoService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proyectos")
@RequiredArgsConstructor
public class ProyectoController {

    private final ProyectoService proyectoService;

    // Sólo USER: la cuenta de ADMIN existe para gestionar solicitudes, no maneja proyectos.
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ProyectoResponse>> listar(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(proyectoService.listarDelUsuario(usuario));
    }

    // Sin @PreAuthorize: el colaborador también entra acá, pero sólo a su propio proyecto.
    @GetMapping("/{id}")
    public ResponseEntity<ProyectoResponse> detalle(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(proyectoService.getDetalle(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProyectoResponse> create(
        @Valid @RequestBody RegisterProyectoRequest request,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proyectoService.create(request, usuario));
    }

    /** Cambiar el nombre. Sigue siendo único en toda la app. */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProyectoResponse> renombrar(
        @PathVariable Long id,
        @Valid @RequestBody RegisterProyectoRequest request,
        @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(proyectoService.renombrar(id, request, usuario));
    }

    /** Borrado físico: se lleva puestos a los colaboradores del proyecto. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> eliminar(
        @PathVariable Long id,
        @AuthenticationPrincipal Usuario usuario
    ) {
        proyectoService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}
