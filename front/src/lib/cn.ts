import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/**
 * Une clases condicionales (clsx) y resuelve los conflictos de Tailwind (tailwind-merge).
 * Sin twMerge, `cn("px-4", "px-8")` dejaría ambas y ganaría la que Tailwind haya
 * emitido último en el CSS, no la que pasaste.
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
