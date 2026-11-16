@echo off
rem Double-click launcher for install.ps1 (builds and installs TUMBA AI).
setlocal
set "SCRIPT=%~dp0install.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
set "CODE=%ERRORLEVEL%"
if not "%~1"=="" exit /b %CODE%
echo.
pause
exit /b %CODE%