# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

# O cache mantém as dependências Maven entre builds e os logs ficam visíveis.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 library
COPY --from=build /app/target/library-management-api-1.0.0.jar app.jar
USER library
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
