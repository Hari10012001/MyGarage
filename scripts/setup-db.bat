@echo off
title MyGarage — Setup Database
color 0A

echo ============================================================
echo  MyGarage — Database Setup Script
echo  Project: APPJFS19
echo ============================================================
echo.

if "%DB_PASSWORD%"=="" (
    set /p DB_PASSWORD="Enter MySQL Root Password (press Enter if none): "
)
if "%DB_USERNAME%"=="" set DB_USERNAME=root

REM Check MySQL is accessible
echo [1/3] Checking MySQL connection...
mysql -u %DB_USERNAME% -p%DB_PASSWORD% -e "SELECT 1;" > nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [FAIL] Cannot connect to MySQL with user %DB_USERNAME%. 
    echo        Make sure MySQL Server 8.0 is running.
    echo        Update DB_PASSWORD environment variable or application.properties if credentials differ.
    pause
    exit /b 1
)
echo [OK] MySQL connection successful.

echo.
echo [2/3] Creating database 'mygarage_db' (if not exists)...
mysql -u %DB_USERNAME% -p%DB_PASSWORD% -e "CREATE DATABASE IF NOT EXISTS mygarage_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if %ERRORLEVEL% EQU 0 (
    echo [OK] Database 'mygarage_db' is ready.
) else (
    echo [FAIL] Could not create database.
    pause
    exit /b 1
)

echo.
echo [3/3] Database setup complete!
echo       Spring Boot will auto-create all tables on first startup.
echo       Admin account will be seeded via data.sql.
echo.
echo       Credentials: admin@mygarage.com / Admin@123
echo.
echo Press any key to exit...
pause > nul
