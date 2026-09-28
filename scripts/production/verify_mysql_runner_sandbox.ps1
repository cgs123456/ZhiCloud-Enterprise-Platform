#requires -Version 5.1
<#
.SYNOPSIS
  使用本机 MySQL 沙箱验证新构建 JAR 的 clean migration 与管理员 bootstrap 闭环。

.DESCRIPTION
  脚本只创建全新的 zhicloud_runner_clean_<时间戳> 和独立的
  zhicloud_runner_existing_<时间戳>/zhicloud_runner_incomplete_<时间戳> 数据库及专用账号，
  不会覆盖旧证据库，也不会把数据库口令或 bootstrap 明文口令写入 evidence。

  验证内容：
    1. clean baseline + Flyway V2..V117 首次迁移；
    2. 相同 clean 模式重复执行，必须 applied=0；
    3. existing 模式在完整 schema 且存在成功 V1.1 history 时必须通过，并且不得重复执行 V1.1；
    4. existing 模式在 history 缺少成功 V1.1 时必须 fail-closed，且不得自动补写 V1.1；
    5. existing 模式在非空 legacy schema 缺少 history 时必须 fail-closed，且不得创建基线；
    6. existing 模式在完整 history 但缺少基础表时必须 fail-closed；
    7. 管理员 ensure 首次创建、相同口令 no-op、不同口令 fail-closed；
    8. rotate 更新口令，旧口令随后必须失败；
    9. 只读审计器检查管理员、角色、岗位、索引与哈希形态。

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
    [int]$TimeoutSeconds = 1800
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

if ($TimeoutSeconds -le 0) {
    throw 'TimeoutSeconds 必须大于 0'
}
$script:OverallTimeoutSeconds = $TimeoutSeconds

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
$DbUser = 'zhicloud_run_' + $Stamp
if ($DbName.Length -gt 64) { throw "数据库名过长: $DbName" }
if ($ValidationDbName.Length -gt 64) { throw "数据库名过长: $ValidationDbName" }
if ($IncompleteDbName.Length -gt 64) { throw "数据库名过长: $IncompleteDbName" }
if ($DbUser.Length -gt 32) { throw "数据库账号过长: $DbUser" }

