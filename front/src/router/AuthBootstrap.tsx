import { useEffect, useState, type ReactNode } from "react";

import { Spinner } from "@/components/ui/Spinner";
import { getSesion } from "@/features/auth/api";
import { refreshAccessToken } from "@/lib/refresh";
import { getAccessToken } from "@/lib/tokens";
import { subscribeToSessionSync, useAuthStore } from "@/stores/auth";

/**
 * Recupera la sesión al cargar la página.
 *
 * El access token vive en memoria, así que un F5 lo borra aunque el refresh token
 * siga guardado. Antes de renderizar canjeamos uno nuevo y revalidamos la identidad
 * contra /me, porque la sesión persistida puede haber quedado vieja (cambió el nombre,
 * el rol, o el colaborador fue movido de proyecto). Si algo falla, limpiamos.
 */
export function AuthBootstrap({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);

  useEffect(() => {
    let cancelled = false;

    const finish = () => {
      if (!cancelled) setReady(true);
    };

    const { refreshToken } = useAuthStore.getState();
    if (!refreshToken || getAccessToken()) {
      finish();
      return;
    }

    void (async () => {
      try {
        await refreshAccessToken();
        const sesion = await getSesion();
        useAuthStore.getState().setSesion(sesion);
      } catch {
        useAuthStore.getState().clear();
      } finally {
        finish();
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => subscribeToSessionSync(), []);

  if (!ready) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spinner className="size-8 text-brand-600" />
        <span className="sr-only">Cargando sesión</span>
      </div>
    );
  }

  return children;
}
