import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { useLoginColaborador } from "@/features/auth/hooks";
import {
  loginColaboradorSchema,
  type LoginColaboradorValues,
} from "@/features/auth/schemas";

export default function LoginColaborador() {
  const login = useLoginColaborador();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginColaboradorValues>({
    resolver: zodResolver(loginColaboradorSchema),
    defaultValues: { codigoProyecto: "", email: "", password: "" },
  });

  return (
    <div className="mx-auto max-w-sm px-6 py-16">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
        Acceso de colaborador
      </h1>
      <p className="mt-2 text-sm text-slate-600">
        Ingresa con los datos que recibiste por mail al ser invitado al proyecto.
      </p>

      <form
        noValidate
        onSubmit={handleSubmit((values) => login.mutate(values))}
        className="mt-8 flex flex-col gap-4"
      >
        <Field
          label="Código del proyecto"
          htmlFor="codigoProyecto"
          ayuda="lo encontrás en el mail de invitación, con la forma PIPOE-0000"
          error={errors.codigoProyecto?.message}
        >
          <Input
            id="codigoProyecto"
            aria-describedby="codigoProyecto-ayuda"
            placeholder="PIPOE-0000"
            autoCapitalize="characters"
            spellCheck={false}
            invalid={Boolean(errors.codigoProyecto)}
            {...register("codigoProyecto")}
          />
        </Field>

        <Field label="Email" htmlFor="email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            invalid={Boolean(errors.email)}
            {...register("email")}
          />
        </Field>

        <Field label="Contraseña" htmlFor="password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            invalid={Boolean(errors.password)}
            {...register("password")}
          />
        </Field>

        {login.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {login.error.message}
          </p>
        )}

        <Button type="submit" loading={login.isPending} className="mt-2">
          Entrar
        </Button>
      </form>

      <Link
        to="/login"
        className="mt-8 inline-block text-sm text-brand-700 hover:underline"
      >
        Volver al login de usuario
      </Link>
    </div>
  );
}
