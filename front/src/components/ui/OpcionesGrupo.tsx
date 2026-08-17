import type { ReactNode } from "react";

type Opcion<T extends string> = { valor: T; etiqueta: string };

type GrupoProps<T extends string> = {
  /** Título del grupo. Se anuncia como leyenda del fieldset. */
  label: string;
  opciones: Opcion<T>[];
  error?: string;
  /** Texto de ayuda bajo el título ("puede marcar varias opciones"). */
  ayuda?: string;
  /** Contenido extra al final: el campo "especifique" de la opción Otro. */
  children?: ReactNode;
};

type RadioProps<T extends string> = GrupoProps<T> & {
  valor: T | undefined;
  onChange: (valor: T) => void;
};

type CheckboxProps<T extends string> = GrupoProps<T> & {
  valores: T[];
  onChange: (valores: T[]) => void;
};

const CONTENEDOR = "flex flex-col gap-2";
const OPCION =
  "flex cursor-pointer items-start gap-2.5 rounded-lg px-2 py-1.5 text-sm text-slate-700 transition-colors hover:bg-slate-50";
const CONTROL =
  "mt-0.5 size-4 shrink-0 accent-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600";

function Encabezado({ label, ayuda }: { label: string; ayuda?: string }) {
  return (
    <legend className="text-sm font-medium text-slate-700">
      {label}
      {ayuda && <span className="ml-1 font-normal text-slate-500">({ayuda})</span>}
    </legend>
  );
}

/** Una sola opción entre varias. */
export function RadioGrupo<T extends string>({
  label,
  opciones,
  valor,
  onChange,
  error,
  ayuda,
  children,
}: RadioProps<T>) {
  return (
    <fieldset className={CONTENEDOR}>
      <Encabezado label={label} ayuda={ayuda} />

      <div className="flex flex-col">
        {opciones.map((opcion) => (
          <label key={opcion.valor} className={OPCION}>
            <input
              type="radio"
              className={CONTROL}
              checked={valor === opcion.valor}
              onChange={() => onChange(opcion.valor)}
            />
            <span>{opcion.etiqueta}</span>
          </label>
        ))}
      </div>

      {children}

      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
    </fieldset>
  );
}

/** Varias opciones a la vez. */
export function CheckboxGrupo<T extends string>({
  label,
  opciones,
  valores,
  onChange,
  error,
  ayuda,
  children,
}: CheckboxProps<T>) {
  const alternar = (valor: T) =>
    onChange(
      valores.includes(valor)
        ? valores.filter((actual) => actual !== valor)
        : [...valores, valor],
    );

  return (
    <fieldset className={CONTENEDOR}>
      <Encabezado label={label} ayuda={ayuda} />

      <div className="flex flex-col">
        {opciones.map((opcion) => (
          <label key={opcion.valor} className={OPCION}>
            <input
              type="checkbox"
              className={CONTROL}
              checked={valores.includes(opcion.valor)}
              onChange={() => alternar(opcion.valor)}
            />
            <span>{opcion.etiqueta}</span>
          </label>
        ))}
      </div>

      {children}

      {error && (
        <p role="alert" className="text-sm text-red-600">
          {error}
        </p>
      )}
    </fieldset>
  );
}
