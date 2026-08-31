import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import { useConfiguracion } from "@/features/parametros/hooks";
import { useCrearProyecto, useProyectos } from "@/features/proyectos/hooks";
import {
  nuevoProyectoSchema,
  type NuevoProyectoValues,
} from "@/features/proyectos/schemas";

export default function MisProyectos() {
  const proyectos = useProyectos();
  const configuracion = useConfiguracion();
  const [creando, setCreando] = useState(false);

  const usados = proyectos.data?.length ?? 0;
  const maximo = configuracion.data?.maxProyectosPorUsuario;
  // Mientras no sepamos el máximo no escondemos la card de alta: la API rechaza el exceso igual.
  const enElMaximo = maximo !== undefined && usados >= maximo;

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
          Mis proyectos
        </h1>

        {/* Único lugar donde se habla del cupo, y sólo al llegar: explica por qué desapareció
            la tarjeta de alta. Mientras haya lugar, el máximo no se menciona. */}
        {enElMaximo && (
          <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-800">
            Llegaste al máximo de proyectos. Para crear otro, elimina alguno.
          </p>
        )}
      </header>

      {proyectos.isPending && (
        <div className="flex items-center gap-2 text-slate-500">
          <Spinner />
          <span>Cargando proyectos…</span>
        </div>
      )}

      {proyectos.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {proyectos.error.message}
        </p>
      )}

      {proyectos.isSuccess && (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {proyectos.data.map((proyecto) => (
            <li key={proyecto.id}>
              <Link
                to={`/proyectos/${proyecto.id}`}
                className="group flex h-full flex-col justify-between gap-6 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
              >
                <div>
                  <h2 className="text-lg font-semibold text-slate-900">
                    {proyecto.nombre}
                  </h2>
                  <p className="mt-1 text-sm text-slate-500">
                    {proyecto.usuarioNombreCompleto}
                  </p>
                </div>

                <span className="text-sm font-medium text-brand-600 group-hover:text-brand-700">
                  Abrir proyecto →
                </span>
              </Link>
            </li>
          ))}

          {!enElMaximo && (
            <li>
              <button
                type="button"
                onClick={() => setCreando(true)}
                className="flex h-full min-h-40 w-full flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-slate-300 bg-white/50 p-5 text-slate-500 transition-colors hover:border-brand-400 hover:bg-white hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
              >
                <span
                  aria-hidden
                  className="flex size-10 items-center justify-center rounded-full border border-current text-2xl leading-none"
                >
                  +
                </span>
                <span className="text-sm font-medium">Nuevo proyecto</span>
              </button>
            </li>
          )}
        </ul>
      )}

      {proyectos.isSuccess && proyectos.data.length === 0 && (
        <p className="text-slate-600">
          Todavía no tienes proyectos. Crea el primero con la tarjeta de arriba.
        </p>
      )}

      <NuevoProyectoModal open={creando} onClose={() => setCreando(false)} />
    </div>
  );
}

type NuevoProyectoModalProps = {
  open: boolean;
  onClose: () => void;
};

/**
 * Alta en dos pasos dentro del mismo modal: primero el nombre, después la confirmación.
 * El nombre de un proyecto es único en toda la app y no se puede editar después, así que
 * conviene que el usuario lo lea una vez más antes de fijarlo.
 */
function NuevoProyectoModal({ open, onClose }: NuevoProyectoModalProps) {
  const crear = useCrearProyecto();
  const [confirmando, setConfirmando] = useState<NuevoProyectoValues | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NuevoProyectoValues>({
    resolver: zodResolver(nuevoProyectoSchema),
    defaultValues: { nombre: "" },
  });

  const cerrar = () => {
    setConfirmando(null);
    crear.reset();
    reset();
    onClose();
  };

  return (
    <Modal
      open={open}
      title="Nuevo proyecto"
      onClose={cerrar}
      dismissable={!crear.isPending}
    >
      {confirmando ? (
        <div className="flex flex-col gap-5">
          <p className="text-sm text-slate-700">
            Se va a crear el proyecto{" "}
            <strong className="font-medium text-slate-900">{confirmando.nombre}</strong>.
          </p>

          {crear.isError && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {crear.error.message}
            </p>
          )}

          <div className="flex justify-end gap-2">
            <Button
              variant="ghost"
              disabled={crear.isPending}
              onClick={() => setConfirmando(null)}
            >
              Volver
            </Button>
            <Button
              loading={crear.isPending}
              onClick={() => crear.mutate(confirmando, { onSuccess: cerrar })}
            >
              Crear proyecto
            </Button>
          </div>
        </div>
      ) : (
        <form
          noValidate
          onSubmit={handleSubmit((values) => setConfirmando(values))}
          className="flex flex-col gap-5"
        >
          <Field label="Nombre" htmlFor="nombre" error={errors.nombre?.message}>
            <Input id="nombre" invalid={Boolean(errors.nombre)} {...register("nombre")} />
          </Field>

          <div className="flex justify-end gap-2">
            <Button type="button" variant="ghost" onClick={cerrar}>
              Cancelar
            </Button>
            <Button type="submit">Continuar</Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
