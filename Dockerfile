# Build stage
FROM gradle:9.6.1-jdk26 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./

RUN ./gradlew dependencies --no-daemon

COPY src ./src
COPY config ./config

RUN ./gradlew build -x spotlessCheck -x checkstyleMain -x checkstyleTest -x test --parallel --build-cache --no-daemon

# Run stage
FROM eclipse-temurin:26-jre-jammy
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY start.sh .

EXPOSE 8080

ENTRYPOINT ["./start.sh"]
