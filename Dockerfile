FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --uid 1001 workforce
COPY --from=build /build/target/workforce-*.jar app.jar
USER workforce
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Europe/Zagreb
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
