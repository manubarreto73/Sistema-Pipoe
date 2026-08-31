/** Espeja dominio/comentarios/dtos/ComentarioResponse.java. */
export type Comentario = {
  id: number;
  texto: string;
  autor: string;
  /** Para distinguir de un vistazo quién escribió: el equipo o quien acompaña el proyecto. */
  autorTipo: "USUARIO" | "COLABORADOR";
  creadoEn: string;
  /**
   * Si esta sesión puede borrarlo: la dueña del proyecto o la administración del sistema.
   * Lo decide la API; acá no se recalcula.
   */
  puedeBorrar: boolean;
};
