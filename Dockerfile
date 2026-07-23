FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package && \
    JAR_FILE=$(find target -maxdepth 1 -type f -name "*.jar" \
    ! -name "*.original" ! -name "*-plain.jar" | head -n 1) && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 sope \
    && useradd --system --uid 10001 --gid sope --home-dir /app --shell /usr/sbin/nologin sope

COPY --from=build --chown=10001:10001 /build/app.jar app.jar

ENV PORT=8080

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${PORT}/api/health" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
