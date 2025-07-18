@echo off
echo ========================================
echo   Java Environment Check
echo ========================================
echo.

echo Checking Java installation...
java -version
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 8 or 17
    pause
    exit /b 1
)

echo.
echo Checking Java compiler...
javac -version
if errorlevel 1 (
    echo.
    echo ========================================
    echo   JDK ISSUE DETECTED
    echo ========================================
    echo.
    echo You have Java Runtime Environment (JRE) but not Java Development Kit (JDK)
    echo Maven needs JDK to compile Java code.
    echo.
    echo SOLUTIONS:
    echo.
    echo 1. Install JDK 8 (for current setup):
    echo    - Download from: https://adoptium.net/temurin/releases/?version=8
    echo    - Install and set JAVA_HOME environment variable
    echo.
    echo 2. Install JDK 17 (recommended for Spring Boot 3.x):
    echo    - Download from: https://adoptium.net/temurin/releases/?version=17
    echo    - Install and set JAVA_HOME environment variable
    echo.
    echo 3. Set JAVA_HOME environment variable:
    echo    - Right-click on 'This PC' ^> Properties ^> Advanced system settings
    echo    - Click 'Environment Variables'
    echo    - Add new System Variable: JAVA_HOME = C:\Program Files\Java\jdk-17.x.x
    echo    - Add %JAVA_HOME%\bin to PATH variable
    echo.
    echo After installing JDK, restart your terminal and run this script again.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo   JAVA ENVIRONMENT OK
echo ========================================
echo.
echo Java version: 
java -version
echo.
echo Java compiler version:
javac -version
echo.
echo You can now run the startup script!
echo.
pause 