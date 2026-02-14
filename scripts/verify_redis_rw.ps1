param(
    [string]$EnvFile = "ops/docker/.env",
    [string]$RedisContainer = "easypan-redis"
)

$ErrorActionPreference = "Stop"

$dotenv = Join-Path $PSScriptRoot "_dotenv.ps1"
if (-not (Test-Path $dotenv)) {
    throw "Missing helper: $dotenv"
}
. $dotenv

Load-DotEnv $EnvFile

$redisPassword = if (-not [string]::IsNullOrWhiteSpace($env:SPRING_DATA_REDIS_PASSWORD)) {
    $env:SPRING_DATA_REDIS_PASSWORD
} elseif (-not [string]::IsNullOrWhiteSpace($env:REDIS_PASSWORD)) {
    $env:REDIS_PASSWORD
} else {
    ""
}

if ([string]::IsNullOrWhiteSpace($redisPassword)) {
    throw "Missing Redis password. Provide REDIS_PASSWORD (or SPRING_DATA_REDIS_PASSWORD) in env."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker not found in PATH."
}

function Invoke-Redis([string[]]$RedisArgs) {
    # PowerShell may treat stderr output from native commands as non-terminating errors.
    # We rely on exit code instead, and keep output for diagnostics.
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = docker exec $RedisContainer redis-cli --no-auth-warning -a $redisPassword @RedisArgs 2>&1
    $exit = $LASTEXITCODE
    $ErrorActionPreference = $oldEap

    if ($exit -ne 0) {
        throw "redis-cli failed: $out"
    }
    return ($out | Select-Object -First 1).Trim()
}

$ping = Invoke-Redis @("PING")
if ($ping -ne "PONG") {
    throw "Redis PING unexpected response: $ping"
}

$guid = [guid]::NewGuid().ToString("N")
$key = "easypan:smoke:redis:$guid"
$value = "redis-rw-smoke-$guid"

try {
    $null = Invoke-Redis @("SET", $key, $value)
    $got = Invoke-Redis @("GET", $key)
    if ($got -ne $value) {
        throw "Redis GET mismatch."
    }

    $del = Invoke-Redis @("DEL", $key)
    if ($del -ne "1") {
        throw "Redis DEL unexpected response: $del"
    }

    Write-Output "PASS: Redis can read/write keys."
} catch {
    try { Invoke-Redis @("DEL", $key) | Out-Null } catch { }
    throw
}
