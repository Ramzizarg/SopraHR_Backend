@echo off
echo Creating databases for microservices architecture...
echo.

REM Check if MySQL is accessible
mysql --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: MySQL is not accessible. Please make sure MySQL is installed and in your PATH.
    echo You can also run the SQL script manually in MySQL Workbench or phpMyAdmin.
    pause
    exit /b 1
)

echo Creating databases...
mysql -u root -p < create_databases.sql

if errorlevel 1 (
    echo.
    echo ERROR: Failed to create databases. Please check your MySQL credentials.
    echo You can run the SQL script manually in MySQL Workbench or phpMyAdmin.
    echo.
    echo SQL script location: create_databases.sql
) else (
    echo.
    echo SUCCESS: All databases created successfully!
    echo.
    echo Created databases:
    echo - analytics_db
    echo - contactdb
    echo - notification_db
    echo - planningdb
    echo - reservationdb
    echo - teletravaildb
    echo - SopraHR
)

pause 