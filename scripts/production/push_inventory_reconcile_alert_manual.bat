@echo off
REM ZhiCloud inventory reconcile alert - MANUAL ONLY
REM This file intentionally does NOT register any scheduled task.
REM Double-click or run it manually when an operator wants to push a one-off alert.
REM Required env vars: ZC_RECONCILE_TOKEN and ZC_RECONCILE_WEBHOOK_URL
REM Optional: pass extra arguments such as --dry-run to the Python script.

set SCRIPT_DIR=%~dp0
python "%SCRIPT_DIR%push_inventory_reconcile_alert.py" --base-url http://127.0.0.1:48080/admin-api --tenant-id 1 %*
if errorlevel 1 (
    echo Manual reconcile alert push failed. Check the output above.
) else (
    echo Manual reconcile alert push finished.
)
pause
