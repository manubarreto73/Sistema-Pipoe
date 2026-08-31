import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  actualizarPermisos,
  crearColaborador,
  crearProyecto,
  eliminarColaborador,
  eliminarProyecto,
  getColaboradores,
  getProyecto,
  getProyectos,
  renombrarProyecto,
} from "@/features/proyectos/api";
import type {
  NuevoColaboradorValues,
  NuevoProyectoValues,
} from "@/features/proyectos/schemas";
import {
  getProyectosAdmin,
  type FiltrosProyectos,
} from "@/features/proyectos/adminApi";
import type { Permiso } from "@/features/proyectos/types";
import { useAuthStore } from "@/stores/auth";

/** Claves de caché centralizadas para no desincronizar los invalidate. */
export const proyectosKeys = {
  todos: ["proyectos"] as const,
  detalle: (id: number) => ["proyectos", id] as const,
  colaboradores: (id: number) => ["proyectos", id, "colaboradores"] as const,
};

/**
 * Todos los proyectos del sistema, para la administradora. Aparte de useProyectos, que es "los
 * míos": son dos preguntas distintas y dos endpoints distintos.
 */
export function useProyectosAdmin(filtros: FiltrosProyectos, page: number) {
  return useQuery({
    queryKey: [...proyectosKeys.todos, "admin", { ...filtros, page }] as const,
    queryFn: () => getProyectosAdmin(filtros, page),
    // Al cambiar de filtro o de página, sostiene lo anterior en pantalla en vez de parpadear.
    placeholderData: (anterior) => anterior,
  });
}

export function useProyectos() {
  return useQuery({
    queryKey: proyectosKeys.todos,
    queryFn: getProyectos,
  });
}

export function useProyecto(proyectoId: number) {
  return useQuery({
    queryKey: proyectosKeys.detalle(proyectoId),
    queryFn: () => getProyecto(proyectoId),
    enabled: Number.isFinite(proyectoId),
  });
}

/**
 * Si la sesión es la dueña de ESTE proyecto.
 *
 * No alcanza con `type === "USUARIO"`, que es como se resolvía antes: desde que la
 * administradora entra a proyectos ajenos hay usuarios mirando algo que no es suyo, y
 * ofrecerles acciones que la API les va a negar con un 403 es peor que no ofrecerlas.
 *
 * Se apoya en la misma query que ya cargó el marco del proyecto, así que no pide nada de más.
 */
export function useEsDuenio(proyectoId: number) {
  const sesion = useAuthStore((state) => state.sesion);
  const proyecto = useProyecto(proyectoId);

  return sesion?.type === "USUARIO" && proyecto.data?.usuarioId === sesion.id;
}

export function useColaboradores(proyectoId: number, habilitado: boolean) {
  return useQuery({
    queryKey: proyectosKeys.colaboradores(proyectoId),
    queryFn: () => getColaboradores(proyectoId),
    // Un colaborador no puede listar colaboradores (403), así que ni preguntamos.
    enabled: habilitado && Number.isFinite(proyectoId),
  });
}

export function useCrearProyecto() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: crearProyecto,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: proyectosKeys.todos });
    },
  });
}

export function useRenombrarProyecto(proyectoId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: NuevoProyectoValues) => renombrarProyecto(proyectoId, values),
    onSuccess: (proyecto) => {
      queryClient.setQueryData(proyectosKeys.detalle(proyectoId), proyecto);
      queryClient.invalidateQueries({ queryKey: proyectosKeys.todos });
    },
  });
}

export function useEliminarProyecto() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: eliminarProyecto,
    onSuccess: (_data, proyectoId) => {
      // El detalle y los colaboradores del proyecto borrado ya no existen: sacarlos de la
      // caché evita que una vuelta atrás del navegador muestre datos fantasma.
      queryClient.removeQueries({ queryKey: proyectosKeys.detalle(proyectoId) });
      queryClient.removeQueries({ queryKey: proyectosKeys.colaboradores(proyectoId) });
      queryClient.invalidateQueries({ queryKey: proyectosKeys.todos });
    },
  });
}

export function useCrearColaborador(proyectoId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: NuevoColaboradorValues) => crearColaborador(proyectoId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: proyectosKeys.colaboradores(proyectoId),
      });
    },
  });
}

export function useActualizarPermisos(proyectoId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      colaboradorId,
      permisos,
    }: {
      colaboradorId: number;
      permisos: Permiso[];
    }) => actualizarPermisos(proyectoId, colaboradorId, permisos),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: proyectosKeys.colaboradores(proyectoId),
      });
    },
  });
}

export function useEliminarColaborador(proyectoId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (colaboradorId: number) => eliminarColaborador(proyectoId, colaboradorId),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: proyectosKeys.colaboradores(proyectoId),
      });
    },
  });
}
