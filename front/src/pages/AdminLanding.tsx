import { format, parseISO } from "date-fns";
import { es } from "date-fns/locale";
import { useState } from "react";
import { Link } from "react-router";

import { Button } from "@/components/ui/Button";
import { Editor } from "@/components/ui/Editor";
import { Input } from "@/components/ui/Input";
import { Spinner } from "@/components/ui/Spinner";
import { useActualizarTextoLanding, useTextosLanding } from "@/features/landing/hooks";
import type { TextoLanding } from "@/features/landing/types";

/**
 * Edición de los textos de la portada pública.
 *
 * Los textos son de Arlette y esta pantalla existe para que los mantenga ella: hasta ahora
 * cambiar una coma de la página de inicio pedía tocar el código y publicar de nuevo.
 */
export default function AdminLanding() {
  const textos = useTextosLanding();

  if (textos.isPending) {
    return (
      <div className="flex items-center gap-2 text-slate-500">
        <Spinner />
        <span>Cargando textos…</span>
      </div>
    );
  }

  if (textos.isError) {
    return (
      <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
        {textos.error.message}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-8">
      <header>
        <h1 className="font-serif text-3xl font-bold tracking-tight text-slate-900">
          Textos de la página de inicio
        </h1>
        <p className="mt-1 text-sm text-slate-600">
          Se guardan uno por uno y se ven al instante en{" "}
          <Link to="/" className="text-brand-700 hover:underline">
            la portada
          </Link>
          .
        </p>
      </header>

      <ul className="flex flex-col gap-5">
        {textos.data.map((texto) => (
          <TextoCard key={texto.clave} texto={texto} />
        ))}
      </ul>
    </div>
  );
}

function TextoCard({ texto }: { texto: TextoLanding }) {
  const actualizar = useActualizarTextoLanding();

  const [borrador, setBorrador] = useState(texto.contenido);

  // Al guardar, la caché trae la versión del servidor: el borrador local tiene que seguirla,
  // o el botón quedaría habilitado para siempre. Se ajusta durante el render.
  const [guardado, setGuardado] = useState(texto.contenido);
  if (guardado !== texto.contenido) {
    setGuardado(texto.contenido);
    setBorrador(texto.contenido);
  }

  const sucio = borrador !== texto.contenido;
  const vacio = borrador.trim() === "" || borrador.trim() === "<p></p>";

  return (
    <li className="rounded-xl border border-slate-200 bg-white p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-medium text-slate-900">{texto.etiqueta}</h2>
          <p className="text-sm text-slate-500">{texto.ayuda}</p>
        </div>

        {texto.actualizadoEn && (
          <p className="text-xs text-slate-400">
            Editado el {format(parseISO(texto.actualizadoEn), "d MMM yyyy, HH:mm", { locale: es })}
          </p>
        )}
      </div>

      <div className="mt-4">
        {texto.tipo === "PLANO" ? (
          <Input
            aria-label={texto.etiqueta}
            value={borrador}
            onChange={(event) => setBorrador(event.target.value)}
          />
        ) : (
          <Editor
            contenido={texto.contenido}
            editable
            recargaId={texto.actualizadoEn ?? "inicial"}
            onChange={setBorrador}
          />
        )}
      </div>

      {actualizar.isError && (
        <p role="alert" className="mt-3 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {actualizar.error.message}
        </p>
      )}

      {sucio && (
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <Button
            loading={actualizar.isPending}
            disabled={vacio}
            onClick={() =>
              actualizar.mutate({ clave: texto.clave, contenido: borrador })
            }
          >
            Guardar
          </Button>
          <Button
            variant="ghost"
            disabled={actualizar.isPending}
            onClick={() => setBorrador(texto.contenido)}
          >
            Descartar
          </Button>
          {vacio && (
            <span className="text-sm text-slate-500">
              El texto no puede quedar vacío.
            </span>
          )}
        </div>
      )}
    </li>
  );
}
