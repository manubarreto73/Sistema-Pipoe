# Eje visual

Plan para alinear la identidad gráfica de la app con la de Arlette. **Ningún punto de este
documento cambia funcionalidad**: son colores, tipografías, espaciados y piezas gráficas.

## De dónde salen las decisiones

Todo lo de acá está sacado del sitio real de Arlette (`arlettepichardo.com`), no inventado. Los
valores se leyeron de su hoja de estilos (`arlette-pichardo.webflow.shared.css`). El sitio está
hecho en Webflow.

### Su paleta

| Color | Hex | Uso en su sitio |
|---|---|---|
| Azul institucional | `#1a4691` | Color de marca. Títulos, barra de navegación, pie |
| Rojo | `#c12026` | El acento más presente de todos: botones, subrayados, destacados |
| Celeste | `#90c6e6` | Fondos de sección, tarjetas |
| Celeste pálido | `#c2e0f1` | Bloques de cita y separadores |
| Amarillo | `#fec01a` | Acento puntual |
| Grises | `#fafafa` `#f5f5f5` `#e6e6e6` `#ddd` `#999` `#333` `#222` | Fondos y texto |

Los demás colores que aparecen en su HTML (`#4285f4`, `#34a853`, `#f05022`, `#00a4ee`…) son de
logos de terceros —Google, íconos de redes— y **no** forman parte de su identidad.

### Sus tipografías

- **Lato** — sans serif. Es la de todo el cuerpo y la interfaz (aparece 271 veces en su CSS).
  Pesos cargados: 100, 300, 400, 700, 900, con itálicas.
- **Alegreya** — serif. La de los títulos (38 apariciones). Pesos: 400, 500, 600, 800.

La combinación serif-para-títulos / sans-para-texto es lo que le da el aire académico del sitio.

### Dónde estamos hoy

La app usa **índigo de Tailwind** (`#4f46e5` y su rampa) como color de marca y **system-ui** como
única tipografía. Son los valores por defecto que puse para arrancar; no tienen relación con
Arlette. Todo vive en `front/src/index.css`, dentro del bloque `@theme`, así que el cambio es
puntual y no toca ninguna pantalla.

---

## ✅ Prioridad 1 · La base de marca — hecho el 2026-08-13

Con esto solo, la app deja de parecer un template y pasa a parecer de ella. Es además lo más
barato: dos archivos.

**Lo que quedó implementado:**

- Rampa `brand-*` construida sobre `#1a4691`, con `brand-600` exactamente en ese azul porque es
  el tono de los botones primarios. Se propagó sola a toda la aplicación.
- Tokens `acento-rojo`, `acento-celeste`, `acento-celeste-claro` y `acento-amarillo`, **usados
  sólo en la portada**: adentro de la app el color es información y no decoración.
- Lato y Alegreya auto-hospedadas en `front/src/assets/fonts`, subconjunto latino, 114 KB entre
  los cuatro archivos. Alegreya en los títulos vía `font-serif`, Lato en todo lo demás.
- Un detalle que costó decidir: Lato no trae los pesos 500 ni 600, así que las caras se declaran
  con **rangos** (`font-weight: 100 400` y `500 900`). Sin eso, `font-medium` —que la interfaz usa
  para dar énfasis en decenas de lugares— caería silenciosamente a peso normal.
- `Marca`, un logotipo tipográfico con la E en rojo, en un componente propio para que el día que
  exista el logo se reemplace en un solo lugar.

### 1.1 Cambiar la rampa de color

Reemplazar la rampa `brand-*` de `index.css` por una construida alrededor de `#1a4691`, y sumar
tokens para el rojo, el celeste y el amarillo. Todos los componentes ya usan `brand-600`,
`brand-50`, etc., así que **el cambio se propaga solo** a toda la app.

Cuidado con dos cosas:

- El rojo `#c12026` es su acento principal, pero en la app el rojo **ya significa "peligro"**
  (borrar un proyecto, quitar un colaborador, errores). Si lo uso también como acento decorativo,
  se rompe el código de colores y un botón bonito se confunde con uno destructivo. Propongo:
  el rojo de marca queda para la **portada pública**, y adentro de la app se reserva para lo
  destructivo. Es una decisión a confirmar con vos.
- Verificar contraste. `#c12026` sobre blanco da 5.9:1 (pasa AA para texto normal). `#fec01a`
  sobre blanco da 1.7:1: **no sirve para texto**, sólo para fondos y detalles.

### 1.2 Tipografías

Sumar Lato y Alegreya, **auto-hospedadas** (los archivos dentro del proyecto), no desde el CDN de
Google. Tres motivos: la página carga sin depender de un tercero, no se filtra la IP de cada
visitante a Google —que es un problema legal real en Europa— y permite una Content-Security-Policy
más cerrada, que es justo lo que pide el eje de seguridad.

- Alegreya para los títulos de la portada y los encabezados de sección.
- Lato para todo el resto, incluida la interfaz de la app.

### 1.3 Logo y favicon — pendiente, y es del sistema

