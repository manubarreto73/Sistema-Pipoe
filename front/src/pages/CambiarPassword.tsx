import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { useCambiarPassword } from "@/features/auth/hooks";
import {
  cambiarPasswordSchema,
  type CambiarPasswordValues,
} from "@/features/auth/schemas";

export default function CambiarPassword() {
  const cambiar = useCambiarPassword();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CambiarPasswordValues>({
    resolver: zodResolver(cambiarPasswordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  return (
    <div className="max-w-sm">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
        Cambiar contraseña
      </h1>

      <form
        noValidate
        onSubmit={handleSubmit((values) =>
          cambiar.mutate(values, { onSuccess: () => reset() }),
        )}
        className="mt-8 flex flex-col gap-4"
      >
        <Field
          label="Contraseña actual"
          htmlFor="currentPassword"
          error={errors.currentPassword?.message}
        >
          <Input
            id="currentPassword"
            type="password"
            autoComplete="current-password"
            invalid={Boolean(errors.currentPassword)}
            {...register("currentPassword")}
          />
        </Field>

        <Field
          label="Nueva contraseña"
          htmlFor="newPassword"
          error={errors.newPassword?.message}
        >
          <Input
            id="newPassword"
            type="password"
            autoComplete="new-password"
            invalid={Boolean(errors.newPassword)}
            {...register("newPassword")}
          />
        </Field>

        {/* Los requisitos se anuncian antes de escribir, no sólo cuando el submit falla. */}
        <p className="-mt-2 text-xs text-slate-500">
          Mínimo 8 caracteres, con letras, números y al menos una mayúscula.
        </p>

        <Field
          label="Repetir nueva contraseña"
          htmlFor="confirmPassword"
          error={errors.confirmPassword?.message}
        >
          <Input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            invalid={Boolean(errors.confirmPassword)}
            {...register("confirmPassword")}
          />
        </Field>

        {cambiar.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {cambiar.error.message}
          </p>
        )}

        {cambiar.isSuccess && (
          <p role="status" className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
            Contraseña actualizada.
          </p>
        )}

        <Button type="submit" loading={cambiar.isPending} className="mt-2">
          Guardar
        </Button>
      </form>
    </div>
  );
}
