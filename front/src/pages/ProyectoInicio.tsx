import { Link, useParams } from "react-router";

import { Spinner } from "@/components/ui/Spinner";
import { useFases } from "@/features/pipoe/hooks";

/**
 * Portada del proyecto: el estado de las 5 fases de un vistazo. La barra lateral ya permite
 * saltar a cualquiera, así que acá el foco está en ver dónde se viene trabajando.
 */
export default function ProyectoInicio() {
  const { proyectoId: proyectoIdParam } = useParams();
  const proyectoId = Number(proyectoIdParam);

  const fases = useFases(proyectoId);

  if (fases.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando fases…</span>
      </div>
    );
  }

  if (fases.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {fases.error.message}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="text-lg font-semibold text-slate-900">Modelo PipoE</h2>
        <p className="mt-1 text-sm text-slate-600">
          Las fases no son secuenciales: puedes avanzar en cualquiera cuando quieras.
        </p>
      </div>

      <ul className="grid gap-4 sm:grid-cols-2">
        {fases.data.map((fase) => (
          <li key={fase.fase}>
            <Link
              to={`/proyectos/${proyectoId}/fases/${fase.fase}`}
              className="flex h-full flex-col justify-between gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition-all hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
            >
              <div>
                <h3 className="font-semibold text-slate-900">{fase.nombre}</h3>
                <p className="mt-0.5 text-sm text-slate-500">{fase.ideaCentral}</p>
              </div>

              <div>
                <span
                  aria-hidden
                  className="block h-1.5 overflow-hidden rounded-full bg-slate-200"
                >
                  <span
                    className={
                      fase.pasosCompletados === fase.totalPasos
                        ? "block h-full rounded-full bg-green-500"
                        : "block h-full rounded-full bg-brand-500"
                    }
                    style={{
                      width: `${(fase.pasosCompletados / fase.totalPasos) * 100}%`,
                    }}
                  />
                </span>

                <p className="mt-2 text-xs text-slate-500">
                  Producto: {fase.producto}
                  {fase.productoCompletado
                    ? " · terminado"
                    : fase.productoHabilitado
                      ? " · listo para cerrar"
                      : ""}
                </p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
