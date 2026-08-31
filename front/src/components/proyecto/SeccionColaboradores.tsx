import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import { Spinner } from "@/components/ui/Spinner";
import { useConfiguracion } from "@/features/parametros/hooks";
import type { Fase } from "@/features/pipoe/types";
import {
  useActualizarPermisos,
  useColaboradores,
  useCrearColaborador,
  useEliminarColaborador,
} from "@/features/proyectos/hooks";
import { useAuthStore } from "@/stores/auth";
import {
  nuevoColaboradorSchema,
  type NuevoColaboradorValues,
} from "@/features/proyectos/schemas";
import {
  NIVELES,
  type Colaborador,
  type NivelPermiso,
  type Permiso,
} from "@/features/proyectos/types";

/** Etiqueta legible de un nivel, para los diálogos de confirmación. */
function etiquetaNivel(nivel: NivelPermiso) {
  return NIVELES.find((opcion) => opcion.valor === nivel)?.etiqueta ?? nivel;
}

/**
 * Los permisos en una línea, para poder leerlos con el desplegable cerrado. Si son todos
 * iguales lo dice de una; si no, cuenta en cuántas fases puede editar.
 */
function resumenPermisos(permisos: Permiso[]) {
  const primero = permisos[0]?.nivel;
  if (primero && permisos.every((permiso) => permiso.nivel === primero))
    return `· ${etiquetaNivel(primero)} en las ${permisos.length} fases`;

  const conEdicion = permisos.filter((permiso) => permiso.nivel === "EDICION").length;
  return conEdicion === 0
    ? "· sin edición en ninguna fase"
    : `· edición en ${conEdicion} de ${permisos.length} fases`;
}

/**
 * Quiénes trabajan en el proyecto y qué puede hacer cada uno en cada fase.
 *
 * Vive dentro de la configuración del proyecto: gestionar gente es administrar el proyecto,
 * no trabajar en él.
 */
export function SeccionColaboradores({ proyectoId }: { proyectoId: number }) {
  const colaboradores = useColaboradores(proyectoId, true);
  const configuracion = useConfiguracion();

  const [invitando, setInvitando] = useState(false);

  const usados = colaboradores.data?.length ?? 0;
  const maximo = configuracion.data?.maxColaboradoresPorProyecto;
  const enElMaximo = maximo !== undefined && usados >= maximo;

  return (
    <section className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Colaboradores</h2>
          <p className="mt-0.5 text-sm text-slate-500">
            Cada colaborador tiene un permiso propio en cada una de las 5 fases.
          </p>
        </div>

        {/* Al llegar al cupo el botón desaparece y listo: el máximo es implícito, no algo
            que haya que andar informando. */}
        {!enElMaximo && (
          <Button onClick={() => setInvitando(true)}>Agregar colaborador</Button>
        )}
      </div>

      {colaboradores.isPending && (
        <div className="flex items-center gap-2 text-slate-500">
          <Spinner />
          <span>Cargando colaboradores…</span>
        </div>
      )}

      {colaboradores.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {colaboradores.error.message}
        </p>
      )}

      {colaboradores.isSuccess &&
        (colaboradores.data.length === 0 ? (
          <p className="text-slate-600">
            Este proyecto todavía no tiene colaboradores. Agrega el primero con el botón de
            arriba.
          </p>
        ) : (
          <ul className="flex flex-col gap-4">
            {colaboradores.data.map((colaborador) => (
              <ColaboradorCard
                key={colaborador.id}
                colaborador={colaborador}
                proyectoId={proyectoId}
              />
            ))}
          </ul>
        ))}

      <InvitarColaboradorModal
        open={invitando}
        proyectoId={proyectoId}
        onClose={() => setInvitando(false)}
      />
    </section>
  );
}

