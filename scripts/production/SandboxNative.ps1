#requires -Version 5.1
<#
.SYNOPSIS
  沙箱类门禁脚本唯一的原生命令执行器：带硬超时，超时即杀整棵进程树。

.DESCRIPTION
  这些验证脚本会启动 docker / java / mvn / mysql 等外部进程。PowerShell 的 `&` 调用没有原生超时，
  所以各脚本的 $TimeoutSeconds 此前只停留在参数定义处：子进程一旦挂起（迁移死锁、镜像拉取卡住、
  复制追不平、导入半路阻塞），整个门禁就会无限等下去（登记为 R-G4-2 / R-G3.5-2）。

  全仓库只保留**一份**实现，由各脚本 dot-source 后在各自的咽喉函数里调用。之所以不允许每份脚本
  各写一遍：同一规则的多份实现已经在本仓库漂移过一次（check_bare_throws 与 convert_bare_throws
  对"注释里的标记"判定不一致，后果是转换脚本会改写被注释掉的代码）。

  StdOut 与 StdErr 分开返回。`2>&1` 混流会把 mysql 的告警混进查询结果，让"期望单值查询"的断言
  看到本不属于结果的数据；调用方要合并时自己合并（写入 -LogPath 时即用换行分隔）。

  超时是硬失败，不返回半截输出：拿不到完整输出就抛异常，否则调用方会把截断日志当成成功证据。

.EXAMPLE
  . (Join-Path $PSScriptRoot 'SandboxNative.ps1')
  $run = Invoke-BoundedNative -FilePath 'docker' -Arguments @('ps','-q') -TimeoutSeconds 30 -Stage 'probe'
  if ($run.ExitCode -ne 0) { throw $run.StdErr }
#>

