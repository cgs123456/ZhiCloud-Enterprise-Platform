# 备份 / 恢复 / 回滚演练。
#
# 退出码约定：0=演练通过；2=演练或校验失败；1=用法或内部错误。
#
# 设计要点：
#   - 只接受 mysql-sandbox.ps1 创建的随机命名沙箱容器，且主机必须是 localhost/127.0.0.1；
#   - 恢复目标库必须带 drill/sandbox 前缀，且不得等于源库；
#   - 备份之后写入的数据在恢复后会丢失，这正是 RPO 的暴露面，必须被量化而不是被隐藏；
#   - 没有行数、校验和、Flyway 历史这三类校验的恢复，一律判定为失败。
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('drill', 'verify', 'selftest')]
    [string]$Command,

    [string]$SandboxState,
    [string]$SourceDatabase,
    [string]$TargetDatabase,
    [string]$ProbeTable = 'zc_drill_probe',
    [string]$EvidenceFile,
    [string]$BeforeSnapshot,
    [string]$AfterSnapshot,
    [switch]$DryRun,
    [switch]$SkipCleanup
)

$ErrorActionPreference = 'Stop'

$script:Container = $null
$script:RootPassword = $null

function Get-DrillScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    throw 'Unable to resolve script directory.'
}

function ConvertTo-DrillJson {
    param([Parameter(Mandatory = $true)]$Payload)
    return ($Payload | ConvertTo-Json -Depth 8)
}

function Write-DrillJsonFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)]$Payload
    )
    $directory = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($directory)) {
        New-Item -ItemType Directory -Path $directory -Force | Out-Null
    }
    $jsonText = (ConvertTo-DrillJson -Payload $Payload) + "`n"
    [System.IO.File]::WriteAllText($Path, $jsonText, [System.Text.UTF8Encoding]::new($false))
}

function Read-DrillJsonFile {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "JSON file not found: $Path"
    }
    return ([System.IO.File]::ReadAllText($Path) | ConvertFrom-Json)
}

function Invoke-SandboxSql {
    param(
        [Parameter(Mandatory = $true)][string]$SqlText,
        [string]$Database
    )
    $arguments = @(
        'exec', '-i',
        '-e', "MYSQL_PWD=$script:RootPassword",
        $script:Container,
        'mysql', '--batch', '--raw', '--skip-column-names',
        '-uroot', '--default-character-set=utf8mb4'
    )
    if (-not [string]::IsNullOrWhiteSpace($Database)) {
        $arguments += $Database
    }
    $output = $SqlText | & docker @arguments 2>&1
    $exitCode = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output | ForEach-Object { [string]$_ })
    }
}

function Invoke-Mysqldump {
    param(
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$ResultFile,
        [string[]]$Tables = @()
    )
    $arguments = @(
        'exec',
        '-e', "MYSQL_PWD=$script:RootPassword",
        $script:Container,
        'mysqldump', '-uroot',
        '--single-transaction', '--routines', '--triggers',
        '--set-gtid-purged=OFF', '--default-character-set=utf8mb4',
        "--result-file=$ResultFile"
    )
    $arguments += $Database
    if ($Tables.Count -gt 0) { $arguments += $Tables }
    $output = & docker @arguments 2>&1
    $exitCode = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output | ForEach-Object { [string]$_ })
    }
}

function Invoke-SandboxShell {
    param([Parameter(Mandatory = $true)][string]$ShellCommand)
    $arguments = @(
        'exec',
        '-e', "MYSQL_PWD=$script:RootPassword",
        $script:Container,
        'sh', '-c', $ShellCommand
    )
    $output = & docker @arguments 2>&1
    $exitCode = $LASTEXITCODE
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output | ForEach-Object { [string]$_ })
    }
}

function ConvertTo-DrillChecksumKey {
    # CHECKSUM TABLE 的输出是 `库名.表名`，而 Get-TableRowCounts 用的是裸表名。
    # 演练会把全量 dump 恢复到另一个库（zhicloud_drill_xxxx）再与源库比对，
    # 如果不去掉库名前缀，源库快照的键永远匹配不上目标库快照，会全表误报缺失。
    param([Parameter(Mandatory = $true)][string]$Name)

    $index = $Name.LastIndexOf('.')
    if ($index -ge 0) { return $Name.Substring($index + 1) }
    return $Name
}

