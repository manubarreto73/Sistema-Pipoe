package com.pipoe.pipoeapi.dominio.solicitudes.dtos.request;

import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.CanalDifusion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.Genero;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.NivelInstruccion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.Ocupacion;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.RangoEdad;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.SolicitudAcceso;
import com.pipoe.pipoeapi.dominio.solicitudes.entities.UsoPrevisto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RegisterSolicitudAccesoRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    private String nombre;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 150, message = "Los apellidos no pueden exceder los 150 caracteres")
    private String apellidos;

    @NotBlank
    @Email
    @Size(max = 150, message = "El email no puede exceder los 150 caracteres")
    private String email;

    @NotNull(message = "El nivel de instrucción es obligatorio")
    private NivelInstruccion nivelInstruccion;

    @NotNull(message = "El género es obligatorio")
    private Genero genero;

    @NotNull(message = "La edad es obligatoria")
    private RangoEdad rangoEdad;

    @NotNull(message = "La ocupación es obligatoria")
    private Ocupacion ocupacion;

    @Size(max = 150, message = "No puede exceder los 150 caracteres")
    private String ocupacionOtra;

    @NotBlank(message = "La institución u organización es obligatoria")
    @Size(max = 200, message = "No puede exceder los 200 caracteres")
    private String institucion;

    @NotBlank(message = "El país de nacimiento es obligatorio")
    @Size(max = 100, message = "No puede exceder los 100 caracteres")
    private String paisNacimiento;

    @Size(max = 100, message = "No puede exceder los 100 caracteres")
    private String paisResidencia;

    @NotBlank(message = "Contanos por qué te interesa el Modelo PipoE")
    @Size(max = 1000, message = "No puede exceder los 1000 caracteres")
    private String motivacion;

    @NotEmpty(message = "Elegí al menos un uso")
    @Builder.Default
    private Set<UsoPrevisto> usos = new LinkedHashSet<>();

    @Size(max = 150, message = "No puede exceder los 150 caracteres")
    private String usosOtro;

    @NotEmpty(message = "Contanos cómo te enteraste")
    @Builder.Default
    private Set<CanalDifusion> canales = new LinkedHashSet<>();

    @Size(max = 150, message = "No puede exceder los 150 caracteres")
    private String canalOtro;

    // Las tres opciones "Otro" piden un texto que las explique. Se valida acá y no en el
    // frontend solamente, porque el endpoint es público y entra cualquiera.

    @JsonIgnore
    @AssertTrue(message = "Especificá cuál es tu ocupación")
    public boolean isOcupacionOtraValida() {
        return ocupacion != Ocupacion.OTRA || (ocupacionOtra != null && !ocupacionOtra.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "Especificá qué otro uso le vas a dar")
    public boolean isUsosOtroValido() {
        return usos == null || !usos.contains(UsoPrevisto.OTRO)
            || (usosOtro != null && !usosOtro.isBlank());
    }

    @JsonIgnore
    @AssertTrue(message = "Especificá por qué otro medio te enteraste")
    public boolean isCanalOtroValido() {
        return canales == null || !canales.contains(CanalDifusion.OTRO)
            || (canalOtro != null && !canalOtro.isBlank());
    }

    // estado y fechaSolicitud los pone @PrePersist.
    public SolicitudAcceso toEntity() {
        return SolicitudAcceso.builder()
            .nombre(this.nombre)
            .apellidos(this.apellidos)
            .email(this.email)
            .nivelInstruccion(this.nivelInstruccion)
            .genero(this.genero)
            .rangoEdad(this.rangoEdad)
            .ocupacion(this.ocupacion)
            .ocupacionOtra(this.ocupacion == Ocupacion.OTRA ? this.ocupacionOtra : null)
            .institucion(this.institucion)
            .paisNacimiento(this.paisNacimiento)
            // Guardar el mismo país dos veces sólo agrega ruido al listado del admin.
            .paisResidencia(
                this.paisResidencia != null && !this.paisResidencia.equals(this.paisNacimiento)
                    ? this.paisResidencia
                    : null
            )
            .motivacion(this.motivacion)
            .usos(new LinkedHashSet<>(this.usos))
            .usosOtro(this.usos.contains(UsoPrevisto.OTRO) ? this.usosOtro : null)
            .canales(new LinkedHashSet<>(this.canales))
            .canalOtro(this.canales.contains(CanalDifusion.OTRO) ? this.canalOtro : null)
            .build();
    }
}
