package com.pipoe.pipoeapi.dominio.documentos.services;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.colaboradores.security.ColaboradorPrincipal;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.proyectos.services.ProyectoService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import lombok.RequiredArgsConstructor;

/**
 * Único lugar donde se responde "esta sesión, sobre esta fase de este proyecto, ¿qué puede
 * hacer?". El equivalente a lo que ProyectoService.findDelUsuario hace con la propiedad.
 *
 * El dueño del proyecto tiene edición en las 5 fases siempre. El colaborador tiene el nivel
 * que le hayan puesto en esa fase, y sólo sobre su propio proyecto.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccesoFaseService {

    private final ProyectoService proyectoService;

    /** Proyecto al que la sesión tiene acceso, sea dueño o colaborador. */
    public Proyecto proyectoAccesible(Long proyectoId, UserDetails principal) {
        if (principal instanceof Usuario usuario)
            return proyectoService.findDelUsuario(proyectoId, usuario);

        if (principal instanceof ColaboradorPrincipal colaborador) {
            if (!colaborador.getProyectoId().equals(proyectoId))
                throw new AccessDeniedException("No tenés acceso a este proyecto");

            return colaborador.getColaborador().getProyecto();
        }

        throw new AccessDeniedException("Sesión no válida");
    }

    /**
     * Exige ser quien creó el proyecto. Hay cosas que no son cuestión de nivel de permiso sino
     * de autoría: el historial de quién escribió qué es una de ellas.
     */
    public Proyecto exigirDuenio(Long proyectoId, UserDetails principal) {
        if (principal instanceof Usuario usuario)
            return proyectoService.findDelUsuario(proyectoId, usuario);

        throw new AccessDeniedException("Sólo quien creó el proyecto puede hacer esto");
    }

    public NivelPermiso nivel(Long proyectoId, Fase fase, UserDetails principal) {
        if (principal instanceof Usuario usuario) {
            proyectoService.findDelUsuario(proyectoId, usuario);
            return NivelPermiso.EDICION;
        }

        if (principal instanceof ColaboradorPrincipal principalColaborador) {
            Colaborador colaborador = principalColaborador.getColaborador();

            if (!colaborador.getProyecto().getId().equals(proyectoId))
                throw new AccessDeniedException("No tenés acceso a este proyecto");

            return colaborador.nivelEn(fase);
        }

        throw new AccessDeniedException("Sesión no válida");
    }

    /**
     * Exige poder escribir. COMENTARIOS todavía no habilita a escribir: hasta que existan los
     * comentarios como funcionalidad, se comporta igual que sólo lectura.
     */
    public void exigirEdicion(Long proyectoId, Fase fase, UserDetails principal) {
        if (nivel(proyectoId, fase, principal) != NivelPermiso.EDICION)
            throw new AccessDeniedException("No tenés permiso de edición en esta fase");
    }

    /** Nombre de quien está operando, para el historial y el cartel de "lo está editando X". */
    public String nombreDe(UserDetails principal) {
        if (principal instanceof Usuario usuario) return usuario.getNombreCompleto();
        if (principal instanceof ColaboradorPrincipal colaborador)
            return colaborador.getColaborador().getNombre();
        return "Desconocido";
    }

    public String tipoDe(UserDetails principal) {
        return principal instanceof ColaboradorPrincipal ? "COLABORADOR" : "USUARIO";
    }
}
