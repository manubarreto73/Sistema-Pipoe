import { useAuthStore } from "@/stores/auth";

type RefreshResponse = {
  accessToken: string;
  refreshToken: string;
};

/**
 * Promesa de refresh en vuelo, compartida por todas las requests que fallan a la vez.
 *
 * Es el punto crítico de la integración: la API rota el refresh token con GETDEL
 * atómico, así que el token viejo muere apenas se usa. Si cuatro queries vencidas
 * dispararan cuatro /refresh en paralelo, el primero ganaría y los otros tres
 * recibirían "Refresh token inválido", desconectando al usuario sin motivo.
 */
let inFlight: Promise<string> | null = null;

export function refreshAccessToken(): Promise<string> {
  inFlight ??= doRefresh().finally(() => {
    inFlight = null;
  });
  return inFlight;
}

async function doRefresh(): Promise<string> {
  const { refreshToken, setTokens, clear } = useAuthStore.getState();

  if (!refreshToken) {
    clear();
    throw new Error("No hay refresh token guardado");
  }

  let response: Response;
  try {
    // Deliberadamente con fetch crudo y no con apiFetch: apiFetch llama acá ante un
    // 401, y usarlo de vuelta armaría un ciclo de import y una recursión de refresh.
    response = await fetch(`${import.meta.env.VITE_API_URL}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    // Falla de red: no invalidamos la sesión, el refresh token sigue siendo válido.
    throw new Error("No se pudo contactar al servidor para renovar la sesión");
  }

  if (!response.ok) {
    clear();
    throw new Error("La sesión expiró");
  }

  const data = (await response.json()) as RefreshResponse;
  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}
