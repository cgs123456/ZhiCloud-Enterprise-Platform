#requires -Version 5.1
<#
.SYNOPSIS
    One-shot recovery of the local sandbox stack: docker containers (mysql/redis/rabbit),
    then the zhicloud-server JVM on 48080, then a business-API health probe.

.DESCRIPTION
    Why this exists: the local stack is started by hand across sessions. After a host
    reboot or a MySQL crash-restart, the three docker containers stay stopped and / or the
    server JVM keeps running with a dead datasource pool ("zombie" connections), which
    turns every business API query into a database error. This script encodes the
    recovery procedure that was previously manual:

      1. docker start the three zc-live-* containers (they persist via volumes; nothing
         here re-creates them - if a container is missing, the script fails closed with
         the documented troubleshooting reference).
      2. wait for TCP readiness of the published ports, plus an authoritative
         mysqladmin ping inside the mysql container.
      3. service JVM: if 48080 is listening AND the business API probe returns code=0,
         leave it running; otherwise (not listening, probe failed, or -ForceRestart)
         kill the old JVM and start it with the same argument set used by the recorded
         hand launch, then wait for the API probe to return code=0.

    The probe uses the sandbox mock token accepted when --zhicloud.security.mock-enable=true;
    tenant 9001 is the seeded sandbox tenant. All native calls go through
    SandboxNative.ps1 (Invoke-BoundedNative) so every subprocess has a hard timeout.

    Passwords are read only from environment variables (never literals):
      - AdminPasswordEnv  (default MYSQL_ROOT_PASSWORD): mysql root password, reused as the
        application datasource master/slave password (they are the same value in this stack).
      - RedisPasswordEnv  (default REDIS_PASSWORD): spring.data.redis.password.

    Evidence: writes logs/restore-local-stack-<stamp>.json under the repository root.

.EXAMPLE
    $env:MYSQL_ROOT_PASSWORD = '...'; $env:REDIS_PASSWORD = '...'
    powershell -ExecutionPolicy Bypass -File scripts/production/restore_local_stack.ps1
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$ContainerMysql = 'zc-live-mysql',
    [string]$ContainerRedis = 'zc-live-redis',
    [string]$ContainerRabbit = 'zc-live-rabbit',
    [int]$DatabasePort = 55101,
    [int]$RedisPort = 16380,
    [int]$RabbitPort = 56721,
    [int]$ServerPort = 48080,
    [string]$AdminPasswordEnv = 'MYSQL_ROOT_PASSWORD',
    [string]$RedisPasswordEnv = 'REDIS_PASSWORD',
    [string]$JavaExe,
    [string]$DatabaseName = 'zhicloud_sandbox_v140',
    [switch]$SkipService,
    [switch]$ForceRestart,
    [int]$ContainerTimeoutSeconds = 300,
    [int]$BootTimeoutSeconds = 1800,
    [int]$ProbeTimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'
$startedAt = Get-Date
$stamp = $startedAt.ToString('yyyyMMdd_HHmmss')
if (-not $RepoRoot) {
    # scripts/production/restore_local_stack.ps1 -> repo root is two levels up.
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
}
. (Join-Path $PSScriptRoot 'SandboxNative.ps1')

function Assert-LoopbackProbeHost {
    # fail-closed: this tool only touches services on loopback ports of this machine.
    param([Parameter(Mandatory = $true)][string]$HostName, [string]$Stage = 'startup')
    $allowed = @('127.0.0.1', '::1', 'localhost')
    if ($allowed -notcontains $HostName) {
        throw "Refusing non-loopback host '$HostName' at stage $Stage; this recovery tool is restricted to the local sandbox only."
    }
}
Assert-LoopbackProbeHost -HostName '127.0.0.1' -Stage 'startup'

