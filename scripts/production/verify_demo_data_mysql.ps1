#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$MysqlClient = 'C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe',
    [string]$DatabaseHost = '127.0.0.1',
    [int]$DatabasePort = 53306,
    [string]$AdminUser = 'root',
    [string]$AdminPasswordEnv,
    [string]$EvidenceDir,
    [int]$TimeoutSeconds = 900
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
# 硬止损：PowerShell 的 `&` 原生命令调用没有原生超时，所以 $TimeoutSeconds 此前只停留在参数定义处，
# 导入或迁移挂起时会无限等待（登记为 R-G4-2）。所有原生调用必须经过共享执行器，超时即杀进程树。
# 实现只有一份（SandboxNative.ps1），本脚本不再自带副本：同一规则的两份实现会漂移。
$script:NativeTimeoutSeconds = if ($TimeoutSeconds -gt 0) { $TimeoutSeconds } else { 900 }
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

$Stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$DbName = 'zhicloud_demo_' + $Stamp
$DbUser = 'zdemo_' + $Stamp.Substring($Stamp.Length - 6)
$DbPassword = 'Zc!' + [guid]::NewGuid().ToString('N') + '9x'
$LogBase = 'demo-data-mysql-' + $Stamp
$MigrationLog = Join-Path $EvidenceDir ($LogBase + '-migration.log')
$MasterLog = Join-Path $EvidenceDir ($LogBase + '-master-import.log')
$BusinessLog = Join-Path $EvidenceDir ($LogBase + '-business-import.log')
$CleanupLog = Join-Path $EvidenceDir ($LogBase + '-cleanup.log')
$EvidenceJson = Join-Path $EvidenceDir ($LogBase + '.json')

$JarPath = Join-Path $RepoRoot 'zhicloud-server\target\zhicloud-server.jar'
$MasterSql = Join-Path $RepoRoot 'demo-data\sql\mysql\demo_master_data.sql'
$BusinessSql = Join-Path $RepoRoot 'demo-data\sql\mysql\demo_business_documents.sql'
$CleanupSql = Join-Path $RepoRoot 'demo-data\cleanup\cleanup_demo.sql'
$MasterManifest = Join-Path $RepoRoot 'demo-data\schema\demo_master_data_summary.json'
$BusinessManifest = Join-Path $RepoRoot 'demo-data\schema\demo_business_documents_summary.json'

foreach ($path in @($JarPath, $MasterSql, $BusinessSql, $CleanupSql, $MasterManifest, $BusinessManifest, $MysqlClient)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "文件不存在: $path" }
}

$AdminPassword = ''
if (-not [string]::IsNullOrWhiteSpace($AdminPasswordEnv)) {
    $AdminPassword = [Environment]::GetEnvironmentVariable($AdminPasswordEnv, 'Process')
    if ([string]::IsNullOrEmpty($AdminPassword)) { throw "环境变量 $AdminPasswordEnv 不存在或为空" }
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try { $bytes = $sha.ComputeHash($stream) } finally { $stream.Dispose() }
    } finally { $sha.Dispose() }
    return [System.BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
}

function Invoke-MysqlText {
    param([Parameter(Mandatory = $true)][string]$Sql, [string]$Database, [string]$Password = $AdminPassword, [string]$User = $AdminUser)
    $previousPassword = [Environment]::GetEnvironmentVariable('MYSQL_PWD', 'Process')
    try {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $Password, 'Process')
        $arguments = @(
            '--protocol=TCP', "--host=$DatabaseHost", "--port=$DatabasePort", "--user=$User",
            '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4', '--connect-timeout=10'
        )
        if (-not [string]::IsNullOrWhiteSpace($Database)) { $arguments += $Database }
        $run = Invoke-BoundedNative -FilePath $MysqlClient -Arguments $arguments -StandardInput $Sql -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'mysql-query'
        if ($run.ExitCode -ne 0) {
            throw "mysql 退出码 $($run.ExitCode)：$($run.StdOut) $($run.StdErr)".TrimEnd()
        }
        return $run.StdOut
    } finally {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $previousPassword, 'Process')
    }
}

