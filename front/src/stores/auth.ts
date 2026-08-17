import { create } from "zustand";
import { persist } from "zustand/middleware";

import type { Sesion } from "@/features/auth/types";
import { setAccessToken } from "@/lib/tokens";

type AuthState = {
  sesion: Sesion | null;
  refreshToken: string | null;
  /** Login: guarda el par de tokens y la identidad que devolvió la API. */
  iniciarSesion: (accessToken: string, refreshToken: string, sesion: Sesion) => void;
  /** Refresh: sólo rota los tokens, la identidad no cambia al renovar. */
  setTokens: (accessToken: string, refreshToken: string) => void;
  /** Rehidratación desde GET /api/auth/me. */
  setSesion: (sesion: Sesion) => void;
  clear: () => void;
};

export const AUTH_STORAGE_KEY = "pipoe.auth";

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      sesion: null,
      refreshToken: null,

      iniciarSesion: (accessToken, refreshToken, sesion) => {
        setAccessToken(accessToken);
        set({ refreshToken, sesion });
      },

      setTokens: (accessToken, refreshToken) => {
        setAccessToken(accessToken);
        set({ refreshToken });
      },

      setSesion: (sesion) => set({ sesion }),

      clear: () => {
        setAccessToken(null);
        set({ sesion: null, refreshToken: null });
      },
    }),
    {
      name: AUTH_STORAGE_KEY,
      // El access token nunca se persiste: vive en memoria (ver lib/tokens.ts).
      partialize: (state) => ({
        sesion: state.sesion,
        refreshToken: state.refreshToken,
      }),
    },
  ),
);

/**
 * Sincroniza el cierre de sesión entre pestañas.
 *
 * El evento `storage` sólo dispara en las pestañas *distintas* de la que escribió,
 * así que si el usuario cierra sesión en una, las demás se enteran y limpian su
 * estado en memoria en vez de quedar con una UI de sesión activa y un token muerto.
 */
export function subscribeToSessionSync() {
  const onStorage = (event: StorageEvent) => {
    if (event.key !== AUTH_STORAGE_KEY) return;

    const store = useAuthStore.getState();
    if (event.newValue === null) {
      store.clear();
      return;
    }

    try {
      const parsed = JSON.parse(event.newValue) as {
        state?: { refreshToken: string | null };
      };
      if (!parsed.state?.refreshToken) store.clear();
    } catch {
      store.clear();
    }
  };

  window.addEventListener("storage", onStorage);
  return () => window.removeEventListener("storage", onStorage);
}
