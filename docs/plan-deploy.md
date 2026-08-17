# Eje de deploy · las decisiones

**El *cómo* ya está escrito** en [`../DEPLOY.md`](../DEPLOY.md): Dockerfiles, compose, nginx,
variables, backups, CI. Este documento no lo repite. Es la capa que falta: **qué contratar, en qué
orden, y qué esperar** cuando nunca hosteaste un sitio.

---

## Las tres decisiones

### 1 · Dónde correrlo

#### Primero: qué es un VPS y qué es un PaaS

Son dos formas de alquilar una computadora ajena, que se diferencian en **hasta dónde llega la
responsabilidad del proveedor**.

**VPS** (*Virtual Private Server*, servidor privado virtual) es una computadora entera para vos,
sólo que virtual: el proveedor tiene una máquina grande y la parte en varias, y una de esas
partes es tuya. Te entregan Linux recién instalado y una dirección IP. **Desde ahí, todo es
tuyo**: instalar Docker, abrir y cerrar puertos, configurar el certificado, hacer los backups,
aplicar las actualizaciones de seguridad del sistema. El proveedor sólo garantiza que la máquina
esté encendida y conectada.

> La analogía: alquilás un local vacío. Las llaves son tuyas, ponés lo que quieras adentro, y si
> se rompe una canilla la arreglás vos.

**PaaS** (*Platform as a Service*, plataforma como servicio) es un escalón más arriba. No te dan
una máquina: les das tu código y ellos se encargan del resto. Conectás el repositorio, el
proveedor lo compila, lo levanta, le pone el certificado, lo reinicia si se cae, y te ofrece la
base de datos como un servicio con backups incluidos. Vos no ves el sistema operativo ni sabés en
qué máquina corre.

> La analogía: alquilás una oficina en un edificio con recepción, limpieza y mantenimiento. Menos
> libertad, pero no te ocupás de la canilla.

| | VPS | PaaS |
|---|---|---|
| Qué recibís | Una máquina Linux vacía | Una plataforma donde subís código |
| Quién instala Docker, nginx, TLS | Vos | Ellos |
| Quién hace los backups | Vos | Ellos (según el plan) |
| Quién actualiza el sistema operativo | Vos | Ellos |
| Si se cae a las 3 AM | Te levantás vos | Se reinicia solo |
| Costo para este proyecto | 5–15 USD/mes | 20–40 USD/mes |
| Cuánto vas a aprender | Mucho | Poco |

**Para este proyecto recomiendo VPS**, y dentro de esa categoría, **Hetzner**. El motivo no es el
precio sino la forma del sistema: son cuatro contenedores ya descritos en un `docker-compose.yml`.
En un VPS eso es literalmente un comando. En un PaaS hay que desarmarlo en cuatro servicios del
proveedor, aprender su manera de conectar variables y redes, y perder el compose como
documentación de cómo encaja todo. **La "facilidad" del PaaS se paga cuando algo falla: no sabés
dónde mirar.**

El contraargumento honesto: si más adelante no querés ocuparte del mantenimiento del servidor, un
PaaS es una salida perfectamente razonable y no es difícil mudarse. No es una puerta que se
cierre.

#### La comparación completa

| | Costo | A favor | En contra |
|---|---|---|---|
| **VPS (Hetzner, DigitalOcean)** | 5–15 USD/mes | Todo el plan de `DEPLOY.md` aplica tal cual. Control total. Barato. | Administrás vos el sistema operativo, los backups y las actualizaciones |
| **PaaS (Railway, Render, Fly)** | 20–40 USD/mes | Postgres y Redis gestionados con backup, deploy desde git, TLS solo | Más caro, menos control de red, y hay que traducir el compose |
| **Cloud grande (AWS, GCP)** | Variable, más caro | Escala sin techo | Sobra por completo. La complejidad no se paga sola sin escala |

**Por qué VPS y no PaaS**, aunque el PaaS parezca más fácil: el stack son cuatro contenedores que
ya están descritos en un compose. En un VPS eso es un `docker compose up -d` y listo. En un PaaS
hay que desarmarlo en servicios del proveedor y aprender su forma de hacer las cosas. La
"facilidad" del PaaS se cobra en que cuando algo falla, no sabés dónde mirar.

**Por qué Hetzner:** un CX22 (2 vCPU, 4 GB, 40 GB SSD) sale **unos 4–5 euros por mes** y sobra para
este sistema. DigitalOcean cuesta más del doble por lo mismo. Si te importa que el servidor esté
en América —por latencia desde Costa Rica o República Dominicana— Hetzner tiene datacenter en
Estados Unidos (Ashburn e Hillsboro); igual, con Cloudflare adelante la diferencia se nota poco.

