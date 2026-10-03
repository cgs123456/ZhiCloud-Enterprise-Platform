#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$MysqlClient = 'C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 53306,
    [string]$AdminUser = 'root',
    [string]$AdminPasswordEnv,
    [string]$SandboxStateFile,
    [string]$EvidenceDir,
    [int]$HoldSeconds = 8,
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = 'Stop'
# fail-closed 守卫：本脚本会执行 CREATE DATABASE / CREATE USER / GRANT / DROP DATABASE，
# 因此只允许连接回环地址上的沙箱实例。此前 -DatabaseHost 可取任意主机，
# 一旦误指共享库或生产库就会直接造成破坏（登记的 R-G4-1 / R-G3-1 即此）。
$script:SandboxLoopbackHosts = @('127.0.0.1', '::1', 'localhost')
function Assert-LoopbackSandboxHost {
    param([Parameter(Mandatory = $true)][string]$Host_, [string]$Stage = 'startup')
    if ($script:SandboxLoopbackHosts -notcontains $Host_) {
        throw "拒绝在非回环 -DatabaseHost='$Host_'（阶段：$Stage）上执行建库/授权/DROP；沙箱脚本只允许 $($script:SandboxLoopbackHosts -join ', ')。如确需远程实例，请改为显式的远程审批路径，不要放宽本守卫。"
    }
}
Assert-LoopbackSandboxHost -Host_ $DatabaseHost
$ProgressPreference = 'SilentlyContinue'

