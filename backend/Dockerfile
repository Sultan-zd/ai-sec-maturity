# syntax=docker/dockerfile:1
# =====================================================================
# Image du backend AIMA (Java 21 / Spring Boot)
# Contexte de build : la RACINE du dépôt (le référentiel JSON est dans ../referential)
#   docker build -f backend/Dockerfile .
# =====================================================================

# ---- Étape 1 : compilation (JDK complet + Maven Wrapper) -------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dépendances d'abord : cette couche n'est reconstruite que si pom.xml change
COPY backend/.mvn backend/.mvn
COPY backend/mvnw backend/pom.xml backend/
RUN sed -i 's/\r$//' backend/mvnw && chmod +x backend/mvnw
RUN --mount=type=cache,target=/root/.m2 cd backend && ./mvnw -B -q dependency:go-offline

# Code source + référentiel, puis construction du jar (les tests tournent dans la CI)
COPY referential referential
COPY backend/src backend/src
RUN --mount=type=cache,target=/root/.m2 cd backend && ./mvnw -B -q -DskipTests package \
    && cp target/aima-backend-*.jar /workspace/app.jar \
    && java -Djarmode=tools -jar /workspace/app.jar extract --layers --launcher --destination /workspace/extracted

# ---- Étape 2 : exécution (JRE seul, utilisateur non-root) ------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S aima && adduser -S aima -G aima
WORKDIR /app

# Couches du jar, de la moins à la plus souvent modifiée (meilleur cache Docker)
COPY --from=build --chown=aima:aima /workspace/extracted/dependencies/ ./
COPY --from=build --chown=aima:aima /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=aima:aima /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=aima:aima /workspace/extracted/application/ ./

USER aima
EXPOSE 8090
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:8090/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
