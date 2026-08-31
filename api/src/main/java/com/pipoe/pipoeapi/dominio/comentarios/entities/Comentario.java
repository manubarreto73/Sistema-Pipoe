package com.pipoe.pipoeapi.dominio.comentarios.entities;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.documentos.entities.Documento;

import jakarta.persistence.*;
import lombok.*;

/**
 * Un comentario sobre el documento de un paso.
 *
 * Cuelga del documento y no del paso: el paso es catálogo compartido por todos los proyectos,
 * el documento es el de este proyecto. Como el producto de cada fase (Plan de promoción,
 * Diagnóstico situacional...) también es un paso con su documento, esta misma entidad cubre
 * los pasos del despliegue y los productos sin un segundo camino para lo mismo.
 *
 * No guarda a qué parte del texto se refiere, a propósito: anclar un comentario a un fragmento
 * obliga a reubicarlo en cada edición del documento, y acá el comentario es sobre el documento.
 */
@Entity
@Table(name = "comentarios")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comentario_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    /**
     * El nombre de quien comentó, copiado. Igual que en DocumentoVersion: queda congelado para
     * que el hilo se siga leyendo aunque después la persona cambie su nombre o la den de baja
     * del proyecto.
     */
    @Column(nullable = false, length = 150)
    private String autor;

    /** USUARIO o COLABORADOR. Sin esto, el colaborador 3 podría borrar lo del usuario 3. */
    @Column(name = "autor_tipo", nullable = false, length = 20)
    private String autorTipo;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }
}
