import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { actualizarTextoLanding, getTextosLanding } from "@/features/landing/api";
import type { ClaveTexto } from "@/features/landing/types";

export const landingKeys = {
  textos: ["landing", "textos"] as const,
};

export function useTextosLanding() {
  return useQuery({
    queryKey: landingKeys.textos,
    queryFn: getTextosLanding,
    // La portada es lo primero que se ve y cambia muy de vez en cuando: no tiene sentido
    // volver a pedirla cada vez que la pestaña recupera el foco.
    staleTime: 5 * 60 * 1000,
  });
}

export function useActualizarTextoLanding() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ clave, contenido }: { clave: ClaveTexto; contenido: string }) =>
      actualizarTextoLanding(clave, contenido),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: landingKeys.textos });
    },
  });
}
