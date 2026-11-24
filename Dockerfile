FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src/main ./src/main
RUN mvn -B -ntp -DskipTests package   \
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring spring   \
COPY --from=build --chown=spring:spring /app/target/espelhovital-back-0.0.1-SNAPSHOT.jar app.jar
USER spring:spring
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]