@echo off
echo Starting Notification System Services...
echo.

echo 1. Starting Eureka Server (Service Discovery)...
start "Eureka Server" cmd /k "cd eureka-server && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo 2. Starting User Service...
start "User Service" cmd /k "cd user-service && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo 3. Starting Notification Service...
start "Notification Service" cmd /k "cd notification-service && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo 4. Starting Teletravail Service...
start "Teletravail Service" cmd /k "cd teletravail-service && mvn spring-boot:run"
timeout /t 10 /nobreak > nul

echo.
echo All services are starting...
echo.
echo Service URLs:
echo - Eureka Server: http://localhost:8761
echo - User Service: http://localhost:9001
echo - Notification Service: http://localhost:3001
echo - Teletravail Service: http://localhost:7001
echo.
echo The notification system is now ready!
echo When you create a telework request, notifications will be automatically sent.
pause 