# 只在本进程内存在；不写入日志、evidence 或 argv。
$DbPassword = 'Zc!' + [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N').Substring(0, 8) + '9a'
$BootstrapPassword1 = 'Zc!' + [guid]::NewGuid().ToString('N') + '9a'
$BootstrapPassword2 = 'Zc!' + [guid]::NewGuid().ToString('N') + '7b'
$BootstrapPassword3 = 'Zc!' + [guid]::NewGuid().ToString('N') + '5c'

$DbUrl = "jdbc:mysql://${DatabaseHost}:${DatabasePort}/${DbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$ValidationDbUrl = "jdbc:mysql://${DatabaseHost}:${DatabasePort}/${ValidationDbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
$IncompleteDbUrl = "jdbc:mysql://${DatabaseHost}:${DatabasePort}/${IncompleteDbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
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
$AuditJson = Join-Path $EvidenceDir ($LogBase + '-audit.json')
$AuditStdoutLog = Join-Path $EvidenceDir ($LogBase + '-audit.stdout.log')
$EvidenceJson = Join-Path $EvidenceDir ($LogBase + '.json')

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
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

function ConvertTo-WindowsProcessArgument {
    param([AllowEmptyString()][string]$Argument)

    if ($null -eq $Argument -or $Argument.Length -eq 0) { return '""' }
    if ($Argument -notmatch '[\s"]') { return $Argument }

    $builder = New-Object System.Text.StringBuilder
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Argument.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
            continue
        }
        if ($character -eq '"') {
            [void]$builder.Append('\' * (($backslashes * 2) + 1))
            [void]$builder.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append('\' * $backslashes)
            $backslashes = 0
        }
        [void]$builder.Append($character)
    }
    if ($backslashes -gt 0) {
        [void]$builder.Append('\' * ($backslashes * 2))
    }
    [void]$builder.Append('"')
    return $builder.ToString()
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

function Invoke-ExternalProcess {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$LogPath,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )

    if ($null -ne $script:Deadline) {
        $remainingSeconds = [int][Math]::Floor(($script:Deadline - (Get-Date)).TotalSeconds)
    } else {
        $remainingSeconds = $TimeoutSeconds
    }
    if ($remainingSeconds -le 0) {
        throw "整体执行超过 ${TimeoutSeconds} 秒"
    }
    if ($remainingSeconds -gt $TimeoutSeconds) {
        $remainingSeconds = $TimeoutSeconds
    }

    $processInfo = New-Object System.Diagnostics.ProcessStartInfo
    $processInfo.FileName = $FilePath
    $processInfo.Arguments = (($Arguments | ForEach-Object { ConvertTo-WindowsProcessArgument -Argument ([string]$_) }) -join ' ')
    $processInfo.UseShellExecute = $false
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $processInfo.CreateNoWindow = $true
    $processInfo.StandardOutputEncoding = [System.Text.Encoding]::UTF8
    $processInfo.StandardErrorEncoding = [System.Text.Encoding]::UTF8
    $processInfo.WorkingDirectory = $RepoRoot

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $processInfo
    try {
        if (-not $process.Start()) { throw "无法启动进程: $FilePath" }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $exited = $process.WaitForExit([int]($remainingSeconds * 1000))
        if (-not $exited) {
            try { $process.Kill() } catch { }
            $process.WaitForExit()
            $timeoutText = "[timeout] process exceeded ${remainingSeconds}s`n"
            $combined = $timeoutText + $stdoutTask.Result + $stderrTask.Result
            [System.IO.File]::WriteAllText($LogPath, $combined, [System.Text.UTF8Encoding]::new($false))
            throw "进程超时: $FilePath (${remainingSeconds}s)"
        }

        $combined = $stdoutTask.Result + $stderrTask.Result
        [System.IO.File]::WriteAllText($LogPath, $combined, [System.Text.UTF8Encoding]::new($false))
        return [int]$process.ExitCode
    } finally {
        $process.Dispose()
    }
}

function Invoke-MysqlText {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database
    )
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
        $output = $Sql | & $MysqlClient @arguments 2>&1
        $exitCode = $LASTEXITCODE
        if ($exitCode -ne 0) {
            $text = ($output | Out-String).Trim()
            throw "mysql 退出码 $exitCode`: $text"
        }
        return (($output | Out-String).Trim())
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

function New-SandboxDatabase {
    param([Parameter(Mandatory = $true)][string]$DatabaseName)

    Invoke-MysqlText -Sql "CREATE DATABASE ``$DatabaseName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    Invoke-MysqlText -Sql "GRANT ALL PRIVILEGES ON ``$DatabaseName``.* TO '$DbUser'@'127.0.0.1'; FLUSH PRIVILEGES;" | Out-Null
}

function Invoke-JavaMain {
    param(
        [Parameter(Mandatory = $true)][string]$MainClass,
        [Parameter(Mandatory = $true)][hashtable]$Environment,
        [Parameter(Mandatory = $true)][string]$LogPath
    )

    Set-ProcessEnvironment -Values $Environment -Action {
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

if ([string]::IsNullOrWhiteSpace($Reviewer)) {
    $Reviewer = if (-not [string]::IsNullOrWhiteSpace($env:USERNAME)) { $env:USERNAME } else { 'unassigned' }
}
$ReviewerSource = if ($PSBoundParameters.ContainsKey('Reviewer')) { 'explicit' } else { 'os_user_default' }

$JarSha256 = Get-Sha256 -Path $JarPath
$AuditorSha256 = Get-Sha256 -Path $AuditorPath
$ScriptSha256 = Get-Sha256 -Path $ScriptPath

$MysqlClientVersion = ''
try {
    $MysqlClientVersion = ((& $MysqlClient --version 2>&1) | Out-String).Trim()
} catch {
    $MysqlClientVersion = ''
}
$JavaVersion = ''
try {
    $JavaVersion = ((& $JavaPath -version 2>&1) | Out-String).Trim()
} catch {
    $JavaVersion = ''
}
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
    "-TimeoutSeconds $TimeoutSeconds"
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
    validation_databases = [ordered]@{
        existing_history = $ValidationDbName
        incomplete_schema = $IncompleteDbName
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
    secret_handling = [ordered]@{
        database_password_persisted = $false
        bootstrap_plaintext_password_persisted = $false
        note = '数据库口令和 bootstrap 明文口令只存在于本次 PowerShell 进程；evidence 仅记录退出码、日志摘要和数据库查询结果。'
    }
    caveats = @(
        '本验证仅覆盖本机 MySQL 8.4.9 沙箱，不证明目标生产数据库、TLS、Kubernetes 或 ArgoCD 部署已通过。',
        'V116/V117 的并发与软删除语义未由本脚本证明。',
        'BPM/Flowable 事务回滚、备份恢复、容量与高可用未由本脚本证明。',
        '工作区存在未提交改动时，commit_sha 不能单独唯一标识被验证代码；必须结合 JAR 哈希、脚本哈希和 git status 哈希复核。',
        'reviewer 默认为执行主机用户，不代表独立复核；生产放行前必须补充独立复核人。',
        'existing 成功场景由 clean baseline 构造，只证明成功 V1.1 history 不会被重复执行；不证明外部 legacy schema 的列、索引和约束均已通过强签名校验。',
        'Production readiness 和 Gate 3 仍为 false，R-011 和 R-029 仍为 OPEN，直到所有阻断项关闭。'
    )
}

try {
    Invoke-MysqlText -Sql "CREATE DATABASE ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    $createUserSql = "CREATE USER '$DbUser'@'127.0.0.1' IDENTIFIED BY '$DbPassword'; GRANT ALL PRIVILEGES ON ``$DbName``.* TO '$DbUser'@'127.0.0.1'; FLUSH PRIVILEGES;"
    Invoke-MysqlText -Sql $createUserSql | Out-Null
    $result.mysql.server_version = Invoke-MysqlScalar -Sql 'SELECT VERSION();'

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
    $result.database_checks.version_115_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '115';") -eq 1)
    $result.database_checks.version_116_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '116';") -eq 1)
    $result.database_checks.version_117_success = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '117';") -eq 1)
    $result.database_checks.active_username_unique_index = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'system_users' AND index_name = 'uk_system_users_tenant_active_username';") -gt 0)
    $result.database_checks.receivable_idempotency_unique_index = ([int](Invoke-MysqlScalar -Database $DbName -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'crm_receivable' AND index_name = 'uk_crm_receivable_idempotency';") -gt 0)
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
    $existingCompleteExit = Invoke-JavaMain -MainClass 'cn.zhicloud.server.DatabaseMigrationRunner' -Environment $existingEnvironment -LogPath $ExistingHistoryLog
    $completeAfterTotal = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql 'SELECT COUNT(*) FROM flyway_schema_history;')
    $completeAfterV11 = [int](Invoke-MysqlScalar -Database $ValidationDbName -Sql "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND version = '1.1';")

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
    }
    $result.existing_checks.complete_schema_with_v11_accepted = ($existingCompleteExit -eq 0)
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
        $script:lastAuditExitCode = Invoke-ExternalProcess `
            -FilePath $PythonPath `
            -Arguments $auditArguments `
            -LogPath $AuditStdoutLog `
            -TimeoutSeconds $script:OverallTimeoutSeconds
    }
    $auditExit = [int]$script:lastAuditExitCode
    if ($auditExit -ne 0 -and -not (Test-Path -LiteralPath $AuditJson -PathType Leaf)) {
        throw "管理员只读审计失败，退出码 $auditExit，见 $AuditStdoutLog"
    }
    if (-not (Test-Path -LiteralPath $AuditJson -PathType Leaf)) {
        throw "管理员只读审计未生成 JSON，见 $AuditStdoutLog"
    }
    $audit = Get-Content -LiteralPath $AuditJson -Raw -Encoding UTF8 | ConvertFrom-Json
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
        ($result.runs.clean_first.applied_migrations -gt 0),
        ($result.runs.clean_resume.applied_migrations -eq 0),
        $result.existing_checks.complete_schema_with_v11_accepted,
        $result.existing_checks.v11_not_reapplied,
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
