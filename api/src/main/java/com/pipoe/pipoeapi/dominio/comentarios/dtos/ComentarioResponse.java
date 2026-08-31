package com.pipoe.pipoeapi.dominio.comentarios.dtos;

import java.time.LocalDateTime;

import com.pipoe.pipoeapi.dominio.comentarios.entities.Comentario;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ComentarioResponse {

    private Long id;
    private String texto;
    private String autor;
    /** USUARIO o COLABORADOR: el front lo usa para distinguir de un vistazo quién escribió. */
    private String autorTipo;
    private LocalDateTime creadoEn;

    /**
     * Si quien está mirando puede borrarlo: la dueña del proyecto o la administración del
     * sistema. Se resuelve en la API y no en el front, porque es una regla de negocio y
     * porque el front tendría que recibir la identidad de todo el mundo para calcularla.
     */
    private boolean puedeBorrar;

    public static ComentarioResponse from(Comentario comentario, boolean puedeBorrar) {
        return ComentarioResponse.builder()
            .id(comentario.getId())
            .texto(comentario.getTexto())
            .autor(comentario.getAutor())
            .autorTipo(comentario.getAutorTipo())
            .creadoEn(comentario.getCreadoEn())
            .puedeBorrar(puedeBorrar)
            .build();
    }
}
