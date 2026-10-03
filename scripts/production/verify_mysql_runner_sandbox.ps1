#requires -Version 5.1
<#
.SYNOPSIS
  使用本机 MySQL 沙箱验证新构建 JAR 的 clean migration 与管理员 bootstrap 闭环。

.DESCRIPTION
  脚本只创建全新的 zhicloud_runner_clean_<时间戳> 和独立的
  zhicloud_runner_existing_<时间戳>/zhicloud_runner_incomplete_<时间戳> 数据库及专用账号，
  不会覆盖旧证据库，也不会把数据库口令或 bootstrap 明文口令写入 evidence。

  验证内容：
    1. clean baseline + Flyway V2..V121 首次迁移，且 V1.1 为 BASELINE（checksum IS NULL）；
       并对 V115-V121 逐版本断言 history 成功行，以及 V116/V117/V119 唯一索引、
       V120 owner/lease 列与租约索引、V121 claim_token 列的存在性（列/索引存在性只证明 DDL 落地，
       不证明运行期语义；claim_token 的类型与兼容性由 DatabaseMigrationRunner 的
       V121_BPM_IDEMPOTENCY_CLAIM_TOKEN_COLUMN 硬门禁负责）；
    2. 相同 clean 模式重复执行，必须 applied=0；
    3. existing 模式在完整 schema 且存在成功 clean BASELINE V1.1 history 时必须通过，不得改写该行，也不得重复执行 V1.1；
    4. existing 模式在 history 缺少成功 V1.1 时必须 fail-closed，且不得自动补写 V1.1；
    5. existing 模式在非空 legacy schema 缺少 history 时必须 fail-closed，且不得创建基线；
    6. existing 模式在完整 history 但缺少基础表时必须 fail-closed；
    7. existing 模式对已发布 legacy V1.1 SQL checksum 做一次性受控对齐，只改该行且幂等；
    8. existing 模式遇到未知 V1.1 checksum 必须 fail-closed 且 history 不变；
    9. 管理员 ensure 首次创建、相同口令 no-op、不同口令 fail-closed；
    10. rotate 更新口令，旧口令随后必须失败；
    11. 只读审计器检查管理员、角色、岗位、索引与哈希形态。

.PARAMETER AdminPasswordEnv
  可选：保存 MySQL 管理账号口令的环境变量名。未指定时按空口令沙箱处理。
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$MysqlClient = 'C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 53306,
    [string]$AdminUser = 'root',
    [string]$AdminPasswordEnv,
    [string]$EvidenceDir,
    [string]$Reviewer,
    [int]$TimeoutSeconds = 1800,
    [int]$TransportProbes = 6,
    [int]$TransportProbeDelaySeconds = 5,
    # R-043 的补偿控制：本脚本用 --env-file 把口令移出命令行，代价是运行被强杀时
    # 自己的 Remove-Item 不会执行（OS 层拿不到这种保证）。下次正常运行时清扫
    # 同前缀且已老到不可能是并发运行的孤儿文件；阈值必须 >=1 小时。
    [int]$StaleEnvMinAgeHours = 24,
    # 方向(b)：把拉起迁移的 JVM 也放进 mysqld 的网络命名空间，链路里不再出现宿主端口转发。
    # 默认关闭——不加这两个开关时脚本行为与改动前完全一致（宿主 JVM 走 -DatabaseHost/-DatabasePort）。
    [switch]$JavaInSandboxNamespace,
    [string]$SandboxContainerName,
    [string]$JvmImage = 'maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237'
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

if ($TimeoutSeconds -le 0) {
    throw 'TimeoutSeconds 必须大于 0'
}
if ($JavaInSandboxNamespace) {
    # 方向(b) 的两条 fail-closed：容器名必须给（否则不知道进谁的命名空间），
    # JVM 镜像必须 digest 钉定（与沙箱 mysqld 镜像同一口径，不给可变标签的机会）。
    if ([string]::IsNullOrWhiteSpace($SandboxContainerName)) {
        throw '-JavaInSandboxNamespace 需要同时给出 -SandboxContainerName（mysqld 沙箱容器名）'
    }
    if ($JvmImage -match 'latest') {
        throw "拒绝可变 JVM 镜像标签 'latest'：请用 @sha256 钉定 $JvmImage"
    }
    if ($JvmImage -notmatch '@sha256:[0-9a-f]{64}$') {
        throw "拒绝 JVM 镜像 '$JvmImage'：容器内 JVM 必须用 image@sha256:<64 hex> 钉定"
    }
}
$script:OverallTimeoutSeconds = $TimeoutSeconds

# 唯一的原生进程执行器（硬超时 + 杀进程树 + stdin/stdout 分流）。全仓库只有这一份实现，
# 各沙箱脚本一律 dot-source，不再各自复制一份——复制的副本会漂移。
. (Join-Path $PSScriptRoot 'SandboxNative.ps1')
. (Join-Path $PSScriptRoot 'SandboxSecretSweep.ps1')
$script:StaleEnvSweep = $null

function Get-ScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    if ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
        return (Split-Path -Parent $MyInvocation.MyCommand.Path)
    }
    throw '无法解析脚本目录'
}

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
} else {
    $RepoRoot = (Resolve-Path $RepoRoot).Path
}

$ScriptPath = if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) {
    $PSCommandPath
} elseif ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
    $MyInvocation.MyCommand.Path
} else {
    Join-Path (Get-ScriptDirectory) 'verify_mysql_runner_sandbox.ps1'
}
$ScriptPath = [System.IO.Path]::GetFullPath($ScriptPath)
if (-not (Test-Path -LiteralPath $ScriptPath -PathType Leaf)) { throw "脚本自身不存在: $ScriptPath" }

$JavaCommand = Get-Command java -ErrorAction Stop
$JavaPath = if ($JavaCommand.Path) { $JavaCommand.Path } else { $JavaCommand.Source }
if ([string]::IsNullOrWhiteSpace($JavaPath)) { $JavaPath = 'java' }
$PythonCommand = Get-Command python -ErrorAction Stop
$PythonPath = if ($PythonCommand.Path) { $PythonCommand.Path } else { $PythonCommand.Source }
if ([string]::IsNullOrWhiteSpace($PythonPath)) { $PythonPath = 'python' }

$script:StartedAt = Get-Date
$script:Deadline = (Get-Date).AddSeconds($TimeoutSeconds)

$JarPath = Join-Path $RepoRoot 'zhicloud-server\target\zhicloud-server.jar'
$AuditorPath = Join-Path $RepoRoot 'scripts\production\audit_bootstrap_admin.py'
if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) { throw "JAR 不存在: $JarPath" }
if (-not (Test-Path -LiteralPath $AuditorPath -PathType Leaf)) { throw "审计器不存在: $AuditorPath" }
if (-not (Test-Path -LiteralPath $MysqlClient -PathType Leaf)) { throw "mysql 客户端不存在: $MysqlClient" }

