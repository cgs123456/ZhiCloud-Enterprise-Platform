#requires -Version 5.1
<#
.SYNOPSIS
  为 CRM/BPM 动态数据源验证器产出前置库 zhicloud_crm_bpm_chain_test（R-035 的仓库内产出路径）。

.DESCRIPTION
  动态数据源验证器（verify_crm_bpm_dynamic_datasource_mysql.ps1）要求源库里已有 Flowable schema，
  但仓库里此前没有任何脚本创建这个库，更没有脚本产出它的表：历史 45/7 是仓库外人工预置，
  当前流水线可复现的 32/6 也只是"某次真链路 IT 恰好跑过之后留下的结果"。本脚本把这两步补成流程：

    1) 在已运行的隔离沙箱里创建（或按 -Replace 重建）zhicloud_crm_bpm_chain_test；
    2) -RunRealChainIt 时用 Maven 真跑 CrmReceivableBpmTransactionMysqlIntegrationTest，
       由该 IT 的 Spring Boot 上下文以 flowable.database-schema-update=true 建出 Flowable 表；
    3) 计数并与期望值严格相等断言，落 JSON 证据。

  第 2 步是本脚本存在的理由：仓库里没有任何 .sql 创建 ACT_/FLW_ 表（已实测：
  db/migration 与测试 fixture 里的 ACT/FLW 命中数为 0），所以"可复现的源库"只能由真实引擎引导产生，
  不能靠复制粘贴 DDL。没有 -RunRealChainIt 时脚本只交付一个**空库**并如实报告 0/0，
  不会伪造出满足前置条件的样子。

.NOTES
  只允许操作本机 Docker daemon 上的隔离沙箱容器与预定义库名，失败也写证据。
#>
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$ContainerName = '',
    [string]$SourceDatabase = 'zhicloud_crm_bpm_chain_test',
    [string]$MavenCommand = 'mvn',
    [string]$EvidenceDir,
    [int]$TimeoutSeconds = 1800,
    [int]$DatabasePort = 0,
    [int]$ExpectedFlowableTables = 0,
    [int]$ExpectedPropertyRows = 0,
    [switch]$RunRealChainIt,
    [switch]$Replace,
    # R-040 方案 B（2026-10-02 责任人点选）：并列新增一条"宿主原生 mysqld"通道。
    # 容器名白名单一条不删 —— native 模式有它自己的三条约束（回环、专用端口、datadir 归属），
    # 少任何一条就拒绝运行，因此这是增加通道而不是拆掉守卫。
    [ValidateSet('sandbox', 'native')][string]$TargetKind = 'sandbox',
    [string]$NativeAdminClient = '',
    [int]$NativePort = 0,
    [ValidateSet('127.0.0.1', '::1', 'localhost')][string]$NativeHost = '127.0.0.1',
    [string]$NativePasswordEnv = '',
    [string]$NativeDataDirPrefix = ''
)

$ErrorActionPreference = 'Stop'

