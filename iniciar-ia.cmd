@echo off
setlocal
call "%~dp0iniciar-integracoes.cmd" -ComIA %*
exit /b %ERRORLEVEL%
