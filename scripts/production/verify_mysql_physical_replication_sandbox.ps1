#requires -Version 5.1
<#
Isolated two-container MySQL physical binlog replication approximation.

This is deliberately NOT a production acceptance test:
- both containers run on one Docker host;
- the network is a local Docker bridge, not a production topology;
- there is no HA, failover, real network partition, production data, or production scale;
- the evidence always keeps production_ready=false and gate3_passed=false.

The script creates two digest-pinned MySQL 8.4 containers, configures GTID-based
source/replica replication, verifies a small synthetic dataset, read-only
enforcement, replica catch-up after a controlled restart, applies a minimal
V100->V104->V107->V120 application migration chain, replicates Outbox/Inbox
fixtures, performs a one-way source-stop/replica-promotion exercise, and runs
direct SQL owner-fencing assertions. It removes the temporary resources unless
-KeepSandbox is supplied.
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$Image = 'mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d',
    [string]$EvidenceDir,
    [int]$ReadyTimeoutSeconds = 240,
    [int]$ReplicationTimeoutSeconds = 180,
    [switch]$KeepSandbox,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
# fail-closed: this script creates/removes containers, volumes, networks and test
# databases, so it must only ever talk to a local Docker daemon. It previously did not
# validate DOCKER_HOST or the active context (registered as R-G3.5-1): with a remote
# daemon configured, an innocuous-looking call would create and delete containers on a
# shared or production host.
# Keep this file ASCII-only: it carries no UTF-8 BOM, and Windows PowerShell 5.1
# decodes a BOM-less file as ANSI, which turns non-ASCII text into parse errors.
function Assert-LocalDockerDaemon {
    param([string[]]$AllowedContexts = @('default', 'desktop-linux', 'colima', 'desktop'))
    if (-not [string]::IsNullOrWhiteSpace($env:DOCKER_HOST)) {
        throw "DOCKER_HOST='$($env:DOCKER_HOST)' detected: refusing to create or remove containers on a remote Docker daemon. Use an explicit reviewed path for remote execution instead of relaxing this guard."
    }
    # Under script-level Stop, any stderr from the docker CLI is promoted to a terminating error,
    # so one harmless warning would make this guard report a false "cannot determine context".
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $ctxText = ((& docker context show 2>&1) | Out-String).Trim()
        $ctxExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($ctxExit -ne 0) {
        throw "Unable to determine the active Docker context (exit=$ctxExit): $ctxText"
    }
    if ($AllowedContexts -notcontains $ctxText) {
        throw "Active Docker context '$ctxText' is not in the allowed set ($($AllowedContexts -join ', ')): refusing to operate on containers."
    }
    Write-Output "Docker daemon guard: context=$ctxText"
}
Assert-LocalDockerDaemon

$ProgressPreference = 'SilentlyContinue'

$script:RootPassword = $null
$script:Checks = New-Object System.Collections.ArrayList
$script:LogLines = New-Object System.Collections.Generic.List[string]
$script:CleanupFailures = New-Object System.Collections.ArrayList

function Get-ScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    if ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
        return (Split-Path -Parent $MyInvocation.MyCommand.Path)
    }
    throw 'Unable to resolve script directory.'
}

# Single shared implementation of bounded native process execution (hard timeout plus
# process-tree kill). A plain `& docker ... 2>&1` call has no native timeout, so -ReadyTimeoutSeconds
# never reached the subprocess and one hung container call could block this gate forever
# (registered as R-G3.5-2). $ReadyTimeoutSeconds is reused as the per-call ceiling instead of
# inventing a new constant here.
$script:NativeTimeoutSeconds = if ($ReadyTimeoutSeconds -gt 0) { $ReadyTimeoutSeconds } else { 240 }
. (Join-Path (Get-ScriptDirectory) 'SandboxNative.ps1')

function Get-RepoRoot {
    if (-not [string]::IsNullOrWhiteSpace($RepoRoot)) {
        return [System.IO.Path]::GetFullPath($RepoRoot)
    }
    return (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
}

function New-Hex {
    param([int]$ByteCount = 4)
    $bytes = New-Object byte[] $ByteCount
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return (($bytes | ForEach-Object { $_.ToString('x2') }) -join '')
}

function New-SandboxSecret {
    return ('Zc' + (New-Hex -ByteCount 16))
}

function Write-RunLog {
    param([Parameter(Mandatory = $true)][string]$Message)
    $line = ('[{0}] {1}' -f ([DateTimeOffset]::UtcNow.ToString('o')), $Message)
    [void]$script:LogLines.Add($line)
    [Console]::Out.WriteLine($line)
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "File not found: $Path" }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Write-TextFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Text
    )
    [System.IO.File]::WriteAllText($Path, $Text, [System.Text.UTF8Encoding]::new($false))
}

function Write-JsonFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Payload
    )
    $json = $Payload | ConvertTo-Json -Depth 16
    Write-TextFile -Path $Path -Text ($json + [Environment]::NewLine)
}

function Invoke-Docker {
    param(
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure
    )
    # Output keeps stdout and stderr together on purpose: downstream assertions match error text that
    # mysql/docker write to stderr, so dropping it would silently turn those assertions into no-ops.
    $run = Invoke-BoundedNative -FilePath 'docker' -Arguments $Arguments -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'docker'
    $text = ($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim()
    if ($run.ExitCode -ne 0 -and -not $AllowFailure) {
        throw "docker command failed with exit code $($run.ExitCode). Output: $text"
    }
    return [pscustomobject]@{
        ExitCode = $run.ExitCode
        Output   = $text
    }
}

function Invoke-MySqlText {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database,
        [switch]$AllowFailure,
        [switch]$NoHeaders
    )
    # R-041: keep the password out of the docker process command line. It goes into this
    # process's environment only, and `docker exec -e MYSQL_PWD` (name without "=value") inherits it.
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $script:RootPassword, 'Process')
    $mysqlArgs = @(
        'exec',
        '-e', 'MYSQL_PWD',
        $Container,
        'mysql',
        '--protocol=TCP',
        '--host=127.0.0.1',
        '--port=3306',
        '-uroot',
        '--batch',
        '--raw',
        '--default-character-set=utf8mb4'
    )
    if (-not [string]::IsNullOrWhiteSpace($Database)) { $mysqlArgs += ('--database=' + $Database) }
    if ($NoHeaders) { $mysqlArgs += '--skip-column-names' }
    $mysqlArgs += @('-e', $Sql)
    return Invoke-Docker -Arguments $mysqlArgs -AllowFailure:$AllowFailure
}

function Invoke-MySqlScript {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database,
        [switch]$AllowFailure,
        [switch]$NoHeaders
    )
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $script:RootPassword, 'Process')
    $mysqlArgs = @(
        'exec',
        '-i',
        '-e', 'MYSQL_PWD',
        $Container,
        'mysql',
        '--protocol=TCP',
        '--host=127.0.0.1',
        '--port=3306',
        '-uroot',
        '--batch',
        '--raw',
        '--default-character-set=utf8mb4'
    )
    if (-not [string]::IsNullOrWhiteSpace($Database)) { $mysqlArgs += ('--database=' + $Database) }
    if ($NoHeaders) { $mysqlArgs += '--skip-column-names' }
    # SQL goes to the child's stdin and the call is bounded; previously `$Sql | & docker 2>&1`
    # blocked forever if a statement waited on a lock or the container stopped answering.
    $run = Invoke-BoundedNative -FilePath 'docker' -Arguments $mysqlArgs -StandardInput $Sql -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'mysql'
    $text = ($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim()
    if ($run.ExitCode -ne 0 -and -not $AllowFailure) {
        throw "mysql script failed with exit code $($run.ExitCode). Output: $text"
    }
    return [pscustomobject]@{
        ExitCode = $run.ExitCode
        Output   = $text
    }
}

function Invoke-MySqlUpdateAndGetRowCount {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Sql
    )
    $scriptSql = $Sql.TrimEnd() + " SELECT ROW_COUNT();"
    $result = Invoke-MySqlText -Container $Container -Database $Database -Sql $scriptSql -NoHeaders
    if ($result.ExitCode -ne 0) {
        throw "mysql update failed on $Container`: $($result.Output)"
    }
    $numbers = @($result.Output -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^\d+$' })
    if ($numbers.Count -eq 0) {
        throw "mysql update did not return ROW_COUNT on $Container`: $($result.Output)"
    }
    return [int]$numbers[-1]
}

function Get-MySqlScalar {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Sql,
        [string]$Database
    )
    $result = Invoke-MySqlText -Container $Container -Database $Database -Sql $Sql -NoHeaders
    if ($result.ExitCode -ne 0) {
        throw "mysql scalar query failed on $Container`: $($result.Output)"
    }
    return $result.Output.Trim()
}

function Wait-ContainerReady {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = ''
    while ((Get-Date) -lt $deadline) {
        $probe = Invoke-MySqlText -Container $Container -Sql 'SELECT 1;' -NoHeaders -AllowFailure
        if ($probe.ExitCode -eq 0 -and $probe.Output.Trim() -eq '1') {
            return $true
        }
        $lastError = $probe.Output
        Start-Sleep -Seconds 3
    }
    throw "Container $Container did not become ready within $TimeoutSeconds seconds. Last error: $lastError"
}

function Get-ReplicaStatus {
    param([Parameter(Mandatory = $true)][string]$Container)
    $result = Invoke-MySqlText -Container $Container -Sql 'SHOW REPLICA STATUS\G' -AllowFailure
    if ($result.ExitCode -ne 0) {
        throw "SHOW REPLICA STATUS failed on $Container`: $($result.Output)"
    }
    $status = [ordered]@{}
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line -match '^\s*([^:]+):\s*(.*)$') {
            $status[$Matches[1].Trim()] = $Matches[2].Trim()
        }
    }
    return $status
}

