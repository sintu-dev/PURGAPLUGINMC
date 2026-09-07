@echo off
echo =======================================
echo Compilando PurgaPlugin...
echo =======================================
powershell -ExecutionPolicy Bypass -File "%~dp0compile.ps1"
echo.
pause
