export const ESTADOS_SOLICITUD = ["PENDIENTE", "APROBADA", "RECHAZADA"] as const;

export type EstadoSolicitud = (typeof ESTADOS_SOLICITUD)[number];

export type NivelInstruccion =
  | "GRADO_INCOMPLETO"
  | "GRADO_COMPLETO"
  | "ESPECIALIZACION"
  | "MAESTRIA"
  | "DOCTORADO"
  | "POSTDOCTORADO";

export type Genero = "FEMENINO" | "MASCULINO" | "PREFIERE_NO_DECIR";

export type RangoEdad = "HASTA_29" | "DE_30_A_45" | "DE_46_A_60" | "DE_61_Y_MAS";

export type Ocupacion =
  | "ESTUDIANTE"
  | "DOCENTE_UNIVERSITARIO"
  | "EMPLEADO_PUBLICO"
  | "EMPLEADO_ONG"
  | "FUNCIONARIO_INTERNACIONAL"
  | "LIDER_ORGANIZACION_SOCIAL"
  | "OTRA";

export type UsoPrevisto =
  | "TAREA_CURSO"
  | "PREPARAR_CLASE"
  | "TRABAJO_INSTITUCIONAL"
  | "ACCION_COMUNITARIA"
  | "OTRO";

export type CanalDifusion =
  | "LIBRO"
  | "ARTICULO"
  | "SITIO_WEB"
  | "DOCENTE"
  | "AMISTAD"
  | "REDES_SOCIALES"
  | "OTRO";

/**
 * Etiquetas de cada opción, en el orden en que se muestran.
 *
 * Viven acá y no en la API porque son texto de interfaz: la API guarda y devuelve el nombre
 * del enum. El formulario y la pantalla de solicitudes leen las dos de la misma fuente.
 */
export const NIVELES_INSTRUCCION: { valor: NivelInstruccion; etiqueta: string }[] = [
  { valor: "GRADO_INCOMPLETO", etiqueta: "Grado universitario incompleto" },
  { valor: "GRADO_COMPLETO", etiqueta: "Grado universitario completo" },
  { valor: "ESPECIALIZACION", etiqueta: "Especialización" },
  { valor: "MAESTRIA", etiqueta: "Maestría" },
  { valor: "DOCTORADO", etiqueta: "Doctorado" },
  { valor: "POSTDOCTORADO", etiqueta: "Postdoctorado" },
];

export const GENEROS: { valor: Genero; etiqueta: string }[] = [
  { valor: "FEMENINO", etiqueta: "Femenino" },
  { valor: "MASCULINO", etiqueta: "Masculino" },
  { valor: "PREFIERE_NO_DECIR", etiqueta: "Prefiere no decir" },
];

export const RANGOS_EDAD: { valor: RangoEdad; etiqueta: string }[] = [
  { valor: "HASTA_29", etiqueta: "29 o menos" },
  { valor: "DE_30_A_45", etiqueta: "De 30 a 45" },
  { valor: "DE_46_A_60", etiqueta: "De 46 a 60" },
  { valor: "DE_61_Y_MAS", etiqueta: "De 61 y más" },
];

export const OCUPACIONES: { valor: Ocupacion; etiqueta: string }[] = [
  { valor: "ESTUDIANTE", etiqueta: "Estudiante" },
  { valor: "DOCENTE_UNIVERSITARIO", etiqueta: "Docente universitario" },
  { valor: "EMPLEADO_PUBLICO", etiqueta: "Empleado/a público/a" },
  { valor: "EMPLEADO_ONG", etiqueta: "Empleado/a de organización no gubernamental" },
  { valor: "FUNCIONARIO_INTERNACIONAL", etiqueta: "Funcionario/a de organismo internacional" },
  { valor: "LIDER_ORGANIZACION_SOCIAL", etiqueta: "Líder de organizaciones sociales" },
  { valor: "OTRA", etiqueta: "Otra" },
];

export const USOS_PREVISTOS: { valor: UsoPrevisto; etiqueta: string }[] = [
  { valor: "TAREA_CURSO", etiqueta: "Tarea de un curso universitario" },
  { valor: "PREPARAR_CLASE", etiqueta: "Preparar una clase para docencia" },
  { valor: "TRABAJO_INSTITUCIONAL", etiqueta: "Realizar un trabajo institucional" },
  { valor: "ACCION_COMUNITARIA", etiqueta: "Utilizarlo en la preparación de una acción comunitaria" },
  { valor: "OTRO", etiqueta: "Otro" },
];

export const CANALES_DIFUSION: { valor: CanalDifusion; etiqueta: string }[] = [
  { valor: "LIBRO", etiqueta: "Libro «Cómo hacer Planificación Situacional aprendiendo»" },
  { valor: "ARTICULO", etiqueta: "Artículo «Repensando la Planificación como método de trabajo»" },
  { valor: "SITIO_WEB", etiqueta: "Hoja web arlettepichardo.com" },
  { valor: "DOCENTE", etiqueta: "Consejo de un/a docente" },
  { valor: "AMISTAD", etiqueta: "Recomendación de una amistad" },
  { valor: "REDES_SOCIALES", etiqueta: "Redes sociales" },
  { valor: "OTRO", etiqueta: "Otro" },
];

/** Busca la etiqueta de un valor. Devuelve un guion cuando el dato no existe. */
export function etiquetaDe<T extends string>(
  opciones: { valor: T; etiqueta: string }[],
  valor: T | null | undefined,
) {
  if (!valor) return "—";
  return opciones.find((opcion) => opcion.valor === valor)?.etiqueta ?? valor;
}

/** Espeja dominio/solicitudes/dtos/SolicitudAccesoResponse.java. */
export type SolicitudAcceso = {
  id: number;
  nombre: string;
  apellidos: string | null;
  nombreCompleto: string;
  email: string;
  /** Null en las solicitudes anteriores a la ampliación del formulario. */
  nivelInstruccion: NivelInstruccion | null;
  genero: Genero | null;
  rangoEdad: RangoEdad | null;
  ocupacion: Ocupacion | null;
  ocupacionOtra: string | null;
  institucion: string;
  paisNacimiento: string;
  /** Sólo si es distinto al de nacimiento. */
  paisResidencia: string | null;
  motivacion: string;
  usos: UsoPrevisto[];
  usosOtro: string | null;
  canales: CanalDifusion[];
  canalOtro: string | null;
  estado: EstadoSolicitud;
  /** ISO-8601 sin offset (LocalDateTime de Java). */
  fechaSolicitud: string;
  /** null mientras la solicitud está pendiente. */
  fechaResolucion: string | null;
};
