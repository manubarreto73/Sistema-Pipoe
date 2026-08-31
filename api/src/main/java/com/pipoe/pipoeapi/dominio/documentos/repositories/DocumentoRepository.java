package com.pipoe.pipoeapi.dominio.documentos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;
import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    Optional<Documento> findByProyectoIdAndPasoId(Long proyectoId, Long pasoId);

    /** Todos los documentos del proyecto: alcanza para calcular el progreso de las 5 fases. */
    @Query("SELECT d FROM Documento d JOIN FETCH d.paso WHERE d.proyecto = :proyecto")
    List<Documento> findDelProyecto(@Param("proyecto") Proyecto proyecto);

    @Query("""
        SELECT d FROM Documento d JOIN FETCH d.paso p
        WHERE d.proyecto = :proyecto AND p.fase = :fase
        """)
    List<Documento> findDelProyectoYFase(@Param("proyecto") Proyecto proyecto, @Param("fase") Fase fase);

    /**
     * Lo completado en varios proyectos de una sola vez, como (proyectoId, fase, esProducto).
     *
     * Es para el listado de la administradora, que pinta el avance de las 5 fases de cada
     * proyecto de la página. Preguntarlo proyecto por proyecto sería el N+1 de manual: con una
     * página de 30, treinta viajes a la base para dibujar unos cuadraditos de color.
     *
     * Sólo trae lo completado, que es todo lo que el indicador necesita saber.
     */
    @Query("""
        SELECT d.proyecto.id, p.fase, p.esProducto
        FROM Documento d JOIN d.paso p
        WHERE d.proyecto.id IN :proyectoIds AND d.completado = TRUE
        """)
    List<Object[]> completadosDe(@Param("proyectoIds") List<Long> proyectoIds);
}
