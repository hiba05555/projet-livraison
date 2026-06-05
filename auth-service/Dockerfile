# ═══════════════════════════════════════════════════════════════════════════
# Stage 1 – Build
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Cache dependency resolution separately from source compilation
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

# Build the application
COPY src ./src
RUN mvn -B package -DskipTests -q

# ═══════════════════════════════════════════════════════════════════════════
# Stage 2 – Extract layers (Spring Boot layered JAR for better caching)
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine AS extractor
WORKDIR /build
COPY --from=builder /build/target/auth-service-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ═══════════════════════════════════════════════════════════════════════════
# Stage 3 – Runtime (minimal, non-root, read-only FS)
# ═══════════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security hardening: run as non-root
RUN addgroup -S vrp && adduser -S authsvc -G vrp
USER authsvc

# Copy Spring Boot layers from stage 2 (improves Docker layer cache hits)
COPY --from=extractor /build/dependencies          ./
COPY --from=extractor /build/spring-boot-loader    ./
COPY --from=extractor /build/snapshot-dependencies ./
COPY --from=extractor /build/application           ./

EXPOSE 8081

# Health check for Docker (Kubernetes uses Actuator probes instead)
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:docker}", \
  "org.springframework.boot.loader.launch.JarLauncher"]
