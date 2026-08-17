import { QueryCache, QueryClient } from "@tanstack/react-query";

import { ApiError } from "@/lib/apiError";

export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      // Punto de enganche para notificaciones globales cuando sumemos una librería
      // de toasts. Los 401 ya los maneja http.ts cerrando la sesión.
      if (import.meta.env.DEV) console.error("[query]", error);
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Un 4xx no se arregla reintentando: falta permiso, no existe, o el dato
        // que mandamos está mal. Solo reintentamos fallas de red y 5xx.
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});
