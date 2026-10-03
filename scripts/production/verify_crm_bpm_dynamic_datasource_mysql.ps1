#requires -Version 5.1
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$ContainerName = 'zhicloud-mysql-sandbox-87323300',
    [string]$SourceDatabase = 'zhicloud_crm_bpm_chain_test',
    [string]$MasterDatabase = 'zhicloud_crm_bpm_ds_master_test',
    [string]$SlaveDatabase = 'zhicloud_crm_bpm_ds_slave_test',
    [string]$MavenCommand = 'mvn',
    [string]$EvidenceDir,
    [int]$TimeoutSeconds = 900,
    # R-035（2026-10-01 授权后落地）：历史 45 张 Flowable 表 / 7 行 ACT_GE_PROPERTY 来自仓库外的
    # 人工预置库；当前流水线（真链路 IT 的 @Sql + Flowable 自动建表，且 app/idm/event-registry 关闭）
    # 实测只能产出 32/6。默认因此断言"可复现值"，要用人工预置库必须显式传 -ExpectedFlowableTables/-ExpectedPropertyRows
    # 并把 -SourceProvisioning 设为 manual —— 严格性不降，只是把"这份证据覆盖哪个库的来历"写进参数与证据。
    [int]$ExpectedFlowableTables = 32,
    [int]$ExpectedPropertyRows = 6,
    [ValidateSet('pipeline', 'manual')][string]$SourceProvisioning = 'pipeline',
    # R-040 方案 B（2026-10-02 责任人点选，见 handoff §12.60/§12.61）：并列新增宿主原生 mysqld 通道。
    # 沙箱形态的容器名与库名白名单一条不删；native 有它自己三条约束。
    [ValidateSet('sandbox', 'native')][string]$TargetKind = 'sandbox',
    [string]$NativeAdminClient = '',
    [int]$NativePort = 0,
    [ValidateSet('127.0.0.1', '::1', 'localhost')][string]$NativeHost = '127.0.0.1',
    [string]$NativePasswordEnv = '',
    [string]$NativeDataDirPrefix = ''
)

$ErrorActionPreference = 'Stop'
# fail-closed：本脚本会创建/删除容器、volume、network 和测试库，
# 因此只允许操作本机 Docker daemon。此前完全不校验 DOCKER_HOST 与 context（R-G3.5-1）：
# 一旦环境里配了远程 daemon，同一个"看起来无害"的调用就会在共享/生产主机上建删容器。
function Assert-LocalDockerDaemon {
    param([string[]]$AllowedContexts = @('default', 'desktop-linux', 'colima', 'desktop'))
    if (-not [string]::IsNullOrWhiteSpace($env:DOCKER_HOST)) {
        throw "检测到 DOCKER_HOST='$($env:DOCKER_HOST)'：拒绝在远程 Docker daemon 上创建/删除容器或卷。如确需远程执行，请走显式审批流程，不要放宽本守卫。"
    }
    # docker CLI 的任何 stderr 在脚本级 Stop 语义下都会被升级为终止性异常，
    # 一次无害警告就会让守卫报出"无法确定 context"的假失败，因此这里临时降级并在 finally 还原。
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $ctxText = ((& docker context show 2>&1) | Out-String).Trim()
        $ctxExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($ctxExit -ne 0) {
        throw "无法确定当前 Docker context（exit=$ctxExit）：$ctxText"
    }
    if ($AllowedContexts -notcontains $ctxText) {
        throw "当前 Docker context 为 '$ctxText'，不在允许集合 ($($AllowedContexts -join ', ')) 内：拒绝继续操作容器。"
    }
    Write-Output "Docker daemon guard: context=$ctxText"
}
if ($TargetKind -eq 'sandbox') {
    Assert-LocalDockerDaemon
} else {
    # R-040 方案 B：原生形态不使用 Docker，因此不跑 daemon 守卫（否则 daemon 不可用时这条不依赖 Docker 的路径会被无关守卫挡掉）。
    Write-Output 'target=native：跳过 Docker daemon 守卫（本形态不碰 docker）'
}

