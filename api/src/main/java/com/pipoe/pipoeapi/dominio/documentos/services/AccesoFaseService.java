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
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import lombok.RequiredArgsConstructor;

/**
 * Único lugar donde se responde "esta sesión, sobre esta fase de este proyecto, ¿qué puede
 * hacer?". El equivalente a lo que ProyectoService.findDelUsuario hace con la propiedad.
 *
 * El dueño del proyecto tiene edición en las 5 fases siempre. El colaborador tiene el nivel
 * que le hayan puesto en esa fase, y sólo sobre su propio proyecto.
 *
 * La administradora es el tercer caso: entra a cualquier proyecto, pero únicamente a comentar.
 * Nunca edita lo que escribió otra persona y nunca ve el historial de autoría, que sigue siendo
 * del dueño (exigirDuenio). Es el acompañamiento académico del modelo, no una cuenta con
 * privilegios sobre el contenido ajeno.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccesoFaseService {

    private final ProyectoService proyectoService;

    /** Proyecto al que la sesión tiene acceso: dueño, colaborador o administradora. */
    public Proyecto proyectoAccesible(Long proyectoId, UserDetails principal) {
        if (principal instanceof Usuario usuario) {
            if (usuario.getRole() == Role.ADMIN) return proyectoService.findById(proyectoId);

            return proyectoService.findDelUsuario(proyectoId, usuario);
        }

        if (principal instanceof ColaboradorPrincipal colaborador) {
            if (!colaborador.getProyectoId().equals(proyectoId))
                throw new AccessDeniedException("No tienes acceso a este proyecto");

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
            Proyecto proyecto = proyectoService.findById(proyectoId);

            if (proyectoService.esDuenio(proyecto, usuario)) return NivelPermiso.EDICION;

            // La administradora entra a cualquier proyecto, y en todos con el mismo nivel:
            // comentar. No hereda edición por ser admin.
            if (usuario.getRole() == Role.ADMIN) return NivelPermiso.COMENTARIOS;

            throw new AccessDeniedException("No tienes acceso a este proyecto");
        }

        if (principal instanceof ColaboradorPrincipal principalColaborador) {
            Colaborador colaborador = principalColaborador.getColaborador();

            if (!colaborador.getProyecto().getId().equals(proyectoId))
                throw new AccessDeniedException("No tienes acceso a este proyecto");

            return colaborador.nivelEn(fase);
        }

        throw new AccessDeniedException("Sesión no válida");
    }

    /** Exige poder escribir el documento. Sólo EDICION: COMENTARIOS no toca el texto. */
    public void exigirEdicion(Long proyectoId, Fase fase, UserDetails principal) {
        if (nivel(proyectoId, fase, principal) != NivelPermiso.EDICION)
            throw new AccessDeniedException("No tienes permiso de edición en esta fase");
    }

    /**
     * Exige poder dejar un comentario. Vale COMENTARIOS y también EDICION: los niveles están
     * ordenados de menos a más permisivo, y quien puede reescribir el documento entero
     * difícilmente no pueda opinar sobre él.
     *
     * Leer los comentarios, en cambio, no pide nada más que poder leer el documento: son parte
     * de lo que hay que ver para trabajar sobre él.
     */
    public void exigirComentarios(Long proyectoId, Fase fase, UserDetails principal) {
        if (nivel(proyectoId, fase, principal) == NivelPermiso.LECTURA)
            throw new AccessDeniedException("No tienes permiso para comentar en esta fase");
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

    /**
     * Id de quien opera, dentro de su tipo. Un usuario y un colaborador pueden compartir el
     * número, así que sirve junto a tipoDe y nunca solo: es lo que decide si un comentario es
     * propio a la hora de borrarlo.
     */
    public Long idDe(UserDetails principal) {
        if (principal instanceof Usuario usuario) return usuario.getId();
        if (principal instanceof ColaboradorPrincipal colaborador)
            return colaborador.getColaborador().getId();

        throw new AccessDeniedException("Sesión no válida");
    }
}
