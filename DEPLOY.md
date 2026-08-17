# Pipoe en producción

Plan de puesta en producción: qué hay que hacer, en qué orden y por qué, para llevar la API
y el front desde `localhost` hasta un dominio con TLS.

Once fases. Las tres primeras son trabajo de código que hoy falta; el resto es empaquetado,
infraestructura y operación.

> Versión navegable de este documento: https://claude.ai/code/artifact/1a2517f3-3062-4e16-b34f-262ff769c8c5

**Stack:** Spring Boot 4.0.6 / Java 17 · React 19 + Vite (SPA) · PostgreSQL + Flyway · Redis ·
JWT con refresh rotativo · SMTP

**Orden sugerido de trabajo:** las fases 00 a 02 son código y se pueden hacer ya, sin esperar
a terminar las features. Las fases 03 a 05 se prueban enteras en tu máquina — `docker compose up`
local con el mismo compose de producción es el mejor ensayo posible. Recién ahí conviene
contratar el servidor y el dominio.

---

## 00 · Bloqueantes

Cuatro cosas que hoy impiden un deploy. No son mejoras: sin resolverlas el sistema no arranca,
no se puede usar o no se puede versionar.

| | Problema | Por qué bloquea |
|---|---|---|
| **Sin control de versiones** | El proyecto no es un repo git | No hay historial, ni rollback, ni forma de construir una imagen reproducible o de conectar CI. Todo lo demás depende de esto. |
| **Nadie puede entrar** | No existe el primer usuario ADMIN | `POST /api/auth/register` pide `hasRole('ADMIN')` y ninguna migración inserta un admin. En una base nueva no hay con qué loguearse. |
| **Sin sonda de salud** | Falta Actuator | Sin `/actuator/health` no hay healthcheck de contenedor, ni `depends_on: healthy`, ni readiness para el proxy o el CI. |
| **Config atada al build** | `VITE_API_URL` se hornea en el bundle | Vite reemplaza `import.meta.env` en tiempo de compilación. No es una variable de runtime: define a qué API apunta la imagen del front. |

---

## 01 · Higiene del repo

El repo define qué entra en la imagen. Si arranca sucio, arrastra basura y credenciales a
producción.

```bash
git init
git add .
git commit -m "Estado inicial: API + front"
```

### Qué excluir

Antes del primer `add`, un `.gitignore` en la raíz:

```gitignore
front/node_modules/
front/dist/
front/.env
api/target/
target/           # residuo de un build en la raíz, se puede borrar
*.log
.idea/
.vscode/
```

> ⚠️ **Cuidado.** Hoy existe `front/.env` con contenido real. Si entra al primer commit, queda
> en el historial aunque después lo borres. Verificá que el ignore esté puesto *antes* de
> commitear. `front/.env.example` sí va versionado.

### Además

- Un `README` corto con cómo levantar el proyecto: dentro de tres meses vas a ser vos el que
  lo lea.
- Rama `main` protegida si vas a trabajar con más gente.
- `spring-boot-devtools` no se activa cuando la app corre desde el jar empaquetado, así que no
  molesta en producción. Igual podés excluirlo del repackage para achicar la imagen.

---

## 02 · Cambios de código

Cuatro cambios chicos en la API, todos por el mismo motivo: va a correr detrás de un proxy, en
una red de contenedores, sin nadie que la mire arrancar.

### Actuator y su ruta pública

Agregá la dependencia y abrí *solo* el health en `SecurityConfig`. Hoy
`anyRequest().authenticated()` dejaría el healthcheck devolviendo 401 y Docker marcaría el
contenedor como enfermo para siempre.

```xml
<!-- api/pom.xml -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```java
// SecurityConfig.java, dentro de authorizeHttpRequests
.requestMatchers("/actuator/health/**").permitAll()
```

Y en `application.yml`, exponer únicamente lo necesario:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
```

### Password de Redis

La config actual solo tiene host y puerto. En producción Redis va con `requirepass`, así que la
app necesita poder mandarlo:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

### Headers del proxy

