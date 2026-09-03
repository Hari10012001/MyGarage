@echo off
title MyGarage — Stop Application
echo Stopping MyGarage (port 8080)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Killing PID: %%a
    taskkill /F /PID %%a
    echo Application stopped.
    goto :done
)
echo No process found on port 8080.
:done
pause
