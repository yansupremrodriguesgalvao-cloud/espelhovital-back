FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests
EXPOSE 8081
CMD ["java", "-jar", "target/espelhovital-back-0.0.1-SNAPSHOT.jar"]