package com.pipoe.pipoeapi.dominio.usuarios.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndEnabledTrue(String email);
    boolean existsByEmail(String email);
}
