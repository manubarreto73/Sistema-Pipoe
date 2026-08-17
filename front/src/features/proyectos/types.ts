import type { Fase } from "@/features/pipoe/types";

/** Espeja dominio/proyectos/dtos/ProyectoResponse.java. */
export type Proyecto = {
  id: number;
  nombre: string;
  usuarioId: number;
  usuarioNombreCompleto: string;
};

/** Espeja dominio/colaboradores/entities/NivelPermiso.java, de menos a más permisivo. */
export type NivelPermiso = "LECTURA" | "COMENTARIOS" | "EDICION";

/** Espeja dominio/colaboradores/dtos/PermisoResponse.java. */
export type Permiso = {
  fase: Fase;
  /** Nombre legible ("Indagación"): lo manda la API para no duplicar el diccionario acá. */
  faseNombre: string;
  nivel: NivelPermiso;
};

/** Espeja dominio/colaboradores/dtos/ColaboradorResponse.java. */
export type Colaborador = {
  id: number;
  nombre: string;
  email: string;
  proyectoId: number;
  proyectoNombre: string;
  /** Siempre las 5 fases, ordenadas. */
  permisos: Permiso[];
};

/** Etiquetas de los niveles, en el orden en que se ofrecen al elegir. */
export const NIVELES: { valor: NivelPermiso; etiqueta: string; descripcion: string }[] = [
  { valor: "LECTURA", etiqueta: "Sólo lectura", descripcion: "Puede ver la fase" },
  { valor: "COMENTARIOS", etiqueta: "Comentarios", descripcion: "Puede ver y comentar" },
  { valor: "EDICION", etiqueta: "Edición", descripcion: "Puede ver, comentar y editar" },
];
