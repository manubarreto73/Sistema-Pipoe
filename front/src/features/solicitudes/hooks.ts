import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  aprobarSolicitud,
  crearSolicitud,
  getSolicitudes,
  rechazarSolicitud,
  type FiltrosSolicitudes,
} from "@/features/solicitudes/api";

export const solicitudesKeys = {
  todas: ["solicitudes"] as const,
  lista: (filtros: FiltrosSolicitudes, page: number) =>
    ["solicitudes", { ...filtros, page }] as const,
};

export function useCrearSolicitud() {
  return useMutation({ mutationFn: crearSolicitud });
}

export function useSolicitudes(filtros: FiltrosSolicitudes, page: number) {
  return useQuery({
    queryKey: solicitudesKeys.lista(filtros, page),
    queryFn: () => getSolicitudes(filtros, page),
    // Al cambiar de filtro o página, mantiene los datos anteriores en pantalla
    // en vez de parpadear a estado vacío mientras llega la nueva página.
    placeholderData: (anterior) => anterior,
  });
}

/** Aprobar y rechazar invalidan toda la lista: la solicitud cambia de estado y de filtro. */
function useResolverSolicitud(resolver: (id: number) => Promise<unknown>) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: resolver,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: solicitudesKeys.todas });
    },
  });
}

export function useAprobarSolicitud() {
  return useResolverSolicitud(aprobarSolicitud);
}

export function useRechazarSolicitud() {
  return useResolverSolicitud(rechazarSolicitud);
}