if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($AdminPasswordEnv))) {
    throw "Environment variable $AdminPasswordEnv (mysql root password, also used as datasource password) is not set."
}
$mysqlRootPassword = [Environment]::GetEnvironmentVariable($AdminPasswordEnv)
$redisPassword = [Environment]::GetEnvironmentVariable($RedisPasswordEnv)
if (-not $SkipService -and [string]::IsNullOrWhiteSpace($redisPassword)) {
    throw "Environment variable $RedisPasswordEnv is not set; it is required to boot the server JVM."
}

function Get-ContainerRunningState {
    param([Parameter(Mandatory = $true)][string]$Name, [string]$Stage)
    $run = Invoke-BoundedNative -FilePath 'docker.exe' -Arguments @(
        'inspect', '--format', '{{.State.Running}}', $Name
    ) -TimeoutSeconds 60 -Stage $Stage
    if ($run.ExitCode -ne 0) {
        if ($run.StdErr -match 'No such object') {
            throw "Container '$Name' does not exist on this machine. Recovery cannot re-create it here; see docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md for the original docker run commands (volumes: zc-live-mysql-data)."
        }
        throw "docker inspect for '$Name' failed (exit $($run.ExitCode)): $($run.StdErr)"
    }
    return $run.StdOut.Trim() -eq 'true'
}

function Wait-DatabasePort {
    param([Parameter(Mandatory = $true)][int]$Port, [Parameter(Mandatory = $true)][int]$TimeoutSeconds)
    # Authoritative mysql readiness: mysqladmin ping inside the container with the root account.
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = ''
    while ((Get-Date) -lt $deadline) {
        $run = Invoke-BoundedNative -FilePath 'docker.exe' -Arguments @(
            'exec', $ContainerMysql, 'sh', '-c', "mysqladmin ping -h 127.0.0.1 --password='$mysqlRootPassword'"
        ) -TimeoutSeconds 60 -Stage 'mysqladmin-ping'
        if ($run.ExitCode -eq 0 -and $run.StdOut -match 'mysqld is alive') {
            return $true
        }
        $lastError = ($run.StdOut + ' ' + $run.StdErr).Trim()
        Start-Sleep -Seconds 5
    }
    throw "MySQL inside $ContainerMysql not ready within ${TimeoutSeconds}s (last ping: $lastError)"
}

function Wait-TcpPort {
    param([Parameter(Mandatory = $true)][int]$Port, [Parameter(Mandatory = $true)][string]$Label, [Parameter(Mandatory = $true)][int]$TimeoutSeconds)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $opened = $client.ConnectAsync('127.0.0.1', $Port).Wait(2000)
            if ($opened -and $client.Connected) { return $true }
        } catch {
            # retry until deadline
        } finally {
            $client.Close()
        }
        Start-Sleep -Seconds 3
    }
    throw "$Label TCP 127.0.0.1:$Port not reachable within ${TimeoutSeconds}s"
}

function Test-ServerEndpoint {
    # Business probe mirrors the hand-verified query used across sessions:
    # GET /admin-api/crm/clue/page with the local mock token and tenant 9001 -> {"code":0,...}
    $uri = "http://127.0.0.1:$ServerPort/admin-api/crm/clue/page?pageNo=1&pageSize=1"
    try {
        $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 15 -Headers @{
            'Authorization' = 'Bearer test-only-for-local900100'
            'tenant-id'     = '9001'
        }
        if ($response.StatusCode -ne 200) { return $false }
        $body = [string]$response.Content
        # 5.1-safe containment check: both `"code":0` and `"code": 0` shapes are accepted.
        if ($body -contains '"code":0' -or $body -match '"code"\s*:\s*0(\D|$)') { return $true }
        return $false
    } catch {
        return $false
    }
}

function Get-ServerOwningProcess {
    try {
        $conns = @(Get-NetTCPConnection -LocalPort $ServerPort -State Listen -ErrorAction SilentlyContinue)
        if ($conns.Count -gt 0) { return $conns[0].OwningProcess }
    } catch {
        # Get-NetTCPConnection missing on some downlevel hosts; fall back to netstat.
        $lines = & netstat -ano | Select-String ":$ServerPort .*LISTENING"
        if ($lines) {
            $procId = ($lines[0].ToString().Trim() -split '\s+')[-1]
            return [int]$procId
        }
    }
    return $null
}

