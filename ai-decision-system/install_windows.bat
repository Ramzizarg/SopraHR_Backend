@echo off
chcp 65001 >nul
echo ========================================
echo    Workstation AI Decision System
echo    Windows Installation & Setup
echo ========================================
echo.

echo 🚀 Starting AI System Setup...
echo.

REM Check if Python is installed
echo 📋 Checking Python installation...
python --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: Python is not installed or not in PATH
    echo.
    echo 📥 Please install Python 3.8+ from: https://python.org
    echo 💡 Make sure to check "Add Python to PATH" during installation
    echo.
    pause
    exit /b 1
)

echo ✅ Python found
python --version

REM Check if pip is available
echo.
echo 📦 Checking pip...
pip --version >nul 2>&1
if errorlevel 1 (
    echo ❌ ERROR: pip is not available
    echo 💡 Please reinstall Python with pip included
    pause
    exit /b 1
)

echo ✅ pip found

REM Create virtual environment (optional but recommended)
echo.
echo 🔧 Creating virtual environment...
if not exist "venv" (
    python -m venv venv
    echo ✅ Virtual environment created
) else (
    echo ✅ Virtual environment already exists
)

REM Activate virtual environment
echo.
echo 🔄 Activating virtual environment...
call venv\Scripts\activate.bat

REM Upgrade pip
echo.
echo ⬆️ Upgrading pip...
python -m pip install --upgrade pip

REM Install dependencies
echo.
echo 📥 Installing dependencies...
pip install -r requirements.txt
if errorlevel 1 (
    echo ❌ ERROR: Failed to install dependencies
    echo 💡 Try running as Administrator or check your internet connection
    pause
    exit /b 1
)

echo ✅ Dependencies installed successfully

REM Create logs directory
if not exist "logs" mkdir logs
echo ✅ Logs directory ready

REM Create .env file if it doesn't exist
if not exist ".env" (
    echo.
    echo ⚙️ Creating configuration file...
    copy .env.example .env
    echo ✅ Configuration file created (.env)
    echo 💡 Please edit .env file with your database settings
)

echo.
echo ========================================
echo    ✅ Setup Completed Successfully!
echo ========================================
echo.
echo 🎯 Next steps:
echo    1. Edit .env file with your database settings
echo    2. Run: start_ai_system.bat
echo    3. Or run: start_ai_system.ps1 (PowerShell)
echo.
echo 🌐 Access URLs:
echo    - API Docs: http://localhost:8000/docs
echo    - Dashboard: http://localhost:8050
echo    - Health: http://localhost:8000/api/health
echo.
pause 