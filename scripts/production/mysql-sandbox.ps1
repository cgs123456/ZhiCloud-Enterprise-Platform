# 一次性 MySQL 8 迁移沙箱：每次 start 都创建随机容器 + 全新临时卷，stop 时彻底删除。
# 退出码约定：0=成功；2=沙箱校验或健康检查失败；1=用法错误或 Docker 执行错误。
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('start', 'health', 'stop')]
    [string]$Command,

    [string]$RepoRoot,

    # 必须 digest 锁定，避免沙箱和生产用到的 MySQL 版本漂移。
    [string]$Image = 'mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d',
    [string]$Name,
    [string]$Volume,
    [string]$BindAddress = '127.0.0.1',
    [string]$DatabaseHost = 'localhost',
    [string]$SandboxDatabase = 'zhicloud_sandbox',
    [string]$StateFile,
    [string]$EvidenceFile,
    [switch]$SkipEvidence,
    [switch]$DryRun,
    [int]$ReadyTimeoutSeconds = 240
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 下 $PSScriptRoot 在 param 默认值里可能为空，因此统一在运行期解析脚本目录。
function Get-ScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    if ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
        return (Split-Path -Parent $MyInvocation.MyCommand.Path)
    }
    throw 'Unable to resolve script directory.'
}

$namePattern = '^zhicloud-mysql-sandbox-[0-9a-f]{8}$'
$volumePattern = '^zhicloud-mysql-sandbox-data-[0-9a-f]{8}$'
$allowedLocalHosts = @('127.0.0.1', '::1', 'localhost')

function Get-RepoRoot {
    if (-not [string]::IsNullOrWhiteSpace($RepoRoot)) {
        return [System.IO.Path]::GetFullPath($RepoRoot)
    }
    return (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
}

function Get-DefaultEvidenceFile {
    $root = Get-RepoRoot
    return (Join-Path (Split-Path -Parent $root) 'docs\enterprise-platform\evidence\production\mysql-sandbox.json')
}

function Invoke-Docker {
    # 统一捕获 docker 的 stdout/stderr 与退出码；绝不吞掉错误。
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure
    )
    $output = & docker @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($output | Out-String).Trim()
    if ($exitCode -ne 0 -and -not $AllowFailure) {
        throw "docker $($Arguments -join ' ') failed with exit code $exitCode. Output: $text"
    }
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = $text
    }
}

function Test-VolumeExists {
    param([Parameter(Mandatory = $true)][string]$VolumeName)
    $result = Invoke-Docker -Arguments @('volume', 'ls', '--format', '{{.Name}}', '--filter', "name=^$VolumeName`$") -AllowFailure
    if ($result.ExitCode -ne 0) { throw "Unable to list docker volumes: $($result.Output)" }
    return (($result.Output -split "`r?`n") -contains $VolumeName)
}

function Assert-GlobalGuards {
    # 这些守卫不依赖 state，必须先执行：否则给出的是“state 缺失”而不是真正的参数问题。
    if ($Image -match 'latest') {
        throw "Refusing mutable image tag 'latest'. Pin mysql:8.4 with a @sha256 digest."
    }
    if ($Image -notmatch '@sha256:[0-9a-f]{64}$') {
        throw "Refusing image '$Image': the sandbox requires a digest-pinned image (image@sha256:<64 hex>)."
    }
    if ($BindAddress -notin $allowedLocalHosts) {
        throw "Refusing bind address '$BindAddress': the sandbox may only listen on localhost (127.0.0.1 / ::1)."
    }
    if ($DatabaseHost -notin $allowedLocalHosts) {
        throw "Refusing database host '$DatabaseHost': the sandbox only ever talks to localhost, never to a remote or production database."
    }
}

function Assert-IdentityGuards {
    param([string]$ResolvedName, [string]$ResolvedVolume)

    if (-not [string]::IsNullOrWhiteSpace($Name) -and $Name -notmatch $namePattern) {
        throw "Refusing container name '$Name': sandbox containers must use a random name matching zhicloud-mysql-sandbox-<8 hex>."
    }
    if (-not [string]::IsNullOrWhiteSpace($Volume) -and $Volume -notmatch $volumePattern) {
        throw "Refusing volume name '$Volume': the sandbox requires a fresh random volume matching zhicloud-mysql-sandbox-data-<8 hex>."
    }
    if ($ResolvedName -and $ResolvedName -notmatch $namePattern) {
        throw "Refusing container name '$ResolvedName': sandbox containers must use a random name matching zhicloud-mysql-sandbox-<8 hex>."
    }
    if ($ResolvedVolume -and $ResolvedVolume -notmatch $volumePattern) {
        throw "Refusing volume name '$ResolvedVolume': the sandbox requires a fresh random volume matching zhicloud-mysql-sandbox-data-<8 hex>."
    }
}

