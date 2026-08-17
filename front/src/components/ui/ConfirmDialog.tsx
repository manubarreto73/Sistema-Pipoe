import type { ReactNode } from "react";

import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  /** Qué va a pasar si confirma. Conviene ser concreto: nombres, cantidades, si se manda un mail. */
  description?: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  /** `danger` para lo irreversible: borrar un proyecto, quitar a un colaborador. */
  variant?: "primary" | "danger";
  loading?: boolean;
  /** Mensaje de la API si la acción falló. El diálogo queda abierto para poder reintentar. */
  error?: string;
  onConfirm: () => void;
  onCancel: () => void;
  children?: ReactNode;
};

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  variant = "primary",
  loading = false,
  error,
  onConfirm,
  onCancel,
  children,
}: ConfirmDialogProps) {
  return (
    <Modal
      open={open}
      title={title}
      description={description}
      onClose={onCancel}
      dismissable={!loading}
    >
      {children}

      {error && (
        <p role="alert" className="mb-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="flex justify-end gap-2">
        <Button variant="ghost" disabled={loading} onClick={onCancel}>
          {cancelLabel}
        </Button>
        <Button
          variant={variant === "danger" ? "danger" : "primary"}
          loading={loading}
          onClick={onConfirm}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
