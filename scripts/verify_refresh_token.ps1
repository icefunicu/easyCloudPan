param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode
)

$ErrorActionPreference = "Stop"

$api = Join-Path $PSScriptRoot "_easypan_api.ps1"
if (-not (Test-Path $api)) {
    throw "Missing helper: $api"
}
. $api

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

$session = New-EasyPanSession
$login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
$refreshToken = [string]$login.refreshToken

$headers = @{
    "X-Tenant-Id" = $TenantId
}
$body = @{
    refreshToken = $refreshToken
}

$resp = Invoke-WebRequest -Uri "$BaseUrl/refreshToken" -Method POST -WebSession $session -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $body
$json = $resp.Content | ConvertFrom-Json

if ($null -eq $json) {
    throw "FAIL: /refreshToken returned empty response body."
}
if ([int]$json.code -ne 200) {
    $info = [string]$json.info
    throw "FAIL: /refreshToken code=$($json.code) info=$info"
}

$newToken = [string]$json.data.token
$newRefreshToken = [string]$json.data.refreshToken
if ([string]::IsNullOrWhiteSpace($newToken) -or [string]::IsNullOrWhiteSpace($newRefreshToken)) {
    throw "FAIL: /refreshToken response missing token/refreshToken."
}

Write-Output "PASS: /refreshToken returns a new token."

