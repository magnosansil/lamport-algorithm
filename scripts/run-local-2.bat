@echo off
set JAR=target\lamport-algorithm-1.0.0.jar
if not exist %JAR% (
    echo Compilando projeto...
    call mvn -q package
)

echo Iniciando P1 (porta 5001)...
start "Lamport P1" cmd /k java -jar %JAR% --id 1 --port 5001 --peers 2:127.0.0.1:5002

timeout /t 2 /nobreak > nul

echo Iniciando P2 (porta 5002)...
start "Lamport P2" cmd /k java -jar %JAR% --id 2 --port 5002 --peers 1:127.0.0.1:5001

echo.
echo Dois processos iniciados (minimo para SD). Teste enviando mensagens em ambas janelas.
