package com.pipoe.pipoeapi.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pipoe.pipoeapi.dominio.colaboradores.security.ColaboradorPrincipal;
import com.pipoe.pipoeapi.dominio.colaboradores.services.ColaboradorService;
import com.pipoe.pipoeapi.exceptions.exceptions.ResourceNotFoundException;
import com.pipoe.pipoeapi.exceptions.handlers.ErrorResponse;
import com.pipoe.pipoeapi.redis.RedisKeys;
import com.pipoe.pipoeapi.redis.RedisService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ColaboradorService colaboradorService;
    private final ObjectMapper objectMapper;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String token = authHeader.substring(7);

            if (Boolean.TRUE.equals(redisService.exists(RedisKeys.blacklist + ":" + token))) {
                sendUnauthorized(response, "Token revocado");
                return;
            }

            final String subject = jwtService.extractUsername(token);

            if (subject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = "COLABORADOR".equals(jwtService.extractType(token))
                    ? new ColaboradorPrincipal(colaboradorService.findById(Long.valueOf(subject)))
                    : userDetailsService.loadUserByUsername(subject);

                // Baja lógica del colaborador (o usuario deshabilitado) con el token todavía
                // vigente. Se responde "Token inválido" y no "expirado" a propósito: el cliente
                // no debe reintentar con un refresh, tiene que cerrar la sesión.
                if (!userDetails.isEnabled()) {
                    sendUnauthorized(response, "Token inválido");
                    return;
                }

                if (jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            sendUnauthorized(response, "Token expirado");
        } catch (JwtException | ResourceNotFoundException e) {
            sendUnauthorized(response, "Token inválido");
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        ErrorResponse error = ErrorResponse.builder()
            .status(HttpServletResponse.SC_UNAUTHORIZED)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
