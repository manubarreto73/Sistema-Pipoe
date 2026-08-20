# Build en dos etapas. La primera tiene Maven, el JDK y medio Maven Central; la segunda
# sólo el JRE y el jar. La imagen final ronda los 180 MB en vez de 500, y no lleva adentro
# el compilador ni el código fuente.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# El pom primero y solo: mientras no cambien las dependencias, Docker reusa esta capa y no
# vuelve a bajar nada. Copiar todo de una haría que cualquier cambio en un .java rehiciera
# la descarga completa.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Sin tests: el único que hay levanta el contexto entero de Spring, que necesita Postgres,
# Redis y el secreto JWT. Un build de imagen no tiene ni debería tener eso a mano; los tests
# van en el pipeline de CI, donde se levantan los servicios aparte.
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# curl lo usa el healthcheck del compose. La imagen de Temurin no lo trae.
RUN apt-get update && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# Usuario sin privilegios: si alguien logra ejecutar algo dentro del contenedor, no es root.
RUN useradd --system --uid 1001 pipoe

COPY --from=build /build/target/*.jar app.jar
USER pipoe

EXPOSE 8080

# Heap FIJO, no un porcentaje. La forma habitual es -XX:MaxRAMPercentage, que reparte sobre
# el límite del contenedor; pero eso depende de que la JVM sepa leer ese límite, y no siempre
# lo logra: con Docker Desktop sobre WSL2, con un límite de 512 MB la JVM igual se cree dueña
# de la RAM del host y pide un heap de casi 6 GB. Un valor explícito da el mismo resultado en
# todos lados y no hay que confiar en la autodetección.
#
# 1 GB alcanza de sobra: el trabajo pesado es exportar documentos, no volumen de tráfico. Se
# puede subir sin reconstruir la imagen, pasando JAVA_OPTS por variable de entorno.
ENV JAVA_OPTS="-Xms256m -Xmx1g"

# `exec` para que java quede como PID 1 y reciba las señales de Docker: sin eso, un
# `docker stop` no le llega a la aplicación y el contenedor muere de un corte a los 10s en
# vez de cerrar las conexiones a la base y apagarse prolijo.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