**Lo que hay que asumir del VPS:** el servidor es tuyo, y eso incluye mantenerlo. En concreto:
actualizaciones de seguridad del sistema (`unattended-upgrades` lo automatiza), firewall cerrado
salvo 22, 80 y 443, SSH sólo con clave y sin contraseña, y fail2ban. Es media jornada de setup
inicial y después casi nada.

### 2 · El dominio

Dos cosas separadas que conviene no mezclar:

- **Registrador** (dónde comprás el nombre): **Cloudflare Registrar** o **Namecheap**. Cloudflare
  vende al costo, sin margen ni promoción del primer año que después se triplica. Un `.com` ronda
  los 10–12 USD por año. **Evitá GoDaddy**: precio de entrada bajo, renovación cara y venta cruzada
  constante.
- **DNS** (quién responde dónde vive el sitio): **Cloudflare**, gratis, aunque el dominio esté
  comprado en otro lado.

**Qué nombre.** Esto es de Arlette, no técnico. Lo que sí importa técnicamente:

- Si ella ya tiene `arlettepichardo.com`, **la opción más simple es un subdominio**:
  `pipoe.arlettepichardo.com` o `app.arlettepichardo.com`. No se paga nada, hereda la reputación
  del dominio para el correo, y se configura con un registro DNS.
- Si prefiere un dominio propio (`modelopipoe.com` o similar), es una decisión de identidad. Se
  puede empezar con el subdominio y mudar después: mudar de dominio es molesto pero no grave.

**Preguntale antes de comprar nada.** Si el dominio queda a nombre tuyo y no de ella, es un
problema el día que quieran separarse.

### 3 · El correo

Es el punto que más se subestima y **el sistema depende de él**: las contraseñas de los
colaboradores viajan sólo por mail. Si el correo no llega, nadie puede entrar.

No uses Gmail personal con una *app password*: tiene límites diarios bajos y termina en spam. Usá
un servicio de envío transaccional. **Brevo** o **Resend** tienen plan gratuito suficiente
(alrededor de 100–300 mails por día, y este sistema manda unos pocos por semana). Los dos dan las
credenciales SMTP que la aplicación ya sabe usar; es cambiar tres variables de entorno.

Además hay que configurar **SPF, DKIM y DMARC** en el DNS del dominio. Sin eso, los mails caen en
spam. El servicio de envío te dice exactamente qué registros poner; es copiar y pegar en
Cloudflare.

---

---

## Las piezas que hay que contratar o instalar

Ninguna de estas es opcional para un sitio público, y todas tienen plan gratuito o son gratis.

### Cloudflare — el portero de la puerta

**Qué es.** Un intermediario entre internet y tu servidor. En vez de que el dominio apunte
directo a tu máquina, apunta a Cloudflare, y Cloudflare le pregunta a tu máquina. Todo el tráfico
pasa por ellos antes de llegarte.

**Para qué sirve.** Que ese intermediario exista te da varias cosas de una:

- **WAF** (*Web Application Firewall*, cortafuegos de aplicación): un filtro con reglas que
  reconocen ataques conocidos —inyecciones, escaneos, exploits de librerías— y los frenan antes
  de que lleguen a la API.
- **Mitigación de bots**: distingue navegadores de gente real de programas que recorren internet
  buscando qué romper. Los segundos son la mayoría del tráfico de un sitio nuevo.
- **Protección contra DDoS**: si alguien te manda un millón de pedidos para tumbarte, los absorbe
  su red, que es enorme, en vez de tu VPS de 4 GB.
- **Límite de peticiones** por IP y por ruta, configurable sin tocar código.
- **Ocultar la IP real del servidor**: nadie sabe dónde está tu máquina, así que no la pueden
  atacar salteándose el filtro.
- De yapa, **caché y CDN**: los archivos del front se sirven desde el nodo más cercano al
  visitante, y la página carga más rápido en Costa Rica que si viajara desde Alemania.

**Cómo se integra.** No se instala nada en el servidor. Se crea la cuenta, se agrega el dominio,
y ellos te dan dos *nameservers* que hay que poner en el registrador. A partir de ahí el DNS lo
manejás desde su panel: creás un registro `A` con la IP del VPS y activás el ícono de la nubecita
naranja, que es lo que hace que el tráfico pase por ellos. Media hora en total.

