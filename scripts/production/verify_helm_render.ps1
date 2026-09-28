# 退出码约定：0=Helm 渲染与离线 schema 证据合格；2=证据不合格；1=用法或内部错误。
[CmdletBinding()]
param(
    [string]$RepoRoot,
    [string]$Helm3 = 'C:\Users\Administrator\AppData\Local\zhicloud-tools\helm\v3.22.0\windows-amd64\helm.exe',
    [string]$Helm4 = 'C:\Users\Administrator\AppData\Local\zhicloud-tools\helm\v4.3.0\windows-amd64\helm.exe',
    [string]$Kubeconform = 'C:\Users\Administrator\AppData\Local\zhicloud-tools\kubeconform\v0.8.0\kubeconform.exe',
    [string]$SchemaRoot134 = 'C:\Users\Administrator\AppData\Local\zhicloud-tools\k8s-schemas\v1.34.0',
    [string]$SchemaRoot136 = 'C:\Users\Administrator\AppData\Local\zhicloud-tools\k8s-schemas\v1.36.1',
    [string]$OutputDir
)

$ErrorActionPreference = 'Stop'
$script:FailureReasons = @()

function Add-Failure {
    param([Parameter(Mandatory = $true)][string]$Reason)
    if ($script:FailureReasons -notcontains $Reason) {
        $script:FailureReasons += $Reason
    }
}

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
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $stream = [System.IO.File]::OpenRead($Path)
        try { $hashBytes = $sha256.ComputeHash($stream) }
        finally { $stream.Dispose() }
    }
    finally { $sha256.Dispose() }
    return [System.BitConverter]::ToString($hashBytes).Replace('-', '').ToLowerInvariant()
}

function Get-RelativeEvidencePath {
    param([Parameter(Mandatory = $true)][string]$Path)
    $full = [System.IO.Path]::GetFullPath($Path)
    if ($full.StartsWith($resolvedRepo, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $full.Substring($resolvedRepo.Length).TrimStart('\', '/')
    }
    return $full
}

function Invoke-CapturedProcess {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [Parameter(Mandatory = $true)][string]$StdoutPath,
        [Parameter(Mandatory = $true)][string]$StderrPath
    )
    $stdoutDirectory = Split-Path -Parent $StdoutPath
    if (-not [string]::IsNullOrWhiteSpace($stdoutDirectory)) {
        New-Item -ItemType Directory -Path $stdoutDirectory -Force | Out-Null
    }
    $stderrDirectory = Split-Path -Parent $StderrPath
    if (-not [string]::IsNullOrWhiteSpace($stderrDirectory)) {
        New-Item -ItemType Directory -Path $stderrDirectory -Force | Out-Null
    }
    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $FilePath
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $startInfo.StandardErrorEncoding = [System.Text.UTF8Encoding]::new($false)
    foreach ($argument in $Arguments) {
        [void]$startInfo.ArgumentList.Add($argument)
    }
    $process = [System.Diagnostics.Process]::Start($startInfo)
    try {
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $process.WaitForExit()
        [System.IO.File]::WriteAllText($StdoutPath, $stdoutTask.GetAwaiter().GetResult(), [System.Text.UTF8Encoding]::new($false))
        [System.IO.File]::WriteAllText($StderrPath, $stderrTask.GetAwaiter().GetResult(), [System.Text.UTF8Encoding]::new($false))
        return [int]$process.ExitCode
    } finally {
        $process.Dispose()
    }
}

function Read-TextIfExists {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path -PathType Leaf)) { return '' }
    return [System.IO.File]::ReadAllText($Path)
}

function Get-AnnotationHash {
    param([string]$Text, [string]$Key)
    $pattern = '(?m)^\s*' + [regex]::Escape($Key) + ':\s*([0-9a-f]{64})\s*$'
    $match = [regex]::Match($Text, $pattern)
    if ($match.Success) { return $match.Groups[1].Value }
    return $null
}
function Get-RenderedDocument {
    param(
        [string]$Text,
        [Parameter(Mandatory = $true)][string]$Kind,
        [Parameter(Mandatory = $true)][string]$Name
    )
    if ([string]::IsNullOrWhiteSpace($Text)) { return $null }
    $documents = [regex]::Split($Text, '(?m)^---\s*\r?\n')
    foreach ($document in $documents) {
        $kindPattern = '(?m)^kind:\s*' + [regex]::Escape($Kind) + '\s*$'
        $namePattern = '(?m)^\s*name:\s*' + [regex]::Escape($Name) + '\s*$'
        if ([regex]::IsMatch($document, $kindPattern) -and [regex]::IsMatch($document, $namePattern)) {
            return $document
        }
    }
    return $null
}

