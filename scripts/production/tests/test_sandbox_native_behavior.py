"""Behavioral test for the shared native-process executor (scripts/production/SandboxNative.ps1).

Why this exists next to the structural seam guards: `test_sandbox_native_seam.py` can prove that
every sandbox script *routes* through the executor, but routing to a broken executor is still
broken. Two defects in this family were only ever caught by actually running things — the
array-splat that made every call fail instantly, and a timeout that killed only the direct child
and left `ping`/`java` grandchildren holding the output pipe. Those cannot be seen by reading.

The harness is generated into a temp directory and written **with a UTF-8 BOM**: Windows PowerShell
5.1 decodes a BOM-less file as ANSI, which turns any non-ASCII comment into a parse error.
"""

import os
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

PRODUCTION_DIR = Path(__file__).resolve().parent.parent
MODULE = PRODUCTION_DIR / "SandboxNative.ps1"

EXPECTED_RESULTS = (
    "stdout-captured",
    "exitcode-7",
    "streams-split",
    "stdin-read",
    "stdin-utf8-roundtrip",
    "stdin-write-failure-not-masked",
    "timeout-throws",
    "timeout-reports-parent-exited",
    "timeout-log-marked",
    "grandchild-was-writing-heartbeat",
    "process-tree-killed",
    "blank-filepath-rejected",
    "working-directory",
)

# 需要外部工具的检查项：前置条件缺失时探针会报 SKIP，而 SKIP 绝不能算通过，
# 所以是否"必须出现"由 Python 侧用同一个前置条件决定。
PYTHON_ONLY_RESULTS = (
    "arg-space",
    "arg-trailing-backslash",
    "arg-empty",
    "batch-args-and-stdin",
    "batch-exitcode-propagates",
)


def expected_results():
    names = list(EXPECTED_RESULTS)
    if shutil.which('python'):
        names += list(PYTHON_ONLY_RESULTS)
    maven = shutil.which('mvn')
    if maven and maven.lower().endswith(('.cmd', '.bat')):
        names.append('batch-mvn-runs')
    return names

