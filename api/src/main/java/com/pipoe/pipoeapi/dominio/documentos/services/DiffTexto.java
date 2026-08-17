package com.pipoe.pipoeapi.dominio.documentos.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Qué cambió entre dos versiones de un documento.
 *
 * Compara el texto plano, no el HTML: al autor del historial le importa qué palabras se
 * escribieron o se borraron, no que TipTap haya reordenado un atributo.
 *
 * La comparación es por palabras y no por caracteres porque el resultado se lee: un diff de
 * caracteres parte las palabras a la mitad y no se entiende nada.
 */
public final class DiffTexto {

    private DiffTexto() {}

    public enum Tipo { IGUAL, AGREGADO, QUITADO }

    public record Segmento(Tipo tipo, String texto) {}

    /** Cuántas palabras entraron y cuántas salieron, para el renglón del historial. */
    public record Resumen(int agregadas, int quitadas) {}

    /**
     * Tope de la matriz de la comparación fina. Por encima, el cambio es tan grande que
     * mostrar palabra por palabra no aportaría nada: se informa como reemplazo del bloque.
     */
    private static final int MAXIMO_CELDAS = 1_000_000;

    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    public static List<Segmento> comparar(String htmlAnterior, String htmlNuevo) {
        List<String> anterior = tokenizar(aTextoPlano(htmlAnterior));
        List<String> nuevo = tokenizar(aTextoPlano(htmlNuevo));

        // Los guardados automáticos suelen tocar unas pocas palabras: recortar lo que ya era
        // igual al principio y al final deja una matriz diminuta aunque el documento sea largo.
        int inicio = 0;
        while (inicio < anterior.size() && inicio < nuevo.size()
            && anterior.get(inicio).equals(nuevo.get(inicio))) inicio++;

        int fin = 0;
        while (fin < anterior.size() - inicio && fin < nuevo.size() - inicio
            && anterior.get(anterior.size() - 1 - fin).equals(nuevo.get(nuevo.size() - 1 - fin))) fin++;

        List<String> medioAnterior = anterior.subList(inicio, anterior.size() - fin);
        List<String> medioNuevo = nuevo.subList(inicio, nuevo.size() - fin);

        List<Segmento> segmentos = new ArrayList<>();
        agregar(segmentos, Tipo.IGUAL, anterior.subList(0, inicio));

        if ((long) medioAnterior.size() * medioNuevo.size() > MAXIMO_CELDAS) {
            agregar(segmentos, Tipo.QUITADO, medioAnterior);
            agregar(segmentos, Tipo.AGREGADO, medioNuevo);
        } else {
            segmentos.addAll(compararMedio(medioAnterior, medioNuevo));
        }

        agregar(segmentos, Tipo.IGUAL, anterior.subList(anterior.size() - fin, anterior.size()));

        return unir(segmentos);
    }

    public static Resumen resumir(List<Segmento> segmentos) {
        int agregadas = 0;
        int quitadas = 0;

        for (Segmento segmento : segmentos) {
            if (segmento.tipo() == Tipo.IGUAL) continue;

            int palabras = contarPalabras(segmento.texto());
            if (segmento.tipo() == Tipo.AGREGADO) agregadas += palabras;
            else quitadas += palabras;
        }

        return new Resumen(agregadas, quitadas);
    }

    public static Resumen resumir(String htmlAnterior, String htmlNuevo) {
        return resumir(comparar(htmlAnterior, htmlNuevo));
    }

    // ------------------------------------------------------------------ interno

    /** Subsecuencia común más larga, reconstruida hacia atrás para armar los segmentos. */
    private static List<Segmento> compararMedio(List<String> anterior, List<String> nuevo) {
        int n = anterior.size();
        int m = nuevo.size();

        if (n == 0 && m == 0) return List.of();
        if (n == 0) return List.of(new Segmento(Tipo.AGREGADO, String.join("", nuevo)));
        if (m == 0) return List.of(new Segmento(Tipo.QUITADO, String.join("", anterior)));

        int[][] comun = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--)
            for (int j = m - 1; j >= 0; j--)
                comun[i][j] = anterior.get(i).equals(nuevo.get(j))
                    ? comun[i + 1][j + 1] + 1
                    : Math.max(comun[i + 1][j], comun[i][j + 1]);

        List<Segmento> segmentos = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < n && j < m) {
            if (anterior.get(i).equals(nuevo.get(j))) {
                segmentos.add(new Segmento(Tipo.IGUAL, anterior.get(i)));
                i++;
                j++;
            } else if (comun[i + 1][j] >= comun[i][j + 1]) {
                segmentos.add(new Segmento(Tipo.QUITADO, anterior.get(i)));
                i++;
            } else {
                segmentos.add(new Segmento(Tipo.AGREGADO, nuevo.get(j)));
                j++;
            }
        }

        agregar(segmentos, Tipo.QUITADO, anterior.subList(i, n));
        agregar(segmentos, Tipo.AGREGADO, nuevo.subList(j, m));

        return segmentos;
    }

    private static void agregar(List<Segmento> segmentos, Tipo tipo, List<String> tokens) {
        if (!tokens.isEmpty()) segmentos.add(new Segmento(tipo, String.join("", tokens)));
    }

    /** Junta los segmentos consecutivos del mismo tipo: uno por token sería ilegible. */
    private static List<Segmento> unir(List<Segmento> segmentos) {
        List<Segmento> unidos = new ArrayList<>();

        for (Segmento segmento : segmentos) {
            if (segmento.texto().isEmpty()) continue;

            int ultimo = unidos.size() - 1;
            if (ultimo >= 0 && unidos.get(ultimo).tipo() == segmento.tipo())
                unidos.set(ultimo, new Segmento(
                    segmento.tipo(), unidos.get(ultimo).texto() + segmento.texto()));
            else
                unidos.add(segmento);
        }

        return unidos;
    }

    /**
     * Corta en palabras, guardando los espacios como tokens propios. Así el texto se puede
     * reconstruir tal cual concatenando, sin inventar ni comerse separaciones.
     */
    private static List<String> tokenizar(String texto) {
        if (texto.isEmpty()) return Collections.emptyList();

        List<String> tokens = new ArrayList<>();
        Matcher matcher = ESPACIOS.matcher(texto);
        int desde = 0;

        while (matcher.find()) {
            if (matcher.start() > desde) tokens.add(texto.substring(desde, matcher.start()));
            tokens.add(matcher.group());
            desde = matcher.end();
        }

        if (desde < texto.length()) tokens.add(texto.substring(desde));

        return tokens;
    }

    private static int contarPalabras(String texto) {
        String limpio = texto.trim();
        return limpio.isEmpty() ? 0 : ESPACIOS.split(limpio).length;
    }

    /**
     * El HTML de TipTap a texto legible. Los cierres de bloque se vuelven saltos de línea para
     * no pegar el final de un párrafo con el principio del siguiente.
     */
    public static String aTextoPlano(String html) {
        if (html == null || html.isBlank()) return "";

        String texto = html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</(p|div|li|h[1-6]|blockquote|tr|pre)>", "\n")
            .replaceAll("<[^>]*>", "");

        return desescapar(texto)
            .replaceAll("[ \\t]+", " ")
            .replaceAll(" ?\n ?", "\n")
            .replaceAll("\n{3,}", "\n\n")
            .trim();
    }

    private static String desescapar(String texto) {
        return texto
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            // El &amp; va último: si no, desarma las entidades que acaban de expandirse.
            .replace("&amp;", "&");
    }
}