if ([string]::IsNullOrWhiteSpace($EvidenceDir)) {
    $EvidenceDir = Join-Path $RepoRoot 'docs\enterprise-platform\evidence\production'
}
$EvidenceDir = [System.IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null

$AdminPassword = ''
if (-not [string]::IsNullOrWhiteSpace($AdminPasswordEnv)) {
    $AdminPassword = [Environment]::GetEnvironmentVariable($AdminPasswordEnv, 'Process')
    if ([string]::IsNullOrEmpty($AdminPassword)) {
        throw "环境变量 $AdminPasswordEnv 不存在或为空"
    }
}

$Stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$DbName = 'zhicloud_runner_clean_' + $Stamp
$ValidationDbName = 'zhicloud_runner_existing_' + $Stamp
$IncompleteDbName = 'zhicloud_runner_incomplete_' + $Stamp
$LegacyDbName = 'zhicloud_runner_legacy_' + $Stamp
$DbUser = 'zhicloud_run_' + $Stamp
if ($DbName.Length -gt 64) { throw "数据库名过长: $DbName" }
if ($ValidationDbName.Length -gt 64) { throw "数据库名过长: $ValidationDbName" }
if ($IncompleteDbName.Length -gt 64) { throw "数据库名过长: $IncompleteDbName" }
if ($LegacyDbName.Length -gt 64) { throw "数据库名过长: $LegacyDbName" }
if ($DbUser.Length -gt 32) { throw "数据库账号过长: $DbUser" }

# 只在本进程内存在；不写入日志、evidence 或 argv。
$DbPassword = 'Zc!' + [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N').Substring(0, 8) + '9a'
$BootstrapPassword1 = 'Zc!' + [guid]::NewGuid().ToString('N') + '9a'
$BootstrapPassword2 = 'Zc!' + [guid]::NewGuid().ToString('N') + '7b'
$BootstrapPassword3 = 'Zc!' + [guid]::NewGuid().ToString('N') + '5c'

# JDBC 端点按启动形态分叉：宿主 JVM 走 -DatabaseHost/-DatabasePort（经发布端口，来源呈现为 172.17.0.1，
# 靠中继或授权写法解决）；容器内 JVM 走 mysqld 命名空间里的真实回环 127.0.0.1:3306，
# 此时链路里完全没有宿主端口转发，@'127.0.0.1' 授权天然匹配。
$JdbcHost = $DatabaseHost
$JdbcPort = $DatabasePort
if ($JavaInSandboxNamespace) {
    $JdbcHost = '127.0.0.1'
    $JdbcPort = 3306
}

$DbUrl = "jdbc:mysql://${JdbcHost}:${JdbcPort}/${DbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$ValidationDbUrl = "jdbc:mysql://${JdbcHost}:${JdbcPort}/${ValidationDbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$IncompleteDbUrl = "jdbc:mysql://${JdbcHost}:${JdbcPort}/${IncompleteDbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$LegacyDbUrl = "jdbc:mysql://${JdbcHost}:${JdbcPort}/${LegacyDbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$LogBase = 'mysql-runner-sandbox-' + $Stamp
$CleanLog = Join-Path $EvidenceDir ($LogBase + '-clean.log')
$ResumeLog = Join-Path $EvidenceDir ($LogBase + '-resume.log')
$EnsureFirstLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-ensure-first.log')
$EnsureRepeatLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-ensure-repeat.log')
$EnsureMismatchLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-ensure-mismatch.log')
$RotateLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-rotate.log')
$EnsureOldLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-old-password.log')
$EnsureNewLog = Join-Path $EvidenceDir ($LogBase + '-bootstrap-new-password.log')
$ValidationFixtureLog = Join-Path $EvidenceDir ($LogBase + '-existing-fixture-clean.log')
$ExistingHistoryLog = Join-Path $EvidenceDir ($LogBase + '-existing-complete-history.log')
$MissingV11Log = Join-Path $EvidenceDir ($LogBase + '-existing-missing-v11.log')
$NoHistoryLog = Join-Path $EvidenceDir ($LogBase + '-existing-no-history.log')
$IncompleteFixtureLog = Join-Path $EvidenceDir ($LogBase + '-incomplete-fixture-clean.log')
$IncompleteSchemaLog = Join-Path $EvidenceDir ($LogBase + '-existing-incomplete-schema.log')
$LegacyFixtureLog = Join-Path $EvidenceDir ($LogBase + '-legacy-fixture-clean.log')
$LegacyAlignLog = Join-Path $EvidenceDir ($LogBase + '-legacy-checksum-align.log')
$LegacyIdempotentLog = Join-Path $EvidenceDir ($LogBase + '-legacy-checksum-idempotent.log')
$LegacyRejectLog = Join-Path $EvidenceDir ($LogBase + '-legacy-checksum-rejected.log')
$AuditJson = Join-Path $EvidenceDir ($LogBase + '-audit.json')
$AuditStdoutLog = Join-Path $EvidenceDir ($LogBase + '-audit.stdout.log')
$EvidenceJson = Join-Path $EvidenceDir ($LogBase + '.json')

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try {
            $bytes = $sha.ComputeHash($stream)
        } finally {
            $stream.Dispose()
        }
    } finally {
        $sha.Dispose()
    }
    return [System.BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}

function Set-ProcessEnvironment {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Values,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )
    $saved = @{}
    foreach ($key in $Values.Keys) {
        $saved[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
        [Environment]::SetEnvironmentVariable($key, [string]$Values[$key], 'Process')
    }
    try {
        & $Action
    } finally {
        foreach ($key in $saved.Keys) {
            [Environment]::SetEnvironmentVariable($key, $saved[$key], 'Process')
        }
    }
}

function ConvertTo-SingleQuotedPowerShellLiteral {
    param([AllowEmptyString()][string]$Value)
    return "'" + $Value.Replace("'", "''") + "'"
}

function Get-GitMetadata {
    param([Parameter(Mandatory = $true)][string]$RepositoryRoot)

    $commitSha = $null
    $worktreeDirty = $false
    $statusSha256 = $null
    $statusEntryCount = 0

    try {
        $gitOutput = & git -C $RepositoryRoot rev-parse HEAD 2>$null
        $gitExitCode = $LASTEXITCODE
        if ($gitExitCode -eq 0 -and $gitOutput) {
            $candidateSha = ([string]($gitOutput | Select-Object -First 1)).Trim()
            if ($candidateSha -match '^[0-9a-fA-F]{40,64}$') {
                $commitSha = $candidateSha.ToLowerInvariant()
            }
        }

        $statusOutput = & git -C $RepositoryRoot status --porcelain=v1 --untracked-files=all 2>$null
        $statusExitCode = $LASTEXITCODE
        if ($statusExitCode -eq 0) {
            $statusLines = @()
            if ($null -ne $statusOutput) {
                $statusLines = @($statusOutput | ForEach-Object { [string]$_ } | Where-Object { $_.Length -gt 0 })
            }
            $statusEntryCount = $statusLines.Count
            $worktreeDirty = $statusEntryCount -gt 0
            if ($worktreeDirty) {
                $normalized = ($statusLines | Sort-Object) -join "`n"
                $sha = [System.Security.Cryptography.SHA256]::Create()
                try {
                    $hashBytes = $sha.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($normalized))
                } finally {
                    $sha.Dispose()
                }
                $statusSha256 = ([System.BitConverter]::ToString($hashBytes)).Replace('-', '').ToLowerInvariant()
            }
        }
    } catch {
        # 元数据不可用时不伪造值；证据中保留 null/false 并由复核人判断。
    }

    return [ordered]@{
        commit_sha = $commitSha
        worktree_dirty = $worktreeDirty
        status_sha256 = $statusSha256
        status_entry_count = $statusEntryCount
        note = '工作区存在未提交改动时，commit_sha 不能单独唯一标识被验证代码；必须结合 JAR 哈希、脚本哈希和 status_sha256 复核。'
    }
}

function Resolve-NativeBudget {
    param([Parameter(Mandatory = $true)][int]$TimeoutSeconds)

    # 整体预算优先于单次超时：本脚本有 15+ 次 java/mysql 调用，只限单次会让总时长无界增长。
    if ($null -ne $script:Deadline) {
        $remainingSeconds = [int][Math]::Floor(($script:Deadline - (Get-Date)).TotalSeconds)
    } else {
        $remainingSeconds = $TimeoutSeconds
    }
    if ($remainingSeconds -le 0) {
        throw "整体执行超过 $TimeoutSeconds 秒"
    }
    if ($remainingSeconds -gt $TimeoutSeconds) { $remainingSeconds = $TimeoutSeconds }
    return $remainingSeconds
}

function Invoke-ExternalProcess {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    $run = Invoke-BoundedNative `
        -FilePath $FilePath `
        -Arguments $Arguments `
        -TimeoutSeconds (Resolve-NativeBudget -TimeoutSeconds $TimeoutSeconds) `
        -Stage 'runner-process' `
        -WorkingDirectory $RepoRoot `
        -LogPath $LogPath
    return [int]$run.ExitCode
}

function Invoke-MysqlText {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database
    )
    if ($JavaInSandboxNamespace) {
        # 方向(b) 的收尾：管理侧 SQL 也走容器命名空间内的真实回环，于是本形态下
        # 不存在任何"宿主 → 发布端口 → DNAT"的数据路径，§12.22/§12.28 的抖动没有落脚处。
        # 与 mysql-sandbox.ps1 的 Get-SandboxMetrics 同一口径：口令只进进程环境，
        # docker 命令行里出现的是 `-e MYSQL_PWD`（不带值）。这里的旧注释曾写"不落命令行"，
        # 而当时代码恰恰落了——注释与实现相反的那条假声明一并修正（R-041）。
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $AdminPassword, 'Process')
        $arguments = @(
            'exec', '-i',
            '-e', 'MYSQL_PWD',
            $SandboxContainerName,
            'mysql',
            '--protocol=TCP',
            '--host=127.0.0.1',
            '--port=3306',
            "--user=$AdminUser",
            '--batch',
            '--raw',
            '--skip-column-names',
            '--default-character-set=utf8mb4',
            '--connect-timeout=10'
        )
        if (-not [string]::IsNullOrWhiteSpace($Database)) { $arguments += $Database }
        $run = Invoke-BoundedNative `
            -FilePath 'docker' `
            -Arguments $arguments `
            -StandardInput $Sql `
            -TimeoutSeconds (Resolve-NativeBudget -TimeoutSeconds $script:OverallTimeoutSeconds) `
            -Stage 'mysql-query-namespace'
        if ($run.ExitCode -ne 0) {
            throw "mysql 退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
        }
        return $run.StdOut
    }
    $previousPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')
    try {
        if ([string]::IsNullOrEmpty($AdminPassword)) {
            [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')
        } else {
            [Environment]::SetEnvironmentVariable('MYSQL_PWD', $AdminPassword, 'Process')
        }
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
        # 走共享执行器而不是 `| & mysql 2>&1`：后者既不设超时（mysql 卡在锁等待就整体挂死），
        # 又会把 stderr 告警混进查询结果，污染 Invoke-MysqlScalar 的"期望单值"断言。
        $run = Invoke-BoundedNative `
            -FilePath $MysqlClient `
            -Arguments $arguments `
            -StandardInput $Sql `
            -TimeoutSeconds (Resolve-NativeBudget -TimeoutSeconds $script:OverallTimeoutSeconds) `
            -Stage 'mysql-query'
        if ($run.ExitCode -ne 0) {
            throw "mysql 退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
        }
        return $run.StdOut
    } finally {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $previousPassword, 'Process')
    }
}

function Invoke-MysqlScalar {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database
    )
    $value = Invoke-MysqlText -Sql $Sql -Database $Database
    if ($value -match "`r?`n") { throw "期望单值查询，实际返回多行: $Sql" }
    return $value.Trim()
}

