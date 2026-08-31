package com.pipoe.pipoeapi.dominio.proyectos.controller;

import static com.pipoe.pipoeapi.parametros.Constantes.PAGE_SIZE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.proyectos.dtos.AdminProyectoResponse;
import com.pipoe.pipoeapi.dominio.proyectos.services.ProyectoAdminService;

import lombok.RequiredArgsConstructor;

/**
 * Todos los proyectos del sistema, para la administradora.
 *
 * Ruta propia bajo /api/admin y no un parámetro más de /api/proyectos: son dos preguntas
 * distintas —"mis proyectos" y "todos los proyectos"— y mezclarlas en un endpoint dejaría el
 * alcance de la respuesta dependiendo de un query param, que es la clase de detalle que después
 * se filtra mal.
 */
@RestController
@RequestMapping("/api/admin/proyectos")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProyectoAdminController {

    private final ProyectoAdminService proyectoAdminService;

    /**
     * `texto` busca en el nombre del proyecto, su código y el nombre del dueño.
     * `terminado` sin mandar trae todos; true, sólo los cerrados; false, los que siguen en curso.
     */
    @GetMapping
    public ResponseEntity<Page<AdminProyectoResponse>> listar(
        @RequestParam(required = false) String texto,
        @RequestParam(required = false) Boolean terminado,
        @RequestParam(defaultValue = "0") int page
    ) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("nombre").ascending());
        return ResponseEntity.ok(proyectoAdminService.listar(texto, terminado, pageable));
    }
}
