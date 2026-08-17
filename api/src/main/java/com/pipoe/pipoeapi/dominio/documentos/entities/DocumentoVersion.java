package com.pipoe.pipoeapi.dominio.documentos.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Una foto del documento por cada guardado. Es append-only: nunca se actualiza ni se borra
 * salvo que se borre el documento entero.
 *
 * Es la base de la trazabilidad entre participantes: sin esto, saber quién escribió qué se
 * pierde para siempre en cuanto alguien sobrescribe el texto.
 */
@Entity
@Table(name = "documento_versiones")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DocumentoVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(nullable = false, length = 150)
    private String autor;

    /** USUARIO | COLABORADOR. El id no sirve como referencia: los dos tienen ids propios. */
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private String autorTipo;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
