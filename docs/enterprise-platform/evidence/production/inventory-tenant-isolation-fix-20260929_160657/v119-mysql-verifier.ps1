[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$StateFile,
    [Parameter(Mandatory = $true)][string]$EvidenceDir,
    [Parameter(Mandatory = $true)][string]$RepoRoot
)

$ErrorActionPreference = 'Stop'

function Write-Utf8NoBom {
    param([Parameter(Mandatory = $true)][string]$Path, [AllowEmptyString()][string]$Text)
    $directory = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($directory)) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    [System.IO.File]::WriteAllText($Path, $Text, [System.Text.UTF8Encoding]::new($false))
}

function Write-JsonNoBom {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)]$Payload)
    Write-Utf8NoBom -Path $Path -Text (($Payload | ConvertTo-Json -Depth 12) + "`n")
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-MysqlErrorCode {
    param([string]$Text)
    if ($Text -match 'ERROR\s+(\d{4})') { return [int]$Matches[1] }
    return $null
}

function Invoke-MysqlSql {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [AllowEmptyString()][string]$Sql,
        [switch]$AllowFailure
    )

    $args = @(
        'exec', '-i',
        '-e', "MYSQL_PWD=$($state.root_password)",
        $state.container_name,
        'mysql',
        '--protocol=TCP',
        '--host=127.0.0.1',
        '--port=3306',
        '-uroot',
        '--default-character-set=utf8mb4',
        '--batch',
        '--raw',
        '--skip-column-names',
        $state.database
    )

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = 'docker'
    $psi.UseShellExecute = $false
    $psi.RedirectStandardInput = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    foreach ($arg in $args) { [void]$psi.ArgumentList.Add($arg) }

    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $psi
    [void]$process.Start()
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    $exitCode = $process.ExitCode

    $safeCommand = 'docker exec -i -e MYSQL_PWD=<redacted> ' + $state.container_name + ' mysql --protocol=TCP --host=127.0.0.1 --port=3306 -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names ' + $state.database
    Write-Utf8NoBom -Path (Join-Path $EvidenceDir "v119-$Label.command.txt") -Text ($safeCommand + "`n")
    Write-Utf8NoBom -Path (Join-Path $EvidenceDir "v119-$Label.stdout.log") -Text $stdout
    Write-Utf8NoBom -Path (Join-Path $EvidenceDir "v119-$Label.stderr.log") -Text $stderr

    $record = [ordered]@{
        label             = $Label
        exit_code         = $exitCode
        mysql_error_code  = Get-MysqlErrorCode ($stdout + "`n" + $stderr)
        stdout            = $stdout
        stderr            = $stderr
    }
    Write-JsonNoBom -Path (Join-Path $EvidenceDir "v119-$Label.result.json") -Payload $record

    if (-not $AllowFailure -and $exitCode -ne 0) {
        throw "SQL step '$Label' failed with exit code $exitCode. stderr: $($stderr.Trim())"
    }
    return [pscustomobject]$record
}

function Invoke-MysqlFile {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Path,
        [switch]$AllowFailure
    )
    $sql = [System.IO.File]::ReadAllText($Path)
    return Invoke-MysqlSql -Label $Label -Sql $sql -AllowFailure:$AllowFailure
}

function Require-Success {
    param([Parameter(Mandatory = $true)]$Result)
    if ($Result.exit_code -ne 0) {
        throw "expected success for '$($Result.label)', got exit $($Result.exit_code): $($Result.stderr.Trim())"
    }
}

function Require-Failure {
    param([Parameter(Mandatory = $true)]$Result, [Parameter(Mandatory = $true)][string]$Pattern)
    if ($Result.exit_code -eq 0) {
        throw "expected failure for '$($Result.label)', but exit code was 0"
    }
    $text = $Result.stdout + "`n" + $Result.stderr
    if ($text -notmatch $Pattern) {
        throw "expected failure '$($Result.label)' to match '$Pattern', got: $($text.Trim())"
    }
}

