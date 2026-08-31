import { zodResolver } from "@hookform/resolvers/zod";
import { format, parseISO } from "date-fns";
import { es } from "date-fns/locale";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import { Spinner } from "@/components/ui/Spinner";
import { FILTROS_USUARIOS_VACIOS, type FiltrosUsuarios } from "@/features/usuarios/api";
import { useCambiarActivo, useCrearAdmin, useUsuarios } from "@/features/usuarios/hooks";
import { nuevoAdminSchema, type NuevoAdminValues } from "@/features/usuarios/schemas";
import type { UsuarioAdmin } from "@/features/usuarios/types";
import { useAuthStore } from "@/stores/auth";
import { cn } from "@/lib/cn";
import { useDebounce } from "@/lib/useDebounce";

export default function AdminUsuarios() {
  const [filtros, setFiltros] = useState<FiltrosUsuarios>(FILTROS_USUARIOS_VACIOS);
  const [page, setPage] = useState(0);
  const [creando, setCreando] = useState(false);
  const [cambiando, setCambiando] = useState<UsuarioAdmin | null>(null);

  // El texto se escribe letra por letra; la consulta espera a que la persona frene.
  const textoDiferido = useDebounce(filtros.texto);
  const usuarios = useUsuarios({ ...filtros, texto: textoDiferido }, page);
  const cambiarEstado = useCambiarActivo();

  /** Cualquier cambio de filtro vuelve a la primera página: la 4 podría ya no existir. */
  const cambiar = (parcial: Partial<FiltrosUsuarios>) => {
    setFiltros((actuales) => ({ ...actuales, ...parcial }));
    setPage(0);
  };

  const filtrado = filtros.texto !== "" || filtros.activo !== null || filtros.role !== null;

  return (
    <div className="flex flex-col gap-6">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">Usuarios</h1>
          <p className="mt-2 text-sm text-slate-600">
            Todas las cuentas del sistema. Dar de baja a alguien le impide entrar, pero no borra sus
            proyectos ni lo que escribió.
          </p>
        </div>

        <Button onClick={() => setCreando(true)}>Nuevo administrador</Button>
      </header>

      <div className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <Input
            type="search"
            placeholder="Buscar por nombre o correo…"
            aria-label="Buscar usuarios"
            className="sm:flex-1"
            value={filtros.texto}
            onChange={(event) => cambiar({ texto: event.target.value })}
          />

          <Select
            aria-label="Filtrar por estado"
            value={filtros.activo === null ? "" : String(filtros.activo)}
            onChange={(event) =>
              cambiar({
                activo: event.target.value === "" ? null : event.target.value === "true",
              })
            }
          >
            <option value="">Todos los estados</option>
            <option value="true">Activos</option>
            <option value="false">Dados de baja</option>
          </Select>

          <Select
            aria-label="Filtrar por rol"
            value={filtros.role ?? ""}
            onChange={(event) =>
              cambiar({
                role: event.target.value === "" ? null : (event.target.value as "ADMIN" | "USER"),
              })
            }
          >
            <option value="">Todos los roles</option>
            <option value="USER">Usuarios</option>
            <option value="ADMIN">Administración</option>
          </Select>

          {filtrado && (
            <Button
              variant="ghost"
              onClick={() => {
                setFiltros(FILTROS_USUARIOS_VACIOS);
                setPage(0);
              }}
            >
              Limpiar
            </Button>
          )}
        </div>

        {usuarios.isSuccess && (
          <p className="text-sm text-slate-500">
            {usuarios.data.totalElements === 0
              ? "Ninguna cuenta coincide"
              : `${usuarios.data.totalElements} ${
                  usuarios.data.totalElements === 1 ? "cuenta" : "cuentas"
                }`}
          </p>
        )}
      </div>

      {usuarios.isPending && (
        <div className="flex items-center gap-2 text-slate-500">
          <Spinner />
          <span>Cargando usuarios…</span>
        </div>
      )}

      {usuarios.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {usuarios.error.message}
        </p>
      )}

      {cambiarEstado.isError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {cambiarEstado.error.message}
        </p>
      )}

      {usuarios.isSuccess && !usuarios.data.empty && (
        <>
          <ul className="flex flex-col gap-3">
            {usuarios.data.content.map((usuario) => (
              <UsuarioFila
                key={usuario.id}
                usuario={usuario}
                onCambiarEstado={() => setCambiando(usuario)}
              />
            ))}
          </ul>

          <div className="flex items-center justify-between border-t border-slate-200 pt-4">
            <span className="text-sm text-slate-500">
              Página {usuarios.data.number + 1} de {usuarios.data.totalPages} ·{" "}
              {usuarios.data.totalElements} en total
            </span>

            <div className="flex gap-2">
              <Button
                variant="secondary"
                disabled={usuarios.data.first}
                onClick={() => setPage((actual) => Math.max(0, actual - 1))}
              >
                Anterior
              </Button>
              <Button
                variant="secondary"
                disabled={usuarios.data.last}
                onClick={() => setPage((actual) => actual + 1)}
              >
                Siguiente
              </Button>
            </div>
          </div>
        </>
      )}

      <ModalNuevoAdmin abierto={creando} onCerrar={() => setCreando(false)} />

      <ConfirmDialog
        open={cambiando !== null}
        title={cambiando?.activo ? "Dar de baja la cuenta" : "Reactivar la cuenta"}
        description={
          cambiando?.activo
            ? `${cambiando.nombreCompleto} no va a poder entrar. Sus proyectos, documentos y comentarios quedan como están, y puedes reactivar la cuenta cuando quieras.`
            : `${cambiando?.nombreCompleto} va a poder entrar de nuevo con su contraseña de siempre.`
        }
        confirmLabel={cambiando?.activo ? "Dar de baja" : "Reactivar"}
        cancelLabel="Cancelar"
        loading={cambiarEstado.isPending}
        onCancel={() => setCambiando(null)}
        onConfirm={() => {
          if (cambiando)
            cambiarEstado.mutate(
              { id: cambiando.id, activo: !cambiando.activo },
              { onSuccess: () => setCambiando(null) },
            );
        }}
      />
    </div>
  );
}

