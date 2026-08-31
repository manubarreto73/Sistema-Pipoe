import { z } from "zod";

/** Espeja RegisterComentarioRequest.java (@NotBlank, @Size(max = 2000)). */
export const comentarioSchema = z.object({
  texto: z
    .string()
    .trim()
    .min(1, "Escribe algo antes de enviar")
    .max(2000, "El comentario no puede exceder los 2000 caracteres"),
});

export type ComentarioValues = z.infer<typeof comentarioSchema>;
