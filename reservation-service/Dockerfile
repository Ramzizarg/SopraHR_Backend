FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/reservation-service-0.0.1-SNAPSHOT.jar /app/reservation-service.jar

EXPOSE 6001

CMD ["java", "-jar", "/app/reservation-service.jar"]