function Get-TableRowCounts {
    param([Parameter(Mandatory = $true)][string]$Database)
    $generator = @"
SELECT CONCAT('SELECT ''', TABLE_NAME, ''' AS table_name, COUNT(*) AS row_count FROM `', TABLE_NAME, '` UNION ALL ')
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
"@
    $generated = Invoke-SandboxSql -SqlText $generator -Database $Database
    if ($generated.ExitCode -ne 0) {
        throw "failed to generate row count SQL: $($generated.Output -join ' ')"
    }
    $fragments = @($generated.Output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($fragments.Count -eq 0) {
        return [ordered]@{}
    }
    $sql = (($fragments -join '')).TrimEnd()
    $sql = $sql.Substring(0, $sql.Length - 'UNION ALL'.Length).TrimEnd() + ';'
    $result = Invoke-SandboxSql -SqlText $sql -Database $Database
    if ($result.ExitCode -ne 0) {
        throw "failed to collect row counts: $($result.Output -join ' ')"
    }
    $counts = [ordered]@{}
    foreach ($line in $result.Output) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $parts = $line.Split("`t")
        if ($parts.Count -lt 2) { continue }
        $counts[$parts[0]] = [int64]$parts[1]
    }
    return $counts
}

function Get-TableChecksums {
    param(
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string[]]$Tables,
        [int]$BatchSize = 40
    )
    $checksums = [ordered]@{}
    for ($offset = 0; $offset -lt $Tables.Count; $offset += $BatchSize) {
        $batch = @($Tables | Select-Object -Skip $offset -First $BatchSize)
        $quoted = ($batch | ForEach-Object { "``$_``" }) -join ', '
        $result = Invoke-SandboxSql -SqlText "CHECKSUM TABLE $quoted;" -Database $Database
        if ($result.ExitCode -ne 0) {
            throw "failed to checksum batch: $($result.Output -join ' ')"
        }
        foreach ($line in $result.Output) {
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            $parts = $line.Split("`t")
            if ($parts.Count -lt 2) { continue }
            $checksums[(ConvertTo-DrillChecksumKey -Name ([string]$parts[0]))] = [string]$parts[1]
        }
    }
    return $checksums
}

function Get-FlywayState {
    param([Parameter(Mandatory = $true)][string]$Database)
    $sql = @"
SELECT CONCAT((SELECT COUNT(*) FROM flyway_schema_history), CHAR(9),
              COALESCE((SELECT MAX(version) FROM flyway_schema_history), 'none'));
"@
    $result = Invoke-SandboxSql -SqlText $sql -Database $Database
    if ($result.ExitCode -ne 0) {
        return [ordered]@{ history_rows = 0; max_version = 'none'; collected = $false }
    }
    $line = @($result.Output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) | Select-Object -First 1
    if ($null -eq $line) {
        return [ordered]@{ history_rows = 0; max_version = 'none'; collected = $true }
    }
    $parts = ([string]$line).Split("`t")
    return [ordered]@{
        history_rows = [int]$parts[0]
        max_version  = if ($parts.Count -gt 1) { $parts[1] } else { 'none' }
        collected    = $true
    }
}

function New-Snapshot {
    param(
        [Parameter(Mandatory = $true)][string]$Database,
        [Parameter(Mandatory = $true)][string]$ProbeTableName
    )
    $rowCounts = Get-TableRowCounts -Database $Database
    $checksums = Get-TableChecksums -Database $Database -Tables @($rowCounts.Keys)
    return [ordered]@{
        database     = $Database
        generated_at = [DateTimeOffset]::UtcNow.ToString('o')
        probe_table  = $ProbeTableName
        row_counts   = $rowCounts
        checksums    = $checksums
        flyway       = (Get-FlywayState -Database $Database)
    }
}

function Get-DrillMapEntries {
    # Compare-Snapshots 会被两种输入调用：
    #   1) `verify` 命令：JSON 反序列化出来的 PSCustomObject，PSObject.Properties 即数据条目；
    #   2) `drill` 命令：脚本内实时构造的 [ordered]@{}，PSObject.Properties 返回的是
    #      Count/Keys/Values 这类 CLR 属性，直接遍历会把属性当成表名并抛类型转换异常。
    # 因此这里统一按「IDictionary 走 Keys，其余走 PSObject.Properties」返回条目列表。
    param([Parameter(Mandatory = $false)]$Map)

    $entries = [System.Collections.Generic.List[object]]::new()
    if ($null -eq $Map) { return @() }
    if ($Map -is [System.Collections.IDictionary]) {
        foreach ($key in @($Map.Keys)) {
            $entries.Add([pscustomobject]@{ Name = [string]$key; Value = $Map[$key] })
        }
        return @($entries)
    }
    foreach ($property in @($Map.PSObject.Properties)) {
        $entries.Add([pscustomobject]@{ Name = [string]$property.Name; Value = $property.Value })
    }
    return @($entries)
}

function ConvertTo-DrillLookup {
    param([Parameter(Mandatory = $false)]$Map)
    $lookup = @{}
    foreach ($entry in (Get-DrillMapEntries -Map $Map)) {
        $lookup[$entry.Name] = $entry.Value
    }
    return $lookup
}

function Compare-Snapshots {
    param(
        [Parameter(Mandatory = $true)]$Before,
        [Parameter(Mandatory = $true)]$After
    )
    $reasons = [System.Collections.Generic.List[string]]::new()

    $beforeProbe = [string]$Before.probe_table
    $afterProbe = [string]$After.probe_table
    if ([string]::IsNullOrWhiteSpace($afterProbe)) {
        $reasons.Add('probe_table_missing: restored snapshot has no probe table name')
    }
    elseif ($beforeProbe -ne $afterProbe) {
        $reasons.Add("probe_table_mismatch: expected $beforeProbe got $afterProbe")
    }

    $beforeRows = @(Get-DrillMapEntries -Map $Before.row_counts)
    $afterRows = @(Get-DrillMapEntries -Map $After.row_counts)
    if ($afterRows.Count -eq 0) {
        $reasons.Add('row_counts_missing: restored snapshot has no row counts')
    }
    elseif ($beforeRows.Count -gt 0) {
        $afterRowLookup = ConvertTo-DrillLookup -Map $After.row_counts
        foreach ($entry in $beforeRows) {
            if (-not $afterRowLookup.ContainsKey($entry.Name)) {
                $reasons.Add("row_counts_missing_table: $($entry.Name)")
                continue
            }
            $afterValue = $afterRowLookup[$entry.Name]
            if ([int64]$afterValue -ne [int64]$entry.Value) {
                $reasons.Add("row_count_mismatch: $($entry.Name) expected $($entry.Value) got $afterValue")
            }
        }
    }

    $beforeChecksums = @(Get-DrillMapEntries -Map $Before.checksums)
    $afterChecksums = @(Get-DrillMapEntries -Map $After.checksums)
    if ($afterChecksums.Count -eq 0) {
        $reasons.Add('checksums_missing: restored snapshot has no checksums')
    }
    elseif ($beforeChecksums.Count -gt 0) {
        $afterChecksumLookup = ConvertTo-DrillLookup -Map $After.checksums
        foreach ($entry in $beforeChecksums) {
            if (-not $afterChecksumLookup.ContainsKey($entry.Name)) {
                $reasons.Add("checksum_missing_table: $($entry.Name)")
                continue
            }
            $afterValue = $afterChecksumLookup[$entry.Name]
            if ([string]$afterValue -ne [string]$entry.Value) {
                $reasons.Add("checksum_mismatch: $($entry.Name) expected $($entry.Value) got $afterValue")
            }
        }
    }

    $beforeFlyway = $Before.flyway
    $afterFlyway = $After.flyway
    if ($null -eq $afterFlyway) {
        $reasons.Add('flyway_missing: restored snapshot has no flyway history')
    }
    else {
        if ([int]$afterFlyway.history_rows -ne [int]$beforeFlyway.history_rows) {
            $reasons.Add("flyway_history_rows_mismatch: expected $($beforeFlyway.history_rows) got $($afterFlyway.history_rows)")
        }
        if ([string]$afterFlyway.max_version -ne [string]$beforeFlyway.max_version) {
            $reasons.Add("flyway_max_version_mismatch: expected $($beforeFlyway.max_version) got $($afterFlyway.max_version)")
        }
    }

    return $reasons
}
function Invoke-VerifyCommand {
    param(
        [Parameter(Mandatory = $true)][string]$Before,
        [Parameter(Mandatory = $true)][string]$After
    )

    $beforeSnapshot = Read-DrillJsonFile -Path $Before
    $afterSnapshot = Read-DrillJsonFile -Path $After
    $reasons = Compare-Snapshots -Before $beforeSnapshot -After $afterSnapshot
    $status = if ($reasons.Count -eq 0) { 'passed' } else { 'failed' }

    $payload = [ordered]@{
        status             = $status
        generated_at       = [DateTimeOffset]::UtcNow.ToString('o')
        command            = 'verify'
        before_snapshot    = $Before
        after_snapshot     = $After
        tables_compared    = @(Get-DrillMapEntries -Map $beforeSnapshot.row_counts).Count
        checksums_compared = @(Get-DrillMapEntries -Map $beforeSnapshot.checksums).Count
        failure_reasons    = @($reasons)
    }

    Write-Output (ConvertTo-DrillJson -Payload $payload)

    if ($status -eq 'passed') { exit 0 }
    [Console]::Error.WriteLine("restore verification failed: $($reasons -join '; ')")
    exit 2
}

function New-DrillStep {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [int]$ExitCode = 0,
        [double]$DurationSeconds = 0,
        [string]$Note = ''
    )
    return [ordered]@{
        name             = $Name
        exit_code        = $ExitCode
        duration_seconds = [math]::Round($DurationSeconds, 3)
        note             = $Note
    }
}

function Invoke-DrillCommand {
    param(
        [Parameter(Mandatory = $true)]$State,
        [Parameter(Mandatory = $true)][string]$SourceDb,
        [Parameter(Mandatory = $true)][string]$TargetDb,
        [Parameter(Mandatory = $true)][string]$ProbeTableName,
        [Parameter(Mandatory = $true)][string]$EvidencePath,
        [switch]$DryRunOnly,
        [switch]$NoCleanup
    )

    $script:Container = [string]$State.container_name
    $script:RootPassword = [string]$State.root_password

    $backupFile = "/tmp/zc-drill-full-" + [guid]::NewGuid().ToString('n').Substring(0, 8) + ".sql"
    $probeFile = "/tmp/zc-drill-probe-" + [guid]::NewGuid().ToString('n').Substring(0, 8) + ".sql"

    if ($DryRunOnly) {
        $plan = [ordered]@{
            dry_run          = $true
            command          = 'drill'
            container        = $script:Container
            source_database  = $SourceDb
            target_database  = $TargetDb
            probe_table      = $ProbeTableName
            backup_file      = $backupFile
            recovery_steps   = @(
                'seed probe rows',
                'snapshot before',
                'mysqldump full database',
                'write rows after backup to expose the RPO window',
                'drop probe table to simulate an incident',
                'restore probe table from its backup',
                'snapshot after and verify row counts, checksums, flyway history',
                'restore the full dump into the drill target database',
                'compare source and target, then clean up'
            )
            backup_command   = "mysqldump -uroot --single-transaction --routines --triggers --set-gtid-purged=OFF $SourceDb --result-file=$backupFile"
            restore_command  = "mysql -uroot --default-character-set=utf8mb4 $TargetDb < $backupFile"
            disclaimer       = 'FAKE-SIMULATED-DATA'
        }
        Write-Output (ConvertTo-DrillJson -Payload $plan)
        exit 0
    }

    $steps = [System.Collections.Generic.List[object]]::new()
    $failureReasons = [System.Collections.Generic.List[string]]::new()
    $seedRows = 24
    $afterBackupRows = 6
    $rpoWindowSeconds = $null
    $pointRestoreSeconds = $null
    $fullRestoreSeconds = $null
    $backupSeconds = $null
    $backupSizeBytes = $null
    $backupSha256 = $null
    $probeRowsRecovered = $null
    $probeRowsLostAfterBackup = $null
    $targetComparisons = $null
    $verificationReasons = @()

    try {
        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        $seedSql = @"
DROP TABLE IF EXISTS ``$ProbeTableName``;
CREATE TABLE ``$ProbeTableName`` (
  ``id`` bigint NOT NULL AUTO_INCREMENT,
  ``batch`` varchar(16) NOT NULL,
  ``payload`` varchar(255) NOT NULL,
  ``created_at`` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (``id``),
  KEY ``idx_batch`` (``batch``)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
"@
        $seedValues = @()
        for ($i = 1; $i -le $seedRows; $i++) {
            $seedValues += "('seeded', 'FAKE-SIM drill-seed-$i')"
        }
        $seedSql += "`nINSERT INTO ``$ProbeTableName`` (batch, payload) VALUES " + ($seedValues -join ',') + ";"

        $seed = Invoke-SandboxSql -SqlText $seedSql -Database $SourceDb
        $steps.Add((New-DrillStep -Name 'seed_probe_rows' -ExitCode $seed.ExitCode -DurationSeconds $watch.Elapsed.TotalSeconds -Note "$seedRows simulated rows"))
        if ($seed.ExitCode -ne 0) { throw "seed failed: $($seed.Output -join ' ')" }

        $watch.Restart()
        $before = New-Snapshot -Database $SourceDb -ProbeTableName $ProbeTableName
        $steps.Add((New-DrillStep -Name 'snapshot_before' -ExitCode 0 -DurationSeconds $watch.Elapsed.TotalSeconds -Note "$(@(Get-DrillMapEntries -Map $before.row_counts).Count) tables"))

        $watch.Restart()
        $probeDump = Invoke-Mysqldump -Database $SourceDb -ResultFile $probeFile -Tables @($ProbeTableName)
        if ($probeDump.ExitCode -ne 0) { throw "probe dump failed: $($probeDump.Output -join ' ')" }

        $watch.Restart()
        $fullDump = Invoke-Mysqldump -Database $SourceDb -ResultFile $backupFile
        $backupSeconds = $watch.Elapsed.TotalSeconds
        $steps.Add((New-DrillStep -Name 'backup_full_database' -ExitCode $fullDump.ExitCode -DurationSeconds $backupSeconds -Note $backupFile))
        if ($fullDump.ExitCode -ne 0) { throw "full dump failed: $($fullDump.Output -join ' ')" }
        $backupCompletedAt = [DateTimeOffset]::UtcNow

        $size = Invoke-SandboxShell -ShellCommand "stat -c %s $backupFile"
        if ($size.ExitCode -eq 0 -and $size.Output.Count -gt 0) {
            $backupSizeBytes = [int64]([string]$size.Output[0]).Trim()
        }
        $hash = Invoke-SandboxShell -ShellCommand "sha256sum $backupFile | cut -d ' ' -f 1"
        if ($hash.ExitCode -eq 0 -and $hash.Output.Count -gt 0) {
            $backupSha256 = ([string]$hash.Output[0]).Trim()
        }

        $afterValues = @()
        for ($i = 1; $i -le $afterBackupRows; $i++) {
            $afterValues += "('after_backup', 'FAKE-SIM drill-after-$i')"
        }
        $afterSql = "INSERT INTO ``$ProbeTableName`` (batch, payload) VALUES " + ($afterValues -join ',') + ";"
        $afterWrite = Invoke-SandboxSql -SqlText $afterSql -Database $SourceDb
        $steps.Add((New-DrillStep -Name 'write_rows_after_backup' -ExitCode $afterWrite.ExitCode -DurationSeconds 0 -Note "$afterBackupRows rows inside the RPO window"))
        if ($afterWrite.ExitCode -ne 0) { throw "post-backup write failed: $($afterWrite.Output -join ' ')" }

        $incidentAt = [DateTimeOffset]::UtcNow
        $rpoWindowSeconds = ($incidentAt - $backupCompletedAt).TotalSeconds
        $drop = Invoke-SandboxSql -SqlText "DROP TABLE ``$ProbeTableName``;" -Database $SourceDb
        $steps.Add((New-DrillStep -Name 'simulate_incident_drop_probe' -ExitCode $drop.ExitCode -DurationSeconds 0 -Note "dropped $ProbeTableName"))
        if ($drop.ExitCode -ne 0) { throw "incident simulation failed: $($drop.Output -join ' ')" }

        $watch.Restart()
        $pointRestore = Invoke-SandboxShell -ShellCommand "mysql -uroot --default-character-set=utf8mb4 $SourceDb < $probeFile"
        $pointRestoreSeconds = $watch.Elapsed.TotalSeconds
        $steps.Add((New-DrillStep -Name 'restore_probe_table' -ExitCode $pointRestore.ExitCode -DurationSeconds $pointRestoreSeconds -Note 'point-in-time restore of the probe table'))
        if ($pointRestore.ExitCode -ne 0) { throw "point restore failed: $($pointRestore.Output -join ' ')" }

        $watch.Restart()
        $after = New-Snapshot -Database $SourceDb -ProbeTableName $ProbeTableName
        $steps.Add((New-DrillStep -Name 'snapshot_after' -ExitCode 0 -DurationSeconds $watch.Elapsed.TotalSeconds -Note "$(@(Get-DrillMapEntries -Map $after.row_counts).Count) tables"))

        $verificationReasons = @(Compare-Snapshots -Before $before -After $after)
        $steps.Add((New-DrillStep -Name 'verify_source_after_restore' -ExitCode $(if ($verificationReasons.Count -eq 0) { 0 } else { 2 }) -DurationSeconds 0 -Note "$($verificationReasons.Count) verification problems"))
        if ($verificationReasons.Count -gt 0) {
            foreach ($reason in $verificationReasons) { $failureReasons.Add($reason) }
        }

        $probeStats = Invoke-SandboxSql -SqlText "SELECT batch, COUNT(*) FROM ``$ProbeTableName`` GROUP BY batch ORDER BY batch;" -Database $SourceDb
        $recovered = 0
        $lost = 0
        if ($probeStats.ExitCode -eq 0) {
            foreach ($line in $probeStats.Output) {
                if ([string]::IsNullOrWhiteSpace($line)) { continue }
                $parts = ([string]$line).Split("`t")
                if ($parts[0] -eq 'seeded') { $recovered = [int]$parts[1] }
                if ($parts[0] -eq 'after_backup') { $lost = [int]$parts[1] }
            }
        }
        $probeRowsRecovered = $recovered
        $probeRowsLostAfterBackup = $afterBackupRows - $lost
        if ($recovered -ne $seedRows) {
            $failureReasons.Add("probe_rows_not_restored: expected $seedRows got $recovered")
        }

        $watch.Restart()
        $createTarget = Invoke-SandboxSql -SqlText "CREATE DATABASE IF NOT EXISTS ``$TargetDb`` DEFAULT CHARACTER SET utf8mb4;" -Database ''
        if ($createTarget.ExitCode -ne 0) { throw "target database creation failed: $($createTarget.Output -join ' ')" }
        $fullRestore = Invoke-SandboxShell -ShellCommand "mysql -uroot --default-character-set=utf8mb4 $TargetDb < $backupFile"
        $fullRestoreSeconds = $watch.Elapsed.TotalSeconds
        $steps.Add((New-DrillStep -Name 'restore_full_dump_into_target' -ExitCode $fullRestore.ExitCode -DurationSeconds $fullRestoreSeconds -Note $TargetDb))
        if ($fullRestore.ExitCode -ne 0) { throw "full restore failed: $($fullRestore.Output -join ' ')" }

        $targetSnapshot = New-Snapshot -Database $TargetDb -ProbeTableName $ProbeTableName
        $targetReasons = @(Compare-Snapshots -Before $after -After $targetSnapshot)
        $targetComparisons = [ordered]@{
            tables_compared    = @(Get-DrillMapEntries -Map $targetSnapshot.row_counts).Count
            checksums_compared = @(Get-DrillMapEntries -Map $targetSnapshot.checksums).Count
            reasons            = @($targetReasons)
        }
        $steps.Add((New-DrillStep -Name 'compare_source_and_target' -ExitCode $(if ($targetReasons.Count -eq 0) { 0 } else { 2 }) -DurationSeconds 0 -Note "$($targetReasons.Count) differences"))
        foreach ($reason in $targetReasons) { $failureReasons.Add("target_$reason") }
    }
    catch {
        $failureReasons.Add("drill_aborted: $($_.Exception.Message)")
    }
    finally {
        if (-not $NoCleanup) {
            $cleanupWatch = [System.Diagnostics.Stopwatch]::StartNew()
            $dropTarget = Invoke-SandboxSql -SqlText "DROP DATABASE IF EXISTS ``$TargetDb``;" -Database ''
            $dropProbe = Invoke-SandboxSql -SqlText "DROP TABLE IF EXISTS ``$ProbeTableName``;" -Database $SourceDb
            $steps.Add((New-DrillStep -Name 'cleanup' -ExitCode $(if ($dropTarget.ExitCode -eq 0 -and $dropProbe.ExitCode -eq 0) { 0 } else { 1 }) -DurationSeconds $cleanupWatch.Elapsed.TotalSeconds -Note 'dropped drill target database and probe table'))
            if ($dropTarget.ExitCode -ne 0 -or $dropProbe.ExitCode -ne 0) {
                $failureReasons.Add('cleanup_incomplete')
            }
        }
        else {
            $steps.Add((New-DrillStep -Name 'cleanup' -ExitCode 0 -DurationSeconds 0 -Note 'skipped by -SkipCleanup'))
        }
    }

    $status = if ($failureReasons.Count -eq 0) { 'passed' } else { 'failed' }
    $evidence = [ordered]@{
        status                     = $status
        generated_at               = [DateTimeOffset]::UtcNow.ToString('o')
        command                    = 'drill'
        disclaimer                 = 'FAKE-SIMULATED-DATA'
        production_import_allowed  = $false
        container                  = $script:Container
        source_database            = $SourceDb
        target_database            = $TargetDb
        probe_table                = $ProbeTableName
        backup                     = [ordered]@{
            file          = $backupFile
            size_bytes    = $backupSizeBytes
            sha256        = $backupSha256
            duration_seconds = if ($null -eq $backupSeconds) { $null } else { [math]::Round($backupSeconds, 3) }
        }
        restore                    = [ordered]@{
            point_duration_seconds = if ($null -eq $pointRestoreSeconds) { $null } else { [math]::Round($pointRestoreSeconds, 3) }
            full_duration_seconds  = if ($null -eq $fullRestoreSeconds) { $null } else { [math]::Round($fullRestoreSeconds, 3) }
        }
        recovery                   = [ordered]@{
            rows_seeded_before_backup = $seedRows
            rows_written_after_backup = $afterBackupRows
            rows_recovered            = $probeRowsRecovered
            rows_lost_inside_rpo      = $probeRowsLostAfterBackup
            rpo_window_seconds        = if ($null -eq $rpoWindowSeconds) { $null } else { [math]::Round($rpoWindowSeconds, 3) }
            rto_seconds               = if ($null -eq $pointRestoreSeconds -or $null -eq $fullRestoreSeconds) { $null } else { [math]::Round($pointRestoreSeconds + $fullRestoreSeconds, 3) }
        }
        target_comparison          = $targetComparisons
        verification_failure_reasons = @($verificationReasons)
        steps                      = @($steps)
        failure_reasons            = @($failureReasons)
    }

    Write-DrillJsonFile -Path $EvidencePath -Payload $evidence

    if ($status -eq 'passed') {
        Write-Output "Restore drill passed: $EvidencePath"
        exit 0
    }
    [Console]::Error.WriteLine("Restore drill failed: $($failureReasons -join '; ')")
    exit 2
}

# ---------------------------------------------------------------- 入口与守卫 ----

function Resolve-DrillEvidencePath {
    $scriptDirectory = Get-DrillScriptDirectory
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $scriptDirectory '..'))
    $repoRoot = [System.IO.Path]::GetFullPath((Join-Path $repoRoot '..'))
    return (Join-Path (Split-Path -Parent $repoRoot) 'docs\enterprise-platform\evidence\production\restore-drill.json')
}

if ($Command -eq 'selftest') {
    $liveBefore = [ordered]@{
        database    = 'zhicloud_selftest'
        probe_table = 'zc_drill_probe'
        row_counts  = [ordered]@{ erp_customer = [int64]12; erp_sale_order = [int64]7 }
        checksums   = [ordered]@{ erp_customer = '918273'; erp_sale_order = '64512' }
        flyway      = [ordered]@{ history_rows = 96; max_version = '89' }
    }
    $liveIdentical = [ordered]@{
        database    = 'zhicloud_selftest'
        probe_table = 'zc_drill_probe'
        row_counts  = [ordered]@{ erp_customer = [int64]12; erp_sale_order = [int64]7 }
        checksums   = [ordered]@{ erp_customer = '918273'; erp_sale_order = '64512' }
        flyway      = [ordered]@{ history_rows = 96; max_version = '89' }
    }
    $liveDrifted = [ordered]@{
        database    = 'zhicloud_selftest'
        probe_table = 'zc_drill_probe'
        row_counts  = [ordered]@{ erp_customer = [int64]11; erp_sale_order = [int64]7 }
        checksums   = [ordered]@{ erp_customer = '918273'; erp_sale_order = '64512' }
        flyway      = [ordered]@{ history_rows = 96; max_version = '89' }
    }

    $identicalReasons = @(Compare-Snapshots -Before $liveBefore -After $liveIdentical)
    $driftReasons = @(Compare-Snapshots -Before $liveBefore -After $liveDrifted)
    $tableCount = @(Get-DrillMapEntries -Map $liveBefore.row_counts).Count
    $keySamples = @((ConvertTo-DrillChecksumKey -Name 'zhicloud_prod.erp_customer'), (ConvertTo-DrillChecksumKey -Name 'erp_customer'))
    $keysNormalized = (($keySamples[0] -eq 'erp_customer') -and ($keySamples[1] -eq 'erp_customer'))
    $passed = (($identicalReasons.Count -eq 0) -and ($driftReasons.Count -gt 0) -and ($tableCount -eq 2) -and $keysNormalized)

    $payload = [ordered]@{
        command           = 'selftest'
        live_dictionary   = $true
        identical_reasons = @($identicalReasons)
        drift_reasons     = @($driftReasons)
        table_count       = $tableCount
        checksum_keys_normalized = $keysNormalized
        passed            = $passed
    }
    Write-Output (ConvertTo-DrillJson -Payload $payload)
    if (-not $passed) {
        [Console]::Error.WriteLine("selftest failed: identical=$($identicalReasons.Count) drift=$($driftReasons.Count) tables=$tableCount")
        exit 2
    }
    exit 0
}

if ($Command -eq 'verify') {
    if ([string]::IsNullOrWhiteSpace($BeforeSnapshot)) {
        [Console]::Error.WriteLine('usage: backup-restore-drill.ps1 verify -BeforeSnapshot <json> -AfterSnapshot <json>')
        exit 1
    }
    if ([string]::IsNullOrWhiteSpace($AfterSnapshot)) {
        [Console]::Error.WriteLine('usage: backup-restore-drill.ps1 verify -BeforeSnapshot <json> -AfterSnapshot <json>')
        exit 1
    }
    if (-not (Test-Path -LiteralPath $BeforeSnapshot -PathType Leaf)) {
        [Console]::Error.WriteLine("before snapshot not found: $BeforeSnapshot")
        exit 1
    }
    if (-not (Test-Path -LiteralPath $AfterSnapshot -PathType Leaf)) {
        [Console]::Error.WriteLine("after snapshot not found: $AfterSnapshot")
        exit 1
    }
    Invoke-VerifyCommand -Before $BeforeSnapshot -After $AfterSnapshot
}

if ([string]::IsNullOrWhiteSpace($SandboxState)) {
    [Console]::Error.WriteLine('usage: backup-restore-drill.ps1 drill -SandboxState <state.json> [-TargetDatabase <name>] [-EvidenceFile <path>]')
    exit 1
}
if (-not (Test-Path -LiteralPath $SandboxState -PathType Leaf)) {
    [Console]::Error.WriteLine("sandbox state file not found: $SandboxState")
    exit 1
}

$state = Read-DrillJsonFile -Path $SandboxState
if ($null -eq $state.container_name -or -not ([string]$state.container_name -match '^zhicloud-mysql-sandbox-[0-9a-f]{8}$')) {
    [Console]::Error.WriteLine("refusing to run: state container_name must look like zhicloud-mysql-sandbox-<hex8>, got '$($state.container_name)'")
    exit 1
}
$stateHost = [string]$state.host
if ([string]::IsNullOrWhiteSpace($stateHost)) { $stateHost = '127.0.0.1' }
if ($stateHost -notin @('localhost', '127.0.0.1')) {
    [Console]::Error.WriteLine("refusing to run: sandbox host must be localhost or 127.0.0.1, got '$stateHost'")
    exit 1
}

$sourceDb = $SourceDatabase
if ([string]::IsNullOrWhiteSpace($sourceDb)) { $sourceDb = [string]$state.database }
if ([string]::IsNullOrWhiteSpace($sourceDb)) {
    [Console]::Error.WriteLine('unable to resolve source database from state; pass -SourceDatabase')
    exit 1
}

$targetDb = $TargetDatabase
if ([string]::IsNullOrWhiteSpace($targetDb)) {
    $targetDb = 'zhicloud_drill_' + [guid]::NewGuid().ToString('n').Substring(0, 8)
}
if ($targetDb -eq $sourceDb) {
    [Console]::Error.WriteLine("refusing to run: target database must differ from source database ($sourceDb)")
    exit 1
}
if (-not ($targetDb -match '^zhicloud_(drill|sandbox)_')) {
    [Console]::Error.WriteLine("refusing to run: target database must be drill scoped, expected prefix zhicloud_drill_ or zhicloud_sandbox_, got '$targetDb'")
    exit 1
}

$evidencePath = $EvidenceFile
if ([string]::IsNullOrWhiteSpace($evidencePath)) {
    $evidencePath = Resolve-DrillEvidencePath
}
$evidencePath = [System.IO.Path]::GetFullPath($evidencePath)

Invoke-DrillCommand -State $state -SourceDb $sourceDb -TargetDb $targetDb -ProbeTableName $ProbeTable -EvidencePath $evidencePath -DryRunOnly:$DryRun -NoCleanup:$SkipCleanup