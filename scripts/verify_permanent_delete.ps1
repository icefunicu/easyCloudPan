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

$tmpDir = Join-Path $env:TEMP ("easypan_permanent_" + [guid]::NewGuid().ToString("N"))
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
    $fileId = "TMP_PERM_" + [guid]::NewGuid().ToString("N").Substring(0, 10)

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

    Write-Output "Step 3: Delete file to recycle bin..."
    $delBody = "fileIds=$returnedFileId"
    $delResp = $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $delRespBody = $delResp.Content.ReadAsStringAsync().Result
    $delJson = $delRespBody | ConvertFrom-Json
    if ([int]$delJson.code -ne 200) {
        throw "FAIL: /file/delFile code=$($delJson.code) info=$($delJson.info)"
    }
    Write-Output "  File moved to recycle bin"

    Write-Output "Step 4: Permanently delete file..."
    $permBody = "fileIds=$returnedFileId"
    $permResp = $client.PostAsync("$BaseUrl/recycle/delFile", 
        (New-Object System.Net.Http.StringContent($permBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $permRespBody = $permResp.Content.ReadAsStringAsync().Result

    if (-not $permResp.IsSuccessStatusCode) {
        throw "FAIL: /recycle/delFile httpStatus=$([int]$permResp.StatusCode)"
    }

    $permJson = $permRespBody | ConvertFrom-Json
    if ([int]$permJson.code -ne 200) {
        throw "FAIL: /recycle/delFile code=$($permJson.code) info=$($permJson.info)"
    }
    Write-Output "  Permanent delete request successful"

    Write-Output "Step 5: Verify file not in normal list..."
    $listBody = "pageNo=1&pageSize=15&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listRespBody = $listResp.Content.ReadAsStringAsync().Result
    $listJson = $listRespBody | ConvertFrom-Json

    $foundInList = $false
    if ($null -ne $listJson.data.list) {
        foreach ($item in $listJson.data.list) {
            if ([string]$item.fileId -eq $returnedFileId) {
                $foundInList = $true
                break
            }
        }
    }

    if ($foundInList) {
        throw "FAIL: File still in normal list after permanent delete"
    }
    Write-Output "  File not in normal list (expected)"

    Write-Output "Step 6: Verify file not in recycle bin..."
    $recycleBody = "pageNo=1&pageSize=15"
    $recycleResp = $client.PostAsync("$BaseUrl/recycle/loadRecycleList", 
        (New-Object System.Net.Http.StringContent($recycleBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $recycleRespBody = $recycleResp.Content.ReadAsStringAsync().Result
    $recycleJson = $recycleRespBody | ConvertFrom-Json

    $foundInRecycle = $false
    if ($null -ne $recycleJson.data.list) {
        foreach ($item in $recycleJson.data.list) {
            if ([string]$item.fileId -eq $returnedFileId) {
                $foundInRecycle = $true
                break
            }
        }
    }

    if ($foundInRecycle) {
        throw "FAIL: File still in recycle bin after permanent delete"
    }
    Write-Output "  File not in recycle bin (expected)"

    Write-Output "PASS: Permanent delete verified (upload -> delete -> permanent delete -> file gone)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