function Start-ServerJvm {
    param([Parameter(Mandatory = $true)][string]$JavaPath)
    $logsDir = Join-Path $RepoRoot 'logs'
    if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir | Out-Null }
    $outLog = Join-Path $logsDir "restore-server-$stamp.log"
    $errLog = Join-Path $logsDir "restore-server-$stamp.err.log"
    $datasourceUrl = "jdbc:mysql://127.0.0.1:$DatabasePort/$DatabaseName" +
        '?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' +
        '&nullCatalogMeansCurrent=true&rewriteBatchedStatements=true'
    $arguments = @(
        "-Dspring.datasource.dynamic.datasource.master.password=$mysqlRootPassword",
        "-Dspring.datasource.dynamic.datasource.slave.password=$mysqlRootPassword",
        "-Dspring.datasource.dynamic.datasource.master.url=$datasourceUrl",
        "-Dspring.datasource.dynamic.datasource.slave.url=$datasourceUrl",
        '-Dspring.data.redis.host=127.0.0.1',
        "-Dspring.data.redis.port=$RedisPort",
        "-Dspring.data.redis.password=$redisPassword",
        '-Xms512m', '-Xmx1536m', '-XX:+UseSerialGC',
        '-Djava.net.preferIPv4Stack=true',
        '-jar', 'zhicloud-server/target/zhicloud-server.jar',
        '--spring.profiles.active=local',
        "--server.port=$ServerPort",
        "--spring.data.rabbitmq.port=$RabbitPort",
        '--spring.main.allow-circular-references=true',
        '--zhicloud.security.mock-enable=true',
        '--zhicloud.captcha.enable=false'
    )
    # Start-Process (not Invoke-BoundedNative): the JVM must outlive this script.
    # Each array element is one argv token; java parses its own argument line, so the
    # URL characters (&, =, :) need no shell quoting. Passwords land in the JVM
    # process command line - the same exposure as every recorded hand launch.
    $proc = Start-Process -FilePath $JavaPath -ArgumentList $arguments `
        -WorkingDirectory $RepoRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    return @{ ProcessId = $proc.Id; StdOutLog = $outLog; StdErrLog = $errLog }
}

if (-not $JavaExe) {
    $resolved = Get-Command 'java.exe' -ErrorAction SilentlyContinue
    if ($resolved) { $JavaExe = $resolved.Source } else { $JavaExe = 'java' }
}

$result = [ordered]@{
    started_at = $startedAt.ToString('yyyy-MM-dd HH:mm:ss')
    containers = [ordered]@{}
    service    = $null
    duration_seconds = $null
}

$containerNames = @($ContainerMysql, $ContainerRedis, $ContainerRabbit)
foreach ($name in $containerNames) {
    $running = Get-ContainerRunningState -Name $name -Stage "inspect-$name"
    if (-not $running) {
        $startRun = Invoke-BoundedNative -FilePath 'docker.exe' -Arguments @('start', $name) `
            -TimeoutSeconds 120 -Stage "start-$name"
        if ($startRun.ExitCode -ne 0) {
            throw "docker start $name failed: $($startRun.StdErr)"
        }
    }
    $running = Get-ContainerRunningState -Name $name -Stage "verify-$name"
    if (-not $running) {
        throw "Container $name reports Running=false after docker start."
    }
    $result.containers[$name] = 'running'
}

Wait-TcpPort -Port $DatabasePort -Label 'mysql' -TimeoutSeconds $ContainerTimeoutSeconds
Wait-TcpPort -Port $RedisPort -Label 'redis' -TimeoutSeconds $ContainerTimeoutSeconds
Wait-TcpPort -Port $RabbitPort -Label 'rabbitmq' -TimeoutSeconds $ContainerTimeoutSeconds
Wait-DatabasePort -Port $DatabasePort -TimeoutSeconds $ContainerTimeoutSeconds

