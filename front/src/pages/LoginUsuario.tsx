import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { useLoginUsuario } from "@/features/auth/hooks";
import {
  loginUsuarioSchema,
  type LoginUsuarioValues,
} from "@/features/auth/schemas";

export default function LoginUsuario() {
  const login = useLoginUsuario();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginUsuarioValues>({
    resolver: zodResolver(loginUsuarioSchema),
    defaultValues: { email: "", password: "" },
  });

  return (
    <div className="mx-auto max-w-sm px-6 py-16">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
        Iniciar sesión
      </h1>

      <form
        noValidate
        onSubmit={handleSubmit((values) => login.mutate(values))}
        className="mt-8 flex flex-col gap-4"
      >
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
          Iniciar sesión
        </Button>
      </form>

      <div className="mt-8 flex flex-col gap-1 text-sm text-slate-600">
        <Link to="/login/colaborador" className="text-brand-700 hover:underline">
          ¿Eres colaborador?
        </Link>
        <Link to="/pedir-acceso" className="text-brand-700 hover:underline">
          ¿No tienes cuenta? Pedir acceso
        </Link>
      </div>
    </div>
  );
}
