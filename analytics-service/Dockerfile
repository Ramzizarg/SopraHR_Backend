FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/analytics-service-0.0.1-SNAPSHOT.jar /app/analytics-service.jar

EXPOSE 5001

CMD ["java", "-jar", "/app/analytics-service.jar"]