if ($SkipService) {
    $result.service = 'skipped by -SkipService'
    $result.duration_seconds = [int]((Get-Date) - $startedAt).TotalSeconds
    $finishingAt = Get-Date
    $result.finished_at = $finishingAt.ToString('yyyy-MM-dd HH:mm:ss')
    $json = $result | ConvertTo-Json -Depth 4
    $logPath = Join-Path $RepoRoot "logs\restore-local-stack-$stamp.json"
    New-Item -ItemType Directory -Force -Path (Join-Path $RepoRoot 'logs') | Out-Null
    [System.IO.File]::WriteAllText($logPath, $json, [System.Text.UTF8Encoding]::new($false))
    Write-Host "containers recovered; service handling skipped (-SkipService). summary: $logPath"
    exit 0
}

$owned = Get-ServerOwningProcess
$listening = ($null -ne $owned)
$healthy = $listening -and (Test-ServerEndpoint)

if ($listening -and $healthy -and -not $ForceRestart) {
    Write-Host "server already running (pid $owned) and business probe returns code=0; leaving it untouched."
    $result.service = "already healthy (pid $owned); no restart"
} else {
    if ($listening) {
        Write-Host "stopping previous server JVM (pid $owned) because ForceRestart=$ForceRestart probeHealthy=$healthy"
        $kill = Invoke-BoundedNative -FilePath 'taskkill.exe' -Arguments @('/F', '/T', "/PID", "$owned") `
            -TimeoutSeconds 60 -Stage 'stop-previous-jvm'
        if ($kill.ExitCode -ne 0) {
            throw "taskkill for pid $owned failed: $($kill.StdErr)"
        }
        Start-Sleep -Seconds 3
    }
    $boot = Start-ServerJvm -JavaPath $JavaExe
    Write-Host "started server JVM pid $($boot.ProcessId); stdout -> $($boot.StdOutLog); stderr -> $($boot.StdErrLog)"
    $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
    $healthy = $false
    $lastProbe = 'not probed yet'
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 10
        $alive = Get-Process -Id $boot.ProcessId -ErrorAction SilentlyContinue
        if (-not $alive) {
            throw "server JVM pid $($boot.ProcessId) exited during boot; see $($boot.StdErrLog)"
        }
        if (Test-ServerEndpoint) {
            $healthy = $true
            $lastProbe = 'code=0'
            break
        } else {
            $lastProbe = 'probe failed this round'
        }
    }
    if (-not $healthy) {
        # Daemon discipline rule 3 (seam contract, cf. native_mysqld_sandbox.ps1): reap on failure -
        # a JVM that failed to become healthy must not be left behind for the caller to clean up.
        # taskkill after the timeout, then fail; ignore a taskkill error when the JVM already exited.
        Invoke-BoundedNative -FilePath 'taskkill.exe' -Arguments @('/F', '/T', "/PID", "$($boot.ProcessId)") `
            -TimeoutSeconds 60 -Stage 'reap-boot-failed-jvm' | Out-Null
        throw "server JVM did not become healthy within ${BootTimeoutSeconds}s ($lastProbe); see $($boot.StdOutLog)"
    }
    $result.service = "restarted (pid $($boot.ProcessId)); probe code=0 after $([int]((Get-Date) - $startedAt).TotalSeconds)s"
}

$result.duration_seconds = [int]((Get-Date) - $startedAt).TotalSeconds
$finishingAt = Get-Date
$result.finished_at = $finishingAt.ToString('yyyy-MM-dd HH:mm:ss')
$json = $result | ConvertTo-Json -Depth 4
$logPath = Join-Path $RepoRoot "logs\restore-local-stack-$stamp.json"
New-Item -ItemType Directory -Force -Path (Join-Path $RepoRoot 'logs') | Out-Null
[System.IO.File]::WriteAllText($logPath, $json, [System.Text.UTF8Encoding]::new($false))
Write-Host "local stack recovered. summary: $logPath"
exit 0
