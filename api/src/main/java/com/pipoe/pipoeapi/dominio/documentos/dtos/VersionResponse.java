package com.pipoe.pipoeapi.dominio.documentos.dtos;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.documentos.entities.DocumentoVersion;
import com.pipoe.pipoeapi.dominio.documentos.services.DiffTexto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VersionResponse {

    private Long id;
    private String autor;
    private String autorTipo;
    private LocalDateTime creadoEn;
    /** Cuántos caracteres de texto tenía, para ver de un vistazo si creció o se vació. */
    private Integer largo;
    /** Palabras que este guardado sumó respecto del anterior. */
    private Integer palabrasAgregadas;
    /** Palabras que este guardado borró respecto del anterior. */
    private Integer palabrasQuitadas;

    /** `contenidoPrevio` es el del guardado anterior, o null si éste es el primero. */
    public static VersionResponse from(DocumentoVersion version, String contenidoPrevio) {
        String texto = DiffTexto.aTextoPlano(version.getContenido());
        DiffTexto.Resumen resumen = DiffTexto.resumir(contenidoPrevio, version.getContenido());

        return VersionResponse.builder()
            .id(version.getId())
            .autor(version.getAutor())
            .autorTipo(version.getAutorTipo())
            .creadoEn(version.getCreadoEn())
            .largo(texto.length())
            .palabrasAgregadas(resumen.agregadas())
            .palabrasQuitadas(resumen.quitadas())
            .build();
    }
}