function Invoke-MysqlFile {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Database, [Parameter(Mandatory = $true)][string]$Password, [string]$User = $AdminUser)
    $sql = [System.IO.File]::ReadAllText($Path, [System.Text.UTF8Encoding]::new($false))
    return Invoke-MysqlText -Sql $sql -Database $Database -Password $Password -User $User
}

function Invoke-JavaMigration {
    param([Parameter(Mandatory = $true)][string]$Url)
    $envNames = @('MASTER_DATASOURCE_URL','MASTER_DATASOURCE_USERNAME','MASTER_DATASOURCE_PASSWORD','MIGRATION_BASELINE_MODE')
    $saved = @{}
    foreach ($name in $envNames) { $saved[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
    try {
        $env:MASTER_DATASOURCE_URL = $Url
        $env:MASTER_DATASOURCE_USERNAME = $DbUser
        $env:MASTER_DATASOURCE_PASSWORD = $DbPassword
        $env:MIGRATION_BASELINE_MODE = 'clean'
        $javaArgs = @(
            '-Dfile.encoding=UTF-8',
            '-Dloader.main=cn.zhicloud.server.DatabaseMigrationRunner',
            '-cp', $JarPath,
            'org.springframework.boot.loader.launch.PropertiesLauncher'
        )
        $run = Invoke-BoundedNative -FilePath 'java' -Arguments $javaArgs -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'migration'
        # 迁移日志必须先落盘再判定，否则超时/失败时只剩一句退出码。
        [System.IO.File]::WriteAllText($MigrationLog, ($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim(), (New-Object System.Text.UTF8Encoding($false)))
        if ($run.ExitCode -ne 0) { throw "DatabaseMigrationRunner 退出码 $($run.ExitCode)，见 $MigrationLog" }
    } finally {
        foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $saved[$name], 'Process') }
    }
}

function Read-CountMap {
    param([Parameter(Mandatory = $true)][string]$Database, [Parameter(Mandatory = $true)][string]$Password, [string]$User = $AdminUser, [switch]$Remaining)
    $master = Get-Content -LiteralPath $MasterManifest -Raw -Encoding UTF8 | ConvertFrom-Json
    $business = Get-Content -LiteralPath $BusinessManifest -Raw -Encoding UTF8 | ConvertFrom-Json
    $tables = @($master.tables) + @($business.tables)
    $result = [ordered]@{}
    $mismatches = @()
    foreach ($entry in $tables) {
        $where = if ($Remaining) { "tenant_id = 9001" } else { "tenant_id = 9001 AND creator = 'demo-seed'" }
        $actual = [int](Invoke-MysqlText -Sql "SELECT COUNT(*) FROM $($entry.table) WHERE $where;" -Database $Database -Password $Password -User $User)
        $expected = if ($Remaining) { 0 } else { [int]$entry.rows }
        $result[$entry.table] = $actual
        if ($actual -ne $expected) {
            $mismatches += [ordered]@{ table = $entry.table; expected = $expected; actual = $actual }
        }
    }
    return [ordered]@{ tables = $tables.Count; values = $result; mismatches = $mismatches }
}

$result = [ordered]@{
    generated_at = (Get-Date).ToString('o')
    status = 'failed'
    scope = 'MySQL 8.4.9 fresh clean baseline + demo master/business import + row counts + cross-domain scenario queries + cleanup validation'
    production_ready = $false
    gate3_passed = $false
    r011_status = 'OPEN'
    r029_status = 'OPEN'
    database = $DbName
    database_user = $DbUser
    mysql = [ordered]@{ host = $DatabaseHost; port = $DatabasePort; client = $MysqlClient }
    artifacts = [ordered]@{
        jar_sha256 = Get-Sha256 -Path $JarPath
        master_sql_sha256 = Get-Sha256 -Path $MasterSql
        business_sql_sha256 = Get-Sha256 -Path $BusinessSql
        cleanup_sql_sha256 = Get-Sha256 -Path $CleanupSql
        master_manifest_sha256 = Get-Sha256 -Path $MasterManifest
        business_manifest_sha256 = Get-Sha256 -Path $BusinessManifest
    }
    row_counts = $null
    scenario_counts = $null
    cleanup_counts = $null
    required_checks = [ordered]@{}
    caveats = @(
        '本验证只证明静态 SQL 可在本机 MySQL 8.4.9 临时库导入、计数、关联查询和清理，不证明应用层业务流程、权限、并发、幂等或真实业务规则通过。',
        '六条场景为数据级跨域关联证明，不是 UI/API 端到端验收；未启动后端服务调用业务接口。',
        '当前演示数据仅允许导入开发、测试、演示和沙箱环境，禁止导入生产。'
    )
}

try {
    Invoke-MysqlText -Sql "DROP DATABASE IF EXISTS ``$DbName``; DROP USER IF EXISTS '$DbUser'@'%'; CREATE DATABASE ``$DbName`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER '$DbUser'@'%' IDENTIFIED BY '$DbPassword'; GRANT ALL PRIVILEGES ON ``$DbName``.* TO '$DbUser'@'%'; FLUSH PRIVILEGES;" | Out-Null
    # R-033 方向 2：宿主 Java 进程经 Docker 发布端口连入时来源地址是 bridge 网关 172.17.0.1，授权主机改为 %

    $url = "jdbc:mysql://${DatabaseHost}:${DatabasePort}/${DbName}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
    Invoke-JavaMigration -Url $url
    Invoke-MysqlFile -Path $MasterSql -Database $DbName -Password $DbPassword -User $DbUser | Out-Null
    Invoke-MysqlFile -Path $BusinessSql -Database $DbName -Password $DbPassword -User $DbUser | Out-Null

    $result.row_counts = Read-CountMap -Database $DbName -Password $DbPassword -User $DbUser
    $scenarioSql = @"
SELECT 'S1_lead_to_cash' AS scenario, COUNT(*) AS cnt FROM crm_clue c JOIN crm_business b ON b.customer_id=c.customer_id JOIN crm_contract ct ON ct.business_id=b.id AND ct.customer_id=b.customer_id JOIN crm_receivable r ON r.contract_id=ct.id AND r.customer_id=ct.customer_id JOIN crm_invoice inv ON inv.contract_id=ct.id JOIN crm_invoice_line il ON il.invoice_id=inv.id WHERE c.tenant_id=9001 AND c.transform_status=1 AND b.end_status=1 AND r.price>0
UNION ALL SELECT 'S1_erp_sale_to_out', COUNT(*) FROM erp_sale_order so JOIN erp_sale_order_items soi ON soi.master_id=so.id JOIN erp_sale_out soo ON soo.customer_id=so.customer_id JOIN erp_sale_out_items sooi ON sooi.master_id=soo.id WHERE so.tenant_id=9001
UNION ALL SELECT 'S2_purchase_to_qms', COUNT(*) FROM erp_purchase_order po JOIN erp_purchase_order_items poi ON poi.master_id=po.id JOIN erp_purchase_in pi ON pi.supplier_id=po.supplier_id JOIN erp_purchase_in_items pii ON pii.master_id=pi.id AND pii.product_id=poi.product_id JOIN qms_inspection_order q ON q.supplier_id=po.supplier_id AND q.product_id=poi.product_id AND q.biz_type='PURCHASE_IN' WHERE po.tenant_id=9001
UNION ALL SELECT 'S3_work_order_to_feedback', COUNT(*) FROM mes_pro_work_order wo JOIN mes_pro_task t ON t.work_order_id=wo.id JOIN mes_pro_feedback f ON f.work_order_id=wo.id AND f.task_id=t.id JOIN qms_inspection_order q ON q.work_order_id=wo.id WHERE wo.tenant_id=9001 AND wo.quantity>=wo.quantity_produced AND t.quantity>=t.produced_quantity
UNION ALL SELECT 'S4_order_to_tms', COUNT(*) FROM erp_sale_order so JOIN wms_shipment_order ws ON ws.biz_order_no=so.no JOIN wms_shipment_order_detail wsd ON wsd.order_id=ws.id JOIN wms_wave_order_detail wvd ON wvd.shipment_order_id=ws.id JOIN wms_wave_order wo ON wo.id=wvd.wave_order_id JOIN wms_pick_task pt ON pt.shipment_order_id=ws.id AND pt.wave_order_id=wo.id JOIN tms_shipment ts ON ts.source_order_no=so.no WHERE so.tenant_id=9001
UNION ALL SELECT 'S5_complaint_to_ncr_capa_return', COUNT(*) FROM qms_customer_complaint cc JOIN qms_ncr_document ncr ON ncr.product_id=cc.product_id JOIN qms_capa_document capa ON capa.status IN (10,20,30,40) JOIN erp_sale_return_items sri ON sri.product_id=cc.product_id JOIN erp_sale_return sr ON sr.id=sri.master_id WHERE cc.tenant_id=9001
UNION ALL SELECT 'S6_hire_to_retire', COUNT(*) FROM hr_employee e JOIN hr_contract hc ON hc.employee_id=e.id JOIN hr_attendance ha ON ha.employee_id=e.id JOIN hr_salary hs ON hs.employee_id=e.id JOIN hr_performance hp ON hp.employee_id=e.id JOIN hr_leave_request lr ON lr.employee_id=e.id WHERE e.tenant_id=9001;
"@
    $scenarioOutput = Invoke-MysqlText -Sql $scenarioSql -Database $DbName -Password $DbPassword -User $DbUser
    $scenarioCounts = [ordered]@{}
    foreach ($line in ($scenarioOutput -split "`r?`n" | Where-Object { $_.Trim().Length -gt 0 })) {
        $parts = $line -split "`t"
        if ($parts.Count -ge 2) { $scenarioCounts[$parts[0]] = [int]$parts[1] }
    }
    $result.scenario_counts = $scenarioCounts

    $cleanupOutput = Invoke-MysqlFile -Path $CleanupSql -Database $DbName -Password $DbPassword -User $DbUser
    [System.IO.File]::WriteAllText($CleanupLog, $cleanupOutput, [System.Text.UTF8Encoding]::new($false))
    $result.cleanup_counts = Read-CountMap -Database $DbName -Password $DbPassword -User $DbUser -Remaining

    $result.required_checks = [ordered]@{
        row_count_tables_118 = ($result.row_counts.tables -eq 118)
        row_count_mismatches_zero = ($result.row_counts.mismatches.Count -eq 0)
        scenario_s1_positive = ([int]$scenarioCounts['S1_lead_to_cash'] -gt 0 -and [int]$scenarioCounts['S1_erp_sale_to_out'] -gt 0)
        scenario_s2_positive = ([int]$scenarioCounts['S2_purchase_to_qms'] -gt 0)
        scenario_s3_positive = ([int]$scenarioCounts['S3_work_order_to_feedback'] -gt 0)
        scenario_s4_positive = ([int]$scenarioCounts['S4_order_to_tms'] -gt 0)
        scenario_s5_positive = ([int]$scenarioCounts['S5_complaint_to_ncr_capa_return'] -gt 0)
        scenario_s6_positive = ([int]$scenarioCounts['S6_hire_to_retire'] -gt 0)
        cleanup_tables_118 = ($result.cleanup_counts.tables -eq 118)
        cleanup_mismatches_zero = ($result.cleanup_counts.mismatches.Count -eq 0)
    }
    $result.status = if (($result.required_checks.Values | Where-Object { -not $_ }).Count -eq 0) { 'passed' } else { 'failed' }
} catch {
    $result.error = $_.Exception.Message
} finally {
    try {
        Invoke-MysqlText -Sql "DROP DATABASE IF EXISTS ``$DbName``; DROP USER IF EXISTS '$DbUser'@'127.0.0.1';" | Out-Null
    } catch { }
    [System.IO.File]::WriteAllText($EvidenceJson, ($result | ConvertTo-Json -Depth 10), [System.Text.UTF8Encoding]::new($false))
}

Write-Output ('evidence=' + $EvidenceJson)
Write-Output ('status=' + $result.status)
if ($result.status -ne 'passed') { exit 1 }
