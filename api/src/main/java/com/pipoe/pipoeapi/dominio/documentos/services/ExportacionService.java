package com.pipoe.pipoeapi.dominio.documentos.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.pipoe.pipoeapi.exceptions.exceptions.BusinessException;

/**
 * El producto de una fase, sacado de la aplicación como archivo.
 *
 * El documento se guarda como HTML —es lo que produce el editor—, así que exportar es
 * traducir ese HTML a cada formato: a párrafos con estilo para Word, a una hoja A4 para PDF.
 */
@Service
public class ExportacionService {

    /** Qué se puede descargar. El nombre es el que llega por querystring. */
    public enum Formato {
        DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        PDF("pdf", "application/pdf");

        public final String extension;
        public final String contentType;

        Formato(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }

        public static Formato de(String valor) {
            for (Formato formato : values())
                if (formato.extension.equalsIgnoreCase(valor)) return formato;

            throw new BusinessException("Formato no soportado: " + valor + ". Se puede docx o pdf.");
        }
    }

    public record Archivo(String nombre, String contentType, byte[] contenido) {}

    private static final DateTimeFormatter FECHA =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"));

    private static final Set<String> ENCABEZADOS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    public Archivo exportar(Formato formato, Datos datos) {
        byte[] contenido = formato == Formato.DOCX ? aWord(datos) : aPdf(datos);
        return new Archivo(nombreArchivo(datos, formato), formato.contentType, contenido);
    }

    /** Lo que hace falta saber del paso para armar el archivo. */
    public record Datos(String proyecto, String fase, String titulo, String html) {}

    // -------------------------------------------------------------------- Word

    private byte[] aWord(Datos datos) {
        try (XWPFDocument documento = new XWPFDocument();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            escribirPortadaWord(documento, datos);

            Element cuerpo = Jsoup.parseBodyFragment(datos.html() == null ? "" : datos.html()).body();
            if (cuerpo.children().isEmpty()) {
                parrafo(documento, "Este documento todavía está vacío.", 11, false, true, 0);
            } else {
                for (Element bloque : cuerpo.children()) escribirBloqueWord(documento, bloque, 0);
            }

            documento.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el archivo de Word");
        }
    }

    private void escribirPortadaWord(XWPFDocument documento, Datos datos) {
        parrafo(documento, datos.proyecto(), 18, true, false, 0);
        parrafo(documento, datos.fase() + " · " + datos.titulo(), 12, false, true, 0);
        parrafo(documento, "Exportado el " + LocalDate.now().format(FECHA), 9, false, true, 0);

        XWPFParagraph separador = documento.createParagraph();
        separador.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
    }

    private void escribirBloqueWord(XWPFDocument documento, Element bloque, int nivel) {
        String etiqueta = bloque.tagName().toLowerCase(Locale.ROOT);

        if (etiqueta.equals("ul") || etiqueta.equals("ol")) {
            int numero = 1;
            for (Element item : bloque.children()) {
                String viñeta = etiqueta.equals("ol") ? (numero++) + ". " : "• ";
                XWPFParagraph parrafo = nuevoParrafo(documento, nivel + 1);
                run(parrafo, viñeta, Estilo.vacio(), 11);

                for (Node hijo : item.childNodes())
                    if (!esLista(hijo)) escribirInlineWord(parrafo, hijo, Estilo.vacio(), 11);

                // Una lista adentro de un ítem se escribe como bloque aparte, un nivel más adentro.
                for (Element anidada : item.children())
                    if (esLista(anidada)) escribirBloqueWord(documento, anidada, nivel + 1);
            }
            return;
        }

        if (etiqueta.equals("blockquote")) {
            for (Element hijo : bloque.children()) escribirBloqueWord(documento, hijo, nivel + 1);
            return;
        }

        boolean encabezado = ENCABEZADOS.contains(etiqueta);
        int tamanio = switch (etiqueta) {
            case "h1" -> 16;
            case "h2" -> 14;
            case "h3" -> 12;
            default -> 11;
        };

        XWPFParagraph parrafo = nuevoParrafo(documento, nivel);
        for (Node hijo : bloque.childNodes())
            escribirInlineWord(parrafo, hijo, new Estilo(encabezado, false, false, false, false), tamanio);
    }

    private void escribirInlineWord(XWPFParagraph parrafo, Node nodo, Estilo estilo, int tamanio) {
        if (nodo instanceof TextNode texto) {
            if (!texto.text().isEmpty()) run(parrafo, texto.text(), estilo, tamanio);
            return;
        }

        if (!(nodo instanceof Element elemento)) return;

        if (elemento.tagName().equals("br")) {
            parrafo.createRun().addBreak();
            return;
        }

        Estilo propio = estilo.con(elemento.tagName());
        for (Node hijo : elemento.childNodes()) escribirInlineWord(parrafo, hijo, propio, tamanio);
    }

    private boolean esLista(Node nodo) {
        return nodo instanceof Element elemento
            && (elemento.tagName().equals("ul") || elemento.tagName().equals("ol"));
    }

    private XWPFParagraph nuevoParrafo(XWPFDocument documento, int sangria) {
        XWPFParagraph parrafo = documento.createParagraph();
        parrafo.setSpacingAfter(120);
        if (sangria > 0) parrafo.setIndentationLeft(360 * sangria);
        return parrafo;
    }

