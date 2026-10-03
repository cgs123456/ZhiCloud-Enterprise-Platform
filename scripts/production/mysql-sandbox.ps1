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

    # R-033 方向3（责任人 2026-09-30 裁决）：在 mysqld 自己的网络命名空间里跑一个回环中继，
    # 使宿主侧 JDBC/mysql 客户端被 mysqld 看作 127.0.0.1，从而不必把 CREATE USER 的 @'127.0.0.1'
    # 放宽到 Docker 网关地址。默认关闭：不加 -RelayLoopback 时行为与改动前逐字节一致。
    [switch]$RelayLoopback,
    # 中继实现必须用真正的 TCP 代理：busybox nc 的 accept/exec 在 30 次"新 JVM 首连"采样里丢了 3 次
    # （08S01 Communications link failure，而 mysqld 侧 Aborted_connects=0，说明 SYN 根本没到 mysqld）；
    # 同一采样下 nginx stream 代理 30/30 通过。见 handoff §12.21。
    [string]$RelayImage = 'nginx:1.30.5-alpine@sha256:0985e772fb9f729e6fa0980da05fca5d9c468e870eed43071545afa9d2e27d94',
    [int]$RelayListenPort = 13306,

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
$relayNamePattern = '^zhicloud-mysql-sandbox-relay-[0-9a-f]{8}$'
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

# 唯一的原生进程执行器（硬超时 + 杀进程树），全仓库一份实现，各沙箱脚本一律 dot-source。
# `docker run -d` 在镜像仓库卡住时会无限挂起，此前 $ReadyTimeoutSeconds 只管"等就绪"，
# 完全没管"起容器"这一步（登记的 R-G3.5-2）。
$script:NativeTimeoutSeconds = if ($ReadyTimeoutSeconds -gt 0) { $ReadyTimeoutSeconds } else { 240 }
. (Join-Path (Get-ScriptDirectory) 'SandboxNative.ps1')
. (Join-Path (Get-ScriptDirectory) 'ImageProvenance.ps1')

function Resolve-SandboxImageProvenance {
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [Parameter(Mandatory = $true)][string]$PinnedImage,
        [Parameter(Mandatory = $true)][string]$Stage
    )
    # R-G3.5-3：钉摘要不等于"用的就是那个摘要的镜像"。容器 .Image 只是本地 image ID，
    # 必须再经 RepoDigests 反查一次才能声称溯源成立。
    $idRun = Invoke-BoundedNative -FilePath 'docker' -Arguments @('inspect', '--format', '{{.Image}}', $ContainerName) `
        -TimeoutSeconds $script:NativeTimeoutSeconds -Stage $Stage
    if ($idRun.ExitCode -ne 0) {
        throw "Unable to resolve the image id of $ContainerName ($Stage): $(($idRun.StdOut + [Environment]::NewLine + $idRun.StdErr).Trim())"
    }
    $imageId = $idRun.StdOut.Trim()
    $digestRun = Invoke-BoundedNative -FilePath 'docker' -Arguments @('image', 'inspect', '--format', '{{json .RepoDigests}}', $imageId) `
        -TimeoutSeconds $script:NativeTimeoutSeconds -Stage $Stage
    if ($digestRun.ExitCode -ne 0) {
        throw "Unable to resolve RepoDigests of $imageId ($Stage): $(($digestRun.StdOut + [Environment]::NewLine + $digestRun.StdErr).Trim())"
    }
    $digests = @(ConvertFrom-RepoDigestsJson -Json $digestRun.StdOut)
    return Assert-ImageDigestPinned -ImageId $imageId -RepoDigests $digests -PinnedImage $PinnedImage -Stage $Stage
}

