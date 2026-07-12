# Build stage
FROM gradle:9.6.1-jdk26 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY src ./src
COPY config ./config

RUN gradle build -x spotlessCheck -x checkstyleMain -x test --no-daemon

# Run stage
FROM eclipse-temurin:26-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