function Get-Scalar {
    param([Parameter(Mandatory = $true)][string]$Label, [Parameter(Mandatory = $true)][string]$Sql)
    $result = Invoke-MysqlSql -Label $Label -Sql $Sql
    Require-Success $result
    return (($result.stdout -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1).Trim()
}

function Reset-Inventory {
    $sql = @"
DROP TABLE IF EXISTS ``inventory_item``;
DROP PROCEDURE IF EXISTS p_v119_assert_table;
DROP PROCEDURE IF EXISTS p_v119_assert_column;
DROP PROCEDURE IF EXISTS p_v119_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v119_drop_legacy_index_if_exists;
DROP PROCEDURE IF EXISTS p_v119_assert_no_global_unique_index;
"@
    $result = Invoke-MysqlSql -Label 'reset-inventory' -Sql $sql
    Require-Success $result
}

function Get-VerifyChecks {
    param([Parameter(Mandatory = $true)]$Result)
    $checks = [ordered]@{}
    foreach ($line in ($Result.stdout -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $parts = $line -split "`t", 2
        if ($parts.Count -eq 2 -and $parts[0] -like 'V119_*') {
            $checks[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $checks
}

function Require-VerifyPass {
    param([Parameter(Mandatory = $true)]$Result, [Parameter(Mandatory = $true)][string[]]$Expected)
    Require-Success $Result
    $checks = Get-VerifyChecks $Result
    $missing = @($Expected | Where-Object { -not $checks.Contains($_) })
    $failed = @($Expected | Where-Object { $checks.Contains($_) -and $checks[$_] -ne 'PASS' })
    if ($missing.Count -gt 0 -or $failed.Count -gt 0 -or $checks.Count -ne $Expected.Count) {
        throw "verification checks were not all PASS. checks=$($checks | ConvertTo-Json -Compress); missing=$($missing -join ','); failed=$($failed -join ',')"
    }
    return $checks
}

function Add-Scenario {
    param([Parameter(Mandatory = $true)][string]$Name, [Parameter(Mandatory = $true)][bool]$Passed, [Parameter(Mandatory = $true)]$Details)
    return [ordered]@{
        name    = $Name
        passed  = $Passed
        details = $Details
    }
}

$statePath = [System.IO.Path]::GetFullPath($StateFile)
$evidenceDir = [System.IO.Path]::GetFullPath($EvidenceDir)
$repoRoot = [System.IO.Path]::GetFullPath($RepoRoot)
if (-not (Test-Path -LiteralPath $statePath -PathType Leaf)) { throw "state file not found: $statePath" }
if (-not (Test-Path -LiteralPath $evidenceDir -PathType Container)) { throw "evidence directory not found: $evidenceDir" }
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
if ($state.container_name -notmatch '^zhicloud-mysql-sandbox-[0-9a-f]{8}$') { throw "unexpected sandbox container: $($state.container_name)" }
if ($state.database -notmatch '^zhicloud_sandbox[A-Za-z0-9_]*$') { throw "unexpected sandbox database: $($state.database)" }
if ($state.image -notmatch '@sha256:[0-9a-f]{64}$') { throw "sandbox image is not digest pinned: $($state.image)" }

$v81 = Join-Path $repoRoot 'zhicloud-server/src/main/resources/db/migration/V81__inventory_item.sql'
$v119 = Join-Path $repoRoot 'zhicloud-server/src/main/resources/db/migration/V119__inventory_item_tenant_unique.sql'
$v119Verify = Join-Path $repoRoot 'zhicloud-server/src/main/resources/db/verification/V119_verify.sql'
foreach ($path in @($v81, $v119, $v119Verify)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "required SQL file missing: $path" }
}

$inspect = & docker inspect --format '{{.State.Running}}|{{.Config.Image}}|{{.Id}}' $state.container_name 2>&1
if ($LASTEXITCODE -ne 0) { throw "docker inspect failed for $($state.container_name): $($inspect | Out-String)" }
$inspectText = ($inspect | Out-String).Trim()
Write-Utf8NoBom -Path (Join-Path $evidenceDir 'v119-docker-inspect.txt') -Text ($inspectText + "`n")

$expectedChecks = @(
    'V119_INVENTORY_ITEM_TENANT_COLUMNS',
    'V119_INVENTORY_ITEM_TENANT_UNIQUE_INDEX',
    'V119_INVENTORY_ITEM_LEGACY_INDEX_REMOVED',
    'V119_INVENTORY_ITEM_TENANT_UNIQUE_DATA'
)

$scenarios = New-Object System.Collections.ArrayList
$fatalError = $null
try {
    $server = Invoke-MysqlSql -Label 'server-metrics' -Sql "SELECT VERSION(), @@version_comment, @@character_set_server, @@collation_server, @@sql_mode, @@session.time_zone, @@global.time_zone, DATABASE();"
    Require-Success $server
    $serverFields = (($server.stdout -split "`r?`n") | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -First 1) -split "`t"
    if ($serverFields.Count -lt 8) { throw "unexpected server metrics output: $($server.stdout)" }
    $serverRecord = [ordered]@{
        version        = $serverFields[0]
        version_comment= $serverFields[1]
        charset_server = $serverFields[2]
        collation_server = $serverFields[3]
        sql_mode       = $serverFields[4]
        time_zone      = $serverFields[5]
        global_time_zone = $serverFields[6]
        database       = $serverFields[7]
    }

    # Happy path: clean V81 -> V119 -> verification -> idempotent V119 -> tenant separation.
    try {
        Reset-Inventory
        $v81Result = Invoke-MysqlFile -Label 'happy-v81' -Path $v81
        Require-Success $v81Result
        $v119Result = Invoke-MysqlFile -Label 'happy-v119-first' -Path $v119
        Require-Success $v119Result
        $verifyResult = Invoke-MysqlFile -Label 'happy-v119-verify-first' -Path $v119Verify
        $verifyChecks = Require-VerifyPass -Result $verifyResult -Expected $expectedChecks
        $repeatResult = Invoke-MysqlFile -Label 'happy-v119-repeat' -Path $v119
        Require-Success $repeatResult
        $repeatVerify = Invoke-MysqlFile -Label 'happy-v119-verify-repeat' -Path $v119Verify
        $repeatChecks = Require-VerifyPass -Result $repeatVerify -Expected $expectedChecks
        $tenantInsert = Invoke-MysqlSql -Label 'happy-tenant-separation-insert' -Sql "INSERT INTO inventory_item (tenant_id,item_id,warehouse_id,location_id,area_id,batch_id,quantity) VALUES (1,1001,10,0,0,0,1.00),(2,1001,10,0,0,0,1.00);"
        Require-Success $tenantInsert
        $sameTenantDuplicate = Invoke-MysqlSql -Label 'happy-same-tenant-duplicate' -Sql "INSERT INTO inventory_item (tenant_id,item_id,warehouse_id,location_id,area_id,batch_id,quantity) VALUES (1,1001,10,0,0,0,1.00);" -AllowFailure
        Require-Failure -Result $sameTenantDuplicate -Pattern '1062'
        [void]$scenarios.Add((Add-Scenario -Name 'happy_path_and_tenant_separation' -Passed $true -Details ([ordered]@{
            v81_exit = $v81Result.exit_code
            v119_first_exit = $v119Result.exit_code
            verification = $verifyChecks
            v119_repeat_exit = $repeatResult.exit_code
            repeat_verification = $repeatChecks
            tenant_separation_insert_exit = $tenantInsert.exit_code
            same_tenant_duplicate_exit = $sameTenantDuplicate.exit_code
            same_tenant_duplicate_mysql_error = $sameTenantDuplicate.mysql_error_code
        })))
    } catch {
        [void]$scenarios.Add((Add-Scenario -Name 'happy_path_and_tenant_separation' -Passed $false -Details ([ordered]@{ error = $_.Exception.Message })))
    }

    # Duplicate pre-state: the legacy index is deliberately removed to induce duplicate rows.
    try {
        Reset-Inventory
        $v81Result = Invoke-MysqlFile -Label 'duplicate-v81' -Path $v81
        Require-Success $v81Result
        $dropLegacy = Invoke-MysqlSql -Label 'duplicate-drop-legacy' -Sql 'ALTER TABLE `inventory_item` DROP INDEX `uk_item_warehouse_location_area_batch`;'
        Require-Success $dropLegacy
        $seed = Invoke-MysqlSql -Label 'duplicate-seed' -Sql "INSERT INTO inventory_item (tenant_id,item_id,warehouse_id,location_id,area_id,batch_id,quantity) VALUES (1,2001,20,0,0,0,1.00),(1,2001,20,0,0,0,2.00);"
        Require-Success $seed
        $blocked = Invoke-MysqlFile -Label 'duplicate-v119-blocked' -Path $v119 -AllowFailure
        Require-Failure -Result $blocked -Pattern 'V119 duplicate rows block unique index'
        $newIndexCount = Get-Scalar -Label 'duplicate-new-index-count' -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inventory_item' AND index_name='uk_inventory_item_tenant_composite';"
        if ($newIndexCount -ne '0') { throw "duplicate pre-state unexpectedly created new index: $newIndexCount" }
        [void]$scenarios.Add((Add-Scenario -Name 'duplicate_rows_fail_closed' -Passed $true -Details ([ordered]@{
            v81_exit = $v81Result.exit_code
            induced_legacy_index_removed = $true
            seed_exit = $seed.exit_code
            v119_exit = $blocked.exit_code
            mysql_error_code = $blocked.mysql_error_code
            new_index_count_after_failure = $newIndexCount
        })))
    } catch {
        [void]$scenarios.Add((Add-Scenario -Name 'duplicate_rows_fail_closed' -Passed $false -Details ([ordered]@{ error = $_.Exception.Message })))
    }

    # Wrong new index definition must be rejected without dropping the legacy index.
    try {
        Reset-Inventory
        $v81Result = Invoke-MysqlFile -Label 'wrong-new-v81' -Path $v81
        Require-Success $v81Result
        $wrongNew = Invoke-MysqlSql -Label 'wrong-new-create' -Sql 'ALTER TABLE `inventory_item` ADD UNIQUE KEY `uk_inventory_item_tenant_composite` (`tenant_id`,`item_id`,`warehouse_id`,`location_id`,`area_id`);'
        Require-Success $wrongNew
        $blocked = Invoke-MysqlFile -Label 'wrong-new-v119-blocked' -Path $v119 -AllowFailure
        Require-Failure -Result $blocked -Pattern 'V119 index mismatch'
        $legacyCount = Get-Scalar -Label 'wrong-new-legacy-count' -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inventory_item' AND index_name='uk_item_warehouse_location_area_batch';"
        $wrongCount = Get-Scalar -Label 'wrong-new-column-count' -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inventory_item' AND index_name='uk_inventory_item_tenant_composite';"
        if ($legacyCount -ne '5') { throw "wrong new index caused legacy index count $legacyCount instead of 5" }
        if ($wrongCount -ne '5') { throw "wrong new index column count $wrongCount instead of 5" }
        [void]$scenarios.Add((Add-Scenario -Name 'wrong_new_index_fail_closed' -Passed $true -Details ([ordered]@{
            v81_exit = $v81Result.exit_code
            wrong_new_index_exit = $wrongNew.exit_code
            v119_exit = $blocked.exit_code
            mysql_error_code = $blocked.mysql_error_code
            legacy_index_column_count = $legacyCount
            wrong_new_index_column_count = $wrongCount
        })))
    } catch {
        [void]$scenarios.Add((Add-Scenario -Name 'wrong_new_index_fail_closed' -Passed $false -Details ([ordered]@{ error = $_.Exception.Message })))
    }

    # Wrong legacy index definition must be rejected after the correct new index is present.
    try {
        Reset-Inventory
        $v81Result = Invoke-MysqlFile -Label 'wrong-legacy-v81' -Path $v81
        Require-Success $v81Result
        $correctNew = Invoke-MysqlSql -Label 'wrong-legacy-create-correct-new' -Sql 'ALTER TABLE `inventory_item` ADD UNIQUE KEY `uk_inventory_item_tenant_composite` (`tenant_id`,`item_id`,`warehouse_id`,`location_id`,`area_id`,`batch_id`);'
        Require-Success $correctNew
        $dropLegacy = Invoke-MysqlSql -Label 'wrong-legacy-drop-correct-legacy' -Sql 'ALTER TABLE `inventory_item` DROP INDEX `uk_item_warehouse_location_area_batch`;'
        Require-Success $dropLegacy
        $wrongLegacy = Invoke-MysqlSql -Label 'wrong-legacy-create-wrong' -Sql 'ALTER TABLE `inventory_item` ADD UNIQUE KEY `uk_item_warehouse_location_area_batch` (`item_id`,`warehouse_id`,`location_id`,`area_id`);'
        Require-Success $wrongLegacy
        $blocked = Invoke-MysqlFile -Label 'wrong-legacy-v119-blocked' -Path $v119 -AllowFailure
        Require-Failure -Result $blocked -Pattern 'V119 legacy index mismatch'
        $newCount = Get-Scalar -Label 'wrong-legacy-new-column-count' -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inventory_item' AND index_name='uk_inventory_item_tenant_composite';"
        $wrongLegacyCount = Get-Scalar -Label 'wrong-legacy-wrong-column-count' -Sql "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='inventory_item' AND index_name='uk_item_warehouse_location_area_batch';"
        if ($newCount -ne '6') { throw "correct new index column count $newCount instead of 6" }
        if ($wrongLegacyCount -ne '4') { throw "wrong legacy index column count $wrongLegacyCount instead of 4" }
        [void]$scenarios.Add((Add-Scenario -Name 'wrong_legacy_index_fail_closed' -Passed $true -Details ([ordered]@{
            v81_exit = $v81Result.exit_code
            correct_new_index_exit = $correctNew.exit_code
            wrong_legacy_index_exit = $wrongLegacy.exit_code
            v119_exit = $blocked.exit_code
            mysql_error_code = $blocked.mysql_error_code
            correct_new_index_column_count = $newCount
            wrong_legacy_index_column_count = $wrongLegacyCount
        })))
    } catch {
        [void]$scenarios.Add((Add-Scenario -Name 'wrong_legacy_index_fail_closed' -Passed $false -Details ([ordered]@{ error = $_.Exception.Message })))
    }
} catch {
    $fatalError = $_.Exception.Message
}

$failed = @($scenarios | Where-Object { -not $_.passed })
$status = if ($fatalError -or $failed.Count -gt 0) { 'failed' } else { 'passed' }
$payload = [ordered]@{
    status = $status
    generated_at = [DateTimeOffset]::UtcNow.ToString('o')
    scope = 'isolated MySQL 8.4 sandbox; synthetic schema/data only'
    sandbox = [ordered]@{
        container_name = $state.container_name
        volume = $state.volume
        database = $state.database
        host_port = $state.host_port
        image = $state.image
        docker_inspect = $inspectText
    }
    server = if ($serverRecord) { $serverRecord } else { $null }
    artifacts = [ordered]@{
        v81 = [ordered]@{ path = $v81; sha256 = Get-Sha256 $v81 }
        v119_migration = [ordered]@{ path = $v119; sha256 = Get-Sha256 $v119 }
        v119_verification = [ordered]@{ path = $v119Verify; sha256 = Get-Sha256 $v119Verify }
        verifier = [ordered]@{ path = $MyInvocation.MyCommand.Path; sha256 = Get-Sha256 $MyInvocation.MyCommand.Path }
    }
    scenarios = $scenarios.ToArray()
    summary = [ordered]@{
        total = $scenarios.Count
        passed = $scenarios.Count - $failed.Count
        failed = $failed.Count
    }
    fatal_error = $fatalError
    disclaimer = 'Isolated H2/MySQL sandbox evidence only. It does not prove production-equivalent MySQL topology, real tenant datasource routing, production-scale DDL/MDL/replication behavior, or independent review.'
}
$evidencePath = Join-Path $evidenceDir 'v119-mysql-evidence.json'
Write-JsonNoBom -Path $evidencePath -Payload $payload
Write-Utf8NoBom -Path (Join-Path $evidenceDir 'v119-mysql-evidence.json.sha256') -Text ((Get-Sha256 $evidencePath) + "  v119-mysql-evidence.json`n")
Write-Output ($payload | ConvertTo-Json -Depth 12)
if ($status -ne 'passed') { exit 1 }
