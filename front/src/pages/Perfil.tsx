import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { useActualizarPerfil } from "@/features/auth/hooks";
import { perfilSchema, type PerfilValues } from "@/features/auth/schemas";
import { useAuthStore } from "@/stores/auth";

/** Iniciales para el avatar. Dos como mucho: con tres deja de leerse. */
function iniciales(nombre: string | undefined) {
  if (!nombre) return "?";

  return nombre
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((parte) => parte.charAt(0).toUpperCase())
    .join("");
}

export default function Perfil() {
  const sesion = useAuthStore((state) => state.sesion);
  const actualizar = useActualizarPerfil();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<PerfilValues>({
    resolver: zodResolver(perfilSchema),
    defaultValues: { nombreCompleto: sesion?.nombreCompleto ?? "" },
  });

  return (
    <div className="mx-auto max-w-xl">
      {/* Encabezado con la identidad de la cuenta: sin esto la pantalla se parecía a un login,
          que es justo lo contrario de donde uno cree estar al entrar a su perfil. */}
      <header className="flex flex-col items-center text-center">
        <span
          aria-hidden
          className="flex size-20 items-center justify-center rounded-full bg-brand-100 font-serif text-2xl font-bold text-brand-700"
        >
          {iniciales(sesion?.nombreCompleto)}
        </span>

        <h1 className="mt-4 font-serif text-3xl font-bold tracking-tight text-slate-900">
          {sesion?.nombreCompleto}
        </h1>
        <p className="mt-1 text-sm text-slate-500">{sesion?.email}</p>

        {sesion?.type === "COLABORADOR" && sesion.proyectoNombre && (
          <p className="mt-3 rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
            Colaborador en {sesion.proyectoNombre}
          </p>
        )}
      </header>

      <section className="mt-10 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">Datos de la cuenta</h2>

        {/*
          El reset con los valores guardados limpia el estado "sucio": es lo que hace
          desaparecer el cartel de éxito en cuanto el usuario vuelve a escribir.
        */}
        <form
          noValidate
          onSubmit={handleSubmit((values) =>
            actualizar.mutate(values, { onSuccess: () => reset(values) }),
          )}
          className="mt-5 flex flex-col gap-4"
        >
          <Field label="Nombre" htmlFor="nombreCompleto" error={errors.nombreCompleto?.message}>
            <Input
              id="nombreCompleto"
              autoComplete="name"
              invalid={Boolean(errors.nombreCompleto)}
              {...register("nombreCompleto")}
            />
          </Field>

          {/* El email identifica la cuenta y es parte del login: no se edita desde acá. */}
          <Field label="Email" htmlFor="email">
            <Input id="email" value={sesion?.email ?? ""} disabled readOnly />
          </Field>
          <p className="-mt-2 text-xs text-slate-500">
            El email identifica la cuenta y no se puede cambiar.
          </p>

          {actualizar.isError && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {actualizar.error.message}
            </p>
          )}

          {actualizar.isSuccess && !isDirty && (
            <p role="status" className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-700">
              Nombre actualizado.
            </p>
          )}

          <div className="mt-1 flex justify-end">
            <Button type="submit" loading={actualizar.isPending} disabled={!isDirty}>
              Guardar cambios
            </Button>
          </div>
        </form>
      </section>

      <section className="mt-5 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Contraseña</h2>
          <p className="mt-0.5 text-sm text-slate-500">
            Vas a necesitar la actual para poder cambiarla.
          </p>
        </div>

        <Link to="/cambiar-password">
          <Button variant="secondary">Cambiar contraseña</Button>
        </Link>
      </section>
    </div>
  );
}
