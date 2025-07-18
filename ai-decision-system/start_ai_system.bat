@echo off
echo ========================================
echo    Workstation AI Decision System
echo ========================================
echo.

echo Starting AI Decision System...
echo.

REM Check if Python is installed
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.8+ and try again
    pause
    exit /b 1
)

REM Check if requirements are installed
echo Checking dependencies...
pip show pandas >nul 2>&1
if errorlevel 1 (
    echo Installing dependencies...
    pip install -r requirements.txt
    if errorlevel 1 (
        echo ERROR: Failed to install dependencies
        pause
        exit /b 1
    )
)

REM Create logs directory if it doesn't exist
if not exist "logs" mkdir logs

echo.
echo Starting AI API Server (Port 8000)...
start "AI API Server" cmd /k "python dashboard_api.py"

echo Waiting for API server to start...
timeout /t 5 /nobreak >nul

echo.
echo Starting AI Dashboard (Port 8050)...
start "AI Dashboard" cmd /k "python visualization_dashboard.py"

echo.
echo ========================================
echo    AI System Started Successfully!
echo ========================================
echo.
echo API Documentation: http://localhost:8000/docs
echo Dashboard: http://localhost:8050
echo Health Check: http://localhost:8000/api/health
echo.
echo Press any key to open the dashboard...
pause >nul

REM Open dashboard in default browser
start http://localhost:8050

echo.
echo AI System is running in background.
echo Close the command windows to stop the services.
echo.
pause 