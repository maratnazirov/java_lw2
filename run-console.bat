@echo off
chcp 65001 > nul
cd /d "%~dp0"

if not exist "target\minidb-1.0.0.jar" (
    echo [MiniDB] Исполняемый файл не найден. Выполняется автоматическая сборка...
    call mvnw.cmd package -DskipTests
    if not exist "target\data" mkdir "target\data"
    xcopy /E /I /Y "data" "target\data" > nul
)

echo Запуск MiniDB в консольном режиме...
java -jar "target\minidb-1.0.0.jar" "data"
pause
