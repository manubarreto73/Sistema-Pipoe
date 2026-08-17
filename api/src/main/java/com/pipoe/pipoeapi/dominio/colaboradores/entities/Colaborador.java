package com.pipoe.pipoeapi.dominio.colaboradores.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pipoe.pipoeapi.dominio.pasos.entities.Fase;
import com.pipoe.pipoeapi.dominio.proyectos.entities.Proyecto;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "colaboradores",
    uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "email"})
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "colaborador_id")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    /** Baja lógica: el colaborador dado de baja no puede entrar ni aparece en el listado. */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @OneToMany(mappedBy = "colaborador", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fase ASC")
    @Builder.Default
    private List<ColaboradorPermiso> permisos = new ArrayList<>();

    /**
     * Deja las 5 fases en el nivel más restrictivo; el dueño las sube después.
     *
     * Reusa las filas que ya existan en vez de vaciar la colección y volver a crearlas: con
     * orphanRemoval, Hibernate inserta las nuevas antes de borrar las viejas y choca contra
     * uk_permiso_colaborador_fase. Pasa al re-invitar a alguien dado de baja.
     */
    public void reiniciarPermisos() {
        Map<Fase, ColaboradorPermiso> porFase = permisos.stream()
            .collect(Collectors.toMap(ColaboradorPermiso::getFase, permiso -> permiso));

        for (Fase fase : Fase.values()) {
            ColaboradorPermiso permiso = porFase.get(fase);

            if (permiso == null)
                permisos.add(ColaboradorPermiso.builder()
                    .colaborador(this)
                    .fase(fase)
                    .nivel(NivelPermiso.LECTURA)
                    .build());
            else
                permiso.setNivel(NivelPermiso.LECTURA);
        }
    }

    /** Nivel en una fase. LECTURA si por lo que sea no tiene fila para esa fase. */
    public NivelPermiso nivelEn(Fase fase) {
        return permisos.stream()
            .filter(permiso -> permiso.getFase() == fase)
            .findFirst()
            .map(ColaboradorPermiso::getNivel)
            .orElse(NivelPermiso.LECTURA);
    }
}
