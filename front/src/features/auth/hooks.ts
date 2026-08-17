import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";

import {
  actualizarPerfil,
  cambiarPassword,
  loginColaborador,
  loginUsuario,
  logout,
} from "@/features/auth/api";
import type { Sesion } from "@/features/auth/types";
import { useAuthStore } from "@/stores/auth";

/** Adónde va cada tipo de sesión después de entrar. */
export function destinoInicial(sesion: Sesion) {
  if (sesion.type === "COLABORADOR" && sesion.proyectoId) {
    return `/proyectos/${sesion.proyectoId}`;
  }
  // El admin no maneja proyectos: su cuenta existe para gestionar solicitudes.
  if (sesion.role === "ADMIN") return "/admin/solicitudes";

  return "/proyectos";
}

export function useLoginUsuario() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: loginUsuario,
    onSuccess: ({ accessToken, refreshToken, usuario }) => {
      useAuthStore.getState().iniciarSesion(accessToken, refreshToken, usuario);
      navigate(destinoInicial(usuario), { replace: true });
    },
  });
}

export function useLoginColaborador() {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: loginColaborador,
    onSuccess: ({ accessToken, refreshToken, usuario }) => {
      useAuthStore.getState().iniciarSesion(accessToken, refreshToken, usuario);
      navigate(destinoInicial(usuario), { replace: true });
    },
  });
}

export function useLogout() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async () => {
      const { refreshToken } = useAuthStore.getState();
      if (!refreshToken) return;

      try {
        await logout(refreshToken);
      } catch {
        // Si el server no responde igual cerramos del lado del cliente: dejar al
        // usuario "adentro" porque falló el logout remoto sería peor.
      }
    },
    onSettled: () => {
      useAuthStore.getState().clear();
      queryClient.clear();
      navigate("/login", { replace: true });
    },
  });
}

export function useCambiarPassword() {
  return useMutation({ mutationFn: cambiarPassword });
}

/** El nombre se muestra en el header y en el listado de colaboradores: hay que refrescar la sesión. */
export function useActualizarPerfil() {
  return useMutation({
    mutationFn: actualizarPerfil,
    onSuccess: (sesion) => useAuthStore.getState().setSesion(sesion),
  });
}
