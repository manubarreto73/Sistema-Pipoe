package com.pipoe.pipoeapi.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Limpia el HTML que llega de los editores antes de guardarlo.
 *
 * El editor del navegador produce HTML inofensivo, pero **nadie está obligado a usar el
 * editor**: cualquiera con permiso de escritura puede mandar el `PUT` a mano con
 * `<img src=x onerror="...">` adentro. Ese HTML después se muestra con
 * `dangerouslySetInnerHTML` en la sesión de otra persona —típicamente la dueña del proyecto,
 * que es la que más permisos tiene—, así que sin esto un permiso de edición alcanza para
 * quedarse con la cuenta de quien lea el documento.
 *
 * La lista blanca es cerrada: sólo las etiquetas que el editor puede generar, y **ningún
 * atributo**. Sin `style`, sin `href`, sin `src`. Eso además cierra la puerta de la
 * exportación a PDF, donde un `<img src="file:///etc/passwd">` haría que el servidor leyera
 * sus propios archivos y los incrustara en la descarga.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {}

    /**
     * Exactamente lo que produce el editor (TipTap con StarterKit). Cualquier otra cosa se
     * descarta conservando su texto.
     */
    private static final Safelist PERMITIDO = Safelist.none()
        .addTags(
            "p", "br", "strong", "b", "em", "i", "u", "s", "del",
            "h1", "h2", "h3", "ul", "ol", "li", "blockquote", "code", "pre"
        );

    /**
     * `prettyPrint(false)` es importante: con el formateo activado jsoup mete saltos de línea
     * e indentación, y eso cambiaría el texto guardado en cada limpieza.
     */
    private static final Document.OutputSettings SIN_FORMATEO =
        new Document.OutputSettings().prettyPrint(false);

    public static String limpiar(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.clean(html, "", PERMITIDO, SIN_FORMATEO);
    }

    /** Para los campos que son de una sola línea: se descarta cualquier etiqueta. */
    public static String aTextoPlano(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return Jsoup.clean(texto, "", Safelist.none(), SIN_FORMATEO).trim();
    }
}
