[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$Image = 'maven:3.9.11-eclipse-temurin-21@sha256:6fdc855a6ed81d288ca7ca37ac6ff5e9308b612485c0801d70b25a858c83d237',
    [string[]]$MavenArgs = @('clean', 'verify'),
    [switch]$DryRun
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

& docker @arguments
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    throw "Containerized Maven build failed with exit code $exitCode"
}