function New-SandboxIdentity {
    $suffix = [guid]::NewGuid().ToString('N').Substring(0, 8)
    return [pscustomobject]@{
        Suffix = $suffix
        Name   = "zhicloud-mysql-sandbox-$suffix"
        Volume = "zhicloud-mysql-sandbox-data-$suffix"
    }
}

function New-SandboxPassword {
    $bytes = New-Object 'byte[]' 24
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return (($bytes | ForEach-Object { $_.ToString('x2') }) -join '')
}

function Read-SandboxState {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Sandbox state file not found: $Path. Run 'start' first."
    }
    return ([System.IO.File]::ReadAllText($Path) | ConvertFrom-Json)
}

function Write-JsonFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Payload
    )
    $directory = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    # 用 .NET 写出无 BOM 的 UTF-8：Windows PowerShell 5.1 的 Set-Content -Encoding utf8
    # 会写入 BOM，破坏严格 JSON 解析器（如 Python json.loads）的读取。
    $jsonText = ($Payload | ConvertTo-Json -Depth 8) + "`n"
    [System.IO.File]::WriteAllText($Path, $jsonText, [System.Text.UTF8Encoding]::new($false))
}

function Get-SandboxMetrics {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][string]$RootPassword,
        [Parameter(Mandatory = $true)][string]$Database
    )

    $sqlPath = Join-Path (Join-Path (Get-ScriptDirectory) 'sql') 'sandbox_health.sql'
    if (-not (Test-Path -LiteralPath $sqlPath -PathType Leaf)) {
        throw "Health SQL not found: $sqlPath"
    }
    $sqlText = [System.IO.File]::ReadAllText($sqlPath)

    $result = $sqlText | & docker exec -i -e "MYSQL_PWD=$RootPassword" $ContainerName mysql --batch --raw --skip-column-names -uroot --default-character-set=utf8mb4 $Database 2>&1
    $exitCode = $LASTEXITCODE
    $text = ($result | Out-String)
    if ($exitCode -ne 0) {
        throw "Sandbox health SQL failed with exit code $exitCode. Output: $($text.Trim())"
    }

    $metrics = [ordered]@{}
    foreach ($line in ($text -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $parts = $line -split "`t", 2
        if ($parts.Count -lt 2) { throw "Unexpected health SQL output line: $line" }
        $metrics[$parts[0].Trim()] = $parts[1].Trim()
    }
    if ($metrics.Count -eq 0) {
        throw 'Sandbox health SQL returned no rows.'
    }
    return $metrics
}

function Test-SandboxMetrics {
    param([Parameter(Mandatory = $true)]$Metrics)

    $checks = New-Object System.Collections.ArrayList
    $reasons = New-Object System.Collections.ArrayList

    $expectations = @(
        [pscustomobject]@{ name = 'character_set_server';    expected = 'utf8mb4';                  passed = ([string]$Metrics['character_set_server'] -eq 'utf8mb4');                    actual = [string]$Metrics['character_set_server'] },
        [pscustomobject]@{ name = 'collation_server';        expected = 'utf8mb4*';                 passed = ([string]$Metrics['collation_server'] -like 'utf8mb4*');                     actual = [string]$Metrics['collation_server'] },
        [pscustomobject]@{ name = 'session_time_zone';       expected = 'Asia/Shanghai (+08:00)';   passed = ([string]$Metrics['session_time_zone'] -in @('+08:00', 'Asia/Shanghai'));    actual = [string]$Metrics['session_time_zone'] },
        [pscustomobject]@{ name = 'now_utc_offset';          expected = '08:00:00';                 passed = ([string]$Metrics['now_utc_offset'] -eq '08:00:00');                         actual = [string]$Metrics['now_utc_offset'] },
        [pscustomobject]@{ name = 'sql_mode_strict';         expected = 'contains STRICT_TRANS_TABLES'; passed = ([string]$Metrics['sql_mode'] -match 'STRICT_TRANS_TABLES');             actual = [string]$Metrics['sql_mode'] },
        [pscustomobject]@{ name = 'server_version';          expected = '8.4.x';                    passed = ([string]$Metrics['server_version'] -match '^8\.4\.');                       actual = [string]$Metrics['server_version'] },
        [pscustomobject]@{ name = 'sandbox_database_present'; expected = 'yes';                     passed = ([string]$Metrics['sandbox_database_present'] -eq 'yes');                    actual = [string]$Metrics['sandbox_database_present'] }
    )

    foreach ($item in $expectations) {
        [void]$checks.Add([ordered]@{
            check    = $item.name
            expected = $item.expected
            actual   = $item.actual
            passed   = [bool]$item.passed
        })
        if (-not $item.passed) {
            [void]$reasons.Add("$($item.name) expected '$($item.expected)' but got '$($item.actual)'")
        }
    }

    return [pscustomobject]@{
        checks          = $checks.ToArray()
        failure_reasons = $reasons.ToArray()
    }
}