function Assert-LocalDockerDaemon {
    param([string[]]$AllowedContexts = @('default', 'desktop-linux', 'colima', 'desktop'))
    if (-not [string]::IsNullOrWhiteSpace($env:DOCKER_HOST)) {
        throw "检测到 DOCKER_HOST='$($env:DOCKER_HOST)'：拒绝在远程 Docker daemon 上创建数据库。"
    }
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $ctxText = ((& docker context show 2>&1) | Out-String).Trim()
        $ctxExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($ctxExit -ne 0) { throw "无法确定当前 Docker context（exit=$ctxExit）：$ctxText" }
    if ($AllowedContexts -notcontains $ctxText) {
        throw "当前 Docker context 为 '$ctxText'，不在允许集合 ($($AllowedContexts -join ', ')) 内：拒绝继续。"
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
$script:NativeTimeoutSeconds = if ($TimeoutSeconds -gt 0) { $TimeoutSeconds } else { 1800 }
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
    # 参数不再是 Mandatory（否则 native 形态连绑定都过不了），因此这里必须自己补一条存在性守卫：
    # 少这一句就会出现"忘了传容器名 → 报一个跟真实问题无关的错"。
    if ([string]::IsNullOrWhiteSpace($ContainerName)) {
        throw 'sandbox 形态要求 -ContainerName（沙箱容器名）；要连宿主原生实例请显式传 -TargetKind native'
    }
    if ($ContainerName -notmatch '^zhicloud-mysql-sandbox-[0-9a-f]{8}$') {
        throw "拒绝非隔离沙箱容器: $ContainerName"
    }
} else {
    # native 形态的三条自有约束
    if ([string]::IsNullOrWhiteSpace($NativeAdminClient) -or -not (Test-Path -LiteralPath $NativeAdminClient -PathType Leaf)) {
        throw "native 形态要求 -NativeAdminClient 指向存在的 mysql.exe（当前 '$NativeAdminClient'）"
    }
    if ($NativePort -lt 1024 -or $NativePort -gt 65535) {
        throw "native 形态要求专用端口 1024-65535（当前 $NativePort）"
    }
    if ($NativePort -eq 3306) {
        throw 'native 形态拒绝 3306：本机默认端口可能属于其它项目的 mysqld（§12.60），本脚本只连自己起的专用实例'
    }
    if ([string]::IsNullOrWhiteSpace($NativePasswordEnv)) {
        throw 'native 形态要求 -NativePasswordEnv 给出存放口令的环境变量名（口令不得进命令行）'
    }
    if ([string]::IsNullOrWhiteSpace($NativeDataDirPrefix)) {
        throw 'native 形态要求 -NativeDataDirPrefix：服务端 @@datadir 必须落在该前缀下，否则拒绝运行'
    }
    if (-not [string]::IsNullOrWhiteSpace($ContainerName)) {
        throw 'native 形态不接受 -ContainerName：两种形态只能选一种，避免半沙箱半原生的混合目标'
    }
}
if ($SourceDatabase -ne 'zhicloud_crm_bpm_chain_test') {
    throw "拒绝操作预定义源库以外的数据库: $SourceDatabase"
}
if ($ExpectedFlowableTables -lt 0 -or $ExpectedPropertyRows -lt 0) {
    throw "期望表数/行数不能为负（当前 $ExpectedFlowableTables / $ExpectedPropertyRows）"
}
# 空库与引擎引导库是两个不同的期望值，不允许把 0 配成 32 之类自相矛盾的组合。
if ($RunRealChainIt) {
    # 引擎产出的表数不由调用方决定：不显式传期望值时取可复现的 32/6，
    # 显式传了就必须完全一致，否则"期望值"退化成可以被随意调和的东西。
    if (-not $PSBoundParameters.ContainsKey('ExpectedFlowableTables') -and
        -not $PSBoundParameters.ContainsKey('ExpectedPropertyRows')) {
        $ExpectedFlowableTables = 32
        $ExpectedPropertyRows = 6
    }
    if ($ExpectedFlowableTables -ne 32 -or $ExpectedPropertyRows -ne 6) {
        throw "-RunRealChainIt 的产出只可能是 32 张 Flowable 表 / 6 行 ACT_GE_PROPERTY，你传了 $ExpectedFlowableTables/$ExpectedPropertyRows"
    }
} elseif ($ExpectedFlowableTables -ne 0 -or $ExpectedPropertyRows -ne 0) {
    throw "不带 -RunRealChainIt 只创建空库，期望值必须是 0/0，你传了 $ExpectedFlowableTables/$ExpectedPropertyRows"
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
           (Test-Path -LiteralPath (Join-Path $Directory ($base + '.log')))) {
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
$testClass = 'CrmReceivableBpmTransactionMysqlIntegrationTest'
$scriptPath = $PSCommandPath

function Invoke-DockerMysqlText {
    param([Parameter(Mandatory = $true)][string]$Sql)
    # 形态分派放在咽喉函数里：容器模式与原生模式各自受自己的守卫约束，
    # 调用点一字不改，因此不存在"改了这条路径、忘了那条"的漂移。
    if ($script:TargetKindValue -eq 'native') { return Invoke-NativeMysqlText -Sql $Sql }
    # MYSQL_PWD 只经父进程环境继承（`-e MYSQL_PWD` 不带 =值）：写成 `-e MYSQL_PWD=xxx`
    # 会把 root 口令放进 docker 进程的命令行，任何进程监视器都能读到。
    $dockerArgs = @(
        'exec',
        '-e', 'MYSQL_PWD',
        $ContainerName,
        'mysql', '-uroot',
        '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4',
        '-e', $Sql
    )
    $run = Invoke-BoundedNative -FilePath $Docker.Source -Arguments $dockerArgs `
        -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'docker-mysql-query'
    if ($run.ExitCode -ne 0) {
        throw "docker mysql 退出码 $($run.ExitCode)`: $(($run.StdOut + [Environment]::NewLine + $run.StdErr).Trim())"
    }
    return $run.StdOut
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

function Invoke-DockerMysqlScalar {
    param([Parameter(Mandatory = $true)][string]$Sql)
    return (Invoke-DockerMysqlText -Sql $Sql).Trim()
}
$script:TargetKindValue = $TargetKind

function Invoke-AdminSqlScalar {
    param([Parameter(Mandatory = $true)][string]$Sql)
    return (Invoke-AdminSqlText -Sql $Sql).Trim()
}

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

function Get-FlowableTableCount {
    param([Parameter(Mandatory = $true)][string]$Database)
    $rows = @((Invoke-DockerMysqlText -Sql "SELECT table_name FROM information_schema.tables WHERE table_schema='$Database' ORDER BY table_name;") -split "`r?`n" |
        ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^(ACT|FLW)_' })
    return $rows.Count
}

if ($TargetKind -eq 'native') {
    $script:NativePassword = [Environment]::GetEnvironmentVariable($NativePasswordEnv)
    if ([string]::IsNullOrEmpty($script:NativePassword)) {
        throw "环境变量的口令为空：-NativePasswordEnv 指向的变量必须存在且非空（拒绝无口令连原生实例）"
    }
    # 归属对照：连的必须是我自己起的专用实例。@@port 防串端，@@datadir 防连到别人的数据目录（§12.60）。
    $serverFacts = (Invoke-DockerMysqlScalar -Sql "SELECT CONCAT(VERSION(), '|', @@port, '|', @@datadir);")
    $parts = $serverFacts.Split('|')
    if ($parts.Count -lt 3) { throw "原生实例事实读取异常：$serverFacts" }
    if ([int]$parts[1] -ne [int]$NativePort) { throw "@@port=$($parts[1]) 与请求端口 $NativePort 不符：拒绝继续" }
    if (-not $parts[2].TrimEnd('').StartsWith($NativeDataDirPrefix.TrimEnd(''), [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "@@datadir='$($parts[2])' 不在 -NativeDataDirPrefix '$NativeDataDirPrefix' 之下：这看起来是别人的实例，拒绝运行"
    }
    $script:RootPassword = $script:NativePassword
    $rootPassword = $script:NativePassword
    $databasePort = [int]$NativePort
    $publishedPort = [int]$NativePort
    $endpointSource = 'native_loopback'
    $nativeVersion = $parts[0]
    $nativeDataDir = $parts[2]
    $containerInspect = [pscustomobject]@{ Config = [pscustomobject]@{ Image = 'native-mysqld' }; State = [pscustomobject]@{ Running = $true } }
    $imageRepoDigest = "native-mysqld-$nativeVersion"
    Write-Output "native target: version=$nativeVersion port=$NativePort datadir=$nativeDataDir"
    $script:SkipContainerInspect = $true
} else {
    $script:SkipContainerInspect = $false
}
if (-not $script:SkipContainerInspect) {
$containerInspectJson = Invoke-DockerInspectJson -FailureMessage "容器不存在: $ContainerName" -Arguments @($ContainerName)
$containerInspect = $containerInspectJson | ConvertFrom-Json
if (-not [bool]$containerInspect.State.Running) { throw "容器未运行: $ContainerName" }

$envJson = Invoke-DockerInspectJson -FailureMessage '无法读取容器环境变量' -Arguments @('--format', '{{json .Config.Env}}', $ContainerName)
$envValues = (($envJson.Trim() | ConvertFrom-Json) -as [array])
$passwordLine = $envValues | Where-Object { $_ -like 'MYSQL_ROOT_PASSWORD=*' } | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($passwordLine)) { throw '容器未提供 MYSQL_ROOT_PASSWORD' }
$rootPassword = $passwordLine.Substring('MYSQL_ROOT_PASSWORD='.Length)
if ([string]::IsNullOrEmpty($rootPassword)) { throw 'MYSQL_ROOT_PASSWORD 为空' }

$portMapJson = Invoke-DockerInspectJson -FailureMessage '无法读取容器端口映射' -Arguments @('--format', '{{json .NetworkSettings.Ports}}', $ContainerName)
$portMap = $portMapJson.Trim() | ConvertFrom-Json
$portProperty = $portMap.PSObject.Properties['3306/tcp']
if ($null -eq $portProperty -or $null -eq $portProperty.Value -or @($portProperty.Value).Count -eq 0) {
    throw '容器没有 3306/tcp 端口映射'
}
$publishedPort = [int](@($portProperty.Value)[0].HostPort)
if ($publishedPort -le 0) { throw '容器端口映射无效' }
if ($DatabasePort -lt 0 -or $DatabasePort -gt 65535) {
    throw "拒绝端口 $DatabasePort`：-DatabasePort 只允许 0（用容器自身发布端口）或 1-65535。"
}
# 显式端口是给"容器内回环中继"留的口子：中继宿主端口是让 mysqld 把连接看作 127.0.0.1 的入口，
# 而直连发布端口在 Windows Docker 上会以"TCP 已建立、mysqld 首包从未到达"的形态卡住握手
# （实测证据：本脚本 211243 那次失败栈停在 NativeProtocol.readServerCapabilities）。
if ($DatabasePort -gt 0) {
    $databasePort = $DatabasePort
    $endpointSource = 'explicit'
} else {
    $databasePort = $publishedPort
    $endpointSource = 'container_published_port'
}
$imageRepoDigest = Invoke-DockerInspectJson -FailureMessage '无法读取镜像 digest' `
    -Arguments @('--format', '{{index .RepoDigests 0}}', $ContainerName) -AllowFailure
if ([string]::IsNullOrWhiteSpace($imageRepoDigest)) { $imageRepoDigest = [string]$containerInspect.Config.Image }
}


if (-not (Test-Path -LiteralPath (Join-Path $repo '.git') -PathType Container)) {
    throw "仓库未找到 .git 目录: $repo"
}
$sourceHead = ((& git -C $repo rev-parse HEAD 2>$null) | Out-String).Trim()
$branch = ((& git -C $repo branch --show-current 2>$null) | Out-String).Trim()
$dirtyCount = @((& git -C $repo status --porcelain 2>$null)).Count

$base = Get-UniqueBaseName -Directory $EvidenceDir -Prefix 'crm-bpm-chain-source-provision'
$logPath = Join-Path $EvidenceDir ($base + '.log')
$jsonPath = Join-Path $EvidenceDir ($base + '.json')
$jsonShaPath = Join-Path $EvidenceDir ($base + '.json.sha256')

$mavenArgs = @(
    '-o', '-B',
    '-pl', 'zhicloud-module-crm',
    # R-036：源库的 Flowable 表由上游 zhicloud-module-bpm 里的引擎引导创建，
    # 没有 -am 时该模块取的是 ~/.m2 已安装 jar，产出结果不反映当前工作树。
    '-am',
    "-Dtest=$testClass",
    '-Dsurefire.failIfNoSpecifiedTests=false',
    'test'
)
$commandText = $Maven.Source + ' ' + ($mavenArgs -join ' ')

# 与 §12.18.1 那次唯一成功的真链路跑保持同一 URL 形态：显式 connectTimeout，
# 否则连接阶段的问题会以"约 10 秒超时"出现，与认证失败/库不存在难以区分（R-034 未定论项）。
$jdbcUrl = "jdbc:mysql://127.0.0.1:$databasePort/$SourceDatabase`?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Singapore&connectTimeout=8000"

$result = [ordered]@{
    generated_at        = (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')
    status              = 'failed'
    scope               = '为动态数据源验证器产出前置库 zhicloud_crm_bpm_chain_test（建库 + 可选真链路 IT 引导 Flowable schema）'
    source_head         = $sourceHead
    branch              = $branch
    working_tree_state  = "dirty_entries=$dirtyCount; evidence_not_committed_at_generation_time"
    environment         = [ordered]@{
        classification      = '隔离 MySQL 单实例沙箱的逻辑测试库，非生产'
        container           = $ContainerName
        image               = [string]$containerInspect.Config.Image
        image_repo_digest   = $imageRepoDigest
        host                = '127.0.0.1'
        port                = $databasePort
        server_version      = $null
        source_database     = $SourceDatabase
        target_kind = $TargetKind
        native_port = if ($TargetKind -eq 'native') { [int]$NativePort } else { $null }
        native_version = if ($TargetKind -eq 'native') { $nativeVersion } else { $null }
        native_datadir = if ($TargetKind -eq 'native') { $nativeDataDir } else { $null }
        native_datadir_prefix = if ($TargetKind -eq 'native') { $NativeDataDirPrefix } else { $null }
        admin_transport = if ($TargetKind -eq 'native') { 'host_client_loopback_native_mysqld' } else { 'docker_exec_in_namespace' }
        jdbc_endpoint       = "127.0.0.1:$databasePort"
        jdbc_endpoint_source = $endpointSource
        container_published_port = $publishedPort
        jdbc_url_parameters = 'connectTimeout=8000'
    }
    provisioning        = [ordered]@{
        mode                        = if ($Replace) { 'drop_then_create' } else { 'create_if_absent' }
        existed_before              = $null
        created                     = $false
        schema_producer             = if ($RunRealChainIt) { 'real_chain_it_flowable_schema_update' } else { 'empty_database_only' }
        expected_flowable_tables    = $ExpectedFlowableTables
        expected_property_rows      = $ExpectedPropertyRows
        flowable_tables_after_create = $null
        flowable_tables_final       = $null
        property_rows_final         = $null
    }
    command             = $commandText
    test                = [ordered]@{
        class      = "cn.zhicloud.module.crm.service.receivable.$testClass"
        tests_run  = $null
        failures   = $null
        errors     = $null
        skipped    = $null
        build      = $null
        exit_code  = $null
        note       = '未启用 -RunRealChainIt 时不运行 IT'
    }
    artifacts           = [ordered]@{
        script               = 'scripts/production/provision_crm_bpm_chain_source.ps1'
        script_sha256        = $null
        raw_log_sha256       = $null
        it_source_sha256     = $null
        it_source_note       = 'IT 源文件按哈希钉定，本脚本不修改它'
    }
    downstream          = [ordered]@{
        verifier        = 'scripts/production/verify_crm_bpm_dynamic_datasource_mysql.ps1'
        verifier_flags  = '-SourceProvisioning pipeline -ExpectedFlowableTables 32 -ExpectedPropertyRows 6'
    }
    production_ready    = $false
    gate3_passed        = $false
    independent_review  = $false
    risk_register       = [ordered]@{ R_011 = 'OPEN'; R_029 = 'OPEN' }
    Go_No_Go            = 'No-Go'
    caveats             = @(
        '本脚本只在隔离 MySQL 沙箱内创建预定义源库，不代表生产可部署。',
        '不创建 ACT_/FLW_ 表的 SQL：仓库内没有 Flowable DDL，表只能由真实引擎引导产生，因此 -RunRealChainIt 是产出 32/6 的唯一路径。',
        '产出 32/6 不等于历史人工预置库的 45/7；引用本证据时必须同时写下 -SourceProvisioning pipeline。',
        'root 口令取自容器环境变量，只经进程环境继承传给 docker/maven，不进入命令行参数。',
        '失败时同样写出 JSON 证据，且不覆盖既有证据文件。'
    )
}

$envNames = @('ZHICLOUD_CRM_BPM_REAL_CHAIN_IT', 'ZHICLOUD_CRM_BPM_IT_MYSQL_URL',
    'ZHICLOUD_CRM_BPM_IT_MYSQL_USERNAME', 'ZHICLOUD_CRM_BPM_IT_MYSQL_PASSWORD', 'MYSQL_PWD')
$previousEnv = @{}
foreach ($name in $envNames) { $previousEnv[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }

$logLines = @()
$exitCode = 0
try {
    # MYSQL_PWD 必须在整个管理阶段都留在本进程环境里：`docker exec -e MYSQL_PWD`（不带 =值）
    # 是从 docker CLI 的进程环境取值的。第一次查询后就清掉它，后面的查询会变成
    # "Access denied for user 'root'@'localhost' (using password: NO)"。
    $env:MYSQL_PWD = $rootPassword
    $serverFacts = (Invoke-DockerMysqlScalar -Sql 'SELECT CONCAT(VERSION(), ''|'', @@transaction_isolation);').Split('|')
    $result.environment.server_version = $serverFacts[0]

    $existed = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$SourceDatabase';")
    $result.provisioning.existed_before = ($existed -gt 0)
    if ($existed -gt 0 -and -not $Replace) {
        throw "$SourceDatabase 已存在：确认要重建请显式加 -Replace（不会静默 DROP 一个可能正被其它证据引用的库）"
    }
    if ($existed -gt 0) {
        Invoke-DockerMysqlText -Sql "DROP DATABASE IF EXISTS ``$SourceDatabase``;" | Out-Null
    }
    Invoke-DockerMysqlText -Sql "CREATE DATABASE ``$SourceDatabase`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" | Out-Null
    $result.provisioning.created = $true
    $result.provisioning.flowable_tables_after_create = Get-FlowableTableCount -Database $SourceDatabase

    if ($RunRealChainIt) {
        $env:ZHICLOUD_CRM_BPM_REAL_CHAIN_IT = 'true'
        $env:ZHICLOUD_CRM_BPM_IT_MYSQL_URL = $jdbcUrl
        $env:ZHICLOUD_CRM_BPM_IT_MYSQL_USERNAME = 'root'
        $env:ZHICLOUD_CRM_BPM_IT_MYSQL_PASSWORD = $rootPassword

        $run = Invoke-BoundedNative -FilePath $Maven.Source -Arguments $mavenArgs `
            -TimeoutSeconds $script:NativeTimeoutSeconds -Stage 'maven-real-chain-it' -WorkingDirectory $repo
        $exitCode = $run.ExitCode
        $logLines = @((($run.StdOut + [Environment]::NewLine + $run.StdErr)) -split "\r?\n")
        $logText = (($logLines | ForEach-Object { [string]$_ }) -join "`n")
        $runMatches = [regex]::Matches($logText, 'Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)')
        if ($runMatches.Count -gt 0) {
            $match = $runMatches[$runMatches.Count - 1]
            $result.test.tests_run = [int]$match.Groups[1].Value
            $result.test.failures = [int]$match.Groups[2].Value
            $result.test.errors = [int]$match.Groups[3].Value
            $result.test.skipped = [int]$match.Groups[4].Value
        }
        if ($logText -match 'BUILD SUCCESS') { $result.test.build = 'SUCCESS' }
        elseif ($logText -match 'BUILD FAILURE') { $result.test.build = 'FAILURE' }
        $result.test.exit_code = [int]$exitCode

        if ($exitCode -ne 0) { throw "真链路 IT 未通过（exit=$exitCode build=$($result.test.build)），源库不能作为动态数据源验证的前置" }
        # SKIP 绝不算产出：IT 未被启用时会以 "Skipped" 通过构建，此时库里仍然一张表都没有。
        if ($result.test.tests_run -ne 2) { throw "真链路 IT 运行数不是 2（实际 $($result.test.tests_run)）" }
        if ($result.test.skipped -ne 0) { throw "真链路 IT 出现 skipped=$($result.test.skipped)：SKIP 不等于建表成功" }
    }

    $finalTables = Get-FlowableTableCount -Database $SourceDatabase
    $propertyTableExists = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SourceDatabase' AND table_name='ACT_GE_PROPERTY';")
    $finalRows = 0
    if ($propertyTableExists -gt 0) {
        $finalRows = [int](Invoke-DockerMysqlScalar -Sql "SELECT COUNT(*) FROM ``$SourceDatabase``.ACT_GE_PROPERTY;")
    }
    $result.provisioning.flowable_tables_final = $finalTables
    $result.provisioning.property_rows_final = $finalRows
    if ($finalTables -ne $ExpectedFlowableTables) {
        throw "$SourceDatabase 的 Flowable 表数不是 $ExpectedFlowableTables（实际 $finalTables，模式=$($result.provisioning.schema_producer)）"
    }
    if ($finalRows -ne $ExpectedPropertyRows) {
        throw "$SourceDatabase 的 ACT_GE_PROPERTY 行数不是 $ExpectedPropertyRows（实际 $finalRows，模式=$($result.provisioning.schema_producer)）"
    }

    $result.status = 'passed'
} catch {
    $result.status = 'failed'
    $result.error = $_.Exception.Message
} finally {
    foreach ($name in $previousEnv.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousEnv[$name], 'Process')
    }
    if ($logLines.Count -gt 0) {
        [System.IO.File]::WriteAllLines($logPath, @($logLines | ForEach-Object { [string]$_ }), [System.Text.UTF8Encoding]::new($false))
        $result.artifacts.raw_log_sha256 = Get-Sha256 -Path $logPath
    }
    $result.artifacts.script_sha256 = Get-Sha256 -Path $scriptPath
    $itSource = Join-Path $repo "zhicloud-module-crm\src\test\java\cn\zhicloud\module\crm\service\receivable\$testClass.java"
    if (Test-Path -LiteralPath $itSource -PathType Leaf) { $result.artifacts.it_source_sha256 = Get-Sha256 -Path $itSource }
    [System.IO.File]::WriteAllText($jsonPath, ($result | ConvertTo-Json -Depth 12), [System.Text.UTF8Encoding]::new($false))
    $jsonHash = Get-Sha256 -Path $jsonPath
    [System.IO.File]::WriteAllText($jsonShaPath, ($jsonHash + ' *' + [System.IO.Path]::GetFileName($jsonPath) + "`n"), [System.Text.UTF8Encoding]::new($false))
}

Write-Output ('evidence=' + $jsonPath)
Write-Output ('status=' + $result.status)
if ($result.status -ne 'passed') { exit 1 }
