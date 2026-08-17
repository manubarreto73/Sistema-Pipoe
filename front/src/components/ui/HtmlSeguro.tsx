import DOMPurify from "dompurify";

import { cn } from "@/lib/cn";

/**
 * Muestra HTML que vino de la API, filtrándolo antes de insertarlo.
 *
 * La API ya lo limpia al guardarlo (`HtmlSanitizer.java`), así que esto es defensa en
 * profundidad: cubre lo que se haya guardado *antes* de que existiera aquella limpieza, y
 * cualquier camino nuevo que en el futuro escriba en esas columnas sin pasar por el servicio.
 *
 * Es el único lugar de la aplicación que puede usar `dangerouslySetInnerHTML`.
 */

/** Lo mismo que permite el servidor. Cualquier otra etiqueta se descarta con su contenido. */
const ETIQUETAS = [
  "p", "br", "strong", "b", "em", "i", "u", "s", "del",
  "h1", "h2", "h3", "ul", "ol", "li", "blockquote", "code", "pre",
];

type Props = {
  html: string;
  className?: string;
};

export function HtmlSeguro({ html, className }: Props) {
  const limpio = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ETIQUETAS,
    // Ningún atributo: sin `style`, sin `href`, sin `on*`.
    ALLOWED_ATTR: [],
  });

  return <div className={cn(className)} dangerouslySetInnerHTML={{ __html: limpio }} />;
}
