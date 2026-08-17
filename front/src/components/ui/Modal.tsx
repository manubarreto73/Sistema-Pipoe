import { useEffect, useId, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";

import { cn } from "@/lib/cn";

type ModalProps = {
  open: boolean;
  title: string;
  /** Texto opcional bajo el título. Describe la consecuencia de la acción. */
  description?: ReactNode;
  onClose: () => void;
  /** `false` mientras hay una request en vuelo: evita cerrar a mitad de camino. */
  dismissable?: boolean;
  className?: string;
  children: ReactNode;
};

/**
 * Diálogo modal de la app.
 *
 * Va por un portal a `document.body` para que ningún `overflow` u `overlay` de una card lo
 * recorte, y devuelve el foco al elemento que lo abrió al cerrarse — si no, después de
 * confirmar algo el foco queda en el `body` y la navegación por teclado arranca de cero.
 */
export function Modal({
  open,
  title,
  description,
  onClose,
  dismissable = true,
  className,
  children,
}: ModalProps) {
  const titleId = useId();
  const panelRef = useRef<HTMLDivElement>(null);
  const origenRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    if (!open) return;

    origenRef.current = document.activeElement as HTMLElement | null;

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape" && dismissable) onClose();
    };

    document.addEventListener("keydown", onKeyDown);

    // El scroll del fondo con un modal abierto es desorientador en pantallas chicas.
    const overflowPrevio = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    // Primer foco adentro del panel: el modal tiene que ser operable sin mouse.
    const enfocable = panelRef.current?.querySelector<HTMLElement>(
      "input, select, textarea, button",
    );
    enfocable?.focus();

    return () => {
      document.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = overflowPrevio;
      origenRef.current?.focus?.();
    };
  }, [open, dismissable, onClose]);

  if (!open) return null;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center overflow-y-auto bg-slate-900/40 p-4 backdrop-blur-[1px] sm:items-center">
      {/* Click en el fondo = cancelar, salvo que haya algo en vuelo. */}
      <div
        aria-hidden
        className="absolute inset-0"
        onClick={() => dismissable && onClose()}
      />

      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={cn(
          "relative w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-xl",
          className,
        )}
      >
        <h2 id={titleId} className="text-lg font-semibold text-slate-900">
          {title}
        </h2>

        {description && <div className="mt-2 text-sm text-slate-600">{description}</div>}

        <div className="mt-5">{children}</div>
      </div>
    </div>,
    document.body,
  );
}
