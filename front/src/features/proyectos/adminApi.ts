import type { Fase } from "@/features/pipoe/types";
import { apiFetch } from "@/lib/http";
import type { Page } from "@/types/page";

/** Espeja dominio/proyectos/dtos/EstadoFase.java. */
export type EstadoFase = "SIN_EMPEZAR" | "EN_PROGRESO" | "COMPLETA";

/** Espeja dominio/proyectos/dtos/AdminProyectoResponse.java. */
export type ProyectoAdmin = {
  id: number;
  nombre: string;
  codigo: string;
  duenioId: number;
  duenio: string;
  colaboradores: number;
  /** Siempre las 5 fases. */
  fases: Record<Fase, EstadoFase>;
  terminado: boolean;
};

export type FiltrosProyectos = {
  /** Busca en el nombre del proyecto, su código y el nombre del dueño. */
  texto: string;
  /** null trae todos; true sólo los terminados; false los que siguen en curso. */
  terminado: boolean | null;
};

export const FILTROS_PROYECTOS_VACIOS: FiltrosProyectos = {
  texto: "",
  terminado: null,
};

export function getProyectosAdmin(filtros: FiltrosProyectos, page: number) {
  const params = new URLSearchParams({ page: String(page) });

  if (filtros.texto.trim()) params.set("texto", filtros.texto.trim());
  if (filtros.terminado !== null) params.set("terminado", String(filtros.terminado));

  return apiFetch<Page<ProyectoAdmin>>(`/api/admin/proyectos?${params}`);
}