# R-034：本机 Docker 发布端口存在低概率首连抖动（直连与中继两条路径都实测到，签名 08S01/连接超时，
# 且 mysqld 侧 Aborted_connects=0，说明 SYN 到不了 mysqld）。抖动本身不是本脚本能修的，
# 但一次抖动落在 16 次 java 调用中间就会让整个验证以"看不出原因的失败"收尾。
# 所以每次启动 java 之前先做一次**无副作用**的可达性预检并做有界重试：
# 只重试 `SELECT 1`，绝不重试迁移/runner 调用本身——后者带副作用，重试会把 fail-closed 语义搅浑。
$script:TransportProbeAttempts = 0
$script:TransportProbeWaits = 0
function Wait-TransportReady {
    param([int]$MaxAttempts = $TransportProbes)

    if ($TransportProbes -lt 1) { throw 'TransportProbes 必须大于等于 1' }
    if ($TransportProbeDelaySeconds -lt 0) { throw 'TransportProbeDelaySeconds 不得为负' }
    $lastError = ''
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $script:TransportProbeAttempts = $script:TransportProbeAttempts + 1
        try {
            $probe = Invoke-MysqlScalar -Sql 'SELECT 1;'
            if ($probe -eq '1') {
                if ($attempt -gt 1) { $script:TransportProbeWaits = $script:TransportProbeWaits + 1 }
                return $attempt
            }
            $lastError = "预检返回值不是 1：'$probe'"
        } catch {
            $lastError = $_.Exception.Message
        }
        if ($attempt -lt $MaxAttempts) { Start-Sleep -Seconds $TransportProbeDelaySeconds }
    }
    throw "沙箱连接预检连续 $MaxAttempts 次失败（阶段：transport-preflight）：$lastError"
}

function New-SandboxDatabase {
    param([Parameter(Mandatory = $true)][string]$DatabaseName)

    Invoke-MysqlText -Sql "CREATE DATABASE ``$DatabaseName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    Invoke-MysqlText -Sql "GRANT ALL PRIVILEGES ON ``$DatabaseName``.* TO '$DbUser'@'127.0.0.1'; FLUSH PRIVILEGES;" | Out-Null
}

function Wait-SandboxNamespaceReady {
    param([int]$MaxAttempts = $TransportProbes)

    # 方向(b) 下宿主没有通往数据库的路径（可能压根没发布端口），所以 `SELECT 1` 预检不适用；
    # 这里改成同样无副作用的就绪门：沙箱容器必须处于 Running，否则 `--network container:` 会直接失败。
    $lastState = ''
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $script:TransportProbeAttempts = $script:TransportProbeAttempts + 1
        $probe = Invoke-BoundedNative -FilePath 'docker' -Arguments @('inspect', '--format', '{{.State.Running}}', $SandboxContainerName) `
            -TimeoutSeconds (Resolve-NativeBudget -TimeoutSeconds 60) -Stage 'namespace-readiness'
        $lastState = ($probe.StdOut + ' ' + $probe.StdErr).Trim()
        if ($probe.ExitCode -eq 0 -and $lastState -eq 'true') {
            if ($attempt -gt 1) { $script:TransportProbeWaits = $script:TransportProbeWaits + 1 }
            return $attempt
        }
        if ($attempt -lt $MaxAttempts) { Start-Sleep -Seconds $TransportProbeDelaySeconds }
    }
    throw "沙箱容器 $SandboxContainerName 未处于 running（阶段：namespace-readiness）：$lastState"
}

function Get-ContainerJavaVersion {
    $probe = Invoke-BoundedNative -FilePath 'docker' -Arguments @('run', '--rm', '--entrypoint', 'java', $JvmImage, '-version') `
        -TimeoutSeconds (Resolve-NativeBudget -TimeoutSeconds 120) -Stage 'jvm-probe'
    return (($probe.StdOut + [Environment]::NewLine + $probe.StdErr).Trim())
}

