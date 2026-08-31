package com.pipoe.pipoeapi.dominio.proyectos.entities;

import com.pipoe.pipoeapi.dominio.usuarios.entities.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "proyectos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Proyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proyecto_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    /**
     * Con lo que entra un colaborador. Se genera al crear el proyecto y no se toca nunca más:
     * es lo único estable que tiene el proyecto, porque el nombre lo puede cambiar el dueño.
     */
    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
