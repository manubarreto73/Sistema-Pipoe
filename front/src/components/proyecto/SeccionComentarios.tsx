import { zodResolver } from "@hookform/resolvers/zod";
import { format, parseISO } from "date-fns";
import { es } from "date-fns/locale";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Spinner } from "@/components/ui/Spinner";
import { Textarea } from "@/components/ui/Textarea";
import {
  useComentarios,
  useCrearComentario,
  useEliminarComentario,
} from "@/features/comentarios/hooks";
import {
  comentarioSchema,
  type ComentarioValues,
} from "@/features/comentarios/schemas";
import type { Comentario } from "@/features/comentarios/types";
import type { NivelPermiso } from "@/features/proyectos/types";

type Props = {
  proyectoId: number;
  pasoId: number;
  /** El de la fase a la que pertenece el paso. LECTURA lee pero no escribe. */
  nivel: NivelPermiso;
};

/**
 * Los comentarios de un paso, sea un paso del despliegue o el producto de la fase.
 *
 * Van sobre el documento entero y no sobre una parte del texto: es lo que hace que sigan
 * teniendo sentido después de que alguien reescriba el documento.
 */
export function SeccionComentarios({ proyectoId, pasoId, nivel }: Props) {
  const comentarios = useComentarios(proyectoId, pasoId);
  const crear = useCrearComentario(proyectoId, pasoId);
  const eliminar = useEliminarComentario(proyectoId, pasoId);

  const [borrando, setBorrando] = useState<Comentario | null>(null);
  const puedeComentar = nivel !== "LECTURA";

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ComentarioValues>({
    resolver: zodResolver(comentarioSchema),
    defaultValues: { texto: "" },
  });

  const enviar = handleSubmit((values) =>
    // El reset va en el onSuccess y no después del mutate: si la API rechaza el comentario,
    // el texto tiene que seguir en el campo para no obligar a reescribirlo.
    crear.mutate(values.texto, { onSuccess: () => reset() }),
  );

  const total = comentarios.data?.length ?? 0;

  return (
    <section className="border-t border-slate-200 pt-6">
      <h2 className="font-serif text-lg font-semibold text-slate-900">
        Comentarios{total > 0 && <span className="text-slate-400"> ({total})</span>}
      </h2>

      {puedeComentar ? (
        <form noValidate onSubmit={enviar} className="mt-3 flex flex-col gap-2">
          <Textarea
            id="comentario"
            rows={3}
            maxLength={2000}
            placeholder="Deja una observación sobre este documento…"
            aria-label="Nuevo comentario"
            invalid={Boolean(errors.texto)}
            {...register("texto")}
          />

          {errors.texto && (
            <p role="alert" className="text-sm text-red-600">
              {errors.texto.message}
            </p>
          )}

          {crear.isError && (
            <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
              {crear.error.message}
            </p>
          )}

          <div className="flex justify-end">
            <Button type="submit" loading={crear.isPending}>
              Comentar
            </Button>
          </div>
        </form>
      ) : (
        <p className="mt-2 text-sm text-slate-500">
          Puedes leer los comentarios, pero no dejar los tuyos.
        </p>
      )}

      <div className="mt-4">
        {comentarios.isPending && (
          <div className="flex items-center gap-2 text-sm text-slate-500">
            <Spinner />
            <span>Cargando comentarios…</span>
          </div>
        )}

        {comentarios.isError && (
          <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {comentarios.error.message}
          </p>
        )}

        {comentarios.isSuccess &&
          (total === 0 ? (
            <p className="text-sm text-slate-500">Todavía no hay comentarios en este paso.</p>
          ) : (
            <ul className="flex flex-col gap-3">
              {comentarios.data.map((comentario) => (
                <ComentarioFila
                  key={comentario.id}
                  comentario={comentario}
                  onBorrar={() => setBorrando(comentario)}
                />
              ))}
            </ul>
          ))}

        {eliminar.isError && (
          <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
            {eliminar.error.message}
          </p>
        )}
      </div>

      <ConfirmDialog
        open={borrando !== null}
        title="Borrar el comentario"
        description="Se borra para todo el mundo y no se puede deshacer."
        confirmLabel="Borrar"
        cancelLabel="Dejarlo"
        loading={eliminar.isPending}
        onCancel={() => setBorrando(null)}
        onConfirm={() => {
          if (borrando) eliminar.mutate(borrando.id, { onSuccess: () => setBorrando(null) });
        }}
      />
    </section>
  );
}

function ComentarioFila({
  comentario,
  onBorrar,
}: {
  comentario: Comentario;
  onBorrar: () => void;
}) {
  return (
    <li className="rounded-xl border border-slate-200 bg-white px-4 py-3">
      <div className="flex items-baseline justify-between gap-3">
        <p className="text-sm font-medium text-slate-800">{comentario.autor}</p>

        <div className="flex items-center gap-3">
          <time
            dateTime={comentario.creadoEn}
            className="shrink-0 text-xs text-slate-500"
            title={format(parseISO(comentario.creadoEn), "d 'de' MMMM 'de' yyyy, HH:mm", {
              locale: es,
            })}
          >
            {format(parseISO(comentario.creadoEn), "d MMM, HH:mm", { locale: es })}
          </time>

          {comentario.puedeBorrar && (
            <button
              type="button"
              onClick={onBorrar}
              className="text-xs text-slate-500 hover:text-red-700 hover:underline"
            >
              Borrar
            </button>
          )}
        </div>
      </div>

      {/* whitespace-pre-line: el comentario es texto plano, y los saltos de línea que escribió
          la persona son parte de lo que quiso decir. */}
      <p className="mt-1 text-sm whitespace-pre-line text-slate-700">{comentario.texto}</p>
    </li>
  );
}