function Invoke-JavaMain {
    param(
        [Parameter(Mandatory = $true)][string]$MainClass,
        [Parameter(Mandatory = $true)][hashtable]$Environment,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    if ($JavaInSandboxNamespace) {
        # 方向(b)：JVM 在 mysqld 的网络命名空间里跑，URL 指向 127.0.0.1:3306（命名空间内的真实回环）。
        # 口令走 --env-file 而不是 `--env K=V`：后者会把口令放进命令行，而执行器会写命令日志 → 泄进证据。
        if ($null -eq $script:StaleEnvSweep) {
            # 只在每次运行里扫一次；清扫结果存下来写进证据，否则"清过了"只是我的一句话。
            $script:StaleEnvSweep = Clear-StaleRunnerEnvFiles -DirectoryPath $env:TEMP -MinAgeHours $StaleEnvMinAgeHours
        }
        $envFile = Join-Path $env:TEMP ('zhicloud-runner-env-' + [guid]::NewGuid().ToString('N') + '.txt')
        $jarName = Split-Path -Leaf $JarPath
        $jarDirectory = Split-Path -Parent $JarPath
        try {
            $lines = @($Environment.Keys | ForEach-Object { $_ + '=' + [string]$Environment[$_] })
            [System.IO.File]::WriteAllText($envFile, ($lines -join "`n"), (New-Object System.Text.UTF8Encoding($false)))
            $null = Wait-SandboxNamespaceReady
            $arguments = @(
                'run', '--rm',
                '--network', "container:$SandboxContainerName",
                '--env-file', $envFile,
                '--volume', "$($jarDirectory):/work/target:ro",
                '--entrypoint', 'java',
                $JvmImage,
                '-Dfile.encoding=UTF-8',
                '-Duser.timezone=Asia/Shanghai',
                '-cp', "/work/target/$jarName",
                "-Dloader.main=$MainClass",
                'org.springframework.boot.loader.launch.PropertiesLauncher'
            )
            $script:lastJavaExitCode = Invoke-ExternalProcess `
                -FilePath 'docker' `
                -Arguments $arguments `
                -LogPath $LogPath `
                -TimeoutSeconds $script:OverallTimeoutSeconds
        } finally {
            if (Test-Path -LiteralPath $envFile) { Remove-Item -LiteralPath $envFile -Force }
        }
        return [int]$script:lastJavaExitCode
    }

    Set-ProcessEnvironment -Values $Environment -Action {
        # 副作用边界：预检只读 `SELECT 1`，在拉起 java 之前确认端口真的可达；
        # 迁移/bootstrap 调用一次都不重试，失败就按原样把退出码交给上层断言。
        $null = Wait-TransportReady
        $arguments = @(
            '-Dfile.encoding=UTF-8',
            '-cp', $JarPath,
            "-Dloader.main=$MainClass",
            'org.springframework.boot.loader.launch.PropertiesLauncher'
        )
        $script:lastJavaExitCode = Invoke-ExternalProcess `
            -FilePath $JavaPath `
            -Arguments $arguments `
            -LogPath $LogPath `
            -TimeoutSeconds $script:OverallTimeoutSeconds
    }
    return [int]$script:lastJavaExitCode
}

function Initialize-SandboxCleanFixture {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    $environment = @{
        MASTER_DATASOURCE_URL = $Url
        MASTER_DATASOURCE_USERNAME = $DbUser
        MASTER_DATASOURCE_PASSWORD = $DbPassword
        MIGRATION_BASELINE_MODE = 'clean'
    }
    $exitCode = Invoke-JavaMain `
        -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' `
        -Environment $environment `
        -LogPath $LogPath
    if ($exitCode -ne 0) { throw "existing 验证夹具 clean migration 退出码 $exitCode，见 $LogPath" }
    return [int]$exitCode
}

function Read-Match {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Pattern,
        [int]$Group = 1,
        [string]$Default = ''
    )
    if (-not (Test-Path -LiteralPath $Path)) { return $Default }
    $text = Get-Content -LiteralPath $Path -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if ([string]::IsNullOrEmpty($text)) { return $Default }
    $match = [regex]::Match($text, $Pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if (-not $match.Success) { return $Default }
    return $match.Groups[$Group].Value
}

function Read-LogText {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { return '' }
    return (Get-Content -LiteralPath $Path -Raw -Encoding UTF8 -ErrorAction SilentlyContinue)
}

# 计算 flyway_schema_history 的确定性摘要，用于断言“只改了目标行”。
# 传入 ExcludeVersion 时忽略该 version 的行（本脚本中即 V1.1），从而验证非目标行完全未变。
function Get-HistoryDigest {
    param(
        [Parameter(Mandatory = $true)][string]$DatabaseName,
        [string]$ExcludeVersion
    )
    $filter = if ([string]::IsNullOrWhiteSpace($ExcludeVersion)) { '' } else { " WHERE version <> '$ExcludeVersion'" }
    $sql = 'SET SESSION group_concat_max_len = 1048576; SELECT COALESCE(MD5(GROUP_CONCAT(CONCAT_WS(''|'', installed_rank, IFNULL(version, ''~''), IFNULL(description, ''~''), IFNULL(type, ''~''), IFNULL(script, ''~''), IFNULL(CAST(checksum AS CHAR), ''NULL''), CAST(success AS CHAR)) ORDER BY installed_rank SEPARATOR ''#'')), ''empty'') FROM flyway_schema_history' + $filter + ';'
    $value = (Invoke-MysqlText -Database $DatabaseName -Sql $sql).Trim()
    if ($value -match "`r?`n") {
        $lines = @($value -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 })
        if ($lines.Count -eq 0) { throw '无法解析 flyway_schema_history 摘要' }
        $value = $lines[$lines.Count - 1].Trim()
    }
    return $value
}

function Get-HistoryV11Field {
    param(
        [Parameter(Mandatory = $true)][string]$DatabaseName,
        [Parameter(Mandatory = $true)][string]$ColumnName
    )
    return (Invoke-MysqlScalar -Database $DatabaseName -Sql "SELECT IFNULL(CAST($ColumnName AS CHAR),'<NULL>') FROM flyway_schema_history WHERE version = '1.1';")
}

if ([string]::IsNullOrWhiteSpace($Reviewer)) {
    $Reviewer = if (-not [string]::IsNullOrWhiteSpace($env:USERNAME)) { $env:USERNAME } else { 'unassigned' }
}
$ReviewerSource = if ($PSBoundParameters.ContainsKey('Reviewer')) { 'explicit' } else { 'os_user_default' }

$JarSha256 = Get-Sha256 -Path $JarPath
$AuditorSha256 = Get-Sha256 -Path $AuditorPath
$ScriptSha256 = Get-Sha256 -Path $ScriptPath

# 版本探针也走同一执行器：它们跑的是与查询相同的 mysql.exe / java.exe，没有理由假定它们不会挂。
# java -version 刻意写到 stderr，所以这里把两条流合起来——与旧的 `2>&1` 结果保持一致。
function Get-NativeVersionText {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [string[]]$Arguments = @('--version')
    )
    try {
        $run = Invoke-BoundedNative -FilePath $Path -Arguments $Arguments -TimeoutSeconds 60 -Stage 'version-probe'
        return ($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim()
    } catch {
        return ''
    }
}

$MysqlClientVersion = Get-NativeVersionText -Path $MysqlClient
$JavaVersion = Get-NativeVersionText -Path $JavaPath -Arguments @('-version')
$GitMetadata = Get-GitMetadata -RepositoryRoot $RepoRoot

$executionParts = @(
    "& $(ConvertTo-SingleQuotedPowerShellLiteral -Value $ScriptPath)",
    "-RepoRoot $(ConvertTo-SingleQuotedPowerShellLiteral -Value $RepoRoot)",
    "-MysqlClient $(ConvertTo-SingleQuotedPowerShellLiteral -Value $MysqlClient)",
    "-DatabaseHost $(ConvertTo-SingleQuotedPowerShellLiteral -Value $DatabaseHost)",
    "-DatabasePort $DatabasePort",
    "-AdminUser $(ConvertTo-SingleQuotedPowerShellLiteral -Value $AdminUser)"
)
if ($PSBoundParameters.ContainsKey('AdminPasswordEnv')) {
    $executionParts += "-AdminPasswordEnv $(ConvertTo-SingleQuotedPowerShellLiteral -Value $AdminPasswordEnv)"
}
$executionParts += @(
    "-EvidenceDir $(ConvertTo-SingleQuotedPowerShellLiteral -Value $EvidenceDir)",
    "-Reviewer $(ConvertTo-SingleQuotedPowerShellLiteral -Value $Reviewer)",
    "-TimeoutSeconds $TimeoutSeconds",
    "-TransportProbes $TransportProbes",
    "-TransportProbeDelaySeconds $TransportProbeDelaySeconds"
)
$ExecutionCommand = $executionParts -join ' '

$result = [ordered]@{
    generated_at = (Get-Date).ToString('o')
    status = 'running'
    scope = 'MySQL 8.4 sandbox clean/existing migration fail-closed validation and bootstrap administrator end-to-end validation'
    production_ready = $false
    gate3_passed = $false
    r011_status = 'OPEN'
    r029_status = 'OPEN'
    blocking_risks = @('R-011', 'R-029')
    database = [ordered]@{
        name = $DbName
        user = $DbUser
        host = $DatabaseHost
        port = $DatabasePort
    }
    java_runtime = [ordered]@{
        mode = if ($JavaInSandboxNamespace) { 'container_namespace' } else { 'host' }
        sandbox_container = if ($JavaInSandboxNamespace) { $SandboxContainerName } else { $null }
        jvm_image = if ($JavaInSandboxNamespace) { $JvmImage } else { $null }
        jdbc_endpoint = "${JdbcHost}:${JdbcPort}"
        host_java_version = $JavaVersion
        container_java_version = $null
        mysql_transport = if ($JavaInSandboxNamespace) { 'docker_exec_in_namespace' } else { 'host_mysql_client' }
        admin_endpoint = if ($JavaInSandboxNamespace) { '127.0.0.1:3306 (inside sandbox namespace)' } else { "${DatabaseHost}:${DatabasePort}" }
        auditor_transport = if ($JavaInSandboxNamespace) { "host python + mysql.exe -> ${DatabaseHost}:${DatabasePort} (仅这一条仍在宿主侧；只读，传输类错误可重试)" } else { "host mysql.exe -> ${DatabaseHost}:${DatabasePort}" }
        note = '两种形态不是同一个被测量：容器内是 Linux + 钉定镜像里的 JVM（默认 TZ=UTC，本次显式 -Duser.timezone=Asia/Shanghai），宿主是 Windows 上的 java.exe。引用本证据时必须连同本节一起引用。'
    }
    validation_databases = [ordered]@{
        existing_history = $ValidationDbName
        incomplete_schema = $IncompleteDbName
        legacy_checksum = $LegacyDbName
        host = $DatabaseHost
        port = $DatabasePort
    }
    mysql = [ordered]@{
        client_path = $MysqlClient
        client_version = $MysqlClientVersion
        server_version = $null
    }
    environment = [ordered]@{
        os = [System.Environment]::OSVersion.VersionString
        powershell = $PSVersionTable.PSVersion.ToString()
        java_path = $JavaPath
        java_version = $JavaVersion
        repo_root = $RepoRoot
    }
    git = $GitMetadata
    script = [ordered]@{
        path = $ScriptPath
        sha256 = $ScriptSha256
    }
    execution = [ordered]@{
        command = $ExecutionCommand
        command_note = '规范化复现命令；不包含任何口令值。'
        started_at = $script:StartedAt.ToString('o')
        completed_at = $null
        timeout_seconds = $TimeoutSeconds
        deadline = $script:Deadline.ToString('o')
    }
    review = [ordered]@{
        reviewer = $Reviewer
        reviewer_source = $ReviewerSource
        independent_review = $false
        note = 'reviewer 默认为执行主机用户，不代表独立复核；生产放行前必须补充独立复核人。'
    }
    jar = [ordered]@{
        path = $JarPath
        sha256 = $JarSha256
    }
    auditor = [ordered]@{
        path = $AuditorPath
        sha256 = $AuditorSha256
    }
    runs = [ordered]@{}
    database_checks = [ordered]@{}
    existing_checks = [ordered]@{}
    bootstrap_checks = [ordered]@{}
    transport = [ordered]@{}
    secret_handling = [ordered]@{
        database_password_persisted = $false
        bootstrap_plaintext_password_persisted = $false
        namespace_env_file = 'transient_disk_file_removed_after_each_call'
        stale_env_sweep = $script:StaleEnvSweep
        # 旧 note 说口令"只存在于本次 PowerShell 进程"，这在同一形态下并不成立：
        # --env-file 会把明文口令短暂落到 %TEMP%（移出 argv 是必要的，但没消除磁盘面）。
        # 这里改成事实描述，并把 R-043 的清扫结果一并落证据，"清过了"才不用靠我的一句话。
        note = '数据库口令与 bootstrap 明文口令不写进 evidence。命名空间形态下 JVM 口令经 %TEMP% 的 --env-file 传递，调用结束即删；进程被强杀时该文件会残留（R-043），因此每次运行先按精确前缀 + 年龄阈值清扫孤儿，结果记在 stale_env_sweep。'
    }
    caveats = @(
        '本验证仅覆盖本机 MySQL 8.4.9 沙箱，不证明目标生产数据库、TLS、Kubernetes 或 ArgoCD 部署已通过。',
        'V116/V117 的并发与软删除语义未由本脚本证明；V120 owner fencing 的接管与结算语义、V119 租户唯一约束的重复行 fail-closed 语义、V121 claim_token 的当前读 ownership 语义（含 REPEATABLE-READ 一致性读路径）也未由本脚本证明。本脚本对 V115-V121 只断言 history 成功行与列/索引存在性，即 DDL 落地，不等于运行期语义或生产等价验证。',
        'BPM/Flowable 事务回滚、备份恢复、容量与高可用未由本脚本证明。',
        '工作区存在未提交改动时，commit_sha 不能单独唯一标识被验证代码；必须结合 JAR 哈希、脚本哈希和 git status 哈希复核。',
        'reviewer 默认为执行主机用户，不代表独立复核；生产放行前必须补充独立复核人。',
        'existing 成功场景由 clean baseline（V1.1 为 BASELINE、checksum IS NULL）构造，只证明该成功 history 不会被重复执行或改写；不证明外部 legacy schema 的列、索引和约束均已通过强签名校验。',
        'legacy checksum 正/负例均由 clean fixture 的 rank=1 行人工改写构造（checksum 设为已发布 legacy 值或未知值），只验证 DatabaseMigrationRunner 的受控对齐与 fail-closed 分支，不证明真实历史生产库的 Flyway 状态。',
        'Production readiness 和 Gate 3 仍为 false，R-011 和 R-029 仍为 OPEN，直到所有阻断项关闭。'
    )
}

try {
    Invoke-MysqlText -Sql "CREATE DATABASE ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    $createUserSql = "CREATE USER '$DbUser'@'127.0.0.1' IDENTIFIED BY '$DbPassword'; GRANT ALL PRIVILEGES ON ``$DbName``.* TO '$DbUser'@'127.0.0.1'; FLUSH PRIVILEGES;"
    Invoke-MysqlText -Sql $createUserSql | Out-Null
    $result.mysql.server_version = Invoke-MysqlScalar -Sql 'SELECT VERSION();'
    if ($JavaInSandboxNamespace) {
        $result.java_runtime.container_java_version = (Get-ContainerJavaVersion -split "`r?`n" | Select-Object -First 1)
    }

    $migrationEnvironment = @{
        MASTER_DATASOURCE_URL = $DbUrl
        MASTER_DATASOURCE_USERNAME = $DbUser
        MASTER_DATASOURCE_PASSWORD = $DbPassword
        MIGRATION_BASELINE_MODE = 'clean'
    }

    $cleanExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $migrationEnvironment -LogPath $CleanLog
    $result.runs.clean_first = [ordered]@{
        exit_code = $cleanExit
        log_path = $CleanLog
        log_sha256 = Get-Sha256 -Path $CleanLog
        applied_migrations = [int](Read-Match -Path $CleanLog -Pattern 'success: applied=(\d+)' -Default '-1')
        initial_version = Read-Match -Path $CleanLog -Pattern 'initialVersion=([^,\r\n]+)' -Default ''
        target_version = Read-Match -Path $CleanLog -Pattern 'targetVersion=([^,\r\n]+)' -Default ''
        clean_baseline_statements = [int](Read-Match -Path $CleanLog -Pattern 'statements=(\d+)' -Default '-1')
    }
    if ($cleanExit -ne 0) { throw "clean migration 退出码 $cleanExit，见 $CleanLog" }

    $result.database_checks.pre_bootstrap_system_users = [int](Invoke-MysqlScalar -Database $DbName -Sql 'SELECT COUNT(*) FROM system_users;')
    $result.database_checks.base_table_count = [int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE';")
    $result.database_checks.flyway_history_rows = [int](Invoke-MysqlScalar -Database $DbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $result.database_checks.flyway_success_rows = [int](Invoke-MysqlScalar -Database $DbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1;')
    $result.database_checks.flyway_failed_rows = [int](Invoke-MysqlScalar -Database $DbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;')
    $result.database_checks.clean_baseline_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';") -eq 1)
    $result.database_checks.clean_baseline_history_type = Get-HistoryV11Field -DatabaseName $DbName -ColumnName 'type'
    $result.database_checks.clean_baseline_history_script = Get-HistoryV11Field -DatabaseName $DbName -ColumnName 'script'
    $result.database_checks.clean_baseline_history_checksum = Get-HistoryV11Field -DatabaseName $DbName -ColumnName 'checksum'
    $result.database_checks.clean_baseline_history_row_shape = (
        $result.database_checks.clean_baseline_history_type -eq 'BASELINE' -and
        $result.database_checks.clean_baseline_history_script -eq '<< Flyway Baseline >>' -and
        $result.database_checks.clean_baseline_history_checksum -eq '<NULL>'
    )
    $result.database_checks.version_115_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '115';") -eq 1)
    $result.database_checks.version_116_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '116';") -eq 1)
    $result.database_checks.version_117_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '117';") -eq 1)
    $result.database_checks.active_username_unique_index = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'system_users' AND index_name = 'uk_system_users_tenant_active_username';") -gt 0)
    $result.database_checks.receivable_idempotency_unique_index = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'crm_receivable' AND index_name = 'uk_crm_receivable_idempotency';") -gt 0)
    $result.database_checks.version_118_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '118';") -eq 1)
    $result.database_checks.version_119_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '119';") -eq 1)
    $result.database_checks.version_120_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '120';") -eq 1)
    $result.database_checks.version_121_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '121';") -eq 1)
    $result.database_checks.inventory_tenant_unique_index = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inventory_item' AND index_name = 'uk_inventory_item_tenant_composite' AND NON_UNIQUE = 0;") -eq 1)
    $result.database_checks.outbox_owner_lease_columns = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'event_outbox' AND column_name IN ('processing_owner', 'lease_until');") -eq 2)
    $result.database_checks.inbox_owner_lease_columns = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'event_inbox' AND column_name IN ('processing_owner', 'lease_until');") -eq 2)
    # 列序必须是 (status, lease_until, id)：只查 index_name 存在的话，建成 (status, id) 也算通过。
    $result.database_checks.outbox_status_lease_index = ((Invoke-MysqlScalar -Database $DbName -Sql "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'event_outbox' AND index_name = 'idx_event_outbox_status_lease';") -eq 'status,lease_until,id')
    $result.database_checks.inbox_status_lease_index = ((Invoke-MysqlScalar -Database $DbName -Sql "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'event_inbox' AND index_name = 'idx_event_inbox_status_lease';") -eq 'status,lease_until,id')
    $result.database_checks.bpm_claim_token_column = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bpm_process_instance_idempotency' AND column_name = 'claim_token';") -eq 1)
    $seedNames = "'admin','zhicloud','yuanma','test','admin107','admin108','admin109','admin110','newobject','aoteman','hrmgr','aotemane','admin123','goudan','wwbwwb','admin1','test01','a00001','aoteman001'"
    $result.database_checks.public_seed_users_after_clean_migration = [int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM system_users WHERE username IN ($seedNames);")

    $resumeExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $migrationEnvironment -LogPath $ResumeLog
    $result.runs.clean_resume = [ordered]@{
        exit_code = $resumeExit
        log_path = $ResumeLog
        log_sha256 = Get-Sha256 -Path $ResumeLog
        applied_migrations = [int](Read-Match -Path $ResumeLog -Pattern 'success: applied=(\d+)' -Default '-1')
        initial_version = Read-Match -Path $ResumeLog -Pattern 'initialVersion=([^,\r\n]+)' -Default ''
        target_version = Read-Match -Path $ResumeLog -Pattern 'targetVersion=([^,\r\n]+)' -Default ''
    }
    New-SandboxDatabase -DatabaseName $ValidationDbName
    Initialize-SandboxCleanFixture -Url $ValidationDbUrl -LogPath $ValidationFixtureLog | Out-Null

    $existingEnvironment = @{
        MASTER_DATASOURCE_URL = $ValidationDbUrl
        MASTER_DATASOURCE_USERNAME = $DbUser
        MASTER_DATASOURCE_PASSWORD = $DbPassword
        MIGRATION_BASELINE_MODE = 'existing'
    }

    $completeBeforeTotal = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $completeBeforeV11 = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    $completeBeforeV11Type = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'type'
    $completeBeforeV11Script = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'script'
    $completeBeforeV11Checksum = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'checksum'
    $completeBeforeDigest = Get-HistoryDigest -DatabaseName $ValidationDbName
    $existingCompleteExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $existingEnvironment -LogPath $ExistingHistoryLog
    $completeAfterTotal = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $completeAfterV11 = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    $completeAfterV11Type = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'type'
    $completeAfterV11Script = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'script'
    $completeAfterV11Checksum = Get-HistoryV11Field -DatabaseName $ValidationDbName -ColumnName 'checksum'
    $completeAfterDigest = Get-HistoryDigest -DatabaseName $ValidationDbName

    $result.runs.existing_complete_history = [ordered]@{
        exit_code = $existingCompleteExit
        applied_migrations = [int](Read-Match -Path $ExistingHistoryLog -Pattern 'success: applied=(\d+)' -Default '-1')
        fixture_exit_code = 0
        fixture_log_path = $ValidationFixtureLog
        fixture_log_sha256 = Get-Sha256 -Path $ValidationFixtureLog
        log_path = $ExistingHistoryLog
        log_sha256 = Get-Sha256 -Path $ExistingHistoryLog
        history_rows_before = $completeBeforeTotal
        history_rows_after = $completeAfterTotal
        v11_rows_before = $completeBeforeV11
        v11_rows_after = $completeAfterV11
        v11_type_before = $completeBeforeV11Type
        v11_type_after = $completeAfterV11Type
        v11_script_before = $completeBeforeV11Script
        v11_script_after = $completeAfterV11Script
        v11_checksum_before = $completeBeforeV11Checksum
        v11_checksum_after = $completeAfterV11Checksum
        history_digest_before = $completeBeforeDigest
        history_digest_after = $completeAfterDigest
    }
    $result.existing_checks.complete_schema_with_v11_accepted = ($existingCompleteExit -eq 0)
    $result.existing_checks.clean_baseline_row_shape_preserved = (
        $completeBeforeV11Type -eq 'BASELINE' -and
        $completeBeforeV11Script -eq '<< Flyway Baseline >>' -and
        $completeBeforeV11Checksum -eq '<NULL>' -and
        $completeAfterV11Type -eq 'BASELINE' -and
        $completeAfterV11Script -eq '<< Flyway Baseline >>' -and
        $completeAfterV11Checksum -eq '<NULL>' -and
        $completeBeforeDigest -eq $completeAfterDigest
    )
    $result.existing_checks.v11_not_reapplied = (
        $existingCompleteExit -eq 0 -and
        $completeBeforeV11 -eq 1 -and
        $completeAfterV11 -eq 1 -and
        $completeAfterTotal -eq $completeBeforeTotal -and
        $result.runs.existing_complete_history.applied_migrations -eq 0
    )

    Invoke-MysqlText -Database $ValidationDbName -Sql "DELETE FROM flyway_schema_history WHERE success = 1 AND version = '1.1';" | Out-Null
    $missingV11BeforeTotal = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $missingV11BeforeRows = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    $missingV11Exit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $existingEnvironment -LogPath $MissingV11Log
    $missingV11AfterTotal = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $missingV11AfterRows = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    $missingV11Text = Read-LogText -Path $MissingV11Log
    $result.runs.existing_missing_v11_history = [ordered]@{
        exit_code = $missingV11Exit
        log_path = $MissingV11Log
        log_sha256 = Get-Sha256 -Path $MissingV11Log
        history_rows_before = $missingV11BeforeTotal
        history_rows_after = $missingV11AfterTotal
        v11_rows_before = $missingV11BeforeRows
        v11_rows_after = $missingV11AfterRows
        log_mentions_v11 = ($missingV11Text -match 'V1\.1')
        log_mentions_history_table = ($missingV11Text -match 'flyway_schema_history')
    }
    $result.existing_checks.missing_v11_history_fail_closed = (
        $missingV11Exit -ne 0 -and
        $missingV11BeforeRows -eq 0 -and
        $missingV11AfterRows -eq 0 -and
        $missingV11AfterTotal -eq $missingV11BeforeTotal -and
        $result.runs.existing_missing_v11_history.log_mentions_v11 -and
        $result.runs.existing_missing_v11_history.log_mentions_history_table
    )

    Invoke-MysqlText -Database $ValidationDbName -Sql 'DROP TABLE flyway_schema_history;' | Out-Null
    $historyTableAfterDrop = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history';")
    $noHistoryExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $existingEnvironment -LogPath $NoHistoryLog
    $historyTableAfterRun = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'flyway_schema_history';")
    $noHistoryText = Read-LogText -Path $NoHistoryLog
    $result.runs.existing_nonempty_without_history = [ordered]@{
        exit_code = $noHistoryExit
        log_path = $NoHistoryLog
        log_sha256 = Get-Sha256 -Path $NoHistoryLog
        history_table_before_run = $historyTableAfterDrop
        history_table_after_run = $historyTableAfterRun
        log_mentions_v11 = ($noHistoryText -match 'V1\.1')
        log_mentions_history_table = ($noHistoryText -match 'flyway_schema_history')
    }
    $result.existing_checks.nonempty_without_history_fail_closed = (
        $noHistoryExit -ne 0 -and
        $historyTableAfterDrop -eq 0 -and
        $historyTableAfterRun -eq 0 -and
        $result.runs.existing_nonempty_without_history.log_mentions_v11 -and
        $result.runs.existing_nonempty_without_history.log_mentions_history_table
    )

    New-SandboxDatabase -DatabaseName $IncompleteDbName
    Initialize-SandboxCleanFixture -Url $IncompleteDbUrl -LogPath $IncompleteFixtureLog | Out-Null
    $incompleteBeforeTotal = [int](Invoke-MysqlScalar -Database $IncompleteDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $incompleteBeforeV11 = [int](Invoke-MysqlScalar -Database $IncompleteDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    Invoke-MysqlText -Database $IncompleteDbName -Sql 'SET FOREIGN_KEY_CHECKS=0; DROP TABLE `system_menu`; SET FOREIGN_KEY_CHECKS=1;' | Out-Null
    $incompleteEnvironment = @{} + $existingEnvironment
    $incompleteEnvironment.MASTER_DATASOURCE_URL = $IncompleteDbUrl
    $incompleteExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $incompleteEnvironment -LogPath $IncompleteSchemaLog
    $incompleteAfterTotal = [int](Invoke-MysqlScalar -Database $IncompleteDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $incompleteAfterV11 = [int](Invoke-MysqlScalar -Database $IncompleteDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")
    $incompleteSchemaText = Read-LogText -Path $IncompleteSchemaLog
    $result.runs.existing_incomplete_schema = [ordered]@{
        exit_code = $incompleteExit
        missing_table = 'system_menu'
        fixture_exit_code = 0
        fixture_log_path = $IncompleteFixtureLog
        fixture_log_sha256 = Get-Sha256 -Path $IncompleteFixtureLog
        log_path = $IncompleteSchemaLog
        log_sha256 = Get-Sha256 -Path $IncompleteSchemaLog
        history_rows_before = $incompleteBeforeTotal
        history_rows_after = $incompleteAfterTotal
        v11_rows_before = $incompleteBeforeV11
        v11_rows_after = $incompleteAfterV11
        log_mentions_missing_table = ($incompleteSchemaText -match 'system_menu')
    }
    $result.existing_checks.incomplete_schema_fail_closed_with_history = (
        $incompleteExit -ne 0 -and
        $result.runs.existing_incomplete_schema.log_mentions_missing_table
    )
    $result.existing_checks.incomplete_schema_history_unchanged = (
        $incompleteBeforeV11 -eq 1 -and
        $incompleteAfterV11 -eq $incompleteBeforeV11 -and
        $incompleteAfterTotal -eq $incompleteBeforeTotal
    )
    if ($resumeExit -ne 0) { throw "clean migration resume 退出码 $resumeExit，见 $ResumeLog" }

    # legacy V1.1 checksum：正例（一次性受控对齐）与负例（未知 checksum fail-closed）
    New-SandboxDatabase -DatabaseName $LegacyDbName
    Initialize-SandboxCleanFixture -Url $LegacyDbUrl -LogPath $LegacyFixtureLog | Out-Null
    $legacyFixtureType = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'type'
    $legacyFixtureScript = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'script'
    $legacyFixtureChecksum = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'checksum'
    $legacyFixtureIsBaseline = (
        $legacyFixtureType -eq 'BASELINE' -and
        $legacyFixtureScript -eq '<< Flyway Baseline >>' -and
        $legacyFixtureChecksum -eq '<NULL>'
    )
    Invoke-MysqlText -Database $LegacyDbName -Sql "INSERT IGNORE INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (0, '1', 'baseline', 'SQL', 'V1__baseline.sql', 348571611, 'legacy-fixture', 0, 1); UPDATE flyway_schema_history SET type = 'SQL', script = 'V1.1__system_base_schema.sql', description = 'system base schema', checksum = -1213470718 WHERE version = '1.1';" | Out-Null
    $legacyFixtureV1Rank = [int](Invoke-MysqlScalar -Database $LegacyDbName -Sql "SELECT installed_rank FROM flyway_schema_history WHERE version = '1' AND success = 1;")
    $legacyFixtureV1Script = Invoke-MysqlScalar -Database $LegacyDbName -Sql "SELECT script FROM flyway_schema_history WHERE version = '1' AND success = 1;"
    $legacyFixtureV1Checksum = Invoke-MysqlScalar -Database $LegacyDbName -Sql "SELECT IFNULL(CAST(checksum AS CHAR), '<NULL>') FROM flyway_schema_history WHERE version = '1' AND success = 1;"
    $legacyFixtureV1RowMatches = ($legacyFixtureV1Rank -eq 0 -and $legacyFixtureV1Script -eq 'V1__baseline.sql' -and $legacyFixtureV1Checksum -eq '348571611')
    $legacyBeforeTotal = [int](Invoke-MysqlScalar -Database $LegacyDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $legacyNonTargetDigestBefore = Get-HistoryDigest -DatabaseName $LegacyDbName -ExcludeVersion '1.1'

    $legacyEnvironment = @{} + $existingEnvironment
    $legacyEnvironment.MASTER_DATASOURCE_URL = $LegacyDbUrl
    $legacyAlignExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $legacyEnvironment -LogPath $LegacyAlignLog
    $legacyAfterTotal = [int](Invoke-MysqlScalar -Database $LegacyDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $legacyAfterType = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'type'
    $legacyAfterScript = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'script'
    $legacyAfterChecksum = Get-HistoryV11Field -DatabaseName $LegacyDbName -ColumnName 'checksum'
    $legacyNonTargetDigestAfter = Get-HistoryDigest -DatabaseName $LegacyDbName -ExcludeVersion '1.1'
    $legacyAfterDigest = Get-HistoryDigest -DatabaseName $LegacyDbName
    $legacyAlignText = Read-LogText -Path $LegacyAlignLog
    $legacyAlignMatch = [regex]::Match($legacyAlignText, 'V1\.1 checksum aligned: legacy=-1213470718 current=(-?\d+) rank=(\d+)')
    $legacyAlignedCurrent = if ($legacyAlignMatch.Success) { $legacyAlignMatch.Groups[1].Value } else { '' }
    $legacyAlignedRank = if ($legacyAlignMatch.Success) { $legacyAlignMatch.Groups[2].Value } else { '' }

    $result.runs.existing_legacy_checksum_alignment = [ordered]@{
        fixture_log_path = $LegacyFixtureLog
        fixture_log_sha256 = Get-Sha256 -Path $LegacyFixtureLog
        fixture_baseline_row_matches = $legacyFixtureIsBaseline
        fixture_v1_row_matches = $legacyFixtureV1RowMatches
        exit_code = $legacyAlignExit
        log_path = $LegacyAlignLog
        log_sha256 = Get-Sha256 -Path $LegacyAlignLog
        history_rows_before = $legacyBeforeTotal
        history_rows_after = $legacyAfterTotal
        checksum_before = '-1213470718'
        checksum_after = $legacyAfterChecksum
        aligned_current_checksum_from_log = $legacyAlignedCurrent
        aligned_rank_from_log = $legacyAlignedRank
        type_after = $legacyAfterType
        script_after = $legacyAfterScript
        non_target_digest_before = $legacyNonTargetDigestBefore
        non_target_digest_after = $legacyNonTargetDigestAfter
    }
    $result.existing_checks.clean_fixture_is_baseline = $legacyFixtureIsBaseline
    $result.existing_checks.legacy_fixture_v1_row_matches = $legacyFixtureV1RowMatches
    $result.existing_checks.legacy_checksum_alignment_succeeded = (
        $legacyFixtureIsBaseline -and
        $legacyFixtureV1RowMatches -and
        $legacyAlignExit -eq 0 -and
        $legacyAlignMatch.Success -and
        $legacyAlignedRank -eq '1' -and
        $legacyAlignedCurrent -eq $legacyAfterChecksum -and
        $legacyAfterChecksum -ne '-1213470718' -and
        $legacyAfterType -eq 'SQL' -and
        $legacyAfterScript -eq 'V1.1__system_base_schema.sql' -and
        $legacyAfterTotal -eq $legacyBeforeTotal -and
        $legacyNonTargetDigestAfter -eq $legacyNonTargetDigestBefore
    )

    $legacyIdempotentExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $legacyEnvironment -LogPath $LegacyIdempotentLog
    $legacyIdempotentDigest = Get-HistoryDigest -DatabaseName $LegacyDbName
    $legacyIdempotentText = Read-LogText -Path $LegacyIdempotentLog
    $result.runs.existing_legacy_checksum_idempotent = [ordered]@{
        exit_code = $legacyIdempotentExit
        applied_migrations = [int](Read-Match -Path $LegacyIdempotentLog -Pattern 'success: applied=(\d+)' -Default '-1')
        log_path = $LegacyIdempotentLog
        log_sha256 = Get-Sha256 -Path $LegacyIdempotentLog
        history_digest_after_align = $legacyAfterDigest
        history_digest_after_rerun = $legacyIdempotentDigest
        log_mentions_alignment = ($legacyIdempotentText -match 'checksum aligned')
    }
    $result.existing_checks.legacy_checksum_alignment_idempotent = (
        $legacyIdempotentExit -eq 0 -and
        -not ($legacyIdempotentText -match 'checksum aligned') -and
        $legacyIdempotentDigest -eq $legacyAfterDigest
    )

    Invoke-MysqlText -Database $LegacyDbName -Sql "UPDATE flyway_schema_history SET checksum = 123456789 WHERE version = '1.1';" | Out-Null
    $legacyRejectDigestBefore = Get-HistoryDigest -DatabaseName $LegacyDbName
    $legacyRejectExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $legacyEnvironment -LogPath $LegacyRejectLog
    $legacyRejectDigestAfter = Get-HistoryDigest -DatabaseName $LegacyDbName
    $legacyRejectText = Read-LogText -Path $LegacyRejectLog
    $result.runs.existing_legacy_checksum_rejected = [ordered]@{
        exit_code = $legacyRejectExit
        log_path = $LegacyRejectLog
        log_sha256 = Get-Sha256 -Path $LegacyRejectLog
        history_digest_before = $legacyRejectDigestBefore
        history_digest_after = $legacyRejectDigestAfter
        log_mentions_unknown_checksum = (($legacyRejectText -match '未知 V1\.1 checksum') -or ($legacyRejectText -match '123456789'))
    }
    $result.existing_checks.unknown_legacy_checksum_fail_closed = (
        $legacyRejectExit -ne 0 -and
        $legacyRejectDigestBefore -eq $legacyRejectDigestAfter -and
        $result.runs.existing_legacy_checksum_rejected.log_mentions_unknown_checksum
    )


    $bootstrapEnvironment = @{
        MASTER_DATASOURCE_URL = $DbUrl
        MASTER_DATASOURCE_USERNAME = $DbUser
        MASTER_DATASOURCE_PASSWORD = $DbPassword
        BOOTSTRAP_ADMIN_MODE = 'ensure'
        BOOTSTRAP_ADMIN_USERNAME = 'zhicloud_admin'
        BOOTSTRAP_ADMIN_PASSWORD = $BootstrapPassword1
        BOOTSTRAP_ADMIN_TENANT_ID = '1'
        BOOTSTRAP_ADMIN_ROLE_ID = '1'
        BOOTSTRAP_ADMIN_POST_ID = '4'
        BOOTSTRAP_ADMIN_BCRYPT_COST = '10'
    }

    $ensureFirstExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $bootstrapEnvironment -LogPath $EnsureFirstLog
    $result.runs.bootstrap_ensure_first = [ordered]@{
        exit_code = $ensureFirstExit
        action = Read-Match -Path $EnsureFirstLog -Pattern 'success: action=([^ ]+)' -Default ''
        log_path = $EnsureFirstLog
        log_sha256 = Get-Sha256 -Path $EnsureFirstLog
    }
    if ($ensureFirstExit -ne 0) { throw "bootstrap ensure 首次执行退出码 $ensureFirstExit，见 $EnsureFirstLog" }

    $ensureRepeatExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $bootstrapEnvironment -LogPath $EnsureRepeatLog
    $result.runs.bootstrap_ensure_repeat_same_password = [ordered]@{
        exit_code = $ensureRepeatExit
        action = Read-Match -Path $EnsureRepeatLog -Pattern 'success: action=([^ ]+)' -Default ''
        log_path = $EnsureRepeatLog
        log_sha256 = Get-Sha256 -Path $EnsureRepeatLog
    }
    if ($ensureRepeatExit -ne 0) { throw "bootstrap ensure 相同口令重复执行退出码 $ensureRepeatExit，见 $EnsureRepeatLog" }

    $hashBeforeMismatch = Invoke-MysqlScalar -Database $DbName -Sql "SELECT password FROM system_users WHERE tenant_id = 1 AND username = 'zhicloud_admin' AND deleted = 0;"
    $mismatchEnvironment = @{} + $bootstrapEnvironment
    $mismatchEnvironment.BOOTSTRAP_ADMIN_PASSWORD = $BootstrapPassword2
    $mismatchExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $mismatchEnvironment -LogPath $EnsureMismatchLog
    $hashAfterMismatch = Invoke-MysqlScalar -Database $DbName -Sql "SELECT password FROM system_users WHERE tenant_id = 1 AND username = 'zhicloud_admin' AND deleted = 0;"
    $result.runs.bootstrap_ensure_mismatch = [ordered]@{
        exit_code = $mismatchExit
        fail_closed = ($mismatchExit -ne 0)
        hash_unchanged = ($hashBeforeMismatch -ceq $hashAfterMismatch)
        log_path = $EnsureMismatchLog
        log_sha256 = Get-Sha256 -Path $EnsureMismatchLog
    }
    if ($mismatchExit -eq 0 -or $hashBeforeMismatch -cne $hashAfterMismatch) { throw 'bootstrap ensure 不同口令未按 fail-closed 处理' }

    $rotateEnvironment = @{} + $bootstrapEnvironment
    $rotateEnvironment.BOOTSTRAP_ADMIN_MODE = 'rotate'
    $rotateEnvironment.BOOTSTRAP_ADMIN_PASSWORD = $BootstrapPassword3
    $rotateExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $rotateEnvironment -LogPath $RotateLog
    $result.runs.bootstrap_rotate = [ordered]@{
        exit_code = $rotateExit
        action = Read-Match -Path $RotateLog -Pattern 'success: action=([^ ]+)' -Default ''
        log_path = $RotateLog
        log_sha256 = Get-Sha256 -Path $RotateLog
    }
    if ($rotateExit -ne 0) { throw "bootstrap rotate 退出码 $rotateExit，见 $RotateLog" }

    $oldPasswordEnvironment = @{} + $bootstrapEnvironment
    $oldPasswordEnvironment.BOOTSTRAP_ADMIN_PASSWORD = $BootstrapPassword1
    $oldPasswordExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $oldPasswordEnvironment -LogPath $EnsureOldLog
    $result.runs.bootstrap_old_password_after_rotate = [ordered]@{
        exit_code = $oldPasswordExit
        rejected = ($oldPasswordExit -ne 0)
        log_path = $EnsureOldLog
        log_sha256 = Get-Sha256 -Path $EnsureOldLog
    }
    if ($oldPasswordExit -eq 0) { throw 'rotate 后旧口令仍被接受' }

    $newPasswordEnvironment = @{} + $bootstrapEnvironment
    $newPasswordEnvironment.BOOTSTRAP_ADMIN_PASSWORD = $BootstrapPassword3
    $newPasswordExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.BootstrapAdminRunner' -Environment $newPasswordEnvironment -LogPath $EnsureNewLog
    $result.runs.bootstrap_new_password_after_rotate = [ordered]@{
        exit_code = $newPasswordExit
        action = Read-Match -Path $EnsureNewLog -Pattern 'success: action=([^ ]+)' -Default ''
        log_path = $EnsureNewLog
        log_sha256 = Get-Sha256 -Path $EnsureNewLog
    }
    if ($newPasswordExit -ne 0) { throw "rotate 后新口令 ensure 退出码 $newPasswordExit，见 $EnsureNewLog" }

    $auditEnvironment = @{ ZCLICLOUD_DB_PASSWORD = $DbPassword }
    Set-ProcessEnvironment -Values $auditEnvironment -Action {
        $auditArguments = @(
            $AuditorPath,
            '--mysql-client', $MysqlClient,
            '--host', $DatabaseHost,
            '--port', [string]$DatabasePort,
            '--database', $DbName,
            '--user', $DbUser,
            '--password-env', 'ZCLICLOUD_DB_PASSWORD',
            '--username', 'zhicloud_admin',
            '--out', $AuditJson
        )
        if ($JavaInSandboxNamespace) {
            # (b) 的最后一格：审计查询也进容器命名空间，于是该形态下没有任何宿主→数据库的网络路径了。
            # 口令仍走环境变量（docker exec -e MYSQL_PWD 从本进程取值），不进 argv。
            $auditArguments = @($auditArguments) + @('--docker-container', $SandboxContainerName)
        }
        # 只读审计器可以安全重试（它只 SELECT），但重试窗口必须窄：
        # 只对传输类错误重试，认证/内容类错误（1045、断言不符）立刻原样交给上层判定，
        # 否则就把"真缺陷被刷掉"变成绿灯——这是 §12.23 反复强调的那条边界。
        $transientPattern = '(?i)(Can''t connect to MySQL server|Lost connection|Communications link failure|ERROR 2003|ERROR 2013|Connection timed out|timed out after)'
        $deterministicPattern = '(?i)(ERROR 1045|Access denied)'
        $attempts = if ($JavaInSandboxNamespace) { [Math]::Max(1, $TransportProbes) } else { 1 }
        for ($attempt = 1; $attempt -le $attempts; $attempt++) {
            $script:lastAuditExitCode = Invoke-ExternalProcess `
                -FilePath $PythonPath `
                -Arguments $auditArguments `
                -LogPath $AuditStdoutLog `
                -TimeoutSeconds $script:OverallTimeoutSeconds
            if ([int]$script:lastAuditExitCode -eq 0) { return }
            if (-not $JavaInSandboxNamespace) { return }
            $auditText = Read-LogText -Path $AuditStdoutLog
            if ($auditText -match $deterministicPattern) { return }
            if ($auditText -notmatch $transientPattern) { return }
            $script:TransportProbeWaits = $script:TransportProbeWaits + 1
            Start-Sleep -Seconds $TransportProbeDelaySeconds
        }
    }
    $auditExit = [int]$script:lastAuditExitCode
    if ($auditExit -ne 0 -and -not (Test-Path -LiteralPath $AuditJson -PathType Leaf)) {
        throw "管理员只读审计失败，退出码 $auditExit，见 $AuditStdoutLog"
    }
    if (-not (Test-Path -LiteralPath $AuditJson -PathType Leaf)) {
        throw "管理员只读审计未生成 JSON，见 $AuditStdoutLog"
    }
    $audit = [System.IO.File]::ReadAllText($AuditJson, [System.Text.UTF8Encoding]::new($false)) | ConvertFrom-Json
    $result.runs.bootstrap_audit = [ordered]@{
        exit_code = $auditExit
        status = $audit.status
        evidence_path = $AuditJson
        evidence_sha256 = Get-Sha256 -Path $AuditJson
        stdout_log_path = $AuditStdoutLog
        stdout_log_sha256 = Get-Sha256 -Path $AuditStdoutLog
    }
    if ($auditExit -ne 0 -or $audit.status -ne 'passed') { throw "管理员只读审计失败，见 $AuditJson" }

    $result.database_checks.post_bootstrap_system_users = [int](Invoke-MysqlScalar -Database $DbName -Sql 'SELECT COUNT(*) FROM system_users WHERE deleted = 0;')
    $result.bootstrap_checks.user_count_exactly_one = ($result.database_checks.post_bootstrap_system_users -eq 1)
    $result.bootstrap_checks.ensure_created = ($result.runs.bootstrap_ensure_first.action -eq 'created')
    $result.bootstrap_checks.ensure_same_password_noop = ($result.runs.bootstrap_ensure_repeat_same_password.action -eq 'noop')
    $result.bootstrap_checks.mismatch_fail_closed = $result.runs.bootstrap_ensure_mismatch.fail_closed
    $result.bootstrap_checks.mismatch_hash_unchanged = $result.runs.bootstrap_ensure_mismatch.hash_unchanged
    $result.bootstrap_checks.rotate_succeeded = ($result.runs.bootstrap_rotate.action -in @('rotated', 'rotated_and_repaired'))
    $result.bootstrap_checks.old_password_rejected_after_rotate = $result.runs.bootstrap_old_password_after_rotate.rejected
    $result.bootstrap_checks.new_password_noop_after_rotate = ($result.runs.bootstrap_new_password_after_rotate.action -eq 'noop')

    $requiredChecks = @(
        $result.database_checks.clean_baseline_success,
        $result.database_checks.version_115_success,
        $result.database_checks.version_116_success,
        $result.database_checks.version_117_success,
        ($result.database_checks.flyway_failed_rows -eq 0),
        ($result.database_checks.pre_bootstrap_system_users -eq 0),
        ($result.database_checks.public_seed_users_after_clean_migration -eq 0),
        $result.database_checks.active_username_unique_index,
        $result.database_checks.receivable_idempotency_unique_index,
        $result.database_checks.version_118_success,
        $result.database_checks.version_119_success,
        $result.database_checks.version_120_success,
        $result.database_checks.version_121_success,
        $result.database_checks.inventory_tenant_unique_index,
        $result.database_checks.outbox_owner_lease_columns,
        $result.database_checks.inbox_owner_lease_columns,
        $result.database_checks.outbox_status_lease_index,
        $result.database_checks.inbox_status_lease_index,
        $result.database_checks.bpm_claim_token_column,
        ($result.runs.clean_first.applied_migrations -gt 0),
        ($result.runs.clean_resume.applied_migrations -eq 0),
        $result.database_checks.clean_baseline_history_row_shape,
        $result.existing_checks.complete_schema_with_v11_accepted,
        $result.existing_checks.clean_baseline_row_shape_preserved,
        $result.existing_checks.v11_not_reapplied,
        $result.existing_checks.clean_fixture_is_baseline,
        $result.existing_checks.legacy_fixture_v1_row_matches,
        $result.existing_checks.legacy_checksum_alignment_succeeded,
        $result.existing_checks.legacy_checksum_alignment_idempotent,
        $result.existing_checks.unknown_legacy_checksum_fail_closed,
        $result.existing_checks.missing_v11_history_fail_closed,
        $result.existing_checks.nonempty_without_history_fail_closed,
        $result.existing_checks.incomplete_schema_fail_closed_with_history,
        $result.existing_checks.incomplete_schema_history_unchanged,
        $result.bootstrap_checks.user_count_exactly_one,
        $result.bootstrap_checks.ensure_created,
        $result.bootstrap_checks.ensure_same_password_noop,
        $result.bootstrap_checks.mismatch_fail_closed,
        $result.bootstrap_checks.mismatch_hash_unchanged,
        $result.bootstrap_checks.rotate_succeeded,
        $result.bootstrap_checks.old_password_rejected_after_rotate,
        $result.bootstrap_checks.new_password_noop_after_rotate
    )
    $result.status = if ($requiredChecks -contains $false) { 'failed' } else { 'passed' }
} catch {
    $result.status = 'failed'
    $result.error = $_.Exception.Message
} finally {
    $result.execution.completed_at = (Get-Date).ToString('o')
    $result.transport = [ordered]@{
        probe_attempts_total = $script:TransportProbeAttempts
        probe_waits          = $script:TransportProbeWaits
        probe_limit_per_step = $TransportProbes
        probe_delay_seconds  = $TransportProbeDelaySeconds
        note                 = '预检只执行 SELECT 1（无副作用），用于规避本机 Docker 发布端口的首连抖动（R-034）；迁移与 bootstrap 调用从未重试，fail-closed 断言的退出码语义未改变。'
    }
    # 清扫结果必须在**写证据时**取，而不是在 [ordered] 字面量里取：字面量在脚本开头求值，
    # 那时第一次 JVM 调用还没发生，$script:StaleEnvSweep 永远是 null ——
    # 于是"字段存在"会伪装成"记录有效"，而实际上它什么都没说（§12.50 的实测教训）。
    if ($null -ne $script:StaleEnvSweep) {
        $result.secret_handling['stale_env_sweep'] = $script:StaleEnvSweep
    }
    $jsonText = ($result | ConvertTo-Json -Depth 12) + "`n"
    [System.IO.File]::WriteAllText($EvidenceJson, $jsonText, [System.Text.UTF8Encoding]::new($false))
    Write-Output "evidence=$EvidenceJson"
    if (Test-Path -LiteralPath $AuditJson) { Write-Output "audit=$AuditJson" }
    Write-Output "status=$($result.status)"
    Write-Output "database=$DbName"
    Write-Output "jar_sha256=$($result.jar.sha256)"
}

if ($result.status -ne 'passed') { exit 1 }
exit 0
