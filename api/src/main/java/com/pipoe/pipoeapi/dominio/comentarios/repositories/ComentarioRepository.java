package com.pipoe.pipoeapi.dominio.comentarios.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pipoe.pipoeapi.dominio.comentarios.entities.Comentario;
import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /** Del más nuevo al más viejo, que es el orden en que se leen. */
    List<Comentario> findByDocumentoOrderByCreadoEnDesc(Documento documento);

    long countByDocumento(Documento documento);
}
