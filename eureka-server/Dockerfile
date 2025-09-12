FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/*.jar /app/eureka-server.jar

ENTRYPOINT ["java", "-jar", "/app/eureka-server.jar"]
