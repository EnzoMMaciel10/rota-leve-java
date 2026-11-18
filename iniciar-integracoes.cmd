@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\iniciar-integracoes.ps1" %*
set "ROTALEVE_EXIT=%ERRORLEVEL%"
if not "%ROTALEVE_EXIT%"=="0" pause
exit /b %ROTALEVE_EXIT%