function Wait-ReplicationHealthy {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = ''
    while ((Get-Date) -lt $deadline) {
        try {
            $status = Get-ReplicaStatus -Container $Container
            $io = [string]$status['Replica_IO_Running']
            $sql = [string]$status['Replica_SQL_Running']
            $last = "Replica_IO_Running=$io; Replica_SQL_Running=$sql; Last_IO_Error=$([string]$status['Last_IO_Error']); Last_SQL_Error=$([string]$status['Last_SQL_Error'])"
            if ($io -eq 'Yes' -and $sql -eq 'Yes') {
                return $status
            }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Replication did not become healthy within $TimeoutSeconds seconds. Last status: $last"
}

function Wait-ReplicationCaughtUp {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = ''
    while ((Get-Date) -lt $deadline) {
        try {
            $status = Get-ReplicaStatus -Container $Container
            $io = [string]$status['Replica_IO_Running']
            $sql = [string]$status['Replica_SQL_Running']
            $behind = [string]$status['Seconds_Behind_Source']
            $last = "Replica_IO_Running=$io; Replica_SQL_Running=$sql; Seconds_Behind_Source=$behind"
            if ($io -eq 'Yes' -and $sql -eq 'Yes' -and ([string]::IsNullOrWhiteSpace($behind) -or $behind -eq '0')) {
                return $status
            }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Replication did not catch up within $TimeoutSeconds seconds. Last status: $last"
}

function Get-RowCount {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Table
    )
    return [int](Get-MySqlScalar -Container $Container -Sql "SELECT COUNT(*) FROM $Database.$Table;")
}

function Wait-RowCount {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Table,
        [Parameter(Mandatory = $true)][int]$Expected,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = $null
    while ((Get-Date) -lt $deadline) {
        try {
            $last = Get-RowCount -Container $Container -Database $Database -Table $Table
            if ($last -eq $Expected) { return $last }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Row count on $Container.$Database.$Table did not reach $Expected within $TimeoutSeconds seconds. Last value: $last"
}

function Get-Fingerprint {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Table
    )
    $sql = "SELECT CONCAT(COUNT(*), '|', COALESCE(SHA2(GROUP_CONCAT(CONCAT(id, ':', tenant_id, ':', payload) ORDER BY id SEPARATOR '~'), 256), 'empty')) FROM $Database.$Table;"
    return (Get-MySqlScalar -Container $Container -Sql $sql)
}

function Get-ApplicationFingerprint {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table
    )
    $columns = if ($Table -eq 'event_outbox') {
        "COALESCE(id,''), COALESCE(event_id,''), COALESCE(tenant_id,''), COALESCE(status,''), COALESCE(processing_owner,''), COALESCE(lease_until,''), COALESCE(retry_count,'')"
    }
    else {
        "COALESCE(id,''), COALESCE(consumer_name,''), COALESCE(event_id,''), COALESCE(tenant_id,''), COALESCE(status,''), COALESCE(processing_owner,''), COALESCE(lease_until,''), COALESCE(retry_count,'')"
    }
    $sql = "SET SESSION group_concat_max_len = 1048576; SELECT CONCAT(COUNT(*), '|', COALESCE(SHA2(GROUP_CONCAT(CONCAT_WS(':', $columns) ORDER BY id SEPARATOR '~'), 256), 'empty')) FROM ``$Table``;"
    return (Get-MySqlScalar -Container $Container -Database $Database -Sql $sql)
}

function Get-ApplicationState {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table,
        [Parameter(Mandatory = $true)][int]$Id
    )
    $sql = "SELECT CONCAT_WS('|', status, IFNULL(processing_owner, ''), IFNULL(lease_until, ''), retry_count) FROM ``$Table`` WHERE id = $Id AND tenant_id = 1;"
    return (Get-MySqlScalar -Container $Container -Database $Database -Sql $sql)
}

function Get-ApplicationOwner {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table,
        [Parameter(Mandatory = $true)][int]$Id
    )
    $sql = "SELECT IFNULL(processing_owner, '') FROM ``$Table`` WHERE id = $Id AND tenant_id = 1;"
    return (Get-MySqlScalar -Container $Container -Database $Database -Sql $sql)
}

function Add-Check {
    param(
        [Parameter(Mandatory = $true)][string]$Scenario,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Passed,
        [Parameter(Mandatory = $true)][string]$Expected,
        [Parameter(Mandatory = $true)][string]$Actual
    )
    $item = [ordered]@{
        scenario = $Scenario
        name     = $Name
        passed   = $Passed
        expected = $Expected
        actual   = $Actual
    }
    [void]$script:Checks.Add($item)
    if (-not $Passed) {
        throw "[$Scenario] $Name failed: expected '$Expected', actual '$Actual'"
    }
}

function Add-OwnerFencingResult {
    param(
        [Parameter(Mandatory = $true)]$Results,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][int]$Expected,
        [Parameter(Mandatory = $true)][int]$Actual
    )
    $passed = ($Actual -eq $Expected)
    $Results[$Name] = [ordered]@{
        expected = $Expected
        actual   = $Actual
        passed   = $passed
    }
    Add-Check -Scenario 'owner_fencing' -Name $Name -Passed $passed -Expected ([string]$Expected) -Actual ([string]$Actual)
}

function Invoke-OwnerFencingUpdate {
    param(
        [Parameter(Mandatory = $true)]$Results,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Sql,
        [Parameter(Mandatory = $true)][int]$Expected
    )
    $actual = Invoke-MySqlUpdateAndGetRowCount -Container $Container -Database $Database -Sql $Sql
    Add-OwnerFencingResult -Results $Results -Name $Name -Expected $Expected -Actual $actual
}

function Assert-OwnerFencingState {
    param(
        [Parameter(Mandatory = $true)]$Results,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table,
        [Parameter(Mandatory = $true)][int]$Id,
        [Parameter(Mandatory = $true)][string]$Expected
    )
    $actual = Get-ApplicationState -Container $Container -Database $Database -Table $Table -Id $Id
    $passed = ($actual -eq $Expected)
    $Results[$Name] = [ordered]@{
        expected = $Expected
        actual   = $actual
        passed   = $passed
    }
    Add-Check -Scenario 'owner_fencing' -Name $Name -Passed $passed -Expected $Expected -Actual $actual
}

function Assert-OwnerFencingOwner {
    param(
        [Parameter(Mandatory = $true)]$Results,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table,
        [Parameter(Mandatory = $true)][int]$Id,
        [Parameter(Mandatory = $true)][string]$Expected
    )
    $actual = Get-ApplicationOwner -Container $Container -Database $Database -Table $Table -Id $Id
    $passed = ($actual -eq $Expected)
    $Results[$Name] = [ordered]@{
        expected = $Expected
        actual   = $actual
        passed   = $passed
    }
    Add-Check -Scenario 'owner_fencing' -Name $Name -Passed $passed -Expected $Expected -Actual $actual
}

function Add-CleanupFailure {
    param([Parameter(Mandatory = $true)][string]$Message)
    [void]$script:CleanupFailures.Add($Message)
    Write-RunLog "CLEANUP: $Message"
}

