You are writing the production-grade
Dockerfile for the RideList Spring Boot
3.2 backend.

Repository: ofspain/ride-listing
Place file at: Dockerfile
.dockerignore

Read before writing anything:
- pom.xml (confirm Java version,
  packaging, final artifact name)
- src/main/resources/application.yml
  or application.properties
  (confirm server.port, active profiles)
- Any existing Dockerfile if present

---

DOCKERFILE REQUIREMENTS

Multi-stage build with exactly
two stages: builder and runner.

STAGE 1 — builder
Base image: eclipse-temurin:21-jdk-alpine

Why alpine: smallest JDK image,
reduces build time and layer size.

Steps:
1. WORKDIR /app
2. Copy pom.xml first (layer caching)
   COPY pom.xml .
3. Download dependencies in a
   separate layer for caching:
   RUN mvn dependency:go-offline -B
   This means if only source changes
   (not pom.xml) the dependency layer
   is served from cache — builds are
   significantly faster in CI.
4. Copy source:
   COPY src ./src
5. Build jar (skip tests —
   tests run in CI before Docker build):
   RUN mvn clean package -DskipTests -B

STAGE 2 — runner
Base image: eclipse-temurin:21-jre-alpine

Why JRE not JDK: no compiler needed
at runtime, smaller attack surface,
smaller image size (~100MB saving).

Steps:
1. WORKDIR /app

2. Create non-root user for security:
   RUN addgroup -S ridelist && \
   adduser -S ridelist -G ridelist

3. Copy jar from builder stage:
   COPY --from=builder /app/target/*.jar \
   app.jar

4. Set ownership:
   RUN chown ridelist:ridelist app.jar

5. Switch to non-root user:
   USER ridelist

6. Expose port:
   EXPOSE 8080

7. JVM environment variable:
   ENV JAVA_OPTS="-Xmx350m -Xms256m \
   -XX:+UseContainerSupport \
   -XX:MaxRAMPercentage=75.0 \
   -Djava.security.egd=file:/dev/./urandom"

   Explanation of each flag:
   -Xmx350m           Hard cap heap at 350MB
   (critical for t2.micro)
   -Xms256m           Start with 256MB heap
   +UseContainerSupport  Respect Docker memory limits
   MaxRAMPercentage   Fallback if Xmx not set
   security.egd       Faster random seed for JVM
   startup (important in containers)

8. Entrypoint:
   ENTRYPOINT ["sh", "-c", \
   "java $JAVA_OPTS \
   -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
   -jar app.jar"]

   The SPRING_PROFILES_ACTIVE defaults
   to prod if not set via environment.
   Override per environment:
   test: SPRING_PROFILES_ACTIVE=prod
   local: SPRING_PROFILES_ACTIVE=dev

---

.DOCKERIGNORE FILE

Create .dockerignore at project root.
This prevents large unnecessary files
from entering the build context —
dramatically speeds up docker build.

Contents:
.git
.gitignore
.github
target/
*.md
*.log
docker-compose*.yml
Dockerfile*
.env*
.idea/
*.iml
*.iws
.DS_Store
node_modules/

---

HEALTHCHECK

Add Docker-native healthcheck so
Docker Compose knows when the backend
is truly ready (not just started):

HEALTHCHECK --interval=30s \
--timeout=10s \
--start-period=60s \
--retries=3 \
CMD wget -q -O /dev/null \
http://localhost:8080/actuator/health \
|| exit 1

start-period=60s gives Spring Boot
time to start before health checks
begin. Critical on t2.micro where
JVM startup is slower.

---

VERIFY the Dockerfile works locally:

# Build the image
docker build -t ridelist-backend:test .

# Check image size (should be under 250MB)
docker images ridelist-backend:test

# Run it locally (will fail without DB
# but should start the JVM)
docker run --rm \
-e DB_HOST=localhost \
-e DB_PORT=5432 \
-e DB_NAME=ridelist \
-e DB_USERNAME=ridelist \
-e DB_PASSWORD=test \
-e JWT_SECRET=test-secret-32-chars-minimum \
-e AWS_ACCESS_KEY=test \
-e AWS_SECRET_KEY=test \
-e AWS_REGION=eu-west-1 \
-e AWS_S3_BUCKET=test \
-p 8080:8080 \
ridelist-backend:test

# Confirm in logs:
# - JVM starts
# - Spring context loads
# - Flyway attempts migration
#   (will fail without real DB — expected)
# - No OOM errors

# Check image layers
docker history ridelist-backend:test

---

DOCUMENT in a comment block at top
of Dockerfile:

# RideList Backend
# Base: eclipse-temurin:21-jre-alpine
# Build: mvn clean package -DskipTests
# Run:   java $JAVA_OPTS -jar app.jar
# Port:  8080
# User:  ridelist (non-root)
# Health: GET /actuator/health