function Get-HostPort {
    param([Parameter(Mandatory = $true)][string]$ContainerName)
    $result = Invoke-Docker -Arguments @('port', $ContainerName, '3306/tcp')
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line -match '^(?<host>127\.0\.0\.1):(?<port>\d+)$') {
            return [int]$Matches.port
        }
    }
    throw "Unable to resolve the localhost port published by $ContainerName. Output: $($result.Output)"
}

function Wait-SandboxReady {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][string]$RootPassword
    )
    $deadline = (Get-Date).AddSeconds($ReadyTimeoutSeconds)
    $lastError = ''
    while ((Get-Date) -lt $deadline) {
        # 两个条件缺一不可：
        # 1) 必须走 TCP —— 镜像初始化阶段的临时 mysqld 只监听 socket，socket 探测会过早返回成功；
        # 2) 必须做带认证的真实查询 —— mysqladmin ping 在 Access denied 时依然返回 0。
        $probe = & docker exec -e "MYSQL_PWD=$RootPassword" $ContainerName mysql --protocol=TCP --host=127.0.0.1 --port=3306 -uroot --batch --skip-column-names -e 'SELECT 1' 2>&1
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0 -and ($probe | Out-String).Trim() -eq '1') {
            return $true
        }
        $lastError = ($probe | Out-String).Trim()
        Start-Sleep -Seconds 3
    }
    throw "Sandbox container $ContainerName did not become ready within $ReadyTimeoutSeconds seconds. Last error: $lastError"
}

function Write-SandboxEvidence {
    param(
        [Parameter(Mandatory = $true)][string]$Status,
        [Parameter(Mandatory = $true)][string]$Step,
        $State,
        $Metrics,
        $Checks = @(),
        [string[]]$FailureReasons = @(),
        $Extra = @{}
    )

    $payload = [ordered]@{
        status          = $Status
        step            = $Step
        generated_at    = [DateTimeOffset]::UtcNow.ToString('o')
        image           = $Image
        repo_root       = (Get-RepoRoot)
        container_name  = if ($State) { $State.container_name } else { $Name }
        volume          = if ($State) { $State.volume } else { $Volume }
        bind_address    = $BindAddress
        host_port       = if ($State) { $State.host_port } else { $null }
        metrics         = $Metrics
        checks          = $Checks
        failure_reasons = $FailureReasons
        note            = 'Sandbox only. Never point these scripts at production data.'
    }
    foreach ($key in $Extra.Keys) { $payload[$key] = $Extra[$key] }

    if (-not $SkipEvidence) {
        Write-JsonFile -Path $EvidenceFile -Payload $payload
    }
    return $payload
}

# ---------------------------------------------------------------------------
# 参数解析与守卫
# ---------------------------------------------------------------------------

$RepoRoot = Get-RepoRoot
if ([string]::IsNullOrWhiteSpace($StateFile)) {
    $StateFile = Join-Path $env:TEMP 'zhicloud-mysql-sandbox\state.json'
}
$evidenceProvided = -not [string]::IsNullOrWhiteSpace($EvidenceFile)
if (-not $evidenceProvided) { $EvidenceFile = Get-DefaultEvidenceFile }
$StateFile = [System.IO.Path]::GetFullPath($StateFile)
$EvidenceFile = [System.IO.Path]::GetFullPath($EvidenceFile)

