package com.pipoe.pipoeapi.dominio.documentos.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Una foto del documento por cada **sesión de escritura**, no por cada guardado.
 *
 * El editor guarda solo cada 2,5 segundos de inactividad. Una fila por guardado convertía una
 * tarde de trabajo en cientos de entradas casi idénticas, entre las que no se encontraba nada.
 * Por eso los guardados seguidos de una misma persona sobre un mismo documento van
 * actualizando esta fila: creado_en es cuándo empezó a escribir y actualizado_en cuándo dejó
 * de hacerlo. Las reglas de qué cuenta como la misma sesión están en DocumentoService.
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

    /**
     * Junto a autorTipo, quién escribió. El nombre solo no alcanza para decidir si un guardado
     * continúa la sesión de alguien: dos personas homónimas fundirían su trabajo en una entrada.
     *
     * Null en las filas anteriores a V15, que nunca se fusionan por eso mismo.
     */
    @Column(name = "autor_id")
    private Long autorId;

    /** Cuándo empezó la sesión: el primer guardado de la tanda. */
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    /** Cuándo fue el último guardado de la sesión. Igual a creadoEn si hubo uno solo. */
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    /** Cuántos guardados absorbió la sesión. Es lo que explica de un vistazo qué es la entrada. */
    @Column(nullable = false)
    private Integer guardados;
}
