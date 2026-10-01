@echo off
setlocal
where mvn >nul 2>&1
if errorlevel 1 (
  echo Maven non trovato. Installa Maven oppure apri il progetto in IntelliJ IDEA come progetto Maven.
  exit /b 1
)
mvn clean package
if errorlevel 1 exit /b 1
echo.
echo Build completata. JAR: target\XeonKitPvP-1.0.0.jar
