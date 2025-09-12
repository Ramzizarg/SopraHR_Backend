FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/notification-service-0.0.1-SNAPSHOT.jar /app/notification-service.jar

EXPOSE 3001

CMD ["java", "-jar", "/app/notification-service.jar"]
