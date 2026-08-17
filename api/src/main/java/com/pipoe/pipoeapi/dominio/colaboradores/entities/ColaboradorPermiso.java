package com.pipoe.pipoeapi.dominio.colaboradores.entities;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;

import jakarta.persistence.*;
import lombok.*;

/** Nivel de un colaborador en una fase concreta. Siempre hay una fila por cada una de las 5 fases. */
@Entity
@Table(
    name = "colaborador_permisos",
    uniqueConstraints = @UniqueConstraint(columnNames = {"colaborador_id", "fase"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ColaboradorPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permiso_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Fase fase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NivelPermiso nivel;
}
