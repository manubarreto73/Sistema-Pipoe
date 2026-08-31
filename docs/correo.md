# El correo del sistema

El correo no es un accesorio de Pipoe: **es el único canal por el que se entregan las
contraseñas**. Las genera el sistema, nadie las elige y nadie puede verlas —ni siquiera desde la
cuenta de administración—. No hay "olvidé mi clave". Si el correo no sale, las altas de usuarios
y de colaboradores dejan de funcionar y sólo puede entrar quien ya tenga su clave.

El envío está dentro de la misma transacción que el alta, así que si el SMTP falla **se deshace
la creación de la cuenta**. Es a propósito: es preferible un error en pantalla antes que una
cuenta viva con una contraseña que nadie conoce.

---

## Las tres direcciones que no hay que confundir

Es el punto donde se traba todo el mundo, porque son tres cosas distintas y dos de ellas se
parecen.

| | Qué es | Cuál va | ¿La elegís vos? |
|---|---|---|---|
| **Cuenta de Brevo** | Con qué correo entrás al panel de Brevo | Tu Gmail personal | Sí, cualquiera |
| **Remitente** (`MAIL_FROM`) | Lo que ve quien recibe el mensaje | `no-reply@modelopipoe.com` | Sí, elegís la parte de la izquierda |
| **Usuario SMTP** (`MAIL_USERNAME`) | Con qué se autentica la aplicación contra Brevo | Algo como `9a1b2c001@smtp-brevo.com` | No, lo genera Brevo |

El error clásico es poner el remitente en `MAIL_USERNAME`. No son lo mismo: el usuario SMTP es
una credencial que Brevo inventa y que nadie ve nunca; el remitente es el nombre con el que sale
el mensaje.

### El remitente no se carga en Brevo

No hay ninguna pantalla donde se elija. El remitente lo pone **la aplicación** en cada mensaje,
desde `MAIL_FROM`. Lo que hace Brevo es otra cosa: al autenticar el dominio entero, autoriza
*cualquier* dirección `@modelopipoe.com`. Por eso conviene autenticar el dominio y no una sola
dirección — mañana sirve `hola@modelopipoe.com` sin tocar nada.

Si algún envío fallara diciendo que el remitente no está autorizado, se agrega
`no-reply@modelopipoe.com` en la lista de *Senders* del panel. Con el dominio ya autenticado no
pide confirmar ningún enlace.

### El remitente no necesita ser una casilla que exista

`no-reply@modelopipoe.com` no tiene que ser un buzón real. Es una cabecera del mensaje, y lo que
la hace legítima ante Gmail o Outlook no es que haya un buzón detrás, sino **los registros DNS de
modelopipoe.com** que dicen "Brevo tiene permiso para enviar en mi nombre".

La contra de no tener buzón es que si alguien responde, la respuesta se pierde. Para mensajes de
"esta es tu contraseña" es lo habitual y por eso se llaman `no-reply`. Si más adelante querés una
dirección de contacto que reciba de verdad, se agrega aparte y no cambia nada de esto.

---

## Por qué el remitente NO puede ser un Gmail

Aunque crearas `modelopipoe@gmail.com`, no serviría como remitente:

- **No podés agregar registros DNS en `gmail.com`.** Es de Google. Y la autenticación del
  remitente se hace justamente con registros DNS en el dominio que envía. Sin eso, Brevo no
  puede firmar los mensajes como tuyos.
- **Brevo no deja autenticar dominios de correo gratuito**, por ese mismo motivo.
- Un mensaje que dice venir de `@gmail.com` pero sale de los servidores de Brevo es exactamente
  el patrón de una suplantación. Los filtros lo tratan como tal.

Con `modelopipoe.com` no hay ninguno de esos problemas: el dominio es tuyo y podés agregarle los
registros que Brevo pida.

---

## Alta en Brevo

Los nombres exactos de los menús cambian cada tanto; el orden de los pasos no.

1. **Crear la cuenta** en brevo.com con tu correo personal y confirmarla. El plan gratuito da
   300 envíos por día, de sobra: Pipoe manda un correo por alta de persona, no por uso.
2. **Autenticar el dominio.** En el panel, la sección de remitentes y dominios
   (*Senders, Domains & Dedicated IPs* → *Domains*), agregar `modelopipoe.com`.
3. Brevo devuelve **los registros DNS a cargar** (ver abajo).
4. **Cargarlos en el panel donde compraste el dominio.** Si es Hostinger:
   hPanel → *Dominios* → *DNS / Nameservers*.
5. Volver a Brevo y pedir la verificación. Suele tardar minutos; el DNS puede tardar horas.
6. **Sacar las credenciales SMTP** en *SMTP & API* → pestaña *SMTP*. De ahí salen el servidor,
   el puerto, el usuario y la clave.

### Los registros DNS

Los valores exactos los da Brevo y son distintos para cada cuenta: **hay que copiar los que
muestre el panel**, no inventarlos. Suelen ser cuatro, y se reconocen por el nombre y no por el
tipo, porque hay dos de cada uno:

| Nombre | Tipo | Para qué |
|---|---|---|
| `@`, con valor `brevo-code:…` | `TXT` | Prueba que el dominio es tuyo |
| `brevo1._domainkey` | `CNAME` | Firma de cada mensaje (DKIM) |
| `brevo2._domainkey` | `CNAME` | La segunda clave de firma, para poder rotarlas |
| `_dmarc`, con valor `v=DMARC1;…` | `TXT` | Qué debe hacer el receptor con un mensaje que no pase las comprobaciones |

**Los dos DKIM no son opcionales ni alternativos**: van los dos. Brevo firma con una y mantiene
la otra para poder cambiar de clave sin cortar los envíos.

