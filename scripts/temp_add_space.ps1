param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$UserId = "6190528218",
    [int]$AddSpaceMB = 10240
)

$ErrorActionPreference = "Stop"

$api = Join-Path $PSScriptRoot "_easypan_api.ps1"
. $api

$Email = $env:EASYPAN_EMAIL
$Password = $env:EASYPAN_PASSWORD

if ([string]::IsNullOrWhiteSpace($Email) -or [string]::IsNullOrWhiteSpace($Password)) {
    throw "Set EASYPAN_EMAIL and EASYPAN_PASSWORD environment variables"
}

$session = New-EasyPanSession
$login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password
$token = [string]$login.token

$headers = @{
    "Authorization" = "Bearer $token"
}

$body = "userId=$UserId&changeSpace=$AddSpaceMB"
$resp = Invoke-WebRequest -Uri "$BaseUrl/admin/updateUserSpace" -Method POST -WebSession $session -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $body
Write-Output $resp.Content
