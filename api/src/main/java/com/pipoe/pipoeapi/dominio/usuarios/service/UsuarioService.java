package com.pipoe.pipoeapi.dominio.usuarios.service;

import java.time.LocalDateTime;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.auth.dto.RegisterUsuarioRequest;
import com.pipoe.pipoeapi.auth.dto.UsuarioResponse;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.dominio.usuarios.repository.UsuarioRepository;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.utils.EmailService;
import com.pipoe.pipoeapi.utils.Emails;
import com.pipoe.pipoeapi.utils.PasswordGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return findByEmail(email);
    }

    /** Igual que loadUserByUsername, pero tipado: hace falta para armar el SesionResponse. */
    public Usuario findByEmail(String email) {
        // Normalizado también al buscar, no sólo al guardar: quien escriba su correo con una
        // mayúscula de más en el login tiene que encontrar su cuenta igual.
        return usuarioRepository.findByEmailAndEnabledTrue(Emails.normalizar(email))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }

    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmail(Emails.normalizar(email));
    }

    @Transactional
    public UsuarioResponse register(RegisterUsuarioRequest request) {
        return register(request.getEmail(), request.getNombreCompleto(), request.getRole());
    }

    /** Alta del usuario con clave generada y enviada por mail. La usan el admin y la aprobación de solicitudes. */
    @Transactional
    public UsuarioResponse register(String email, String nombreCompleto, Role role) {
        // Normalizado antes de comprobar: sin esto "Ana@correo.com" no colisionaba con
        // "ana@correo.com" y se creaban dos cuentas para la misma persona.
        email = Emails.normalizar(email);

        if (usuarioRepository.existsByEmail(email))
            throw new BusinessException("Ya existe un usuario con ese email");

        String rawPassword = PasswordGenerator.generate();

        Usuario usuario = Usuario.builder()
            .email(email)
            .nombreCompleto(nombreCompleto)
            .password(passwordEncoder.encode(rawPassword))
            .role(role != null ? role : Role.USER)
            .enabled(true)
            .build();

        usuarioRepository.save(usuario);
        emailService.enviarCredencialesUsuario(usuario.getEmail(), usuario.getNombreCompleto(), rawPassword);

        return UsuarioResponse.from(usuario);
    }

    /**
     * Deja constancia de que la cuenta entró. Se llama después de autenticar y nunca antes: un
     * intento fallido no es un acceso, y contarlo como tal haría que el panel mostrara actividad
     * de alguien que justamente no logró entrar.
     */
    @Transactional
    public void registrarAcceso(Usuario usuario) {
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario actualizarNombre(Usuario usuario, String nombreCompleto) {
        usuario.setNombreCompleto(nombreCompleto);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void changePassword(Usuario usuario, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, usuario.getPassword()))
            throw new BusinessException("La contraseña actual es incorrecta");

        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }
}
