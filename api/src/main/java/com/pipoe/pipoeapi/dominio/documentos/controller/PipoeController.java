package com.pipoe.pipoeapi.dominio.documentos.controller;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.dominio.documentos.dtos.FaseResumenResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.PasoDetalleResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.PasoResumenResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.VersionDiffResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.VersionResponse;
import com.pipoe.pipoeapi.dominio.documentos.dtos.request.CompletarPasoRequest;
import com.pipoe.pipoeapi.dominio.documentos.dtos.request.GuardarDocumentoRequest;
import com.pipoe.pipoeapi.dominio.documentos.services.DocumentoService;
import com.pipoe.pipoeapi.dominio.documentos.services.ExportacionService;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * El trabajo dentro de un proyecto. Sin @PreAuthorize a nivel clase: acá entran tanto el dueño
 * como los colaboradores, y quién puede qué lo resuelve AccesoFaseService fase por fase.
 */
@RestController
@RequestMapping("/api/proyectos/{proyectoId}")
@RequiredArgsConstructor
public class PipoeController {

    private final DocumentoService documentoService;

    /** Las 5 fases con su progreso. Una sola request para pintar toda la navegación. */
    @GetMapping("/fases")
    public ResponseEntity<List<FaseResumenResponse>> fases(
        @PathVariable Long proyectoId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(documentoService.resumenFases(proyectoId, principal));
    }

    /** Los pasos de una fase, en orden, con el estado de cada uno. */
    @GetMapping("/fases/{fase}/pasos")
    public ResponseEntity<List<PasoResumenResponse>> pasos(
        @PathVariable Long proyectoId,
        @PathVariable Fase fase,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(documentoService.pasosDeFase(proyectoId, fase, principal));
    }

    @GetMapping("/pasos/{pasoId}")
    public ResponseEntity<PasoDetalleResponse> paso(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(documentoService.detalle(proyectoId, pasoId, principal));
    }

    /** Guardado del documento. 409 si alguien más guardó mientras tanto. */
    @PutMapping("/pasos/{pasoId}/documento")
    public ResponseEntity<PasoDetalleResponse> guardar(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @Valid @RequestBody GuardarDocumentoRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(documentoService.guardar(proyectoId, pasoId, request, principal));
    }

    @PutMapping("/pasos/{pasoId}/completado")
    public ResponseEntity<PasoDetalleResponse> completar(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @Valid @RequestBody CompletarPasoRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(
            documentoService.completar(proyectoId, pasoId, request.getCompletado(), principal)
        );
    }

    /** Historial de guardados. Sólo para quien creó el proyecto. */
    @GetMapping("/pasos/{pasoId}/versiones")
    public ResponseEntity<List<VersionResponse>> versiones(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(documentoService.historial(proyectoId, pasoId, principal));
    }

    /** Qué agregó y qué quitó ese guardado respecto del anterior. */
    @GetMapping("/pasos/{pasoId}/versiones/{versionId}")
    public ResponseEntity<VersionDiffResponse> diferencias(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @PathVariable Long versionId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        return ResponseEntity.ok(
            documentoService.diferencias(proyectoId, pasoId, versionId, principal)
        );
    }

    /** Descarga del producto de una fase. `formato` es docx o pdf. */
    @GetMapping("/pasos/{pasoId}/exportar")
    public ResponseEntity<byte[]> exportar(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @RequestParam(defaultValue = "pdf") String formato,
        @AuthenticationPrincipal UserDetails principal
    ) {
        ExportacionService.Archivo archivo =
            documentoService.exportar(proyectoId, pasoId, formato, principal);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(archivo.contentType()))
            // El builder se encarga de codificar los acentos del nombre según la RFC 6266.
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(archivo.nombre(), StandardCharsets.UTF_8)
                .build()
                .toString())
            // Sin esto el navegador no ve el header y la descarga sale con un nombre inventado.
            .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
            .body(archivo.contenido());
    }

    /**
     * Latido de presencia mientras alguien tiene el documento abierto para editar. Devuelve el
     * nombre de otra persona editando el mismo paso, o null.
     */
    @PostMapping("/pasos/{pasoId}/presencia")
    public ResponseEntity<Map<String, String>> presencia(
        @PathVariable Long proyectoId,
        @PathVariable Long pasoId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        String otro = documentoService.registrarPresencia(proyectoId, pasoId, principal);
        // singletonMap y no Map.of: el valor es null cuando no hay nadie más y Map.of no lo admite.
        return ResponseEntity.ok(Collections.singletonMap("editandoOtro", otro));
    }
}
