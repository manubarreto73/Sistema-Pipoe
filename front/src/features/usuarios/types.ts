import type { Role } from "@/features/auth/types";

/** Espeja dominio/usuarios/dtos/UsuarioAdminResponse.java. */
export type UsuarioAdmin = {
  id: number;
  email: string;
  nombreCompleto: string;
  role: Role;
  /** false es una baja lógica: no entra, pero todo lo que hizo sigue en el sistema. */
  activo: boolean;
  /** null si la cuenta nunca inició sesión. */
  ultimoAcceso: string | null;
  proyectos: number;
};
