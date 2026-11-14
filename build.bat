@echo off
chcp 65001 > nul
cd /d "%~dp0"
echo Сборка MiniDB с запуском тестов...

if exist "mvnw.cmd" goto use_mvnw
if exist "maven\apache-maven-3.9.6\bin\mvn.cmd" goto use_local_maven
goto use_global_maven

:use_mvnw
call mvnw.cmd clean package
goto copy_data

:use_local_maven
call maven\apache-maven-3.9.6\bin\mvn.cmd clean package
goto copy_data

:use_global_maven
call mvn clean package
goto copy_data

:copy_data
if not exist "target\data" mkdir "target\data"
xcopy /E /I /Y "data" "target\data" > nul
echo Готово. Исполняемый файл: target\minidb-1.0.0.jar
pause