$ProgressPreference = 'SilentlyContinue'

# 唯一的原生进程执行器（硬超时 + 杀进程树）。此前 `& docker/mvn 2>&1` 完全不受 -TimeoutSeconds
# 约束：镜像卡住、mysql 连接挂起或 maven 死锁都会让门禁无限等待（登记的 R-G3.5-2 即此）。
$script:NativeTimeoutSeconds = if ($TimeoutSeconds -gt 0) { $TimeoutSeconds } else { 900 }
. (Join-Path $PSScriptRoot 'SandboxNative.ps1')

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
} else {
    $RepoRoot = (Resolve-Path -LiteralPath $RepoRoot).Path
}
if ([string]::IsNullOrWhiteSpace($EvidenceDir)) {
    $EvidenceDir = Join-Path $RepoRoot 'docs\enterprise-platform\evidence\production'
}
$EvidenceDir = [System.IO.Path]::GetFullPath($EvidenceDir)
New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null

if ($TargetKind -eq 'sandbox') {
    if ($ContainerName -notmatch '^zhicloud-mysql-sandbox-[0-9a-f]{8}$') {
        throw "拒绝非隔离沙箱容器: $ContainerName"
    }
} else {
    if ([string]::IsNullOrWhiteSpace($NativeAdminClient) -or -not (Test-Path -LiteralPath $NativeAdminClient -PathType Leaf)) {
        throw "native 形态要求 -NativeAdminClient 指向存在的 mysql.exe（当前 '$NativeAdminClient'）"
    }
    if ($NativePort -lt 1024 -or $NativePort -gt 65535) {
        throw "native 形态要求专用端口 1024-65535（当前 $NativePort）"
    }
    if ($NativePort -eq 3306) {
        throw 'native 形态拒绝 3306：本机默认端口可能属于其它项目的 mysqld（handoff §12.60），只连自己起的专用实例'
    }
    if ([string]::IsNullOrWhiteSpace($NativePasswordEnv)) {
        throw 'native 形态要求 -NativePasswordEnv 给出存放口令的环境变量名（口令不得进命令行）'
    }
    if ([string]::IsNullOrWhiteSpace($NativeDataDirPrefix)) {
        throw 'native 形态要求 -NativeDataDirPrefix：服务端 @@datadir 必须落在该前缀下，否则拒绝运行'
    }
    if ($PSBoundParameters.ContainsKey('ContainerName')) {
        throw 'native 形态不接受 -ContainerName：两种形态只能选一种'
    }
}
if ($SourceDatabase -ne 'zhicloud_crm_bpm_chain_test' -or
    $MasterDatabase -ne 'zhicloud_crm_bpm_ds_master_test' -or
    $SlaveDatabase -ne 'zhicloud_crm_bpm_ds_slave_test') {
    throw '拒绝操作预定义沙箱目标库以外的数据库'
}

function Get-Sha256 {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) { throw "文件不存在: $Path" }
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-UniqueBaseName {
    param([Parameter(Mandatory = $true)][string]$Directory, [Parameter(Mandatory = $true)][string]$Prefix)
    $stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
    $base = $Prefix + '-' + $stamp
    $suffix = 1
    while ((Test-Path -LiteralPath (Join-Path $Directory ($base + '.json'))) -or
           (Test-Path -LiteralPath (Join-Path $Directory ($base + '.log'))) -or
           (Test-Path -LiteralPath (Join-Path $Directory ($base + '.exit.txt')))) {
        $base = $Prefix + '-' + $stamp + '-' + $suffix
        $suffix++
    }
    return $base
}

