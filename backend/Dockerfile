# syntax=docker/dockerfile:1
# =============================================================================
#  Backend Spring Boot — image multi-etapes
#  L'etape de build resout les dependances AVANT de copier les sources, pour que
#  la couche des dependances reste en cache tant que le pom ne change pas.
# =============================================================================

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Wrapper et descripteur d'abord : cette couche survit aux modifications de code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Les tests ont besoin de Docker et tournent en integration continue, pas ici.
RUN ./mvnw -B -q clean package -DskipTests \
 && mv target/*.jar /build/application.jar

# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS execution
WORKDIR /app

# Utilisateur non privilegie : un conteneur applicatif ne tourne jamais en root.
RUN addgroup -S presences && adduser -S -G presences presences \
 && apk add --no-cache curl
USER presences:presences

COPY --from=build --chown=presences:presences /build/application.jar application.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar"]
