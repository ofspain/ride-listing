# RideList Backend
# Base: eclipse-temurin:21-jre-alpine
# Build: mvn clean package -DskipTests
# Run:   java $JAVA_OPTS -jar app.jar
# Port:  8090
# User:  ridelist (non-root)
# Health: GET /actuator/health

# =============================================================================
# STAGE 1: Builder
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Install Maven (not included in eclipse-temurin)
RUN apk add --no-cache maven

# Copy pom.xml first for dependency layer caching
COPY pom.xml .

# Download dependencies in separate layer for caching
# If only source changes (not pom.xml), this layer is served from cache
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build jar (skip tests - tests run in CI before Docker build)
RUN mvn clean package -DskipTests -B

# =============================================================================
# STAGE 2: Runner
# =============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create non-root user for security
RUN addgroup -S ridelist && \
    adduser -S ridelist -G ridelist

# Copy jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown ridelist:ridelist app.jar

# Switch to non-root user
USER ridelist

# Expose port (matches server.port default)
EXPOSE 8090

# JVM environment variable optimized for containers
# -Xmx350m           Hard cap heap at 350MB (critical for t2.micro)
# -Xms256m           Start with 256MB heap
# +UseContainerSupport  Respect Docker memory limits
# MaxRAMPercentage   Fallback if Xmx not set
# security.egd       Faster random seed for JVM startup in containers
ENV JAVA_OPTS="-Xmx350m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Health check for Docker/Docker Compose
# start-period=60s gives Spring Boot time to start before health checks begin
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8090/actuator/health || exit 1

# Entrypoint with configurable Spring profile
# SPRING_PROFILES_ACTIVE defaults to prod if not set via environment
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -jar app.jar"]


############################################################################
############################################################################
########To verify locally:
########  docker build -t ridelist-backend:test .
########  docker images ridelist-backend:test  # Should be under 250MB
############################################################################
############################################################################