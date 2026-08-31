package com.pipoe.pipoeapi.dominio.comentarios.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterComentarioRequest {

    /**
     * Texto plano, no HTML. El documento pasa por HtmlSanitizer porque lo escribe un editor
     * enriquecido; un comentario es un párrafo suelto y no necesita formato, así que la forma
     * más segura de tratarlo es no admitir marcado en absoluto.
     */
    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 2000, message = "El comentario no puede exceder los 2000 caracteres")
    private String texto;
}