function ColaboradorCard({
  colaborador,
  proyectoId,
}: {
  colaborador: Colaborador;
  proyectoId: number;
}) {
  const actualizar = useActualizarPermisos(proyectoId);
  const eliminar = useEliminarColaborador(proyectoId);

  // Los permisos se editan en local y se mandan de una: así un cambio a medio hacer no
  // dispara una request por cada select, y se puede descartar sin consecuencias.
  const [permisos, setPermisos] = useState<Permiso[]>(colaborador.permisos);
  const [confirmando, setConfirmando] = useState<"permisos" | "baja" | null>(null);
  // Cerrado por defecto: con cinco selects por persona, la lista se vuelve imposible de
  // recorrer en cuanto hay más de dos o tres colaboradores.
  const [abierto, setAbierto] = useState(false);

  const nivelGuardado = (fase: Fase) =>
    colaborador.permisos.find((permiso) => permiso.fase === fase)?.nivel;

  const cambios = permisos.filter((permiso) => permiso.nivel !== nivelGuardado(permiso.fase));

  const cambiarNivel = (fase: Fase, nivel: NivelPermiso) =>
    setPermisos((actuales) =>
      actuales.map((permiso) => (permiso.fase === fase ? { ...permiso, nivel } : permiso)),
    );

  const cerrarDialogo = () => {
    actualizar.reset();
    eliminar.reset();
    setConfirmando(null);
  };

  return (
    <li className="rounded-xl border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium text-slate-900">{colaborador.nombre}</p>
          <p className="text-sm text-slate-500">{colaborador.email}</p>
        </div>

        <Button
          variant="ghost"
          className="text-red-600 hover:bg-red-50"
          onClick={() => setConfirmando("baja")}
        >
          Quitar
        </Button>
      </div>

      <div className="mt-4 border-t border-slate-100 pt-3">
        <button
          type="button"
          onClick={() => setAbierto((valor) => !valor)}
          aria-expanded={abierto}
          className="flex w-full items-center justify-between gap-3 rounded-lg py-1 text-left text-sm font-medium text-slate-700 hover:text-slate-900"
        >
          <span className="flex items-center gap-2">
            Permisos por fase
            {/* El resumen evita tener que desplegar sólo para saber si hay algo distinto. */}
            <span className="font-normal text-slate-500">{resumenPermisos(permisos)}</span>
          </span>

          <span className="flex items-center gap-2">
            {cambios.length > 0 && (
              <span className="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                {cambios.length} sin guardar
              </span>
            )}
            <span aria-hidden className="text-slate-400">
              {abierto ? "−" : "+"}
            </span>
          </span>
        </button>

        {abierto && (
          <>
            <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {permisos.map((permiso) => (
                <label key={permiso.fase} className="flex flex-col gap-1.5">
                  <span className="text-xs font-medium text-slate-500">
                    {permiso.faseNombre}
                  </span>
                  <Select
                    value={permiso.nivel}
                    onChange={(event) =>
                      cambiarNivel(permiso.fase, event.target.value as NivelPermiso)
                    }
                  >
                    {NIVELES.map((nivel) => (
                      <option key={nivel.valor} value={nivel.valor}>
                        {nivel.etiqueta}
                      </option>
                    ))}
                  </Select>
                </label>
              ))}
            </div>

            {cambios.length > 0 && (
              <div className="mt-4 flex flex-wrap items-center gap-2">
                <Button onClick={() => setConfirmando("permisos")}>Guardar permisos</Button>
                <Button variant="ghost" onClick={() => setPermisos(colaborador.permisos)}>
                  Descartar
                </Button>
              </div>
            )}
          </>
        )}
      </div>

      <ConfirmDialog
        open={confirmando === "permisos"}
        title={`Cambiar permisos de ${colaborador.nombre}`}
        confirmLabel="Guardar permisos"
        loading={actualizar.isPending}
        error={actualizar.error?.message}
        description="Los cambios se aplican en cuanto confirmes."
        onCancel={cerrarDialogo}
        onConfirm={() =>
          actualizar.mutate(
            { colaboradorId: colaborador.id, permisos },
            { onSuccess: cerrarDialogo },
          )
        }
      >
        <ul className="mb-5 flex flex-col gap-1.5 rounded-lg bg-slate-50 p-3 text-sm">
          {cambios.map((permiso) => (
            <li key={permiso.fase} className="flex justify-between gap-3">
              <span className="text-slate-500">{permiso.faseNombre}</span>
              <span className="text-slate-800">
                {etiquetaNivel(nivelGuardado(permiso.fase) ?? permiso.nivel)} →{" "}
                <strong className="font-medium">{etiquetaNivel(permiso.nivel)}</strong>
              </span>
            </li>
          ))}
        </ul>
      </ConfirmDialog>

      <ConfirmDialog
        open={confirmando === "baja"}
        variant="danger"
        title={`Quitar a ${colaborador.nombre}`}
        confirmLabel="Quitar del proyecto"
        loading={eliminar.isPending}
        error={eliminar.error?.message}
        description={
          <>
            Pierde el acceso al proyecto de inmediato, incluso si tiene la sesión abierta.
            Puedes volver a invitarlo más adelante, pero recibiría una contraseña nueva y sus
            permisos arrancarían de cero.
          </>
        }
        onCancel={cerrarDialogo}
        onConfirm={() => eliminar.mutate(colaborador.id, { onSuccess: cerrarDialogo })}
      />
    </li>
  );
}

