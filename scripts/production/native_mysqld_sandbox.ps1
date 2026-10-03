#requires -Version 5.1
<#
.SYNOPSIS
  启停"宿主原生 mysqld 专用实例"（R-040 方案 B 的仓库内路径）。

.DESCRIPTION
  方案 B 的目的：让真链路 IT 与动态数据源验证器不经过"宿主 → Docker 发布端口"那段抖动路径
  （handoff §12.40 量到直连 0/3、中继 1/3 连接成功，而容器内 SELECT 1 正常）。
  §12.62/§12.63 已证明这条路能跑通，但启停一直是手敲命令 —— 按 §12.18.3 给自己立的标准，
  "只能靠人记着敲"的通道不算仓库内产出路径，所以落成脚本，并把踩过的危险写成守卫：

    * 不复用别人的数据目录：datadir 必须带本脚本写的标记文件，否则拒绝（§12.60 那 71 个 amz_* 库）；
    * 不碰默认端口：3306 直接拒绝，只允许专用高位端口；
    * 只听回环：bind-address 固定 127.0.0.1，不提供改成的口子；
    * 不把数据或凭据写进仓库工作树：BaseDir 落在仓库内即拒绝（R-042 同方向）；
    * stop 前必须核对 PID 是 mysqld.exe 且命令行含我们的 datadir，对不上就拒绝；
    * start 后必须跑隔离对照（实例内不得有 amz_* 库），失败即报错退出。

  口令只写进 BaseDir 旁边的 .pw，绝不出现在命令行或证据里。
  §12.63 的自坑之一：删掉 .pw 等于让那份 datadir 变成砖，所以 stop 不删它，只有 purge -Force 才删。
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][ValidateSet('start', 'stop', 'status', 'purge')][string]$Command,
    [string]$BaseDir,
    [string]$MysqlBaseDir = 'C:\tools\mysql8\mysql-8.0.39-winx64',
    [int]$Port = 13306,
    [string]$RepoRoot,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$MarkerName = '.zhicloud-native-sandbox'
$script:NativeTimeoutSeconds = 300
. (Join-Path $PSScriptRoot 'SandboxNative.ps1')

function Get-RepoRootPath {
    if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
        return (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
    }
    return [System.IO.Path]::GetFullPath((Resolve-Path -LiteralPath $RepoRoot).Path)
}

