@echo off
title MyGarage — Health Check
echo ============================================================
echo  MyGarage Health Check
echo ============================================================
echo.
echo Checking http://localhost:8080/actuator/health ...
echo.
curl -s http://localhost:8080/actuator/health
echo.
echo.
echo Checking http://localhost:8080/ ...
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:8080/
echo.
pause
