import type { NuevaSolicitudValues } from "@/features/solicitudes/schemas";
import type { EstadoSolicitud, SolicitudAcceso } from "@/features/solicitudes/types";
import { apiFetch } from "@/lib/http";
import type { Page } from "@/types/page";

/** Endpoint público. La API aplica rate limit por IP (3 por hora). */
export function crearSolicitud(values: NuevaSolicitudValues) {
  return apiFetch<SolicitudAcceso>("/api/solicitudes-acceso", {
    method: "POST",
    body: values,
    auth: false,
  });
}

/** Filtros del listado del admin. Todos opcionales y combinables. */
export type FiltrosSolicitudes = {
  estado: EstadoSolicitud | null;
  /** Busca en nombre, apellidos, email e institución. */
  texto: string;
  /** Fechas en formato YYYY-MM-DD, que es lo que devuelve un <input type="date">. */
  desde: string;
  hasta: string;
};

export const FILTROS_VACIOS: FiltrosSolicitudes = {
  estado: "PENDIENTE",
  texto: "",
  desde: "",
  hasta: "",
};

export function getSolicitudes(filtros: FiltrosSolicitudes, page: number) {
  const params = new URLSearchParams({ page: String(page) });

  if (filtros.estado) params.set("estado", filtros.estado);
  if (filtros.texto.trim()) params.set("texto", filtros.texto.trim());
  if (filtros.desde) params.set("desde", filtros.desde);
  if (filtros.hasta) params.set("hasta", filtros.hasta);

  return apiFetch<Page<SolicitudAcceso>>(`/api/solicitudes-acceso?${params}`);
}

/** Crea el usuario y le manda la clave por mail. */
export function aprobarSolicitud(id: number) {
  return apiFetch<SolicitudAcceso>(`/api/solicitudes-acceso/${id}/aprobar`, {
    method: "POST",
  });
}

/** Sólo marca el estado: la API no manda ningún mail al rechazar. */
export function rechazarSolicitud(id: number) {
  return apiFetch<SolicitudAcceso>(`/api/solicitudes-acceso/${id}/rechazar`, {
    method: "POST",
  });
}
