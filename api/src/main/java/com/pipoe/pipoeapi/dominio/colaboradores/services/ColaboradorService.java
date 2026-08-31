package com.pipoe.pipoeapi.dominio.colaboradores.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.colaboradores.dtos.ColaboradorResponse;
import com.pipoe.pipoeapi.dominio.colaboradores.dtos.request.ActualizarPermisosRequest;
import com.pipoe.pipoeapi.dominio.colaboradores.dtos.request.RegisterColaboradorRequest;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.ColaboradorPermiso;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.NivelPermiso;
import com.pipoe.pipoeapi.dominio.colaboradores.repositories.ColaboradorRepository;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.proyectos.services.ProyectoService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;
import com.pipoe.pipoeapi.utils.Emails;
import com.pipoe.pipoeapi.parametros.service.ParametrosService;
import com.pipoe.pipoeapi.utils.EmailService;
import com.pipoe.pipoeapi.utils.PasswordGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColaboradorService {

    private final ColaboradorRepository colaboradorRepository;
    private final ProyectoService proyectoService;
    private final ParametrosService parametrosService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public Colaborador findById(Long id) {
        return colaboradorRepository.findByIdConProyecto(id)
            .orElseThrow(() -> new ResourceNotFoundException("Colaborador no encontrado con id: " + id));
    }

    public List<ColaboradorResponse> listar(Long proyectoId, Usuario solicitante) {
        Proyecto proyecto = proyectoService.findDelUsuario(proyectoId, solicitante);

        return colaboradorRepository.findActivosDelProyecto(proyecto).stream()
            .map(ColaboradorResponse::from)
            .toList();
    }

    @Transactional
    public ColaboradorResponse create(Long proyectoId, RegisterColaboradorRequest request, Usuario solicitante) {
        Proyecto proyecto = proyectoService.findDelUsuario(proyectoId, solicitante);

        request.setEmail(Emails.normalizar(request.getEmail()));

        // El dueño ya tiene edición en las cinco fases. Agregándose como colaborador se creaba
        // una segunda identidad sobre el mismo proyecto, con otra contraseña y con permisos que
        // podían ser menores que los suyos: dos sesiones distintas para la misma persona y un
        // historial que la muestra como dos autores. Se compara contra el dueño del proyecto y
        // no contra quien pide, para que siga valiendo si algún día alguien más puede invitar.
        if (proyecto.getUsuario().getEmail().equalsIgnoreCase(request.getEmail()))
            throw new BusinessException(
                "No puedes agregarte como colaborador de tu propio proyecto: "
                + "ya tienes permiso de edición en las cinco fases");

        Optional<Colaborador> existente =
            colaboradorRepository.findByProyectoAndEmail(proyecto, request.getEmail());
        if (existente.isPresent() && existente.get().isActivo())
            throw new BusinessException("Ese email ya es colaborador de este proyecto");

        int maxColaboradores = parametrosService.getConfiguracion().getMaxColaboradoresPorProyecto();
        if (colaboradorRepository.countByProyectoAndActivoTrue(proyecto) >= maxColaboradores)
            throw new BusinessException("Alcanzaste el máximo de colaboradores permitidos (" + maxColaboradores + ")");

        // Volver a invitar a alguien dado de baja reutiliza su fila: el email es único por
        // proyecto, así que insertar una nueva fallaría contra la constraint.
        Colaborador colaborador = existente.orElseGet(() -> {
            Colaborador nuevo = request.toEntity();
            nuevo.setProyecto(proyecto);
            return nuevo;
        });

        colaborador.setNombre(request.getNombre());
        colaborador.setActivo(true);
        colaborador.reiniciarPermisos();

        String rawPassword = PasswordGenerator.generate();
        colaborador.setPassword(passwordEncoder.encode(rawPassword));

        colaboradorRepository.save(colaborador);
        emailService.enviarAccesoColaborador(
            colaborador.getEmail(), colaborador.getNombre(),
            proyecto.getNombre(), proyecto.getCodigo(), rawPassword
        );

        return ColaboradorResponse.from(colaborador);
    }

    /** Reemplaza el nivel de las 5 fases. Sólo el dueño del proyecto. */
    @Transactional
    public ColaboradorResponse actualizarPermisos(
        Long proyectoId, Long colaboradorId, ActualizarPermisosRequest request, Usuario solicitante
    ) {
        Colaborador colaborador = findActivoDelProyecto(proyectoId, colaboradorId, solicitante);

        Map<Fase, NivelPermiso> niveles = request.getPermisos().stream()
            .collect(Collectors.toMap(
                ActualizarPermisosRequest.PermisoFaseRequest::getFase,
                ActualizarPermisosRequest.PermisoFaseRequest::getNivel,
                (primero, segundo) -> {
                    throw new BusinessException("Hay una fase repetida en los permisos");
                }
            ));

        if (niveles.size() != Fase.values().length)
            throw new BusinessException("Hay que enviar el nivel de las " + Fase.values().length + " fases");

        Map<Fase, ColaboradorPermiso> actuales = colaborador.getPermisos().stream()
            .collect(Collectors.toMap(ColaboradorPermiso::getFase, Function.identity()));

        niveles.forEach((fase, nivel) -> {
            ColaboradorPermiso permiso = actuales.get(fase);

            // Defensivo: un colaborador siempre tiene sus 5 filas, pero si faltara alguna
            // (fila vieja, carga manual) se crea en vez de romper.
            if (permiso == null)
                colaborador.getPermisos().add(ColaboradorPermiso.builder()
                    .colaborador(colaborador).fase(fase).nivel(nivel).build());
            else
                permiso.setNivel(nivel);
        });

        colaboradorRepository.save(colaborador);
        return ColaboradorResponse.from(colaborador);
    }

    /** Baja lógica: deja de listarse y no puede volver a entrar. */
    @Transactional
    public void eliminar(Long proyectoId, Long colaboradorId, Usuario solicitante) {
        Colaborador colaborador = findActivoDelProyecto(proyectoId, colaboradorId, solicitante);

        colaborador.setActivo(false);
        colaboradorRepository.save(colaborador);
    }

    /** Colaborador activo de un proyecto del solicitante. Valida propiedad y pertenencia. */
    private Colaborador findActivoDelProyecto(Long proyectoId, Long colaboradorId, Usuario solicitante) {
        Proyecto proyecto = proyectoService.findDelUsuario(proyectoId, solicitante);
        Colaborador colaborador = findById(colaboradorId);

        if (!colaborador.getProyecto().getId().equals(proyecto.getId()) || !colaborador.isActivo())
            throw new ResourceNotFoundException("Colaborador no encontrado con id: " + colaboradorId);

        return colaborador;
    }

    public Colaborador login(String codigoProyecto, String email, String password) {
        Proyecto proyecto;
        try {
            proyecto = proyectoService.findByCodigo(codigoProyecto);
        } catch (ResourceNotFoundException e) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Colaborador colaborador = colaboradorRepository
            .findByProyectoAndEmail(proyecto, Emails.normalizar(email))
            .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        // Dado de baja: mismo error que una clave equivocada, para no confirmar que el mail existe.
        if (!colaborador.isActivo())
            throw new BadCredentialsException("Credenciales inválidas");

        if (colaborador.getPassword() == null || colaborador.getPassword().isBlank()
            || !passwordEncoder.matches(password, colaborador.getPassword()))
            throw new BadCredentialsException("Credenciales inválidas");

        return colaborador;
    }

    @Transactional
    public void changePassword(Colaborador colaborador, String currentPassword, String newPassword) {
        if (colaborador.getPassword() == null || colaborador.getPassword().isBlank()
            || !passwordEncoder.matches(currentPassword, colaborador.getPassword()))
            throw new BusinessException("La contraseña actual es incorrecta");

        colaborador.setPassword(passwordEncoder.encode(newPassword));
        colaboradorRepository.save(colaborador);
    }

    @Transactional
    public Colaborador actualizarNombre(Colaborador colaborador, String nombre) {
        colaborador.setNombre(nombre);
        return colaboradorRepository.save(colaborador);
    }
}
