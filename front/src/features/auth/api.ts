import type {
  CambiarPasswordValues,
  LoginColaboradorValues,
  LoginUsuarioValues,
  PerfilValues,
} from "@/features/auth/schemas";
import type { Sesion } from "@/features/auth/types";
import { apiFetch } from "@/lib/http";

/** Espeja auth/dto/LoginResponse.java. */
export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  usuario: Sesion;
};

export function loginUsuario(values: LoginUsuarioValues) {
  return apiFetch<LoginResponse>("/api/auth/login", {
    method: "POST",
    body: values,
    auth: false,
  });
}

export function loginColaborador(values: LoginColaboradorValues) {
  return apiFetch<LoginResponse>("/api/auth/colaborador/login", {
    method: "POST",
    body: values,
    auth: false,
  });
}

export function logout(refreshToken: string) {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    body: { refreshToken },
  });
}

/** Identidad de la sesión actual. Sirve para usuarios y colaboradores por igual. */
export function getSesion() {
  return apiFetch<Sesion>("/api/auth/me");
}

/** Cambio de nombre propio. Devuelve la sesión actualizada, igual que /me. */
export function actualizarPerfil(values: PerfilValues) {
  return apiFetch<Sesion>("/api/auth/perfil", { method: "PUT", body: values });
}

export function cambiarPassword(values: CambiarPasswordValues) {
  return apiFetch<void>("/api/auth/change-password", {
    method: "PUT",
    body: {
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    },
  });
}