**No hay registro SPF, y no falta.** Brevo autentica por DKIM: firma con una clave que vive en
`modelopipoe.com`, y con eso el DMARC alinea sin necesidad de que el dominio autorice servidores
por IP. Sólo hay que cargar un SPF si el panel de Brevo lo pide expresamente. Inventar uno por
las dudas es peor que no tenerlo.

Si el panel sí lo pidiera y el dominio ya tuviera otro, se **edita** el existente: un dominio
puede tener un solo SPF y dos se invalidan entre sí, sin ningún error visible.

**Quien manda es el botón de verificar de Brevo**, no esta tabla. Si el panel da el dominio por
autenticado, está listo.

#### El campo "Nombre" lleva sólo la parte de la izquierda

Es el error que más tiempo hace perder. En el panel de Hostinger, el campo *Nombre* (o *Host*)
lleva `brevo1._domainkey` **a secas**: el panel le agrega `.modelopipoe.com` solo. Escribiendo
`brevo1._domainkey.modelopipoe.com` queda
`brevo1._domainkey.modelopipoe.com.modelopipoe.com`, que no valida nunca y no avisa por qué.

Para los registros que van sobre el dominio pelado, el nombre es `@`.

> El registro `A` de la aplicación se carga en este mismo panel. Un dominio recién comprado ya
> trae uno apuntando a la página de cortesía de Hostinger: cuando exista el VPS **se edita ese**,
> no se agrega otro. Dos registros `A` sobre el mismo nombre reparten las visitas entre las dos
> IPs y la mitad de la gente ve la página equivocada.

---

## Configurar la aplicación

Cinco variables, todas en el `.env`. **Nunca van al repositorio** — `.env` está en el
`.gitignore`, y la clave SMTP es una credencial como cualquier otra.

```bash
MAIL_HOST=smtp-relay.brevo.com     # el que muestre el panel de Brevo
MAIL_PORT=587
MAIL_USERNAME=<el usuario que da Brevo, tipo 9a1b2c001@smtp-brevo.com>
MAIL_PASSWORD=<la clave SMTP de Brevo>
MAIL_FROM=no-reply@modelopipoe.com
```

La pantalla *SMTP & API* → *SMTP* de Brevo muestra tres datos, y sus nombres no coinciden con
los de las variables:

| Lo que dice Brevo | Va en |
|---|---|
| Servidor / *SMTP server* | `MAIL_HOST` |
| Puerto | `MAIL_PORT` |
| **Iniciar sesión** / *Login* | `MAIL_USERNAME` |
| La clave SMTP que se genera aparte | `MAIL_PASSWORD` |

"Iniciar sesión" es una traducción de *Login* y despista: no es una acción, es el valor del
usuario. Se copia tal cual aparece.

La clave se muestra **una sola vez**, cuando se crea. Si no quedó guardada no se puede volver a
ver: se genera otra y listo.

`MAIL_FROM` es lo único que elegís vos. Los otros cuatro salen del panel de Brevo — **no de
Hostinger**, que sólo administra el dominio y sus registros DNS.

La clave SMTP es una credencial viva: quien la tenga puede mandar correo firmado como
`@modelopipoe.com`. No se pega en un chat, no se manda por mensaje y no se versiona. Vive en el
`.env`, que está en el `.gitignore`, y en ningún otro lado. Si alguna vez se filtra, se revoca y
se genera otra desde el mismo panel.

---

## Probarlo en la máquina local

En el día a día, el entorno local manda todo a **Mailpit**, una bandeja falsa que muestra los
mensajes sin entregarlos. Eso lo hace `docker-compose.dev.yml`, que fija `MAIL_HOST: mailpit`
por encima de lo que diga el `.env`.

Para probar Brevo de verdad hay que levantar **sin ese archivo**:

```bash
# El down lleva los dos archivos, o Mailpit queda corriendo suelto
docker compose -f docker-compose.yml -f docker-compose.dev.yml down

# cargar las cinco variables de arriba en .env, y despues:
docker compose up -d            # OJO: sin  -f docker-compose.dev.yml
```

Después, entrando como administrador, dar de alta un usuario con **una casilla tuya** y esperar
el mensaje. Si llega, el circuito completo funciona.

Para volver a la bandeja falsa:

```bash
docker compose down
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

Se nota en qué modo está mirando a dónde apunta la API:

```bash
docker compose exec api sh -c 'echo $MAIL_HOST'
```

`mailpit` es la bandeja falsa; cualquier otra cosa es envío real.

Un detalle que confunde: mientras el dominio todavía no esté verificado, el mensaje puede salir
igual y caer en correo no deseado. Eso no es una falla de la aplicación, es la falta de los
registros DNS — y es justamente lo que esos registros arreglan.

---

## Si no llega

```bash
docker compose logs api | grep -i mail
```

- **`AuthenticationFailedException`** → usuario o clave SMTP mal copiados. Acordate de que el
  usuario NO es `no-reply@modelopipoe.com`.
- **El alta devuelve error y el usuario no se crea** → es el comportamiento correcto: el envío
  falló y la transacción se deshizo. El problema está en el SMTP, no en el alta.
- **Brevo dice "enviado" y no llega** → mirar correo no deseado, y revisar en el panel de Brevo
  el registro de envíos, que dice si el destinatario lo rechazó.
- **Sale pero cae siempre en spam** → falta autenticar el dominio, o el SPF quedó duplicado.

`/actuator/health` **no** comprueba el correo, a propósito: sin correo el sistema sigue sirviendo
todo salvo el alta de personas, y una caída del proveedor no debería marcar la API entera como
caída.
