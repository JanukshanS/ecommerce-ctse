# ─── Stage 1: Build ───────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml ./pom.xml
COPY order-service/pom.xml ./order-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f order-service/pom.xml dependency:go-offline -B 2>/dev/null || true
COPY order-service/src ./order-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f order-service/pom.xml clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /app/order-service/target/*.jar app.jar
EXPOSE 8084
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8084/actuator/health || exit 1
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
