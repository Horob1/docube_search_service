# ============================================================
# Stage 1: Build
# ============================================================
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy build files first (cache dependencies layer)
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./
COPY gradle ./gradle

# Download dependencies (cached if build files unchanged)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build fat JAR (skip tests — tests run in CI pipeline)
RUN gradle clean bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="horob1"
LABEL service="search-service"

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Expose port
EXPOSE 9111

# JVM tuning (can be overridden via docker-compose environment)
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

