import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  actualizarPasoCatalogo,
  exportarProducto,
  getCatalogo,
  getDiffVersion,
  getFases,
  getPaso,
  getPasosDeFase,
  getVersiones,
  guardarDocumento,
  marcarCompletado,
} from "@/features/pipoe/api";
import type { Fase, FormatoExportacion, PasoDetalle } from "@/features/pipoe/types";
import { proyectosKeys } from "@/features/proyectos/hooks";
import { guardarArchivo } from "@/lib/http";

export const pipoeKeys = {
  fases: (proyectoId: number) => ["proyectos", proyectoId, "fases"] as const,
  pasosDeFase: (proyectoId: number, fase: Fase) =>
    ["proyectos", proyectoId, "fases", fase] as const,
  paso: (proyectoId: number, pasoId: number) =>
    ["proyectos", proyectoId, "pasos", pasoId] as const,
  versiones: (proyectoId: number, pasoId: number) =>
    ["proyectos", proyectoId, "pasos", pasoId, "versiones"] as const,
  // Fuera del prefijo de `versiones` a propósito: invalidar el listado no tiene por qué
  // invalidar un diff, que describe un guardado ya cerrado y no cambia nunca.
  diff: (proyectoId: number, pasoId: number, versionId: number) =>
    ["proyectos", proyectoId, "pasos", pasoId, "diff", versionId] as const,
  catalogo: ["catalogo"] as const,
};

export function useFases(proyectoId: number) {
  return useQuery({
    queryKey: pipoeKeys.fases(proyectoId),
    queryFn: () => getFases(proyectoId),
    enabled: Number.isFinite(proyectoId),
  });
}

export function usePasosDeFase(proyectoId: number, fase: Fase | undefined) {
  return useQuery({
    queryKey: pipoeKeys.pasosDeFase(proyectoId, fase as Fase),
    queryFn: () => getPasosDeFase(proyectoId, fase as Fase),
    enabled: Number.isFinite(proyectoId) && Boolean(fase),
  });
}

export function usePaso(proyectoId: number, pasoId: number) {
  return useQuery({
    queryKey: pipoeKeys.paso(proyectoId, pasoId),
    queryFn: () => getPaso(proyectoId, pasoId),
    enabled: Number.isFinite(proyectoId) && Number.isFinite(pasoId),
  });
}

export function useVersiones(proyectoId: number, pasoId: number, habilitado: boolean) {
  return useQuery({
    queryKey: pipoeKeys.versiones(proyectoId, pasoId),
    queryFn: () => getVersiones(proyectoId, pasoId),
    enabled: habilitado && Number.isFinite(pasoId),
  });
}

export function useDiffVersion(
  proyectoId: number,
  pasoId: number,
  versionId: number | null,
) {
  return useQuery({
    queryKey: pipoeKeys.diff(proyectoId, pasoId, versionId ?? 0),
    queryFn: () => getDiffVersion(proyectoId, pasoId, versionId as number),
    enabled: versionId !== null,
    // Una versión guardada no cambia nunca: una vez traído el diff, no hay nada que revalidar.
    staleTime: Infinity,
  });
}

/** Baja el archivo y dispara el "guardar como". No pasa por la caché: no hay nada que cachear. */
export function useExportarProducto(proyectoId: number, pasoId: number) {
  return useMutation({
    mutationFn: (formato: FormatoExportacion) =>
      exportarProducto(proyectoId, pasoId, formato),
    onSuccess: guardarArchivo,
  });
}

/**
 * Guardar cambia el estado del paso, y con él el progreso de la fase y del proyecto. En vez
 * de invalidar todo, se escribe el detalle que devolvió la API y se invalidan los agregados.
 */
function useSincronizarPaso(proyectoId: number) {
  const queryClient = useQueryClient();

  return (detalle: PasoDetalle) => {
    queryClient.setQueryData(pipoeKeys.paso(proyectoId, detalle.pasoId), detalle);
    queryClient.invalidateQueries({ queryKey: pipoeKeys.fases(proyectoId) });
    queryClient.invalidateQueries({
      queryKey: pipoeKeys.pasosDeFase(proyectoId, detalle.fase),
    });
    queryClient.invalidateQueries({
      queryKey: pipoeKeys.versiones(proyectoId, detalle.pasoId),
    });
  };
}

export function useGuardarDocumento(proyectoId: number, pasoId: number) {
  const sincronizar = useSincronizarPaso(proyectoId);

  return useMutation({
    mutationFn: ({ contenido, version }: { contenido: string; version: number }) =>
      guardarDocumento(proyectoId, pasoId, contenido, version),
    onSuccess: sincronizar,
  });
}

export function useCompletarPaso(proyectoId: number, pasoId: number) {
  const sincronizar = useSincronizarPaso(proyectoId);

  return useMutation({
    mutationFn: (completado: boolean) => marcarCompletado(proyectoId, pasoId, completado),
    onSuccess: sincronizar,
  });
}

// ------------------------------------------------------------------ catálogo (ADMIN)

export function useCatalogo() {
  return useQuery({ queryKey: pipoeKeys.catalogo, queryFn: getCatalogo });
}

export function useActualizarPasoCatalogo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      pasoId,
      ...values
    }: {
      pasoId: number;
      explicacion: string;
      ejemplo: string;
      tituloCorto: string;
    }) => actualizarPasoCatalogo(pasoId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: pipoeKeys.catalogo });
      // El detalle de cualquier paso muestra la explicación: quedó vieja en toda la app.
      queryClient.invalidateQueries({ queryKey: proyectosKeys.todos });
    },
  });
}
