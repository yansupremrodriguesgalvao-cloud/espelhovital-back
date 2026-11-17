FROM eclipse-temurin:20-jdk-alpine
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/espelhovital-back-0.0.1-SNAPSHOT.jar"]