import { z } from "zod";

/** Espeja RegisterProyectoRequest.java (@NotBlank, @Size(max = 100)). */
export const nuevoProyectoSchema = z.object({
  nombre: z
    .string()
    .min(1, "El nombre es obligatorio")
    .max(100, "El nombre no puede exceder los 100 caracteres"),
});

export type NuevoProyectoValues = z.infer<typeof nuevoProyectoSchema>;

/** Espeja RegisterColaboradorRequest.java. */
export const nuevoColaboradorSchema = z.object({
  nombre: z
    .string()
    .min(1, "El nombre es obligatorio")
    .max(100, "El nombre no puede exceder los 100 caracteres"),
  email: z
    .email("Ingresá un email válido")
    .max(150, "El email no puede exceder los 150 caracteres"),
});

export type NuevoColaboradorValues = z.infer<typeof nuevoColaboradorSchema>;