function UsuarioFila({
  usuario,
  onCambiarEstado,
}: {
  usuario: UsuarioAdmin;
  onCambiarEstado: () => void;
}) {
  // Nadie puede darse de baja a sí mismo: la API lo rechaza, así que acá ni se ofrece.
  const propia = useAuthStore((state) => state.sesion?.id === usuario.id);

  return (
    <li
      className={cn(
        "flex flex-wrap items-center justify-between gap-x-4 gap-y-3 rounded-xl border bg-white px-4 py-3",
        usuario.activo ? "border-slate-200" : "border-slate-200 bg-slate-50",
      )}
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className={cn("font-medium", usuario.activo ? "text-slate-900" : "text-slate-500")}>
            {usuario.nombreCompleto}
          </h2>

          {usuario.role === "ADMIN" && (
            <span className="bg-brand-100 text-brand-700 rounded-full px-2.5 py-0.5 text-xs font-medium">
              Administración
            </span>
          )}

          {!usuario.activo && (
            <span className="rounded-full bg-slate-200 px-2.5 py-0.5 text-xs font-medium text-slate-600">
              Dada de baja
            </span>
          )}
        </div>

        <p className="mt-0.5 truncate text-sm text-slate-500">{usuario.email}</p>

        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-slate-500">
          <span>
            {usuario.proyectos === 0
              ? "sin proyectos"
              : usuario.proyectos === 1
                ? "1 proyecto"
                : `${usuario.proyectos} proyectos`}
          </span>
          <span className="text-slate-300" aria-hidden>
            ·
          </span>
          <span>
            {usuario.ultimoAcceso
              ? `última entrada el ${format(parseISO(usuario.ultimoAcceso), "d 'de' MMMM 'de' yyyy, HH:mm", { locale: es })}`
              : "nunca entró"}
          </span>
        </div>
      </div>

      {!propia && (
        <Button variant="secondary" onClick={onCambiarEstado}>
          {usuario.activo ? "Dar de baja" : "Reactivar"}
        </Button>
      )}
    </li>
  );
}

/**
 * Alta de otra cuenta de administración.
 *
 * Pide el nombre además del correo porque es obligatorio en la base y porque es con lo que se
 * saluda a la persona en el mail donde viaja su contraseña; después puede cambiarlo desde su
 * perfil. La contraseña no se elige acá ni se muestra: la genera el servidor y la manda por
 * correo, igual que en cualquier otra alta del sistema.
 */
function ModalNuevoAdmin({ abierto, onCerrar }: { abierto: boolean; onCerrar: () => void }) {
  const crear = useCrearAdmin();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NuevoAdminValues>({
    resolver: zodResolver(nuevoAdminSchema),
    defaultValues: { email: "", nombreCompleto: "" },
  });

  const cerrar = () => {
    reset();
    crear.reset();
    onCerrar();
  };

  return (
    <Modal
      open={abierto}
      onClose={cerrar}
      dismissable={!crear.isPending}
      title="Nuevo administrador"
    >
      <form
        noValidate
        onSubmit={handleSubmit((values) => crear.mutate(values, { onSuccess: cerrar }))}
        className="flex flex-col gap-4"
      >
        <p className="text-sm text-slate-600">
          Va a poder entrar a todos los proyectos para comentar, aprobar solicitudes y administrar
          cuentas. Recibe su contraseña por correo.
        </p>

        <Field label="Nombre" htmlFor="nombreCompleto" error={errors.nombreCompleto?.message}>
          <Input
            id="nombreCompleto"
            autoComplete="name"
            invalid={Boolean(errors.nombreCompleto)}
            {...register("nombreCompleto")}
          />
        </Field>

        <Field label="Correo electrónico" htmlFor="email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            invalid={Boolean(errors.email)}
            {...register("email")}
          />
        </Field>

        {crear.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {crear.error.message}
          </p>
        )}

        <div className="mt-1 flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={cerrar}>
            Cancelar
          </Button>
          <Button type="submit" loading={crear.isPending}>
            Crear cuenta
          </Button>
        </div>
      </form>
    </Modal>
  );
}
