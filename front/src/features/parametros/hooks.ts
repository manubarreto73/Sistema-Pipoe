import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { actualizarConfiguracion, getConfiguracion } from "@/features/parametros/api";
import type { CuposValues } from "@/features/parametros/schemas";

export const parametrosKeys = {
  configuracion: ["parametros"] as const,
};

/**
 * Los límites casi nunca cambian —los ajusta la dueña desde su pantalla y ahí queda—, así que
 * no tiene sentido revalidarlos en cada foco de ventana.
 */
export function useConfiguracion() {
  return useQuery({
    queryKey: parametrosKeys.configuracion,
    queryFn: getConfiguracion,
    staleTime: 15 * 60 * 1000,
  });
}

export function useActualizarConfiguracion() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (values: CuposValues) => actualizarConfiguracion(values),
    onSuccess: (configuracion) => {
      queryClient.setQueryData(parametrosKeys.configuracion, configuracion);
    },
  });
}
