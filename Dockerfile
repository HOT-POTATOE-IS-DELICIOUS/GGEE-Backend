FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY . .

RUN chmod +x gradlew \
    && ./gradlew --no-daemon :core-bootstrap:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/core-bootstrap/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
