package com.pipoe.pipoeapi.dominio.documentos.dtos;

import java.time.LocalDateTime;
import java.util.List;

import com.pipoe.pipoeapi.dominio.documentos.entities.DocumentoVersion;
import com.pipoe.pipoeapi.dominio.documentos.services.DiffTexto;

import lombok.*;

/**
 * Una sesión de escritura mostrada como cambio y no como foto: qué texto agregó y qué texto
 * sacó su autor respecto de lo que había. Es la vista que responde "¿quién escribió esto?".
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class VersionDiffResponse {

    private Long id;
    private String autor;
    private String autorTipo;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private Integer guardados;
    private Integer palabrasAgregadas;
    private Integer palabrasQuitadas;
    private List<SegmentoResponse> segmentos;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    @Builder
    public static class SegmentoResponse {
        /** IGUAL | AGREGADO | QUITADO. */
        private String tipo;
        private String texto;
    }

    public static VersionDiffResponse from(DocumentoVersion version, String contenidoPrevio) {
        List<DiffTexto.Segmento> segmentos =
            DiffTexto.comparar(contenidoPrevio, version.getContenido());
        DiffTexto.Resumen resumen = DiffTexto.resumir(segmentos);

        return VersionDiffResponse.builder()
            .id(version.getId())
            .autor(version.getAutor())
            .autorTipo(version.getAutorTipo())
            .creadoEn(version.getCreadoEn())
            .actualizadoEn(version.getActualizadoEn())
            .guardados(version.getGuardados())
            .palabrasAgregadas(resumen.agregadas())
            .palabrasQuitadas(resumen.quitadas())
            .segmentos(segmentos.stream()
                .map(segmento -> SegmentoResponse.builder()
                    .tipo(segmento.tipo().name())
                    .texto(segmento.texto())
                    .build())
                .toList())
            .build();
    }
}
