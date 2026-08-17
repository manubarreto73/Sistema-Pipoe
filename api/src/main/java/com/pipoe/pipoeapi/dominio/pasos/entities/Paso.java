package com.pipoe.pipoeapi.dominio.pasos.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Un paso del modelo PipoE. Es catálogo: no pertenece a ningún proyecto, es igual para todos.
 *
 * El producto de cada fase también es un Paso, con `esProducto` en true y orden 99, para que
 * su documento se maneje exactamente igual que el de los demás.
 */
@Entity
@Table(name = "pasos", uniqueConstraints = @UniqueConstraint(columnNames = {"fase", "orden"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Paso {

    /** Orden reservado para el producto de la fase, que va siempre último. */
    public static final int ORDEN_PRODUCTO = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "paso_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fase fase;

    @Column(nullable = false)
    private Integer orden;

    /** Texto tal cual el modelo. Largo: hay títulos de más de 150 caracteres. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String titulo;

    /** Etiqueta para el diagrama de flujo, donde el título entero no entra. */
    @Column(name = "titulo_corto", nullable = false, length = 60)
    private String tituloCorto;

    /** Cómo completar el paso. Lo carga la dueña; arranca vacío. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String explicacion;

    /** Ejemplo de referencia, igual para todos los proyectos. Lo carga la dueña. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String ejemplo;

    @Column(name = "es_producto", nullable = false)
    private boolean esProducto;
}