function Test-PathUnder {
    param([Parameter(Mandatory = $true)][string]$Path, [Parameter(Mandatory = $true)][string]$Prefix)
    $prefixTrim = $Prefix.TrimEnd('\', '/')
    if ($Path.Length -le $prefixTrim.Length) { return $false }
    return $Path.Substring(0, $prefixTrim.Length + 1).Equals(
        $prefixTrim + [System.IO.Path]::DirectorySeparatorChar,
        [System.StringComparison]::OrdinalIgnoreCase)
}

# ---- 所有命令共用的危险面守卫 ----
if ($Port -eq 3306) {
    throw '拒绝 3306：本机默认端口可能属于其它项目的 mysqld，本脚本只管理自己起的专用实例'
}
if ($Port -lt 1024 -or $Port -gt 65535) {
    throw "端口 $Port 越界：只允许 1024-65535"
}
$repo = Get-RepoRootPath
if ([string]::IsNullOrWhiteSpace($BaseDir)) {
    $BaseDir = Join-Path $env:TEMP 'zhicloud-native-mysqld'
}
$BaseDir = [System.IO.Path]::GetFullPath($BaseDir)
$dataDir = Join-Path $BaseDir 'data'
if ((Test-PathUnder -Path $BaseDir -Prefix $repo) -or (Test-PathUnder -Path $dataDir -Prefix $repo)) {
    throw "拒绝把原生实例目录放在仓库工作树（$repo）内：数据与凭据不得进入版本库"
}
$marker = Join-Path $BaseDir $MarkerName
$pwFile = Join-Path $BaseDir '.pw'
$logDir = Join-Path $BaseDir 'log'
$mysqldBin = Join-Path (Join-Path $MysqlBaseDir 'bin') 'mysqld.exe'
$mysqlBin = Join-Path (Join-Path $MysqlBaseDir 'bin') 'mysql.exe'
$adminBin = Join-Path (Join-Path $MysqlBaseDir 'bin') 'mysqladmin.exe'

function Get-NativeServerPid {
    # 端口 → 拥有者 PID 只能问内核；netstat 是唯一稳定给 PID 的入口。
    $netstatBin = Join-Path $env:SystemRoot 'System32\netstat.exe'
    $probe = Invoke-BoundedNative -FilePath $netstatBin -Arguments @('-ano', '-p', 'TCP') `
        -TimeoutSeconds 60 -Stage 'netstat'
    if ($probe.ExitCode -ne 0) { throw "netstat 退出码 $($probe.ExitCode)：$($probe.StdErr.Trim())" }
    $pattern = '127\.0\.0\.1:' + [regex]::Escape([string]$Port) + '\s.*LISTENING\s+(\d+)'
    foreach ($line in ($probe.StdOut -split "\r?\n")) {
        if ($line -match $pattern) { return [int]$Matches[1] }
    }
    return 0
}

function Assert-OwnInstance {
    if (-not (Test-Path -LiteralPath $marker -PathType Leaf)) {
        throw "$BaseDir 没有 $MarkerName 标记：这不是本脚本创建的实例，拒绝操作（绝不把别人的数据目录当沙箱）"
    }
}

function Assert-PidIsOurMysqld {
    param([Parameter(Mandatory = $true)][int]$ProcId)
    # 本机 wmic.exe 已不存在（实测），因此走 CIM。读不到就拒绝停，而不是"看起来像就停"。
    $row = $null
    try {
        $row = Get-CimInstance -ClassName Win32_Process -Filter ('ProcessId=' + [string]$ProcId) -ErrorAction Stop
    } catch {
        throw "无法用 CIM 读取 PID $ProcId：拒绝停止任何进程（$($_.Exception.Message)）"
    }
    if ($null -eq $row) { throw "PID $ProcId 已不存在，拒绝猜测" }
    if ([string]$row.Name -ne 'mysqld.exe') {
        throw "端口 $Port 的持有者是 $([string]$row.Name)，不是 mysqld.exe：拒绝停止别人的进程"
    }
    $cmdline = [string]$row.CommandLine
    if ([string]::IsNullOrWhiteSpace($cmdline)) {
        throw "读不到 PID $ProcId 的命令行：拒绝停止，宁可留一个待人工确认的进程"
    }
    if (-not $cmdline.Contains($dataDir)) {
        throw "PID $ProcId 的命令行不含我们的 datadir（$dataDir）：这不是本脚本的实例，拒绝停止"
    }
}

switch ($Command) {

    'start' {
        foreach ($bin in @($mysqldBin, $mysqlBin, $adminBin)) {
            if (-not (Test-Path -LiteralPath $bin -PathType Leaf)) { throw "缺少原生 MySQL 组件：$bin" }
        }
        New-Item -ItemType Directory -Force -Path $BaseDir | Out-Null
        New-Item -ItemType Directory -Force -Path $logDir | Out-Null

        $dataExists = Test-Path -LiteralPath $dataDir -PathType Container
        if (-not (Test-Path -LiteralPath $marker -PathType Leaf)) {
            if ($dataExists -and @(Get-ChildItem -LiteralPath $dataDir -Force).Count -gt 0) {
                throw "datadir 已存在且非空，却没有 $MarkerName 标记：$dataDir 可能属于别人，拒绝初始化、拒绝复用、更拒绝删除"
            }
        }
        if (-not $dataExists) { New-Item -ItemType Directory -Force -Path $dataDir | Out-Null }
        if (-not (Test-Path -LiteralPath $marker -PathType Leaf)) {
            Set-Content -LiteralPath $marker -Value ('created=' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:sszzz')) -Encoding ASCII
        }

        $freshData = @(Get-ChildItem -LiteralPath $dataDir -Force).Count -eq 0
        if ($freshData) {
            $init = Invoke-BoundedNative -FilePath $mysqldBin -Arguments @(
                '--no-defaults', '--initialize-insecure',
                ('--datadir=' + $dataDir), ('--basedir=' + $MysqlBaseDir)
            ) -TimeoutSeconds 900 -Stage 'mysqld-initialize'
            if ($init.ExitCode -ne 0) {
                throw "初始化 datadir 失败（exit=$($init.ExitCode)）：$($init.StdErr.Trim()) 见 $(Join-Path $logDir 'mysqld.err')"
            }
        }

        if ((Get-NativeServerPid) -eq 0) {
            if (-not (Test-Path -LiteralPath $pwFile -PathType Leaf)) {
                Set-Content -LiteralPath $pwFile -Value ('zc' + [guid]::NewGuid().ToString('N').Substring(0, 24)) -Encoding ASCII -NoNewline
                Write-Output 'password_source=generated_file'
            } else {
                Write-Output 'password_source=existing_file'
            }
            $pw = (Get-Content -LiteralPath $pwFile -Raw).Trim()
            $errLog = Join-Path $logDir 'mysqld.err'
            $startArgs = @(
                '--no-defaults', ('--datadir=' + $dataDir), ('--basedir=' + $MysqlBaseDir),
                ('--port=' + [string]$Port), '--bind-address=127.0.0.1', '--mysqlx=OFF',
                ('--pid-file=' + (Join-Path $BaseDir 'mysqld.pid')),
                ('--log-error=' + $errLog), '--console'
            )
            $initFile = ''
            if ($freshData) {
                # 新库先以空口令引导，再用 init-file 把 root@localhost 设成 .pw 里的值：
                # SQL 走文件，绝不进命令行。该文件含明文口令，用完立刻删。
                $initFile = Join-Path $BaseDir 'init-set-password.sql'
                # SET PASSWORD 在刚 --initialize-insecure 起来的实例上不生效（实测 root 仍是空口令，
                # 后续所有连接 1045）；必须点名改 root@localhost，这与本会话手工复位成功的形式一致。
                Set-Content -LiteralPath $initFile -Value ("ALTER USER 'root'@'localhost' IDENTIFIED BY '" + $pw + "';`nFLUSH PRIVILEGES;`n") -Encoding ASCII
                $startArgs += ('--init-file=' + $initFile)
            }
            # 不重定向标准流：Start-Process 的重定向在本机 PowerShell 上会撞
            # "An item with the same key has already been added (Path/PATH)"，而 mysqld 自己有 --log-error。
            $launch = Start-Process -FilePath $mysqldBin -ArgumentList $startArgs -WindowStyle Hidden -PassThru
            Write-Output ("started_pid=" + $launch.Id)
            $ready = $false
            for ($i = 0; $i -lt 60; $i++) {
                Start-Sleep -Seconds 2
                if ((Get-NativeServerPid) -gt 0) { $ready = $true; break }
            }
            if (-not $ready) { throw "120 秒内端口 $Port 未监听：见 $errLog" }
            if ($initFile -ne '' -and (Test-Path -LiteralPath $initFile -PathType Leaf)) {
                Remove-Item -LiteralPath $initFile -Force
            }
        } else {
            Write-Output 'already_running=true'
            Assert-OwnInstance
            if (-not (Test-Path -LiteralPath $pwFile -PathType Leaf)) { throw "实例在跑但缺少 $pwFile：无法核对归属，拒绝继续" }
        }

        $pw2 = (Get-Content -LiteralPath $pwFile -Raw).Trim()
        $env:MYSQL_PWD = $pw2
        try {
            $verify = Invoke-BoundedNative -FilePath $mysqlBin -Arguments @(
                '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', ('--port=' + [string]$Port),
                '-uroot', '-N', '-B', '-e',
                "SELECT CONCAT('version=',VERSION(),' port=',@@port,' datadir=',@@datadir,' amz_schemas=',(SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name LIKE 'amz\\_%'));"
            ) -TimeoutSeconds 120 -Stage 'isolation-control'
        } finally {
            [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')
        }
        if ($verify.ExitCode -ne 0) {
            throw "隔离对照查询失败（exit=$($verify.ExitCode)）：$(($verify.StdOut + ' ' + $verify.StdErr).Trim())"
        }
        $facts = $verify.StdOut.Trim()
        Write-Output $facts
        if ($facts -notmatch 'amz_schemas=0') {
            throw "隔离对照不通过：实例内存在 amz_* 库（$facts）—— 说明这个 datadir 不是本脚本初始化的空实例"
        }
        # 服务端把 @@datadir 以双反斜杠回显（C:\Users\...），直接拿单反斜杠路径比较会把
        # 自己的合法实例判成外来实例（"守卫过严"的假阳性，实测就踩了）。
        # 第一版用 Replace('\','') 归一化也不可靠（实测分隔符被整个吃掉），
        # 因此改成"去掉所有分隔符后比较"：只关心路径本身，不关心它怎么被转义。
        $flatFacts = ($facts -split '[^A-Za-z0-9:.]') -join ''
        $flatExpected = ($dataDir -split '[^A-Za-z0-9:.]') -join ''
        if ($flatFacts.IndexOf($flatExpected, [System.StringComparison]::OrdinalIgnoreCase) -lt 0) {
            # 对照不通过时必须先把刚起的实例收掉：否则"报失败、留一个没人认领的进程"就是本轮的既成事实。
            $orphanPid = Get-NativeServerPid
            if ($orphanPid -gt 0) {
                try {
                    Assert-PidIsOurMysqld -ProcId $orphanPid
                    Stop-Process -Id $orphanPid -Force
                    Write-Output "isolated_instance_reaped_pid=$orphanPid"
                } catch {
                    Write-Output "reap_refused_or_unsafe=$($_.Exception.Message)"
                }
            }
            throw "隔离对照不通过：服务端 @@datadir 与预期不符（facts=$facts，预期 dataDir=$dataDir）"
        }
        Write-Output ("state base_dir=$BaseDir port=$Port data_dir=$dataDir")
        break
    }

    'stop' {
        Assert-OwnInstance
        $procId = Get-NativeServerPid
        if ($procId -eq 0) { Write-Output 'not_running=true'; break }
        Assert-PidIsOurMysqld -ProcId $procId
        if (-not (Test-Path -LiteralPath $pwFile -PathType Leaf)) {
            throw "缺少 $pwFile：无法在不把口令写进命令行的前提下完成 shutdown，拒绝瞎停"
        }
        $pw = (Get-Content -LiteralPath $pwFile -Raw).Trim()
        $env:MYSQL_PWD = $pw
        try {
            $shut = Invoke-BoundedNative -FilePath $adminBin -Arguments @(
                '--no-defaults', '--protocol=TCP', '--host=127.0.0.1', ('--port=' + [string]$Port), '-uroot', 'shutdown'
            ) -TimeoutSeconds 180 -Stage 'mysqladmin-shutdown'
        } finally {
            [Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')
        }
        Write-Output ("shutdown_exit=" + $shut.ExitCode)
        if ($shut.ExitCode -ne 0) { throw "mysqladmin shutdown 失败：$($shut.StdErr.Trim())" }
        for ($i = 0; $i -lt 20; $i++) {
            Start-Sleep -Seconds 1
            if ((Get-NativeServerPid) -eq 0) { Write-Output 'stopped=true'; break }
        }
        break
    }

    'status' {
        $procId = 0
        $hasMarker = Test-Path -LiteralPath $marker -PathType Leaf
        if ($hasMarker) { $procId = Get-NativeServerPid }
        Write-Output ("base_dir=$BaseDir marker=$hasMarker port=$Port pid=$procId listening=$($procId -gt 0) pw_file=$(Test-Path -LiteralPath $pwFile -PathType Leaf)")
        break
    }

    'purge' {
        Assert-OwnInstance
        if (-not $Force) {
            throw 'purge 会删除 datadir 与凭据文件（数据目录随即不可用）：确认后再加 -Force'
        }
        if ((Get-NativeServerPid) -gt 0) { throw '实例仍在运行：先 stop 再 purge' }
        Remove-Item -LiteralPath $dataDir -Recurse -Force
        Remove-Item -LiteralPath $pwFile -Force
        Remove-Item -LiteralPath $marker -Force
        Write-Output 'purged=true'
        break
    }
}

exit 0
