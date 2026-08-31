package com.pipoe.pipoeapi.dominio.documentos.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;
import com.pipoe.pipoeapi.dominio.documentos.entities.DocumentoVersion;

public interface DocumentoVersionRepository extends JpaRepository<DocumentoVersion, Long> {

    /**
     * Por id y no por fecha: dos guardados pueden caer en el mismo milisegundo y el orden
     * importa, porque cada versión se compara contra la anterior.
     */
    List<DocumentoVersion> findByDocumentoOrderByIdDesc(Documento documento, Pageable pageable);

    /**
     * La última fila del documento, sea de quien sea. Es lo único que hay que mirar para saber
     * si un guardado continúa una sesión: si la más reciente no es de quien está guardando,
     * es que alguien escribió en el medio y la sesión se corta sola.
     */
    Optional<DocumentoVersion> findFirstByDocumentoOrderByIdDesc(Documento documento);

    /** La versión inmediatamente anterior, contra la que se calcula el diff. */
    Optional<DocumentoVersion> findFirstByDocumentoAndIdLessThanOrderByIdDesc(
        Documento documento, Long id);

    /**
     * Borra las versiones intermedias anteriores al corte, dejando **la última de cada autor
     * por documento y por día**. Ver PodaHistorialService.
     *
     * El id sirve de desempate porque es creciente: la fila de id más alto de un grupo es
     * siempre la más reciente de ese grupo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        DELETE FROM DocumentoVersion v
        WHERE v.creadoEn < :corte
          AND v.id NOT IN (
            SELECT MAX(ultima.id) FROM DocumentoVersion ultima
            WHERE ultima.creadoEn < :corte
            GROUP BY ultima.documento, ultima.autor, CAST(ultima.creadoEn AS date)
          )
        """)
    int podarAnterioresA(@Param("corte") LocalDateTime corte);
}
