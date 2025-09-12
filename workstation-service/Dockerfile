FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/workstation-service-0.0.1-SNAPSHOT.jar /app/workstation-service.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/workstation-service.jar"]
