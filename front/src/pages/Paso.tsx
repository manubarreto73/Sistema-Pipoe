import { format, isSameDay, parseISO } from "date-fns";
import { es } from "date-fns/locale";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";

import { SeccionComentarios } from "@/components/proyecto/SeccionComentarios";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Editor } from "@/components/ui/Editor";
import { HtmlSeguro } from "@/components/ui/HtmlSeguro";
import { Spinner } from "@/components/ui/Spinner";
import { latirPresencia } from "@/features/pipoe/api";
import { useEsDuenio } from "@/features/proyectos/hooks";
import {
  useCompletarPaso,
  useDiffVersion,
  useExportarProducto,
  useGuardarDocumento,
  usePaso,
  useVersiones,
} from "@/features/pipoe/hooks";
import { ESTADO_ETIQUETAS, type VersionDocumento } from "@/features/pipoe/types";

/** Cada cuánto se guarda solo, contado desde la última tecla. */
const AUTOSAVE_MS = 2500;
/** El latido de presencia caduca al minuto en el servidor; se renueva bastante antes. */
const PRESENCIA_MS = 25_000;

export default function Paso() {
  const { proyectoId: proyectoIdParam, pasoId: pasoIdParam } = useParams();
  const proyectoId = Number(proyectoIdParam);
  const pasoId = Number(pasoIdParam);

  // El historial de quién escribió qué es del creador del proyecto: ni de sus colaboradores ni
  // de la administradora, que entra a acompañar y no a auditar quién escribió cada cosa.
  const esDuenio = useEsDuenio(proyectoId);

  const paso = usePaso(proyectoId, pasoId);
  const guardar = useGuardarDocumento(proyectoId, pasoId);
  const completar = useCompletarPaso(proyectoId, pasoId);
  const exportar = useExportarProducto(proyectoId, pasoId);

  const [borrador, setBorrador] = useState<string | null>(null);
  const [verHistorial, setVerHistorial] = useState(false);
  const [confirmandoCierre, setConfirmandoCierre] = useState(false);
  const [editandoOtro, setEditandoOtro] = useState<string | null>(null);

  const versiones = useVersiones(proyectoId, pasoId, verHistorial && esDuenio);

  // El borrador vive acá y no en react-query: la caché se revalida sola y pisaría lo tipeado.
  const contenidoServidor = paso.data?.contenido ?? "";
  const sucio = borrador !== null && borrador !== contenidoServidor;

  // Autosave: se dispara cuando pasa un rato sin escribir, no en cada tecla. `mutate` es
  // estable entre renders, así que el timer no se reinicia solo por volver a renderizar.
  const guardarDocumento = guardar.mutate;
  const version = paso.data?.version;
  const puedeEditar = paso.data?.puedeEditar ?? false;

  useEffect(() => {
    if (!sucio || !puedeEditar || version === undefined) return;

    const timer = setTimeout(
      () => guardarDocumento({ contenido: borrador as string, version }),
      AUTOSAVE_MS,
    );

    return () => clearTimeout(timer);
  }, [borrador, sucio, puedeEditar, version, guardarDocumento]);

  // Presencia: avisa que hay alguien acá y se entera si hay otra persona.
  useEffect(() => {
    if (!paso.data?.puedeEditar) return;

    let vigente = true;
    const latir = () =>
      latirPresencia(proyectoId, pasoId)
        .then((respuesta) => vigente && setEditandoOtro(respuesta.editandoOtro))
        .catch(() => {
          // Un latido perdido no es motivo para molestar: el siguiente reintenta.
        });

    latir();
    const intervalo = setInterval(latir, PRESENCIA_MS);

    return () => {
      vigente = false;
      clearInterval(intervalo);
    };
  }, [proyectoId, pasoId, paso.data?.puedeEditar]);

  // Al navegar a otro paso, el componente no se desmonta: hay que descartar el borrador del
  // anterior. Se ajusta durante el render y no en un efecto, que dispararía un render extra
  // mostrando por un instante el texto del paso que ya dejamos atrás.
  const [pasoMostrado, setPasoMostrado] = useState(pasoId);
  if (pasoMostrado !== pasoId) {
    setPasoMostrado(pasoId);
    setBorrador(null);
  }

  if (paso.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando paso…</span>
      </div>
    );
  }

  if (paso.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {paso.error.message}
      </p>
    );
  }

  const detalle = paso.data;
  const conflicto = guardar.error?.status === 409;

  const guardarAhora = () =>
    guardar.mutate({ contenido: borrador ?? detalle.contenido, version: detalle.version });

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <Link
            to={`/proyectos/${proyectoId}/fases/${detalle.fase}`}
            className="text-sm text-slate-500 hover:text-slate-700"
          >
            ← {detalle.faseNombre}
          </Link>

          <p className="mt-2 text-xs font-medium text-slate-500">
            {detalle.esProducto ? "Producto de la fase" : `Paso ${detalle.orden}`} ·{" "}
            {ESTADO_ETIQUETAS[detalle.estado]}
          </p>
          <h2 className="mt-1 text-lg font-semibold text-slate-900">{detalle.titulo}</h2>
        </div>

        <div className="flex shrink-0 flex-wrap items-center gap-2">
          {/* El producto es el cierre de la fase: lo que se lleva y se comparte fuera de acá. */}
          {detalle.esProducto && (
            <>
              <Button
                variant="secondary"
                loading={exportar.isPending && exportar.variables === "docx"}
                disabled={exportar.isPending}
                onClick={() => exportar.mutate("docx")}
              >
                Descargar Word
              </Button>
              <Button
                variant="secondary"
                loading={exportar.isPending && exportar.variables === "pdf"}
                disabled={exportar.isPending}
                onClick={() => exportar.mutate("pdf")}
              >
                Descargar PDF
              </Button>
            </>
          )}

          {detalle.puedeEditar && (
            <>
              <EstadoGuardado
                sucio={sucio}
                guardando={guardar.isPending}
                actualizadoEn={detalle.actualizadoEn}
                actualizadoPor={detalle.actualizadoPor}
              />

              {detalle.estado === "COMPLETADO" ? (
                <Button
                  variant="secondary"
                  loading={completar.isPending}
                  onClick={() => completar.mutate(false)}
                >
                  Reabrir
                </Button>
              ) : (
                <Button
                  disabled={!detalle.puedeCompletarse}
                  title={detalle.motivoBloqueo ?? undefined}
                  onClick={() => setConfirmandoCierre(true)}
                >
                  Marcar como completado
                </Button>
              )}
            </>
          )}
        </div>
      </header>

      {exportar.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {exportar.error.message}
        </p>
      )}

      {!detalle.puedeEditar && (
        <p className="rounded-lg bg-slate-100 px-3 py-2 text-sm text-slate-700">
          {detalle.nivel === "COMENTARIOS"
            ? `Tienes permiso de comentarios en ${detalle.faseNombre}: puedes leer el documento y
               dejar comentarios, pero no editarlo.`
            : `Tienes sólo lectura en ${detalle.faseNombre}, así que puedes leer pero no editar.`}
        </p>
      )}

      {editandoOtro && (
        <p className="rounded-lg bg-amber-50 px-3 py-2 text-sm text-amber-900">
          <strong className="font-medium">{editandoOtro}</strong> tiene este paso abierto.
          Todavía no hay edición simultánea: si escriben los dos a la vez, el segundo en
          guardar va a tener que recargar.
        </p>
      )}

      {conflicto && (
        <div className="flex flex-wrap items-center gap-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          <span>{guardar.error?.message}</span>
          <Button
            variant="secondary"
            onClick={() => {
              setBorrador(null);
              guardar.reset();
              paso.refetch();
            }}
          >
            Recargar
          </Button>
        </div>
      )}

      {guardar.isError && !conflicto && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {guardar.error.message}
        </p>
      )}

      {completar.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {completar.error.message}
        </p>
      )}

      <Consigna explicacion={detalle.explicacion} ejemplo={detalle.ejemplo} />

      {detalle.puedeEditar ? (
        <Editor
          contenido={detalle.contenido}
          editable
          recargaId={detalle.version}
          onChange={setBorrador}
        />
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white px-5 py-4">
          {detalle.contenido ? (
            <HtmlSeguro html={detalle.contenido} className="prose-editor text-slate-800" />
          ) : (
            <p className="text-slate-500">Todavía no hay nada escrito en este paso.</p>
          )}
        </div>
      )}

      {detalle.puedeEditar && sucio && (
        <div className="flex gap-2">
          <Button loading={guardar.isPending} onClick={guardarAhora}>
            Guardar ahora
          </Button>
        </div>
      )}

      <SeccionComentarios proyectoId={proyectoId} pasoId={pasoId} nivel={detalle.nivel} />

      {esDuenio && (
        <section className="border-t border-slate-200 pt-4">
          <button
            type="button"
            onClick={() => setVerHistorial((abierto) => !abierto)}
            className="text-sm font-medium text-brand-600 hover:underline"
          >
            {verHistorial ? "Ocultar historial" : "Ver historial de cambios"}
          </button>

          {verHistorial && (
            <div className="mt-3">
              {versiones.isPending && (
                <div className="flex items-center gap-2 text-sm text-slate-500">
                  <Spinner />
                  <span>Cargando historial…</span>
                </div>
              )}

              {versiones.isError && (
                <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
                  {versiones.error.message}
                </p>
              )}

              {versiones.isSuccess &&
                (versiones.data.length === 0 ? (
                  <p className="text-sm text-slate-500">Todavía no hay guardados.</p>
                ) : (
                  <ul className="flex flex-col gap-2">
                    {versiones.data.map((version) => (
                      <VersionFila
                        key={version.id}
                        version={version}
                        proyectoId={proyectoId}
                        pasoId={pasoId}
                      />
                    ))}
                  </ul>
                ))}
            </div>
          )}
        </section>
      )}

      <ConfirmDialog
        open={confirmandoCierre}
        title="Marcar el paso como completado"
        confirmLabel="Marcar completado"
        loading={completar.isPending}
        error={completar.error?.message}
        description={
          detalle.esProducto
            ? "Con esto queda cerrado el producto de la fase. Puedes reabrirlo cuando quieras."
            : "Vas a poder seguir editándolo igual: completarlo sólo habilita el paso siguiente y suma al progreso de la fase."
        }
        onCancel={() => {
          completar.reset();
          setConfirmandoCierre(false);
        }}
        onConfirm={() =>
          completar.mutate(true, { onSuccess: () => setConfirmandoCierre(false) })
        }
      />
    </div>
  );
}

