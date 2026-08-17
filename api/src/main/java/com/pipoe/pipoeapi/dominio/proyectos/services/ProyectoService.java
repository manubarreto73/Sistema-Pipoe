package com.pipoe.pipoeapi.dominio.proyectos.services;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.colaboradores.repositories.ColaboradorRepository;
import com.pipoe.pipoeapi.dominio.colaboradores.security.ColaboradorPrincipal;
import com.pipoe.pipoeapi.dominio.proyectos.dtos.ProyectoResponse;
import com.pipoe.pipoeapi.dominio.proyectos.dtos.request.RegisterProyectoRequest;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.proyectos.repositories.ProyectoRepository;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;
import com.pipoe.pipoeapi.parametros.service.ParametrosService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    // El repositorio y no el servicio: ColaboradorService ya depende de éste, y pedirle el
    // servicio cerraría el ciclo.
    private final ColaboradorRepository colaboradorRepository;
    private final ParametrosService parametrosService;

    public Proyecto findById(Long id) {
        return proyectoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado con id: " + id));
    }

    public Proyecto findByNombre(String nombre) {
        return proyectoRepository.findByNombre(nombre)
            .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado: " + nombre));
    }

    /** Proyecto del que el usuario es dueño. Único punto donde se valida la propiedad. */
    public Proyecto findDelUsuario(Long id, Usuario usuario) {
        Proyecto proyecto = findById(id);

        if (!proyecto.getUsuario().getId().equals(usuario.getId()))
            throw new AccessDeniedException("No tenés acceso a este proyecto");

        return proyecto;
    }

    public List<ProyectoResponse> listarDelUsuario(Usuario usuario) {
        return proyectoRepository.findByUsuarioOrderByNombreAsc(usuario).stream()
            .map(ProyectoResponse::from)
            .toList();
    }

    /** Detalle para cualquier sesión: el dueño ve su proyecto, el colaborador sólo el suyo. */
    public ProyectoResponse getDetalle(Long id, UserDetails principal) {
        if (principal instanceof ColaboradorPrincipal colaborador) {
            if (!colaborador.getProyectoId().equals(id))
                throw new AccessDeniedException("No tenés acceso a este proyecto");

            return ProyectoResponse.from(findById(id));
        }

        if (principal instanceof Usuario usuario)
            return ProyectoResponse.from(findDelUsuario(id, usuario));

        throw new AccessDeniedException("Sesión no válida para ver proyectos");
    }

    @Transactional
    public ProyectoResponse create(RegisterProyectoRequest request, Usuario creador) {
        if (proyectoRepository.existsByNombre(request.getNombre()))
            throw new BusinessException("Ya existe un proyecto con ese nombre");

        int maxProyectos = parametrosService.getConfiguracion().getMaxProyectosPorUsuario();
        if (proyectoRepository.countByUsuario(creador) >= maxProyectos)
            throw new BusinessException("Alcanzaste el máximo de proyectos permitidos (" + maxProyectos + ")");

        Proyecto proyecto = request.toEntity();
        proyecto.setUsuario(creador);

        proyectoRepository.save(proyecto);
        return ProyectoResponse.from(proyecto);
    }

    @Transactional
    public ProyectoResponse renombrar(Long id, RegisterProyectoRequest request, Usuario solicitante) {
        Proyecto proyecto = findDelUsuario(id, solicitante);

        if (proyectoRepository.existsByNombreAndIdNot(request.getNombre(), id))
            throw new BusinessException("Ya existe un proyecto con ese nombre");

        proyecto.setNombre(request.getNombre());
        proyectoRepository.save(proyecto);

        return ProyectoResponse.from(proyecto);
    }

    /**
     * Borrado físico, con sus colaboradores. No hay baja lógica acá: el proyecto es del
     * usuario y no queda nada que auditar una vez que decidió borrarlo.
     *
     * Los permisos se van solos por el ON DELETE CASCADE de colaborador_permisos. Los
     * colaboradores hay que borrarlos a mano porque su FK a proyectos no tiene cascade:
     * es a propósito, así un borrado accidental de un proyecto con gente adentro falla
     * en vez de arrastrarla en silencio.
     */
    @Transactional
    public void eliminar(Long id, Usuario solicitante) {
        Proyecto proyecto = findDelUsuario(id, solicitante);

        colaboradorRepository.deleteByProyecto(proyecto);
        proyectoRepository.delete(proyecto);
    }
}