Assert-GlobalGuards

$state = $null
if ($Command -eq 'health' -or $Command -eq 'stop') {
    $state = Read-SandboxState -Path $StateFile
    $resolvedName = $state.container_name
    $resolvedVolume = $state.volume
}
else {
    $identity = New-SandboxIdentity
    $resolvedName = if ([string]::IsNullOrWhiteSpace($Name)) { $identity.Name } else { $Name }
    $resolvedVolume = if ([string]::IsNullOrWhiteSpace($Volume)) { $identity.Volume } else { $Volume }
}

Assert-IdentityGuards -ResolvedName $resolvedName -ResolvedVolume $resolvedVolume

# 密码占位符让 DryRun 与真实运行共用同一份参数，且 DryRun 绝不打印密码。
$passwordPlaceholder = '__ROOT_PASSWORD__'
$dockerRunArgs = @(
    'run', '--detach',
    '--name', $resolvedName,
    '--label', 'zhicloud.sandbox=true',
    '--label', "zhicloud.sandbox.volume=$resolvedVolume",
    '--publish', "$BindAddress`::3306",
    '--env', "MYSQL_ROOT_PASSWORD=$passwordPlaceholder",
    '--env', "MYSQL_DATABASE=$SandboxDatabase",
    '--mount', "type=volume,src=$resolvedVolume,dst=/var/lib/mysql",
    $Image,
    '--character-set-server=utf8mb4',
    '--collation-server=utf8mb4_0900_ai_ci',
    '--default-time-zone=+08:00'
)

$volumeCheck = 'not_applicable'
if ($Command -eq 'start') {
    if (Test-Path -LiteralPath $StateFile -PathType Leaf) {
        throw "Sandbox state file already exists: $StateFile. Run 'stop' first; sandbox volumes are never reused."
    }
    if ($DryRun) {
        # DryRun 承诺「不接触任何外部状态」；查卷需要 docker daemon，
        # 因此只在真实启动时执行，并在输出里显式标注该守卫被跳过（不能静默）。
        $volumeCheck = 'skipped: dry-run does not contact the docker daemon'
    }
    else {
        if (Test-VolumeExists -VolumeName $resolvedVolume) {
            throw "Refusing to start: volume '$resolvedVolume' already exists. The sandbox never reuses a volume."
        }
        $volumeCheck = 'checked: volume does not exist'
    }
}

if ($DryRun) {
    [ordered]@{
        command          = $Command
        image            = $Image
        container_name   = $resolvedName
        volume           = $resolvedVolume
        bind_address     = $BindAddress
        sandbox_database = $SandboxDatabase
        state_file       = $StateFile
        evidence_file    = $EvidenceFile
        volume_check     = $volumeCheck
        docker_run_args  = @($dockerRunArgs)
        note             = 'DryRun only: nothing was created and no password is shown.'
    } | ConvertTo-Json -Depth 8 -Compress
    exit 0
}