type InvitarModalProps = {
  open: boolean;
  proyectoId: number;
  onClose: () => void;
};

/**
 * Alta en dos pasos: datos y confirmación. La confirmación no es decorativa — invitar manda
 * un mail con una contraseña, así que conviene releer la dirección antes de disparar.
 */
function InvitarColaboradorModal({ open, proyectoId, onClose }: InvitarModalProps) {
  const crear = useCrearColaborador(proyectoId);
  const [confirmando, setConfirmando] = useState<NuevoColaboradorValues | null>(null);
  // Sólo el dueño ve este formulario, así que la sesión es la suya.
  const propioEmail = useAuthStore((state) => state.sesion?.email);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<NuevoColaboradorValues>({
    resolver: zodResolver(nuevoColaboradorSchema),
    defaultValues: { nombre: "", email: "" },
  });

  /**
   * Invitarse a uno mismo crea una segunda identidad sobre el propio proyecto, con otra
   * contraseña y posiblemente con menos permisos de los que ya se tienen. El backend lo
   * rechaza igual; acá se corta antes para no pasar por la pantalla de confirmación.
   */
  const revisar = (values: NuevoColaboradorValues) => {
    if (propioEmail && values.email.trim().toLowerCase() === propioEmail.toLowerCase()) {
      setError("email", {
        message: "Es tu propia dirección: ya tienes edición en las cinco fases",
      });
      return;
    }

    setConfirmando(values);
  };

  const cerrar = () => {
    setConfirmando(null);
    crear.reset();
    reset();
    onClose();
  };

  return (
    <Modal
      open={open}
      title="Agregar colaborador"
      description={
        confirmando
          ? undefined
          : "Arranca con sólo lectura en las 5 fases. Después puedes cambiarle los permisos."
      }
      onClose={cerrar}
      dismissable={!crear.isPending}
    >
      {confirmando ? (
        <div className="flex flex-col gap-5">
          <p className="text-sm text-slate-700">
            Se le va a enviar la contraseña de acceso a{" "}
            <strong className="font-medium text-slate-900">{confirmando.email}</strong>.
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
              Invitar
            </Button>
          </div>
        </div>
      ) : (
        <form
          noValidate
          onSubmit={handleSubmit(revisar)}
          className="flex flex-col gap-4"
        >
          <Field label="Nombre" htmlFor="colaboradorNombre" error={errors.nombre?.message}>
            <Input
              id="colaboradorNombre"
              invalid={Boolean(errors.nombre)}
              {...register("nombre")}
            />
          </Field>

          <Field label="Email" htmlFor="colaboradorEmail" error={errors.email?.message}>
            <Input
              id="colaboradorEmail"
              type="email"
              invalid={Boolean(errors.email)}
              {...register("email")}
            />
          </Field>

          <div className="mt-1 flex justify-end gap-2">
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
