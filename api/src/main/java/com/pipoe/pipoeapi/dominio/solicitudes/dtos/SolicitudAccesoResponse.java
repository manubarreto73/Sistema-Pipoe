package com.pipoe.pipoeapi.dominio.solicitudes.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.pipoe.pipoeapi.dominio.solicitudes.entities.CanalDifusion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.EstadoSolicitud;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.Genero;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.NivelInstruccion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.Ocupacion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.RangoEdad;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.SolicitudAcceso;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.UsoPrevisto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SolicitudAccesoResponse {

    private Long id;
    private String nombre;
    private String apellidos;
    /** Nombre y apellidos juntos: es el nombre con el que se crea el usuario al aprobar. */
    private String nombreCompleto;
    private String email;

    // Null en las solicitudes anteriores a la ampliación del formulario (V9).
    private NivelInstruccion nivelInstruccion;
    private Genero genero;
    private RangoEdad rangoEdad;
    private Ocupacion ocupacion;
    private String ocupacionOtra;

    private String institucion;
    private String paisNacimiento;
    private String paisResidencia;
    private String motivacion;

    private List<UsoPrevisto> usos;
    private String usosOtro;
    private List<CanalDifusion> canales;
    private String canalOtro;

    private EstadoSolicitud estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;  // null mientras está pendiente

    public static SolicitudAccesoResponse from(SolicitudAcceso solicitud) {
        return SolicitudAccesoResponse.builder()
            .id(solicitud.getId())
            .nombre(solicitud.getNombre())
            .apellidos(solicitud.getApellidos())
            .nombreCompleto(solicitud.nombreCompleto())
            .email(solicitud.getEmail())
            .nivelInstruccion(solicitud.getNivelInstruccion())
            .genero(solicitud.getGenero())
            .rangoEdad(solicitud.getRangoEdad())
            .ocupacion(solicitud.getOcupacion())
            .ocupacionOtra(solicitud.getOcupacionOtra())
            .institucion(solicitud.getInstitucion())
            .paisNacimiento(solicitud.getPaisNacimiento())
            .paisResidencia(solicitud.getPaisResidencia())
            .motivacion(solicitud.getMotivacion())
            .usos(ordenar(solicitud.getUsos(), UsoPrevisto.values()))
            .usosOtro(solicitud.getUsosOtro())
            .canales(ordenar(solicitud.getCanales(), CanalDifusion.values()))
            .canalOtro(solicitud.getCanalOtro())
            .estado(solicitud.getEstado())
            .fechaSolicitud(solicitud.getFechaSolicitud())
            .fechaResolucion(solicitud.getFechaResolucion())
            .build();
    }

    /** El Set de JPA no garantiza orden; se devuelve en el del enum para que la UI sea estable. */
    private static <T extends Enum<T>> List<T> ordenar(Set<T> valores, T[] todos) {
        if (valores == null) return List.of();

        return java.util.Arrays.stream(todos).filter(valores::contains).toList();
    }
}
