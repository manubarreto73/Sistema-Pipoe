package com.pipoe.pipoeapi.dominio.landing.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/** Un texto de la portada pública. La clave es la identidad: hay una fila por hueco. */
@Entity
@Table(name = "textos_landing")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TextoLanding {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ClaveTexto clave;

    /** Texto plano o HTML del editor, según el tipo de la clave. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
