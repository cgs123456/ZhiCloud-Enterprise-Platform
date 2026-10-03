#requires -Version 5.1
<#
.SYNOPSIS
  清理被中断的运行留在 %TEMP% 的明文口令临时文件（R-043 的补偿控制）。

.DESCRIPTION
  verify_mysql_runner_sandbox.ps1 用 `--env-file` 把口令移出命令行（移出 argv 是必要的：执行器会把
  命令写进日志，`--env KEY=VALUE` 会泄进证据）。代价是：口令此刻改由**磁盘上的临时文件**承载，
  而脚本自己的 `Remove-Item` 只在正常退出时执行 —— 进程被强杀时不执行。
  2026-10-01 实测到两份这样的孤儿文件（各 340 B，含 MASTER_DATASOURCE_PASSWORD），来源正是
  被我强行终止的那批运行（handoff §12.45）。

  OS 层面拿不到"任何情况下都被执行"的退出钩子，所以这里只做**补偿控制**：
  下次正常运行时清扫同前缀、且已经老到不可能是并发运行的孤儿文件。

  刻意保守的三条：
    1) 只认 `zhicloud-runner-env-<32 位十六进制>.txt` 这个精确形状（GUID('N') + .txt），
       前缀相似的用户文件一律不碰；
    2) 只删**超过阈值年龄**的文件（默认 24 小时）——并发运行的新文件因此天然安全；
    3) 只报告不假设：返回被删清单，由调用方写进证据；删除失败不当错误吞掉。

  这不是"泄漏已消除"：它缩小的是被中断运行的残留窗口。真正消除要改的是密钥传递方式，
  属于 R-042/R-043 的口径裁决范围。
#>

function Get-StaleRunnerEnvFile {
    <#
      纯筛选：给定文件名与年龄，返回是否属于应清扫的孤儿。
      单独可测，是因为"形状判定"和"文件系统操作"混在一起时，测试只能对着字符串点头。
    #>
    param(
        [Parameter(Mandatory = $true)][string]$FileName,
        [Parameter(Mandatory = $true)][string]$DirectoryPath,
        [Parameter(Mandatory = $true)][datetime]$Now,
        [int]$MinAgeHours = 24
    )

    if ([string]::IsNullOrWhiteSpace($DirectoryPath)) { throw 'DirectoryPath 为空：拒绝在未知目录里判定' }
    if ($MinAgeHours -lt 1) { throw "MinAgeHours 必须为正（当前 $MinAgeHours）：0 会删掉并发运行正在用的文件" }
    if ($FileName -notmatch '^zhicloud-runner-env-[0-9a-f]{32}\.txt$') { return $null }

    $full = [System.IO.Path]::Combine($DirectoryPath, $FileName)
    if (-not [System.IO.File]::Exists($full)) { return $null }
    $lastWrite = [System.IO.File]::GetLastWriteTimeUtc($full)
    $ageHours = ($Now.ToUniversalTime() - $lastWrite).TotalHours
    if ($ageHours -lt $MinAgeHours) { return $null }
    return [pscustomobject]@{ Path = $full; Name = $FileName; AgeHours = [Math]::Round($ageHours, 2) }
}

function Clear-StaleRunnerEnvFiles {
    param(
        [string]$DirectoryPath = $env:TEMP,
        [int]$MinAgeHours = 24,
        [datetime]$Now = (Get-Date)
    )

    if ([string]::IsNullOrWhiteSpace($DirectoryPath)) { throw 'DirectoryPath 为空：拒绝在未知目录里删文件' }
    if (-not [System.IO.Directory]::Exists($DirectoryPath)) {
        return [pscustomobject]@{ Directory = $DirectoryPath; Candidates = 0; Removed = @(); Failed = @(); Skipped = 'directory-missing' }
    }

    $result = [ordered]@{
        directory      = $DirectoryPath
        min_age_hours  = [int]$MinAgeHours
        scanned_files  = 0
        candidates     = 0
        removed        = @()
        failed         = @()
    }

    foreach ($file in @(Get-ChildItem -LiteralPath $DirectoryPath -File -ErrorAction SilentlyContinue)) {
        $result['scanned_files'] = [int]$result['scanned_files'] + 1
        $stale = Get-StaleRunnerEnvFile -FileName $file.Name -DirectoryPath $file.DirectoryName -Now $Now -MinAgeHours $MinAgeHours
        if ($null -eq $stale) { continue }
        $result['candidates'] = [int]$result['candidates'] + 1
        try {
            [System.IO.File]::Delete($stale.Path)
            $result['removed'] += , [ordered]@{ name = $stale.Name; age_hours = $stale.AgeHours }
        } catch {
            # 删除失败必须显式记录：静默吞掉就等于"声称已清扫"而实际没清扫。
            $result['failed'] += , [ordered]@{ name = $stale.Name; error = $_.Exception.Message }
        }
    }

    return [pscustomobject]$result
}
