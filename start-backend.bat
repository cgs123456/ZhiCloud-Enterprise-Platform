@echo off
setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"

if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  for /f "delims=" %%i in ('where java 2^>nul') do set "JAVA_EXE=%%i"
)
if not defined JAVA_EXE (
  echo [ERROR] Java 21 not found. Set JAVA_HOME or add java.exe to PATH.
  exit /b 1
)
if not exist "%JAVA_EXE%" (
  echo [ERROR] Java executable not found: "%JAVA_EXE%"
  exit /b 1
)

if not exist "zhicloud-server\target\zhicloud-server.jar" (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo [ERROR] Backend jar missing and Maven not found. Add mvn to PATH first.
    exit /b 1
  )
  echo [INFO] Backend jar missing, building zhicloud-server with local reactor dependencies...
  call mvn -B -DskipTests -pl zhicloud-server -am clean package
  if errorlevel 1 exit /b 1
)

echo [INFO] Starting backend on http://localhost:48080
"%JAVA_EXE%" -Xms512m -Xmx3g -Dfile.encoding=UTF-8 -jar "zhicloud-server\target\zhicloud-server.jar" --spring.profiles.active=local > "%ROOT%server.log" 2>&1