HARNESS = r'''
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
. ('%MODULE%')
$script:fails = 0
function Check($name, $cond, $detail) {
    if ($cond) { Write-Output "RESULT $name PASS" }
    else { Write-Output "RESULT $name FAIL $detail"; $script:fails++ }
}
function Skip($name, $detail) {
    # SKIP 绝不能写成 PASS：前置条件缺失时冒充通过，就是一个恒绿守卫。
    Write-Output "RESULT $name SKIP $detail"
}

$run = Invoke-BoundedNative -FilePath 'cmd.exe' -Arguments @('/c', 'echo', 'hello') -TimeoutSeconds 30 -Stage 't'
Check 'stdout-captured' ($run.ExitCode -eq 0 -and $run.StdOut -eq 'hello') "code=$($run.ExitCode) out=[$($run.StdOut)]"
$run = Invoke-BoundedNative -FilePath 'cmd.exe' -Arguments @('/c', 'exit', '7') -TimeoutSeconds 30 -Stage 't'
Check 'exitcode-7' ($run.ExitCode -eq 7) "code=$($run.ExitCode)"
$run = Invoke-BoundedNative -FilePath 'cmd.exe' -Arguments @('/c', 'echo OUTLINE & echo ERRLINE 1>&2') -TimeoutSeconds 30 -Stage 't'
Check 'streams-split' ($run.StdOut -eq 'OUTLINE' -and $run.StdErr -eq 'ERRLINE') "out=[$($run.StdOut)] err=[$($run.StdErr)]"

# Argument fidelity is judged by python, which parses the command line with CommandLineToArgvW
# exactly like mysql/java/docker do. A .cmd probe would be re-parsed by cmd and lie about it.
$python = (Get-Command python -ErrorAction SilentlyContinue)
if ($python) {
    $snippet = 'import sys;[print("ARG="+repr(a)) for a in sys.argv[1:]]'
    $run = Invoke-BoundedNative -FilePath $python.Source -Arguments @('-c', $snippet, 'a b', 'C:\x y\', '') -TimeoutSeconds 60 -Stage 't'
    $lines = @($run.StdOut -split "\r?\n" | Where-Object { $_ -ne '' })
    $q = [char]39
    Check 'arg-space' ($lines.Count -eq 3 -and $lines[0] -eq ('ARG=' + $q + 'a b' + $q)) "lines=$($lines -join '|')"
    Check 'arg-trailing-backslash' ($lines.Count -eq 3 -and $lines[1] -eq ('ARG=' + $q + 'C:\\x y\\' + $q)) "lines=$($lines -join '|')"
    Check 'arg-empty' ($lines.Count -eq 3 -and $lines[2] -eq ('ARG=' + $q + $q)) "lines=$($lines -join '|')"
} else {
    Skip 'arg-space' 'python missing'
    Skip 'arg-trailing-backslash' 'python missing'
    Skip 'arg-empty' 'python missing'
}

$run = Invoke-BoundedNative -FilePath 'findstr.exe' -Arguments @('needle') -StandardInput "x needle y" -TimeoutSeconds 30 -Stage 't'
Check 'stdin-read' ($run.ExitCode -eq 0 -and $run.StdOut -match 'needle') "code=$($run.ExitCode) out=[$($run.StdOut)]"

# stdin 的编码必须显式钉成 UTF-8。三条流里只钉输出是不够的：
# 物理复制门禁就是因此把含中文注释的迁移 SQL 写成 GBK，服务端读到乱码后报 1064。
if ($python) {
    $probe = 'import sys' + [char]10 + 'data = sys.stdin.buffer.read()' + [char]10 + 'print("BYTES=" + str(len(data)))' + [char]10 + 'try:' + [char]10 + '    text = data.decode("utf-8")' + [char]10 + '    sys.stdout.buffer.write(("DECODED=" + text.strip() + "\n").encode("utf-8")); sys.stdout.buffer.flush()' + [char]10 + 'except UnicodeDecodeError as e:' + [char]10 + '    print("NOT-UTF8 " + str(e))'
    $run = Invoke-BoundedNative -FilePath $python.Source -Arguments @('-c', $probe) -StandardInput "状态 NEW/PUBLISHING" -TimeoutSeconds 60 -Stage 't'
    Check 'stdin-utf8-roundtrip' ($run.ExitCode -eq 0 -and $run.StdOut -match 'DECODED=状态 NEW/PUBLISHING' -and $run.StdOut -match 'BYTES=21') "out=[$($run.StdOut)] err=[$($run.StdErr)]"
} else {
    Skip 'stdin-utf8-roundtrip' 'python missing'
}
$threw = $false; $code = -1
try { $run = Invoke-BoundedNative -FilePath 'cmd.exe' -Arguments @('/c', 'exit', '3') -StandardInput 'ignored' -TimeoutSeconds 30 -Stage 't'; $code = $run.ExitCode } catch { $threw = $true }
Check 'stdin-write-failure-not-masked' ((-not $threw) -and $code -eq 3) "threw=$threw code=$code"

# 进程树是否真的死了：心跳必须由**孙进程**写。
# 如果心跳是父进程自己写的，那么"只杀直接子进程"的错误实现也能让心跳停下——
# 第一版就是这样：去掉 taskkill 的 /T 之后用例仍然全绿，守的其实是"父进程死了"。
# 这里父进程只负责挂住，写心跳的是它 start 出来的孙进程；两者不同命，才能证明 /T 生效。
$hb = Join-Path $env:TEMP ('sbx-hb-' + (Get-Date -Format 'HHmmssfff') + '.txt')
$loop = Join-Path $env:TEMP ('sbx-loop-' + (Get-Date -Format 'HHmmssfff') + '.cmd')
$batch = @(
    '@echo off'
    'if "%~2"=="child" goto child'
    'start "" /b cmd /c ""%~f0" "%~1" child"'
    ':hold'
    'ping -n 30 127.0.0.1 >nul'
    'goto hold'
    ':child'
    'echo tick>>"%~1"'
    'ping -n 2 127.0.0.1 >nul'
    'goto child'
    ''
) -join "`r`n"
[System.IO.File]::WriteAllText($loop, $batch, [System.Text.UTF8Encoding]::new($false))
$log = Join-Path $env:TEMP ('sbx-timeout-' + (Get-Date -Format 'HHmmssfff') + '.log')
$message = ''
$started = Get-Date
try { $null = Invoke-BoundedNative -FilePath $loop -Arguments @($hb) -TimeoutSeconds 6 -Stage 't' -LogPath $log } catch { $message = $_.Exception.Message }
$elapsed = [Math]::Round(((Get-Date) - $started).TotalSeconds, 1)
Check 'timeout-throws' ($message -match '6' -and $elapsed -lt 25) "msg=$message elapsed=$elapsed"
# 只断言"父进程确认已退出"这一观察事实。taskkill 的退出码不能当判据：
# 树里有任何一个进程刚好自己退出，它就返回非 0，本仓库因此抖过一次误报。
Check 'timeout-reports-parent-exited' ($message -match 'parent_exited=True') "msg=$message"
$logText = ''
if (Test-Path -LiteralPath $log) { $logText = [System.IO.File]::ReadAllText($log); Remove-Item -LiteralPath $log -Force }
Check 'timeout-log-marked' ($logText -match '\[timeout\]') "log=[$logText]"
Start-Sleep -Milliseconds 800
$heartbeatFirst = 0
if (Test-Path -LiteralPath $hb) { $heartbeatFirst = (Get-Item -LiteralPath $hb).Length }
Check 'grandchild-was-writing-heartbeat' ($heartbeatFirst -gt 0) "bytes=$heartbeatFirst"
Start-Sleep -Seconds 6
$heartbeatAfter = 0
if (Test-Path -LiteralPath $hb) { $heartbeatAfter = (Get-Item -LiteralPath $hb).Length }
Check 'process-tree-killed' ($heartbeatFirst -eq $heartbeatAfter) `
    "超时后孙进程仍在写心跳：$heartbeatFirst -> $heartbeatAfter 字节（进程树没杀干净）"
Remove-Item -LiteralPath $hb, $loop -Force -ErrorAction SilentlyContinue

$threw = $false
try { $null = Invoke-BoundedNative -FilePath '  ' -Arguments @() -TimeoutSeconds 5 -Stage 't' } catch { $threw = $true }
Check 'blank-filepath-rejected' $threw 'no throw'
$run = Invoke-BoundedNative -FilePath 'cmd.exe' -Arguments @('/c', 'cd') -TimeoutSeconds 30 -Stage 't' -WorkingDirectory 'C:\Windows'
Check 'working-directory' ($run.StdOut.Trim() -ieq 'C:\Windows') "cwd=[$($run.StdOut.Trim())]"

# Batch carriers: CreateProcess cannot launch .cmd/.bat directly, so the executor has to hand the
# whole thing to cmd.exe. That hop is where arguments, stdin and exit codes can silently get lost,
# so probe it with a real batch file rather than trusting the mvn smoke test alone.
if ($python) {
    $batch = Join-Path $env:TEMP ('sbx-probe-' + (Get-Date -Format 'HHmmssfff') + '.cmd')
    # 在 PowerShell 单引号串里 '' 才表示一个单引号；批处理内层交给 python 的字符串必须带单引号。
    $body = '@echo off' + "`r`n" + '"' + $python.Source + '" -c "import sys;[print(''ARG='' + repr(a)) for a in sys.argv[1:]];print(''STDIN='' + repr(sys.stdin.read()))" %*' + "`r`n" + 'exit /b 9' + "`r`n"
    [System.IO.File]::WriteAllText($batch, $body, [System.Text.UTF8Encoding]::new($false))
    $run = Invoke-BoundedNative -FilePath $batch -Arguments @('a b', '') -StandardInput 'piped' -TimeoutSeconds 60 -Stage 't'
    Check 'batch-args-and-stdin' ($run.StdOut -match "ARG='a b'" -and $run.StdOut -match "ARG=''" -and $run.StdOut -match "STDIN='piped'") "out=[$($run.StdOut)]"
    Check 'batch-exitcode-propagates' ($run.ExitCode -eq 9) "code=$($run.ExitCode)"
    Remove-Item -LiteralPath $batch -Force -ErrorAction SilentlyContinue
} else {
    Skip 'batch-args-and-stdin' 'python missing'
    Skip 'batch-exitcode-propagates' 'python missing'
}

# Batch carriers: mvn on Windows is mvn.cmd, which CreateProcess cannot launch directly.
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn -and $mvn.Source -match '\.(cmd|bat)$') {
    $run = Invoke-BoundedNative -FilePath $mvn.Source -Arguments @('-v') -TimeoutSeconds 180 -Stage 't'
    Check 'batch-mvn-runs' ($run.ExitCode -eq 0 -and $run.StdOut -match 'Apache Maven') "code=$($run.ExitCode)"
}
Write-Output "FAILS $script:fails"
Write-Output 'HARNESS-DONE'
'''


