@echo off
title MyGarage — Start Application
color 0B

echo ============================================================
echo  MyGarage — Starting Application
echo  Project: APPJFS19
echo ============================================================
echo.
echo  URL: http://localhost:8080
echo  Admin: admin@mygarage.com / Admin@123
echo.
echo  Press Ctrl+C to stop.
echo ============================================================
echo.

cd /d "%~dp0..\backend"
call mvn spring-boot:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAIL] Application failed to start. See error above.
    pause
)
