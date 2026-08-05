# Multi-stage Dockerfile following 2026 best practices
# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace
COPY pom.xml .
COPY src src

RUN apk add --no-cache maven && \
    mvn clean package -DskipTests && \
    mkdir -p /workspace/target/extracted && \
    cd /workspace/target/extracted && \
    jar xf ../todo-app-*.jar

# Stage 2: Runtime stage (minimal image)
FROM eclipse-temurin:21-jre-alpine

# Security: Run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy extracted JAR layers from builder
COPY --from=builder --chown=appuser:appgroup /workspace/target/extracted/BOOT-INF/lib /app/lib
COPY --from=builder --chown=appuser:appgroup /workspace/target/extracted/BOOT-INF/classes /app
COPY --from=builder --chown=appuser:appgroup /workspace/target/extracted/META-INF /app/META-INF

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-cp", ".:lib/*", "com.example.app.TodoApplication"]
