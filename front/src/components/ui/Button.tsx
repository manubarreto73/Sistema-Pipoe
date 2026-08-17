import type { ButtonHTMLAttributes } from "react";

import { Spinner } from "@/components/ui/Spinner";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "danger";

const variantClasses: Record<Variant, string> = {
  primary: "bg-brand-600 text-white hover:bg-brand-700 outline-brand-600",
  secondary:
    "border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 outline-slate-400",
  ghost: "text-slate-600 hover:bg-slate-100 outline-slate-400",
  // Para lo irreversible. Sólo dentro de un diálogo de confirmación, nunca como acción directa.
  danger: "bg-red-600 text-white hover:bg-red-700 outline-red-600",
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  loading?: boolean;
};

export function Button({
  className,
  variant = "primary",
  loading = false,
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      // className va último dentro de cn() para que quien use el componente pueda
      // pisar cualquier clase por defecto.
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-60",
        variantClasses[variant],
        className,
      )}
      disabled={disabled || loading}
      {...props}
    >
      {loading && <Spinner />}
      {children}
    </button>
  );
}
