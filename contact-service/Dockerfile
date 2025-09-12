FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/contact-service-0.0.1-SNAPSHOT.jar /app/contact-service.jar

EXPOSE 4001

CMD ["java", "-jar", "/app/contact-service.jar"]
