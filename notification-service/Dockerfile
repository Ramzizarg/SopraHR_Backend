# Single-stage Dockerfile using pre-built JAR from Jenkins pipeline
FROM openjdk:17-jre-slim
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 3001
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]