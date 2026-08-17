import type { SelectHTMLAttributes } from "react";

import { cn } from "@/lib/cn";

type SelectProps = SelectHTMLAttributes<HTMLSelectElement>;

export function Select({ className, children, ...props }: SelectProps) {
  return (
    <select
      className={cn(
        "rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 transition-colors outline-brand-600 focus-visible:outline-2 focus-visible:outline-offset-2",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}