function EstadoGuardado({
  sucio,
  guardando,
  actualizadoEn,
  actualizadoPor,
}: {
  sucio: boolean;
  guardando: boolean;
  actualizadoEn: string | null;
  actualizadoPor: string | null;
}) {
  if (guardando) {
    return (
      <span className="flex items-center gap-1.5 text-sm text-slate-500">
        <Spinner />
        Guardando…
      </span>
    );
  }

  if (sucio) return <span className="text-sm text-slate-500">Sin guardar…</span>;

  if (!actualizadoEn) return <span className="text-sm text-slate-400">Sin cambios</span>;

  return (
    <span className="text-sm text-slate-500">
      Guardado {format(parseISO(actualizadoEn), "HH:mm", { locale: es })}
      {actualizadoPor && ` por ${actualizadoPor}`}
    </span>
  );
}

/**
 * Cuándo se escribió una entrada del historial. Una tanda de un solo guardado es un instante;
 * una de veinte es un rato, y mostrarlo como rango explica por qué reúne tantos cambios.
 */
function cuando(desde: string, hasta: string) {
  const inicio = parseISO(desde);
  const fin = parseISO(hasta);
  const etiqueta = format(inicio, "d MMM, HH:mm", { locale: es });

  // Por debajo del minuto el rango se leería "14:32–14:32", que no dice nada.
  if (fin.getTime() - inicio.getTime() < 60_000) return etiqueta;

  // Una tanda que cruza la medianoche necesita repetir el día o se lee al revés.
  return isSameDay(inicio, fin)
    ? `${etiqueta}–${format(fin, "HH:mm", { locale: es })}`
    : `${etiqueta} – ${format(fin, "d MMM, HH:mm", { locale: es })}`;
}

