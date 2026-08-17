import type {
  NuevoColaboradorValues,
  NuevoProyectoValues,
} from "@/features/proyectos/schemas";
import type { Colaborador, Permiso, Proyecto } from "@/features/proyectos/types";
import { apiFetch } from "@/lib/http";

/** Sólo para usuarios con rol USER: un colaborador o un admin reciben 403 acá. */
export function getProyectos() {
  return apiFetch<Proyecto[]>("/api/proyectos");
}

/** Accesible por el dueño, o por el colaborador cuyo token tenga ese proyectoId. */
export function getProyecto(proyectoId: number) {
  return apiFetch<Proyecto>(`/api/proyectos/${proyectoId}`);
}

export function crearProyecto(values: NuevoProyectoValues) {
  return apiFetch<Proyecto>("/api/proyectos", { method: "POST", body: values });
}

/** Cambia el nombre. Sigue siendo único en toda la app, así que puede dar 400. */
export function renombrarProyecto(proyectoId: number, values: NuevoProyectoValues) {
  return apiFetch<Proyecto>(`/api/proyectos/${proyectoId}`, { method: "PUT", body: values });
}

/** Borrado físico: se lleva puestos a los colaboradores del proyecto. */
export function eliminarProyecto(proyectoId: number) {
  return apiFetch<void>(`/api/proyectos/${proyectoId}`, { method: "DELETE" });
}

/** Sólo el dueño del proyecto. Devuelve únicamente los colaboradores activos. */
export function getColaboradores(proyectoId: number) {
  return apiFetch<Colaborador[]>(`/api/proyectos/${proyectoId}/colaboradores`);
}

export function crearColaborador(proyectoId: number, values: NuevoColaboradorValues) {
  return apiFetch<Colaborador>(`/api/proyectos/${proyectoId}/colaboradores`, {
    method: "POST",
    body: values,
  });
}

/** Reemplaza el mapa completo de permisos: la API espera las 5 fases, no un parche. */
export function actualizarPermisos(
  proyectoId: number,
  colaboradorId: number,
  permisos: Permiso[],
) {
  return apiFetch<Colaborador>(
    `/api/proyectos/${proyectoId}/colaboradores/${colaboradorId}/permisos`,
    { method: "PUT", body: { permisos } },
  );
}

/** Baja lógica: el colaborador deja de poder entrar. */
export function eliminarColaborador(proyectoId: number, colaboradorId: number) {
  return apiFetch<void>(`/api/proyectos/${proyectoId}/colaboradores/${colaboradorId}`, {
    method: "DELETE",
  });
}
