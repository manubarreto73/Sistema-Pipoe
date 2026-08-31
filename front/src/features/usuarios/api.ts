import type { Role } from "@/features/auth/types";
import type { NuevoAdminValues } from "@/features/usuarios/schemas";
import type { UsuarioAdmin } from "@/features/usuarios/types";
import { apiFetch } from "@/lib/http";
import type { Page } from "@/types/page";

export type FiltrosUsuarios = {
  /** Busca en el nombre y en el email. */
  texto: string;
  /** null trae todos; true sólo los activos; false sólo los dados de baja. */
  activo: boolean | null;
  /** null trae todos los roles. */
  role: Role | null;
};

export const FILTROS_USUARIOS_VACIOS: FiltrosUsuarios = {
  texto: "",
  activo: null,
  role: null,
};

export function getUsuarios(filtros: FiltrosUsuarios, page: number) {
  const params = new URLSearchParams({ page: String(page) });

  if (filtros.texto.trim()) params.set("texto", filtros.texto.trim());
  if (filtros.activo !== null) params.set("activo", String(filtros.activo));
  if (filtros.role !== null) params.set("role", filtros.role);

  return apiFetch<Page<UsuarioAdmin>>(`/api/admin/usuarios?${params}`);
}

/** La contraseña la genera el servidor y viaja por mail: nunca vuelve en la respuesta. */
export function crearAdmin(values: NuevoAdminValues) {
  return apiFetch<UsuarioAdmin>("/api/admin/usuarios/administradores", {
    method: "POST",
    body: values,
  });
}

export function cambiarActivo(id: number, activo: boolean) {
  return apiFetch<UsuarioAdmin>(`/api/admin/usuarios/${id}/activo`, {
    method: "PUT",
    body: { activo },
  });
}
