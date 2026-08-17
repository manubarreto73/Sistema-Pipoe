package com.pipoe.pipoeapi.dominio.solicitudes.repositories;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.solicitudes.entities.EstadoSolicitud;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.SolicitudAcceso;

public interface SolicitudAccesoRepository extends JpaRepository<SolicitudAcceso, Long> {

    /**
     * Listado del admin, con los tres filtros combinables. Cada uno se ignora cuando llega
     * nulo, así que la misma consulta sirve para "todas" y para cualquier combinación.
     *
     * Sin JOIN FETCH: la entidad no tiene relaciones.
     *
     * `texto` tiene que llegar ya en minúsculas y con los comodines puestos (`%algo%`): armarlo
     * acá obligaría a concatenar dentro del JPQL, que es menos legible y más fácil de romper.
     *
     * Los `CAST` no son decorativos: un parámetro que sólo aparece en un `IS NULL` no le da a
     * Postgres ninguna pista de qué tipo es, y el driver falla con "could not determine data
     * type of parameter". El cast se la da.
     */
    @Query("""
        SELECT s FROM SolicitudAcceso s
        WHERE (:estado IS NULL OR s.estado = :estado)
          AND (CAST(:desde AS timestamp) IS NULL OR s.fechaSolicitud >= :desde)
          AND (CAST(:hasta AS timestamp) IS NULL OR s.fechaSolicitud < :hasta)
          AND (CAST(:texto AS string) IS NULL
               OR LOWER(s.nombre) LIKE :texto
               OR LOWER(s.apellidos) LIKE :texto
               OR LOWER(s.email) LIKE :texto
               OR LOWER(s.institucion) LIKE :texto)
        """)
    Page<SolicitudAcceso> buscar(
        @Param("estado") EstadoSolicitud estado,
        @Param("texto") String texto,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        Pageable pageable
    );

    boolean existsByEmailAndEstado(String email, EstadoSolicitud estado);
}
