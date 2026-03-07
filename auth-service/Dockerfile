# ─── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy parent POM first (layer-cache friendly)
COPY pom.xml ./pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml

# Download dependencies before copying source (better caching)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f auth-service/pom.xml dependency:go-offline -B 2>/dev/null || true

COPY auth-service/src ./auth-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -f auth-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/auth-service/target/*.jar app.jar

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
