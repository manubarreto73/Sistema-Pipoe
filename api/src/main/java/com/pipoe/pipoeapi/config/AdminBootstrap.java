package com.pipoe.pipoeapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.dominio.usuarios.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Crea el primer administrador cuando la base todavía no tiene ninguno.
 *
 * Sin esto una base recién migrada queda cerrada con llave por dentro: dar de alta usuarios
 * exige {@code hasRole('ADMIN')}, y no hay ningún ADMIN al que loguearse para poder darlo.
 *
 * <p>La contraseña llega por variable de entorno y no queda en el repositorio. Se eligió esto
 * antes que una migración de Flyway con el hash adentro: un hash BCrypt versionado es un hash
 * público, y el día que alguien clona el proyecto se lleva la credencial inicial.
 *
 * <p>Corre en cada arranque, pero <b>sólo hace algo si no existe ningún ADMIN</b>. Si ya hay uno
 * no toca nada: no reescribe la contraseña ni recrea la cuenta. Para recuperar el acceso a un
 * sistema con un ADMIN existente hay que entrar a la base, no reiniciar la aplicación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements ApplicationRunner {

    /** Por debajo de esto no vale la pena crear la cuenta: es la puerta de entrada al sistema. */
    private static final int LARGO_MINIMO = 12;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-inicial.email:}")
    private String email;

    @Value("${app.admin-inicial.password:}")
    private String password;

    @Value("${app.admin-inicial.nombre:Administrador}")
    private String nombre;

    /**
     * Los avisos son deliberadamente ruidosos: si el arranque no crea el admin, el sistema queda
     * inaccesible y el motivo tiene que estar en la primera pantalla de log, no escondido.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByRole(Role.ADMIN)) return;

        if (email.isBlank() || password.isBlank()) {
            log.warn("""
                No hay ningún usuario ADMIN en la base y no se configuró el administrador inicial.
                Nadie puede entrar al sistema. Definí ADMIN_INICIAL_EMAIL y ADMIN_INICIAL_PASSWORD \
                y reinicia la aplicación.""");
            return;
        }

        if (password.length() < LARGO_MINIMO) {
            log.error("""
                ADMIN_INICIAL_PASSWORD tiene menos de {} caracteres: no se creó el administrador. \
                Es la credencial con más permisos del sistema y viaja en texto plano hasta acá.""",
                LARGO_MINIMO);
            return;
        }

        Usuario admin = Usuario.builder()
            .email(email.trim().toLowerCase())
            .nombreCompleto(nombre)
            .password(passwordEncoder.encode(password))
            .role(Role.ADMIN)
            .enabled(true)
            .build();

        usuarioRepository.save(admin);

        // No se manda por mail: el que arrancó el servidor ya conoce la contraseña, la puso él.
        log.info("SEGURIDAD admin_inicial_creado cuenta={} — cambia la contraseña en el primer "
            + "login y borra la variable del entorno", admin.getEmail());
    }
}
