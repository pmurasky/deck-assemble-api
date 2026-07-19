FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN ./gradlew dependencies --no-daemon
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 deckassemble
USER deckassemble
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
