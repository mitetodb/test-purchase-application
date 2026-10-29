# syntax=docker/dockerfile:1
# --- build stage ---
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar /app.jar

# --- runtime stage (non-root) ---
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S -G app app \
    && apk add --no-cache wget \
    && mkdir -p /app/uploads && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /app.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
