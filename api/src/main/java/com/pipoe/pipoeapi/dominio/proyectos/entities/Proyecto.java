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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
