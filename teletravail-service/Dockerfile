FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/teletravail-service-0.0.1-SNAPSHOT.jar /app/teletravail-service.jar

EXPOSE 7001

CMD ["java", "-jar", "/app/teletravail-service.jar"]
