@echo off
title MyGarage — Run Tests
color 0E

echo ============================================================
echo  MyGarage — Running Tests
echo  Project: APPJFS19
echo ============================================================
echo.

cd /d "%~dp0..\backend"
call mvn test "-Dspring.profiles.active=test"
echo.
if %ERRORLEVEL% EQU 0 (
    echo [PASS] All tests passed!
) else (
    echo [FAIL] Some tests failed. See output above.
)
echo.
pause
