@echo off
REM ZhiCloud inventory reconcile alert - daily scheduled task
REM Usage: run this script once as Administrator to register the task
REM Requires: ZC_RECONCILE_TOKEN set in system/user environment variables

set SCRIPT_DIR=%~dp0
set LOG_FILE=%SCRIPT_DIR%reconcile_alert.jsonl

schtasks /Create /F /TN "ZhiCloud_ReconcileAlert" /TR "cmd /c set ZC_RECONCILE_LOG_FILE=%LOG_FILE%&& python \"%SCRIPT_DIR%push_inventory_reconcile_alert.py\" --base-url http://127.0.0.1:48080/admin-api --tenant-id 1 --log-file \"%LOG_FILE%\"" /SC DAILY /ST 08:00 /RL HIGHEST

if %errorlevel% equ 0 (
    echo Task registered successfully. Schedule: daily at 08:00.
    echo Log file: %LOG_FILE%
    echo Token: read from ZC_RECONCILE_TOKEN env var (must be set system-wide).
) else (
    echo Failed to register task. Run as Administrator.
)
pause