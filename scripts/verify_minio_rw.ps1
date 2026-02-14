param(
    [string]$Endpoint = "http://localhost:9000",
    [string]$EnvFile = "ops/docker/.env"
)

$ErrorActionPreference = "Stop"

$dotenv = Join-Path $PSScriptRoot "_dotenv.ps1"
if (-not (Test-Path $dotenv)) {
    throw "Missing helper: $dotenv"
}
. $dotenv

function Convert-EndpointForContainer([string]$Endpoint) {
    try {
        $u = [uri]$Endpoint
    } catch {
        throw "Invalid Endpoint: $Endpoint"
    }

    $endpointHost = $u.Host
    if ($endpointHost -eq "localhost" -or $endpointHost -eq "127.0.0.1") {
        # Docker Desktop: containers can reach host services via host.docker.internal
        $endpointHost = "host.docker.internal"
    }

    $portPart = if ($u.IsDefaultPort) { "" } else { ":" + $u.Port }
    return ($u.Scheme + "://" + $endpointHost + $portPart)
}

Load-DotEnv $EnvFile

$endpointInContainer = Convert-EndpointForContainer $Endpoint

$accessKey = $env:MINIO_ROOT_USER
$secretKey = $env:MINIO_ROOT_PASSWORD
$bucket = if (-not [string]::IsNullOrWhiteSpace($env:MINIO_BUCKET)) { $env:MINIO_BUCKET } else { $env:MINIO_BUCKET_NAME }

if ([string]::IsNullOrWhiteSpace($accessKey) -or [string]::IsNullOrWhiteSpace($secretKey)) {
    throw "Missing MINIO_ROOT_USER/MINIO_ROOT_PASSWORD in env."
}
if ([string]::IsNullOrWhiteSpace($bucket)) {
    throw "Missing MINIO_BUCKET (or MINIO_BUCKET_NAME) in env."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker not found in PATH."
}

$tmpDir = Join-Path $env:TEMP ("easypan_minio_rw_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$guid = [guid]::NewGuid().ToString("N")
$src = Join-Path $tmpDir "src.txt"
$dst = Join-Path $tmpDir "dst.txt"
$content = "minio-rw-smoke-$guid"
Set-Content -Path $src -Value $content -Encoding UTF8 -NoNewline

$srcHash = (Get-FileHash -Algorithm SHA256 -Path $src).Hash
$object = "smoke/minio_rw_$guid.txt"

# Use a disposable mc container to avoid requiring host installs.
# host.docker.internal is supported by Docker Desktop (Windows/macOS).
$mcCmd = @"
set -e
mc alias set local "$endpointInContainer" "$accessKey" "$secretKey" > /dev/null
mc mb --ignore-existing "local/$bucket" > /dev/null
mc cp /tmp/src.txt "local/$bucket/$object" > /dev/null
mc cp "local/$bucket/$object" /tmp/dst.txt > /dev/null
mc rm --force "local/$bucket/$object" > /dev/null
"@

try {
    docker run --rm `
        --entrypoint sh `
        -v "${tmpDir}:/tmp" `
        minio/mc:latest `
        -c $mcCmd | Out-Null

    if (-not (Test-Path $dst)) {
        throw "MinIO download did not produce output file."
    }

    $dstHash = (Get-FileHash -Algorithm SHA256 -Path $dst).Hash
    if ($dstHash -ne $srcHash) {
        throw "MinIO read/write mismatch (hash differs)."
    }

    Write-Output "PASS: MinIO can write/read objects from bucket '$bucket'."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
