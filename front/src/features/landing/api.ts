import type { ClaveTexto, TextoLanding } from "@/features/landing/types";
import { apiFetch } from "@/lib/http";

/** Público: lo pide la portada antes de que nadie inicie sesión. */
export function getTextosLanding() {
  return apiFetch<TextoLanding[]>("/api/landing/textos", { auth: false });
}

/** Sólo ADMIN. */
export function actualizarTextoLanding(clave: ClaveTexto, contenido: string) {
  return apiFetch<TextoLanding>(`/api/landing/textos/${clave}`, {
    method: "PUT",
    body: { contenido },
  });
}
