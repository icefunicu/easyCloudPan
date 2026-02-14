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
$token = [string]$login.token

$headers = @{
    "X-Tenant-Id"   = $TenantId
    "Authorization" = ("Bearer " + $token)
}

$resp = Invoke-WebRequest -Uri "$BaseUrl/getUserInfo" -Method POST -WebSession $session -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body @{}
$json = $resp.Content | ConvertFrom-Json

if ($null -eq $json) {
    throw "FAIL: /getUserInfo returned empty response body."
}
if ([int]$json.code -ne 200) {
    $info = [string]$json.info
    throw "FAIL: /getUserInfo code=$($json.code) info=$info"
}

$userId = [string]$json.data.userId
if ([string]::IsNullOrWhiteSpace($userId)) {
    throw "FAIL: /getUserInfo missing data.userId"
}

Write-Output "PASS: /getUserInfo returns userId (protected endpoint works)."