/**
 * Una entrada del historial: todo lo que una persona escribió de corrido, no cada guardado
 * automático. Se despliega para mostrar qué texto entró y cuál salió, porque el listado de
 * "fulano guardó a las 14:32" no alcanza para saber quién escribió qué.
 */
function VersionFila({
  version,
  proyectoId,
  pasoId,
}: {
  version: VersionDocumento;
  proyectoId: number;
  pasoId: number;
}) {
  const [abierta, setAbierta] = useState(false);
  const diff = useDiffVersion(proyectoId, pasoId, abierta ? version.id : null);

  return (
    <li className="overflow-hidden rounded-xl border border-slate-200 bg-white text-sm">
      <button
        type="button"
        onClick={() => setAbierta((valor) => !valor)}
        aria-expanded={abierta}
        className="flex w-full flex-wrap items-center justify-between gap-2 px-4 py-2.5 text-left hover:bg-slate-50"
      >
        <span className="font-medium text-slate-800">{version.autor}</span>

        <span className="flex items-center gap-3 text-slate-500">
          {version.guardados > 1 && (
            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs tabular-nums text-slate-600">
              {version.guardados} guardados
            </span>
          )}
          <span className="tabular-nums">
            <span className="text-green-700">+{version.palabrasAgregadas}</span>{" "}
            <span className="text-red-700">−{version.palabrasQuitadas}</span>
          </span>
          <span>{cuando(version.creadoEn, version.actualizadoEn)}</span>
          <span aria-hidden className="text-slate-400">
            {abierta ? "−" : "+"}
          </span>
        </span>
      </button>

      {abierta && (
        <div className="border-t border-slate-200 px-4 py-3">
          {diff.isPending && (
            <div className="flex items-center gap-2 text-slate-500">
              <Spinner />
              <span>Calculando los cambios…</span>
            </div>
          )}

          {diff.isError && (
            <p role="alert" className="text-red-700">
              {diff.error.message}
            </p>
          )}

          {diff.isSuccess &&
            (diff.data.segmentos.length === 0 ? (
              <p className="text-slate-500">Este cambio no modificó el texto.</p>
            ) : (
              <p className="leading-relaxed whitespace-pre-wrap">
                {diff.data.segmentos.map((segmento, indice) => (
                  <span
                    // Los segmentos no tienen id y su contenido puede repetirse; el índice es
                    // estable porque la lista no se reordena ni crece.
                    key={indice}
                    className={
                      segmento.tipo === "AGREGADO"
                        ? "rounded bg-green-100 text-green-900"
                        : segmento.tipo === "QUITADO"
                          ? "rounded bg-red-100 text-red-900 line-through"
                          : "text-slate-500"
                    }
                  >
                    {segmento.texto}
                  </span>
                ))}
              </p>
            ))}
        </div>
      )}
    </li>
  );
}

