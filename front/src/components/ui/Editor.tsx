import { EditorContent, useEditor, type Editor as TipTapEditor } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import { useEffect } from "react";

import { cn } from "@/lib/cn";

type EditorProps = {
  contenido: string;
  editable: boolean;
  onChange: (html: string) => void;
  /** Cambia cuando el documento se recarga del servidor: fuerza a repoblar el editor. */
  recargaId?: string | number;
};

const BOTONES = [
  { nombre: "negrita", etiqueta: "N", clase: "font-bold", marca: "bold" },
  { nombre: "cursiva", etiqueta: "I", clase: "italic", marca: "italic" },
] as const;

/**
 * Editor de texto enriquecido de la app.
 *
 * Guarda HTML, que es lo que TipTap produce y consume sin transformaciones. El día que
 * sumemos edición en tiempo real, este mismo editor acepta Yjs sin cambiar de librería.
 */
export function Editor({ contenido, editable, onChange, recargaId }: EditorProps) {
  const editor = useEditor({
    extensions: [StarterKit],
    content: contenido,
    editable,
    editorProps: {
      attributes: {
        class:
          "prose-editor min-h-96 w-full rounded-b-xl bg-white px-5 py-4 text-slate-800 focus:outline-none",
      },
    },
    onUpdate: ({ editor: instancia }) => onChange(instancia.getHTML()),
  });

  // El editor se crea una sola vez; cuando el documento se recarga del servidor (por un 409
  // resuelto, por ejemplo) hay que repoblarlo a mano.
  useEffect(() => {
    if (!editor || recargaId === undefined) return;
    if (editor.getHTML() === contenido) return;

    editor.commands.setContent(contenido, { emitUpdate: false });
    // Sólo ante una recarga explícita: incluir `contenido` acá pisaría lo que se está tipeando.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editor, recargaId]);

  useEffect(() => {
    editor?.setEditable(editable);
  }, [editor, editable]);

  if (!editor) return null;

  return (
    <div className="overflow-hidden rounded-xl border border-slate-300 bg-white focus-within:border-brand-400">
      {editable && <BarraHerramientas editor={editor} />}
      <EditorContent editor={editor} />
    </div>
  );
}

function BarraHerramientas({ editor }: { editor: TipTapEditor }) {
  const boton =
    "rounded px-2.5 py-1 text-sm text-slate-600 transition-colors hover:bg-slate-200";
  const activo = "bg-slate-900 text-white hover:bg-slate-900";

  return (
    <div className="flex flex-wrap items-center gap-1 border-b border-slate-200 bg-slate-50 px-2 py-1.5">
      {BOTONES.map((item) => (
        <button
          key={item.nombre}
          type="button"
          aria-label={item.nombre}
          aria-pressed={editor.isActive(item.marca)}
          onClick={() =>
            item.marca === "bold"
              ? editor.chain().focus().toggleBold().run()
              : editor.chain().focus().toggleItalic().run()
          }
          className={cn(boton, item.clase, editor.isActive(item.marca) && activo)}
        >
          {item.etiqueta}
        </button>
      ))}

      <span aria-hidden className="mx-1 h-5 w-px bg-slate-300" />

      {([1, 2] as const).map((nivel) => (
        <button
          key={nivel}
          type="button"
          aria-pressed={editor.isActive("heading", { level: nivel })}
          onClick={() => editor.chain().focus().toggleHeading({ level: nivel }).run()}
          className={cn(
            boton,
            editor.isActive("heading", { level: nivel }) && activo,
          )}
        >
          T{nivel}
        </button>
      ))}

      <span aria-hidden className="mx-1 h-5 w-px bg-slate-300" />

      <button
        type="button"
        aria-label="lista"
        aria-pressed={editor.isActive("bulletList")}
        onClick={() => editor.chain().focus().toggleBulletList().run()}
        className={cn(boton, editor.isActive("bulletList") && activo)}
      >
        Lista
      </button>
      <button
        type="button"
        aria-label="lista numerada"
        aria-pressed={editor.isActive("orderedList")}
        onClick={() => editor.chain().focus().toggleOrderedList().run()}
        className={cn(boton, editor.isActive("orderedList") && activo)}
      >
        1. Lista
      </button>
    </div>
  );
}
