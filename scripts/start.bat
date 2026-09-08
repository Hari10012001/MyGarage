@echo off
title MyGarage — Start Application
color 0B

echo ============================================================
echo  MyGarage — Starting Application
echo  Project: APPJFS19
echo ============================================================
echo.

REM Prompt for MySQL password if not already set in environment
if "%DB_PASSWORD%"=="" (
    for /f "usebackq delims=" %%p in (`powershell -NoProfile -Command "if([Console]::IsInputRedirected){$p=[Console]::ReadLine()}else{$p=[System.Net.NetworkCredential]::new('',(Read-Host -Prompt 'Enter MySQL Root Password (press Enter if none)' -AsSecureString)).Password}; [Console]::Out.Write($p)"`) do set "DB_PASSWORD=%%p"
)
if "%DB_USERNAME%"=="" set DB_USERNAME=root

echo.
echo  URL: http://localhost:8080
echo  Admin: admin@mygarage.com / Admin@123
echo.
echo  Press Ctrl+C to stop.
echo ============================================================
echo.

REM Asynchronously open default browser once application is ready on port 8080
start /b "" powershell -NoProfile -Command "for ($i=0; $i -lt 90; $i++) { try { (New-Object System.Net.Sockets.TcpClient('127.0.0.1', 8080)).Close(); Start-Process 'http://localhost:8080'; break } catch { Start-Sleep -Seconds 1 } }"

cd /d "%~dp0..\backend"
call mvn spring-boot:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Application failed to start. See error above.
    pause
)
