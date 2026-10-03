# Image provenance helper (R-G3.5-3): prove that the image a sandbox container actually
# runs carries the registry digest the script pinned - not merely "some local image whose
# tag looks right".
#
# Deliberately pure functions with no subprocess: the caller already owns a hardened native
# executor (SandboxNative.ps1) that separates stdout from stderr. Running `docker ... 2>&1`
# from here would let a harmless stderr warning become a terminating error under
# $ErrorActionPreference='Stop', which is a failure mode this repository has already been
# bitten by twice.
#
# ASCII-only: this file has no BOM and Windows PowerShell 5.1 decodes BOM-less files as ANSI.

function ConvertFrom-RepoDigestsJson {
    param([AllowEmptyString()][string]$Json)
    $text = $Json.Trim()
    if ([string]::IsNullOrWhiteSpace($text) -or $text -eq '[]' -or $text -eq 'null') {
        return @()
    }
    return @(([regex]::Matches($text, '"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }))
}

function Assert-ImageDigestPinned {
    param(
        [Parameter(Mandatory = $true)][string]$ImageId,
        [AllowEmptyCollection()][array]$RepoDigests,
        [Parameter(Mandatory = $true)][string]$PinnedImage,
        [Parameter(Mandatory = $true)][string]$Stage
    )
    $match = [regex]::Match($PinnedImage, '@sha256:([0-9a-f]{64})$')
    if (-not $match.Success) {
        throw "Refusing $Stage : pinned image '$PinnedImage' carries no @sha256:<64 hex> digest."
    }
    $digest = "sha256:$($match.Groups[1].Value)"
    $digests = @($RepoDigests)
    if ($digests.Count -eq 0) {
        # An image with no RepoDigests (locally built or imported) can never prove registry
        # provenance. Passing it silently is exactly the "empty result looks like a clean
        # result" trap, so this is an error rather than a warning.
        throw "Refusing $Stage : image $ImageId carries no RepoDigests, so it cannot be matched to the pinned digest $digest. Locally built or imported images are not accepted for sandboxes."
    }
    $hits = @($digests | Where-Object { $_ -like "*@$digest" })
    if ($hits.Count -eq 0) {
        throw "Refusing $Stage : image $ImageId resolves to [$($digests -join ', ')] which does not contain the pinned digest $digest."
    }
    return [pscustomobject]@{
        image_id      = $ImageId
        pinned_digest = $digest
        repo_digests  = $digests
        matched_entry = [string]$hits[0]
    }
}
