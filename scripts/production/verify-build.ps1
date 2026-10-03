# 退出码约定：0=构建证据合格；2=证据不合格（构建失败/测试失败/缺少工具链或报告）；1=用法或内部错误。
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [Parameter(Mandatory = $true)]
    [string]$BuildLog,
    [Parameter(Mandatory = $true)]
    [int]$BuildExitCode,
    [string]$Output,
    # 默认使用与 build-container.ps1 相同的 digest 锁定镜像，避免证据里记录可变 tag。
    [string]$Image = 'maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237'
)

$ErrorActionPreference = 'Stop'

# Windows PowerShell 5.1 下 $PSScriptRoot 在 param 默认值里可能为空，因此统一在运行期解析脚本目录。
function Get-ScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    if ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
        return (Split-Path -Parent $MyInvocation.MyCommand.Path)
    }
    throw 'Unable to resolve script directory.'
}

function Get-FileSha256 {
    param([Parameter(Mandatory = $true)][string]$Path)

    # 用 .NET API 而不是 Get-FileHash：实测某些会话（PSModulePath 被外部运行时污染）
    # 会让 powershell.exe 5.1 加载到不含 Get-FileHash 的 Microsoft.PowerShell.Utility。
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try {
            $hashBytes = $sha256.ComputeHash($stream)
        }
        finally {
            $stream.Dispose()
        }
    }
    finally {
        $sha256.Dispose()
    }
    return [System.BitConverter]::ToString($hashBytes).Replace('-', '').ToLowerInvariant()
}

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
}
if ([string]::IsNullOrWhiteSpace($Output)) {
    # 证据默认落在仓库同级目录 docs/enterprise-platform/evidence/production 下。
    $Output = Join-Path (Split-Path -Parent ([System.IO.Path]::GetFullPath($RepoRoot))) 'docs\enterprise-platform\evidence\production\verify-build.json'
}


$resolvedRepo = [System.IO.Path]::GetFullPath($RepoRoot)
$resolvedLog = [System.IO.Path]::GetFullPath($BuildLog)
$resolvedOutput = [System.IO.Path]::GetFullPath($Output)

if (-not (Test-Path -LiteralPath $resolvedRepo -PathType Container)) {
    throw "RepoRoot not found: $resolvedRepo"
}
if (-not (Test-Path -LiteralPath (Join-Path $resolvedRepo 'pom.xml') -PathType Leaf)) {
    throw "pom.xml not found under RepoRoot: $resolvedRepo"
}
if (-not (Test-Path -LiteralPath $resolvedLog -PathType Leaf)) {
    throw "Build log not found: $resolvedLog"
}

# 用 .NET 读取：Windows PowerShell 5.1 的 Get-Content 默认按 ANSI 解码，
# 且部分会话中 Microsoft.PowerShell.Utility 会被替换成缺 cmdlet 的版本。
$logText = [System.IO.File]::ReadAllText($resolvedLog)
$mavenMatch = [regex]::Match($logText, 'Apache Maven\s+([^\s(]+)')
$javaMatch = [regex]::Match($logText, 'Java version:\s*([^,\r\n]+)')
$mavenVersion = if ($mavenMatch.Success) { $mavenMatch.Groups[1].Value.Trim() } else { $null }
$javaVersion = if ($javaMatch.Success) { $javaMatch.Groups[1].Value.Trim() } else { $null }
$buildSucceeded = [regex]::IsMatch($logText, '(?m)^\s*(?:\[INFO\]\s+)?BUILD SUCCESS\s*$')

