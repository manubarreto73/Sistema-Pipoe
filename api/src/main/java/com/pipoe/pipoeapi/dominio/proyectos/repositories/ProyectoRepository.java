package com.pipoe.pipoeapi.dominio.proyectos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    boolean existsByNombre(String nombre);
    /** Para renombrar: el nombre propio no cuenta como colisión. */
    boolean existsByNombreAndIdNot(String nombre, Long id);
    Optional<Proyecto> findByNombre(String nombre);
    long countByUsuario(Usuario usuario);
    List<Proyecto> findByUsuarioOrderByNombreAsc(Usuario usuario);
}
