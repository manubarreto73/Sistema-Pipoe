import { z } from "zod";

/** Espeja RegisterAdminRequest.java. */
export const nuevoAdminSchema = z.object({
  email: z.email("Ingresa un email válido").trim(),
  nombreCompleto: z
    .string()
    .trim()
    .min(1, "El nombre es obligatorio")
    .max(150, "El nombre no puede exceder los 150 caracteres"),
});

export type NuevoAdminValues = z.infer<typeof nuevoAdminSchema>;