$reports = @()
$reportFiles = @(Get-ChildItem -LiteralPath $resolvedRepo -Recurse -File -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match '[\\/]target[\\/]surefire-reports[\\/]' })
foreach ($reportFile in $reportFiles) {
    try {
        [xml]$xml = [System.IO.File]::ReadAllText($reportFile.FullName)
        $suite = $xml.testsuite
        if ($null -eq $suite) {
            throw 'Missing testsuite root element'
        }
        $reports += [ordered]@{
            path = $reportFile.FullName.Substring($resolvedRepo.Length).TrimStart('\', '/')
            name = [string]$suite.name
            tests = [int]$suite.tests
            failures = [int]$suite.failures
            errors = [int]$suite.errors
            skipped = [int]$suite.skipped
        }
    }
    catch {
        $reports += [ordered]@{
            path = $reportFile.FullName.Substring($resolvedRepo.Length).TrimStart('\', '/')
            name = $reportFile.BaseName
            parse_error = $_.Exception.Message
            tests = 0
            failures = 0
            errors = 1
            skipped = 0
        }
    }
}

$testTotal = 0
$failureTotal = 0
$errorTotal = 0
$skippedTotal = 0
foreach ($report in $reports) {
    $testTotal += [int]$report.tests
    $failureTotal += [int]$report.failures
    $errorTotal += [int]$report.errors
    $skippedTotal += [int]$report.skipped
}
$tests = [ordered]@{
    tests = $testTotal
    failures = $failureTotal
    errors = $errorTotal
    skipped = $skippedTotal
}

$failureReasons = @()
if ($BuildExitCode -ne 0) { $failureReasons += 'build_exit_code' }
if (-not $buildSucceeded) { $failureReasons += 'build_success_marker' }
if ([string]::IsNullOrWhiteSpace($mavenVersion) -or [string]::IsNullOrWhiteSpace($javaVersion)) {
    $failureReasons += 'toolchain_evidence'
}
if ($reportFiles.Count -eq 0) { $failureReasons += 'surefire_reports_missing' }
if ($tests.failures -gt 0 -or $tests.errors -gt 0) { $failureReasons += 'surefire_failures' }

$commitSha = $null
try {
    # 不要把 git 直接接进 `| Select-Object -First 1`：PowerShell 会提前终止原生管道，
    # $LASTEXITCODE 会变成非 0（实测 -1），从而把合法仓库的 HEAD SHA 误判为不可用。
    $gitOutput = & git -C $resolvedRepo rev-parse HEAD 2>$null
    $gitExitCode = $LASTEXITCODE
    if ($gitExitCode -eq 0 -and $gitOutput) {
        $candidateSha = ([string]($gitOutput | Select-Object -First 1)).Trim()
        if ($candidateSha -match '^[0-9a-fA-F]{40,64}$') {
            $commitSha = $candidateSha.ToLowerInvariant()
        }
    }
}
catch {
    $commitSha = $null
}

$logHash = Get-FileSha256 -Path $resolvedLog
$status = if ($failureReasons.Count -eq 0) { 'passed' } else { 'failed' }
$evidence = [ordered]@{
    status = $status
    generated_at = [DateTimeOffset]::UtcNow.ToString('o')
    repo_root = $resolvedRepo
    commit_sha = $commitSha
    build_exit_code = $BuildExitCode
    build_success = $buildSucceeded
    image = $Image
    toolchain = [ordered]@{
        maven = $mavenVersion
        java = $javaVersion
    }
    build_log = [ordered]@{
        path = $resolvedLog
        sha256 = $logHash
    }
    surefire = [ordered]@{
        report_count = $reportFiles.Count
        reports = $reports
    }
    tests = $tests
    failure_reasons = $failureReasons
}

$outputDirectory = Split-Path -Parent $resolvedOutput
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
}
# 用 .NET 写出无 BOM 的 UTF-8：Windows PowerShell 5.1 的 Set-Content -Encoding utf8
# 会写入 BOM（EF BB BF），破坏严格 JSON 解析器的读取。
$jsonText = ($evidence | ConvertTo-Json -Depth 10) + "`n"
[System.IO.File]::WriteAllText($resolvedOutput, $jsonText, [System.Text.UTF8Encoding]::new($false))

if ($status -eq 'passed') {
    Write-Output "Build evidence passed: $resolvedOutput"
    exit 0
}

# 不能在这里用 Write-Error：脚本顶部 $ErrorActionPreference='Stop' 会让它变成终止性错误，
# 实际退出码会变成 1，破坏“0=通过 / 2=证据不合格 / 1=用法或内部错误”的约定。
[Console]::Error.WriteLine("Build evidence failed: $($failureReasons -join ', ')")
exit 2


