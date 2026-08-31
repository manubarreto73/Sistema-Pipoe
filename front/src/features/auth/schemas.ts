import { z } from "zod";

/** Espeja las validaciones de LoginRequest.java (@NotBlank @Email). */
export const loginUsuarioSchema = z.object({
  // .trim() antes de validar: un correo pegado con un espacio al final es válido para la
  // persona y la API lo rechazaría como mal formado.
  email: z.email("Ingresa un email válido").trim(),
  password: z.string().min(1, "La contraseña es obligatoria"),
});

export type LoginUsuarioValues = z.infer<typeof loginUsuarioSchema>;

/**
 * Espeja ColaboradorLoginRequest.java.
 *
 * El código del proyecto reemplazó al nombre: el nombre no servía como identificador porque es
 * único distinguiendo mayúsculas y porque el dueño puede cambiarlo. Se acepta escrito con o sin
 * el prefijo PIPOE- y en cualquier caja; la API lo normaliza igual.
 */
export const loginColaboradorSchema = z.object({
  codigoProyecto: z.string().trim().min(1, "El código del proyecto es obligatorio"),
  email: z.email("Ingresa un email válido").trim(),
  password: z.string().min(1, "La contraseña es obligatoria"),
});

export type LoginColaboradorValues = z.infer<typeof loginColaboradorSchema>;

/**
 * Espeja Constantes.PASSWORD_REGEX. Se valida regla por regla en vez de con la regex entera
 * para poder decirle al usuario cuál le falta.
 */
const passwordSchema = z
  .string()
  .min(8, "La contraseña debe tener al menos 8 caracteres")
  .regex(/[A-Za-z]/, "La contraseña debe incluir al menos una letra")
  .regex(/[A-Z]/, "La contraseña debe incluir al menos una mayúscula")
  .regex(/\d/, "La contraseña debe incluir al menos un número");

/**
 * Espeja ChangePasswordRequest.java.
 * La confirmación es sólo del lado del cliente: la API no la recibe.
 */
export const cambiarPasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "La contraseña actual es obligatoria"),
    newPassword: passwordSchema,
    confirmPassword: z.string().min(1, "Repite la nueva contraseña"),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: "Las contraseñas no coinciden",
    path: ["confirmPassword"],
  })
  .refine((values) => values.newPassword !== values.currentPassword, {
    message: "La nueva contraseña debe ser distinta de la actual",
    path: ["newPassword"],
  });

export type CambiarPasswordValues = z.infer<typeof cambiarPasswordSchema>;

/** Espeja ActualizarPerfilRequest.java (@NotBlank, @Size(max = 100)). */
export const perfilSchema = z.object({
  nombreCompleto: z
    .string()
    .min(1, "El nombre es obligatorio")
    .max(100, "El nombre no puede exceder los 100 caracteres"),
});

export type PerfilValues = z.infer<typeof perfilSchema>;
