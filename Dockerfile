FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY target/workstation-0.0.1-SNAPSHOT.jar /app/workstation.jar

CMD ["java", "-jar", "/app/workstation.jar"]