function Test-SpringFlywayDisabled {
    param([string]$Document)
    if ([string]::IsNullOrWhiteSpace($Document)) { return $false }
    $mapEntry = '(?m)^\s*SPRING_FLYWAY_ENABLED:\s*"false"\s*$'
    $containerEnv = '(?ms)^\s*-\s*name:\s*SPRING_FLYWAY_ENABLED\s*\r?\n\s*value:\s*"false"\s*$'
    return ([regex]::IsMatch($Document, $mapEntry) -or [regex]::IsMatch($Document, $containerEnv))
}

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path (Get-ScriptDirectory) '..\..')).Path
}
$resolvedRepo = [System.IO.Path]::GetFullPath($RepoRoot)
$chart = Join-Path $resolvedRepo 'deploy\helm\zhicloud'
if (-not (Test-Path -LiteralPath (Join-Path $resolvedRepo 'pom.xml') -PathType Leaf)) {
    throw "RepoRoot does not look like the ZhiCloud repository: $resolvedRepo"
}
if (-not (Test-Path -LiteralPath $chart -PathType Container)) {
    throw "Helm chart not found: $chart"
}

$evidenceRoot = Join-Path $resolvedRepo 'docs\enterprise-platform\evidence\production'
$runId = Get-Date -Format 'yyyyMMdd_HHmmss'
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $runDir = Join-Path $evidenceRoot "helm-render-$runId"
    $jsonPath = Join-Path $evidenceRoot "helm-render-$runId.json"
} else {
    $runDir = [System.IO.Path]::GetFullPath($OutputDir)
    $jsonPath = "$runDir.json"
}
New-Item -ItemType Directory -Path $runDir -Force | Out-Null

$toolSpecs = @(
    [pscustomobject]@{ name = 'helm3'; path = $Helm3 },
    [pscustomobject]@{ name = 'helm4'; path = $Helm4 },
    [pscustomobject]@{ name = 'kubeconform'; path = $Kubeconform }
)
foreach ($tool in $toolSpecs) {
    if ([string]::IsNullOrWhiteSpace($tool.path) -or -not (Test-Path -LiteralPath $tool.path -PathType Leaf)) {
        Add-Failure "tool_missing:$($tool.name)"
    }
}
foreach ($schemaRoot in @($SchemaRoot134, $SchemaRoot136)) {
    if ([string]::IsNullOrWhiteSpace($schemaRoot) -or -not (Test-Path -LiteralPath $schemaRoot -PathType Container)) {
        Add-Failure "schema_root_missing:$schemaRoot"
    }
}

$commitSha = $null
$branchName = $null
try {
    $gitOutput = & git -C $resolvedRepo rev-parse HEAD 2>$null
    if ($LASTEXITCODE -eq 0 -and $gitOutput) {
        $candidate = ([string]($gitOutput | Select-Object -First 1)).Trim()
        if ($candidate -match '^[0-9a-fA-F]{40,64}$') { $commitSha = $candidate.ToLowerInvariant() }
    }
    $branchOutput = & git -C $resolvedRepo branch --show-current 2>$null
    if ($LASTEXITCODE -eq 0 -and $branchOutput) { $branchName = ([string]($branchOutput | Select-Object -First 1)).Trim() }
} catch {
    $commitSha = $null
    $branchName = $null
}

$toolVersions = [ordered]@{}
$positiveCases = @(
    [pscustomobject]@{
        name = 'dev'; values = 'values-dev.yaml'; namespace = 'zhicloud-dev'; expectTruststore = $false
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001')
    },
    [pscustomobject]@{
        name = 'staging'; values = 'values-staging.yaml'; namespace = 'zhicloud-staging'; expectTruststore = $true
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001')
    },
    [pscustomobject]@{
        name = 'prod'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'; expectTruststore = $true
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001')
    }
)
$negativeCases = @(
    [pscustomobject]@{
        name = 'prod_missing_app_secret_revision'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001')
        expectedPattern = 'secrets\.existingSecretRevision'
    },
    [pscustomobject]@{
        name = 'prod_missing_truststore_secret_revision'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore')
        expectedPattern = 'mysqlTls\.truststore\.existingSecretRevision'
    },
    [pscustomobject]@{
        name = 'prod_missing_truststore_secret'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001')
        expectedPattern = 'mysqlTls\.truststore\.existingSecret'
    },
    [pscustomobject]@{
        name = 'prod_allow_insecure_defaults'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'
        extra = @('--set-string', 'secrets.allowInsecureDefaults=true')
        expectedPattern = '生产环境不允许 allowInsecureDefaults=true'
    },
    [pscustomobject]@{
        name = 'prod_migration_job_disabled'; values = 'values-prod.yaml'; namespace = 'zhicloud-prod'
        extra = @('--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001', '--set', 'migration.enabled=false')
        expectedPattern = 'migration\.enabled'
    }
)

