package com.pipoe.pipoeapi.dominio.solicitudes.entities;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

/**
 * Pedido de acceso que deja un visitante desde la página pública.
 * El admin lo aprueba (se crea el Usuario y le llega la clave por mail) o lo rechaza.
 *
 * No hay soft delete: el ciclo de vida lo lleva {@link EstadoSolicitud}, y la fila resuelta
 * se conserva como historial para saber a quién ya se atendió.
 *
 * Los campos del cuestionario son nullable a nivel base porque las solicitudes anteriores a
 * V9 no los tienen. Para las nuevas, la obligatoriedad la exige el DTO de entrada.
 */
@Entity
@Table(name = "solicitudes_acceso")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SolicitudAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "solicitud_acceso_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String apellidos;

    // Sin unique: tras un rechazo se puede volver a pedir. Que no haya dos PENDIENTES
    // del mismo email lo valida SolicitudAccesoService.
    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_instruccion", length = 30)
    private NivelInstruccion nivelInstruccion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genero genero;

    @Enumerated(EnumType.STRING)
    @Column(name = "rango_edad", length = 20)
    private RangoEdad rangoEdad;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Ocupacion ocupacion;

    /** Sólo cuando ocupacion es OTRA. */
    @Column(name = "ocupacion_otra", length = 150)
    private String ocupacionOtra;

    /** Nombre completo de la institución y su país. */
    @Column(nullable = false, length = 200)
    private String institucion;

    @Column(name = "pais_nacimiento", nullable = false, length = 100)
    private String paisNacimiento;

    /** Sólo si es distinto al de nacimiento. */
    @Column(name = "pais_residencia", length = 100)
    private String paisResidencia;

    /** "¿Por qué le interesa el Modelo PipoE?" */
    @Column(nullable = false, length = 1000)
    private String motivacion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "solicitud_usos",
        joinColumns = @JoinColumn(name = "solicitud_acceso_id")
    )
    @Column(name = "uso", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UsoPrevisto> usos = new LinkedHashSet<>();

    @Column(name = "usos_otro", length = 150)
    private String usosOtro;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "solicitud_canales",
        joinColumns = @JoinColumn(name = "solicitud_acceso_id")
    )
    @Column(name = "canal", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<CanalDifusion> canales = new LinkedHashSet<>();

    @Column(name = "canal_otro", length = 150)
    private String canalOtro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @PrePersist
    void onCreate() {
        this.estado = EstadoSolicitud.PENDIENTE;
        this.fechaSolicitud = LocalDateTime.now();
    }

    /** Nombre para el alta del usuario al aprobar. Las solicitudes viejas no tienen apellidos. */
    public String nombreCompleto() {
        return apellidos == null || apellidos.isBlank() ? nombre : nombre + " " + apellidos;
    }
}
