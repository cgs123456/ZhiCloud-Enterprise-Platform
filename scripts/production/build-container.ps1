[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$Image = 'maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237',
    [string[]]$MavenArgs = @('clean', 'verify'),
    [int]$TimeoutSeconds = 7200,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
if ($TimeoutSeconds -le 0) {
    throw 'TimeoutSeconds 必须大于 0：本脚本会在容器里跑整场 Maven 构建，不允许无硬超时'
}

# Windows PowerShell 5.1 下 $PSScriptRoot 在 param 默认值里可能为空，因此统一在运行期解析脚本目录。
function Get-ScriptDirectory {
    if (-not [string]::IsNullOrWhiteSpace($PSScriptRoot)) { return $PSScriptRoot }
    if (-not [string]::IsNullOrWhiteSpace($PSCommandPath)) { return (Split-Path -Parent $PSCommandPath) }
    if ($MyInvocation -and $MyInvocation.MyCommand -and $MyInvocation.MyCommand.Path) {
        return (Split-Path -Parent $MyInvocation.MyCommand.Path)
    }
    throw 'Unable to resolve script directory.'
}

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
}


if ($Image -match '(^|:)latest$') {
    throw "Refusing mutable image tag 'latest'. Pin a Maven/JDK version."
}
if ($Image -notmatch ':[^/]+$') {
    throw "Docker image must include an explicit version tag."
}

$resolvedRepo = [System.IO.Path]::GetFullPath($RepoRoot)
$pomPath = Join-Path $resolvedRepo 'pom.xml'
$settingsPath = Join-Path $resolvedRepo 'scripts\production\maven-settings.xml'
if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
    throw "pom.xml not found under RepoRoot: $resolvedRepo"
}
if (-not (Test-Path -LiteralPath $settingsPath -PathType Leaf)) {
    throw "Maven settings not found: $settingsPath"
}

$cacheVolume = 'zhicloud-maven-cache'
# 唯一的原生进程执行器：`& docker run` 没有原生超时，此前 $TimeoutSeconds（现在才有）
# 与镜像拉取/依赖下载阶段毫无关系——构建挂住就等于门禁挂住（登记的 R-G3.5-2 / R-G4-2 同一类）。
. (Join-Path (Get-ScriptDirectory) 'SandboxNative.ps1')
$containerName = 'zhicloud-build-' + ([guid]::NewGuid().ToString('N').Substring(0, 8))
$arguments = @(
    'run',
    '--rm',
    '--name', $containerName,
    '-v', "${resolvedRepo}:/workspace",
    '-w', '/workspace',
    '-v', "${cacheVolume}:/root/.m2",
    $Image,
    'mvn', '-B', '-V', '-s', '/workspace/scripts/production/maven-settings.xml'
) + $MavenArgs
$commandLine = 'docker ' + ($arguments -join ' ')

if ($DryRun) {
    [ordered]@{
        image = $Image
        repo_root = $resolvedRepo
        cache_volume = $cacheVolume
        container_name = $containerName
        command_line = $commandLine
        arguments = $arguments
    } | ConvertTo-Json -Depth 6 -Compress
    exit 0
}

# `--rm` 只在 docker 客户端活着陪到容器退出时才保证清理。硬超时杀的是**客户端**，
# 容器可能继续在后台占着 zhicloud-maven-cache 卷与 CPU，所以失败路径必须按精确名字强制移除。
function Remove-BuildContainer {
    param([Parameter(Mandatory = $true)][string]$ContainerName)
    $null = Invoke-BoundedNative -FilePath 'docker' -Arguments @('rm', '-f', $ContainerName) `
        -TimeoutSeconds 60 -Stage 'cleanup'
}

try {
    $run = Invoke-BoundedNative -FilePath 'docker' -Arguments $arguments `
        -TimeoutSeconds $TimeoutSeconds -Stage 'maven-build-container'
} catch {
    Remove-BuildContainer -ContainerName $containerName
    throw
}
# 执行器会等进程结束再一次性交回输出，因此构建日志不再是逐行实时滚动；
# 换来的是"挂住的构建会在 $TimeoutSeconds 秒被杀进程树"。先回显再判失败，否则失败的构建
# 只剩一句退出码，日志永远看不到。顺序仍为 stdout 后 stderr。
if ($run.StdOut) { Write-Output $run.StdOut }
if ($run.StdErr) { Write-Warning $run.StdErr }
$exitCode = $run.ExitCode
if ($exitCode -ne 0) {
    Remove-BuildContainer -ContainerName $containerName
    throw "Containerized Maven build failed with exit code $exitCode"
}
