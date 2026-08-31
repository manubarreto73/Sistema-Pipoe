# Deploy de Pipoe

Todo lo que sólo existe en el servidor. En la raíz del repo queda el `docker-compose.yml` de
desarrollo, que no tiene TLS ni publica el 443.

| Archivo | Para qué |
|---|---|
| `hardening.sh` | Asegura el VPS recién creado. Se corre una vez, como root. |
| `cerrar-acceso-root.sh` | Cierra el acceso por contraseña. **Irreversible desde afuera.** |
| `generar-env.sh` | Crea el `.env` con los secretos generados en el servidor. |
| `docker-compose.prod.yml` | El stack: base, Redis, API, nginx con TLS y certbot. |
| `nginx/templates/` | La configuración de nginx. Va montada, no horneada en la imagen. |
| `init-certbot.sh` | Emite el primer certificado de Let's Encrypt. |
| `backup-db.sh` | Copia nocturna de la base. Va en cron. |
| `.env.prod.example` | Plantilla de variables. El `.env` real no se versiona. |

---

## Antes de tocar el servidor

**1. El DNS tiene que estar apuntando.** Un registro `A` con el subdominio y la IP del VPS.
El campo *Nombre* lleva sólo el subdominio, sin el dominio detrás: los paneles lo agregan
solos y escribirlo entero produce `pipoe.dominio.com.dominio.com`.

Si el DNS está en Cloudflare, tiene que quedar en **"DNS only"** (nube gris). Con el proxy
activado, Cloudflare termina el TLS por su cuenta y rompe la validación de certbot.

```bash
nslookup modelopipoe.com 8.8.8.8
```

**Cargar en la misma pasada los registros de correo.** Brevo pide tres registros TXT/CNAME
sobre el mismo dominio para poder enviar las contraseñas (ver [docs/correo.md](../docs/correo.md)).
Es el mismo panel: haciéndolo junto con el registro `A` se espera una sola propagación en vez
de dos.

**2. Una clave SSH cargada en el VPS.** El panel de Hostinger la pide al crear la máquina.
Sin eso, `hardening.sh` se niega a seguir: cerrar el acceso por contraseña sin una clave que
funcione deja el servidor inalcanzable.

---

## Orden de ejecución

### 1. Endurecer el servidor

Los scripts van por `scp` porque el repositorio todavía no se puede clonar: la deploy key se
configura después.

```bash
# Desde tu máquina, NO desde dentro de la sesión SSH
scp deploy/hardening.sh deploy/cerrar-acceso-root.sh root@IP:/root/

ssh root@IP
bash hardening.sh pipoe pipoe-prod
```

Instala Docker, crea el usuario, arma el firewall, deja fail2ban andando y **agrega 4 GB de
swap**, que en esta máquina no es opcional: con 4 GB de RAM y las imágenes construyéndose
acá mismo, sin swap el kernel mata Postgres a mitad de un build.

**Sin cerrar esa sesión**, abrir otra terminal y confirmar que entrás:

```bash
ssh pipoe@IP
```

Recién cuando eso funcione:

```bash
sudo bash /root/cerrar-acceso-root.sh
```

Y volver a verificar desde otra terminal antes de cerrar todo. Si la clave del usuario nuevo
no quedó bien y se cierra la única sesión abierta, la única salida es la consola de
recuperación de Hostinger.

### 2. Traer el código

Como `pipoe`, no como root: la clave tiene que quedar en su home.

```bash
ssh-keygen -t ed25519 -C "deploy-vps-pipoe" -f ~/.ssh/github
cat ~/.ssh/github.pub
```

Passphrase vacía: la usa el servidor sin nadie adelante, y con passphrase cada `git pull`
quedaría esperando una respuesta que nunca llega.

Esa clave pública va al repositorio en **Settings → Deploy keys → Add deploy key**, con
**"Allow write access" desmarcado**. Deploy key y no token: es de sólo lectura, no vence y
está limitada a este repositorio.

```bash
cat >> ~/.ssh/config <<'EOF'
Host github.com
    IdentityFile ~/.ssh/github
    IdentitiesOnly yes
EOF
chmod 600 ~/.ssh/config

ssh -T git@github.com
git clone git@github.com:manubarreto73/Sistema-Pipoe.git pipoe
```

### 3. Variables y primer arranque

```bash
cd ~/pipoe/deploy
bash generar-env.sh
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

El primer build baja Maven Central entero y el árbol de npm: entre **15 y 25 minutos** en un
núcleo. Los siguientes reusan esas capas mientras no cambien `pom.xml` ni `package*.json`.

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
```

`db`, `redis` y `api` en `Up`. **`web` va a estar reiniciándose y está bien**: nginx no
arranca sin certificado. Se resuelve en el paso siguiente.