**Cuidado con una cosa:** con Cloudflare en el medio, tu servidor ve la IP de Cloudflare, no la
del visitante. La IP real viene en la cabecera `CF-Connecting-IP`, y el nginx del borde tiene que
copiarla a `X-Forwarded-For`. Si no, el bloqueo por intentos fallidos empieza a bloquear a
Cloudflare entero —o sea, a todo el mundo—. **La aplicación ya está preparada**: sólo lee esa
cabecera cuando `TRUST_PROXY=true`, que es una variable que hay que activar a mano justamente
cuando el proxy esté bien configurado.

### fail2ban — el que echa a los insistentes

**Qué es.** Un programa que se instala **en el servidor**, lee los archivos de registro y bloquea
en el cortafuegos del sistema a las direcciones que se portan mal.

**Para qué sirve.** La aplicación ya frena los intentos de adivinar contraseñas, pero lo hace
*dentro* de la aplicación: el atacante igual consume conexiones, CPU y espacio de log. fail2ban
lo saca antes, a nivel de red. Y sobre todo protege **el SSH del servidor**, que la aplicación no
ve: apenas un VPS tiene IP pública, empieza a recibir intentos de entrar por SSH; no es
paranoia, es lo normal, son bots.

**Cómo se integra.** `apt install fail2ban` y un archivo de configuración por servicio. Trae
lista para usar la regla de SSH. Para la aplicación se le agrega una regla que mire los
renglones `SEGURIDAD bloqueo=...` que ahora escribe la API, y bloquee la IP a nivel de sistema
tras varios.

### El registro estructurado — de dónde salen los datos

Ya está hecho, así que es más una explicación de qué mirar. La API escribe renglones así:

```
SEGURIDAD login_fallido cuenta=alguien@ejemplo.com ip=203.0.113.7
SEGURIDAD bloqueo=ip valor=203.0.113.7 intentos=5 minutos=30
SEGURIDAD bloqueo=cuenta valor=victima@ejemplo.com intentos=10 minutos=30
SEGURIDAD reuso_refresh_token familia=8f3c... — se revocan todas sus sesiones
SEGURIDAD login_admin cuenta=admin@pipoe.com ip=203.0.113.7
SEGURIDAD acceso_denegado motivo=...
```

Todos empiezan con la misma palabra a propósito: un `grep SEGURIDAD` alcanza para verlos todos, y
es sobre eso que se configuran fail2ban y las alertas. Nunca se escribe un token ni una
contraseña.

De esos, **`reuso_refresh_token` y `login_admin` son los que hay que mirar siempre**. El primero
significa que alguien tiene una copia de una sesión ajena. El segundo, que entró una cuenta de
administración: son dos en todo el sistema, así que cada renglón debería ser esperado.

### El servicio de correo transaccional

**Qué es.** Una empresa que se dedica a que los mails automáticos lleguen a la bandeja de entrada
y no a spam. **Brevo** y **Resend** tienen plan gratuito de sobra para este sistema.

**Por qué no alcanza con Gmail.** Una cuenta personal tiene límites diarios bajos, y los
proveedores desconfían de un servidor desconocido que manda mails en nombre de un dominio. El
resultado es que la contraseña de un colaborador nuevo cae en spam y esa persona no puede entrar.

**Cómo se integra.** Te dan un usuario y una contraseña SMTP: son exactamente las variables
`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD` que la aplicación ya usa. Además hay
que agregar al DNS tres registros que ellos te dictan:

- **SPF** declara qué servidores tienen permiso de mandar mail en nombre de tu dominio.
- **DKIM** firma cada mail, para que el que lo recibe pueda comprobar que no fue alterado.
- **DMARC** le dice al que recibe qué hacer si SPF o DKIM fallan.

Sin esos tres, los mails llegan a spam aunque el envío funcione.

### Turnstile — el captcha invisible

**Qué es.** El captcha de Cloudflare. A diferencia de los de Google, no te hace señalar
semáforos: en la mayoría de los casos el visitante no ve nada, y el sitio igual comprueba que hay
una persona del otro lado.

**Para qué sirve acá.** El formulario de acceso es público. Hoy tiene un tope de 3 por hora por
IP, que frena a un curioso pero no a un bot con direcciones rotativas. Sin captcha, alguien puede
llenar de basura la bandeja de solicitudes de Arlette.

**Cómo se integra.** Un componente en el formulario que genera un código, y una verificación en
la API contra Cloudflare antes de guardar la solicitud. Es media jornada de trabajo, y necesita
que la cuenta de Cloudflare exista primero. **Está pendiente** (hallazgo M10 del plan de
seguridad).

### El chequeo externo de salud

**Qué es.** Un servicio que le pega a tu sitio cada minuto desde afuera y te avisa si no
responde. **UptimeRobot** o **Better Stack**, gratis para un sitio.

