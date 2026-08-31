package com.pipoe.pipoeapi.dominio.usuarios.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Role;
import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndEnabledTrue(String email);
    boolean existsByEmail(String email);

    /** Lo usa AdminBootstrap para saber si hace falta crear el primer administrador. */
    boolean existsByRole(Role role);

    /**
     * El panel de usuarios, con su búsqueda y sus filtros.
     *
     * `texto` busca en el nombre y en el email, y llega ya en minúsculas y con los comodines
     * puestos, igual que en los otros listados de administración.
     *
     * Los filtros se ignoran cuando llegan nulos, así que la misma consulta sirve para "todos"
     * y para cualquier combinación. Trae también a los deshabilitados: el panel los sigue
     * mostrando, porque desde ahí se los vuelve a activar.
     *
     * El CAST del texto no es decorativo: un parámetro que sólo aparece dentro de un IS NULL no
     * le da a Postgres ninguna pista de su tipo, y el driver falla con "could not determine data
     * type of parameter".
     */
    @Query("""
        SELECT u FROM Usuario u
        WHERE (CAST(:texto AS string) IS NULL
               OR LOWER(u.nombreCompleto) LIKE :texto
               OR LOWER(u.email) LIKE :texto)
          AND (:activo IS NULL OR u.enabled = :activo)
          AND (:role IS NULL OR u.role = :role)
        """)
    Page<Usuario> buscarParaAdmin(
        @Param("texto") String texto,
        @Param("activo") Boolean activo,
        @Param("role") Role role,
        Pageable pageable
    );

    /**
     * Cuántos proyectos tiene cada usuario de la lista, en una sola consulta.
     *
     * Preguntarlo usuario por usuario sería el N+1 de manual. Los que no tienen ninguno no
     * aparecen en el resultado: el que consume completa con cero.
     */
    @Query("""
        SELECT p.usuario.id, COUNT(p) FROM Proyecto p
        WHERE p.usuario.id IN :usuarioIds
        GROUP BY p.usuario.id
        """)
    java.util.List<Object[]> contarProyectosPorUsuario(
        @Param("usuarioIds") java.util.List<Long> usuarioIds
    );
}
