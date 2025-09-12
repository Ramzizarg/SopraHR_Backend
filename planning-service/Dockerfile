FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/planning-service-0.0.1-SNAPSHOT.jar /app/planning-service.jar

EXPOSE 8001

CMD ["java", "-jar", "/app/planning-service.jar"]