$positiveRecords = @()
$schemaRecords = @()
$negativeRecords = @()
$migrationContractRecords = @()
$rotationRecord = [ordered]@{ status = 'not_run'; app_revision_changes_template = $false; truststore_revision_changes_template = $false; details = @() }

if ($script:FailureReasons.Count -eq 0) {
    $toolVersionOut = Join-Path $runDir 'tool-versions.out'
    $toolVersionErr = Join-Path $runDir 'tool-versions.err'
    $toolVersions.helm3 = (& $Helm3 version --short).Trim()
    $toolVersions.helm4 = (& $Helm4 version --short).Trim()
    $toolVersions.kubeconform = (& $Kubeconform -v).Trim()
    [System.IO.File]::WriteAllText($toolVersionOut, ("helm3=$($toolVersions.helm3)`nhelm4=$($toolVersions.helm4)`nkubeconform=$($toolVersions.kubeconform)`n"), [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllText($toolVersionErr, '', [System.Text.UTF8Encoding]::new($false))

    foreach ($helmTool in @(
        [pscustomobject]@{ name = 'helm3'; path = $Helm3 },
        [pscustomobject]@{ name = 'helm4'; path = $Helm4 }
    )) {
        foreach ($case in $positiveCases) {
            $lintOut = Join-Path $runDir "lint-$($helmTool.name)-$($case.name).out"
            $lintErr = Join-Path $runDir "lint-$($helmTool.name)-$($case.name).err"
            $lintArgs = @('lint', '--strict', $chart, '-f', (Join-Path $chart $case.values), '--namespace', $case.namespace) + $case.extra
            $lintExit = Invoke-CapturedProcess -FilePath $helmTool.path -Arguments $lintArgs -StdoutPath $lintOut -StderrPath $lintErr
            if ($lintExit -ne 0) { Add-Failure "helm_lint_failed:$($helmTool.name):$($case.name)" }

            $renderOut = Join-Path $runDir "render-$($helmTool.name)-$($case.name).yaml"
            $renderErr = Join-Path $runDir "render-$($helmTool.name)-$($case.name).err"
            $templateArgs = @('template', 'zhicloud', $chart, '-f', (Join-Path $chart $case.values), '--namespace', $case.namespace) + $case.extra
            $templateExit = Invoke-CapturedProcess -FilePath $helmTool.path -Arguments $templateArgs -StdoutPath $renderOut -StderrPath $renderErr
            if ($templateExit -ne 0) { Add-Failure "helm_template_failed:$($helmTool.name):$($case.name)" }

            $renderText = Read-TextIfExists -Path $renderOut
            $externalRevision = Get-AnnotationHash -Text $renderText -Key 'checksum/external-secret-revision'
            $truststoreRevision = Get-AnnotationHash -Text $renderText -Key 'checksum/mysql-tls-truststore-revision'
            $secretObjectRendered = [regex]::IsMatch($renderText, '(?m)^kind:\s*Secret\s*$')
            $placeholderFound = [regex]::IsMatch($renderText, 'CHANGE_ME|__REQUIRED__|\[INSECURE_DEFAULT\]')

            $configMapDocument = Get-RenderedDocument -Text $renderText -Kind 'ConfigMap' -Name 'zhicloud-config'
            $deploymentDocument = Get-RenderedDocument -Text $renderText -Kind 'Deployment' -Name 'zhicloud'
            $migrationJobDocument = Get-RenderedDocument -Text $renderText -Kind 'Job' -Name 'zhicloud-migration'
            $configMapFlywayDisabled = Test-SpringFlywayDisabled -Document $configMapDocument
            $deploymentFlywayDisabled = Test-SpringFlywayDisabled -Document $deploymentDocument
            $migrationJobPresent = -not [string]::IsNullOrWhiteSpace($migrationJobDocument)
            $migrationJobPreSync = $migrationJobPresent -and [regex]::IsMatch($migrationJobDocument, '(?m)^\s*argocd\.argoproj\.io/hook:\s*PreSync\s*$')
            $migrationContractPassed = ($configMapFlywayDisabled -and $deploymentFlywayDisabled -and $migrationJobPresent -and $migrationJobPreSync)
            if (-not $configMapFlywayDisabled) { Add-Failure "configmap_flyway_not_disabled:$($helmTool.name):$($case.name)" }
            if (-not $deploymentFlywayDisabled) { Add-Failure "deployment_flyway_not_disabled:$($helmTool.name):$($case.name)" }
            if (-not $migrationJobPresent) { Add-Failure "migration_job_missing:$($helmTool.name):$($case.name)" }
            if (-not $migrationJobPreSync) { Add-Failure "migration_job_not_presync:$($helmTool.name):$($case.name)" }

            if ($templateExit -eq 0 -and [string]::IsNullOrWhiteSpace($externalRevision)) {
                Add-Failure "external_secret_revision_annotation_missing:$($helmTool.name):$($case.name)"
            }
            if ($case.expectTruststore -and $templateExit -eq 0 -and [string]::IsNullOrWhiteSpace($truststoreRevision)) {
                Add-Failure "truststore_revision_annotation_missing:$($helmTool.name):$($case.name)"
            }
            if (-not $case.expectTruststore -and -not [string]::IsNullOrWhiteSpace($truststoreRevision)) {
                Add-Failure "unexpected_truststore_revision_annotation:$($helmTool.name):$($case.name)"
            }
            if ($secretObjectRendered) { Add-Failure "secret_object_rendered_with_existing_secret:$($helmTool.name):$($case.name)" }
            if ($placeholderFound) { Add-Failure "placeholder_rendered:$($helmTool.name):$($case.name)" }

            $positiveRecords += [ordered]@{
                helm = $helmTool.name
                environment = $case.name
                lint_exit_code = $lintExit
                template_exit_code = $templateExit
                rendered_path = Get-RelativeEvidencePath -Path $renderOut
                rendered_sha256 = if (Test-Path -LiteralPath $renderOut -PathType Leaf) { Get-FileSha256 -Path $renderOut } else { $null }
                external_secret_revision_annotation = $externalRevision
                truststore_revision_annotation = $truststoreRevision
                secret_object_rendered = $secretObjectRendered
                placeholder_rendered = $placeholderFound
            }
            $migrationContractRecords += [ordered]@{
                helm = $helmTool.name
                environment = $case.name
                configmap_flyway_disabled = $configMapFlywayDisabled
                deployment_flyway_disabled = $deploymentFlywayDisabled
                migration_job_present = $migrationJobPresent
                migration_job_pre_sync = $migrationJobPreSync
                passed = $migrationContractPassed
            }

            if ($templateExit -eq 0) {
                foreach ($schema in @(
                    [pscustomobject]@{ version = '1.34.0'; root = $SchemaRoot134 },
                    [pscustomobject]@{ version = '1.36.1'; root = $SchemaRoot136 }
                )) {
                    $kcOut = Join-Path $runDir "kubeconform-$($helmTool.name)-$($case.name)-$($schema.version).out"
                    $kcErr = Join-Path $runDir "kubeconform-$($helmTool.name)-$($case.name)-$($schema.version).err"
                    $schemaLocation = Join-Path $schema.root '{{ .ResourceKind }}{{ .KindSuffix }}.json'
                    $kcArgs = @('-strict', '-summary', '-kubernetes-version', $schema.version, '-schema-location', $schemaLocation, $renderOut)
                    $kcExit = Invoke-CapturedProcess -FilePath $Kubeconform -Arguments $kcArgs -StdoutPath $kcOut -StderrPath $kcErr
                    $kcText = Read-TextIfExists -Path $kcOut
                    $validMatch = [regex]::Match($kcText, 'Valid:\s*(\d+)')
                    $invalidMatch = [regex]::Match($kcText, 'Invalid:\s*(\d+)')
                    $errorMatch = [regex]::Match($kcText, 'Errors:\s*(\d+)')
                    $skippedMatch = [regex]::Match($kcText, 'Skipped:\s*(\d+)')
                    $validCount = if ($validMatch.Success) { [int]$validMatch.Groups[1].Value } else { 0 }
                    $invalidCount = if ($invalidMatch.Success) { [int]$invalidMatch.Groups[1].Value } else { -1 }
                    $errorCount = if ($errorMatch.Success) { [int]$errorMatch.Groups[1].Value } else { -1 }
                    $skippedCount = if ($skippedMatch.Success) { [int]$skippedMatch.Groups[1].Value } else { -1 }
                    $schemaPassed = ($kcExit -eq 0 -and $validCount -gt 0 -and $invalidCount -eq 0 -and $errorCount -eq 0)
                    if (-not $schemaPassed) { Add-Failure "kubeconform_failed:$($helmTool.name):$($case.name):$($schema.version)" }
                    $schemaRecords += [ordered]@{
                        helm = $helmTool.name
                        environment = $case.name
                        kubernetes_version = $schema.version
                        exit_code = $kcExit
                        valid = $validCount
                        invalid = $invalidCount
                        errors = $errorCount
                        skipped = $skippedCount
                        passed = $schemaPassed
                        stdout_path = Get-RelativeEvidencePath -Path $kcOut
                        stderr_path = Get-RelativeEvidencePath -Path $kcErr
                    }
                }
            }
        }

        foreach ($negative in $negativeCases) {
            $negOut = Join-Path $runDir "negative-$($helmTool.name)-$($negative.name).out"
            $negErr = Join-Path $runDir "negative-$($helmTool.name)-$($negative.name).err"
            $negArgs = @('template', 'zhicloud', $chart, '-f', (Join-Path $chart $negative.values), '--namespace', $negative.namespace) + $negative.extra
            $negExit = Invoke-CapturedProcess -FilePath $helmTool.path -Arguments $negArgs -StdoutPath $negOut -StderrPath $negErr
            $combined = (Read-TextIfExists -Path $negOut) + "`n" + (Read-TextIfExists -Path $negErr)
            $patternMatched = [regex]::IsMatch($combined, $negative.expectedPattern)
            $negativePassed = ($negExit -ne 0 -and $patternMatched)
            if (-not $negativePassed) { Add-Failure "negative_case_not_fail_closed:$($helmTool.name):$($negative.name)" }
            $negativeRecords += [ordered]@{
                helm = $helmTool.name
                name = $negative.name
                exit_code = $negExit
                expected_nonzero = $true
                expected_pattern = $negative.expectedPattern
                expected_pattern_matched = $patternMatched
                passed = $negativePassed
                stdout_path = Get-RelativeEvidencePath -Path $negOut
                stderr_path = Get-RelativeEvidencePath -Path $negErr
            }
        }
    }

    $rotationBase = @('template', 'zhicloud', $chart, '-f', (Join-Path $chart 'values-prod.yaml'), '--namespace', 'zhicloud-prod', '--set-string', 'secrets.existingSecret=zhicloud-app-secret', '--set-string', 'mysqlTls.truststore.existingSecret=zhicloud-mysql-truststore')
    $rotationA = Join-Path $runDir 'rotation-app-rev001.yaml'
    $rotationB = Join-Path $runDir 'rotation-app-rev002.yaml'
    $rotationC = Join-Path $runDir 'rotation-truststore-rev002.yaml'
    $rotationAErr = Join-Path $runDir 'rotation-app-rev001.err'
    $rotationBErr = Join-Path $runDir 'rotation-app-rev002.err'
    $rotationCErr = Join-Path $runDir 'rotation-truststore-rev002.err'
    $rotationAArgs = $rotationBase + @('--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001')
    $rotationBArgs = $rotationBase + @('--set-string', 'secrets.existingSecretRevision=rev-002', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-001')
    $rotationCArgs = $rotationBase + @('--set-string', 'secrets.existingSecretRevision=rev-001', '--set-string', 'mysqlTls.truststore.existingSecretRevision=rev-002')
    $rotationAExit = Invoke-CapturedProcess -FilePath $Helm3 -Arguments $rotationAArgs -StdoutPath $rotationA -StderrPath $rotationAErr
    $rotationBExit = Invoke-CapturedProcess -FilePath $Helm3 -Arguments $rotationBArgs -StdoutPath $rotationB -StderrPath $rotationBErr
    $rotationCExit = Invoke-CapturedProcess -FilePath $Helm3 -Arguments $rotationCArgs -StdoutPath $rotationC -StderrPath $rotationCErr
    $rotationAText = Read-TextIfExists -Path $rotationA
    $rotationBText = Read-TextIfExists -Path $rotationB
    $rotationCText = Read-TextIfExists -Path $rotationC
    $appHashA = Get-AnnotationHash -Text $rotationAText -Key 'checksum/external-secret-revision'
    $appHashB = Get-AnnotationHash -Text $rotationBText -Key 'checksum/external-secret-revision'
    $trustHashB = Get-AnnotationHash -Text $rotationBText -Key 'checksum/mysql-tls-truststore-revision'
    $trustHashC = Get-AnnotationHash -Text $rotationCText -Key 'checksum/mysql-tls-truststore-revision'
    $appRotationPassed = ($rotationAExit -eq 0 -and $rotationBExit -eq 0 -and -not [string]::IsNullOrWhiteSpace($appHashA) -and -not [string]::IsNullOrWhiteSpace($appHashB) -and $appHashA -ne $appHashB)
    $trustRotationPassed = ($rotationBExit -eq 0 -and $rotationCExit -eq 0 -and -not [string]::IsNullOrWhiteSpace($trustHashB) -and -not [string]::IsNullOrWhiteSpace($trustHashC) -and $trustHashB -ne $trustHashC)
    if (-not $appRotationPassed) { Add-Failure 'external_secret_revision_does_not_change_pod_template' }
    if (-not $trustRotationPassed) { Add-Failure 'truststore_revision_does_not_change_pod_template' }
    $rotationRecord = [ordered]@{
        status = if ($appRotationPassed -and $trustRotationPassed) { 'passed' } else { 'failed' }
        app_revision_changes_template = $appRotationPassed
        truststore_revision_changes_template = $trustRotationPassed
        app_revision_hashes = @($appHashA, $appHashB)
        truststore_revision_hashes = @($trustHashB, $trustHashC)
        details = @(
            [ordered]@{ path = Get-RelativeEvidencePath -Path $rotationA; sha256 = if (Test-Path -LiteralPath $rotationA) { Get-FileSha256 -Path $rotationA } else { $null } },
            [ordered]@{ path = Get-RelativeEvidencePath -Path $rotationB; sha256 = if (Test-Path -LiteralPath $rotationB) { Get-FileSha256 -Path $rotationB } else { $null } },
            [ordered]@{ path = Get-RelativeEvidencePath -Path $rotationC; sha256 = if (Test-Path -LiteralPath $rotationC) { Get-FileSha256 -Path $rotationC } else { $null } }
        )
    }
}

$status = if ($script:FailureReasons.Count -eq 0) { 'passed' } else { 'failed' }
$evidence = [ordered]@{
    status = $status
    generated_at = [DateTimeOffset]::UtcNow.ToString('o')
    repo_root = $resolvedRepo
    commit_sha = $commitSha
    branch = $branchName
    chart = Get-RelativeEvidencePath -Path $chart
    tools = [ordered]@{
        helm3 = $Helm3
        helm4 = $Helm4
        kubeconform = $Kubeconform
        schema_root_1_34_0 = $SchemaRoot134
        schema_root_1_36_1 = $SchemaRoot136
        versions = $toolVersions
    }
    positive_cases = $positiveRecords
    negative_cases = $negativeRecords
    schema_validation = $schemaRecords
    migration_contract = $migrationContractRecords
    rotation_contract = $rotationRecord
    server_side_dry_run = [ordered]@{
        status = 'not_run'
        reason = 'No Kubernetes API server or target cluster is available in this environment; offline schema validation does not replace kubectl --dry-run=server.'
    }
    limitations = @(
        'This evidence proves local Helm rendering, fail-closed rendering and offline Kubernetes schema validation only.',
        'It does not prove external Secret object existence, key names, RBAC, file permissions, actual Secret content or live rotation.',
        'It does not replace kubectl --dry-run=server, a staging deployment, a target-cluster rollout or a production migration.'
    )
    failure_reasons = $script:FailureReasons
}

$jsonText = ($evidence | ConvertTo-Json -Depth 20) + "`n"
[System.IO.File]::WriteAllText($jsonPath, $jsonText, [System.Text.UTF8Encoding]::new($false))
$jsonHash = Get-FileSha256 -Path $jsonPath
[System.IO.File]::WriteAllText("$jsonPath.sha256", "$jsonHash  $(Split-Path -Leaf $jsonPath)`n", [System.Text.UTF8Encoding]::new($false))

if ($status -eq 'passed') {
    Write-Output "Helm render evidence passed: $jsonPath"
    exit 0
}
[Console]::Error.WriteLine("Helm render evidence failed: $($script:FailureReasons -join ', ')")
exit 2