# 唯一的原生进程执行器（硬超时 + 杀进程树），本脚本的单发查询与并发会话共用同一份实现。
. (Join-Path $PSScriptRoot 'SandboxNative.ps1')

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
} else {
    $RepoRoot = (Resolve-Path $RepoRoot).Path
}
if ([string]::IsNullOrWhiteSpace($EvidenceDir)) {
    $EvidenceDir = Join-Path $RepoRoot 'docs\enterprise-platform\evidence\production'
}
$EvidenceDir = [System.IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null

if (-not (Test-Path -LiteralPath $MysqlClient -PathType Leaf)) {
    throw "mysql 客户端不存在: $MysqlClient"
}
if ($HoldSeconds -lt 3) {
    throw 'HoldSeconds 必须至少为 3'
}

$AdminPassword = ''
$sandboxState = $null
if (-not [string]::IsNullOrWhiteSpace($AdminPasswordEnv)) {
    $AdminPassword = [Environment]::GetEnvironmentVariable($AdminPasswordEnv, 'Process')
    if ([string]::IsNullOrEmpty($AdminPassword)) {
        throw "环境变量 $AdminPasswordEnv 不存在或为空"
    }
}

if (-not [string]::IsNullOrWhiteSpace($SandboxStateFile)) {
    $SandboxStateFile = (Resolve-Path -LiteralPath $SandboxStateFile).Path
    $sandboxState = [System.IO.File]::ReadAllText($SandboxStateFile, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json
    if ([string]::IsNullOrWhiteSpace([string]$sandboxState.container_name) -or [string]$sandboxState.container_name -notmatch '^zhicloud-mysql-sandbox-[0-9a-f]{8}$') {
        throw "沙箱状态文件的 container_name 不合法: $SandboxStateFile"
    }
    if ($null -eq $sandboxState.host_port -or [int]$sandboxState.host_port -le 0) {
        throw "沙箱状态文件缺少有效 host_port: $SandboxStateFile"
    }
    $DatabaseHost = if ([string]::IsNullOrWhiteSpace([string]$sandboxState.bind_address)) { '127.0.0.1' } else { [string]$sandboxState.bind_address }
    Assert-LoopbackSandboxHost -Host_ $DatabaseHost -Stage 'sandbox-state'
    $DatabasePort = [int]$sandboxState.host_port
    $AdminPassword = [string]$sandboxState.root_password
}

$Stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$DbName = 'zhicloud_lock_test_' + $Stamp
$LogBase = 'mysql-history-lock-' + $Stamp
$SessionAOut = Join-Path $EvidenceDir ($LogBase + '-session-a.stdout.log')
$SessionAErr = Join-Path $EvidenceDir ($LogBase + '-session-a.stderr.log')
$SessionBOut = Join-Path $EvidenceDir ($LogBase + '-session-b.stdout.log')
$SessionBErr = Join-Path $EvidenceDir ($LogBase + '-session-b.stderr.log')
$EvidenceJson = Join-Path $EvidenceDir ($LogBase + '.json')
$script:Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$RepoCommit = $null
$RepoBranch = $null
$git = Get-Command git -ErrorAction SilentlyContinue
if ($null -ne $git) {
    $RepoCommit = (& $git.Source -C $RepoRoot rev-parse HEAD 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { $RepoCommit = $null }
    $RepoBranch = (& $git.Source -C $RepoRoot branch --show-current 2>$null | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) { $RepoBranch = $null }
}

function Invoke-MysqlText {
    param([Parameter(Mandatory = $true)][string]$Sql, [string]$Database)

    $previousPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')
    try {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $AdminPassword, 'Process')
        $arguments = @(
            '--protocol=TCP',
            "--host=$DatabaseHost",
            "--port=$DatabasePort",
            "--user=$AdminUser",
            '--batch',
            '--raw',
            '--skip-column-names',
            '--default-character-set=utf8mb4',
            '--connect-timeout=10'
        )
        if (-not [string]::IsNullOrWhiteSpace($Database)) { $arguments += $Database }
        # 走共享执行器：原来 `$Sql | & mysql 2>&1` 既无超时（等锁的查询能让脚本永远停在这一步），
        # 又会把 stderr 混进查询结果。失败时两条流都进异常文本。
        $run = Invoke-BoundedNative -FilePath $MysqlClient -Arguments $arguments -StandardInput $Sql `
            -TimeoutSeconds $TimeoutSeconds -Stage 'mysql-query'
        if ($run.ExitCode -ne 0) {
            throw "mysql 退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
        }
        return $run.StdOut
    } finally {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $previousPassword, 'Process')
    }
}

function Invoke-MysqlScalar {
    param([Parameter(Mandatory = $true)][string]$Sql, [string]$Database)
    return (Invoke-MysqlText -Sql $Sql -Database $Database).Trim()
}

function Start-MysqlSession {
    param([Parameter(Mandatory = $true)][string]$Sql)

    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $MysqlClient
    $psi.Arguments = '--protocol=TCP --host=' + $DatabaseHost + ' --port=' + $DatabasePort + ' --user=' + $AdminUser + ' --batch --raw --skip-column-names --default-character-set=utf8mb4 --connect-timeout=10 ' + $DbName
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.CreateNoWindow = $true
    $psi.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $psi.StandardErrorEncoding = [System.Text.Encoding]::UTF8
    $psi.EnvironmentVariables['MYSQL_PWD'] = $AdminPassword
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $psi
    if (-not $process.Start()) { throw '无法启动 mysql 会话' }
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    return [ordered]@{
        process = $process
        stdout_task = $stdoutTask
        stderr_task = $stderrTask
        started_at = Get-Date
    }
}

function Wait-Session {
    param($Session, [int]$TimeoutMs)
    if (-not $Session.process.WaitForExit($TimeoutMs)) {
        # 复用执行器的进程树终止：裸 Kill() 只杀直接子进程，且不留退出状态，
        # 超时后仍可能有 mysql 会话握着 flyway_schema_history 的锁继续跑。
        $kill = Stop-NativeProcessTree -Process $Session.process
        throw "MySQL 会话执行超时（$([Math]::Round($TimeoutMs / 1000, 1)) 秒），已强制终止进程树（parent_exited=$($kill.ParentExited) taskkill_rc=$($kill.TaskKillExitCode)）"
    }
    return [ordered]@{
        exit_code = [int]$Session.process.ExitCode
        elapsed_seconds = [Math]::Round(((Get-Date) - $Session.started_at).TotalSeconds, 3)
        stdout = $Session.stdout_task.Result
        stderr = $Session.stderr_task.Result
    }
}

$result = [ordered]@{
    generated_at = (Get-Date).ToString('o')
    status = 'failed'
    scope = 'MySQL flyway_schema_history SELECT FOR UPDATE concurrency lock validation'
    production_ready = $false
    gate3_passed = $false
    database = $DbName
    sandbox_container = if ($null -ne $sandboxState) { [string]$sandboxState.container_name } else { $null }
    sandbox_volume = if ($null -ne $sandboxState) { [string]$sandboxState.volume } else { $null }
    sandbox_image = if ($null -ne $sandboxState) { [string]$sandboxState.image } else { $null }
    bind_address = $DatabaseHost
    host_port = $DatabasePort
    repo_commit = $RepoCommit
    repo_branch = $RepoBranch
    independent_review = $false
    hold_seconds = $HoldSeconds
    lock_observed = $false
    history_unchanged = $false
    session_a = $null
    session_b = $null
    required_checks = [ordered]@{}
    server_version = $null
    client_version = $null
    caveats = @(
        '本验证仅覆盖本机 MySQL 沙箱，不证明生产数据库、TLS、Kubernetes 或跨库分布式锁。',
        '本验证证明 InnoDB 记录锁对相同 history 行的写入会阻塞；不证明业务表并发、死锁恢复或高可用切换。'
    )
}

try {
    # 版本探针走同一执行器：它跑的就是查询用的那个 mysql.exe，没有理由单独放行。
    # mysql --version 把信息写到 stdout，但为与旧 `2>&1` 结果一致，仍合并两条流。
    $versionRun = Invoke-BoundedNative -FilePath $MysqlClient -Arguments @('--version') `
        -TimeoutSeconds $TimeoutSeconds -Stage 'version-probe'
    $result.client_version = (($versionRun.StdOut + [Environment]::NewLine + $versionRun.StdErr).Trim())
    $result.server_version = Invoke-MysqlScalar -Sql 'SELECT VERSION();'
    $result.caveats[0] = "本验证仅覆盖本机 MySQL $($result.server_version) 沙箱，不证明生产数据库、TLS、Kubernetes 或跨库分布式锁。"
    Invoke-MysqlText -Sql "CREATE DATABASE ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    Invoke-MysqlText -Database $DbName -Sql @"
CREATE TABLE flyway_schema_history (
  installed_rank INT NOT NULL,
  version VARCHAR(50) NULL,
  description VARCHAR(200) NOT NULL,
  type VARCHAR(20) NOT NULL,
  script VARCHAR(1000) NOT NULL,
  checksum INT NULL,
  installed_by VARCHAR(100) NOT NULL,
  installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  execution_time INT NOT NULL,
  success TINYINT NOT NULL,
  PRIMARY KEY (installed_rank)
);
INSERT INTO flyway_schema_history
  (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES
  (1, '1.1', 'lock fixture', 'SQL', 'V1.1__system_base_schema.sql', 111111, 'lock-test', 0, 1);
"@ | Out-Null

    $beforeChecksum = Invoke-MysqlScalar -Database $DbName -Sql "SELECT checksum FROM flyway_schema_history WHERE installed_rank = 1;"
    $sessionA = Start-MysqlSession -Sql @"
START TRANSACTION;
SELECT CONNECTION_ID();
SELECT installed_rank FROM flyway_schema_history ORDER BY installed_rank FOR UPDATE;
SELECT SLEEP($HoldSeconds);
ROLLBACK;
SELECT 'SESSION_A_ROLLED_BACK';
"@

    $lockDeadline = (Get-Date).AddSeconds(20)
    while ((Get-Date) -lt $lockDeadline) {
        $lockCount = [int](Invoke-MysqlScalar -Sql "SELECT COUNT(*) FROM performance_schema.data_locks WHERE OBJECT_SCHEMA = '$DbName' AND OBJECT_NAME = 'flyway_schema_history' AND LOCK_TYPE = 'RECORD' AND LOCK_MODE LIKE 'X%';")
        if ($sessionA.process.HasExited) {
            $result.session_a = Wait-Session -Session $sessionA -TimeoutMs 1000
            throw "MySQL 会话 A 提前退出，退出码 $($result.session_a.exit_code): $($result.session_a.stderr.Trim())"
        }
        if ($lockCount -gt 0) {
            $result.lock_observed = $true
            break
        }
        Start-Sleep -Milliseconds 200
    }
    if (-not $result.lock_observed) {
        throw '未观察到 flyway_schema_history 的 FOR UPDATE 记录锁'
    }

    $sessionB = Start-MysqlSession -Sql @"
START TRANSACTION;
SELECT CONNECTION_ID();
UPDATE flyway_schema_history SET checksum = checksum WHERE installed_rank = 1;
SELECT 'SESSION_B_UPDATED';
COMMIT;
"@
    $result.session_b = Wait-Session -Session $sessionB -TimeoutMs (($TimeoutSeconds + $HoldSeconds) * 1000)
    $result.session_a = Wait-Session -Session $sessionA -TimeoutMs (($TimeoutSeconds + $HoldSeconds) * 1000)

    $afterChecksum = Invoke-MysqlScalar -Database $DbName -Sql "SELECT checksum FROM flyway_schema_history WHERE installed_rank = 1;"
    $result.history_unchanged = ($beforeChecksum -eq $afterChecksum)
    $aText = [string]$result.session_a.stdout
    $bText = [string]$result.session_b.stdout
    $result.required_checks = [ordered]@{
        session_a_exit_zero = ($result.session_a.exit_code -eq 0)
        session_b_exit_zero = ($result.session_b.exit_code -eq 0)
        record_lock_observed = $result.lock_observed
        session_b_waited_for_lock = ($result.session_b.elapsed_seconds -ge ($HoldSeconds - 2))
        session_b_committed_after_wait = ($bText -match 'SESSION_B_UPDATED')
        session_a_rolled_back = ($aText -match 'SESSION_A_ROLLED_BACK')
        history_unchanged = $result.history_unchanged
    }
    $result.status = if (($result.required_checks.Values | Where-Object { -not $_ }).Count -eq 0) { 'passed' } else { 'failed' }
} catch {
    $result.error = $_.Exception.Message
} finally {
    if ($result.session_a -eq $null -and $null -ne $sessionA) {
        try { $result.session_a = Wait-Session -Session $sessionA -TimeoutMs 1000 } catch { }
    }
    if ($result.session_b -eq $null -and $null -ne $sessionB) {
        try { $result.session_b = Wait-Session -Session $sessionB -TimeoutMs 1000 } catch { }
    }
    if ($null -ne $result.session_a) {
        [System.IO.File]::WriteAllText($SessionAOut, [string]$result.session_a.stdout, [System.Text.UTF8Encoding]::new($false))
        [System.IO.File]::WriteAllText($SessionAErr, [string]$result.session_a.stderr, [System.Text.UTF8Encoding]::new($false))
        $result.session_a.stdout_path = $SessionAOut
        $result.session_a.stderr_path = $SessionAErr
    }
    if ($null -ne $result.session_b) {
        [System.IO.File]::WriteAllText($SessionBOut, [string]$result.session_b.stdout, [System.Text.UTF8Encoding]::new($false))
        [System.IO.File]::WriteAllText($SessionBErr, [string]$result.session_b.stderr, [System.Text.UTF8Encoding]::new($false))
        $result.session_b.stdout_path = $SessionBOut
        $result.session_b.stderr_path = $SessionBErr
    }
    try { Invoke-MysqlText -Sql "DROP DATABASE IF EXISTS ``$DbName``;" | Out-Null } catch { }
    [System.IO.File]::WriteAllText($EvidenceJson, ($result | ConvertTo-Json -Depth 8), [System.Text.UTF8Encoding]::new($false))
}

Write-Output ('evidence=' + $EvidenceJson)
Write-Output ('status=' + $result.status)
if ($result.status -ne 'passed') { exit 1 }
