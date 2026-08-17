import { z } from "zod";

/**
 * Espeja ActualizarParametrosRequest.java (@NotNull, @Min(1), @Max(100)).
 *
 * Sin `coerce`: los campos se registran con `valueAsNumber`, así que acá ya llega un número.
 * Un campo vacío llega como NaN y cae en el mensaje de "indicá el máximo".
 */
const cupo = (que: string) =>
  z
    .number({ error: `Indicá el máximo de ${que}` })
    .int("Tiene que ser un número entero")
    .min(1, "Tiene que ser al menos 1")
    .max(100, "No puede pasar de 100");

export const cuposSchema = z.object({
  maxProyectosPorUsuario: cupo("proyectos por usuario"),
  maxColaboradoresPorProyecto: cupo("colaboradores por proyecto"),
});

export type CuposValues = z.infer<typeof cuposSchema>;