switch ($Command) {
    'start' {
        $rootPassword = New-SandboxPassword
        Invoke-Docker -Arguments @('volume', 'create', $resolvedVolume) | Out-Null
        Write-Output "Created sandbox volume $resolvedVolume"

        # --env 元素是 "MYSQL_ROOT_PASSWORD=<placeholder>"，必须整条替换，否则容器真的会用占位符当密码。
$passwordArgument = "MYSQL_ROOT_PASSWORD=$passwordPlaceholder"
$runArgs = @($dockerRunArgs | ForEach-Object { if ($_ -eq $passwordArgument) { "MYSQL_ROOT_PASSWORD=$rootPassword" } else { $_ } })
        $startedAt = Get-Date
        Invoke-Docker -Arguments $runArgs | Out-Null
        Write-Output "Started sandbox container $resolvedName"

        try {
            Wait-SandboxReady -ContainerName $resolvedName -RootPassword $rootPassword | Out-Null
        }
        catch {
            Write-Output "Sandbox container left running for inspection: $resolvedName"
            throw
        }

        $hostPort = Get-HostPort -ContainerName $resolvedName
        $statePayload = [ordered]@{
            container_name = $resolvedName
            volume         = $resolvedVolume
            image          = $Image
            bind_address   = $BindAddress
            host_port      = $hostPort
            database       = $SandboxDatabase
            root_password  = $rootPassword
            created_at     = (Get-Date).ToString('o')
            repo_root      = $RepoRoot
            note           = 'Ephemeral sandbox credentials; never reuse for production.'
        }
        Write-JsonFile -Path $StateFile -Payload $statePayload

        $metrics = Get-SandboxMetrics -ContainerName $resolvedName -RootPassword $rootPassword -Database $SandboxDatabase
        $verdict = Test-SandboxMetrics -Metrics $metrics
        $status = if ($verdict.failure_reasons.Count -eq 0) { 'passed' } else { 'failed' }
        Write-SandboxEvidence -Status $status -Step 'start' -State $statePayload -Metrics $metrics -Checks $verdict.checks -FailureReasons $verdict.failure_reasons -Extra ([ordered]@{
            ready_seconds = [math]::Round(((Get-Date) - $startedAt).TotalSeconds, 1)
            sql_mode_source = 'server_default'
        }) | Out-Null

        if ($status -ne 'passed') {
            [Console]::Error.WriteLine("Sandbox health checks failed: $($verdict.failure_reasons -join '; ')")
            exit 2
        }
        Write-Output "Sandbox $resolvedName ready on 127.0.0.1:$hostPort (database=$SandboxDatabase, server=$($metrics['server_version']), sql_mode=$($metrics['sql_mode']))"
    }
    'health' {
        $inspect = Invoke-Docker -Arguments @('inspect', '--format', '{{.State.Running}}', $resolvedName) -AllowFailure
        if ($inspect.ExitCode -ne 0) {
            [Console]::Error.WriteLine("Sandbox container is not available: $resolvedName. Output: $($inspect.Output)")
            exit 2
        }
        if ($inspect.Output.Trim() -ne 'true') {
            [Console]::Error.WriteLine("Sandbox container $resolvedName is not running.")
            exit 2
        }

        $metrics = Get-SandboxMetrics -ContainerName $resolvedName -RootPassword $state.root_password -Database $state.database
        $verdict = Test-SandboxMetrics -Metrics $metrics
        $status = if ($verdict.failure_reasons.Count -eq 0) { 'passed' } else { 'failed' }
        Write-SandboxEvidence -Status $status -Step 'health' -State $state -Metrics $metrics -Checks $verdict.checks -FailureReasons $verdict.failure_reasons | Out-Null

        if ($status -ne 'passed') {
            [Console]::Error.WriteLine("Sandbox health checks failed: $($verdict.failure_reasons -join '; ')")
            exit 2
        }
        Write-Output "Sandbox health: OK (server=$($metrics['server_version']), charset=$($metrics['character_set_server']), time_zone=$($metrics['session_time_zone']), sql_mode=$($metrics['sql_mode']))"
    }
    'stop' {
        # 默认不要把拆卸证据写到健康证据同一个文件里，否则会覆盖 start/health 留下的指标快照。
        if (-not $evidenceProvided) {
            $EvidenceFile = Join-Path (Split-Path -Parent (Get-RepoRoot)) 'docs\enterprise-platform\evidence\production\mysql-sandbox-teardown.json'
        }
        $removeContainer = Invoke-Docker -Arguments @('rm', '--force', $resolvedName) -AllowFailure
        if ($removeContainer.ExitCode -ne 0 -and $removeContainer.Output -notmatch 'No such container') {
            throw "Unable to remove sandbox container '$resolvedName': $($removeContainer.Output)"
        }
        $removeVolume = Invoke-Docker -Arguments @('volume', 'rm', '--force', $resolvedVolume) -AllowFailure
        if ($removeVolume.ExitCode -ne 0) {
            throw "Unable to remove sandbox volume '$resolvedVolume': $($removeVolume.Output)"
        }

        Write-SandboxEvidence -Status 'stopped' -Step 'stop' -State $state -Metrics $null -Extra ([ordered]@{
            container_removed = ($removeContainer.ExitCode -eq 0)
            volume_removed    = ($removeVolume.ExitCode -eq 0)
        }) | Out-Null

        if (Test-Path -LiteralPath $StateFile -PathType Leaf) {
            Remove-Item -LiteralPath $StateFile -Force
        }
        Write-Output "Stopped and removed sandbox $resolvedName (volume $resolvedVolume deleted)"
    }
}
