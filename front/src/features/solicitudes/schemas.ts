import { z } from "zod";

/**
 * Espeja RegisterSolicitudAccesoRequest.java, incluidas las tres reglas condicionales de las
 * opciones "Otro", que del lado del servidor son @AssertTrue.
 */
export const nuevaSolicitudSchema = z
  .object({
    nombre: z
      .string()
      .min(1, "El nombre es obligatorio")
      .max(150, "No puede exceder los 150 caracteres"),
    apellidos: z
      .string()
      .min(1, "Los apellidos son obligatorios")
      .max(150, "No puede exceder los 150 caracteres"),
    email: z
      .email("Ingresá un email válido")
      .max(150, "El email no puede exceder los 150 caracteres"),

    nivelInstruccion: z.enum(
      [
        "GRADO_INCOMPLETO",
        "GRADO_COMPLETO",
        "ESPECIALIZACION",
        "MAESTRIA",
        "DOCTORADO",
        "POSTDOCTORADO",
      ],
      "Elegí tu nivel de instrucción",
    ),
    genero: z.enum(["FEMENINO", "MASCULINO", "PREFIERE_NO_DECIR"], "Elegí una opción"),
    rangoEdad: z.enum(
      ["HASTA_29", "DE_30_A_45", "DE_46_A_60", "DE_61_Y_MAS"],
      "Elegí tu rango de edad",
    ),
    ocupacion: z.enum(
      [
        "ESTUDIANTE",
        "DOCENTE_UNIVERSITARIO",
        "EMPLEADO_PUBLICO",
        "EMPLEADO_ONG",
        "FUNCIONARIO_INTERNACIONAL",
        "LIDER_ORGANIZACION_SOCIAL",
        "OTRA",
      ],
      "Elegí tu ocupación principal",
    ),
    ocupacionOtra: z.string().max(150, "No puede exceder los 150 caracteres").optional(),

    institucion: z
      .string()
      .min(1, "La institución u organización es obligatoria")
      .max(200, "No puede exceder los 200 caracteres"),
    paisNacimiento: z.string().min(1, "Elegí tu país de nacimiento"),
    paisResidencia: z.string().optional(),

    motivacion: z
      .string()
      .min(1, "Contanos por qué te interesa el Modelo PipoE")
      .max(1000, "No puede exceder los 1000 caracteres"),

    usos: z
      .array(
        z.enum([
          "TAREA_CURSO",
          "PREPARAR_CLASE",
          "TRABAJO_INSTITUCIONAL",
          "ACCION_COMUNITARIA",
          "OTRO",
        ]),
      )
      .min(1, "Elegí al menos un uso"),
    usosOtro: z.string().max(150, "No puede exceder los 150 caracteres").optional(),

    canales: z
      .array(
        z.enum([
          "LIBRO",
          "ARTICULO",
          "SITIO_WEB",
          "DOCENTE",
          "AMISTAD",
          "REDES_SOCIALES",
          "OTRO",
        ]),
      )
      .min(1, "Contanos cómo te enteraste"),
    canalOtro: z.string().max(150, "No puede exceder los 150 caracteres").optional(),
  })
  .refine((valores) => valores.ocupacion !== "OTRA" || Boolean(valores.ocupacionOtra?.trim()), {
    message: "Especificá cuál es tu ocupación",
    path: ["ocupacionOtra"],
  })
  .refine((valores) => !valores.usos.includes("OTRO") || Boolean(valores.usosOtro?.trim()), {
    message: "Especificá qué otro uso le vas a dar",
    path: ["usosOtro"],
  })
  .refine((valores) => !valores.canales.includes("OTRO") || Boolean(valores.canalOtro?.trim()), {
    message: "Especificá por qué otro medio te enteraste",
    path: ["canalOtro"],
  });

export type NuevaSolicitudValues = z.infer<typeof nuevaSolicitudSchema>;