**Por qué desde afuera.** Una alerta que corre en el mismo servidor que se cayó no te va a avisar
nada. Este es el chequeo que cubre el susto más común: que el sitio esté caído y no lo sepas
hasta que alguien te escriba.

---

## El orden real de trabajo

`DEPLOY.md` tiene once fases. Este es el orden en el que conviene ejecutarlas, cruzado con el eje
de seguridad.

**Etapa 0 — sin gastar un peso, en tu máquina**

1. `git init` con el `.gitignore` puesto **antes** del primer commit. Hay dos archivos con
   secretos en la raíz (ver M11 del plan de seguridad).
2. Los cuatro bloqueantes de `DEPLOY.md` fase 00: Actuator, admin inicial, `VITE_API_URL`, y el
   repo.
3. Los arreglos de seguridad de código: sanitización de HTML, límites de tamaño, logs (C1, C2,
   C3, A8).
4. Escribir los Dockerfiles y el compose (fases 03–05) y **levantar todo el sistema en tu máquina
   con el mismo compose de producción**. Este ensayo es el que evita el 90% de los sustos del día
   del deploy.

**Etapa 1 — contratar** (todo lo de la sección anterior)

5. Comprar el dominio (o pedirle a Arlette el subdominio).
6. Contratar el VPS en Hetzner.
7. Crear la cuenta de **Cloudflare** y apuntar los nameservers.
8. Crear la cuenta del servicio de **correo** y configurar SPF, DKIM y DMARC.
9. Crear la cuenta del **chequeo de salud**.

**Etapa 2 — el servidor**

10. Endurecerlo: usuario sin privilegios, SSH sólo con clave, firewall cerrado salvo 22/80/443,
    `unattended-upgrades` y **fail2ban**.
11. Instalar Docker, copiar el compose y el `.env` con permisos `600`.
12. Levantar, sacar el certificado TLS y poner la Content-Security-Policy en el nginx del front.
13. Activar el paso por Cloudflare y **recién ahí** poner `TRUST_PROXY=true`, con el nginx
    copiando `CF-Connecting-IP` a `X-Forwarded-For`. En ese orden: al revés, el bloqueo por
    intentos fallidos se vuelve evadible.

**Etapa 3 — antes de contarle a nadie**

14. Turnstile en el formulario público.
15. La lista de verificación de `DEPLOY.md` fase 11, entera, sin saltear.
16. Backup automático **y una restauración de prueba**. Un backup que nunca se restauró es una
    hipótesis.
17. El chequeo externo de salud, avisando a tu teléfono.

Recién ahí se le pasa la dirección a Arlette.

---

## Cuánto mantenimiento pide el servidor después

La respuesta corta: **la mayoría de los meses, cero. En promedio, media hora por mes, más media
jornada una vez al año.** La respuesta larga importa porque el riesgo no está donde uno cree.

### Lo que se hace solo

Configurado una vez en la etapa 2, no se vuelve a tocar:

| | Qué hace |
|---|---|
| `unattended-upgrades` | Instala los parches de seguridad del sistema operativo |
| Renovación de TLS | Caddy o certbot renuevan el certificado antes de que venza |
| `restart: unless-stopped` | Los contenedores vuelven solos tras un crash o un reinicio |
| `cron` del backup | El `pg_dump` diario a un destino externo |
| fail2ban | Bloquea a los insistentes sin que nadie mire |
| Cloudflare | No pide nada nunca |

Con eso el servidor se sostiene solo durante meses. Lo que sigue es lo que **sí** necesita una
persona.

### Cada mes · unos 15 minutos

- **Reiniciar para que los parches de kernel tomen efecto.** `unattended-upgrades` instala pero
  no reinicia, salvo que se lo pidas. Conviene activarle el reinicio automático a una hora de
  madrugada y olvidarse. *(Alternativa: Ubuntu Pro es gratis para hasta 5 máquinas e incluye
  Livepatch, que parchea el kernel sin reiniciar.)*
- **Reconstruir las imágenes de Docker.** Este es el punto que casi todo el mundo pasa por alto:
  `unattended-upgrades` actualiza el sistema del servidor, **no lo que está adentro de los
  contenedores**. Las librerías del JRE, de nginx y de Postgres se actualizan al reconstruir la
  imagen desde su base. Con el pipeline de CI armado es apretar un botón.
- **Mirar el disco.** Entre logs, backups y la base, llenar el disco es la forma más común de que
  un VPS chico se caiga.

### Cada tres meses · una o dos horas

