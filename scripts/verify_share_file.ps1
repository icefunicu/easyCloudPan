param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode,
    [string]$FilePid = "0"
)

$ErrorActionPreference = "Stop"

$null = Add-Type -AssemblyName System.Net.Http -ErrorAction SilentlyContinue

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

$tmpDir = Join-Path $env:TEMP ("easypan_share_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

function Get-FileMd5 {
    param([string]$Path)
    return (Get-FileHash -Algorithm MD5 -Path $Path).Hash.ToLowerInvariant()
}

try {
    $sourceFile = Join-Path $tmpDir "source_test.png"
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X8u6QAAAAASUVORK5CYII="
    [System.IO.File]::WriteAllBytes($sourceFile, [Convert]::FromBase64String($pngBase64))
    $fileName = [System.IO.Path]::GetFileName($sourceFile)
    $fileMd5 = Get-FileMd5 -Path $sourceFile
    $fileId = "TMP_SHR_" + [guid]::NewGuid().ToString("N").Substring(0, 10)

    Write-Output "Step 1: Login..."
    $session = New-EasyPanSession
    $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
    $token = [string]$login.token

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseCookies = $true
    $handler.CookieContainer = $session.Cookies

    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)
    $client.DefaultRequestHeaders.Authorization =
        New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    Write-Output "Step 2: Upload test file..."
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $multipart.Add((New-Object System.Net.Http.StringContent($fileId)), "fileId")
    $multipart.Add((New-Object System.Net.Http.StringContent($fileName)), "fileName")
    $multipart.Add((New-Object System.Net.Http.StringContent($FilePid)), "filePid")
    $multipart.Add((New-Object System.Net.Http.StringContent($fileMd5)), "fileMd5")
    $multipart.Add((New-Object System.Net.Http.StringContent("0")), "chunkIndex")
    $multipart.Add((New-Object System.Net.Http.StringContent("1")), "chunks")

    $fs = [System.IO.File]::OpenRead($sourceFile)
    try {
        $fileContent = New-Object System.Net.Http.StreamContent($fs)
        $fileContent.Headers.ContentType =
            New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
        $multipart.Add($fileContent, "file", $fileName)

        $uploadResp = $client.PostAsync("$BaseUrl/file/uploadFile", $multipart).Result
        $uploadBody = $uploadResp.Content.ReadAsStringAsync().Result
    } finally {
        $fs.Dispose()
    }

    $uploadJson = $uploadBody | ConvertFrom-Json
    if ([int]$uploadJson.code -ne 200) {
        throw "FAIL: /file/uploadFile code=$($uploadJson.code) info=$($uploadJson.info)"
    }
    $returnedFileId = [string]$uploadJson.data.fileId
    Write-Output "  Uploaded fileId=$returnedFileId"

    Write-Output "Step 3: Create share..."
    $shareCode = "TEST" + [guid]::NewGuid().ToString("N").Substring(0, 4).ToUpper()
    $shareBody = "fileId=$returnedFileId&validType=1&code=$shareCode"
    $shareResp = $client.PostAsync("$BaseUrl/share/shareFile", 
        (New-Object System.Net.Http.StringContent($shareBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $shareRespBody = $shareResp.Content.ReadAsStringAsync().Result

    if (-not $shareResp.IsSuccessStatusCode) {
        throw "FAIL: /share/shareFile httpStatus=$([int]$shareResp.StatusCode)"
    }

    $shareJson = $shareRespBody | ConvertFrom-Json
    if ([int]$shareJson.code -ne 200) {
        throw "FAIL: /share/shareFile code=$($shareJson.code) info=$($shareJson.info)"
    }

    $shareId = [string]$shareJson.data.shareId
    if ([string]::IsNullOrWhiteSpace($shareId)) {
        throw "FAIL: /share/shareFile missing shareId"
    }
    Write-Output "  Share created: shareId=$shareId"

    Write-Output "Step 4: Load share list..."
    $listBody = "pageNo=1&pageSize=15"
    $listResp = $client.PostAsync("$BaseUrl/share/loadShareList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listRespBody = $listResp.Content.ReadAsStringAsync().Result

    $listJson = $listRespBody | ConvertFrom-Json
    if ([int]$listJson.code -ne 200) {
        throw "FAIL: /share/loadShareList code=$($listJson.code) info=$($listJson.info)"
    }

    $foundInList = $false
    if ($null -ne $listJson.data.list) {
        foreach ($item in $listJson.data.list) {
            if ([string]$item.shareId -eq $shareId) {
                $foundInList = $true
                break
            }
        }
    }
    if (-not $foundInList) {
        throw "FAIL: Share not found in share list"
    }
    Write-Output "  Share found in list"

    Write-Output "Step 5: Get share info (public)..."
    $infoBody = "shareId=$shareId"
    $infoResp = Invoke-RestMethod -Uri "$BaseUrl/showShare/getShareInfo" -Method POST -Body $infoBody -ContentType "application/x-www-form-urlencoded"
    if ([int]$infoResp.code -ne 200) {
        throw "FAIL: /showShare/getShareInfo code=$($infoResp.code) info=$($infoResp.info)"
    }
    Write-Output "  Share info retrieved"

    Write-Output "Step 6: Cancel share..."
    $cancelBody = "shareIds=$shareId"
    $cancelResp = $client.PostAsync("$BaseUrl/share/cancelShare", 
        (New-Object System.Net.Http.StringContent($cancelBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $cancelRespBody = $cancelResp.Content.ReadAsStringAsync().Result

    $cancelJson = $cancelRespBody | ConvertFrom-Json
    if ([int]$cancelJson.code -ne 200) {
        throw "FAIL: /share/cancelShare code=$($cancelJson.code) info=$($cancelJson.info)"
    }
    Write-Output "  Share cancelled"

    Write-Output "PASS: Share functionality verified (upload -> create share -> list -> get info -> cancel)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
