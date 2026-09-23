# ---------- build
FROM eclipse-temurin:21.0.12_8-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src src
# Tests run in CI (they need Docker for Testcontainers); the image only packages the verified code.
RUN ./mvnw -B -ntp package -DskipTests \
    && java -Djarmode=tools -jar target/nomina-ec-1.0.0.jar extract --layers --launcher --destination target/extracted

# ---------- runtime
FROM eclipse-temurin:21.0.12_8-jre-alpine
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/actuator/health" | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