/** Explicación y ejemplo del paso. Es catálogo: igual para todos los proyectos. */
function Consigna({ explicacion, ejemplo }: { explicacion: string; ejemplo: string }) {
  // Arranca cerrada: quien entra al paso viene a escribir, y la consigna se lee una vez.
  const [abierta, setAbierta] = useState(false);

  if (!explicacion && !ejemplo) {
    return (
      <p className="rounded-xl border border-dashed border-slate-300 px-4 py-3 text-sm text-slate-500">
        Este paso todavía no tiene cargada su explicación ni su ejemplo.
      </p>
    );
  }

  return (
    <section className="overflow-hidden rounded-xl border border-brand-200 bg-brand-50/50">
      <button
        type="button"
        onClick={() => setAbierta((valor) => !valor)}
        className="flex w-full items-center justify-between px-4 py-3 text-left"
      >
        <span className="text-sm font-medium text-brand-900">Cómo completar este paso</span>
        <span aria-hidden className="text-brand-700">
          {abierta ? "−" : "+"}
        </span>
      </button>

      {abierta && (
        <div className="flex flex-col gap-4 border-t border-brand-200 px-4 py-4 text-sm">
          {explicacion && (
            <div>
              <h3 className="font-medium text-brand-900">Explicación</h3>
              <p className="mt-1 whitespace-pre-wrap text-slate-700">{explicacion}</p>
            </div>
          )}

          {ejemplo && (
            <div>
              <h3 className="font-medium text-brand-900">Ejemplo</h3>
              <p className="mt-1 whitespace-pre-wrap text-slate-700">{ejemplo}</p>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
