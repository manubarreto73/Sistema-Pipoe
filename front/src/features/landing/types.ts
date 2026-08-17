/** Espeja dominio/landing/entities/ClaveTexto.java. */
export type ClaveTexto =
  | "HERO_TITULO"
  | "HERO_SUBTITULO"
  | "DESCRIPCION_TITULO"
  | "DESCRIPCION_CUERPO"
  | "MODELO_TITULO"
  | "MODELO_CUERPO"
  | "BIOGRAFIA_TITULO"
  | "BIOGRAFIA_CUERPO";

/** Espeja dominio/landing/dtos/TextoLandingResponse.java. */
export type TextoLanding = {
  clave: ClaveTexto;
  contenido: string;
  etiqueta: string;
  ayuda: string;
  /** PLANO se edita con un campo de una línea; RICO con el editor de texto. */
  tipo: "PLANO" | "RICO";
  orden: number;
  actualizadoEn: string | null;
};

/** Busca un texto por clave. Devuelve "" si todavía no está cargado. */
export function textoDe(textos: TextoLanding[] | undefined, clave: ClaveTexto) {
  return textos?.find((texto) => texto.clave === clave)?.contenido ?? "";
}
