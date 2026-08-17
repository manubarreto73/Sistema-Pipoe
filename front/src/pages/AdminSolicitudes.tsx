import { format, parseISO } from "date-fns";
import { es } from "date-fns/locale";
import { useState, type ReactNode } from "react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Spinner } from "@/components/ui/Spinner";
import {
  FILTROS_VACIOS,
  type FiltrosSolicitudes,
} from "@/features/solicitudes/api";
import {
  useAprobarSolicitud,
  useRechazarSolicitud,
  useSolicitudes,
} from "@/features/solicitudes/hooks";
import {
  CANALES_DIFUSION,
  ESTADOS_SOLICITUD,
  etiquetaDe,
  GENEROS,
  NIVELES_INSTRUCCION,
  OCUPACIONES,
  RANGOS_EDAD,
  USOS_PREVISTOS,
  type EstadoSolicitud,
  type SolicitudAcceso,
} from "@/features/solicitudes/types";
import { cn } from "@/lib/cn";
import { useDebounce } from "@/lib/useDebounce";

const estadoClasses: Record<EstadoSolicitud, string> = {
  PENDIENTE: "bg-amber-100 text-amber-800",
  APROBADA: "bg-green-100 text-green-800",
  RECHAZADA: "bg-slate-200 text-slate-700",
};

function formatFecha(iso: string) {
  return format(parseISO(iso), "d 'de' MMMM yyyy, HH:mm", { locale: es });
}

