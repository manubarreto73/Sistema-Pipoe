package com.pipoe.pipoeapi.dominio.comentarios.services;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.comentarios.dtos.ComentarioResponse;
import com.pipoe.pipoeapi.dominio.comentarios.dtos.request.RegisterComentarioRequest;
import com.pipoe.pipoeapi.dominio.comentarios.entities.Comentario;
import com.pipoe.pipoeapi.dominio.comentarios.repositories.ComentarioRepository;
import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;
import com.pipoe.pipoeapi.dominio.documentos.repositories.DocumentoRepository;
import com.pipoe.pipoeapi.dominio.documentos.services.AccesoFaseService;
import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;
import com.pipoe.pipoeapi.dominio.pasos.repositories.PasoRepository;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.proyectos.services.ProyectoService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Comentarios sobre el documento de un paso.
 *
 * Quién puede qué lo decide AccesoFaseService, igual que para el documento:
 *
 * - **Leer** los comentarios viene con poder leer el paso. No hace falta permiso aparte: son
 *   parte de lo que hay que ver para trabajar sobre el documento.
 * - **Escribir** pide nivel COMENTARIOS o EDICION en la fase.
 * - **Borrar** es tarea de quien administra, no de quien comenta: la dueña del proyecto sobre
 *   los de su proyecto, y la administración del sistema sobre cualquiera.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final DocumentoRepository documentoRepository;
    private final PasoRepository pasoRepository;
    private final AccesoFaseService accesoFaseService;
    private final ProyectoService proyectoService;

    public List<ComentarioResponse> listar(Long proyectoId, Long pasoId, UserDetails principal) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        // Basta con poder ver la fase; nivel() ya rechaza a quien no tiene acceso al proyecto.
        accesoFaseService.nivel(proyectoId, paso.getFase(), principal);

        // Sin documento no hubo comentarios: nadie llegó a abrir el paso todavía.
        return documentoRepository.findByProyectoIdAndPasoId(proyecto.getId(), pasoId)
            .map(documento -> comentarioRepository.findByDocumentoOrderByCreadoEnDesc(documento)
                .stream()
                .map(comentario -> ComentarioResponse.from(comentario, puedeBorrar(proyecto, principal)))
                .toList())
            .orElseGet(List::of);
    }

    @Transactional
    public ComentarioResponse crear(
        Long proyectoId, Long pasoId, RegisterComentarioRequest request, UserDetails principal
    ) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.exigirComentarios(proyectoId, paso.getFase(), principal);

        // Se puede comentar un paso que nadie escribió todavía: el documento se materializa acá
        // igual que cuando alguien lo abre por primera vez.
        Documento documento = documentoRepository.findByProyectoIdAndPasoId(proyecto.getId(), pasoId)
            .orElseGet(() -> documentoRepository.save(Documento.builder()
                .proyecto(proyecto)
                .paso(paso)
                .build()));

        Comentario comentario = comentarioRepository.save(Comentario.builder()
            .documento(documento)
            .texto(request.getTexto().trim())
            .autor(accesoFaseService.nombreDe(principal))
            .autorTipo(accesoFaseService.tipoDe(principal))
            .autorId(accesoFaseService.idDe(principal))
            .build());

        // Quien acaba de comentar no puede borrarlo salvo que además administre el proyecto.
        return ComentarioResponse.from(comentario, puedeBorrar(proyecto, principal));
    }

    @Transactional
    public void eliminar(Long proyectoId, Long pasoId, Long comentarioId, UserDetails principal) {
        Paso paso = findPaso(pasoId);
        Proyecto proyecto = accesoFaseService.proyectoAccesible(proyectoId, principal);
        accesoFaseService.nivel(proyectoId, paso.getFase(), principal);

        Comentario comentario = comentarioRepository.findById(comentarioId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Comentario no encontrado con id: " + comentarioId));

        // Que el comentario sea de ESTE paso de ESTE proyecto. Sin esto, un id ajeno pasado a
        // mano borraría un comentario de otro proyecto en el que la sesión sí tiene permiso.
        Documento documento = comentario.getDocumento();
        if (!documento.getProyecto().getId().equals(proyecto.getId())
            || !documento.getPaso().getId().equals(pasoId))
            throw new ResourceNotFoundException("Ese comentario no pertenece a este paso");

        if (!puedeBorrar(proyecto, principal))
            throw new AccessDeniedException("No tienes permiso para borrar comentarios");

        comentarioRepository.delete(comentario);
    }

    /**
     * La dueña del proyecto, o la administración del sistema sobre cualquier proyecto.
     *
     * Un colaborador no puede borrar ni los propios, y es deliberado: el comentario es parte
     * del registro de lo que se discutió sobre un documento. Dejar que su autor lo haga
     * desaparecer permitiría retirar una observación incómoda después de que la leyeron.
     */
    private boolean puedeBorrar(Proyecto proyecto, UserDetails principal) {
        if (!(principal instanceof Usuario usuario)) return false;

        return usuario.getRole() == Role.ADMIN || proyectoService.esDuenio(proyecto, usuario);
    }

    private Paso findPaso(Long pasoId) {
        return pasoRepository.findById(pasoId)
            .orElseThrow(() -> new ResourceNotFoundException("Paso no encontrado con id: " + pasoId));
    }
}
