FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/workstation.jar app.jar /app/workstation.jar

CMD ["java", "-jar", "/app/workstation.jar"]
