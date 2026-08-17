package com.pipoe.pipoeapi.dominio.documentos.entities;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.pasos.entities.Paso;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

import jakarta.persistence.*;
import lombok.*;

/**
 * El documento de un paso dentro de un proyecto. Se crea vacío la primera vez que alguien
 * entra al paso, no al crear el proyecto: con 37 pasos por proyecto, la mayoría nunca se
 * escribe y no tiene sentido materializarlos todos.
 */
@Entity
@Table(
    name = "documentos",
    uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "paso_id"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "documento_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paso_id", nullable = false)
    private Paso paso;

    /**
     * HTML del editor. Va como TEXT y no como @Lob: en Postgres, @Lob mapea a un large object
     * que vive fuera de la tabla, no se lee con un SELECT normal y complica backups y borrados.
     * TEXT no tiene límite práctico (~1 GB) y se comporta como cualquier otra columna.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String contenido = "";

    @Column(nullable = false)
    @Builder.Default
    private boolean completado = false;

    /** Bloqueo optimista a mano: sube en cada guardado y el cliente manda la que tenía. */
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    /** Nombre de quien guardó por última vez, para mostrarlo sin resolver otra entidad. */
    @Column(name = "actualizado_por", length = 150)
    private String actualizadoPor;

    /** Un paso está "en progreso" apenas tiene algo escrito. */
    public boolean tieneContenido() {
        return contenido != null && !contenido.isBlank() && !esHtmlVacio();
    }

    /**
     * TipTap nunca devuelve la cadena vacía: un documento sin nada es "<p></p>". Sin esto,
     * abrir un paso y no escribir nada lo dejaría contando como empezado.
     */
    private boolean esHtmlVacio() {
        String sinEtiquetas = contenido.replaceAll("<[^>]*>", "").replace("&nbsp;", " ");
        return sinEtiquetas.isBlank();
    }
}
