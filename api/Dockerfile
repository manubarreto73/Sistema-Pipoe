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

# MaxRAMPercentage hace que la JVM calcule su heap sobre el límite del contenedor y no sobre
# la RAM del host. Sin esto, en un servidor de 4 GB con varios contenedores, la JVM se cree
# dueña de todo y el kernel termina matando el proceso por falta de memoria.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
