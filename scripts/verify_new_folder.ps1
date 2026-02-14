param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode,
    [string]$FilePid = "0",
    [string]$FolderName
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

if ([string]::IsNullOrWhiteSpace($FolderName)) {
    $FolderName = ("smoke-folder-" + [guid]::NewGuid().ToString("N").Substring(0, 8))
}

$session = New-EasyPanSession
$login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
$token = [string]$login.token

$headers = @{
    "X-Tenant-Id"   = $TenantId
    "Authorization" = ("Bearer " + $token)
}

$resp = Invoke-WebRequest -Uri "$BaseUrl/file/newFoloder" -Method POST -WebSession $session -Headers $headers `
    -ContentType "application/x-www-form-urlencoded" -Body @{ filePid = $FilePid; fileName = $FolderName }
$json = $resp.Content | ConvertFrom-Json

if ($null -eq $json) {
    throw "FAIL: /file/newFoloder returned empty response body."
}
if ([int]$json.code -ne 200) {
    $info = [string]$json.info
    throw "FAIL: /file/newFoloder code=$($json.code) info=$info"
}

$data = $json.data
if ($null -eq $data) {
    throw "FAIL: /file/newFoloder missing data."
}

$fileId = [string]$data.fileId
$folderType = [string]$data.folderType
$returnedName = [string]$data.fileName

if ([string]::IsNullOrWhiteSpace($fileId)) {
    throw "FAIL: /file/newFoloder missing data.fileId"
}
if ($folderType -ne "1") {
    throw "FAIL: /file/newFoloder expected folderType=1, actual=$folderType"
}
if ($returnedName -ne $FolderName) {
    throw "FAIL: /file/newFoloder expected fileName='$FolderName', actual='$returnedName'"
}

Write-Output "PASS: /file/newFoloder created folder successfully."

