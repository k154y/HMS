# =========================================================
# HMS Backend Production Image
# =========================================================
#
# Stage 1:
# Build the Spring Boot application with Maven and Java 21.
#
# Stage 2:
# Run only the packaged JAR using a smaller Java 21 runtime.
#
# The same image is used for staging and production.
# Environment variables determine the target environment.
# =========================================================


# ---------------------------------------------------------
# Build stage
# ---------------------------------------------------------

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace


# Copy the Maven project descriptor first so Docker can cache
# dependency resolution separately from application source changes.
COPY apps/api/pom.xml apps/api/pom.xml


RUN mvn \
    -B \
    -ntp \
    -f apps/api/pom.xml \
    dependency:go-offline


# Copy backend source only after dependencies are cached.
COPY apps/api/src apps/api/src


# Build the executable Spring Boot JAR.
#
# Tests are intentionally not executed during image packaging.
# CI/CD will run the test suite before an image is accepted.
RUN mvn \
    -B \
    -ntp \
    -DskipTests \
    -f apps/api/pom.xml \
    clean package


# ---------------------------------------------------------
# Runtime stage
# ---------------------------------------------------------

FROM eclipse-temurin:21-jre-noble AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# ---------------------------------------------------------
# Security: run the application as a non-root user
# ---------------------------------------------------------

RUN groupadd \
        --system \
        hms \
    && useradd \
        --system \
        --gid hms \
        --home-dir /app \
        --create-home \
        --shell /usr/sbin/nologin \
        hms


WORKDIR /app


# Copy only the packaged application from the build stage.
COPY --from=build \
    --chown=hms:hms \
    /workspace/apps/api/target/*.jar \
    /app/app.jar


# ---------------------------------------------------------
# Runtime defaults
# ---------------------------------------------------------

ENV SPRING_PROFILES_ACTIVE=prod

ENV HMS_BACKEND_PORT=8081

ENV JAVA_OPTS=""


EXPOSE 8081


# ---------------------------------------------------------
# Container health
# ---------------------------------------------------------

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=60s \
    --retries=3 \
    CMD curl --fail --silent \
        "http://127.0.0.1:${HMS_BACKEND_PORT}/actuator/health" \
        >/dev/null \
        || exit 1


# ---------------------------------------------------------
# Drop root privileges
# ---------------------------------------------------------

USER hms


# ---------------------------------------------------------
# Start Spring Boot
# ---------------------------------------------------------

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]