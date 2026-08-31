package com.pipoe.pipoeapi.dominio.usuarios.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.usuarios.dtos.UsuarioAdminResponse;
import com.pipoe.pipoeapi.dominio.usuarios.dtos.request.RegisterAdminRequest;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.dominio.usuarios.repository.UsuarioRepository;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Las cuentas del sistema, vistas desde la administración.
 *
 * Aparte de UsuarioService, que responde "quién es esta sesión" y es del que depende Spring
 * Security: éste mira el padrón entero y es sólo para el panel.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    /**
     * `activo` y `role` nulos traen todos. La lista incluye a los deshabilitados a propósito:
     * es desde acá que se los vuelve a activar, así que esconderlos los dejaría inalcanzables.
     */
    public Page<UsuarioAdminResponse> listar(
        String texto, Boolean activo, Role role, Pageable pageable
    ) {
        Page<Usuario> pagina = usuarioRepository.buscarParaAdmin(patron(texto), activo, role, pageable);

        List<Long> ids = pagina.getContent().stream().map(Usuario::getId).toList();

        // Una consulta para toda la página en vez de una por usuario. Con la lista vacía no se
        // pregunta: un IN () no es SQL válido.
        Map<Long, Long> proyectos = ids.isEmpty() ? Map.of() : proyectosPorUsuario(ids);

        return pagina.map(usuario ->
            UsuarioAdminResponse.from(usuario, proyectos.getOrDefault(usuario.getId(), 0L))
        );
    }

    /** Alta de otra cuenta de administración. La contraseña la genera y la manda el servidor. */
    @Transactional
    public UsuarioAdminResponse crearAdmin(RegisterAdminRequest request) {
        usuarioService.register(request.getEmail(), request.getNombreCompleto(), Role.ADMIN);

        Usuario creado = usuarioService.findByEmail(request.getEmail());

        // Un administrador nuevo puede entrar a todos los proyectos del sistema: que quede
        // asentado quién existe, igual que se asienta cada login de administración.
        log.warn("SEGURIDAD alta_admin cuenta={}", creado.getEmail());

        return UsuarioAdminResponse.from(creado, 0);
    }

    /**
     * Baja y alta lógica de una cuenta.
     *
     * No borra nada: sus proyectos, sus documentos y sus comentarios siguen donde estaban. Lo
     * único que cambia es que deja de poder entrar, y que puede volver a hacerlo si se la
     * reactiva. Por eso no hay un borrado de usuarios en ningún lado.
     */
    @Transactional
    public UsuarioAdminResponse cambiarActivo(Long id, boolean activo, Usuario solicitante) {
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        // Desactivarse a sí misma dejaría a la persona afuera en el acto y sin forma de volver,
        // porque reactivar exige una sesión de administración.
        if (usuario.getId().equals(solicitante.getId()) && !activo)
            throw new BusinessException("No puedes desactivar tu propia cuenta");

        // Y si es la última cuenta de administración activa, el sistema se queda sin nadie que
        // pueda aprobar solicitudes ni reactivar a los demás.
        if (!activo && usuario.getRole() == Role.ADMIN && esElUltimoAdminActivo(usuario))
            throw new BusinessException(
                "Es la única cuenta de administración activa. Crea otra antes de desactivar esta.");

        usuario.setEnabled(activo);
        usuarioRepository.save(usuario);

        log.warn("SEGURIDAD {} cuenta={} por={}",
            activo ? "alta_usuario" : "baja_usuario", usuario.getEmail(), solicitante.getEmail());

        long proyectos = proyectosPorUsuario(List.of(usuario.getId()))
            .getOrDefault(usuario.getId(), 0L);

        return UsuarioAdminResponse.from(usuario, proyectos);
    }

    // ------------------------------------------------------------------ interno

    private boolean esElUltimoAdminActivo(Usuario candidato) {
        return usuarioRepository
            .buscarParaAdmin(null, true, Role.ADMIN, Pageable.unpaged())
            .stream()
            .allMatch(admin -> admin.getId().equals(candidato.getId()));
    }

    private Map<Long, Long> proyectosPorUsuario(List<Long> ids) {
        Map<Long, Long> conteo = new HashMap<>();

        for (Object[] fila : usuarioRepository.contarProyectosPorUsuario(ids))
            conteo.put((Long) fila[0], ((Number) fila[1]).longValue());

        return conteo;
    }

    /** Mismo criterio que en los otros listados: los comodines que escriba la persona se descartan. */
    private String patron(String texto) {
        if (texto == null || texto.isBlank()) return null;

        String limpio = texto.trim().toLowerCase(Locale.ROOT).replaceAll("[%_\\\\]", "");

        return limpio.isEmpty() ? null : "%" + limpio + "%";
    }
}
