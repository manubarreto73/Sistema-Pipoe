import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link } from "react-router";

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
    // Mismo ancho y misma composición que el perfil: se llega acá desde ahí, y dos pantallas
    // encadenadas que cambian de forma se sienten como dos aplicaciones distintas.
    <div className="mx-auto max-w-xl">
      <header className="flex flex-col items-center text-center">
        <span
          aria-hidden
          className="flex size-20 items-center justify-center rounded-full bg-brand-100 text-brand-700"
        >
          <IconoCandado />
        </span>

        <h1 className="mt-4 font-serif text-3xl font-bold tracking-tight text-slate-900">
          Cambiar contraseña
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Necesitas la contraseña actual para poder cambiarla.
        </p>
      </header>

      <section className="mt-10 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Nueva contraseña</h2>

        <form
          noValidate
          onSubmit={handleSubmit((values) =>
            cambiar.mutate(values, { onSuccess: () => reset() }),
          )}
          className="mt-5 flex flex-col gap-4"
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
            ayuda="Mínimo 8 caracteres, con letras, números y al menos una mayúscula."
            error={errors.newPassword?.message}
          >
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              aria-describedby="newPassword-ayuda"
              invalid={Boolean(errors.newPassword)}
              {...register("newPassword")}
            />
          </Field>

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

          <div className="mt-1 flex items-center justify-between gap-4">
            <Link to="/perfil" className="text-sm text-brand-700 hover:underline">
              Volver al perfil
            </Link>

            <Button type="submit" loading={cambiar.isPending}>
              Guardar
            </Button>
          </div>
        </form>
      </section>
    </div>
  );
}

/** Candado, para que el encabezado tenga el mismo peso visual que el avatar del perfil. */
function IconoCandado() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="size-9"
    >
      <rect x="4" y="10" width="16" height="11" rx="2" />
      <path d="M8 10V7a4 4 0 1 1 8 0v3" />
    </svg>
  );
}
