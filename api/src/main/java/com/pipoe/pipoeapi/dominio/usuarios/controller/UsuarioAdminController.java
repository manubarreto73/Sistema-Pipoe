package com.pipoe.pipoeapi.dominio.usuarios.controller;

import static com.pipoe.pipoeapi.parametros.Constantes.PAGE_SIZE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.usuarios.dtos.UsuarioAdminResponse;
import com.pipoe.pipoeapi.dominio.usuarios.dtos.request.CambiarActivoRequest;
import com.pipoe.pipoeapi.dominio.usuarios.dtos.request.RegisterAdminRequest;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.dominio.usuarios.service.UsuarioAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El padrón de cuentas del sistema.
 *
 * Bajo /api/admin, como el listado de proyectos: son las rutas que sólo existen para quien
 * administra, y tenerlas juntas hace evidente qué superficie hay que proteger.
 */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    /**
     * `texto` busca en el nombre y en el email. `activo` y `role` sin mandar no filtran.
     *
     * Ordenado por nombre: es como se busca a una persona cuando ya se sabe a quién se busca.
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioAdminResponse>> listar(
        @RequestParam(required = false) String texto,
        @RequestParam(required = false) Boolean activo,
        @RequestParam(required = false) Role role,
        @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("nombreCompleto").ascending());
        return ResponseEntity.ok(usuarioAdminService.listar(texto, activo, role, pageable));
    }

    /** Otra cuenta de administración. Recibe su contraseña por mail, igual que cualquier alta. */
    @PostMapping("/administradores")
    public ResponseEntity<UsuarioAdminResponse> crearAdmin(
        @Valid @RequestBody RegisterAdminRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(usuarioAdminService.crearAdmin(request));
    }

    /**
     * Baja y alta lógica. PUT y no DELETE a propósito: no se borra nada, se cambia un estado, y
     * la misma ruta sirve para las dos direcciones.
     */
    @PutMapping("/{id}/activo")
    public ResponseEntity<UsuarioAdminResponse> cambiarActivo(
        @PathVariable Long id,
        @Valid @RequestBody CambiarActivoRequest request,
        @AuthenticationPrincipal Usuario solicitante
    ) {
        return ResponseEntity.ok(
            usuarioAdminService.cambiarActivo(id, request.getActivo(), solicitante)
        );
    }
}
