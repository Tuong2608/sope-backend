# ===============================
# GIAI ĐOẠN BUILD
# ===============================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .

RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -B -DskipTests clean package && \
    JAR_FILE=$(find target -maxdepth 1 -type f -name "*.jar" \
    ! -name "*.original" ! -name "*-plain.jar" | head -n 1) && \
    cp "$JAR_FILE" app.jar

# ===============================
# GIAI ĐOẠN CHẠY
# ===============================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /build/app.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]