@echo off
set JAR=target\lamport-algorithm-1.0.0.jar
if not exist %JAR% (
    echo Compilando projeto...
    call mvn -q package
)

echo Iniciando P1 (porta 5001)...
start "Lamport P1" cmd /k java -jar %JAR% --id 1 --port 5001 --peers 2:127.0.0.1:5002,3:127.0.0.1:5003

timeout /t 2 /nobreak > nul

echo Iniciando P2 (porta 5002)...
start "Lamport P2" cmd /k java -jar %JAR% --id 2 --port 5002 --peers 1:127.0.0.1:5001,3:127.0.0.1:5003

timeout /t 2 /nobreak > nul

echo Iniciando P3 (porta 5003)...
start "Lamport P3" cmd /k java -jar %JAR% --id 3 --port 5003 --peers 1:127.0.0.1:5001,2:127.0.0.1:5002

echo.
echo Tres processos iniciados. Em cada janela, use:
echo   send Mensagem de teste
echo   state
echo   quit
