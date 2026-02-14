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

$tmpDir = Join-Path $env:TEMP ("easypan_delete_" + [guid]::NewGuid().ToString("N"))
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
    $fileId = "TMP_DEL_" + [guid]::NewGuid().ToString("N").Substring(0, 10)

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
    if ($null -eq $uploadJson) {
        throw "FAIL: /file/uploadFile returned empty response body."
    }
    if ([int]$uploadJson.code -ne 200) {
        $info = [string]$uploadJson.info
        throw "FAIL: /file/uploadFile code=$($uploadJson.code) info=$info"
    }

    $returnedFileId = [string]$uploadJson.data.fileId
    if ([string]::IsNullOrWhiteSpace($returnedFileId)) {
        throw "FAIL: /file/uploadFile missing data.fileId"
    }
    Write-Output "  Uploaded fileId=$returnedFileId"

    Write-Output "Step 3: Delete file to recycle bin..."
    $delBody = "fileIds=$returnedFileId"
    $delResp = $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $delRespBody = $delResp.Content.ReadAsStringAsync().Result

    if (-not $delResp.IsSuccessStatusCode) {
        throw "FAIL: /file/delFile httpStatus=$([int]$delResp.StatusCode)"
    }

    $delJson = $delRespBody | ConvertFrom-Json
    if ($null -eq $delJson) {
        throw "FAIL: /file/delFile returned empty response body."
    }
    if ([int]$delJson.code -ne 200) {
        $info = [string]$delJson.info
        throw "FAIL: /file/delFile code=$($delJson.code) info=$info"
    }
    Write-Output "  Delete request successful"

    Write-Output "Step 4: Verify file in recycle bin..."
    $listBody = "pageNo=1&pageSize=15&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listRespBody = $listResp.Content.ReadAsStringAsync().Result

    if (-not $listResp.IsSuccessStatusCode) {
        throw "FAIL: /file/loadDataList httpStatus=$([int]$listResp.StatusCode)"
    }

    $listJson = $listRespBody | ConvertFrom-Json
    if ($null -eq $listJson) {
        throw "FAIL: /file/loadDataList returned empty response body."
    }
    if ([int]$listJson.code -ne 200) {
        $info = [string]$listJson.info
        throw "FAIL: /file/loadDataList code=$($listJson.code) info=$info"
    }

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
        throw "FAIL: File still visible in normal list after deletion (should be in recycle bin)"
    }
    Write-Output "  File not in normal list (expected)"

    $recycleBody = "pageNo=1&pageSize=15"
    $recycleResp = $client.PostAsync("$BaseUrl/recycle/loadRecycleList", 
        (New-Object System.Net.Http.StringContent($recycleBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $recycleRespBody = $recycleResp.Content.ReadAsStringAsync().Result

    if (-not $recycleResp.IsSuccessStatusCode) {
        throw "FAIL: /recycle/loadRecycleList httpStatus=$([int]$recycleResp.StatusCode)"
    }

    $recycleJson = $recycleRespBody | ConvertFrom-Json
    if ($null -eq $recycleJson) {
        throw "FAIL: /recycle/loadRecycleList returned empty response body."
    }
    if ([int]$recycleJson.code -ne 200) {
        $info = [string]$recycleJson.info
        throw "FAIL: /recycle/loadRecycleList code=$($recycleJson.code) info=$info"
    }

    $foundInRecycle = $false
    if ($null -ne $recycleJson.data.list) {
        foreach ($item in $recycleJson.data.list) {
            if ([string]$item.fileId -eq $returnedFileId) {
                $foundInRecycle = $true
                break
            }
        }
    }

    if (-not $foundInRecycle) {
        throw "FAIL: File not found in recycle bin after deletion"
    }
    Write-Output "  File found in recycle bin"

    Write-Output "PASS: Delete to recycle bin verified (upload -> delete -> verify in recycle bin)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