- **Probar una restauración del backup.** Es lo más importante de toda esta lista y lo primero que
  se deja de hacer. Un backup que nunca se restauró no es un backup, es una suposición.
- **Revisar y aplicar las actualizaciones de dependencias** que juntó Dependabot: subir Spring,
  React y compañía, reconstruir y desplegar.

### Cada año o dos · media jornada

- **Actualizar el sistema operativo a la siguiente versión LTS.** Es el trabajo más grande de todo
  el mantenimiento, y aun así son un par de horas si el servidor está limpio. Alternativa muchas
  veces más rápida: levantar un servidor nuevo desde cero con el compose, mover el DNS y apagar el
  viejo.
- **Renovar el dominio** (se puede dejar en automático).

### Cada varios años

- **Subir Postgres de versión mayor** (16 → 17 → 18). No alcanza con cambiar la imagen: hay que
  volcar y restaurar los datos. Un rato de trabajo, y es la única tarea que pide un corte de
  servicio planificado.

### El riesgo real no es el esfuerzo, es el olvido

Un servidor personal casi nunca falla por falta de trabajo. Falla así: nadie lo mira durante ocho
meses, el disco se llena, el backup dejó de correr en silencio hace tres meses, y hay una
vulnerabilidad conocida sin parchear. **Todo eso pasa sin que nada se rompa visiblemente**, hasta
el día que sí.

Por eso lo que de verdad reduce el mantenimiento no es la disciplina, son **las alertas al
teléfono**:

- el sitio no responde (chequeo externo cada minuto);
- disco por encima del 80%;
- **el backup falló** — y también **no hubo backup en 48 horas**, que es un aviso distinto: si el
  cron dejó de correr, nunca va a llegar un aviso de fallo.

Con esas tres, el servidor te avisa cuando te necesita y podés no pensar en él el resto del tiempo.

### Cuánto de esto se ahorra con un PaaS

| Tarea | VPS | PaaS |
|---|---|---|
| Parches del sistema operativo | Vos (automatizable) | Ellos |
| Reinicios por kernel | Vos (automatizable) | Ellos |
| Actualizar el sistema a la siguiente LTS | Vos, cada 2 años | Ellos |
| Backups y su restauración de prueba | Vos | Ellos hacen el backup; **probar el restore sigue siendo tuyo** |
| Reconstruir imágenes por parches de librerías | Vos | Vos |
| Actualizar las dependencias del proyecto | Vos | Vos |
| Subir Postgres de versión mayor | Vos | Ellos (a veces obligándote a una fecha) |
| Vigilar disco y salud | Vos | Ellos |

Un PaaS se lleva **la mitad de la lista mensual y casi toda la anual**, por unos 20 dólares más
por mes. Lo que no se lleva: mantener al día tus propias dependencias y comprobar que los backups
sirven.

Puesto en horas: el VPS son unas **6 a 10 horas al año**; el PaaS, unas **3 o 4**. La diferencia
es real pero chica, y a cambio el VPS te deja entender la máquina el día que algo falla.

---

## Qué esperar

Algunas cosas que conviene saber de antemano, para que no sorprendan:

- **El día del deploy nada anda a la primera.** Es lo normal, no es señal de que algo esté mal
  hecho. Por eso el ensayo local de la etapa 0.
- **El DNS tarda.** Un cambio puede tardar minutos u horas en verse en todos lados. No es que
  esté roto.
- **El primer certificado TLS puede fallar** si el DNS todavía no propagó. Se reintenta y ya.
- **El correo es lo que más problemas da.** Contá con una tarde entera de ajustar SPF, DKIM y
  mandarte mails de prueba a Gmail, Outlook y algún corporativo.
- **Costo total estimado:** unos 5 euros de VPS, 1 dólar de dominio prorrateado, y 0 de Cloudflare
  y correo en los planes gratuitos. **Menos de 10 dólares al mes.**

---

## Dónde puedo ayudar y dónde no

**Puedo hacer yo:** todo el código y los archivos de configuración —Dockerfiles, compose, nginx,
migraciones, el admin inicial, los arreglos de seguridad, el pipeline de CI—, y el ensayo local
completo.

**Tenés que hacer vos:** contratar el VPS y el dominio, crear las cuentas, guardar las
credenciales, y ejecutar los comandos en el servidor. Puedo escribirte cada comando y explicarte
qué hace, pero el acceso al servidor y a la tarjeta son tuyos.

**Sugerencia:** cuando llegue el momento, hagamos la etapa 2 juntos en una sesión, comando por
comando, en lugar de que yo te pase un script largo. Vas a querer entender ese servidor el día
que algo falle.
