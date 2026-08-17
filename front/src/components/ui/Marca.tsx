import { cn } from "@/lib/cn";

/**
 * La marca de la aplicación.
 *
 * Es un logotipo tipográfico, no un logo: el logo de arlettepichardo.com es de Arlette, no del
 * sistema, y el propio está pendiente. Cuando exista, se reemplaza sólo este componente y
 * cambia en todos lados.
 */
export function Marca({ className }: { className?: string }) {
  return (
    <span
      className={cn(
        "font-serif text-xl leading-none font-bold tracking-tight text-brand-600",
        className,
      )}
    >
      Pipo
      {/* La E mayúscula es parte del nombre: son las iniciales de los cinco componentes. */}
      <span className="text-acento-rojo">E</span>
    </span>
  );
}
