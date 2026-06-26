# ============================================================
# Multi-stage Dockerfile — Airline Booking System
# ============================================================
# Stage 1: Build the JAR with Maven
# Stage 2: Run the JAR on a lightweight JRE image
# ============================================================

# ── Stage 1: Build ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching — only re-downloads
# dependencies when pom.xml changes, not on every source change)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Run ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]