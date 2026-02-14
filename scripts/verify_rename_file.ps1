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

$tmpDir = Join-Path $env:TEMP ("easypan_rename_" + [guid]::NewGuid().ToString("N"))
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
    $fileId = "TMP_REN_" + [guid]::NewGuid().ToString("N").Substring(0, 10)

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

    if (-not $uploadResp.IsSuccessStatusCode) {
        throw "FAIL: /file/uploadFile httpStatus=$([int]$uploadResp.StatusCode)"
    }

    $uploadJson = $uploadBody | ConvertFrom-Json
    if ([int]$uploadJson.code -ne 200) {
        throw "FAIL: /file/uploadFile code=$($uploadJson.code) info=$($uploadJson.info)"
    }
    $returnedFileId = [string]$uploadJson.data.fileId
    Write-Output "  Uploaded fileId=$returnedFileId"

    Write-Output "Step 3: Rename file..."
    $newFileName = "renamed_" + [guid]::NewGuid().ToString("N").Substring(0, 8) + ".png"
    $renameBody = "fileId=$returnedFileId&fileName=$newFileName"
    $renameResp = $client.PostAsync("$BaseUrl/file/rename", 
        (New-Object System.Net.Http.StringContent($renameBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $renameRespBody = $renameResp.Content.ReadAsStringAsync().Result

    if (-not $renameResp.IsSuccessStatusCode) {
        throw "FAIL: /file/rename httpStatus=$([int]$renameResp.StatusCode)"
    }

    $renameJson = $renameRespBody | ConvertFrom-Json
    if ([int]$renameJson.code -ne 200) {
        throw "FAIL: /file/rename code=$($renameJson.code) info=$($renameJson.info)"
    }
    Write-Output "  Rename request successful"

    Write-Output "Step 4: Verify file renamed in list..."
    $listBody = "pageNo=1&pageSize=15&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listRespBody = $listResp.Content.ReadAsStringAsync().Result

    if (-not $listResp.IsSuccessStatusCode) {
        throw "FAIL: /file/loadDataList httpStatus=$([int]$listResp.StatusCode)"
    }

    $listJson = $listRespBody | ConvertFrom-Json
    if ([int]$listJson.code -ne 200) {
        throw "FAIL: /file/loadDataList code=$($listJson.code) info=$($listJson.info)"
    }

    $foundRenamed = $false
    if ($null -ne $listJson.data.list) {
        foreach ($item in $listJson.data.list) {
            if ([string]$item.fileId -eq $returnedFileId) {
                if ([string]$item.fileName -eq $newFileName) {
                    $foundRenamed = $true
                }
                break
            }
        }
    }

    if (-not $foundRenamed) {
        throw "FAIL: File not found with new name in list"
    }
    Write-Output "  File renamed to '$newFileName' verified"

    Write-Output "PASS: File rename verified (upload -> rename -> verify new name in list)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
