import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  cambiarActivo,
  crearAdmin,
  getUsuarios,
  type FiltrosUsuarios,
} from "@/features/usuarios/api";

export const usuariosKeys = {
  todos: ["usuarios"] as const,
  lista: (filtros: FiltrosUsuarios, page: number) =>
    ["usuarios", { ...filtros, page }] as const,
};

export function useUsuarios(filtros: FiltrosUsuarios, page: number) {
  return useQuery({
    queryKey: usuariosKeys.lista(filtros, page),
    queryFn: () => getUsuarios(filtros, page),
    // Al cambiar de filtro o de página, sostiene lo anterior en pantalla en vez de parpadear.
    placeholderData: (anterior) => anterior,
  });
}

/**
 * Las dos mutaciones invalidan toda la lista, no sólo la página actual: un alta cambia el
 * orden alfabético y una baja puede sacar la fila del filtro que se esté mirando.
 */
function useInvalidarUsuarios() {
  const queryClient = useQueryClient();

  return () => queryClient.invalidateQueries({ queryKey: usuariosKeys.todos });
}

export function useCrearAdmin() {
  const invalidar = useInvalidarUsuarios();

  return useMutation({ mutationFn: crearAdmin, onSuccess: invalidar });
}

export function useCambiarActivo() {
  const invalidar = useInvalidarUsuarios();

  return useMutation({
    mutationFn: ({ id, activo }: { id: number; activo: boolean }) => cambiarActivo(id, activo),
    onSuccess: invalidar,
  });
}
