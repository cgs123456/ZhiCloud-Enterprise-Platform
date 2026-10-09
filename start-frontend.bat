@echo off
setlocal
set "FRONTEND_DIR=%~dp0zhicloud-ui\zhicloud-ui-admin-vue3"

if not exist "%FRONTEND_DIR%\package.json" (
  echo [ERROR] Frontend package.json not found: "%FRONTEND_DIR%\package.json"
  exit /b 1
)

cd /d "%FRONTEND_DIR%"
where pnpm >nul 2>nul
if errorlevel 1 (
  echo [ERROR] pnpm not found. Install Node.js ^>=20.19.0 and pnpm ^>=8.6.0 first.
  exit /b 1
)

if not exist "node_modules\vite\bin\vite.js" (
  echo [INFO] Frontend dependencies missing, running pnpm install...
  call pnpm install
  if errorlevel 1 exit /b 1
)

echo [INFO] Starting frontend on http://localhost:80 (proxy /admin-api -^> http://127.0.0.1:48080)
call pnpm dev > "%~dp0frontend.log" 2>&1
