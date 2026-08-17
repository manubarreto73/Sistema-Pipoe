import { Link, useParams } from "react-router";

import { Spinner } from "@/components/ui/Spinner";
import { useFases, usePasosDeFase } from "@/features/pipoe/hooks";
import {
  ESTADO_CLASES,
  ESTADO_ETIQUETAS,
  type Fase,
  type PasoResumen,
} from "@/features/pipoe/types";
import { cn } from "@/lib/cn";

/**
 * Los pasos de una fase como un flujo de eventos: cuadrados conectados por flechas.
 *
 * El orden que dibujan las flechas es el del modelo, pero no es una cadena cerrada: se puede
 * entrar a cualquier paso siempre. Lo único que respeta el orden es *completar* un paso, que
 * exige haber empezado el anterior.
 */
export default function FaseFlujo() {
  const { proyectoId: proyectoIdParam, fase } = useParams();
  const proyectoId = Number(proyectoIdParam);

  const pasos = usePasosDeFase(proyectoId, fase as Fase | undefined);
  const fases = useFases(proyectoId);
  const resumen = fases.data?.find((item) => item.fase === fase);

  if (pasos.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando pasos…</span>
      </div>
    );
  }

  if (pasos.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {pasos.error.message}
      </p>
    );
  }

  const delDespliegue = pasos.data.filter((paso) => !paso.esProducto);
  const producto = pasos.data.find((paso) => paso.esProducto);

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h2 className="text-lg font-semibold text-slate-900">
          {resumen?.nombre ?? "Fase"}
        </h2>
        {resumen && (
          <p className="mt-1 text-sm text-slate-600">
            {resumen.ideaCentral}
            {resumen.nivel !== "EDICION" && (
              <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                {resumen.nivel === "COMENTARIOS" ? "Comentarios" : "Sólo lectura"}
              </span>
            )}
          </p>
        )}
      </header>

      <ol className="flex flex-wrap items-stretch gap-x-1 gap-y-4">
        {delDespliegue.map((paso, indice) => (
          <li key={paso.pasoId} className="flex items-stretch">
            <PasoCuadrado paso={paso} proyectoId={proyectoId} />

            {indice < delDespliegue.length - 1 && (
              <span
                aria-hidden
                className="flex w-8 shrink-0 items-center justify-center text-slate-300"
              >
                →
              </span>
            )}
          </li>
        ))}
      </ol>

      {producto && (
        <section className="border-t border-slate-200 pt-6">
          <h3 className="text-sm font-medium text-slate-700">Producto de la fase</h3>
          <p className="mt-1 text-sm text-slate-500">
            {producto.puedeCompletarse || producto.estado !== "PENDIENTE"
              ? "Se puede cerrar: la fase está completa."
              : "Se habilita para cerrar cuando estén completos todos los pasos de arriba, pero podés ir escribiéndolo desde ahora."}
          </p>

          <div className="mt-4 max-w-xs">
            <PasoCuadrado paso={producto} proyectoId={proyectoId} />
          </div>
        </section>
      )}
    </div>
  );
}

function PasoCuadrado({ paso, proyectoId }: { paso: PasoResumen; proyectoId: number }) {
  return (
    <Link
      to={`/proyectos/${proyectoId}/pasos/${paso.pasoId}`}
      title={paso.titulo}
      className={cn(
        "flex w-44 flex-col justify-between gap-3 rounded-xl border-2 p-3 transition-all hover:-translate-y-0.5 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600",
        ESTADO_CLASES[paso.estado],
      )}
    >
      <div>
        <span className="text-xs font-medium opacity-70">
          {paso.esProducto ? "Producto" : `Paso ${paso.orden}`}
        </span>
        <p className="mt-1 text-sm leading-snug font-medium">{paso.tituloCorto}</p>
      </div>

      <span className="text-xs opacity-80">{ESTADO_ETIQUETAS[paso.estado]}</span>
    </Link>
  );
}
