FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY . .

RUN chmod +x gradlew \
    && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
