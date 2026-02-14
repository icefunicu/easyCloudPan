param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode,
    [string]$FilePid = "0",
    [string]$FilePath
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

$tmpDir = Join-Path $env:TEMP ("easypan_upload_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$createdTemp = $false
try {
    if ([string]::IsNullOrWhiteSpace($FilePath)) {
        $FilePath = Join-Path $tmpDir "smoke.png"
        # 1x1 transparent PNG
        $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X8u6QAAAAASUVORK5CYII="
        [System.IO.File]::WriteAllBytes($FilePath, [Convert]::FromBase64String($pngBase64))
        $createdTemp = $true
    }

    if (-not (Test-Path $FilePath)) {
        throw "File not found: $FilePath"
    }

    $fileName = [System.IO.Path]::GetFileName($FilePath)
    $fileMd5 = (Get-FileHash -Algorithm MD5 -Path $FilePath).Hash.ToLowerInvariant()
    $fileId = "TMP_SMOKE_" + [guid]::NewGuid().ToString("N").Substring(0, 8)

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

    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $multipart.Add((New-Object System.Net.Http.StringContent($fileId)), "fileId")
    $multipart.Add((New-Object System.Net.Http.StringContent($fileName)), "fileName")
    $multipart.Add((New-Object System.Net.Http.StringContent($FilePid)), "filePid")
    $multipart.Add((New-Object System.Net.Http.StringContent($fileMd5)), "fileMd5")
    $multipart.Add((New-Object System.Net.Http.StringContent("0")), "chunkIndex")
    $multipart.Add((New-Object System.Net.Http.StringContent("1")), "chunks")

    $fs = [System.IO.File]::OpenRead($FilePath)
    try {
        $fileContent = New-Object System.Net.Http.StreamContent($fs)
        $fileContent.Headers.ContentType =
            New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
        $multipart.Add($fileContent, "file", $fileName)

        $resp = $client.PostAsync("$BaseUrl/file/uploadFile", $multipart).Result
        $respBody = $resp.Content.ReadAsStringAsync().Result
    } finally {
        $fs.Dispose()
    }

    if (-not $resp.IsSuccessStatusCode) {
        throw "FAIL: /file/uploadFile httpStatus=$([int]$resp.StatusCode)"
    }

    $json = $respBody | ConvertFrom-Json
    if ($null -eq $json) {
        throw "FAIL: /file/uploadFile returned empty response body."
    }
    if ([int]$json.code -ne 200) {
        $info = [string]$json.info
        throw "FAIL: /file/uploadFile code=$($json.code) info=$info"
    }

    $status = [string]$json.data.status
    if ($status -ne "upload_finish" -and $status -ne "upload_seconds") {
        throw "FAIL: /file/uploadFile unexpected status=$status"
    }

    $returnedFileId = [string]$json.data.fileId
    if ([string]::IsNullOrWhiteSpace($returnedFileId)) {
        throw "FAIL: /file/uploadFile missing data.fileId"
    }

    Write-Output "PASS: /file/uploadFile returns status=$status (fileId=$returnedFileId)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