function Invoke-Docker {
    # 统一捕获 docker 的 stdout/stderr 与退出码；绝不吞掉错误。
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure
    )
    # ErrorActionPreference=Stop 会把原生命令的任何 stderr 输出升级为终止性异常，
    # 即便退出码为 0（例如 docker 的弃用警告）。执行器在宿主之外跑子进程，
    # 两条流分别捕获、不并入管道，所以调用点既不会被 stderr 打断，也不会丢掉退出码。
    $run = Invoke-BoundedNative -FilePath 'docker' -Arguments $Arguments -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'docker'
    $text = ($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim()
    if ($run.ExitCode -ne 0 -and -not $AllowFailure) {
        throw "docker $($Arguments -join ' ') failed with exit code $($run.ExitCode). Output: $text"
    }
    return [pscustomobject]@{
        ExitCode = $run.ExitCode
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
    if ($RelayLoopback) {
        # 中继是新增的供应链面：与 mysqld 镜像同样必须 digest 钉定，且只监听容器命名空间内的
        # 一个端口；不做这两条校验就等于引入一个可变标签的额外容器去转发数据库流量。
        if ($RelayImage -match 'latest') {
            throw "Refusing mutable relay image tag 'latest'. Pin the relay image with a @sha256 digest."
        }
        if ($RelayImage -notmatch '@sha256:[0-9a-f]{64}$') {
            throw "Refusing relay image '$RelayImage': the relay requires a digest-pinned image (image@sha256:<64 hex>)."
        }
        if ($RelayListenPort -lt 1 -or $RelayListenPort -gt 65535) {
            throw "Refusing relay listen port '${RelayListenPort}': must be 1-65535."
        }
        if ($RelayListenPort -eq 3306) {
            throw 'Refusing relay listen port 3306: it would collide with the direct mysqld mapping.'
        }
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

function Set-SandboxMysqlPasswordEnv {
    param([Parameter(Mandatory = $true)][string]$RootPassword)
    # 口令只进本进程环境，由 docker CLI 继承给 `-e MYSQL_PWD`（不带 =值）。
    # 写成 `-e MYSQL_PWD=<值>` 会把它放进 docker 进程的命令行，任何进程监视器都能读到（R-041）。
    # 代价是调用方必须自己把 MYSQL_PWD 保留到该阶段结束：中途清掉会让后续查询静默变成"无密码连接"。
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $RootPassword, 'Process')
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
    Set-SandboxMysqlPasswordEnv -RootPassword $RootPassword

    $run = Invoke-BoundedNative -FilePath 'docker' -Arguments @(
        'exec', '-i', '-e', 'MYSQL_PWD', $ContainerName,
        'mysql', '--batch', '--raw', '--skip-column-names', '-uroot',
        '--default-character-set=utf8mb4', $Database
    ) -StandardInput $sqlText -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'health-sql'
    if ($run.ExitCode -ne 0) {
        throw "Sandbox health SQL failed with exit code $($run.ExitCode). Output: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
    }
    # 只用 stdout 供下面解析：循环要求每一行都是 key<TAB>value，
    # 而 mysql 的告警写在 stderr——混流会把一条无害告警变成"Unexpected health SQL output line"假失败。
    $text = $run.StdOut

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
    param(
        [Parameter(Mandatory = $true)][string]$ContainerName,
        [int]$ContainerPort = 3306
    )
    $result = Invoke-Docker -Arguments @('port', $ContainerName, "$ContainerPort/tcp")
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line -match '^(?<host>127\.0\.0\.1):(?<port>\d+)$') {
            return [int]$Matches.port
        }
    }
    throw "Unable to resolve the localhost port published by $ContainerName for $ContainerPort/tcp. Output: $($result.Output)"
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
        # 单条探测必须有自己的短超时：否则一次挂住的 `docker exec` 会让上面的 $deadline 循环
        # 形同虚设（登记的 R-G3.5-2）。执行器在宿主之外运行子进程并分别捕获两条流，
        # 所以探测期间 mysql 写到 stderr 的 "Can't connect" 既不会被 ErrorActionPreference=Stop
        # 升级成终止性异常而中止重试，也不会污染 -eq '1' 的结果判定。
        $probeTimeout = [Math]::Min(30, $ReadyTimeoutSeconds)
        Set-SandboxMysqlPasswordEnv -RootPassword $RootPassword
        $probe = Invoke-BoundedNative -FilePath 'docker' -Arguments @(
            'exec', '-e', 'MYSQL_PWD', $ContainerName,
            'mysql', '--protocol=TCP', '--host=127.0.0.1', '--port=3306', '-uroot',
            '--batch', '--skip-column-names', '-e', 'SELECT 1'
        ) -TimeoutSeconds $probeTimeout -Stage 'readiness-probe'
        if ($probe.ExitCode -eq 0 -and $probe.StdOut -eq '1') {
            return $true
        }
        $lastError = ($probe.StdOut + [Environment]::NewLine + $probe.StdErr).Trim()
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
        relay_container = if ($State) { $State.relay_container } else { $relayContainerName }
        relay_port      = if ($State) { $State.relay_port } else { $null }
        relay_image     = if ($State) { $State.relay_image } else { if ($RelayLoopback) { $RelayImage } else { $null } }
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

# R-042 的边界（不需要改 state 的关联方式就能立刻收紧的一条）：state JSON 里有明文 root_password，
# 默认落在每用户 TEMP 并由 stop 消费掉；但若调用方把 -StateFile 指进仓库工作树，
# 一份含口令的文件就会和源码一起被复制、备份，甚至被下一次 git add 带走。
# 这里只拒绝"仓库内"这一个方向，EvidenceFile 仍允许（证据不含口令，且约定就存放在仓库内未跟踪目录）。
$repoPrefix = (Get-RepoRoot).TrimEnd('\', '/') + [System.IO.Path]::DirectorySeparatorChar
$stateTail = $StateFile.Substring(0, [Math]::Min($repoPrefix.Length, $StateFile.Length))
if ($stateTail.Equals($repoPrefix, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing -StateFile '$StateFile': the sandbox state JSON carries MYSQL_ROOT_PASSWORD in cleartext, and this path sits inside the repository working tree '$repoPrefix'. Keep state under the per-user temp directory (the default) so it cannot be copied or committed alongside the source."
}

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

# 中继容器名由沙箱名派生，派生结果必须再过一遍身份守卫：
# 若将来名字格式变了，宁可在这里 fail-closed，也不要悄悄起出一个不受约束的容器。
$relayContainerName = if ($RelayLoopback) { ($resolvedName -replace '^(zhicloud-mysql-sandbox)-([0-9a-f]{8})$', '$1-relay-$2') } else { $null }
if ($RelayLoopback -and $relayContainerName -notmatch $relayNamePattern) {
    throw "Refusing derived relay container name '${relayContainerName}': it must match ${relayNamePattern}."
}
if ($RelayLoopback) {
    # 把中继的监听端口也发布到宿主回环：Docker 将其 DNAT 到同一网络命名空间内 sidecar 的 socket，
    # sidecar 再连 127.0.0.1:3306 —— mysqld 看到的来源因此就是回环。发布顺序必须留在镜像参数之前。
    $insertAt = $dockerRunArgs.IndexOf('--mount')
    if ($insertAt -lt 0) { throw 'Cannot locate --mount in docker run arguments; relay publish insertion refused.' }
    $dockerRunArgs = @(
        $dockerRunArgs[0..($insertAt - 1)]
        '--publish'
        "$BindAddress`::$RelayListenPort"
        $dockerRunArgs[$insertAt..($dockerRunArgs.Count - 1)]
    )
}

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
        relay_enabled    = [bool]$RelayLoopback
        relay_container  = $relayContainerName
        relay_image      = if ($RelayLoopback) { $RelayImage } else { $null }
        relay_container_port = if ($RelayLoopback) { $RelayListenPort } else { $null }
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
        # 就绪之后才做溯源：镜像与摘要不符必须在任何验证之前失败，否则后面的"绿灯"都建立在未知产物上。
        $provenance = Resolve-SandboxImageProvenance -ContainerName $resolvedName -PinnedImage $Image -Stage 'sandbox-image-provenance'
        Write-Output "Sandbox image provenance verified: $($provenance.matched_entry)"
        $relayProvenance = $null
        $relayPort = $null
        if ($RelayLoopback) {
            # sidecar 用 --network container:<mysqld> 加入同一网络命名空间；它连的是本命名空间的
            # 127.0.0.1:3306，因此 mysqld 观察到回环来源，@'127.0.0.1' 授权无需放宽（R-033 方向3）。
            # 用 nginx stream 而不是 busybox nc：nc 的 accept 在高连接 churn 下会丢包（§12.21 实测 3/30）。
            $relayConfLine = 'stream { server { listen ' + $RelayListenPort + '; proxy_pass 127.0.0.1:3306; proxy_timeout 600s; } }'
            $relayShell = 'printf "%s\n" "pid /tmp/nginx.pid;" "error_log /dev/stderr;" "events {}" "' + $relayConfLine + '" > /tmp/relay.conf; exec nginx -c /tmp/relay.conf -g "daemon off;"'
            Invoke-Docker -Arguments @(
                'run', '--detach',
                '--name', $relayContainerName,
                '--network', "container:$resolvedName",
                '--label', 'zhicloud.sandbox=true',
                '--label', "zhicloud.sandbox.parent=$resolvedName",
                '--entrypoint', 'sh',
                $RelayImage,
                '-c', $relayShell
            ) | Out-Null
            $relayPort = Get-HostPort -ContainerName $resolvedName -ContainerPort $RelayListenPort
            $relayProvenance = Resolve-SandboxImageProvenance -ContainerName $relayContainerName -PinnedImage $RelayImage -Stage 'relay-image-provenance'
            Write-Output "Started loopback relay $relayContainerName (127.0.0.1:$relayPort -> $resolvedName 127.0.0.1:3306; image $($relayProvenance.matched_entry))"
        }
        $statePayload = [ordered]@{
            container_name = $resolvedName
            volume         = $resolvedVolume
            image          = $Image
            bind_address   = $BindAddress
            host_port      = $hostPort
            relay_container = $relayContainerName
            relay_port      = $relayPort
            relay_image     = if ($RelayLoopback) { $RelayImage } else { $null }
            relay_container_port = if ($RelayLoopback) { $RelayListenPort } else { $null }
            image_id = $provenance.image_id
            image_matched_digest = $provenance.pinned_digest
            image_repo_digests = @($provenance.repo_digests)
            relay_image_id = if ($relayProvenance) { $relayProvenance.image_id } else { $null }
            relay_image_matched_digest = if ($relayProvenance) { $relayProvenance.pinned_digest } else { $null }
            relay_image_repo_digests = if ($relayProvenance) { @($relayProvenance.repo_digests) } else { @() }
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
            image_provenance = [ordered]@{
                sandbox = [ordered]@{
                    image_id = $provenance.image_id
                    pinned_digest = $provenance.pinned_digest
                    matched_entry = $provenance.matched_entry
                    repo_digests = @($provenance.repo_digests)
                }
                relay = if ($relayProvenance) { [ordered]@{
                    image_id = $relayProvenance.image_id
                    pinned_digest = $relayProvenance.pinned_digest
                    matched_entry = $relayProvenance.matched_entry
                    repo_digests = @($relayProvenance.repo_digests)
                } } else { $null }
                note = '溯源依据是 image 的 RepoDigests 是否包含钉定摘要；容器 .Image 只是本地 image ID，单独引用它不足以证明用的是哪个 registry 版本。'
            }
        }) | Out-Null

        if ($status -ne 'passed') {
            [Console]::Error.WriteLine("Sandbox health checks failed: $($verdict.failure_reasons -join '; ')")
            exit 2
        }
        Write-Output "Sandbox $resolvedName ready on 127.0.0.1:$hostPort (database=$SandboxDatabase, server=$($metrics['server_version']), sql_mode=$($metrics['sql_mode']))"
        if ($RelayLoopback) {
            Write-Output "Loopback relay ready on 127.0.0.1:$relayPort —— 需要 @'127.0.0.1' 授权的宿主侧验证请把 -DatabasePort 指向该端口（来源会被 mysqld 看作 127.0.0.1）。"
        }
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
        # 先删中继。实测纠正：Docker 允许强删父容器，所以顺序反了不会"删不掉"，
        # 而是把还在使用其网络命名空间的 sidecar 变成无人认领的孤儿（R-038）。
        $relayNameFromState = if ($state.PSObject.Properties.Name -contains 'relay_container') { [string]$state.relay_container } else { '' }
        if ([string]::IsNullOrWhiteSpace($relayNameFromState)) {
            $relayNameFromState = ($resolvedName -replace '^(zhicloud-mysql-sandbox)-([0-9a-f]{8})$', '$1-relay-$2')
        }
        $removeRelay = Invoke-Docker -Arguments @('rm', '--force', $relayNameFromState) -AllowFailure
        if ($removeRelay.ExitCode -ne 0 -and $removeRelay.Output -notmatch 'No such container') {
            throw "Unable to remove sandbox relay container '$relayNameFromState': $($removeRelay.Output)"
        }

        # R-038：命名空间里可能有 state 根本不认识的 sidecar（本轮就出现过手工起的 zhicloud-relay-nginx）。
        # 光删派生命名会让它继续 Up 且无人回收，所以按实际网络模式枚举一遍再删。
        # 两个实测坑：`.HostConfig.NetworkMode` 只有 `docker inspect` 支持（`docker ps --format` 会报
        # "can't evaluate field HostConfig"）；且它回显的是 **父容器的 64 位 ID**（`container:<id>`），
        # 不是父容器名——只比名字时漏删过，故三种形式都算命中。
        $attached = @()
        $parentId = ''
        $parentInspect = Invoke-Docker -Arguments @('inspect', '--format', '{{.Id}}', $resolvedName) -AllowFailure
        if ($parentInspect.ExitCode -eq 0) { $parentId = $parentInspect.Output.Trim() }
        $namespaceKeys = @("container:$resolvedName")
        foreach ($candidate in @($parentId, ($parentId -replace '^(.{12}).*$', '$1'))) {
            if ($candidate -match '^[0-9a-f]{12,64}$') { $namespaceKeys += "container:$candidate" }
        }
        $listIds = Invoke-Docker -Arguments @('ps', '--all', '--quiet') -AllowFailure
        if ($listIds.ExitCode -ne 0) {
            throw "Unable to list containers while cleaning $resolvedName : $($listIds.Output)"
        }
        $ids = @($listIds.Output -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^[0-9a-f]{12,64}$' })
        for ($offset = 0; $offset -lt $ids.Count; $offset += 50) {
            $chunk = @($ids[$offset..([Math]::Min($offset + 49, $ids.Count - 1))])
            if ($chunk.Count -eq 0) { continue }
            $listed = Invoke-Docker -Arguments (@('inspect', '--format', '{{.Name}}|{{.HostConfig.NetworkMode}}') + $chunk) -AllowFailure
            if ($listed.ExitCode -ne 0) {
                throw "Unable to inspect containers while cleaning $resolvedName : $($listed.Output)"
            }
            foreach ($line in ($listed.Output -split "`r?`n")) {
                $parts = $line -split '\|', 2
                if ($parts.Count -lt 2) { continue }
                if ($namespaceKeys -contains $parts[1].Trim()) { $attached += ($parts[0].Trim() -replace '^/', '') }
            }
        }
        $detached = @()
        foreach ($attachedName in $attached) {
            $removeAttached = Invoke-Docker -Arguments @('rm', '--force', $attachedName) -AllowFailure
            if ($removeAttached.ExitCode -ne 0 -and $removeAttached.Output -notmatch 'No such container') {
                throw "Unable to remove container '$attachedName' attached to sandbox namespace: $($removeAttached.Output)"
            }
            $detached += $attachedName
        }

        $removeContainer = Invoke-Docker -Arguments @('rm', '--force', $resolvedName) -AllowFailure
        $stillAttached = @()
        foreach ($attachedName in $detached) {
            $check = Invoke-Docker -Arguments @('ps', '--all', '--quiet', '--filter', "name=^$attachedName`$") -AllowFailure
            if ($check.Output.Trim().Length -gt 0) { $stillAttached += $attachedName }
        }
        if ($stillAttached.Count -gt 0) {
            throw "Sandbox teardown left containers attached to $resolvedName namespace: $($stillAttached -join ', ')"
        }
        if ($removeContainer.ExitCode -ne 0 -and $removeContainer.Output -notmatch 'No such container') {
            throw "Unable to remove sandbox container '$resolvedName': $($removeContainer.Output)"
        }
        $removeVolume = Invoke-Docker -Arguments @('volume', 'rm', '--force', $resolvedVolume) -AllowFailure
        if ($removeVolume.ExitCode -ne 0) {
            throw "Unable to remove sandbox volume '$resolvedVolume': $($removeVolume.Output)"
        }

        Write-SandboxEvidence -Status 'stopped' -Step 'stop' -State $state -Metrics $null -Extra ([ordered]@{
            container_removed = ($removeContainer.ExitCode -eq 0)
            relay_removed     = ($removeRelay.ExitCode -eq 0)
            relay_absent      = ($removeRelay.Output -match 'No such container')
            detached_sidecars = @($detached)
            volume_removed    = ($removeVolume.ExitCode -eq 0)
        }) | Out-Null

        if (Test-Path -LiteralPath $StateFile -PathType Leaf) {
            Remove-Item -LiteralPath $StateFile -Force
        }
        Write-Output "Stopped and removed sandbox $resolvedName (volume $resolvedVolume deleted)"
    }
}
