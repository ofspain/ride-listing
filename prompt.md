You are a senior Spring Boot engineer and cloud-native systems architect.

Implement an ultra-lean, production-ready logging and monitoring setup for a Spring Boot application (RideList) running on Kubernetes (AWS EKS).

The goal is:
- Structured logging
- Request traceability
- Minimal operational overhead
- Cloud-native compatibility

DO NOT introduce unnecessary infrastructure like MongoDB or ELK for MVP.

---

# 🧱 LOGGING REQUIREMENTS

## 1. Structured JSON Logging

Use Logback with logstash encoder.

Logs must be JSON formatted and include:

- timestamp
- log level
- service name
- traceId (correlation ID)
- thread
- logger
- message
- optional context (userId, listingId, etc.)

---

## 2. Dependencies

Add:

- spring-boot-starter-logging
- logstash-logback-encoder

---

## 3. Logback Configuration

Create logback-spring.xml:

- Use LogstashEncoder
- Output logs to STDOUT (important for Kubernetes)
- Include MDC fields automatically

---

## 4. Correlation ID (VERY IMPORTANT)

Implement a request filter:

- Generate a UUID per request
- Store in MDC as "traceId"
- Add to response header (X-Trace-Id)

Example:

MDC.put("traceId", UUID.randomUUID().toString());

Ensure:
- Cleared after request completes

---

## 5. Logging Best Practices

- Use parameterized logging (no string concatenation)
- Log key events:
    - user registration
    - login attempts
    - listing creation/update
    - image upload
- Log errors with stack trace

DO NOT log:
- passwords
- sensitive tokens

---

# 🧱 MONITORING REQUIREMENTS

## 6. Spring Boot Actuator

Enable:

/actuator/health
/actuator/info
/actuator/metrics

Expose via application.yml:

management:
endpoints:
web:
exposure:
include: health,info,metrics

---

## 7. Kubernetes Readiness

Ensure:

- /actuator/health used for liveness/readiness probes
- Logs go to STDOUT (Kubernetes best practice)

---

## 8. Metrics (Basic)

Expose:
- HTTP request count
- error rates
- JVM metrics

(No Prometheus setup required in MVP, just ensure Actuator works)

---

# 🧱 OPTIONAL (LIGHT ENHANCEMENT)

If simple to add:

- Integrate Micrometer (already included via actuator)
- Tag metrics with:
    - endpoint
    - status code

---

# 🧱 OUTPUT

Generate:

- logback-spring.xml
- CorrelationIdFilter (OncePerRequestFilter)
- Sample logging usage in a service class
- application.yml additions

Keep implementation clean, minimal, and production-ready.

---

# ⚠️ CONSTRAINTS

- DO NOT introduce MongoDB
- DO NOT introduce ELK stack
- DO NOT overengineer
- Keep it Kubernetes-friendly (STDOUT logging)

---

# 🎯 GOAL

Logs should be easily consumable by Kubernetes log collectors (e.g., Fluent Bit → CloudWatch).

System should be debuggable and observable with minimal setup.


### Update context
Update the CLAUDE.md file of this project with this newly added context/functionalities