Detrás de nginx, Spring ve el request como HTTP y con la IP del proxy. Esto le dice que confíe
en los headers `X-Forwarded-*`:

```yaml
server:
  port: 8080
  forward-headers-strategy: framework
```

> ⚠️ **Agujero de rate limiting.** `RequestUtils.clientIp()` toma el primer valor de
> `X-Forwarded-For`. Ese header lo puede mandar el cliente. Si el proxy del borde hace
> `proxy_add_x_forwarded_for`, el valor del atacante queda primero y puede rotar su "IP" en cada
> intento, evadiendo el bloqueo de login y el tope de solicitudes de acceso. En el nginx del
> borde usá `proxy_set_header X-Forwarded-For $remote_addr;` — pisa lo que venga de afuera con
> la IP real de la conexión.

### El primer administrador

Elegí uno de los dos caminos:

- **Migración Flyway** (`V7__seed_admin.sql`) con el hash BCrypt ya generado y una password que
  se cambia en el primer login. Simple y reproducible; el hash queda en el repo, así que tiene
  que ser una password descartable.
- **Runner de arranque** detrás de un perfil o de una variable `ADMIN_BOOTSTRAP_PASSWORD`, que
  cree el admin solo si la tabla está vacía. Nada sensible en el repo; es el recomendado.

### URL de la API relativa en el front

Si servís front y API bajo el mismo dominio (fase 06), `VITE_API_URL` pasa a ser string vacío y
todas las llamadas quedan relativas: `/api/auth/login`. El código ya lo soporta —
`front/src/lib/http.ts` concatena `BASE_URL + path` y `path` ya arranca con `/api`. Ganás dos
cosas: CORS deja de existir y la misma imagen del front sirve para cualquier ambiente.

---

## 03 · Imagen de la API

Build en dos etapas: Maven compila y corre los tests, y la imagen final solo lleva el JRE y el
jar. Pasa de ~500 MB a ~180 MB y no deja el toolchain de build expuesto.

