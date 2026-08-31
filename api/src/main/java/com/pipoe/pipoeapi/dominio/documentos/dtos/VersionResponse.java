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
    /** Cuándo empezó la sesión de escritura. */
    private LocalDateTime creadoEn;
    /** Cuándo fue el último guardado de esa sesión. Igual a creadoEn si hubo uno solo. */
    private LocalDateTime actualizadoEn;
    /** Cuántos guardados se fusionaron en esta entrada. */
    private Integer guardados;
    /** Cuántos caracteres de texto tenía, para ver de un vistazo si creció o se vació. */
    private Integer largo;
    /** Palabras que este guardado sumó respecto del anterior. */
    private Integer palabrasAgregadas;
    /** Palabras que este guardado borró respecto del anterior. */
    private Integer palabrasQuitadas;

    /** `contenidoPrevio` es el de la sesión anterior, o null si ésta es la primera. */
    public static VersionResponse from(DocumentoVersion version, String contenidoPrevio) {
        String texto = DiffTexto.aTextoPlano(version.getContenido());
        DiffTexto.Resumen resumen = DiffTexto.resumir(contenidoPrevio, version.getContenido());

        return VersionResponse.builder()
            .id(version.getId())
            .autor(version.getAutor())
            .autorTipo(version.getAutorTipo())
            .creadoEn(version.getCreadoEn())
            .actualizadoEn(version.getActualizadoEn())
            .guardados(version.getGuardados())
            .largo(texto.length())
            .palabrasAgregadas(resumen.agregadas())
            .palabrasQuitadas(resumen.quitadas())
            .build();
    }
}
