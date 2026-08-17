import type { CuposValues } from "@/features/parametros/schemas";
import type { Configuracion } from "@/features/parametros/types";
import { apiFetch } from "@/lib/http";

/** Límites de la app (máximos, cantidad de fases). Los usa la UI; la API los aplica igual. */
export function getConfiguracion() {
  return apiFetch<Configuracion>("/api/parametros");
}

/** Sólo ADMIN: los cupos los fija la dueña de la aplicación. */
export function actualizarConfiguracion(values: CuposValues) {
  return apiFetch<Configuracion>("/api/parametros", { method: "PUT", body: values });
}