def windows_powershell_hosts():
    """All usable hosts, deduplicated by resolved path (PATH entry vs. System32 are the same exe)."""
    candidates = []
    system32 = Path(os.environ.get('SystemRoot') or r'C:\Windows')
    candidates.append(system32 / 'WindowsPowerShell' / 'v1.0' / 'powershell.exe')
    candidates.append(shutil.which('powershell'))
    candidates.append(shutil.which('pwsh'))
    hosts = []
    seen = set()
    for candidate in candidates:
        if not candidate:
            continue
        path = Path(candidate)
        if not path.is_file():
            continue
        resolved = path.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        label = 'pwsh-7' if path.name.lower().startswith('pwsh') else 'powershell-5.1'
        hosts.append((label, str(path)))
    return hosts


class SandboxNativeBehaviorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not sys.platform.startswith('win'):
            raise unittest.SkipTest('the native executor is exercised on Windows hosts only')
        if not MODULE.is_file():
            raise unittest.SkipTest(f'missing {MODULE}')

    def _run_harness(self, host_exe, label):
        script_text = HARNESS.replace('%MODULE%', str(MODULE).replace("'", "''"))
        with tempfile.TemporaryDirectory() as tmp:
            script = Path(tmp) / 'harness.ps1'
            # BOM 必带：Windows PowerShell 5.1 对无 BOM 文件按 ANSI 解码，
            # 中文注释会变成解析错误（本仓库已为此踩过一次）。
            script.write_text(script_text, encoding='utf-8-sig', newline='\r\n')
            completed = subprocess.run(
                [host_exe, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', str(script)],
                capture_output=True, text=True, encoding='utf-8', errors='replace',
                timeout=900, check=False)
        lines = (completed.stdout or '').splitlines()
        results = {}
        for line in lines:
            if line.startswith('RESULT '):
                parts = line.split(' ', 3)
                results[parts[1]] = (parts[2], parts[3] if len(parts) > 3 else '')
        self.assertIn('HARNESS-DONE', lines,
                      f'{label} harness died mid-run (exit {completed.returncode}):\n'
                      f'{completed.stdout}\n{completed.stderr}')
        return results

    def _assert_results(self, results, label):
        expected = expected_results()
        self.assertEqual([], [name for name in expected if name not in results],
                         f'{label} harness never reported every check')
        # SKIP 与 FAIL 同等对待：前置条件在 Python 侧已判定存在，探针却报 SKIP
        # 说明两侧条件不一致，那正是一个会长期恒绿的口子。
        not_passing = {name: f'{status} {detail}'.strip()
                       for name, (status, detail) in results.items() if status != 'PASS'}
        self.assertEqual({}, not_passing, f'{label} reported non-PASS results')

    def test_executor_behaves_on_windows_powershell_51(self):
        hosts = {label: exe for label, exe in windows_powershell_hosts()
                 if label.startswith('powershell')}
        self.assertTrue(hosts, 'Windows PowerShell is not available to run the harness')
        for label, exe in hosts.items():
            with self.subTest(host=label):
                self._assert_results(self._run_harness(exe, label), label)

    def test_executor_behaves_on_powershell_7(self):
        exe = shutil.which('pwsh')
        if not exe:
            self.skipTest('pwsh is not on PATH (stale session PATH does not count)')
        self._assert_results(self._run_harness(exe, 'pwsh-7'), 'pwsh-7')


if __name__ == '__main__':
    unittest.main()