Antes de seguir, confirmar que la API arrancó limpia y aplicó las migraciones:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs api | tail -40
```

### 4. Certificado

```bash
bash init-certbot.sh
```

Verifica que el desafío se lea desde internet **antes** de pedir el certificado. Let's Encrypt
permite 5 validaciones fallidas por hora y por dominio; si algo está mal, el script corta sin
gastar ninguna.

### 5. Verificar desde afuera

Desde tu máquina, no desde el servidor:

```bash
D=modelopipoe.com
curl -s -o /dev/null -w "80  -> %{http_code} -> %{redirect_url}\n" "http://$D/"
curl -s -o /dev/null -w "443 -> %{http_code}\n" "https://$D/"
echo | openssl s_client -connect $D:443 -servername $D 2>/dev/null | openssl x509 -noout -issuer -dates
curl -s -o /dev/null -w "ruta del router -> %{http_code}\n" "https://$D/proyectos"
curl -s -o /dev/null -w "api sin token   -> %{http_code}\n" "https://$D/api/proyectos"
curl -s    -w "\nhealth -> %{http_code}\n" "https://$D/actuator/health"
curl -s -o /dev/null -w "www    -> %{http_code} -> %{redirect_url}\n" "https://www.$D/"
```

Esperado: `301` al HTTPS, `200`, certificado de Let's Encrypt, `200` en la ruta del router
(el fallback del SPA), **`401`** en la API sin token, `200` con `"status":"UP"` en el health y
`301` de `www` al dominio pelado.

El `401` es el más informativo: prueba que nginx llega hasta la API. Si no la alcanzara,
verías `502`.

### 6. Probar que el correo sale

**Este paso no se puede saltear.** Las contraseñas las genera el sistema y viajan sólo por
correo: no hay recuperación de clave ni forma de que nadie vea la de otro. Si el correo no
sale, el único que puede entrar es el administrador inicial, y no puede dar de alta a nadie.

Entrando como administrador, crear un usuario de prueba con una casilla propia. Tiene que
llegar el mensaje con la contraseña. Si no llega:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs api | grep -i mail
```

El alta va dentro de la misma transacción que el envío, así que un fallo del SMTP **deshace la
creación**: no quedan cuentas huérfanas con una clave que nadie conoce, y el error se ve en
pantalla. Lo que sí puede pasar es que el proveedor acepte el mensaje y el destinatario nunca
lo reciba, así que hay que mirar también la carpeta de correo no deseado.

`/actuator/health` no cubre esto a propósito: sin correo el sistema sigue sirviendo todo salvo
el alta de personas, y una caída del SMTP no debería marcar la API entera como caída.

### 7. Backups en cron

Probarlo a mano antes de confiar en él:

```bash
bash backup-db.sh
```

```bash
crontab -e
```

```
0 3 * * * /home/pipoe/pipoe/deploy/backup-db.sh >> /var/log/backup-pipoe.log 2>&1
```

Y probar una restauración, que es la mitad que nadie prueba:

```bash
gunzip -c /var/backups/pipoe/pipoe_XXXX.sql.gz | \
  docker compose -f docker-compose.prod.yml --env-file .env exec -T db psql -U pipoe -d pipoe
```

---

## Operación

**Actualizar a una versión nueva.** Con 4 GB y un núcleo no entra el stack corriendo *más* un
build, así que la receta baja todo primero. Es una ventana de mantenimiento en horario sin uso:

```bash
cd ~/pipoe && git pull
cd deploy
docker compose -f docker-compose.prod.yml --env-file .env down   # los volúmenes no se tocan
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker image prune -f
```

Las dos reglas que no hay que saltear: **bajar el stack antes** y no construir las dos
imágenes en paralelo. Maven y Vite compilando a la vez son los dos procesos más hambrientos
de todo el sistema.

Flyway aplica solo las migraciones nuevas al arrancar.

**Cambiar la configuración de nginx** no requiere reconstruir: está montada.

```bash
docker compose -f docker-compose.prod.yml --env-file .env restart web
```

**Cambiar variables de entorno:** editar el `.env` y `up -d api`. Compose recrea sólo ese
contenedor; la base y Redis no se tocan.

**Logs:** `docker compose -f docker-compose.prod.yml --env-file .env logs -f api`

**Recursos:** `docker stats --no-stream`

---

## Lo que hay que saber y no es evidente

**El esquema de la base ya no es descartable.** Flyway registra el checksum de cada migración
aplicada y se niega a arrancar si cambia. De acá en más, todo cambio de modelo es una
migración nueva; editar una vieja rompe el arranque.

**`DB_PASSWORD` queda grabada en el volumen de Postgres** en el primer arranque. Cambiarla
después en el `.env` no cambia la de la base y la API deja de conectarse.

**La pertenencia al grupo `docker` se evalúa al iniciar sesión.** Después de que
`hardening.sh` agregue el usuario, hay que reconectarse o los comandos de Docker van a dar
permiso denegado.

**El usuario de despliegue no tiene contraseña y usa `sudo` sin que se la pidan.** Se entra
sólo con clave SSH. Si `sudo` alguna vez pide una contraseña, es que falta
`/etc/sudoers.d/90-<usuario>`; se recupera entrando como root por `su -` o por la consola del
panel —ninguna de las dos pasa por SSH, así que el bloqueo de root no las afecta.

**Postgres y Redis no publican puertos.** Publicar el 5432 al host lo expone a internet:
Docker escribe sus propias reglas de iptables y `ufw` no las filtra.

**`up -d web` no recrea un contenedor que ya existe.** Si nginx quedó en bucle de reinicio,
hace falta `--force-recreate`. Por eso `init-certbot.sh` lo usa.

**Un `.sh` con finales de línea de Windows** falla con `bad interpreter: /bin/bash^M`. El
`.gitattributes` de la raíz ya fuerza LF en `*.sh`, `*.yml`, `*.conf` y los Dockerfile.

**`scp` se corre en tu máquina, no dentro de la sesión SSH.** El error que da cuando se
confunde ("no such file") no lo sugiere para nada.

**Rotar cualquier secreto que haya estado en el historial de git.** Los `.env` nunca se
versionaron, pero si alguno se coló alguna vez, cambiarlo.