function Remove-SandboxResources {
    param(
        [string]$SourceContainer,
        [string]$ReplicaContainer,
        [string]$SourceVolume,
        [string]$ReplicaVolume,
        [string]$Network
    )
    foreach ($container in @($ReplicaContainer, $SourceContainer)) {
        if ([string]::IsNullOrWhiteSpace($container)) { continue }
        $result = Invoke-Docker -Arguments @('rm', '--force', $container) -AllowFailure
        if ($result.ExitCode -ne 0 -and $result.Output -notmatch 'No such container') {
            Add-CleanupFailure "Unable to remove container $container`: $($result.Output)"
        }
    }
    foreach ($volume in @($ReplicaVolume, $SourceVolume)) {
        if ([string]::IsNullOrWhiteSpace($volume)) { continue }
        $result = Invoke-Docker -Arguments @('volume', 'rm', '--force', $volume) -AllowFailure
        if ($result.ExitCode -ne 0 -and $result.Output -notmatch 'No such volume') {
            Add-CleanupFailure "Unable to remove volume $volume`: $($result.Output)"
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($Network)) {
        $result = Invoke-Docker -Arguments @('network', 'rm', $Network) -AllowFailure
        if ($result.ExitCode -ne 0 -and $result.Output -notmatch 'No such network') {
            Add-CleanupFailure "Unable to remove network $Network`: $($result.Output)"
        }
    }
}

function Get-RepoFacts {
    $facts = [ordered]@{
        repo_root             = (Get-RepoRoot)
        repo_commit           = $null
        repo_branch           = $null
        repo_worktree_dirty   = $null
        repo_worktree_status_count = $null
        repo_worktree_status_sha256 = $null
    }
    $git = Get-Command git -ErrorAction SilentlyContinue
    if ($null -eq $git) { return [pscustomobject]$facts }
    $facts.repo_commit = (& $git.Source -C $facts.repo_root rev-parse HEAD 2>$null | Out-String).Trim()
    $facts.repo_branch = (& $git.Source -C $facts.repo_root branch --show-current 2>$null | Out-String).Trim()
    $statusLines = @(& $git.Source -C $facts.repo_root status --short 2>$null | ForEach-Object { [string]$_ })
    $facts.repo_worktree_status_count = $statusLines.Count
    $facts.repo_worktree_dirty = ($statusLines.Count -gt 0)
    if ($statusLines.Count -gt 0) {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes(($statusLines -join "`n"))
        $sha = [System.Security.Cryptography.SHA256]::Create()
        try { $facts.repo_worktree_status_sha256 = (($sha.ComputeHash($bytes) | ForEach-Object { $_.ToString('x2') }) -join '') }
        finally { $sha.Dispose() }
    }
    return [pscustomobject]$facts
}
function Wait-FingerprintEqual {
    param(
        [Parameter(Mandatory = $true)][string]$SourceContainer,
        [Parameter(Mandatory = $true)][string]$ReplicaContainer,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Table,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = ''
    while ((Get-Date) -lt $deadline) {
        try {
            $sourceFingerprint = Get-Fingerprint -Container $SourceContainer -Database $Database -Table $Table
            $replicaFingerprint = Get-Fingerprint -Container $ReplicaContainer -Database $Database -Table $Table
            $last = "source=$sourceFingerprint; replica=$replicaFingerprint"
            if ($sourceFingerprint -eq $replicaFingerprint) {
                return [pscustomobject]@{
                    Source  = $sourceFingerprint
                    Replica = $replicaFingerprint
                }
            }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Fingerprints did not converge within $TimeoutSeconds seconds. Last values: $last"
}

function Wait-ApplicationFingerprintEqual {
    param(
        [Parameter(Mandatory = $true)][string]$SourceContainer,
        [Parameter(Mandatory = $true)][string]$ReplicaContainer,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][ValidateSet('event_outbox', 'event_inbox')][string]$Table,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = ''
    while ((Get-Date) -lt $deadline) {
        try {
            $sourceFingerprint = Get-ApplicationFingerprint -Container $SourceContainer -Database $Database -Table $Table
            $replicaFingerprint = Get-ApplicationFingerprint -Container $ReplicaContainer -Database $Database -Table $Table
            $last = "source=$sourceFingerprint; replica=$replicaFingerprint"
            if ($sourceFingerprint -eq $replicaFingerprint) {
                return [pscustomobject]@{
                    Source  = $sourceFingerprint
                    Replica = $replicaFingerprint
                }
            }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Application fingerprints did not converge within $TimeoutSeconds seconds. Last values: $last"
}

function Wait-ApplicationSchemaReady {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][int]$TimeoutSeconds
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $last = ''
    while ((Get-Date) -lt $deadline) {
        try {
            $tables = [int](Get-MySqlScalar -Container $Container -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$Database' AND table_name IN ('event_outbox', 'event_inbox');")
            $columns = [int](Get-MySqlScalar -Container $Container -Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = '$Database' AND ((table_name = 'event_outbox' AND column_name IN ('processing_owner', 'lease_until')) OR (table_name = 'event_inbox' AND column_name IN ('processing_owner', 'lease_until', 'claimed_at')));")
            $last = "tables=$tables; owner_lease_columns=$columns"
            if ($tables -eq 2 -and $columns -eq 5) {
                return [pscustomobject]@{
                    Tables  = $tables
                    Columns = $columns
                }
            }
        }
        catch {
            $last = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Application schema did not converge within $TimeoutSeconds seconds. Last values: $last"
}

function Get-VerificationRows {
    param(
        [Parameter(Mandatory = $true)][string]$Container,
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$Sql
    )
    $result = Invoke-MySqlText -Container $Container -Database $Database -Sql $Sql -NoHeaders
    $rows = [ordered]@{}
    foreach ($line in ($result.Output -split "`r?`n")) {
        if ($line -match '^([A-Z0-9_]+)\s+([A-Z]+)$') {
            $rows[$Matches[1]] = $Matches[2]
        }
    }
    return [pscustomobject]@{
        ExitCode = $result.ExitCode
        Rows     = $rows
        Output   = $result.Output
    }
}

function Assert-DigestPinnedImage {
    if ([string]::IsNullOrWhiteSpace($Image)) { throw 'Image is required.' }
    if ($Image -match 'latest') { throw "Refusing mutable image tag 'latest': pin an exact digest." }
    if ($Image -notmatch '^[^@]+@sha256:[0-9a-f]{64}$') {
        throw "Refusing image '$Image': expected image@sha256:<64 lowercase hex>."
    }
}

function New-SandboxContainerName {
    for ($attempt = 0; $attempt -lt 20; $attempt++) {
        $candidate = 'zhicloud-mysql-sandbox-' + (New-Hex -ByteCount 4)
        $inspect = Invoke-Docker -Arguments @('inspect', $candidate) -AllowFailure
        if ($inspect.ExitCode -ne 0 -and $inspect.Output -match 'No such (container|object)') {
            return $candidate
        }
        if ($inspect.ExitCode -eq 0) { continue }
        throw "Unable to check Docker name '$candidate': $($inspect.Output)"
    }
    throw 'Unable to allocate a unique sandbox container name.'
}

function Assert-SandboxContainerName {
    param([Parameter(Mandatory = $true)][string]$Name)
    if ($Name -notmatch '^zhicloud-mysql-sandbox-[0-9a-f]{8}$') {
        throw "Refusing container name '$Name': expected zhicloud-mysql-sandbox-<8 hex>."
    }
}

$startedAt = Get-Date
$runId = New-Hex -ByteCount 4
$resolvedRepoRoot = Get-RepoRoot
if (-not (Test-Path -LiteralPath $resolvedRepoRoot -PathType Container)) {
    throw "Repository root does not exist: $resolvedRepoRoot"
}
Assert-DigestPinnedImage
if ($ReadyTimeoutSeconds -le 0) { throw 'ReadyTimeoutSeconds must be greater than zero.' }
if ($ReplicationTimeoutSeconds -le 0) { throw 'ReplicationTimeoutSeconds must be greater than zero.' }
$dockerCommand = Get-Command docker -ErrorAction Stop

if ($DryRun) {
    [Console]::Out.WriteLine('DRY RUN: no containers, networks, volumes, or evidence files will be created.')
    [Console]::Out.WriteLine("image=$Image")
    [Console]::Out.WriteLine("repo_root=$resolvedRepoRoot")
    [Console]::Out.WriteLine('plan=create isolated network + source/replica volumes + two digest-pinned MySQL containers; configure GTID binlog replication; verify initial/update/restart catch-up and replica read-only; apply V100->V104->V107->V120 to a dedicated application database; replicate Outbox/Inbox fixtures; perform one-way source stop/replica promotion and owner-fencing SQL assertions; remove temporary resources unless -KeepSandbox')
    exit 0
}

if ([string]::IsNullOrWhiteSpace($EvidenceDir)) {
    $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
    $EvidenceDir = Join-Path $resolvedRepoRoot "docs\enterprise-platform\evidence\production\mysql-physical-replication-sandbox-$stamp-$runId"
}
$EvidenceDir = [System.IO.Path]::GetFullPath($EvidenceDir)
[void](New-Item -ItemType Directory -Path $EvidenceDir -Force)
$runLogPath = Join-Path $EvidenceDir 'run.log'
$evidenceJsonPath = Join-Path $EvidenceDir 'evidence.json'
$summaryPath = Join-Path $EvidenceDir 'SUMMARY.md'
$exitPath = Join-Path $EvidenceDir 'exit.txt'

$sourceContainer = $null
$replicaContainer = $null
$sourceVolume = $null
$replicaVolume = $null
$network = $null
$failure = $null
$status = 'failed'
$cleanupAttempted = $false
$resourcesRetained = [bool]$KeepSandbox
$replicaStatusAfterStart = $null
$replicaStatusAfterRestart = $null
$fingerprintAfterStart = $null
$fingerprintAfterRestart = $null
$sourceRowCountAfterStart = $null
$replicaRowCountAfterStart = $null
$sourceRowCountAfterRestart = $null
$replicaRowCountAfterRestart = $null
$readOnlyWriteExitCode = $null
$readOnlyWriteOutput = $null
$readOnlyEnforced = $false
$restartExitCode = $null
$applicationDatabase = 'zhicloud_event_repl_sandbox'
$applicationMigrationFiles = [ordered]@{
    V100 = (Join-Path $resolvedRepoRoot 'zhicloud-server\src\main\resources\db\migration\V100__event_outbox_inbox.sql')
    V104 = (Join-Path $resolvedRepoRoot 'zhicloud-server\src\main\resources\db\migration\V104__integration_monitoring_indexes.sql')
    V107 = (Join-Path $resolvedRepoRoot 'zhicloud-server\src\main\resources\db\migration\V107__event_tenant_unique_keys.sql')
    V120 = (Join-Path $resolvedRepoRoot 'zhicloud-server\src\main\resources\db\migration\V120__event_outbox_inbox_owner_lease.sql')
}
$applicationMigrationHashes = [ordered]@{}
$applicationSchemaReady = $false
$applicationV120Verification = $null
$applicationRowCountsBeforePromotion = $null
$applicationFingerprintsBeforePromotion = $null
$promotionSourceGtidBefore = $null
$promotionReplicaStatusBefore = $null
$promotionSourceStopExitCode = $null
$promotionStopReplicaExitCode = $null
$promotionResetReplicaExitCode = $null
$promotionReadOnlyOffExitCode = $null
$promotionReadOnly = $null
$promotionSuperReadOnly = $null
$promotionWriteProbeAffectedRows = $null
$ownerFencingResults = [ordered]@{}

$rootPassword = New-SandboxSecret
$replicationPassword = 'R' + (New-Hex -ByteCount 12)
$script:RootPassword = $rootPassword
$sourceContainer = New-SandboxContainerName
$replicaContainer = New-SandboxContainerName
Assert-SandboxContainerName -Name $sourceContainer
Assert-SandboxContainerName -Name $replicaContainer
$sourceVolume = 'zhicloud-mysql-repl-source-' + $runId
$replicaVolume = 'zhicloud-mysql-repl-replica-' + $runId
$network = 'zhicloud-mysql-repl-net-' + $runId
try {
    Write-RunLog "Starting isolated MySQL physical replication approximation run_id=$runId"
    Write-RunLog "Creating network=$network source_volume=$sourceVolume replica_volume=$replicaVolume"
    [void](Invoke-Docker -Arguments @('network', 'create', $network))
    [void](Invoke-Docker -Arguments @('volume', 'create', $sourceVolume))
    [void](Invoke-Docker -Arguments @('volume', 'create', $replicaVolume))

    $sourceRun = Invoke-Docker -Arguments @(
        'run', '-d', '--name', $sourceContainer, '--network', $network, '--network-alias', 'source',
        '-e', ('MYSQL_ROOT_PASSWORD=' + $rootPassword),
        '-v', ($sourceVolume + ':/var/lib/mysql'),
        $Image,
        '--server-id=1',
        '--log-bin=mysql-bin',
        '--binlog-format=ROW',
        '--binlog-row-image=FULL',
        '--gtid-mode=ON',
        '--enforce-gtid-consistency=ON',
        '--binlog-expire-logs-seconds=86400'
    )
    Write-RunLog "Source container started: $sourceContainer"

    $replicaRun = Invoke-Docker -Arguments @(
        'run', '-d', '--name', $replicaContainer, '--network', $network, '--network-alias', 'replica',
        '-e', ('MYSQL_ROOT_PASSWORD=' + $rootPassword),
        '-v', ($replicaVolume + ':/var/lib/mysql'),
        $Image,
        '--server-id=2',
        '--relay-log=replica-relay',
        '--gtid-mode=ON',
        '--enforce-gtid-consistency=ON'
    )
    Write-RunLog "Replica container started: $replicaContainer"

    $imageInspect = Invoke-Docker -Arguments @('image', 'inspect', '--format', '{{index .RepoDigests 0}}', $Image) -AllowFailure
    $imageRepoDigest = if ($imageInspect.ExitCode -eq 0) { $imageInspect.Output.Trim() } else { $null }

    Write-RunLog 'Waiting for source readiness'
    [void](Wait-ContainerReady -Container $sourceContainer -TimeoutSeconds $ReadyTimeoutSeconds)
    Write-RunLog 'Waiting for replica readiness'
    [void](Wait-ContainerReady -Container $replicaContainer -TimeoutSeconds $ReadyTimeoutSeconds)

    $sourceServerId = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.server_id;'
    $sourceLogBin = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.log_bin;'
    $sourceBinlogFormat = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.binlog_format;'
    $sourceGtidMode = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.gtid_mode;'
    $sourceGtidConsistency = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.enforce_gtid_consistency;'
    Add-Check -Scenario 'source_configuration' -Name 'server_id' -Passed ($sourceServerId -eq '1') -Expected '1' -Actual $sourceServerId
    Add-Check -Scenario 'source_configuration' -Name 'log_bin' -Passed ($sourceLogBin -eq '1') -Expected '1' -Actual $sourceLogBin
    Add-Check -Scenario 'source_configuration' -Name 'binlog_format' -Passed ($sourceBinlogFormat -eq 'ROW') -Expected 'ROW' -Actual $sourceBinlogFormat
    Add-Check -Scenario 'source_configuration' -Name 'gtid_mode' -Passed ($sourceGtidMode -eq 'ON') -Expected 'ON' -Actual $sourceGtidMode
    Add-Check -Scenario 'source_configuration' -Name 'enforce_gtid_consistency' -Passed ($sourceGtidConsistency -eq 'ON') -Expected 'ON' -Actual $sourceGtidConsistency

    [void](Invoke-MySqlText -Container $replicaContainer -Sql 'SET GLOBAL super_read_only=ON; SET GLOBAL read_only=ON;')
    $replicaServerId = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.server_id;'
    $replicaReadOnly = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.read_only;'
    $replicaSuperReadOnly = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.super_read_only;'
    $replicaGtidMode = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.gtid_mode;'
    $replicaGtidConsistency = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.enforce_gtid_consistency;'
    Add-Check -Scenario 'replica_configuration' -Name 'server_id' -Passed ($replicaServerId -eq '2') -Expected '2' -Actual $replicaServerId
    Add-Check -Scenario 'replica_configuration' -Name 'read_only' -Passed ($replicaReadOnly -eq '1') -Expected '1' -Actual $replicaReadOnly
    Add-Check -Scenario 'replica_configuration' -Name 'super_read_only' -Passed ($replicaSuperReadOnly -eq '1') -Expected '1' -Actual $replicaSuperReadOnly
    Add-Check -Scenario 'replica_configuration' -Name 'gtid_mode' -Passed ($replicaGtidMode -eq 'ON') -Expected 'ON' -Actual $replicaGtidMode
    Add-Check -Scenario 'replica_configuration' -Name 'enforce_gtid_consistency' -Passed ($replicaGtidConsistency -eq 'ON') -Expected 'ON' -Actual $replicaGtidConsistency

    $bootstrapSql = @"
CREATE USER 'repl'@'%' IDENTIFIED BY '$replicationPassword';
ALTER USER 'repl'@'%' IDENTIFIED BY '$replicationPassword';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
"@
    [void](Invoke-MySqlText -Container $sourceContainer -Sql $bootstrapSql)
    Write-RunLog 'Replication user created on source'

    $schemaSql = @"
CREATE DATABASE IF NOT EXISTS zhicloud_replication_probe CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS zhicloud_replication_probe.replication_probe (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    payload VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;
"@
    [void](Invoke-MySqlText -Container $sourceContainer -Sql $schemaSql)

    $stopReplica = Invoke-MySqlText -Container $replicaContainer -Sql 'STOP REPLICA;' -AllowFailure
    $resetReplica = Invoke-MySqlText -Container $replicaContainer -Sql 'RESET REPLICA ALL;'
    if ($resetReplica.ExitCode -ne 0) { throw "RESET REPLICA ALL failed: $($resetReplica.Output)" }
    $changeSql = @"
CHANGE REPLICATION SOURCE TO
    SOURCE_HOST='source',
    SOURCE_PORT=3306,
    SOURCE_USER='repl',
    SOURCE_PASSWORD='$replicationPassword',
    SOURCE_AUTO_POSITION=1,
    SOURCE_SSL=1,
    GET_SOURCE_PUBLIC_KEY=1;
"@
    [void](Invoke-MySqlText -Container $replicaContainer -Sql $changeSql)
    [void](Invoke-MySqlText -Container $replicaContainer -Sql 'START REPLICA;')
    Write-RunLog 'Replication channel configured and started'

    $replicaStatusAfterStart = Wait-ReplicationHealthy -Container $replicaContainer -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'replication_start' -Name 'Replica_IO_Running' -Passed ([string]$replicaStatusAfterStart['Replica_IO_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$replicaStatusAfterStart['Replica_IO_Running'])
    Add-Check -Scenario 'replication_start' -Name 'Replica_SQL_Running' -Passed ([string]$replicaStatusAfterStart['Replica_SQL_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$replicaStatusAfterStart['Replica_SQL_Running'])

    $insertSql = @"
INSERT INTO zhicloud_replication_probe.replication_probe (id, tenant_id, payload) VALUES
    (1, 101, 'alpha'),
    (2, 202, 'beta');
"@
    [void](Invoke-MySqlText -Container $sourceContainer -Sql $insertSql)
    $sourceRowCountAfterStart = Get-RowCount -Container $sourceContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe'
    $replicaRowCountAfterStart = Wait-RowCount -Container $replicaContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe' -Expected 2 -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'initial_data_replication' -Name 'replica_row_count' -Passed ($replicaRowCountAfterStart -eq 2) -Expected '2' -Actual ([string]$replicaRowCountAfterStart)
    Add-Check -Scenario 'initial_data_replication' -Name 'source_row_count' -Passed ($sourceRowCountAfterStart -eq 2) -Expected '2' -Actual ([string]$sourceRowCountAfterStart)
    $fingerprintAfterStart = Wait-FingerprintEqual -SourceContainer $sourceContainer -ReplicaContainer $replicaContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe' -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'initial_data_replication' -Name 'content_fingerprint' -Passed ($fingerprintAfterStart.Source -eq $fingerprintAfterStart.Replica) -Expected $fingerprintAfterStart.Source -Actual $fingerprintAfterStart.Replica
    $readOnlySql = "INSERT INTO zhicloud_replication_probe.replication_probe (id, tenant_id, payload) VALUES (999, 999, 'must-not-write');"
    $readOnlyWrite = Invoke-MySqlText -Container $replicaContainer -Sql $readOnlySql -AllowFailure
    $readOnlyWriteExitCode = $readOnlyWrite.ExitCode
    $readOnlyWriteOutput = $readOnlyWrite.Output
    $readOnlyEnforced = ($readOnlyWrite.ExitCode -ne 0 -and $readOnlyWriteOutput -match '(?i)read[- ]only|super-read-only|read only')
    Add-Check -Scenario 'replica_read_only' -Name 'write_rejected' -Passed $readOnlyEnforced -Expected 'nonzero exit and read-only error' -Actual ("exit=$readOnlyWriteExitCode; output=$readOnlyWriteOutput")

    $restart = Invoke-Docker -Arguments @('restart', $replicaContainer)
    $restartExitCode = $restart.ExitCode
    Add-Check -Scenario 'replica_restart_recovery' -Name 'docker_restart_exit_code' -Passed ($restartExitCode -eq 0) -Expected '0' -Actual ([string]$restartExitCode)
    [void](Wait-ContainerReady -Container $replicaContainer -TimeoutSeconds $ReadyTimeoutSeconds)
    [void](Invoke-MySqlText -Container $replicaContainer -Sql 'START REPLICA;' -AllowFailure)
    [void](Invoke-MySqlText -Container $replicaContainer -Sql 'SET GLOBAL super_read_only=ON; SET GLOBAL read_only=ON;')
    $replicaStatusAfterRestart = Wait-ReplicationHealthy -Container $replicaContainer -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'replica_restart_recovery' -Name 'Replica_IO_Running_after_restart' -Passed ([string]$replicaStatusAfterRestart['Replica_IO_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$replicaStatusAfterRestart['Replica_IO_Running'])
    Add-Check -Scenario 'replica_restart_recovery' -Name 'Replica_SQL_Running_after_restart' -Passed ([string]$replicaStatusAfterRestart['Replica_SQL_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$replicaStatusAfterRestart['Replica_SQL_Running'])
    $replicaSuperReadOnlyAfterRestart = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.super_read_only;'
    Add-Check -Scenario 'replica_restart_recovery' -Name 'super_read_only_after_restart' -Passed ($replicaSuperReadOnlyAfterRestart -eq '1') -Expected '1' -Actual $replicaSuperReadOnlyAfterRestart

    $insertAfterRestartSql = "INSERT INTO zhicloud_replication_probe.replication_probe (id, tenant_id, payload) VALUES (3, 303, 'gamma');"
    [void](Invoke-MySqlText -Container $sourceContainer -Sql $insertAfterRestartSql)
    $sourceRowCountAfterRestart = Get-RowCount -Container $sourceContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe'
    $replicaRowCountAfterRestart = Wait-RowCount -Container $replicaContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe' -Expected 3 -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'replica_restart_recovery' -Name 'source_row_count_after_restart' -Passed ($sourceRowCountAfterRestart -eq 3) -Expected '3' -Actual ([string]$sourceRowCountAfterRestart)
    Add-Check -Scenario 'replica_restart_recovery' -Name 'replica_row_count_after_restart' -Passed ($replicaRowCountAfterRestart -eq 3) -Expected '3' -Actual ([string]$replicaRowCountAfterRestart)
    $fingerprintAfterRestart = Wait-FingerprintEqual -SourceContainer $sourceContainer -ReplicaContainer $replicaContainer -Database 'zhicloud_replication_probe' -Table 'replication_probe' -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'replica_restart_recovery' -Name 'content_fingerprint_after_restart' -Passed ($fingerprintAfterRestart.Source -eq $fingerprintAfterRestart.Replica) -Expected $fingerprintAfterRestart.Source -Actual $fingerprintAfterRestart.Replica

    Write-RunLog 'Starting application migration chain V100->V104->V107->V120'
    foreach ($migrationKey in $applicationMigrationFiles.Keys) {
        $applicationMigrationHashes[$migrationKey] = Get-Sha256 -Path $applicationMigrationFiles[$migrationKey]
    }

    $createApplicationDatabase = Invoke-MySqlText -Container $sourceContainer -Sql "CREATE DATABASE IF NOT EXISTS $applicationDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Add-Check -Scenario 'application_migration' -Name 'database_create_exit_code' -Passed ($createApplicationDatabase.ExitCode -eq 0) -Expected '0' -Actual ([string]$createApplicationDatabase.ExitCode)

    foreach ($migrationKey in $applicationMigrationFiles.Keys) {
        $migrationSql = Get-Content -LiteralPath $applicationMigrationFiles[$migrationKey] -Raw -Encoding UTF8
        $migrationResult = Invoke-MySqlScript -Container $sourceContainer -Database $applicationDatabase -Sql $migrationSql
        Add-Check -Scenario 'application_migration' -Name ("${migrationKey}_exit_code") -Passed ($migrationResult.ExitCode -eq 0) -Expected '0' -Actual ([string]$migrationResult.ExitCode)
    }

    $v120VerifyPath = Join-Path $resolvedRepoRoot 'zhicloud-server\src\main\resources\db\verification\V120_verify.sql'
    $applicationV120Verification = Get-VerificationRows -Container $sourceContainer -Database $applicationDatabase -Sql (Get-Content -LiteralPath $v120VerifyPath -Raw -Encoding UTF8)
    Add-Check -Scenario 'application_migration' -Name 'v120_verify_exit_code' -Passed ($applicationV120Verification.ExitCode -eq 0) -Expected '0' -Actual ([string]$applicationV120Verification.ExitCode)
    $expectedV120Checks = @('V120_EVENT_INBOX_OWNER_COLUMNS', 'V120_EVENT_OUTBOX_OWNER_COLUMNS', 'V120_EVENT_INBOX_LEASE_INDEX', 'V120_EVENT_OUTBOX_LEASE_INDEX')
    foreach ($v120CheckName in $expectedV120Checks) {
        $v120CheckActual = [string]$applicationV120Verification.Rows[$v120CheckName]
        if ([string]::IsNullOrWhiteSpace($v120CheckActual)) { $v120CheckActual = 'MISSING' }
        Add-Check -Scenario 'application_migration' -Name $v120CheckName -Passed ($v120CheckActual -eq 'PASS') -Expected 'PASS' -Actual $v120CheckActual
    }
    Add-Check -Scenario 'application_migration' -Name 'v120_verify_check_count' -Passed ($applicationV120Verification.Rows.Count -eq 4) -Expected '4' -Actual ([string]$applicationV120Verification.Rows.Count)

    $applicationSchema = Wait-ApplicationSchemaReady -Container $replicaContainer -Database $applicationDatabase -TimeoutSeconds $ReplicationTimeoutSeconds
    $applicationSchemaReady = ($applicationSchema.Tables -eq 2 -and $applicationSchema.Columns -eq 5)
    Add-Check -Scenario 'application_schema_replication' -Name 'replica_application_schema_ready' -Passed $applicationSchemaReady -Expected 'tables=2; owner_lease_columns=5' -Actual ("tables=$($applicationSchema.Tables); owner_lease_columns=$($applicationSchema.Columns)")

    $outboxFixtureSql = @'
INSERT INTO `event_outbox` (
    id, event_id, tenant_id, aggregate_type, aggregate_id, event_type,
    payload, status, retry_count, next_retry_at, published_at,
    processing_owner, lease_until, last_error
) VALUES
    (1001, 'sandbox-outbox-1001', 1, 'sandbox', 'agg-1001', 'sandbox.outbox.v1', '{"id":1001}', 'NEW', 0, NULL, NULL, NULL, NULL, NULL),
    (1002, 'sandbox-outbox-1002', 1, 'sandbox', 'agg-1002', 'sandbox.outbox.v1', '{"id":1002}', 'PUBLISHING', 0, NULL, NULL, 'source-active-owner', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), NULL),
    (1003, 'sandbox-outbox-1003', 1, 'sandbox', 'agg-1003', 'sandbox.outbox.v1', '{"id":1003}', 'PUBLISHING', 0, NULL, NULL, 'source-expired-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), NULL),
    (1004, 'sandbox-outbox-1004', 1, 'sandbox', 'agg-1004', 'sandbox.outbox.v1', '{"id":1004}', 'PUBLISHING', 0, NULL, NULL, 'source-expired-retry-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), NULL),
    (1005, 'sandbox-outbox-1005', 1, 'sandbox', 'agg-1005', 'sandbox.outbox.v1', '{"id":1005}', 'PUBLISHING', 0, NULL, NULL, 'source-expired-dead-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), NULL),
    (1006, 'sandbox-outbox-1006', 1, 'sandbox', 'agg-1006', 'sandbox.outbox.v1', '{"id":1006}', 'PUBLISHED', 0, NULL, UTC_TIMESTAMP(), NULL, NULL, NULL);
'@

    $inboxFixtureSql = @'
INSERT INTO `event_inbox` (
    id, tenant_id, consumer_name, event_id, event_type, status, retry_count,
    received_at, processed_at, error, processing_owner, lease_until, claimed_at
) VALUES
    (2001, 1, 'sandbox-consumer', 'sandbox-inbox-2001', 'sandbox.inbox.v1', 'RECEIVED', 0, UTC_TIMESTAMP(), NULL, NULL, NULL, NULL, NULL),
    (2002, 1, 'sandbox-consumer', 'sandbox-inbox-2002', 'sandbox.inbox.v1', 'FAILED', 1, UTC_TIMESTAMP(), NULL, 'source-failed', NULL, NULL, NULL),
    (2003, 1, 'sandbox-consumer', 'sandbox-inbox-2003', 'sandbox.inbox.v1', 'PROCESSING', 0, UTC_TIMESTAMP(), NULL, NULL, 'source-inbox-active-owner', DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), UTC_TIMESTAMP()),
    (2004, 1, 'sandbox-consumer', 'sandbox-inbox-2004', 'sandbox.inbox.v1', 'PROCESSING', 0, UTC_TIMESTAMP(), NULL, NULL, 'source-inbox-expired-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 MINUTE)),
    (2005, 1, 'sandbox-consumer', 'sandbox-inbox-2005', 'sandbox.inbox.v1', 'PROCESSING', 0, UTC_TIMESTAMP(), NULL, NULL, 'source-inbox-failed-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 MINUTE)),
    (2006, 1, 'sandbox-consumer', 'sandbox-inbox-2006', 'sandbox.inbox.v1', 'PROCESSING', 0, UTC_TIMESTAMP(), NULL, NULL, 'source-inbox-dead-owner', DATE_SUB(UTC_TIMESTAMP(), INTERVAL 10 MINUTE), DATE_SUB(UTC_TIMESTAMP(), INTERVAL 20 MINUTE)),
    (2007, 1, 'sandbox-consumer', 'sandbox-inbox-2007', 'sandbox.inbox.v1', 'DEAD', 3, UTC_TIMESTAMP(), NULL, 'source-dead', NULL, NULL, NULL);
'@

    $outboxFixtureResult = Invoke-MySqlText -Container $sourceContainer -Database $applicationDatabase -Sql $outboxFixtureSql
    Add-Check -Scenario 'application_fixture' -Name 'outbox_insert_exit_code' -Passed ($outboxFixtureResult.ExitCode -eq 0) -Expected '0' -Actual ([string]$outboxFixtureResult.ExitCode)
    $inboxFixtureResult = Invoke-MySqlText -Container $sourceContainer -Database $applicationDatabase -Sql $inboxFixtureSql
    Add-Check -Scenario 'application_fixture' -Name 'inbox_insert_exit_code' -Passed ($inboxFixtureResult.ExitCode -eq 0) -Expected '0' -Actual ([string]$inboxFixtureResult.ExitCode)

    $sourceOutboxRowCount = Get-RowCount -Container $sourceContainer -Database $applicationDatabase -Table 'event_outbox'
    $replicaOutboxRowCount = Wait-RowCount -Container $replicaContainer -Database $applicationDatabase -Table 'event_outbox' -Expected 6 -TimeoutSeconds $ReplicationTimeoutSeconds
    $sourceInboxRowCount = Get-RowCount -Container $sourceContainer -Database $applicationDatabase -Table 'event_inbox'
    $replicaInboxRowCount = Wait-RowCount -Container $replicaContainer -Database $applicationDatabase -Table 'event_inbox' -Expected 7 -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'application_fixture' -Name 'source_event_outbox_row_count' -Passed ($sourceOutboxRowCount -eq 6) -Expected '6' -Actual ([string]$sourceOutboxRowCount)
    Add-Check -Scenario 'application_fixture' -Name 'replica_event_outbox_row_count' -Passed ($replicaOutboxRowCount -eq 6) -Expected '6' -Actual ([string]$replicaOutboxRowCount)
    Add-Check -Scenario 'application_fixture' -Name 'source_event_inbox_row_count' -Passed ($sourceInboxRowCount -eq 7) -Expected '7' -Actual ([string]$sourceInboxRowCount)
    Add-Check -Scenario 'application_fixture' -Name 'replica_event_inbox_row_count' -Passed ($replicaInboxRowCount -eq 7) -Expected '7' -Actual ([string]$replicaInboxRowCount)

    $outboxFingerprint = Wait-ApplicationFingerprintEqual -SourceContainer $sourceContainer -ReplicaContainer $replicaContainer -Database $applicationDatabase -Table 'event_outbox' -TimeoutSeconds $ReplicationTimeoutSeconds
    $inboxFingerprint = Wait-ApplicationFingerprintEqual -SourceContainer $sourceContainer -ReplicaContainer $replicaContainer -Database $applicationDatabase -Table 'event_inbox' -TimeoutSeconds $ReplicationTimeoutSeconds
    Add-Check -Scenario 'application_fixture' -Name 'event_outbox_fingerprint_equal' -Passed ($outboxFingerprint.Source -eq $outboxFingerprint.Replica) -Expected $outboxFingerprint.Source -Actual $outboxFingerprint.Replica
    Add-Check -Scenario 'application_fixture' -Name 'event_inbox_fingerprint_equal' -Passed ($inboxFingerprint.Source -eq $inboxFingerprint.Replica) -Expected $inboxFingerprint.Source -Actual $inboxFingerprint.Replica

    $applicationRowCountsBeforePromotion = [ordered]@{
        source_event_outbox = $sourceOutboxRowCount
        replica_event_outbox = $replicaOutboxRowCount
        source_event_inbox  = $sourceInboxRowCount
        replica_event_inbox = $replicaInboxRowCount
    }
    $applicationFingerprintsBeforePromotion = [ordered]@{
        source_event_outbox = $outboxFingerprint.Source
        replica_event_outbox = $outboxFingerprint.Replica
        source_event_inbox  = $inboxFingerprint.Source
        replica_event_inbox = $inboxFingerprint.Replica
    }

    $replicaStatusCaughtUp = Wait-ReplicationCaughtUp -Container $replicaContainer -TimeoutSeconds $ReplicationTimeoutSeconds
    $promotionSourceGtidBefore = Get-MySqlScalar -Container $sourceContainer -Sql 'SELECT @@global.gtid_executed;'
    $promotionReplicaStatusBefore = [ordered]@{
        Replica_IO_Running    = [string]$replicaStatusCaughtUp['Replica_IO_Running']
        Replica_SQL_Running   = [string]$replicaStatusCaughtUp['Replica_SQL_Running']
        Seconds_Behind_Source = [string]$replicaStatusCaughtUp['Seconds_Behind_Source']
        Retrieved_Gtid_Set    = [string]$replicaStatusCaughtUp['Retrieved_Gtid_Set']
        Executed_Gtid_Set     = [string]$replicaStatusCaughtUp['Executed_Gtid_Set']
    }
    Add-Check -Scenario 'promotion_precheck' -Name 'Replica_IO_Running' -Passed ([string]$promotionReplicaStatusBefore['Replica_IO_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$promotionReplicaStatusBefore['Replica_IO_Running'])
    Add-Check -Scenario 'promotion_precheck' -Name 'Replica_SQL_Running' -Passed ([string]$promotionReplicaStatusBefore['Replica_SQL_Running'] -eq 'Yes') -Expected 'Yes' -Actual ([string]$promotionReplicaStatusBefore['Replica_SQL_Running'])
    Add-Check -Scenario 'promotion_precheck' -Name 'Seconds_Behind_Source' -Passed ([string]::IsNullOrWhiteSpace([string]$promotionReplicaStatusBefore['Seconds_Behind_Source']) -or [string]$promotionReplicaStatusBefore['Seconds_Behind_Source'] -eq '0') -Expected '0 or empty' -Actual ([string]$promotionReplicaStatusBefore['Seconds_Behind_Source'])
    Add-Check -Scenario 'promotion_precheck' -Name 'source_gtid_executed_nonempty' -Passed (-not [string]::IsNullOrWhiteSpace($promotionSourceGtidBefore)) -Expected 'non-empty' -Actual $promotionSourceGtidBefore
    Add-Check -Scenario 'promotion_precheck' -Name 'replica_executed_gtid_nonempty' -Passed (-not [string]::IsNullOrWhiteSpace([string]$promotionReplicaStatusBefore['Executed_Gtid_Set'])) -Expected 'non-empty' -Actual ([string]$promotionReplicaStatusBefore['Executed_Gtid_Set'])

    Write-RunLog 'Stopping source container for one-way promotion exercise'
    $sourceStop = Invoke-Docker -Arguments @('stop', $sourceContainer) -AllowFailure
    $promotionSourceStopExitCode = $sourceStop.ExitCode
    Add-Check -Scenario 'promotion' -Name 'source_stop_exit_code' -Passed ($promotionSourceStopExitCode -eq 0) -Expected '0' -Actual ([string]$promotionSourceStopExitCode)

    $sourceStateAfterStop = Invoke-Docker -Arguments @('inspect', '--format', '{{.State.Running}}', $sourceContainer) -AllowFailure
    $sourceRunningAfterStop = ($sourceStateAfterStop.ExitCode -eq 0 -and $sourceStateAfterStop.Output.Trim() -eq 'false')
    Add-Check -Scenario 'promotion' -Name 'source_container_stopped' -Passed $sourceRunningAfterStop -Expected 'false' -Actual ("exit=$($sourceStateAfterStop.ExitCode); running=$($sourceStateAfterStop.Output)")

    $stopReplica = Invoke-MySqlText -Container $replicaContainer -Sql 'STOP REPLICA;' -AllowFailure
    $promotionStopReplicaExitCode = $stopReplica.ExitCode
    Add-Check -Scenario 'promotion' -Name 'replica_stop_exit_code' -Passed ($promotionStopReplicaExitCode -eq 0) -Expected '0' -Actual ([string]$promotionStopReplicaExitCode)

    $resetReplica = Invoke-MySqlText -Container $replicaContainer -Sql 'RESET REPLICA ALL;' -AllowFailure
    $promotionResetReplicaExitCode = $resetReplica.ExitCode
    Add-Check -Scenario 'promotion' -Name 'replica_reset_exit_code' -Passed ($promotionResetReplicaExitCode -eq 0) -Expected '0' -Actual ([string]$promotionResetReplicaExitCode)

    $readOnlyOff = Invoke-MySqlText -Container $replicaContainer -Sql 'SET GLOBAL super_read_only=OFF; SET GLOBAL read_only=OFF;' -AllowFailure
    $promotionReadOnlyOffExitCode = $readOnlyOff.ExitCode
    Add-Check -Scenario 'promotion' -Name 'read_only_off_exit_code' -Passed ($promotionReadOnlyOffExitCode -eq 0) -Expected '0' -Actual ([string]$promotionReadOnlyOffExitCode)

    $promotionReadOnly = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.read_only;'
    $promotionSuperReadOnly = Get-MySqlScalar -Container $replicaContainer -Sql 'SELECT @@global.super_read_only;'
    Add-Check -Scenario 'promotion' -Name 'promoted_read_only' -Passed ($promotionReadOnly -eq '0') -Expected '0' -Actual $promotionReadOnly
    Add-Check -Scenario 'promotion' -Name 'promoted_super_read_only' -Passed ($promotionSuperReadOnly -eq '0') -Expected '0' -Actual $promotionSuperReadOnly

    $promotionWriteProbeAffectedRows = Invoke-MySqlUpdateAndGetRowCount -Container $replicaContainer -Database $applicationDatabase -Sql "UPDATE event_outbox SET last_error = 'promotion-write-probe' WHERE id = 1001 AND tenant_id = 1 AND deleted = 0;"
    Add-Check -Scenario 'promotion' -Name 'promoted_write_probe_affected_rows' -Passed ($promotionWriteProbeAffectedRows -eq 1) -Expected '1' -Actual ([string]$promotionWriteProbeAffectedRows)

    $outboxOwner = 'promoted-outbox-owner'
    $inboxOwner = 'promoted-inbox-owner'

    $outboxClaim1002Sql = @"
UPDATE event_outbox
SET status = 'PUBLISHING', processing_owner = '$outboxOwner',
    lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 1002 AND tenant_id = 1 AND deleted = 0
  AND (
      (status = 'NEW' AND (next_retry_at IS NULL OR next_retry_at <= UTC_TIMESTAMP()))
      OR (status = 'PUBLISHING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_active_claim_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxClaim1002Sql -Expected 0
    Assert-OwnerFencingOwner -Results $ownerFencingResults -Name 'outbox_active_owner_unchanged' -Container $replicaContainer -Database $applicationDatabase -Table event_outbox -Id 1002 -Expected 'source-active-owner'

    $outboxClaim1003Sql = @"
UPDATE event_outbox
SET status = 'PUBLISHING', processing_owner = '$outboxOwner',
    lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 1003 AND tenant_id = 1 AND deleted = 0
  AND (
      (status = 'NEW' AND (next_retry_at IS NULL OR next_retry_at <= UTC_TIMESTAMP()))
      OR (status = 'PUBLISHING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_expired_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxClaim1003Sql -Expected 1

    $outboxMarkPublishedOldSql = @"
UPDATE event_outbox
SET status = 'PUBLISHED', published_at = UTC_TIMESTAMP(), last_error = NULL,
    processing_owner = NULL, lease_until = NULL, next_retry_at = NULL
WHERE id = 1003 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = 'source-expired-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_old_owner_publish_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkPublishedOldSql -Expected 0

    $outboxMarkPublishedNewSql = @"
UPDATE event_outbox
SET status = 'PUBLISHED', published_at = UTC_TIMESTAMP(), last_error = NULL,
    processing_owner = NULL, lease_until = NULL, next_retry_at = NULL
WHERE id = 1003 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = '$outboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_new_owner_publish_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkPublishedNewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'outbox_published_state' -Container $replicaContainer -Database $applicationDatabase -Table event_outbox -Id 1003 -Expected 'PUBLISHED|||0'

    $outboxClaim1004Sql = @"
UPDATE event_outbox
SET status = 'PUBLISHING', processing_owner = '$outboxOwner',
    lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 1004 AND tenant_id = 1 AND deleted = 0
  AND (
      (status = 'NEW' AND (next_retry_at IS NULL OR next_retry_at <= UTC_TIMESTAMP()))
      OR (status = 'PUBLISHING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_retry_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxClaim1004Sql -Expected 1

    $outboxMarkRetryOldSql = @"
UPDATE event_outbox
SET status = 'NEW', retry_count = retry_count + 1,
    next_retry_at = UTC_TIMESTAMP(), last_error = 'sandbox',
    processing_owner = NULL, lease_until = NULL
WHERE id = 1004 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = 'source-expired-retry-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_old_owner_retry_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkRetryOldSql -Expected 0

    $outboxMarkRetryNewSql = @"
UPDATE event_outbox
SET status = 'NEW', retry_count = retry_count + 1,
    next_retry_at = UTC_TIMESTAMP(), last_error = 'sandbox',
    processing_owner = NULL, lease_until = NULL
WHERE id = 1004 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = '$outboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_new_owner_retry_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkRetryNewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'outbox_retry_state' -Container $replicaContainer -Database $applicationDatabase -Table event_outbox -Id 1004 -Expected 'NEW|||1'

    $outboxClaim1005Sql = @"
UPDATE event_outbox
SET status = 'PUBLISHING', processing_owner = '$outboxOwner',
    lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 1005 AND tenant_id = 1 AND deleted = 0
  AND (
      (status = 'NEW' AND (next_retry_at IS NULL OR next_retry_at <= UTC_TIMESTAMP()))
      OR (status = 'PUBLISHING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_dead_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxClaim1005Sql -Expected 1

    $outboxMarkDeadOldSql = @"
UPDATE event_outbox
SET status = 'DEAD', retry_count = retry_count + 1,
    last_error = 'sandbox', next_retry_at = NULL,
    processing_owner = NULL, lease_until = NULL
WHERE id = 1005 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = 'source-expired-dead-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_old_owner_dead_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkDeadOldSql -Expected 0

    $outboxMarkDeadNewSql = @"
UPDATE event_outbox
SET status = 'DEAD', retry_count = retry_count + 1,
    last_error = 'sandbox', next_retry_at = NULL,
    processing_owner = NULL, lease_until = NULL
WHERE id = 1005 AND tenant_id = 1 AND deleted = 0
  AND status = 'PUBLISHING' AND processing_owner = '$outboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'outbox_new_owner_dead_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $outboxMarkDeadNewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'outbox_dead_state' -Container $replicaContainer -Database $applicationDatabase -Table event_outbox -Id 1005 -Expected 'DEAD|||1'

    $inboxClaim2003Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2003 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_active_claim_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2003Sql -Expected 0
    Assert-OwnerFencingOwner -Results $ownerFencingResults -Name 'inbox_active_owner_unchanged' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2003 -Expected 'source-inbox-active-owner'

    $inboxClaim2001Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2001 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_received_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2001Sql -Expected 1

    $inboxMarkProcessed2001Sql = @"
UPDATE event_inbox
SET status = 'PROCESSED', processed_at = UTC_TIMESTAMP(), error = NULL,
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2001 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = '$inboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_received_processed_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkProcessed2001Sql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_received_processed_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2001 -Expected 'PROCESSED|||0'

    $inboxClaim2002Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2002 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_failed_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2002Sql -Expected 1

    $inboxMarkFailed2002OldSql = @"
UPDATE event_inbox
SET status = 'FAILED', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2002 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = 'source-failed';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_old_owner_failed_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkFailed2002OldSql -Expected 0

    $inboxMarkFailed2002NewSql = @"
UPDATE event_inbox
SET status = 'FAILED', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2002 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = '$inboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_new_owner_failed_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkFailed2002NewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_failed_retry_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2002 -Expected 'FAILED|||2'

    $inboxClaim2004Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2004 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_expired_processed_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2004Sql -Expected 1

    $inboxMarkProcessed2004OldSql = @"
UPDATE event_inbox
SET status = 'PROCESSED', processed_at = UTC_TIMESTAMP(), error = NULL,
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2004 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = 'source-inbox-expired-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_old_owner_processed_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkProcessed2004OldSql -Expected 0

    $inboxMarkProcessed2004NewSql = @"
UPDATE event_inbox
SET status = 'PROCESSED', processed_at = UTC_TIMESTAMP(), error = NULL,
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2004 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = '$inboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_new_owner_processed_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkProcessed2004NewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_expired_processed_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2004 -Expected 'PROCESSED|||0'

    $inboxClaim2005Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2005 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_expired_failed_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2005Sql -Expected 1

    $inboxMarkFailed2005OldSql = @"
UPDATE event_inbox
SET status = 'FAILED', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2005 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = 'source-inbox-failed-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_old_owner_expired_failed_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkFailed2005OldSql -Expected 0

    $inboxMarkFailed2005NewSql = @"
UPDATE event_inbox
SET status = 'FAILED', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2005 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = '$inboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_new_owner_expired_failed_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkFailed2005NewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_expired_failed_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2005 -Expected 'FAILED|||1'

    $inboxClaim2006Sql = @"
UPDATE event_inbox
SET status = 'PROCESSING', processing_owner = '$inboxOwner',
    claimed_at = UTC_TIMESTAMP(), lease_until = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 10 MINUTE)
WHERE id = 2006 AND tenant_id = 1 AND deleted = 0
  AND (
      status IN ('RECEIVED', 'FAILED')
      OR (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= UTC_TIMESTAMP()))
  );
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_expired_dead_claim_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxClaim2006Sql -Expected 1

    $inboxMarkDead2006OldSql = @"
UPDATE event_inbox
SET status = 'DEAD', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2006 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = 'source-inbox-dead-owner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_old_owner_dead_blocked' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkDead2006OldSql -Expected 0

    $inboxMarkDead2006NewSql = @"
UPDATE event_inbox
SET status = 'DEAD', retry_count = retry_count + 1, error = 'sandbox',
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2006 AND tenant_id = 1 AND deleted = 0
  AND status = 'PROCESSING' AND processing_owner = '$inboxOwner';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_new_owner_dead_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxMarkDead2006NewSql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_dead_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2006 -Expected 'DEAD|||1'

    $inboxReplay2007Sql = @"
UPDATE event_inbox
SET status = 'FAILED', retry_count = 0, error = NULL,
    processing_owner = NULL, lease_until = NULL, claimed_at = NULL
WHERE id = 2007 AND tenant_id = 1 AND deleted = 0 AND status = 'DEAD';
"@
    Invoke-OwnerFencingUpdate -Results $ownerFencingResults -Name 'inbox_dead_replay_succeeds' -Container $replicaContainer -Database $applicationDatabase -Sql $inboxReplay2007Sql -Expected 1
    Assert-OwnerFencingState -Results $ownerFencingResults -Name 'inbox_dead_replay_state' -Container $replicaContainer -Database $applicationDatabase -Table event_inbox -Id 2007 -Expected 'FAILED|||0'

    $status = 'passed'
    Write-RunLog "Physical replication approximation checks passed: checks=$($script:Checks.Count)"
}
catch {
    $failure = $_.Exception.ToString()
    if (-not [string]::IsNullOrWhiteSpace($rootPassword)) { $failure = $failure.Replace($rootPassword, '[REDACTED]') }
    if (-not [string]::IsNullOrWhiteSpace($replicationPassword)) { $failure = $failure.Replace($replicationPassword, '[REDACTED]') }
    Write-RunLog "FAILED: $($_.Exception.Message)"
}
finally {
    if (-not $KeepSandbox -and (-not [string]::IsNullOrWhiteSpace($sourceContainer) -or -not [string]::IsNullOrWhiteSpace($replicaContainer) -or -not [string]::IsNullOrWhiteSpace($network))) {
        $cleanupAttempted = $true
        Write-RunLog 'Removing temporary sandbox resources'
        Remove-SandboxResources -SourceContainer $sourceContainer -ReplicaContainer $replicaContainer -SourceVolume $sourceVolume -ReplicaVolume $replicaVolume -Network $network
    }
    elseif ($KeepSandbox) {
        $resourcesRetained = $true
        Write-RunLog "KeepSandbox supplied; resources retained: source=$sourceContainer replica=$replicaContainer network=$network"
    }

    if ($script:CleanupFailures.Count -gt 0) { $status = 'failed' }
    $finishedAt = Get-Date
    $durationSeconds = [math]::Round(($finishedAt - $startedAt).TotalSeconds, 1)
    $checks = @($script:Checks)
    $failedChecks = @($checks | Where-Object { -not [bool]$_.passed })
    $validationPassed = ($status -eq 'passed' -and $null -eq $failure -and $failedChecks.Count -eq 0)

    $replicaStatusSnapshot = [ordered]@{}
    foreach ($statusObject in @($replicaStatusAfterStart, $replicaStatusAfterRestart)) {
        if ($null -eq $statusObject) { continue }
        $replicaStatusSnapshot = [ordered]@{
            Replica_IO_Running       = [string]$statusObject['Replica_IO_Running']
            Replica_SQL_Running      = [string]$statusObject['Replica_SQL_Running']
            Seconds_Behind_Source    = [string]$statusObject['Seconds_Behind_Source']
            Source_Host              = [string]$statusObject['Source_Host']
            Retrieved_Gtid_Set       = [string]$statusObject['Retrieved_Gtid_Set']
            Executed_Gtid_Set        = [string]$statusObject['Executed_Gtid_Set']
            Last_IO_Error            = [string]$statusObject['Last_IO_Error']
            Last_SQL_Error           = [string]$statusObject['Last_SQL_Error']
        }
        break
    }
    $replicaStatusAfterRestartSnapshot = [ordered]@{}
    if ($null -ne $replicaStatusAfterRestart) {
        $replicaStatusAfterRestartSnapshot = [ordered]@{
            Replica_IO_Running       = [string]$replicaStatusAfterRestart['Replica_IO_Running']
            Replica_SQL_Running      = [string]$replicaStatusAfterRestart['Replica_SQL_Running']
            Seconds_Behind_Source    = [string]$replicaStatusAfterRestart['Seconds_Behind_Source']
            Source_Host              = [string]$replicaStatusAfterRestart['Source_Host']
            Retrieved_Gtid_Set       = [string]$replicaStatusAfterRestart['Retrieved_Gtid_Set']
            Executed_Gtid_Set        = [string]$replicaStatusAfterRestart['Executed_Gtid_Set']
            Last_IO_Error            = [string]$replicaStatusAfterRestart['Last_IO_Error']
            Last_SQL_Error           = [string]$replicaStatusAfterRestart['Last_SQL_Error']
        }
    }

    $payload = [ordered]@{
        schema_version = 1
        evidence_type = 'mysql_physical_replication_sandbox'
        run_id = $runId
        status = $status
        validation_passed = $validationPassed
        started_at = $startedAt.ToString('o')
        finished_at = $finishedAt.ToString('o')
        duration_seconds = $durationSeconds
        image = $Image
        image_repo_digest = $imageRepoDigest
        repo_root = $resolvedRepoRoot
        repo = Get-RepoFacts
        source_container = $sourceContainer
        replica_container = $replicaContainer
        network = $network
        source_volume = $sourceVolume
        replica_volume = $replicaVolume
        database = 'zhicloud_replication_probe'
        table = 'replication_probe'
        application_database = $applicationDatabase
        application_migration_hashes = $applicationMigrationHashes
        application_schema_ready = $applicationSchemaReady
        application_v120_verification = $applicationV120Verification
        application_row_counts_before_promotion = $applicationRowCountsBeforePromotion
        application_fingerprints_before_promotion = $applicationFingerprintsBeforePromotion
        checks = $checks
        check_count = $checks.Count
        failed_checks = $failedChecks
        replication_status_after_start = $replicaStatusSnapshot
        replication_status_after_restart = $replicaStatusAfterRestartSnapshot
        fingerprint_after_start = $fingerprintAfterStart
        fingerprint_after_restart = $fingerprintAfterRestart
        row_counts = [ordered]@{
            source_after_start = $sourceRowCountAfterStart
            replica_after_start = $replicaRowCountAfterStart
            source_after_restart = $sourceRowCountAfterRestart
            replica_after_restart = $replicaRowCountAfterRestart
        }
        read_only_enforced = $readOnlyEnforced
        read_only_write_exit_code = $readOnlyWriteExitCode
        read_only_write_output = $readOnlyWriteOutput
        restart_exit_code = $restartExitCode
        promotion = [ordered]@{
            source_gtid_executed_before_promotion = $promotionSourceGtidBefore
            replica_status_before_promotion       = $promotionReplicaStatusBefore
            source_stop_exit_code                 = $promotionSourceStopExitCode
            source_running_after_stop             = $sourceRunningAfterStop
            replica_stop_exit_code                = $promotionStopReplicaExitCode
            replica_reset_exit_code               = $promotionResetReplicaExitCode
            read_only_off_exit_code               = $promotionReadOnlyOffExitCode
            promoted_read_only                    = $promotionReadOnly
            promoted_super_read_only              = $promotionSuperReadOnly
            write_probe_affected_rows             = $promotionWriteProbeAffectedRows
        }
        owner_fencing = $ownerFencingResults
        resources_retained = $resourcesRetained
        cleanup_attempted = $cleanupAttempted
        cleanup_failures = @($script:CleanupFailures)
        secrets_recorded = $false
        caveats = @(
            'Both MySQL containers run on one Docker host; this is not a production physical replication topology.',
            'This does not validate HA, failover, real network partition, replication lag under production load, MDL contention, disaster recovery, backup restore, or production data volume.',
            'The V100->V104->V107->V120 chain is a minimal application migration subset, not the full production V1->V120 migration baseline or a production-scale DDL test.',
            'Outbox/Inbox rows are inserted directly as SQL fixtures; the run does not exercise application worker concurrency, DLQ service replay, compensation, reconciliation, or tenant datasource topology.',
            'Direct SQL owner-fencing CAS assertions do not prove end-to-end application fencing under process pause, network partition, or concurrent service execution.',
            'A passed sandbox result must not be represented as production acceptance or as evidence for gate3, independent review, R-011, or R-029 closure.'
        )
        gates = [ordered]@{
            production_ready = $false
            gate3_passed = $false
            independent_review = $false
            R_011 = 'OPEN'
            R_029 = 'OPEN'
            Go_No_Go = 'No-Go'
        }
        failure = $failure
    }

    Write-TextFile -Path $runLogPath -Text (($script:LogLines -join [Environment]::NewLine) + [Environment]::NewLine)
    Write-JsonFile -Path $evidenceJsonPath -Payload $payload
    $evidenceHash = Get-Sha256 -Path $evidenceJsonPath
    Write-TextFile -Path ($evidenceJsonPath + '.sha256') -Text ("{0}  {1}{2}" -f $evidenceHash, [System.IO.Path]::GetFileName($evidenceJsonPath), [Environment]::NewLine)

    $summaryLines = @(
        '# MySQL Physical Replication Sandbox Approximation',
        '',
        "- status: $status",
        "- validation_passed: $validationPassed",
        "- run_id: $runId",
        "- image: $Image",
        "- repo_root: $resolvedRepoRoot",
        "- source_container: $sourceContainer",
        "- replica_container: $replicaContainer",
        "- application_database: $applicationDatabase",
        "- application_schema_ready: $applicationSchemaReady",
        "- promotion: one-way source stop/replica promotion exercise (not HA or failover validation)",
        "- owner_fencing: direct SQL CAS assertions recorded in evidence.json",
        "- checks: $($checks.Count); failed: $($failedChecks.Count)",
        "- evidence.json SHA-256: $evidenceHash",
        '',
        '## Scope',
        '',
        'This evidence proves only that two digest-pinned MySQL containers on one Docker host can be configured for GTID binlog replication, replicate a small synthetic probe table and direct SQL Outbox/Inbox fixtures, enforce replica read-only, resume catch-up after a controlled replica restart, apply a minimal V100->V104->V107->V120 migration chain, perform a one-way source-stop/replica-promotion exercise, and exercise direct SQL owner-fencing CAS assertions.',
        '',
        '## Explicitly Not Proven',
        '',
        '- production physical master/replica topology, HA, failover, or disaster recovery;',
        '- network partition, production latency, MDL contention, or production-scale DDL/data;',
        '- the full production V1->V120 migration baseline or production application migration correctness;',
        '- application worker concurrency, DLQ service replay, compensation, reconciliation, or tenant datasource topology;',
        '- end-to-end application fencing under process pause, network partition, or concurrent service execution;',
        '- production readiness, gate3, independent review, R-011 closure, or R-029 closure.',
        '',
        '## Gate State',
        '',
        '- production_ready=false',
        '- gate3_passed=false',
        '- independent_review=false',
        '- R-011=OPEN',
        '- R-029=OPEN',
        '- Go_No_Go=No-Go',
        '',
        "- failure: $failure"
    )
    Write-TextFile -Path $summaryPath -Text (($summaryLines -join [Environment]::NewLine) + [Environment]::NewLine)
    $summaryHash = Get-Sha256 -Path $summaryPath
    Write-TextFile -Path ($summaryPath + '.sha256') -Text ("{0}  {1}{2}" -f $summaryHash, [System.IO.Path]::GetFileName($summaryPath), [Environment]::NewLine)

    $exitCode = if ($validationPassed) { 0 } else { 2 }
    Write-TextFile -Path $exitPath -Text ([string]$exitCode + [Environment]::NewLine)
    if ($validationPassed) {
        Write-RunLog "Evidence written to $EvidenceDir"
        exit 0
    }
    Write-RunLog "Validation failed; evidence written to $EvidenceDir"
    exit 2
}