El logo de `arlettepichardo.com` es **de Arlette, no del Modelo PipoE**: son dos identidades
distintas y la aplicación necesita la suya. Queda para más adelante decidir si se encarga o se le
pide a ella.

Mientras tanto, la marca es el componente `Marca`: la palabra en Alegreya, en azul, con la **E**
en rojo —la inicial de Evaluación, el quinto componente—. Cuando exista un logo de verdad se
reemplaza ese único archivo y cambia en toda la aplicación.

Falta también el **favicon**, que hoy sigue siendo el de Vite.

---

## ✅ Prioridad 2 · La portada — hecho el 2026-08-13

**Lo que quedó implementado:** encabezado fijo con la marca y los dos accesos, portada a sangre
en azul institucional con los botones arriba, secciones alternando fondo blanco y gris con un
subrayado rojo bajo cada título, la foto flotando para que el texto la rodee, un cierre que repite
la invitación, y un pie con el enlace a `arlettepichardo.com`.

**Lo que quedó afuera a propósito:** dividir el formulario de acceso en tres pasos con indicador
de avance. Cambia comportamiento, no sólo apariencia, así que va como propuesta aparte.

## Prioridad 2 (original) · La portada

Es lo único que ve el público antes de pedir acceso, y lo que Arlette va a mostrar. Hoy es una
columna de texto correcta pero sin ninguna personalidad.

- **Encabezado y pie propios.** La portada hoy no tiene barra de navegación ni pie. Su sitio los
  tiene, con el logo y los enlaces. Sumar un encabezado con el logo y un pie con el enlace a
  `arlettepichardo.com` y el crédito.
- **Portada con imagen.** Su sitio abre con un banner grande. Acá se puede usar una imagen o un
  bloque de color de marca detrás del título y la bajada, en vez del blanco actual.
- **Ritmo de secciones.** Alternar fondo blanco y `#fafafa` o celeste pálido entre secciones, como
  hace ella, para que la página no sea un scroll plano.
- **Los botones de acceso más arriba.** Hoy "Pedir acceso" está al final de todo el texto. Repetirlo
  arriba, junto al título.
- **El formulario de acceso.** Son 15 campos en una sola página; funciona, pero se puede dividir
  visualmente en tres pasos con un indicador de avance. **Ojo**: eso sí toca comportamiento, así
  que lo dejo anotado como propuesta aparte y no lo incluyo en este eje.

---

## Prioridad 3 · Adentro de la app

- **Íconos.** Hoy hay flechas y signos escritos como texto (`→`, `↗`, `−`, `+`) y un único ícono
  SVG, el engranaje. Sumar un set liviano y consistente (Lucide, ~1 KB por ícono usado) y
  reemplazarlos. Es lo que más rápido hace que una interfaz deje de verse improvisada.
- **Identidad por fase.** Las 5 fases son la columna vertebral del modelo y hoy se ven todas
  iguales. Darle a cada una un color de la paleta de Arlette, usado de forma consistente en la
  barra lateral, en el flujo de pasos y en el encabezado del documento. Ayuda a orientarse sin
  leer.
- **Estados de carga.** Hoy todo muestra un spinner con "Cargando…". Reemplazar por esqueletos
  con la forma del contenido que va a llegar: la pantalla no salta y se percibe más rápida.
- **Estados vacíos.** "Todavía no tenés proyectos" es una línea de texto suelta. Merece una
  ilustración o un ícono grande y un botón claro.
- **La hoja del paso.** Es donde la gente pasa el tiempo. Vale darle ancho de lectura cómodo,
  tipografía serif para el cuerpo del documento y algo más de aire.
- **Los archivos exportados.** El Word y el PDF ya se generan con estilos propios
  (`ExportacionService`). Cuando esté el logo, ponerlo en el encabezado: son los documentos que
  la gente va a compartir fuera de la app y hoy salen sin marca.

---

## Prioridad 4 · Accesibilidad

Su sitio menciona explícitamente la accesibilidad, así que asumo que le importa.

- Revisar contraste de toda la paleta nueva contra AA (4.5:1 para texto).
- Foco visible en todo lo navegable por teclado. Los componentes actuales ya lo tienen
  (`focus-visible:outline-2`); hay que mantenerlo al cambiar colores.
- La foto de la biografía y el logo necesitan texto alternativo real.
- Probar la app entera con teclado, sin mouse.

---

## Qué falta

**Trabajo pendiente:** todo lo de Prioridad 3 (íconos, identidad por fase, esqueletos de carga,
estados vacíos, la hoja del paso) y la revisión de accesibilidad de Prioridad 4.

**Decisiones pendientes:**

1. El **logo del sistema** — ver 1.3. Es lo único que bloquea cerrar la identidad.
2. Una **imagen para la portada**, si Arlette tiene alguna que quiera usar. Hoy el encabezado es
   un bloque de color.

**Ya resuelto:** el rojo quedó como acento gráfico en la portada —la E de la marca, el subrayado
de los títulos— y reservado para lo destructivo adentro de la aplicación.
