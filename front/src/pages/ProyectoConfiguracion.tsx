import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { Link, useNavigate, useParams } from "react-router";

import { SeccionColaboradores } from "@/components/proyecto/SeccionColaboradores";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import {
  useColaboradores,
  useEliminarProyecto,
  useProyecto,
  useRenombrarProyecto,
} from "@/features/proyectos/hooks";
import {
  nuevoProyectoSchema,
  type NuevoProyectoValues,
} from "@/features/proyectos/schemas";

/**
 * Todo lo que se administra de un proyecto, junto y fuera de la vista de trabajo: el nombre,
 * quiénes participan y el borrado.
 */
export default function ProyectoConfiguracion() {
  const { proyectoId: proyectoIdParam } = useParams();
  const proyectoId = Number(proyectoIdParam);

  const proyecto = useProyecto(proyectoId);

  if (proyecto.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando proyecto…</span>
      </div>
    );
  }

  if (proyecto.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {proyecto.error.message}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-10">
      <header>
        <Link
          to={`/proyectos/${proyectoId}`}
          className="text-sm text-slate-500 hover:text-slate-700"
        >
          ← {proyecto.data.nombre}
        </Link>

        <h1 className="mt-2 font-serif text-3xl font-bold tracking-tight text-slate-900">
          Configuración del proyecto
        </h1>
      </header>

      <SeccionNombre proyectoId={proyectoId} nombre={proyecto.data.nombre} />

      <div className="border-t border-slate-200 pt-10">
        <SeccionColaboradores proyectoId={proyectoId} />
      </div>

      <div className="border-t border-slate-200 pt-10">
        <SeccionEliminar proyectoId={proyectoId} nombre={proyecto.data.nombre} />
      </div>
    </div>
  );
}

function SeccionNombre({ proyectoId, nombre }: { proyectoId: number; nombre: string }) {
  const renombrar = useRenombrarProyecto(proyectoId);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<NuevoProyectoValues>({
    resolver: zodResolver(nuevoProyectoSchema),
    values: { nombre },
  });

  // useWatch y no watch(): es la API que el compilador de React sabe memoizar.
  const cambiado = useWatch({ control, name: "nombre" }) !== nombre;

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">Nombre</h2>
        <p className="mt-0.5 text-sm text-slate-500">
          Es único entre todos los proyectos de la aplicación.
        </p>
      </div>

      <form
        noValidate
        onSubmit={handleSubmit((values) =>
          renombrar.mutate(values, { onSuccess: () => reset(values) }),
        )}
        className="flex flex-col gap-3"
      >
        <div className="max-w-md">
          <Field label="Nombre del proyecto" htmlFor="nombre" error={errors.nombre?.message}>
            <Input id="nombre" invalid={Boolean(errors.nombre)} {...register("nombre")} />
          </Field>
        </div>

        {renombrar.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {renombrar.error.message}
          </p>
        )}

        {cambiado && (
          <div className="flex flex-wrap gap-2">
            <Button type="submit" loading={renombrar.isPending}>
              Guardar nombre
            </Button>
            <Button
              type="button"
              variant="ghost"
              disabled={renombrar.isPending}
              onClick={() => {
                renombrar.reset();
                reset({ nombre });
              }}
            >
              Descartar
            </Button>
          </div>
        )}
      </form>
    </section>
  );
}

function SeccionEliminar({ proyectoId, nombre }: { proyectoId: number; nombre: string }) {
  const navigate = useNavigate();
  const eliminar = useEliminarProyecto();
  const colaboradores = useColaboradores(proyectoId, true);

  const [abierto, setAbierto] = useState(false);
  const [confirmacion, setConfirmacion] = useState("");

  const cantidad = colaboradores.data?.length ?? 0;
  // Comparación exacta: la gracia de escribir el nombre es tener que leerlo antes.
  const coincide = confirmacion === nombre;

  const cerrar = () => {
    eliminar.reset();
    setConfirmacion("");
    setAbierto(false);
  };

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">Eliminar el proyecto</h2>
        <p className="mt-0.5 text-sm text-slate-500">
          Se borran el proyecto y todo lo escrito en sus pasos. No se puede deshacer.
        </p>
      </div>

      <div>
        {/* El rojo pleno queda para el botón que confirma; éste sólo abre el diálogo. */}
        <Button
          variant="secondary"
          className="border-red-200 text-red-700 hover:bg-red-50"
          onClick={() => setAbierto(true)}
        >
          Eliminar proyecto
        </Button>
      </div>

      <Modal
        open={abierto}
        title={`Eliminar "${nombre}"`}
        description="Se borran el proyecto, sus 37 documentos y todo su historial de cambios. No hay vuelta atrás."
        onClose={cerrar}
        dismissable={!eliminar.isPending}
      >
        <div className="flex flex-col gap-5">
          {cantidad > 0 && (
            <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
              {cantidad === 1
                ? "Su colaborador pierde el acceso"
                : `Sus ${cantidad} colaboradores pierden el acceso`}{" "}
              y su cuenta deja de existir.
            </p>
          )}

          <Field
            label="Escribí el nombre del proyecto para confirmar"
            htmlFor="confirmacionNombre"
          >
            <Input
              id="confirmacionNombre"
              autoComplete="off"
              placeholder={nombre}
              value={confirmacion}
              onChange={(event) => setConfirmacion(event.target.value)}
            />
          </Field>

          {eliminar.isError && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {eliminar.error.message}
            </p>
          )}

          <div className="flex justify-end gap-2">
            <Button variant="ghost" disabled={eliminar.isPending} onClick={cerrar}>
              Cancelar
            </Button>
            <Button
              variant="danger"
              disabled={!coincide}
              loading={eliminar.isPending}
              onClick={() =>
                eliminar.mutate(proyectoId, {
                  onSuccess: () => navigate("/proyectos", { replace: true }),
                })
              }
            >
              Eliminar proyecto
            </Button>
          </div>
        </div>
      </Modal>
    </section>
  );
}
