import type { Comentario } from "@/features/comentarios/types";
import { apiFetch } from "@/lib/http";

/** Cuelgan de la ruta del paso: el documento se materializa solo y el front no conoce su id. */
function ruta(proyectoId: number, pasoId: number) {
  return `/api/proyectos/${proyectoId}/pasos/${pasoId}/comentarios`;
}

export function getComentarios(proyectoId: number, pasoId: number) {
  return apiFetch<Comentario[]>(ruta(proyectoId, pasoId));
}

export function crearComentario(proyectoId: number, pasoId: number, texto: string) {
  return apiFetch<Comentario>(ruta(proyectoId, pasoId), {
    method: "POST",
    body: { texto },
  });
}

export function eliminarComentario(proyectoId: number, pasoId: number, comentarioId: number) {
  return apiFetch<void>(`${ruta(proyectoId, pasoId)}/${comentarioId}`, { method: "DELETE" });
}
