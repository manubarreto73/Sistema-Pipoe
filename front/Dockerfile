# El front compilado son archivos estáticos: Node hace falta para construirlos y después
# desaparece. La imagen final es nginx con la carpeta dist adentro.

# Node 24 y no 22: el package-lock.json se genera con npm 11, y npm 10 lo lee distinto —
# da "Missing: ... from lock file" para dependencias que sí están. La versión de Node de la
# imagen tiene que acompañar a la de la máquina donde se actualiza el lock.
FROM node:24-alpine AS build
WORKDIR /build

# package*.json primero, por la misma razón que el pom en la API: mientras no cambien las
# dependencias, Docker reusa la capa del npm ci.
COPY package*.json ./
RUN npm ci

COPY . .

# OJO: Vite reemplaza import.meta.env en tiempo de COMPILACIÓN, no de ejecución. Este valor
# queda horneado en el bundle; pasarlo como variable del `docker run` no hace absolutamente
# nada. Vacío = rutas relativas contra el mismo origen, que es como se sirve en producción:
# el nginx de acá abajo proxea /api a la API, así no hay CORS ni segundo dominio.
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

FROM nginx:alpine AS runtime
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY cabeceras-seguridad.inc /etc/nginx/conf.d/cabeceras-seguridad.inc
EXPOSE 80
