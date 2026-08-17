import type {
  DiffVersion,
  Fase,
  FaseResumen,
  FormatoExportacion,
  PasoCatalogo,
  PasoDetalle,
  PasoResumen,
  VersionDocumento,
} from "@/features/pipoe/types";
import { apiDownload, apiFetch } from "@/lib/http";

/** Las 5 fases con su progreso. Una sola request para toda la navegación del proyecto. */
export function getFases(proyectoId: number) {
  return apiFetch<FaseResumen[]>(`/api/proyectos/${proyectoId}/fases`);
}

export function getPasosDeFase(proyectoId: number, fase: Fase) {
  return apiFetch<PasoResumen[]>(`/api/proyectos/${proyectoId}/fases/${fase}/pasos`);
}

export function getPaso(proyectoId: number, pasoId: number) {
  return apiFetch<PasoDetalle>(`/api/proyectos/${proyectoId}/pasos/${pasoId}`);
}

/** `version` es la que devolvió la última lectura o guardado. Si quedó vieja, la API da 409. */
export function guardarDocumento(
  proyectoId: number,
  pasoId: number,
  contenido: string,
  version: number,
) {
  return apiFetch<PasoDetalle>(`/api/proyectos/${proyectoId}/pasos/${pasoId}/documento`, {
    method: "PUT",
    body: { contenido, version },
  });
}

export function marcarCompletado(
  proyectoId: number,
  pasoId: number,
  completado: boolean,
) {
  return apiFetch<PasoDetalle>(`/api/proyectos/${proyectoId}/pasos/${pasoId}/completado`, {
    method: "PUT",
    body: { completado },
  });
}

export function getVersiones(proyectoId: number, pasoId: number) {
  return apiFetch<VersionDocumento[]>(
    `/api/proyectos/${proyectoId}/pasos/${pasoId}/versiones`,
  );
}

/** Qué agregó y qué quitó ese guardado. Sólo lo puede pedir el dueño del proyecto. */
export function getDiffVersion(proyectoId: number, pasoId: number, versionId: number) {
  return apiFetch<DiffVersion>(
    `/api/proyectos/${proyectoId}/pasos/${pasoId}/versiones/${versionId}`,
  );
}

/** Descarga del producto de una fase. Devuelve el archivo ya en memoria, con su nombre. */
export function exportarProducto(
  proyectoId: number,
  pasoId: number,
  formato: FormatoExportacion,
) {
  return apiDownload(
    `/api/proyectos/${proyectoId}/pasos/${pasoId}/exportar?formato=${formato}`,
  );
}

/** Latido mientras el paso está abierto para editar. Devuelve quién más lo tiene abierto. */
export function latirPresencia(proyectoId: number, pasoId: number) {
  return apiFetch<{ editandoOtro: string | null }>(
    `/api/proyectos/${proyectoId}/pasos/${pasoId}/presencia`,
    { method: "POST" },
  );
}

// ------------------------------------------------------------------ catálogo (ADMIN)

export function getCatalogo() {
  return apiFetch<PasoCatalogo[]>("/api/catalogo/pasos");
}

export function actualizarPasoCatalogo(
  pasoId: number,
  values: { explicacion: string; ejemplo: string; tituloCorto: string },
) {
  return apiFetch<PasoCatalogo>(`/api/catalogo/pasos/${pasoId}`, {
    method: "PUT",
    body: values,
  });
}
