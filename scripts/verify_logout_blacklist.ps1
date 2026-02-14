param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode,
    [string]$EnvFile = "ops/docker/.env",
    [string]$RedisContainer = "easypan-redis"
)

$ErrorActionPreference = "Stop"

$api = Join-Path $PSScriptRoot "_easypan_api.ps1"
if (-not (Test-Path $api)) {
    throw "Missing helper: $api"
}
. $api

$dotenv = Join-Path $PSScriptRoot "_dotenv.ps1"
if (-not (Test-Path $dotenv)) {
    throw "Missing helper: $dotenv"
}
. $dotenv

if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = $env:EASYPAN_EMAIL
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = $env:EASYPAN_PASSWORD
}
if ([string]::IsNullOrWhiteSpace($Email)) {
    throw "Missing Email. Provide -Email or set EASYPAN_EMAIL."
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    throw "Missing Password. Provide -Password or set EASYPAN_PASSWORD."
}

Load-DotEnv $EnvFile
$redisPassword = if (-not [string]::IsNullOrWhiteSpace($env:REDIS_PASSWORD)) { $env:REDIS_PASSWORD } else { "" }
if ([string]::IsNullOrWhiteSpace($redisPassword)) {
    throw "Missing Redis password. Provide REDIS_PASSWORD in env."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker not found in PATH."
}

function Invoke-Redis([string[]]$RedisArgs) {
    $oldEap = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = docker exec $RedisContainer redis-cli --no-auth-warning -a $redisPassword @RedisArgs 2>&1
    $exit = $LASTEXITCODE
    $ErrorActionPreference = $oldEap

    if ($exit -ne 0) {
        throw "redis-cli failed."
    }
    return ($out | Select-Object -First 1).Trim()
}

$session = New-EasyPanSession
$login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
$token = [string]$login.token
$refreshToken = [string]$login.refreshToken

$headers = @{
    "X-Tenant-Id"   = $TenantId
    "Authorization" = ("Bearer " + $token)
}

$logoutResp = Invoke-WebRequest -Uri "$BaseUrl/logout" -Method POST -WebSession $session -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body @{}
$logoutJson = $logoutResp.Content | ConvertFrom-Json
if ($null -eq $logoutJson -or [int]$logoutJson.code -ne 200) {
    $code = if ($null -eq $logoutJson) { "null" } else { [string]$logoutJson.code }
    throw "FAIL: /logout code=$code"
}

$blacklistKey = "easypan:jwt:blacklist:" + $token
$exists = Invoke-Redis @("EXISTS", $blacklistKey)
if ($exists -ne "1") {
    throw "FAIL: blacklist key not found after logout."
}

# Refresh token should be invalidated after logout.
$refreshResp = Invoke-WebRequest -Uri "$BaseUrl/refreshToken" -Method POST -WebSession $session -Headers @{ "X-Tenant-Id" = $TenantId } `
    -ContentType "application/x-www-form-urlencoded" -Body @{ refreshToken = $refreshToken }
$refreshJson = $refreshResp.Content | ConvertFrom-Json
if ($null -eq $refreshJson) {
    throw "FAIL: /refreshToken returned empty response body."
}
if ([int]$refreshJson.code -ne 901) {
    throw "FAIL: /refreshToken expected code=901 after logout, actual=$([int]$refreshJson.code)"
}

Write-Output "PASS: /logout invalidates refreshToken and adds access token to Redis blacklist."