export default function AdminSolicitudes() {
  const [filtros, setFiltros] = useState<FiltrosSolicitudes>(FILTROS_VACIOS);
  const [page, setPage] = useState(0);

  // El texto se escribe letra por letra; la consulta espera a que la persona frene.
  const textoDiferido = useDebounce(filtros.texto);
  const solicitudes = useSolicitudes({ ...filtros, texto: textoDiferido }, page);

  const aprobar = useAprobarSolicitud();
  const rechazar = useRechazarSolicitud();

  const resolviendo = aprobar.isPending || rechazar.isPending;

  /** Cualquier cambio de filtro vuelve a la primera página: la 4 podría ya no existir. */
  const cambiar = (parcial: Partial<FiltrosSolicitudes>) => {
    setFiltros((actuales) => ({ ...actuales, ...parcial }));
    setPage(0);
  };

  const filtrado =
    filtros.estado !== FILTROS_VACIOS.estado ||
    filtros.texto !== "" ||
    filtros.desde !== "" ||
    filtros.hasta !== "";

  return (
    <div className="flex flex-col gap-6">
      <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
        Solicitudes de acceso
      </h1>

      <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex flex-wrap items-end gap-3">
          <label className="flex min-w-56 flex-1 flex-col gap-1.5">
            <span className="text-xs font-medium text-slate-500">Buscar</span>
            <Input
              type="search"
              placeholder="Nombre, email o institución…"
              value={filtros.texto}
              onChange={(event) => cambiar({ texto: event.target.value })}
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-slate-500">Estado</span>
            <Select
              value={filtros.estado ?? "TODAS"}
              onChange={(event) =>
                cambiar({
                  estado:
                    event.target.value === "TODAS"
                      ? null
                      : (event.target.value as EstadoSolicitud),
                })
              }
            >
              <option value="TODAS">Todas</option>
              {ESTADOS_SOLICITUD.map((valor) => (
                <option key={valor} value={valor}>
                  {valor.charAt(0) + valor.slice(1).toLowerCase()}
                </option>
              ))}
            </Select>
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-slate-500">Desde</span>
            <Input
              type="date"
              // Un rango al revés no devuelve nada: el propio campo lo impide.
              max={filtros.hasta || undefined}
              value={filtros.desde}
              onChange={(event) => cambiar({ desde: event.target.value })}
            />
          </label>

          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-slate-500">Hasta</span>
            <Input
              type="date"
              min={filtros.desde || undefined}
              value={filtros.hasta}
              onChange={(event) => cambiar({ hasta: event.target.value })}
            />
          </label>

          {filtrado && (
            <Button
              variant="ghost"
              onClick={() => {
                setFiltros(FILTROS_VACIOS);
                setPage(0);
              }}
            >
              Limpiar
            </Button>
          )}
        </div>

        {solicitudes.isSuccess && (
          <p className="text-sm text-slate-500">
            {solicitudes.data.totalElements === 0
              ? "Ninguna solicitud coincide"
              : `${solicitudes.data.totalElements} ${
                  solicitudes.data.totalElements === 1 ? "solicitud" : "solicitudes"
                }`}
          </p>
        )}
      </div>

      {solicitudes.isPending && (
        <div className="flex items-center gap-2 text-slate-500">
          <Spinner />
          <span>Cargando solicitudes…</span>
        </div>
      )}

      {solicitudes.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {solicitudes.error.message}
        </p>
      )}

      {(aprobar.isError || rechazar.isError) && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {(aprobar.error ?? rechazar.error)?.message}
        </p>
      )}

      {solicitudes.isSuccess &&
        (solicitudes.data.empty ? (
          <p className="text-slate-600">
            No hay solicitudes con estos filtros.
            {filtrado && " Probá limpiarlos para ver todas."}
          </p>
        ) : (
          <>
            <ul className="flex flex-col gap-4">
              {solicitudes.data.content.map((solicitud) => (
                <SolicitudCard
                  key={solicitud.id}
                  solicitud={solicitud}
                  resolviendo={resolviendo}
                  onAprobar={() => aprobar.mutate(solicitud.id)}
                  onRechazar={() => rechazar.mutate(solicitud.id)}
                />
              ))}
            </ul>

            <div className="flex items-center justify-between border-t border-slate-200 pt-4">
              <span className="text-sm text-slate-500">
                Página {solicitudes.data.number + 1} de {solicitudes.data.totalPages} ·{" "}
                {solicitudes.data.totalElements} en total
              </span>

              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  disabled={solicitudes.data.first}
                  onClick={() => setPage((actual) => Math.max(0, actual - 1))}
                >
                  Anterior
                </Button>
                <Button
                  variant="secondary"
                  disabled={solicitudes.data.last}
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

function Dato({
  etiqueta,
  ancho = false,
  children,
}: {
  etiqueta: string;
  ancho?: boolean;
  children: ReactNode;
}) {
  return (
    <div className={ancho ? "sm:col-span-2" : undefined}>
      <dt className="text-slate-500">{etiqueta}</dt>
      <dd className="text-slate-800">{children}</dd>
    </div>
  );
}

/** Lista de opciones marcadas, con el texto libre de "Otro" al final si lo hay. */
function Etiquetas({ valores, otro }: { valores: string[]; otro: string | null }) {
  const items = [...valores.filter((valor) => valor !== "Otro"), ...(otro ? [otro] : [])];

  if (items.length === 0) return <>—</>;

  return (
    <span className="flex flex-wrap gap-1.5">
      {items.map((item) => (
        <span key={item} className="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs">
          {item}
        </span>
      ))}
    </span>
  );
}

type SolicitudCardProps = {
  solicitud: SolicitudAcceso;
  resolviendo: boolean;
  onAprobar: () => void;
  onRechazar: () => void;
};

function SolicitudCard({
  solicitud,
  resolviendo,
  onAprobar,
  onRechazar,
}: SolicitudCardProps) {
  // Aprobar crea el usuario y le manda la clave por mail, y rechazar no se puede
  // deshacer. Un paso de confirmación evita que un click al pasar arruine las dos.
  const [confirmando, setConfirmando] = useState<"aprobar" | "rechazar" | null>(null);

  return (
    <li className="rounded-xl border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium text-slate-900">{solicitud.nombreCompleto}</p>
          <p className="text-sm text-slate-500">{solicitud.email}</p>
        </div>

        <span
          className={cn(
            "rounded-full px-2.5 py-1 text-xs font-medium",
            estadoClasses[solicitud.estado],
          )}
        >
          {solicitud.estado}
        </span>
      </div>

      <dl className="mt-4 grid gap-x-6 gap-y-3 text-sm sm:grid-cols-2">
        <Dato etiqueta="Nivel de instrucción">
          {etiquetaDe(NIVELES_INSTRUCCION, solicitud.nivelInstruccion)}
        </Dato>
        <Dato etiqueta="Género">{etiquetaDe(GENEROS, solicitud.genero)}</Dato>
        <Dato etiqueta="Edad">{etiquetaDe(RANGOS_EDAD, solicitud.rangoEdad)}</Dato>
        <Dato etiqueta="Ocupación">
          {solicitud.ocupacion === "OTRA" && solicitud.ocupacionOtra
            ? solicitud.ocupacionOtra
            : etiquetaDe(OCUPACIONES, solicitud.ocupacion)}
        </Dato>

        <Dato etiqueta="País de nacimiento">{solicitud.paisNacimiento}</Dato>
        <Dato etiqueta="País de residencia">{solicitud.paisResidencia ?? "El mismo"}</Dato>

        <Dato etiqueta="Institución u organización" ancho>
          {solicitud.institucion}
        </Dato>

        <Dato etiqueta="¿Por qué le interesa?" ancho>
          <span className="whitespace-pre-wrap">{solicitud.motivacion}</span>
        </Dato>

        <Dato etiqueta="Usos previstos" ancho>
          <Etiquetas
            valores={solicitud.usos.map((uso) => etiquetaDe(USOS_PREVISTOS, uso))}
            otro={solicitud.usosOtro}
          />
        </Dato>

        <Dato etiqueta="Cómo se enteró" ancho>
          <Etiquetas
            valores={solicitud.canales.map((canal) => etiquetaDe(CANALES_DIFUSION, canal))}
            otro={solicitud.canalOtro}
          />
        </Dato>
      </dl>

      <p className="mt-4 text-xs text-slate-400">
        Solicitada el {formatFecha(solicitud.fechaSolicitud)}
        {solicitud.fechaResolucion &&
          ` · Resuelta el ${formatFecha(solicitud.fechaResolucion)}`}
      </p>

      {solicitud.estado === "PENDIENTE" &&
        (confirmando === null ? (
          <div className="mt-4 flex gap-2">
            <Button disabled={resolviendo} onClick={() => setConfirmando("aprobar")}>
              Aprobar
            </Button>
            <Button
              variant="secondary"
              disabled={resolviendo}
              onClick={() => setConfirmando("rechazar")}
            >
              Rechazar
            </Button>
          </div>
        ) : (
          <div className="mt-4 flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 p-3">
            <p className="text-sm text-slate-700">
              {confirmando === "aprobar"
                ? `Se va a crear la cuenta de ${solicitud.nombreCompleto} y enviarle la contraseña a ${solicitud.email}.`
                : "La solicitud queda rechazada. No se envía ningún mail."}
            </p>
            <div className="flex gap-2">
              <Button
                disabled={resolviendo}
                onClick={confirmando === "aprobar" ? onAprobar : onRechazar}
              >
                Confirmar
              </Button>
              <Button
                variant="ghost"
                disabled={resolviendo}
                onClick={() => setConfirmando(null)}
              >
                Cancelar
              </Button>
            </div>
          </div>
        ))}
    </li>
  );
}
