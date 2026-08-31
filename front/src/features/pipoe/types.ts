import type { NivelPermiso } from "@/features/proyectos/types";

/** Espeja dominio/pasos/entities/Fase.java. El orden es el de presentación. */
export type Fase =
  | "PROMOCION"
  | "INDAGACION"
  | "PROGRAMACION"
  | "ORGANIZACION"
  | "EVALUACION";

export const FASES: Fase[] = [
  "PROMOCION",
  "INDAGACION",
  "PROGRAMACION",
  "ORGANIZACION",
  "EVALUACION",
];

/** Espeja dominio/documentos/entities/EstadoPaso.java. */
export type EstadoPaso = "PENDIENTE" | "EN_PROGRESO" | "COMPLETADO";

export type FaseResumen = {
  fase: Fase;
  nombre: string;
  ideaCentral: string;
  orden: number;
  producto: string;
  productoCompletado: boolean;
  productoHabilitado: boolean;
  /** No incluye al producto. */
  totalPasos: number;
  pasosCompletados: number;
  nivel: NivelPermiso;
};

export type PasoResumen = {
  pasoId: number;
  orden: number;
  tituloCorto: string;
  titulo: string;
  esProducto: boolean;
  estado: EstadoPaso;
  puedeCompletarse: boolean;
  motivoBloqueo: string | null;
};

export type PasoDetalle = {
  pasoId: number;
  fase: Fase;
  faseNombre: string;
  orden: number;
  titulo: string;
  tituloCorto: string;
  esProducto: boolean;
  explicacion: string;
  ejemplo: string;
  contenido: string;
  version: number;
  estado: EstadoPaso;
  puedeCompletarse: boolean;
  motivoBloqueo: string | null;
  actualizadoEn: string | null;
  actualizadoPor: string | null;
  nivel: NivelPermiso;
  puedeEditar: boolean;
  /** Nombre de otra persona con el paso abierto, o null. */
  editandoOtro: string | null;
};

/**
 * Una sesión de escritura del historial, no un guardado suelto: el backend fusiona los
 * guardados seguidos de una misma persona. Espeja dominio/documentos/dtos/VersionResponse.java.
 */
export type VersionDocumento = {
  id: number;
  autor: string;
  autorTipo: "USUARIO" | "COLABORADOR";
  /** Cuándo empezó la sesión. */
  creadoEn: string;
  /** Cuándo fue el último guardado de la sesión. Igual a creadoEn si hubo uno solo. */
  actualizadoEn: string;
  /** Cuántos guardados se fusionaron en esta entrada. */
  guardados: number;
  largo: number;
  /** Palabras que esta sesión sumó respecto de la anterior. */
  palabrasAgregadas: number;
  /** Palabras que esta sesión borró respecto de la anterior. */
  palabrasQuitadas: number;
};

export type TipoSegmento = "IGUAL" | "AGREGADO" | "QUITADO";

/** Espeja dominio/documentos/dtos/VersionDiffResponse.java. */
export type DiffVersion = {
  id: number;
  autor: string;
  autorTipo: "USUARIO" | "COLABORADOR";
  creadoEn: string;
  actualizadoEn: string;
  guardados: number;
  palabrasAgregadas: number;
  palabrasQuitadas: number;
  segmentos: { tipo: TipoSegmento; texto: string }[];
};

/** Los dos formatos en que se puede bajar el producto de una fase. */
export type FormatoExportacion = "docx" | "pdf";

/** Espeja dominio/pasos/dtos/PasoCatalogoResponse.java. */
export type PasoCatalogo = {
  id: number;
  fase: Fase;
  faseNombre: string;
  orden: number;
  titulo: string;
  tituloCorto: string;
  explicacion: string;
  ejemplo: string;
  esProducto: boolean;
  contenidoCargado: boolean;
};

/** Colores de cada estado, compartidos entre el diagrama de flujo y el detalle del paso. */
export const ESTADO_CLASES: Record<EstadoPaso, string> = {
  PENDIENTE: "border-slate-300 bg-white text-slate-500",
  EN_PROGRESO: "border-amber-400 bg-amber-50 text-amber-900",
  COMPLETADO: "border-green-500 bg-green-50 text-green-900",
};

export const ESTADO_ETIQUETAS: Record<EstadoPaso, string> = {
  PENDIENTE: "Sin empezar",
  EN_PROGRESO: "En progreso",
  COMPLETADO: "Completado",
};
