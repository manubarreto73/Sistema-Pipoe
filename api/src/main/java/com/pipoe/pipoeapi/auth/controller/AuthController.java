package com.pipoe.pipoeapi.auth.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.pipoe.pipoeapi.auth.dto.*;
import com.pipoe.pipoeapi.auth.service.LoginAttemptsService;
import com.pipoe.pipoeapi.auth.service.RefreshTokenService;
import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;
import com.pipoe.pipoeapi.dominio.colaboradores.security.ColaboradorPrincipal;
import com.pipoe.pipoeapi.dominio.colaboradores.services.ColaboradorService;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;
import com.pipoe.pipoeapi.dominio.usuarios.service.UsuarioService;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;
import com.pipoe.pipoeapi.redis.RedisKeys;
import com.pipoe.pipoeapi.redis.RedisService;
import com.pipoe.pipoeapi.security.JwtService;
import com.pipoe.pipoeapi.utils.RequestUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UsuarioService usuarioService;
    private final ColaboradorService colaboradorService;
    private final LoginAttemptsService loginAttemptsService;
    private final RequestUtils requestUtils;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RedisService redisService;

    private static final String PREFIJO_USUARIO = "USUARIO:";
    private static final String PREFIJO_COLABORADOR = "COLABORADOR:";

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody RegisterUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.register(request));
    }

    /** Cambio de nombre propio. Vale para usuarios y colaboradores: se resuelve por el principal. */
    @PutMapping("/perfil")
    public ResponseEntity<SesionResponse> actualizarPerfil(
        @Valid @RequestBody ActualizarPerfilRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal instanceof ColaboradorPrincipal colaborador)
            return ResponseEntity.ok(SesionResponse.from(
                colaboradorService.actualizarNombre(colaborador.getColaborador(), request.getNombreCompleto())
            ));

        return ResponseEntity.ok(SesionResponse.from(
            usuarioService.actualizarNombre((Usuario) principal, request.getNombreCompleto())
        ));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal instanceof ColaboradorPrincipal colaborador)
            colaboradorService.changePassword(
                colaborador.getColaborador(), request.getCurrentPassword(), request.getNewPassword()
            );
        else
            usuarioService.changePassword(
                (Usuario) principal, request.getCurrentPassword(), request.getNewPassword()
            );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/colaborador/login")
    public ResponseEntity<LoginResponse> colaboradorLogin(
        @Valid @RequestBody ColaboradorLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = requestUtils.clientIp(httpRequest);
        // La cuenta de un colaborador es su email dentro de un proyecto: la clave del contador
        // los junta, o dos colaboradores homónimos de proyectos distintos se bloquearían entre sí.
        String cuenta = request.getNombreProyecto() + "/" + request.getEmail();

        verificarBloqueo(ip, cuenta);

        Colaborador colaborador;
        try {
            colaborador = colaboradorService.login(
                request.getNombreProyecto(), request.getEmail(), request.getPassword()
            );
        } catch (BadCredentialsException e) {
            registrarFallo(ip, cuenta);
            throw e;
        }

        loginAttemptsService.limpiarIntentos(ip, cuenta);

        String accessToken = colaboradorAccessToken(colaborador);
        String refreshToken = refreshTokenService.create(PREFIJO_COLABORADOR + colaborador.getId());

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, SesionResponse.from(colaborador)));
    }

    @GetMapping("/me")
    public ResponseEntity<SesionResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(SesionResponse.from(principal));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = requestUtils.clientIp(httpRequest);
        String cuenta = request.getEmail();

        verificarBloqueo(ip, cuenta);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            registrarFallo(ip, cuenta);
            throw e;
        }

        loginAttemptsService.limpiarIntentos(ip, cuenta);

        Usuario usuario = usuarioService.findByEmail(request.getEmail());
        String accessToken = jwtService.generateToken(usuario);
        String refreshToken = refreshTokenService.create(PREFIJO_USUARIO + usuario.getUsername());

        // Cada entrada de una cuenta con permisos de administración debería ser esperada: son
        // dos cuentas en todo el sistema, así que cualquier renglón de éstos merece mirarse.
        if (usuario.getRole() == Role.ADMIN)
            log.warn("SEGURIDAD login_admin cuenta={} ip={}", usuario.getUsername(), ip);

        return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken, SesionResponse.from(usuario)));
    }

    /**
     * Mismo mensaje para el bloqueo por IP y por cuenta: distinguirlos le confirmaría a quien
     * está probando contraseñas cuál de los dos frenos activó.
     */
    private void verificarBloqueo(String ip, String cuenta) {
        if (loginAttemptsService.estaBloqueada(ip) || loginAttemptsService.cuentaBloqueada(cuenta))
            throw new BusinessException("Acceso bloqueado temporalmente por intentos fallidos");
    }

    private void registrarFallo(String ip, String cuenta) {
        loginAttemptsService.registrarIntento(ip, cuenta);
        log.warn("SEGURIDAD login_fallido cuenta={} ip={}", cuenta, ip);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String newRefreshToken = UUID.randomUUID().toString();
        String subject = refreshTokenService.validateAndRotate(request.getRefreshToken(), newRefreshToken);

        String newAccessToken;
        if (subject.startsWith(PREFIJO_COLABORADOR)) {
            Long colaboradorId = Long.valueOf(subject.substring(PREFIJO_COLABORADOR.length()));
            newAccessToken = colaboradorAccessToken(colaboradorService.findById(colaboradorId));
        } else {
            newAccessToken = jwtService.generateToken(
                usuarioService.findByEmail(subject.substring(PREFIJO_USUARIO.length()))
            );
        }

        return ResponseEntity.ok(new RefreshResponse(newAccessToken, newRefreshToken));
    }

    private String colaboradorAccessToken(Colaborador colaborador) {
        return jwtService.generateToken(
            String.valueOf(colaborador.getId()),
            Map.of("type", "COLABORADOR", "proyectoId", colaborador.getProyecto().getId())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @Valid @RequestBody RefreshRequest request,
        HttpServletRequest httpRequest
    ) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            long ttlMinutes = jwtService.getRemainingMinutes(accessToken);
            redisService.set(RedisKeys.blacklist + ":" + accessToken, "1", ttlMinutes);
        }
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
