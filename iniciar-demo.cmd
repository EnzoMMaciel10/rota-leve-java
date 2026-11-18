@echo off
setlocal
pushd "%~dp0"
echo Iniciando Rota Leve em modo de demonstracao...
echo Depois de iniciar, abra http://localhost:8080/swagger-ui/index.html
call mvnw.cmd spring-boot:run
popd
pause
