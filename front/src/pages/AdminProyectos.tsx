import { useState } from "react";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Spinner } from "@/components/ui/Spinner";
import { FASES, type Fase } from "@/features/pipoe/types";
import {
  FILTROS_PROYECTOS_VACIOS,
  type EstadoFase,
  type FiltrosProyectos,
  type ProyectoAdmin,
} from "@/features/proyectos/adminApi";
import { useProyectosAdmin } from "@/features/proyectos/hooks";
import { cn } from "@/lib/cn";
import { useDebounce } from "@/lib/useDebounce";

/** Abreviatura de cada fase para el indicador, donde el nombre entero no entra. */
const ABREVIATURA: Record<Fase, string> = {
  PROMOCION: "Pro",
  INDAGACION: "Ind",
  PROGRAMACION: "Prg",
  ORGANIZACION: "Org",
  EVALUACION: "Eva",
};

const NOMBRE_FASE: Record<Fase, string> = {
  PROMOCION: "Promoción",
  INDAGACION: "Indagación",
  PROGRAMACION: "Programación",
  ORGANIZACION: "Organización",
  EVALUACION: "Evaluación",
};

/**
 * Los tres estados no se distinguen sólo por el color: cada casilla lleva la abreviatura de la
 * fase y un `title` que dice en qué anda. Un indicador que sólo cambia de color deja afuera a
 * quien no distingue el verde del ámbar, que es más gente de la que uno supone.
 */
const CLASES_ESTADO: Record<EstadoFase, string> = {
  COMPLETA: "bg-green-600 text-white",
  EN_PROGRESO: "bg-amber-200 text-amber-900",
  SIN_EMPEZAR: "bg-slate-100 text-slate-400",
};

const TEXTO_ESTADO: Record<EstadoFase, string> = {
  COMPLETA: "completa",
  EN_PROGRESO: "en curso",
  SIN_EMPEZAR: "sin empezar",
};

export default function AdminProyectos() {
  const [filtros, setFiltros] = useState<FiltrosProyectos>(FILTROS_PROYECTOS_VACIOS);
  const [page, setPage] = useState(0);

  // El texto se escribe letra por letra; la consulta espera a que la persona frene.
  const textoDiferido = useDebounce(filtros.texto);
  const proyectos = useProyectosAdmin({ ...filtros, texto: textoDiferido }, page);

  /** Cualquier cambio de filtro vuelve a la primera página: la 4 podría ya no existir. */
  const cambiar = (parcial: Partial<FiltrosProyectos>) => {
    setFiltros((actuales) => ({ ...actuales, ...parcial }));
    setPage(0);
  };

  return (
    <div className="flex flex-col gap-6">
      <header>
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
          Proyectos
        </h1>
        <p className="mt-2 text-sm text-slate-600">
          Todos los proyectos del sistema. Al entrar en uno puedes leerlo y dejar comentarios en
          cualquier paso, pero no editar lo que escribieron.
        </p>
      </header>

      <div className="flex flex-col gap-3 sm:flex-row">
        <Input
          type="search"
          placeholder="Buscar por proyecto, código o responsable…"
          aria-label="Buscar proyectos"
          className="sm:flex-1"
          value={filtros.texto}
          onChange={(e) => cambiar({ texto: e.target.value })}
        />

        <Select
          aria-label="Filtrar por estado"
          value={filtros.terminado === null ? "" : String(filtros.terminado)}
          onChange={(e) =>
            cambiar({ terminado: e.target.value === "" ? null : e.target.value === "true" })
          }
        >
          <option value="">Todos los estados</option>
          <option value="false">Sin terminar</option>
          <option value="true">Terminados</option>
        </Select>
      </div>

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

      {proyectos.isSuccess &&
        (proyectos.data.empty ? (
          <p className="rounded-xl border border-slate-200 bg-white px-4 py-8 text-center text-sm text-slate-500">
            {filtros.texto || filtros.terminado !== null
              ? "Ningún proyecto coincide con la búsqueda."
              : "Todavía no hay proyectos en el sistema."}
          </p>
        ) : (
          <>
            <ul className="flex flex-col gap-3">
              {proyectos.data.content.map((proyecto) => (
                <ProyectoFila key={proyecto.id} proyecto={proyecto} />
              ))}
            </ul>

            <div className="flex items-center justify-between border-t border-slate-200 pt-4">
              <span className="text-sm text-slate-500">
                Página {proyectos.data.number + 1} de {proyectos.data.totalPages} ·{" "}
                {proyectos.data.totalElements} en total
              </span>

              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  disabled={proyectos.data.first}
                  onClick={() => setPage((actual) => Math.max(0, actual - 1))}
                >
                  Anterior
                </Button>
                <Button
                  variant="secondary"
                  disabled={proyectos.data.last}
                  onClick={() => setPage((actual) => actual + 1)}
                >
                  Siguiente
                </Button>
              </div>
            </div>
          </>
        ))}
    </div>
  );
}

function ProyectoFila({ proyecto }: { proyecto: ProyectoAdmin }) {
  const completas = FASES.filter((fase) => proyecto.fases[fase] === "COMPLETA").length;
  const equipo =
    proyecto.colaboradores === 1 ? "1 colaborador" : proyecto.colaboradores + " colaboradores";

  return (
    <li>
      <Link
        to={"/proyectos/" + proyecto.id}
        className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white px-4 py-3 transition-colors hover:border-brand-300 hover:bg-brand-50/40 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
      >
        <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
          <h2 className="font-serif text-lg font-semibold text-slate-900">{proyecto.nombre}</h2>

          {proyecto.terminado && (
            <span className="rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
              Terminado
            </span>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-slate-600">
          <span>{proyecto.duenio}</span>
          <span className="text-slate-300" aria-hidden>
            ·
          </span>
          <span>{equipo}</span>
          <span className="text-slate-300" aria-hidden>
            ·
          </span>
          <span className="font-mono text-xs text-slate-500">{proyecto.codigo}</span>
        </div>

        <div className="flex flex-wrap items-center gap-1.5">
          {FASES.map((fase) => (
            <span
              key={fase}
              title={NOMBRE_FASE[fase] + ": " + TEXTO_ESTADO[proyecto.fases[fase]]}
              className={cn(
                "rounded px-2 py-0.5 text-xs font-medium",
                CLASES_ESTADO[proyecto.fases[fase]],
              )}
            >
              {ABREVIATURA[fase]}
            </span>
          ))}

          <span className="ml-1 text-xs text-slate-500">
            {completas} de {FASES.length} fases completas
          </span>
        </div>
      </Link>
    </li>
  );
}
