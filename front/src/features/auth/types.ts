export type SessionType = "USUARIO" | "COLABORADOR";

export type Role = "ADMIN" | "USER";

/**
 * Espeja auth/dto/SesionResponse.java. Es la misma forma para los dos tipos de sesión:
 * `role` viene null en colaboradores, y `proyectoId`/`proyectoNombre` en usuarios.
 */
export type Sesion = {
  type: SessionType;
  id: number;
  email: string;
  nombreCompleto: string;
  role: Role | null;
  proyectoId: number | null;
  proyectoNombre: string | null;
};
