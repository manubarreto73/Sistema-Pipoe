import type { ReactNode } from "react";

type FieldProps = {
  label: string;
  /** Debe coincidir con el `id` del input que envuelve. */
  htmlFor: string;
  /** Aclaración breve bajo la etiqueta, para lo que no se explica solo (formato, de dónde sale). */
  ayuda?: string;
  error?: string;
  children: ReactNode;
};

export function Field({ label, htmlFor, ayuda, error, children }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-sm font-medium text-slate-700">
        {label}
      </label>

      {/* Antes del input y enlazada por aria-describedby: un lector de pantalla la anuncia al
          enfocar el campo, no después de haberlo completado. */}
      {ayuda && (
        <p id={`${htmlFor}-ayuda`} className="text-xs text-slate-500">
          {ayuda}
        </p>
      )}

      {children}

      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
