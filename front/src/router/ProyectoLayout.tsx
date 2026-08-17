import { Link, NavLink, Outlet, useParams } from "react-router";

import { Button } from "@/components/ui/Button";
import { Spinner } from "@/components/ui/Spinner";
import { useFases } from "@/features/pipoe/hooks";
import { useProyecto } from "@/features/proyectos/hooks";
import { cn } from "@/lib/cn";
import { useAuthStore } from "@/stores/auth";

/**
 * Marco de trabajo de un proyecto: una columna izquierda con la identidad del proyecto y sus
 * 5 fases, y el contenido al lado.
 *
 * Las fases no son secuenciales, así que todas están siempre disponibles; la barra muestra el
 * avance de cada una para orientar sin imponer un orden.
 */
export function ProyectoLayout() {
  const { proyectoId: proyectoIdParam } = useParams();
  const proyectoId = Number(proyectoIdParam);

  const sesion = useAuthStore((state) => state.sesion);
  const esDuenio = sesion?.type === "USUARIO";

  const proyecto = useProyecto(proyectoId);
  const fases = useFases(proyectoId);

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
      <div className="flex flex-col items-start gap-4">
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {proyecto.error.message}
        </p>
        {esDuenio && (
          <Link to="/proyectos">
            <Button variant="secondary">Volver a mis proyectos</Button>
          </Link>
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
      <aside className="flex flex-col gap-4 lg:w-64 lg:shrink-0">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h1 className="font-serif text-2xl font-bold tracking-tight text-slate-900">
              {proyecto.data.nombre}
            </h1>
            <p className="mt-0.5 truncate text-sm text-slate-500">
              {proyecto.data.usuarioNombreCompleto}
            </p>
          </div>

          {/* Todo lo que se administra del proyecto vive detrás de esta ruedita: el nombre,
              los colaboradores y el borrado. La vista de trabajo queda sólo para trabajar. */}
          {esDuenio && (
            <Link
              to={`/proyectos/${proyectoId}/configuracion`}
              aria-label="Configuración del proyecto"
              title="Configuración del proyecto"
              className="mt-0.5 shrink-0 rounded-lg p-1.5 text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
            >
              <IconoEngranaje />
            </Link>
          )}
        </div>

        <nav
          aria-label="Fases del proyecto"
          className="flex gap-2 overflow-x-auto pb-2 lg:flex-col lg:overflow-visible lg:pb-0"
        >
          {fases.isSuccess &&
            fases.data.map((fase) => (
              <NavLink
                key={fase.fase}
                to={`/proyectos/${proyectoId}/fases/${fase.fase}`}
                className={({ isActive }) =>
                  cn(
                    "min-w-44 rounded-xl border px-4 py-3 transition-colors lg:min-w-0",
                    isActive
                      ? "border-brand-300 bg-brand-50"
                      : "border-slate-200 bg-white hover:bg-slate-50",
                  )
                }
              >
                <span className="block text-sm font-medium text-slate-900">
                  {fase.nombre}
                </span>
                <span className="mt-0.5 block text-xs text-slate-500">
                  {fase.ideaCentral}
                </span>

                <span
                  aria-hidden
                  className="mt-2 block h-1.5 overflow-hidden rounded-full bg-slate-200"
                >
                  <span
                    className={cn(
                      "block h-full rounded-full transition-all",
                      fase.pasosCompletados === fase.totalPasos
                        ? "bg-green-500"
                        : "bg-brand-500",
                    )}
                    style={{
                      width: `${(fase.pasosCompletados / fase.totalPasos) * 100}%`,
                    }}
                  />
                </span>
              </NavLink>
            ))}

          {fases.isPending && (
            <div className="flex items-center gap-2 text-sm text-slate-500">
              <Spinner />
              <span>Cargando fases…</span>
            </div>
          )}

          {fases.isError && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {fases.error.message}
            </p>
          )}
        </nav>
      </aside>

      <div className="min-w-0 flex-1">
        <Outlet />
      </div>
    </div>
  );
}

function IconoEngranaje() {
  return (
    <svg
      aria-hidden
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.7}
      strokeLinecap="round"
      strokeLinejoin="round"
      className="size-5"
    >
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1.08-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1.08 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  );
}
