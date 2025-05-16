FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle/libs.versions.toml .
COPY rps-server/build.gradle.kts rps-server/build.gradle.kts
COPY rps-server/src rps-server/src

RUN ./gradlew :rps-server:shadowJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/rps-server/build/libs/rps-server-all.jar app.jar

EXPOSE 5050

CMD ["java", "-jar", "app.jar"]
