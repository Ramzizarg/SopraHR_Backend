# Workstation AI Decision System - PowerShell Launcher
# Run as Administrator if needed for port access

param(
    [switch]$Install,
    [switch]$Update,
    [switch]$Help
)

# Colors for output
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Blue"
$Cyan = "Cyan"

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Show-Banner {
    Clear-Host
    Write-ColorOutput "========================================" $Cyan
    Write-ColorOutput "    Workstation AI Decision System" $Cyan
    Write-ColorOutput "========================================" $Cyan
    Write-Host ""
}

function Test-PythonInstallation {
    Write-ColorOutput "Checking Python installation..." $Blue
    
    try {
        $pythonVersion = python --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✓ Python found: $pythonVersion" $Green
            return $true
        }
    }
    catch {
        Write-ColorOutput "✗ Python not found in PATH" $Red
        return $false
    }
    
    Write-ColorOutput "✗ Python not found in PATH" $Red
    Write-ColorOutput "Please install Python 3.8+ from https://python.org" $Yellow
    return $false
}

function Install-Dependencies {
    Write-ColorOutput "Installing Python dependencies..." $Blue
    
    try {
        pip install -r requirements.txt
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "✓ Dependencies installed successfully" $Green
            return $true
        }
        else {
            Write-ColorOutput "✗ Failed to install dependencies" $Red
            return $false
        }
    }
    catch {
        Write-ColorOutput "✗ Error installing dependencies: $($_.Exception.Message)" $Red
        return $false
    }
}

function Test-Dependencies {
    Write-ColorOutput "Checking dependencies..." $Blue
    
    $requiredPackages = @("pandas", "fastapi", "dash")
    $missingPackages = @()
    
    foreach ($package in $requiredPackages) {
        try {
            $null = python -c "import $package" 2>$null
            if ($LASTEXITCODE -eq 0) {
                Write-ColorOutput "✓ $package" $Green
            }
            else {
                Write-ColorOutput "✗ $package" $Red
                $missingPackages += $package
            }
        }
        catch {
            Write-ColorOutput "✗ $package" $Red
            $missingPackages += $package
        }
    }
    
    return $missingPackages.Count -eq 0
}

function Test-Ports {
    Write-ColorOutput "Checking port availability..." $Blue
    
    $ports = @(8000, 8050)
    $availablePorts = @()
    
    foreach ($port in $ports) {
        try {
            $connection = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
            if ($connection.TcpTestSucceeded) {
                Write-ColorOutput "✗ Port $port is already in use" $Red
            }
            else {
                Write-ColorOutput "✓ Port $port is available" $Green
                $availablePorts += $port
            }
        }
        catch {
            Write-ColorOutput "✓ Port $port is available" $Green
            $availablePorts += $port
        }
    }
    
    return $availablePorts.Count -eq $ports.Count
}

function Start-AIServices {
    Write-ColorOutput "Starting AI Decision System..." $Blue
    
    # Create logs directory
    if (!(Test-Path "logs")) {
        New-Item -ItemType Directory -Path "logs" | Out-Null
        Write-ColorOutput "✓ Created logs directory" $Green
    }
    
    # Start API Server
    Write-ColorOutput "Starting AI API Server (Port 8000)..." $Yellow
    $apiJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        python dashboard_api.py
    } -Name "AI-API-Server"
    
    # Wait for API to start
    Write-ColorOutput "Waiting for API server to start..." $Yellow
    Start-Sleep -Seconds 10
    
    # Test API health
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8000/api/health" -Method Get -TimeoutSec 5
        if ($response.status -eq "healthy") {
            Write-ColorOutput "✓ API Server started successfully" $Green
        }
        else {
            Write-ColorOutput "⚠ API Server status: $($response.status)" $Yellow
        }
    }
    catch {
        Write-ColorOutput "⚠ API Server may not be fully started yet" $Yellow
    }
    
    # Start Dashboard
    Write-ColorOutput "Starting AI Dashboard (Port 8050)..." $Yellow
    $dashboardJob = Start-Job -ScriptBlock {
        Set-Location $using:PWD
        python visualization_dashboard.py
    } -Name "AI-Dashboard"
    
    # Wait for dashboard to start
    Start-Sleep -Seconds 5
    
    return @{
        APIJob = $apiJob
        DashboardJob = $dashboardJob
    }
}

