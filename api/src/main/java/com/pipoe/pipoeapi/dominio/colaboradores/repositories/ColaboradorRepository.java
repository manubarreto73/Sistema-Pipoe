package com.pipoe.pipoeapi.dominio.colaboradores.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.colaboradores.entities.Colaborador;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {
    /** Sin filtrar por activo: el alta lo usa para detectar un colaborador dado de baja y reactivarlo. */
    Optional<Colaborador> findByProyectoAndEmail(Proyecto proyecto, String email);

    long countByProyectoAndActivoTrue(Proyecto proyecto);

    /** Cuántos colaboradores activos tiene cada proyecto de la lista, en una sola consulta. */
    @Query("SELECT c.proyecto.id, COUNT(c) FROM Colaborador c "
         + "WHERE c.proyecto.id IN :proyectoIds AND c.activo = TRUE GROUP BY c.proyecto.id")
    List<Object[]> contarActivosPorProyecto(@Param("proyectoIds") List<Long> proyectoIds);

    /** Para el borrado del proyecto: incluye a los dados de baja. */
    void deleteByProyecto(Proyecto proyecto);

    // DISTINCT porque el fetch de los permisos multiplica las filas del colaborador.
    @Query("""
        SELECT DISTINCT c FROM Colaborador c
        LEFT JOIN FETCH c.permisos
        WHERE c.proyecto = :proyecto AND c.activo = true
        ORDER BY c.nombre ASC
        """)
    List<Colaborador> findActivosDelProyecto(@Param("proyecto") Proyecto proyecto);

    // Lo usa el filtro JWT para armar el principal de cada request de un colaborador, así que
    // trae todo lo que se consulta fuera de una transacción: el proyecto (claim proyectoId,
    // /me, ColaboradorResponse) y los permisos (nivel de acceso a cada fase). Sin el fetch de
    // permisos, cualquier operación sobre un documento revienta con LazyInitializationException.
    @Query("SELECT c FROM Colaborador c JOIN FETCH c.proyecto LEFT JOIN FETCH c.permisos WHERE c.id = :id")
    Optional<Colaborador> findByIdConProyecto(@Param("id") Long id);
}
