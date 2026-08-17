import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import {
  useActualizarConfiguracion,
  useConfiguracion,
} from "@/features/parametros/hooks";
import { cuposSchema, type CuposValues } from "@/features/parametros/schemas";

/**
 * Los cupos de la aplicación. Son decisión de la dueña, no del código: hasta ahora estaban
 * fijos en la base y cambiarlos pedía entrar a mano por SQL.
 */
export default function AdminAjustes() {
  const configuracion = useConfiguracion();

  if (configuracion.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando ajustes…</span>
      </div>
    );
  }

  if (configuracion.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {configuracion.error.message}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">Ajustes</h1>
        <p className="mt-1 text-sm text-slate-600">
          Cuánto puede crear cada persona. Los cambios valen para todas las cuentas, también
          para las que ya existen.
        </p>
      </header>

      <FormularioCupos
        maxProyectosPorUsuario={configuracion.data.maxProyectosPorUsuario}
        maxColaboradoresPorProyecto={configuracion.data.maxColaboradoresPorProyecto}
      />
    </div>
  );
}

function FormularioCupos(valores: CuposValues) {
  const actualizar = useActualizarConfiguracion();

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<CuposValues>({
    resolver: zodResolver(cuposSchema),
    values: valores,
  });

  // useWatch y no watch(): es la API que el compilador de React sabe memoizar.
  const proyectos = useWatch({ control, name: "maxProyectosPorUsuario" });
  const colaboradores = useWatch({ control, name: "maxColaboradoresPorProyecto" });

  const cambiado =
    proyectos !== valores.maxProyectosPorUsuario ||
    colaboradores !== valores.maxColaboradoresPorProyecto;

  return (
    <form
      noValidate
      onSubmit={handleSubmit((values) =>
        actualizar.mutate(values, { onSuccess: () => reset(values) }),
      )}
      className="flex max-w-md flex-col gap-5"
    >
      <Field
        label="Proyectos por usuario"
        htmlFor="maxProyectos"
        error={errors.maxProyectosPorUsuario?.message}
      >
        <Input
          id="maxProyectos"
          type="number"
          min={1}
          max={100}
          invalid={Boolean(errors.maxProyectosPorUsuario)}
          {...register("maxProyectosPorUsuario", { valueAsNumber: true })}
        />
      </Field>
      <p className="-mt-3 text-xs text-slate-500">
        Cuántos proyectos puede tener abiertos a la vez cada persona con cuenta.
      </p>

      <Field
        label="Colaboradores por proyecto"
        htmlFor="maxColaboradores"
        error={errors.maxColaboradoresPorProyecto?.message}
      >
        <Input
          id="maxColaboradores"
          type="number"
          min={1}
          max={100}
          invalid={Boolean(errors.maxColaboradoresPorProyecto)}
          {...register("maxColaboradoresPorProyecto", { valueAsNumber: true })}
        />
      </Field>
      <p className="-mt-3 text-xs text-slate-500">
        A cuánta gente puede invitar el creador de un proyecto.
      </p>

      <p className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-600">
        Bajar un cupo no borra nada: quien ya esté por encima del nuevo número conserva lo que
        tiene, pero no puede crear más hasta quedar por debajo.
      </p>

      {actualizar.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {actualizar.error.message}
        </p>
      )}

      {actualizar.isSuccess && !cambiado && (
        <p className="rounded-lg bg-green-50 px-3 py-2 text-sm text-green-800">
          Ajustes guardados.
        </p>
      )}

      {cambiado && (
        <div className="flex flex-wrap gap-2">
          <Button type="submit" loading={actualizar.isPending}>
            Guardar ajustes
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={actualizar.isPending}
            onClick={() => {
              actualizar.reset();
              reset(valores);
            }}
          >
            Descartar
          </Button>
        </div>
      )}
    </form>
  );
}
