@echo off
echo ========================================
echo   Workstation Management Application
echo   Complete Startup Script
echo ========================================
echo.

echo Step 1: Creating databases...
if exist "ai-decision-system\create_databases.bat" (
    call ai-decision-system\create_databases.bat
) else (
    echo WARNING: Database creation script not found.
    echo Please create the following databases manually:
    echo - analytics_db
    echo - contactdb
    echo - notification_db
    echo - planningdb
    echo - reservationdb
    echo - teletravaildb
    echo - SopraHR
)

echo.
echo Step 2: Starting Eureka Server (Port 8761)...
start "Eureka Server" cmd /k "cd eureka-server && mvnw spring-boot:run"

echo Waiting for Eureka to start...
timeout /t 10 /nobreak >nul

echo.
echo Step 3: Starting Microservices...
echo.

echo Starting Analytics Service (Port 5001)...
start "Analytics Service" cmd /k "cd analytics-service && mvnw spring-boot:run"

echo Starting Contact Service (Port 4001)...
start "Contact Service" cmd /k "cd contact-service && mvnw spring-boot:run"

echo Starting Notification Service (Port 3001)...
start "Notification Service" cmd /k "cd notification-service && mvnw spring-boot:run"

echo Starting Planning Service (Port 8001)...
start "Planning Service" cmd /k "cd planning-service && mvnw spring-boot:run"

echo Starting Reservation Service (Port 6001)...
start "Reservation Service" cmd /k "cd reservation-service && mvnw spring-boot:run"

echo Starting Teletravail Service (Port 7001)...
start "Teletravail Service" cmd /k "cd teletravail-service && mvnw spring-boot:run"

echo Starting User Service (Port 9001)...
start "User Service" cmd /k "cd user-service && mvnw spring-boot:run"

echo Starting Workstation Service (Port 8080)...
start "Workstation Service" cmd /k "cd workstation-service && mvnw spring-boot:run"

echo.
echo Step 4: Starting AI Decision System (Port 8000)...
start "AI Decision System" cmd /k "cd ai-decision-system && python dashboard_api.py"

echo.
echo ========================================
echo   Services Starting...
echo ========================================
echo.
echo Backend Services:
echo - Eureka Server: http://localhost:8761
echo - Analytics Service: http://localhost:5001
echo - Contact Service: http://localhost:4001
echo - Notification Service: http://localhost:3001
echo - Planning Service: http://localhost:8001
echo - Reservation Service: http://localhost:6001
echo - Teletravail Service: http://localhost:7001
echo - User Service: http://localhost:9001
echo - Workstation Service: http://localhost:8080
echo.
echo AI System:
echo - AI API: http://localhost:8000
echo - AI Dashboard: http://localhost:8050 (if running)
echo.
echo Frontend:
echo - Angular App: http://localhost:4200 (start manually)
echo.
echo ========================================
echo   Next Steps:
echo ========================================
echo 1. Wait for all services to start (check Eureka dashboard)
echo 2. Start Angular frontend: cd workstation-angular && ng serve
echo 3. Open browser to http://localhost:4200
echo.
echo Press any key to continue...
pause >nul 