function ConvertTo-WindowsProcessArgument {
    <#
      进程命令行是**一个字符串**，含空格的参数必须自己加引号，否则 "--password=a b" 会被拆成两个。
      反斜杠与引号的转义遵循 CommandLineToArgvW 规则（连续反斜杠在引号前必须翻倍）。
    #>
    param([AllowEmptyString()][string]$Argument)

    if ($null -eq $Argument -or $Argument.Length -eq 0) { return '""' }
    if ($Argument -notmatch '[\s"]') { return $Argument }

    $builder = New-Object System.Text.StringBuilder
    [void]$builder.Append('"')
    $backslashes = 0
    foreach ($character in $Argument.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
            continue
        }
        if ($character -eq '"') {
            [void]$builder.Append('\' * (($backslashes * 2) + 1))
            [void]$builder.Append('"')
            $backslashes = 0
            continue
        }
        if ($backslashes -gt 0) {
            [void]$builder.Append('\' * $backslashes)
            $backslashes = 0
        }
        [void]$builder.Append($character)
    }
    if ($backslashes -gt 0) {
        [void]$builder.Append('\' * ($backslashes * 2))
    }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Stop-NativeProcessTree {
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][System.Diagnostics.Process]$Process)

    # Process.Kill() 只杀直接子进程；java/mvn 派生的孙进程会继续持有数据库连接和输出管道。
    # taskkill 自身的 stderr 必须降级捕获：宿主脚本普遍是 $ErrorActionPreference='Stop'，
    # 而 5.1 下 `& 原生命令 2>&1` 会把每一行 stderr 升级成终止性异常（本仓库已因此废过一次重试逻辑）。
    #
    # 返回的是**观察到的事实**，不是 taskkill 的退出码：树里只要有一个进程刚好自己退出，
    # taskkill 就返回非 0，把"没杀干净"和"杀干净但有个进程已自己走了"混成同一件事。
    # 实测这个歧义会让超时判定偶发误报（本函数的第一版就因此抖了一次）。
    $taskKillExitCode = $null
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $null = & taskkill.exe /PID $Process.Id /T /F 2>&1
        $taskKillExitCode = $LASTEXITCODE
    } catch {
        $taskKillExitCode = -1
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    try {
        if (-not $Process.HasExited) { $Process.Kill() }
    } catch { }
    $parentExited = $false
    try { $parentExited = [bool]$Process.WaitForExit(5000) -or [bool]$Process.HasExited } catch { }
    return [pscustomobject]@{
        ParentExited     = $parentExited
        TaskKillExitCode = $taskKillExitCode
    }
}

function Invoke-BoundedNative {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        # AllowEmptyString 不是冗余：默认绑定会把集合里的空元素判为非法，
        # 而"传一个空参数"是合法需求（ConvertTo-WindowsProcessArgument 会把它编成 ""）。
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][AllowEmptyString()][string[]]$Arguments,
        [AllowNull()][string]$StandardInput,
        [Parameter(Mandatory = $true)][ValidateRange(1, 86400)][int]$TimeoutSeconds,
        [string]$Stage = 'native',
        [string]$WorkingDirectory,
        [string]$LogPath
    )
    # 只在函数作用域内开启，不改变 dot-source 它的宿主脚本语义。
    Set-StrictMode -Version 3.0

    if ([string]::IsNullOrWhiteSpace($FilePath)) { throw "阶段 $Stage 的 -FilePath 为空，拒绝执行" }

    $exeName = [System.IO.Path]::GetFileName($FilePath)
    $started = Get-Date
    $process = New-Object System.Diagnostics.Process
    try {
        $targetPath = $FilePath
        $targetArguments = @($Arguments)
        if ($exeName -match '\.(cmd|bat)$') {
            # UseShellExecute=false 的 CreateProcess 无法直接执行批处理（会报"不是有效的 Win32 应用程序"），
            # 必须让 cmd.exe 承载。mvn 在 Windows 上正是 mvn.cmd，所以这条分支不是假想需求。
            # 路径本身交给下面的统一引号化，不在此手工加引号（手工引号会被再转义一层）。
            if ([string]::IsNullOrWhiteSpace($env:ComSpec)) { throw "阶段 $Stage 要执行批处理 $FilePath，但环境里没有 ComSpec" }
            $targetPath = $env:ComSpec
            $targetArguments = @('/d', '/c', $FilePath) + @($Arguments)
        }

        $info = New-Object System.Diagnostics.ProcessStartInfo
        $info.FileName = $targetPath
        $info.Arguments = (@($targetArguments | ForEach-Object { ConvertTo-WindowsProcessArgument -Argument ([string]$_) }) -join ' ')
        $info.UseShellExecute = $false
        $info.RedirectStandardOutput = $true
        $info.RedirectStandardError = $true
        $info.CreateNoWindow = $true
        $info.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
        $info.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)
        # 注意这里不能用 StandardInputEncoding：它与 ArgumentList 同族，在 .NET Framework
        # （Windows PowerShell 5.1）上不存在，赋值直接抛 PropertyAssignmentException。
        # stdin 的编码问题必须解决：三条流里只钉输出是不够的，stdin 默认沿用控制台代码页（本机 GBK 936），
        # 而 mysql 客户端被告知按 utf8mb4 解释字节 —— 含中文注释的迁移 SQL 被写成 GBK 后，
        # 服务端读到乱码并吃掉引号，实测报 `ERROR 1064 … COMMENT '״̬NEW/…'`（本次接线引入的回归）。
        $hasStandardInput = -not [string]::IsNullOrEmpty($StandardInput)
        if ($hasStandardInput) { $info.RedirectStandardInput = $true }
        if (-not [string]::IsNullOrWhiteSpace($WorkingDirectory)) { $info.WorkingDirectory = $WorkingDirectory }
        $process.StartInfo = $info

        if (-not $process.Start()) { throw "无法启动进程 $FilePath（阶段 $Stage）" }

        # 异步读两条流：同步 ReadToEnd 会在管道写满时与子进程互相等待（死锁），
        # 而"看起来只是慢"的死锁会被误当成超时前的正常等待。
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()

        if ($hasStandardInput) {
            # 写完必须 Close：stdin 不关闭，mysql/java 会一直等输入，止损就成了自伤。
            # 子进程也可能已因参数错误提前退出，此时写入抛 IOException——吞掉它，
            # 让退出码去说明真实失败，否则 mysql 的原始报错会被一个 IO 异常遮住。
            try {
                # 显式 UTF-8 写入器包住 BaseStream：默认 StreamWriter 沿用控制台代码页（GBK）。
                $stdinWriter = New-Object System.IO.StreamWriter($process.StandardInput.BaseStream, [System.Text.UTF8Encoding]::new($false))
                $stdinWriter.Write($StandardInput)
                $stdinWriter.Flush()
                # 必须 Close：不关闭 stdin，mysql/java 会一直等输入。
                $stdinWriter.Close()
            } catch [System.IO.IOException] {
            } catch [System.InvalidOperationException] {
                # ObjectDisposedException 派生自本类型，无需（也不允许）单独再写一个 catch。
            }
        }

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $kill = Stop-NativeProcessTree -Process $process
            $message = "硬止损：阶段 $Stage 的 $exeName 超过 $TimeoutSeconds 秒未结束，已强制终止进程树（parent_exited=$($kill.ParentExited) taskkill_rc=$($kill.TaskKillExitCode)）。见 R-G4-2 / R-G3.5-2。"
            if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
                [System.IO.File]::WriteAllText($LogPath, "[timeout] $message", [System.Text.UTF8Encoding]::new($false))
            }
            throw $message
        }

        # 带毫秒的 WaitForExit 不保证重定向管道已读完；管道可能仍被未随父退出的孙进程持有。
        # 这种情况按超时同等处理——返回半截输出会让调用方把截断日志当成成功证据。
        $graceMilliseconds = [Math]::Min(30000, [Math]::Max(1000, $TimeoutSeconds * 100))
        $stdoutDone = $stdoutTask.Wait($graceMilliseconds)
        $stderrDone = $stderrTask.Wait($graceMilliseconds)
        if (-not ($stdoutDone -and $stderrDone)) {
            $kill = Stop-NativeProcessTree -Process $process
            $message = "硬止损：阶段 $Stage 的 $exeName 进程已退出，但输出管道在 $([int]($graceMilliseconds / 1000)) 秒内未关闭（孙进程仍在持有），已强制终止进程树（parent_exited=$($kill.ParentExited) taskkill_rc=$($kill.TaskKillExitCode)）。见 R-G4-2 / R-G3.5-2。"
            if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
                [System.IO.File]::WriteAllText($LogPath, "[timeout] $message", [System.Text.UTF8Encoding]::new($false))
            }
            throw $message
        }

        $stdOut = $stdoutTask.Result
        $stdErr = $stderrTask.Result
        $exitCode = $process.ExitCode

        if (-not [string]::IsNullOrWhiteSpace($LogPath)) {
            # 两条流之间留换行：直接拼接会把 stdout 末行与 stderr 首行粘成一行，
            # 让按行锚定的日志正则漏读或误读。
            $combined = ($stdOut + [Environment]::NewLine + $stdErr).Trim()
            [System.IO.File]::WriteAllText($LogPath, $combined, [System.Text.UTF8Encoding]::new($false))
        }

        return [pscustomobject]@{
            ExitCode        = $exitCode
            StdOut          = $stdOut.Trim()
            StdErr          = $stdErr.Trim()
            # 落盘用的原始流：Trim() 会吃掉结尾换行。实测这会让 37 份 Helm 渲染证据
            # 与改动前逐份差一个字节——在本仓库里"证据字节变了"必须是有意的，不能是副作用。
            RawStdOut       = $stdOut
            RawStdErr       = $stdErr
            TimedOut        = $false
            Stage           = $Stage
            DurationSeconds = [Math]::Round(((Get-Date) - $started).TotalSeconds, 2)
            TimeoutSeconds  = $TimeoutSeconds
        }
    } finally {
        try { $process.Dispose() } catch { }
    }
}