if ($TargetKind -eq 'sandbox') {
    $Docker = Get-Command docker -ErrorAction Stop
} else {
    # 原生形态不需要 docker CLI；置 $null 后任何误用都会在调用点立刻暴露，而不是静默退回容器路径
    $Docker = $null
}
$Maven = Get-Command $MavenCommand -ErrorAction Stop
$repo = (Resolve-Path -LiteralPath $RepoRoot).Path
$testPath = Join-Path $repo 'zhicloud-module-crm\src\test\java\cn\zhicloud\module\crm\service\receivable\CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest.java'
$schemaFixture = Join-Path $repo 'zhicloud-module-crm\src\test\resources\sql\crm_bpm_transaction_schema_mysql.sql'
$cleanFixture = Join-Path $repo 'zhicloud-module-crm\src\test\resources\sql\crm_bpm_transaction_clean_mysql.sql'
$scriptPath = $PSCommandPath
foreach ($required in @($testPath, $schemaFixture, $cleanFixture)) {
    if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "缺少测试或 fixture: $required" }
}

function Invoke-NativeMysqlText {
    param([Parameter(Mandatory = $true)][string]$Sql)
    $env:MYSQL_PWD = $script:NativePassword
    try {
        $run = Invoke-BoundedNative -FilePath $NativeAdminClient -Arguments @(
            '--no-defaults', '--protocol=TCP',
            ('--host=' + $NativeHost), ('--port=' + [string]$NativePort),
            '-uroot', '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4',
            '-e', $Sql
        ) -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'native-mysql-query'
    } finally {
        [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')
    }
    if ($run.ExitCode -ne 0) {
        throw "native mysql 客户端退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
    }
    return $run.StdOut
}

function Invoke-DockerMysqlText {
    param([Parameter(Mandatory = $true)][string]$Sql)
    # 分派放在咽喉函数第一行：调用点一字不改，因此不存在"改了容器路径、忘了原生路径"的漂移。
    if ($TargetKind -eq 'native') { return Invoke-NativeMysqlText -Sql $Sql }
    # R-041: the password goes into this process's environment only. `docker exec -e MYSQL_PWD`
    # (name without "=value") inherits it, so the secret never appears in the docker command line.
    [Environment]::SetEnvironmentVariable('MYSQL_PWD', $script:RootPassword, 'Process')
    $dockerArgs = @(
        'exec',
        '-e', 'MYSQL_PWD',
        $ContainerName,
        'mysql', '-uroot',
        '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4',
        '-e', $Sql
    )
    # 查询结果只取 stdout：mysql 的告警走 stderr，混进返回值会污染"期望单值"断言。
    # 失败时两条流都进异常文本，不丢诊断信息。
    $run = Invoke-BoundedNative -FilePath $Docker.Source -Arguments $dockerArgs -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'docker-mysql-query'
    if ($run.ExitCode -ne 0) {
        throw "docker mysql 退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
    }
    return $run.StdOut
}

function Invoke-DockerMysqlScalar {
    param([Parameter(Mandatory = $true)][string]$Sql)
    return (Invoke-DockerMysqlText -Sql $Sql).Trim()
}

function Read-Count {
    param([Parameter(Mandatory = $true)][string]$Database, [Parameter(Mandatory = $true)][string]$Table)
    try {
        return [int](Invoke-DockerMysqlScalar -Sql ("SELECT COUNT(*) FROM ``" + $Database + "``.``" + $Table + "``;"))
    } catch {
        return $null
    }
}

# 容器取证用的 docker inspect 同样必须受超时约束：这些调用直接读 $LASTEXITCODE，
# daemon 无响应时会永久停在门禁第一步，连"失败证据"都写不出来。
function Invoke-DockerInspectJson {
    param(
        [Parameter(Mandatory = $true)][string]$FailureMessage,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [switch]$AllowFailure
    )
    $run = Invoke-BoundedNative -FilePath $Docker.Source -Arguments (@('inspect') + @($Arguments)) `
        -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'docker-inspect'
    if ($run.ExitCode -ne 0) {
        if ($AllowFailure) { return '' }
        throw "$FailureMessage（docker inspect 退出码 $($run.ExitCode)：$($run.StdErr.Trim())）"
    }
    return $run.StdOut
}

if ($TargetKind -eq 'native') {
    $script:NativePassword = [Environment]::GetEnvironmentVariable($NativePasswordEnv)
    if ([string]::IsNullOrEmpty($script:NativePassword)) {
        throw "环境变量的口令为空：-NativePasswordEnv 指向的变量必须存在且非空（拒绝无口令连原生实例）"
    }
    $script:RootPassword = $script:NativePassword
    # 归属对照：@@port 防串端，@@datadir 防连到别人的数据目录。
    $facts = (Invoke-DockerMysqlScalar -Sql 'SELECT CONCAT(VERSION(), ''|'', @@port, ''|'', @@datadir);')
    $parts = $facts.Split('|')
    if ($parts.Count -lt 3) { throw "原生实例事实读取异常：$facts" }
    if ([int]$parts[1] -ne [int]$NativePort) { throw "@@port=$($parts[1]) 与请求端口 $NativePort 不符：拒绝继续" }
    if (-not $parts[2].TrimEnd('').StartsWith($NativeDataDirPrefix.TrimEnd(''), [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "@@datadir='$($parts[2])' 不在 -NativeDataDirPrefix '$NativeDataDirPrefix' 之下：这看起来是别人的实例，拒绝运行"
    }
    $databasePort = [int]$NativePort
    $containerInspect = [pscustomobject]@{ Config = [pscustomobject]@{ Image = 'native-mysqld' } }
    $imageRepoDigest = "native-mysqld-$($parts[0])"
    $nativeVersion = $parts[0]
    Write-Output "native target: version=$($parts[0]) port=$NativePort datadir=$($parts[2])"
} else {
$containerInspectJson = Invoke-DockerInspectJson -FailureMessage "容器不存在: $ContainerName" -Arguments @($ContainerName)
$containerInspect = $containerInspectJson | ConvertFrom-Json
if (-not [bool]$containerInspect.State.Running) { throw "容器未运行: $ContainerName" }

$envJson = Invoke-DockerInspectJson -FailureMessage '无法读取容器环境变量' -Arguments @('--format', '{{json .Config.Env}}', $ContainerName)
$envValues = (($envJson.Trim() | ConvertFrom-Json) -as [array])
$passwordLine = $envValues | Where-Object { $_ -like 'MYSQL_ROOT_PASSWORD=*' } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($passwordLine)) { throw '容器未提供 MYSQL_ROOT_PASSWORD' }
$script:RootPassword = $passwordLine.Substring('MYSQL_ROOT_PASSWORD='.Length)
if ([string]::IsNullOrEmpty($script:RootPassword)) { throw 'MYSQL_ROOT_PASSWORD 为空' }

$portMapJson = Invoke-DockerInspectJson -FailureMessage '无法读取容器端口映射' -Arguments @('--format', '{{json .NetworkSettings.Ports}}', $ContainerName)
$portMap = $portMapJson.Trim() | ConvertFrom-Json
$portProperty = $portMap.PSObject.Properties['3306/tcp']
if ($null -eq $portProperty -or $null -eq $portProperty.Value -or @($portProperty.Value).Count -eq 0) {
    throw '容器没有 3306/tcp 端口映射'
}
$portBinding = @($portProperty.Value)[0]
$databasePort = [int]$portBinding.HostPort
if ($databasePort -le 0) { throw '容器端口映射无效' }
# 镜像 digest 是 R-G3.5-3 关心的"证据"，取不到时退回镜像名并保持空串语义不变。
$imageRepoDigest = Invoke-DockerInspectJson -FailureMessage '无法读取镜像 digest' `
    -Arguments @('--format', '{{index .RepoDigests 0}}', $ContainerName) -AllowFailure
if ([string]::IsNullOrWhiteSpace($imageRepoDigest)) {
    $imageRepoDigest = [string]$containerInspect.Config.Image
}
}


if (-not (Test-Path -LiteralPath (Join-Path $repo '.git') -PathType Container)) {
    throw "仓库未找到 .git 目录: $repo"
}
$sourceHead = ((& git -C $repo rev-parse HEAD 2>$null) | Out-String).Trim()
$branch = ((& git -C $repo branch --show-current 2>$null) | Out-String).Trim()
$dirtyCount = @((& git -C $repo status --porcelain 2>$null)).Count

$base = Get-UniqueBaseName -Directory $EvidenceDir -Prefix 'crm-bpm-dynamic-datasource-mysql'
$logPath = Join-Path $EvidenceDir ($base + '.log')
$exitPath = Join-Path $EvidenceDir ($base + '.exit.txt')
$jsonPath = Join-Path $EvidenceDir ($base + '.json')
$jsonShaPath = Join-Path $EvidenceDir ($base + '.json.sha256')
$mavenArgs = @(
    '-o',
    '-pl', 'zhicloud-module-crm',
    # R-036：缺 -am 时上游模块（含 zhicloud-module-bpm 这套幂等/Flowable 链路本体）取的是
    # ~/.m2 里已安装的 jar。实测本机 33 个 zhicloud jar 有 13 个比其模块源码旧 1-4 天
    # （handoff §12.35），所以不加 -am 的"通过"根本不覆盖当前工作树。补 -am 会换掉被测产物集合，
    # 因此本脚本此后产生的证据与 2026-09-29 那批（无 -am）不同批，不可互相引用。
    '-am',
    '-Dtest=CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest',
    '-DfailIfNoTests=false',
    # R-036 补 -am 的必然后果（本轮真跑才暴露）：-am 会把上游模块拉进 reactor，
    # 上游模块里没有这个测试类，surefire 默认按"No tests matching pattern"直接失败。
    # -DfailIfNoTests=false 管的是"没有测试"，-Dsurefire.failIfNoSpecifiedTests=false 管的才是
    # "指定的模式在某个模块里没命中"——两者不是一回事，缺后者 -am 就跑不通。
    '-Dsurefire.failIfNoSpecifiedTests=false',
    'test'
)
$commandText = $Maven.Source + ' ' + ($mavenArgs -join ' ')

$result = [ordered]@{
    generated_at = (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')
    status = 'failed'
    scope = 'CRM 回款提交与真实 BPM/Flowable 在隔离 MySQL 动态数据源上的路由、事务绑定、提交与回滚验证'
    source_head = $sourceHead
    branch = $branch
    working_tree_state = "dirty_entries=$dirtyCount; evidence_not_committed_at_generation_time"
    environment = [ordered]@{
        classification = '隔离 MySQL 单实例，非生产'
        container = $ContainerName
        target_kind = $TargetKind
        native_port = if ($TargetKind -eq 'native') { [int]$NativePort } else { $null }
        native_version = if ($TargetKind -eq 'native') { $nativeVersion } else { $null }
        admin_transport = if ($TargetKind -eq 'native') { 'host_client_loopback_native_mysqld' } else { 'docker_exec_in_namespace' }
        image = [string]$containerInspect.Config.Image
        image_repo_digest = $imageRepoDigest
        host = '127.0.0.1'
        port = $databasePort
        server_version = $null
        transaction_isolation = $null
        autocommit = $null
        source_database = $SourceDatabase
        master_database = $MasterDatabase
        slave_database = $SlaveDatabase
        logical_databases_note = 'master 与 slave 是同实例内的两个逻辑库，不是生产主从复制或独立物理库'
        post_run_counts = [ordered]@{}
    }
    activation_environment = [ordered]@{
        ZHICLOUD_CRM_BPM_DS_IT = 'true'
        ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL = "jdbc:mysql://127.0.0.1:$databasePort/${MasterDatabase}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore"
        ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL = "jdbc:mysql://127.0.0.1:$databasePort/${SlaveDatabase}?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore"
        ZHICLOUD_CRM_BPM_DS_IT_MYSQL_USERNAME = 'root'
        ZHICLOUD_CRM_BPM_DS_IT_MYSQL_PASSWORD = '<redacted>'
    }
    command = $commandText
    test = [ordered]@{
        class = 'cn.zhicloud.module.crm.service.receivable.CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest'
        methods = @(
            'dynamicRoutingShouldExposeMasterAndSlaveDatabases',
            'submitReceivableShouldCommitCrmBpmAndFlowableOnMasterInOneTransaction',
            'submitReceivableShouldRollbackCrmBpmAndFlowableOnMasterInOneTransaction'
        )
        tests_run = $null
        failures = $null
        errors = $null
        skipped = $null
        build = $null
        exit_code = $null
    }
    schema_preparation = [ordered]@{
        classification = '从隔离 chain_test 复制 Flowable schema 到 master_test；尚非空库自举 CI'
        method = 'CREATE TABLE master.table LIKE chain.table，并复制 ACT_GE_PROPERTY 7 行元数据'
        flowable_tables_copied = $null
        act_ge_property_rows_copied = $null
    }
    covered = @(
        'DynamicRoutingDataSource 只暴露 master 和 slave 两个显式路由',
        'master/slave 分别连接两个隔离逻辑库',
        'transactionManager 绑定 DynamicRoutingDataSource',
        'Flowable TransactionAwareDataSourceProxy 的目标是同一动态数据源',
        'Spring 事务绑定连接通过 CONNECTION_ID()/DATABASE() 验证',
        'CRM/BPM 幂等/Flowable 提交与回滚一致性及 slave 无业务写入'
    )
    not_covered = @(
        '生产主从复制、复制延迟、真实读写分离与生产物理库拓扑',
        '网络超时、连接中断、数据库瞬时故障、进程中断和提交边界异常',
        '生产规模并发、MDL/锁等待、长稳压测和容量验证',
        'Outbox/Inbox、DLQ、补偿、重放、异常流程回收、幂等残留清理与 CRM/Flowable 跨域对账',
        'K8s/HA、备份恢复、TLS、外部 Secret 和真实生产变更管理',
        '网络抓包或 MySQL 全量审计日志级别的逐条 SQL 连接证明'
    )
    artifacts = [ordered]@{
        runner_script = 'scripts/production/verify_crm_bpm_dynamic_datasource_mysql.ps1'
        runner_script_sha256 = (Get-Sha256 -Path $scriptPath)
        raw_log = 'docs/enterprise-platform/evidence/production/' + $base + '.log'
        raw_log_sha256 = $null
        exit_code_file = 'docs/enterprise-platform/evidence/production/' + $base + '.exit.txt'
        exit_code_file_sha256 = $null
        test = 'zhicloud-module-crm/src/test/java/cn/zhicloud/module/crm/service/receivable/CrmReceivableBpmDynamicDataSourceMysqlIntegrationTest.java'
        test_sha256 = (Get-Sha256 -Path $testPath)
        schema_fixture = 'zhicloud-module-crm/src/test/resources/sql/crm_bpm_transaction_schema_mysql.sql'
        schema_fixture_sha256 = (Get-Sha256 -Path $schemaFixture)
        clean_fixture = 'zhicloud-module-crm/src/test/resources/sql/crm_bpm_transaction_clean_mysql.sql'
        clean_fixture_sha256 = (Get-Sha256 -Path $cleanFixture)
    }
    production_ready = $false
    gate3_passed = $false
    independent_review = $false
    risk_register = [ordered]@{ R_011 = 'OPEN'; R_029 = 'OPEN' }
    Go_No_Go = 'No-Go'
    caveats = @(
        '该脚本只在隔离 MySQL 沙箱的逻辑测试库执行，不代表生产可部署。',
        '测试通过不等于 R-011 或 R-029 已关闭。',
        'master Flowable schema 从 chain_test 人工复制，尚不是完全空库自举的 CI 流程。',
        '两个逻辑库不能证明生产主从复制、复制延迟或真实读写分离。',
        '连接一致性由 Spring 事务绑定连接的 CONNECTION_ID()/DATABASE() 与提交回滚结果验证，不是网络抓包或 MySQL 全量审计日志。',
        '失败时保留日志和 exit 文件，不覆盖既有证据。'
    )
}

$masterActivationUrl = [string]$result.activation_environment.ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL
$slaveActivationUrl = [string]$result.activation_environment.ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL
if (-not $masterActivationUrl.Contains('/' + $MasterDatabase + '?')) {
    throw "Master JDBC URL 未包含完整数据库名 $MasterDatabase`: $masterActivationUrl"
}
if (-not $slaveActivationUrl.Contains('/' + $SlaveDatabase + '?')) {
    throw "Slave JDBC URL 未包含完整数据库名 $SlaveDatabase`: $slaveActivationUrl"
}

$previousEnv = @{}
foreach ($name in @('ZHICLOUD_CRM_BPM_DS_IT', 'ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL', 'ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL', 'ZHICLOUD_CRM_BPM_DS_IT_MYSQL_USERNAME', 'ZHICLOUD_CRM_BPM_DS_IT_MYSQL_PASSWORD')) {
    $previousEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

$mavenOutput = @()
$exitCode = 1
try {
    $serverFacts = Invoke-DockerMysqlText -Sql "SELECT CONCAT(VERSION(), '|', @@transaction_isolation, '|', @@autocommit);"
    $serverParts = $serverFacts.Split('|')
    if ($serverParts.Count -ge 3) {
        $result.environment.server_version = $serverParts[0]
        $result.environment.transaction_isolation = $serverParts[1]
        $result.environment.autocommit = [int]$serverParts[2]
    }

    $allSourceTables = @((Invoke-DockerMysqlText -Sql "SELECT table_name FROM information_schema.tables WHERE table_schema='$SourceDatabase' ORDER BY table_name;") -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^(ACT|FLW)_' })
    if ($ExpectedFlowableTables -le 0 -or $ExpectedPropertyRows -le 0) {
        throw "期望表数/行数必须为正（当前 $ExpectedFlowableTables / $ExpectedPropertyRows）"
    }
    if ($SourceProvisioning -eq 'pipeline' -and ($ExpectedFlowableTables -ne 32 -or $ExpectedPropertyRows -ne 6)) {
        throw "pipeline 预置只可能是 32 张 Flowable 表 / 6 行 ACT_GE_PROPERTY；你传了 $ExpectedFlowableTables/$ExpectedPropertyRows。若用的是人工预置库，请显式加 -SourceProvisioning manual。"
    }
    $result['source_provisioning'] = $SourceProvisioning
    $result['expected_flowable_tables'] = $ExpectedFlowableTables
    $result['expected_property_rows'] = $ExpectedPropertyRows
    if ($allSourceTables.Count -ne $ExpectedFlowableTables) { throw "SourceDatabase 中 Flowable 表数量不是 $ExpectedFlowableTables（来源=$SourceProvisioning）: $($allSourceTables.Count)" }
    $propertyCount = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM ``$SourceDatabase``.ACT_GE_PROPERTY;")
    if ($propertyCount -ne $ExpectedPropertyRows) { throw "SourceDatabase.ACT_GE_PROPERTY 行数不是 $ExpectedPropertyRows（来源=$SourceProvisioning）: $propertyCount" }

    $rebuildSql = "DROP DATABASE IF EXISTS ``$MasterDatabase``; CREATE DATABASE ``$MasterDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; DROP DATABASE IF EXISTS ``$SlaveDatabase``; CREATE DATABASE ``$SlaveDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    Invoke-DockerMysqlText -Sql $rebuildSql | Out-Null

    $copySql = foreach ($table in $allSourceTables) {
        "CREATE TABLE ``$MasterDatabase``.``$table`` LIKE ``$SourceDatabase``.``$table``;"
    }
    $copySql += "INSERT INTO ``$MasterDatabase``.ACT_GE_PROPERTY SELECT * FROM ``$SourceDatabase``.ACT_GE_PROPERTY;"
    Invoke-DockerMysqlText -Sql ($copySql -join "`n") | Out-Null

    $result.schema_preparation.flowable_tables_copied = $allSourceTables.Count
    $result.schema_preparation.act_ge_property_rows_copied = $propertyCount
    $result.environment.master_table_count = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MasterDatabase';")
    $result.environment.slave_table_count = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SlaveDatabase';")

    $env:ZHICLOUD_CRM_BPM_DS_IT = 'true'
    $env:ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL = [string]$result.activation_environment.ZHICLOUD_CRM_BPM_DS_IT_MASTER_URL
    $env:ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL = [string]$result.activation_environment.ZHICLOUD_CRM_BPM_DS_IT_SLAVE_URL
    $env:ZHICLOUD_CRM_BPM_DS_IT_MYSQL_USERNAME = 'root'
    $env:ZHICLOUD_CRM_BPM_DS_IT_MYSQL_PASSWORD = $script:RootPassword

    # maven 是本门禁最容易挂起的一步（依赖解析、测试内死锁），必须受硬超时约束（R-G3.5-2）。
    # 用 -WorkingDirectory 取代 Push-Location：子进程工作目录由执行器显式决定，不依赖宿主状态。
    $run = Invoke-BoundedNative `
        -FilePath $Maven.Source `
        -Arguments $mavenArgs `
        -TimeoutSeconds $script:NativeTimeoutSeconds `
        -Stage 'maven-test' `
        -WorkingDirectory $repo
    $exitCode = $run.ExitCode
    $mavenOutput = @(($run.StdOut + [Environment]::NewLine + $run.StdErr) -split "\r?\n")

    $logText = (($mavenOutput | ForEach-Object { [string]$_ }) -join "`n")
    $matches = [regex]::Matches($logText, 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)')
    if ($matches.Count -gt 0) {
        $match = $matches[$matches.Count - 1]
        $result.test.tests_run = [int]$match.Groups[1].Value
        $result.test.failures = [int]$match.Groups[2].Value
        $result.test.errors = [int]$match.Groups[3].Value
        $result.test.skipped = [int]$match.Groups[4].Value
    }
    if ($logText -match 'BUILD SUCCESS') { $result.test.build = 'SUCCESS' }
    elseif ($logText -match 'BUILD FAILURE') { $result.test.build = 'FAILURE' }
    $result.test.exit_code = [int]$exitCode

    foreach ($table in @('crm_receivable', 'bpm_process_instance_idempotency', 'ACT_HI_PROCINST', 'ACT_RE_PROCDEF', 'ACT_RE_DEPLOYMENT')) {
        $result.environment.post_run_counts[$table] = Read-Count -Database $MasterDatabase -Table $table
    }
    foreach ($table in @('crm_receivable', 'bpm_process_instance_idempotency')) {
        $result.environment.post_run_counts[('slave_' + $table)] = Read-Count -Database $SlaveDatabase -Table $table
    }

    if ($exitCode -eq 0 -and $result.test.tests_run -eq 3 -and $result.test.failures -eq 0 -and $result.test.errors -eq 0 -and $result.test.skipped -eq 0 -and $result.test.build -eq 'SUCCESS') {
        $result.status = 'passed'
    } else {
        $result.status = 'failed'
    }
} catch {
    $result.status = 'failed'
    $result.error = $_.Exception.Message
} finally {
    foreach ($name in $previousEnv.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousEnv[$name], 'Process')
    }
    [System.IO.File]::WriteAllLines($logPath, @($mavenOutput | ForEach-Object { [string]$_ }), [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($exitPath, [string]$exitCode, [System.Text.UTF8Encoding]::new($false))
    $result.artifacts.raw_log_sha256 = Get-Sha256 -Path $logPath
    $result.artifacts.exit_code_file_sha256 = Get-Sha256 -Path $exitPath
    [System.IO.File]::WriteAllText($jsonPath, ($result | ConvertTo-Json -Depth 12), [System.Text.UTF8Encoding]::new($false))
    $jsonHash = Get-Sha256 -Path $jsonPath
    [System.IO.File]::WriteAllText($jsonShaPath, ($jsonHash + ' *' + [System.IO.Path]::GetFileName($jsonPath) + "`n"), [System.Text.UTF8Encoding]::new($false))
}

Write-Output ('evidence=' + $jsonPath)
Write-Output ('status=' + $result.status)
if ($result.status -ne 'passed') { exit 1 }