function Show-Status {
    Write-Host ""
    Write-ColorOutput "========================================" $Cyan
    Write-ColorOutput "    AI System Started Successfully!" $Cyan
    Write-ColorOutput "========================================" $Cyan
    Write-Host ""
    Write-ColorOutput "🌐 API Documentation: http://localhost:8000/docs" $Blue
    Write-ColorOutput "📊 Dashboard: http://localhost:8050" $Blue
    Write-ColorOutput "❤️  Health Check: http://localhost:8000/api/health" $Blue
    Write-Host ""
    Write-ColorOutput "Press Ctrl+C to stop all services" $Yellow
    Write-Host ""
}

function Open-Dashboard {
    Write-ColorOutput "Opening dashboard in browser..." $Blue
    try {
        Start-Process "http://localhost:8050"
        Write-ColorOutput "✓ Dashboard opened in browser" $Green
    }
    catch {
        Write-ColorOutput "⚠ Could not open browser automatically" $Yellow
        Write-ColorOutput "Please open: http://localhost:8050" $Blue
    }
}

function Stop-Services {
    Write-ColorOutput "Stopping AI services..." $Yellow
    
    Get-Job -Name "AI-*" | Stop-Job
    Get-Job -Name "AI-*" | Remove-Job
    
    Write-ColorOutput "✓ All services stopped" $Green
}

function Show-Help {
    Write-ColorOutput "Workstation AI Decision System - PowerShell Launcher" $Cyan
    Write-Host ""
    Write-ColorOutput "Usage:" $Yellow
    Write-ColorOutput "  .\start_ai_system.ps1              - Start the AI system" $White
    Write-ColorOutput "  .\start_ai_system.ps1 -Install      - Install dependencies only" $White
    Write-ColorOutput "  .\start_ai_system.ps1 -Update       - Update dependencies" $White
    Write-ColorOutput "  .\start_ai_system.ps1 -Help         - Show this help" $White
    Write-Host ""
    Write-ColorOutput "Requirements:" $Yellow
    Write-ColorOutput "  - Python 3.8+" $White
    Write-ColorOutput "  - Windows PowerShell 5.1+" $White
    Write-ColorOutput "  - Internet connection (for first run)" $White
    Write-Host ""
}

# Main execution
Show-Banner

if ($Help) {
    Show-Help
    exit 0
}

# Check if running as administrator (optional)
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")
if (!$isAdmin) {
    Write-ColorOutput "⚠ Running without administrator privileges" $Yellow
    Write-ColorOutput "Some features may require admin rights" $Yellow
    Write-Host ""
}

# Check Python installation
if (!(Test-PythonInstallation)) {
    Write-ColorOutput "Please install Python and try again" $Red
    exit 1
}

# Install or update dependencies
if ($Install -or $Update) {
    if (!(Install-Dependencies)) {
        Write-ColorOutput "Failed to install dependencies" $Red
        exit 1
    }
    if ($Install) {
        Write-ColorOutput "Installation completed. Run without -Install to start the system." $Green
        exit 0
    }
}

# Check dependencies
if (!(Test-Dependencies)) {
    Write-ColorOutput "Missing dependencies detected" $Red
    Write-ColorOutput "Installing dependencies..." $Yellow
    if (!(Install-Dependencies)) {
        Write-ColorOutput "Failed to install dependencies" $Red
        exit 1
    }
}

# Check ports
if (!(Test-Ports)) {
    Write-ColorOutput "Some required ports are not available" $Red
    Write-ColorOutput "Please close applications using ports 8000 or 8050" $Yellow
    exit 1
}

# Start services
$jobs = Start-AIServices

# Show status
Show-Status

# Open dashboard
Open-Dashboard

# Wait for user to stop
try {
    Write-ColorOutput "AI System is running. Press Ctrl+C to stop..." $Green
    while ($true) {
        Start-Sleep -Seconds 1
        
        # Check if jobs are still running
        $apiJob = Get-Job -Name "AI-API-Server" -ErrorAction SilentlyContinue
        $dashboardJob = Get-Job -Name "AI-Dashboard" -ErrorAction SilentlyContinue
        
        if (!$apiJob -or !$dashboardJob) {
            Write-ColorOutput "⚠ One or more services stopped unexpectedly" $Yellow
            break
        }
    }
}
catch {
    Write-Host ""
    Write-ColorOutput "Stopping services..." $Yellow
}
finally {
    Stop-Services
    Write-ColorOutput "Goodbye!" $Green
} 