**`api/Dockerfile`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Copiar el pom solo primero: si no cambió, Docker reusa la capa
# de dependencias y no vuelve a bajar medio Maven Central.
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# Usuario sin privilegios: si alguien escapa del proceso, no es root.
RUN useradd --system --uid 1001 pipoe
COPY --from=build /build/target/*.jar app.jar
USER pipoe
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

**`api/.dockerignore`**

```
target/
.git/
*.md
.idea/
```

> **Memoria.** `MaxRAMPercentage=75` hace que la JVM respete el límite del contenedor. Sin eso,
> en un servidor de 2 GB la JVM calcula su heap sobre la RAM del host y el kernel termina
> matando el proceso con OOM cuando hay carga.

### Verificación

Antes de seguir: `docker build -t pipoe-api ./api` y correr el contenedor con las variables
mínimas apuntando a un Postgres local. Si arranca, Flyway migró y `/actuator/health` responde
`UP`, la imagen está lista.

---

## 04 · Imagen del front

El front compilado son archivos estáticos. Node se usa para construir y después desaparece: la
imagen final es nginx con la carpeta `dist`.

**`front/Dockerfile`**

```dockerfile
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
# Vacío = rutas relativas contra el mismo origen (ver fase 06).
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginx:alpine AS runtime
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

> ⚠️ **La trampa clásica.** `VITE_API_URL` tiene que estar presente en el *build*, no en el
> `docker run`. Si la pasás como variable de entorno del contenedor no pasa nada: el bundle ya
> se generó con el valor viejo — o con el literal `undefined`, que produce requests a
> `undefined/api/auth/login`. Es un error que solo se ve en el navegador, no en los logs.

### Configuración de nginx

Dos cosas importan acá. El `try_files` con fallback a `index.html`: sin eso, apretar F5 en
`/proyectos` devuelve 404 porque la ruta solo existe dentro de React Router. Y el cacheo
diferenciado: los assets llevan hash en el nombre y pueden cachearse un año, pero `index.html`
nunca — es el que apunta a los assets nuevos.

**`front/nginx.conf`**

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;

  gzip on;
  gzip_types text/css application/javascript application/json image/svg+xml;

  location /assets/ {
    expires 1y;
    add_header Cache-Control "public, immutable";
  }

  location = /index.html {
    add_header Cache-Control "no-cache";
  }

  location / {
    try_files $uri $uri/ /index.html;
  }
}
```

---

## 05 · Orquestación

Postgres, Redis, la API y el front. La regla de oro: al host solo se asoman los puertos del
proxy. Todo lo demás se habla por la red interna de Docker.

**`docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: pipoe
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      retries: 5
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    # appendonly: los refresh tokens viven acá. Sin persistencia,
    # un reinicio desloguea a todo el mundo.
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    volumes:
      - redisdata:/data
    healthcheck:
      test: ["CMD", "redis-cli", "--pass", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      retries: 5
    restart: unless-stopped

  api:
    build: ./api
    env_file: .env
    environment:
      DB_URL: jdbc:postgresql://db:5432/pipoe
      REDIS_HOST: redis
    depends_on:
      db: { condition: service_healthy }
      redis: { condition: service_healthy }
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 15s
      start_period: 60s
      retries: 3
    restart: unless-stopped

  front:
    build:
      context: ./front
      args:
        VITE_API_URL: ""
    ports:
      - "80:80"
    depends_on:
      - api
    restart: unless-stopped

volumes:
  pgdata:
  redisdata:
```

### Por qué los healthchecks no son opcionales

La API corre Flyway al arrancar. Si Postgres todavía está inicializando, la migración falla y el
contenedor muere. Con `condition: service_healthy` el arranque se ordena solo. El
`start_period: 60s` le da aire a Spring para levantar sin que Docker lo declare enfermo antes de
tiempo.

> **Redis no es cache descartable.** Ahí viven los refresh tokens rotativos y el estado del rate
> limiting. Perder ese volumen no degrada el servicio: desloguea a todos los usuarios de golpe.
> Va con `appendonly yes`, volumen nombrado y backup, igual que la base.

### Puertos

Fijate que `db` y `redis` no publican puertos. Se alcanzan desde `api` por nombre de servicio
(`db:5432`, `redis:6379`) dentro de la red de compose. Publicar 5432 al host equivale a exponer
Postgres a internet, y los bots lo encuentran en horas.

---

## 06 · Dominio y TLS

La decisión de arquitectura más importante del deploy, y la que más problemas evita: todo bajo
`pipoe.com`, con `/api` proxeado a la API.

El nginx del front recibe todo y reparte. Agregá este bloque a `nginx.conf`:

```nginx
location /api/ {
  proxy_pass http://api:8080;
  proxy_set_header Host $host;
  proxy_set_header X-Forwarded-Proto $scheme;
  # $remote_addr y no $proxy_add_x_forwarded_for:
  # pisa cualquier X-Forwarded-For que mande el cliente.
  proxy_set_header X-Forwarded-For $remote_addr;
  client_max_body_size 2m;
}
```

### Qué resuelve

- **CORS desaparece.** Mismo origen, sin preflight. Igual dejá `CORS_ALLOWED_ORIGINS` apuntando
  al dominio real por si algún día separás los hosts.
- **Una sola imagen de front** para todos los ambientes, porque la URL de la API es relativa.
- **Un solo certificado** y un solo registro DNS.

> ⚠️ **Nunca** `CORS_ALLOWED_ORIGINS=*`. La config usa `allowCredentials(true)`, y la
> especificación prohíbe combinar el comodín con credenciales: el navegador rechaza todas las
> respuestas y Spring puede tirar excepción al armar la configuración. Siempre el dominio
> explícito.

### Certificado

Dos caminos, ambos bien:

- **Caddy** como contenedor del borde en lugar de exponer nginx. Saca y renueva el certificado
  de Let's Encrypt solo, con tres líneas de config. Es el camino corto.
- **nginx + certbot** si querés control fino sobre la config de TLS. Más piezas: renovación por
  cron y recarga de nginx.

Con TLS andando, sumá en el borde: redirección de 80 a 443, HSTS,
`X-Content-Type-Options: nosniff`, `Referrer-Policy` y una Content-Security-Policy.

---

## 07 · Variables y secretos

La buena noticia: `application.yml` ya está parametrizado por completo, no hay nada hardcodeado
que haya que ir a buscar. Solo falta poblar las variables.

| Variable | | Qué es y qué pasa si falta |
|---|---|---|
| `JWT_SECRET_KEY` | **Obligatoria** | Firma de los tokens. No tiene default: sin ella la app no arranca, y está bien que sea así. Generala con `openssl rand -base64 48`. |
| `DB_URL` | **Obligatoria** | En compose: `jdbc:postgresql://db:5432/pipoe`. |
| `DB_USERNAME` | **Obligatoria** | No uses `postgres` en producción; creá un rol propio. |
| `DB_PASSWORD` | **Obligatoria** | Generada, larga, distinta de cualquier otra del sistema. |
| `REDIS_HOST` | **Obligatoria** | `redis` dentro de compose. El default `localhost` no aplica entre contenedores. |
| `REDIS_PASSWORD` | **Obligatoria** | Requiere el cambio de la fase 02. Sin password, cualquier cosa en la red lee los refresh tokens. |
| `CORS_ALLOWED_ORIGINS` | **Obligatoria** | Dominio exacto con esquema: `https://pipoe.com`. Múltiples separados por coma. |
| `MAIL_HOST` · `MAIL_PORT` | **Obligatoria** | Default apunta a Gmail. Si el sistema manda credenciales de colaborador por mail, esto es funcionalidad crítica, no un extra. |
| `MAIL_USERNAME` · `MAIL_PASSWORD` | **Obligatoria** | Con Gmail necesitás una *app password*, no la del usuario. Ojo con los límites diarios de envío. |
| `MAIL_FROM` | Opcional | Default `no-reply@pipoe.com`. Tiene que ser un dominio con SPF y DKIM o los mails caen en spam. |
| `MAX_LOGIN_ATTEMPTS` | Opcional | Default 5 en ventana de 5 minutos, bloqueo de 30. Razonable tal cual. |
| `MAX_SOLICITUDES_ACCESO` | Opcional | Tope por IP del endpoint público. Default 3 por hora. |

### Dónde viven

Para un deploy de este tamaño: un archivo `.env` en el servidor, fuera del repo, con permisos
`600` y dueño el usuario del deploy. Compose lo lee con `env_file`. Si más adelante hay varios
ambientes o más de una persona operando, el paso siguiente es Docker secrets o el gestor del
proveedor.

> **Rotar el secreto JWT.** Cambiar `JWT_SECRET_KEY` invalida todos los access tokens en
> circulación. Los refresh tokens viven en Redis y siguen siendo válidos, así que las sesiones
> se recuperan solas en el siguiente refresh. Si querés un logout global de verdad — después de
> un incidente — rotá el secreto *y* vaciá las claves de refresh en Redis.

---

## 08 · Datos

Flyway corre en cada arranque de la API, con `ddl-auto: validate`. Es el esquema correcto para
producción: la app valida contra el esquema real y se niega a arrancar si no coincide, en vez de
modificar tablas por su cuenta.

### Reglas de las migraciones

- **Nunca editar una migración ya aplicada.** Flyway guarda un checksum; si el archivo cambia,
  la validación falla y la API no levanta. Los cambios van siempre en un `V8__` nuevo.
- **Probá cada migración contra una copia de producción** antes de desplegar. Un `ALTER TABLE`
  sobre una tabla grande puede tomar un lock y dejar la app colgada durante el deploy.
- **Si algún día corrés más de una instancia de la API**, Flyway toma un lock y solo una migra —
  funciona, pero lo prolijo es migrar en un paso separado del deploy, antes de levantar las
  instancias nuevas.

### Backups

Un volumen de Docker no es un backup: protege contra recrear el contenedor, no contra un
`DROP TABLE`, un disco muerto o un ransomware.

- `pg_dump` diario a un destino *fuera* del servidor (S3, Backblaze, otro host).
- Retención escalonada: diarios una semana, semanales un mes.
- **Probá el restore.** Un backup que nunca se restauró es una hipótesis, no un backup.
  Marcalo en el calendario cada tres meses.
- Redis: con `appendonly` el archivo está en el volumen; copialo junto con el dump.

---

## 09 · Operación y CI/CD

### Logs

Spring ya escribe a stdout, que es lo correcto en contenedores. Lo que falta es acotarlos, o el
disco del servidor se llena en silencio:

```yaml
logging:
  driver: json-file
  options: { max-size: "10m", max-file: "3" }
```

Ponelo en cada servicio del compose. Nivel de log por variable (`LOGGING_LEVEL_ROOT`) para poder
subir a `DEBUG` sin rebuildear. Y revisá que ningún log imprima tokens, passwords o el cuerpo
completo de los requests de login.

### Monitoreo

- Un chequeo externo cada minuto contra `/actuator/health` que te avise por mail o Telegram. Es
  lo mínimo y cubre el 90% de los sustos.
- Alerta de disco al 80%: entre logs, backups y el volumen de Postgres, es la forma más común de
  que un VPS se caiga.
- `restart: unless-stopped` en todos los servicios, ya está en el compose de arriba.

### Pipeline

Con el repo en GitHub, un workflow que en cada push a `main`:

- corre `mvn test` y `npm run lint && npm run build`;
- construye las dos imágenes y las publica en GHCR **etiquetadas con el SHA del commit**, no con
  `latest` — sin tag inmutable no hay rollback posible;
- se conecta por SSH al servidor y hace `docker compose pull && docker compose up -d`.

El rollback entonces es cambiar el tag en el `.env` y repetir el `up -d`. Si el proyecto va a
tener usuarios reales, agregá un ambiente de staging con su propia base antes de que main llegue
a producción.

---

## 10 · Dónde hostear

**VPS + compose · ~6–15 USD/mes · recomendado**
Hetzner o DigitalOcean, 2 vCPU y 4 GB. Todo lo de este plan aplica tal cual. Vos administrás el
sistema operativo, los backups y las actualizaciones. Para este stack y este tamaño, es el mejor
equilibrio entre costo y control.

**PaaS · ~20–40 USD/mes**
Railway, Render o Fly. Postgres y Redis gestionados con backups incluidos, deploy desde git, TLS
automático. Menos operación a cambio de más costo y menos control sobre la red.

**Cloud grande · variable**
AWS o GCP con RDS, ElastiCache y contenedores gestionados. Sobra para este proyecto: la
complejidad operativa no se paga sola hasta que hay escala o un requisito de compliance.

---

## 11 · Checklist del día del deploy

En orden. Si algo no da, no sigas: cada punto asume el anterior.

- [ ] El repo está en git, `front/.env` no está versionado y el historial está limpio.
- [ ] Actuator responde y `/actuator/health` está permitido en `SecurityConfig`.
- [ ] Existe la forma de crear el primer ADMIN y la probaste en una base vacía.
- [ ] Las dos imágenes se construyen desde cero, sin caché, sin errores.
- [ ] Todas las variables obligatorias están en el `.env` del servidor, con permisos `600`.
- [ ] `JWT_SECRET_KEY` es un valor generado, distinto del de desarrollo.
- [ ] Postgres y Redis no publican puertos al host.
- [ ] Redis tiene `requirepass` y `appendonly yes`.
- [ ] El DNS apunta al servidor y el certificado TLS está emitido y renovándose solo.
- [ ] `CORS_ALLOWED_ORIGINS` es el dominio real, sin comodines.
- [ ] El proxy pisa `X-Forwarded-For` con la IP real de la conexión.
- [ ] Recargar la página en una ruta interna del front no da 404.
- [ ] El flujo completo anda contra producción: login, refresh de token tras 15 minutos, alta de
      proyecto, alta de colaborador, mail recibido.
- [ ] El rate limiting bloquea después de 5 intentos fallidos de login.
- [ ] El backup automático corrió una vez y lo restauraste en limpio.
- [ ] El chequeo externo de salud está activo y te avisa.
- [ ] Probaste el rollback: volver al tag anterior y levantar.
