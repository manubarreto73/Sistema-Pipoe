import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  crearComentario,
  eliminarComentario,
  getComentarios,
} from "@/features/comentarios/api";

export const comentariosKeys = {
  delPaso: (proyectoId: number, pasoId: number) =>
    ["proyectos", proyectoId, "pasos", pasoId, "comentarios"] as const,
};

export function useComentarios(proyectoId: number, pasoId: number) {
  return useQuery({
    queryKey: comentariosKeys.delPaso(proyectoId, pasoId),
    queryFn: () => getComentarios(proyectoId, pasoId),
  });
}

/**
 * Crear y borrar invalidan el listado del paso en vez de tocar la caché a mano: la respuesta de
 * la API trae `puedeBorrar` ya resuelto para cada comentario, y recalcularlo acá sería duplicar una
 * regla de permisos que vive del otro lado.
 */
function useInvalidarComentarios(proyectoId: number, pasoId: number) {
  const queryClient = useQueryClient();

  return () =>
    queryClient.invalidateQueries({
      queryKey: comentariosKeys.delPaso(proyectoId, pasoId),
    });
}

export function useCrearComentario(proyectoId: number, pasoId: number) {
  const invalidar = useInvalidarComentarios(proyectoId, pasoId);

  return useMutation({
    mutationFn: (texto: string) => crearComentario(proyectoId, pasoId, texto),
    onSuccess: invalidar,
  });
}

export function useEliminarComentario(proyectoId: number, pasoId: number) {
  const invalidar = useInvalidarComentarios(proyectoId, pasoId);

  return useMutation({
    mutationFn: (comentarioId: number) => eliminarComentario(proyectoId, pasoId, comentarioId),
    onSuccess: invalidar,
  });
}
