import { useState } from "react";

import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Spinner } from "@/components/ui/Spinner";
import { Textarea } from "@/components/ui/Textarea";
import { useActualizarPasoCatalogo, useCatalogo } from "@/features/pipoe/hooks";
import { FASES, type PasoCatalogo } from "@/features/pipoe/types";
import { cn } from "@/lib/cn";

/**
 * Carga de la explicación y el ejemplo de los 37 pasos del modelo.
 *
 * Es contenido de la metodología, igual para todos los proyectos, así que lo edita el ADMIN.
 * El título y la fase no se tocan desde acá: cambiarlos sería editar el modelo PipoE sin
 * dejar rastro, y para eso está la migración.
 */
export default function AdminCatalogo() {
  const catalogo = useCatalogo();
  const [editando, setEditando] = useState<PasoCatalogo | null>(null);

  if (catalogo.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando catálogo…</span>
      </div>
    );
  }

  if (catalogo.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {catalogo.error.message}
      </p>
    );
  }

  const cargados = catalogo.data.filter((paso) => paso.contenidoCargado).length;

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
          Contenido de los pasos
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          {cargados} de {catalogo.data.length} pasos tienen explicación y ejemplo cargados.
          Esto es igual para todos los proyectos.
        </p>
      </header>

      {FASES.map((fase) => {
        const pasos = catalogo.data.filter((paso) => paso.fase === fase);
        if (pasos.length === 0) return null;

        return (
          <section key={fase}>
            <h2 className="text-lg font-semibold text-slate-900">{pasos[0].faseNombre}</h2>

            <ul className="mt-3 divide-y divide-slate-200 overflow-hidden rounded-xl border border-slate-200 bg-white">
              {pasos.map((paso) => (
                <li
                  key={paso.id}
                  className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
                >
                  <div className="min-w-0">
                    <p className="flex items-center gap-2 text-sm font-medium text-slate-800">
                      <span
                        aria-hidden
                        className={cn(
                          "size-2 shrink-0 rounded-full",
                          paso.contenidoCargado ? "bg-green-500" : "bg-slate-300",
                        )}
                      />
                      {paso.esProducto ? "Producto" : `${paso.orden}.`} {paso.tituloCorto}
                    </p>
                    <p className="mt-0.5 truncate text-xs text-slate-500">{paso.titulo}</p>
                  </div>

                  <Button variant="secondary" onClick={() => setEditando(paso)}>
                    {paso.contenidoCargado ? "Editar" : "Cargar"}
                  </Button>
                </li>
              ))}
            </ul>
          </section>
        );
      })}

      {editando && (
        <EditarPasoModal paso={editando} onClose={() => setEditando(null)} />
      )}
    </div>
  );
}

function EditarPasoModal({ paso, onClose }: { paso: PasoCatalogo; onClose: () => void }) {
  const actualizar = useActualizarPasoCatalogo();

  const [tituloCorto, setTituloCorto] = useState(paso.tituloCorto);
  const [explicacion, setExplicacion] = useState(paso.explicacion);
  const [ejemplo, setEjemplo] = useState(paso.ejemplo);

  return (
    <Modal
      open
      title={paso.tituloCorto}
      description={paso.titulo}
      onClose={onClose}
      dismissable={!actualizar.isPending}
      className="max-w-2xl"
    >
      <form
        noValidate
        onSubmit={(event) => {
          event.preventDefault();
          actualizar.mutate(
            { pasoId: paso.id, tituloCorto, explicacion, ejemplo },
            { onSuccess: onClose },
          );
        }}
        className="flex flex-col gap-4"
      >
        <Field label="Título corto" htmlFor="tituloCorto">
          <Input
            id="tituloCorto"
            maxLength={60}
            value={tituloCorto}
            onChange={(event) => setTituloCorto(event.target.value)}
          />
        </Field>
        <p className="-mt-2 text-xs text-slate-500">
          Es la etiqueta que se ve en el diagrama de flujo, donde el título completo no entra.
        </p>

        <Field label="Explicación" htmlFor="explicacion">
          <Textarea
            id="explicacion"
            rows={6}
            placeholder="Cómo se completa este paso…"
            value={explicacion}
            onChange={(event) => setExplicacion(event.target.value)}
          />
        </Field>

        <Field label="Ejemplo" htmlFor="ejemplo">
          <Textarea
            id="ejemplo"
            rows={6}
            placeholder="Un ejemplo de referencia…"
            value={ejemplo}
            onChange={(event) => setEjemplo(event.target.value)}
          />
        </Field>

        {actualizar.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {actualizar.error.message}
          </p>
        )}

        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="ghost"
            disabled={actualizar.isPending}
            onClick={onClose}
          >
            Cancelar
          </Button>
          <Button type="submit" loading={actualizar.isPending}>
            Guardar
          </Button>
        </div>
      </form>
    </Modal>
  );
}
