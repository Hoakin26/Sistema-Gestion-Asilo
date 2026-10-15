# syntax=docker/dockerfile:1
#
# YS-174. Imagen del backend en dos etapas: la primera compila con el JDK y la
# segunda lleva solo el JRE y el .jar, sin código fuente ni Maven.

# ---- Etapa 1: compilación ----
FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /fuente

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src/ src/

# Sin pruebas: necesitan Docker (Testcontainers) y ya corrieron antes de
# fusionar. La caché de Maven se conserva entre construcciones en el mismo
# servidor, así que las dependencias se descargan una sola vez.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q package -DskipTests

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Sin privilegios de administrador: si alguien lograra ejecutar código dentro
# del contenedor, no sería root. La carpeta de los archivos de laboratorio
# (HU-13) le pertenece, para que el volumen que se monta ahí nazca con permiso
# de escritura.
RUN groupadd --system asilo \
    && useradd --system --gid asilo --no-create-home asilo \
    && mkdir -p /var/lib/asilo/archivos \
    && chown asilo:asilo /var/lib/asilo/archivos

COPY --from=construccion /fuente/target/*.jar app.jar
USER asilo

EXPOSE 8080
# Hasta el 75 % de la memoria del contenedor para la JVM: por defecto toma solo
# el 25 %, poco para un servidor pequeño.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