    private void parrafo(
        XWPFDocument documento, String texto, int tamanio, boolean negrita, boolean tenue, int sangria
    ) {
        XWPFParagraph parrafo = nuevoParrafo(documento, sangria);
        parrafo.setAlignment(ParagraphAlignment.LEFT);

        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontSize(tamanio);
        run.setBold(negrita);
        if (tenue) run.setColor("64748B");
    }

    private void run(XWPFParagraph parrafo, String texto, Estilo estilo, int tamanio) {
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontSize(tamanio);
        run.setBold(estilo.negrita());
        run.setItalic(estilo.cursiva());
        if (estilo.subrayado()) run.setUnderline(org.apache.poi.xwpf.usermodel.UnderlinePatterns.SINGLE);
        if (estilo.tachado()) run.setStrikeThrough(true);
        if (estilo.codigo()) run.setFontFamily("Consolas");
    }

    /** Marcas de formato que arrastra el recorrido del HTML hacia adentro. */
    private record Estilo(
        boolean negrita, boolean cursiva, boolean subrayado, boolean tachado, boolean codigo
    ) {
        static Estilo vacio() { return new Estilo(false, false, false, false, false); }

        Estilo con(String etiqueta) {
            return switch (etiqueta.toLowerCase(Locale.ROOT)) {
                case "strong", "b" -> new Estilo(true, cursiva, subrayado, tachado, codigo);
                case "em", "i" -> new Estilo(negrita, true, subrayado, tachado, codigo);
                case "u" -> new Estilo(negrita, cursiva, true, tachado, codigo);
                case "s", "del", "strike" -> new Estilo(negrita, cursiva, subrayado, true, codigo);
                case "code" -> new Estilo(negrita, cursiva, subrayado, tachado, true);
                default -> this;
            };
        }
    }

    // --------------------------------------------------------------------- PDF

    /**
     * Se arma una hoja HTML completa y la renderiza openhtmltopdf. El HTML se pasa por jsoup
     * antes: el renderizador exige XHTML bien formado y el editor no garantiza eso.
     */
    private byte[] aPdf(Datos datos) {
        String cuerpo = datos.html() == null || datos.html().isBlank()
            ? "<p class=\"vacio\">Este documento todavía está vacío.</p>"
            : datos.html();

        String pagina = """
            <html><head><meta charset="utf-8"/><style>
              @page { size: A4; margin: 2.5cm; }
              body { font-family: serif; font-size: 11pt; line-height: 1.55; color: #1f2937; }
              h1.portada { font-size: 18pt; margin: 0; }
              p.subtitulo { color: #475569; margin: 4pt 0 0; }
              p.fecha { color: #94a3b8; font-size: 9pt; margin: 2pt 0 0; }
              hr { border: 0; border-top: 1px solid #cbd5e1; margin: 14pt 0 18pt; }
              h1, h2, h3 { margin: 14pt 0 6pt; }
              h1 { font-size: 16pt; } h2 { font-size: 14pt; } h3 { font-size: 12pt; }
              p { margin: 0 0 8pt; }
              ul, ol { margin: 0 0 8pt; padding-left: 18pt; }
              li { margin-bottom: 3pt; }
              blockquote { margin: 0 0 8pt 14pt; padding-left: 10pt;
                           border-left: 2px solid #cbd5e1; color: #475569; }
              code, pre { font-family: monospace; font-size: 10pt; }
              p.vacio { color: #94a3b8; }
            </style></head><body>
              <h1 class="portada">%s</h1>
              <p class="subtitulo">%s · %s</p>
              <p class="fecha">Exportado el %s</p>
              <hr/>
              %s
            </body></html>
            """.formatted(
                escapar(datos.proyecto()),
                escapar(datos.fase()),
                escapar(datos.titulo()),
                LocalDate.now().format(FECHA),
                cuerpo
            );

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            org.w3c.dom.Document dom = new W3CDom().fromJsoup(Jsoup.parse(pagina));

            PdfRendererBuilder constructor = new PdfRendererBuilder();
            constructor.useFastMode();
            // El renderizador iría a buscar cualquier recurso que aparezca en el HTML. Un
            // `<img src="file:///etc/passwd">` o un `<img src="http://169.254.169.254/...">`
            // guardado en un documento haría que el servidor leyera sus propios archivos o
            // consultara su red interna, y lo incrustara en la descarga. Devolver null para
            // todo desactiva esa resolución por completo. El sanitizado de entrada ya quita
            // las etiquetas que podrían pedir recursos; esto es la segunda cerradura.
            constructor.useUriResolver((baseUri, uri) -> null);
            constructor.withW3cDocument(dom, "");
            constructor.toStream(salida);
            constructor.run();

            return salida.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("No se pudo generar el PDF");
        }
    }

    // ------------------------------------------------------------------ apoyo

    /**
     * Nombre del archivo que va a ver quien lo descargue. Se limpian los caracteres que
     * Windows y macOS no aceptan en un nombre de archivo.
     */
    private String nombreArchivo(Datos datos, Formato formato) {
        String base = String.join(" - ", List.of(datos.proyecto(), datos.fase(), datos.titulo()));

        String limpio = base
            .replaceAll("[\\\\/:*?\"<>|\\r\\n]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (limpio.length() > 120) limpio = limpio.substring(0, 120).trim();

        return limpio + "." + formato.extension;
    }

    private String escapar(String texto) {
        return texto == null ? "" : texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
