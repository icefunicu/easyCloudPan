param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode,
    [string]$FilePid = "0",
    [int]$MaxRetries = 60,
    [int]$RetryIntervalSec = 1
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

$tmpDir = Join-Path $env:TEMP ("easypan_download_" + [guid]::NewGuid().ToString("N"))
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
    $fileId = "TMP_DL_" + [guid]::NewGuid().ToString("N").Substring(0, 10)

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

    Write-Output "Step 3: Create download URL..."
    $createDlResp = $client.PostAsync("$BaseUrl/file/createDownloadUrl/$returnedFileId", $null).Result
    $createDlBody = $createDlResp.Content.ReadAsStringAsync().Result

    if (-not $createDlResp.IsSuccessStatusCode) {
        throw "FAIL: /file/createDownloadUrl httpStatus=$([int]$createDlResp.StatusCode)"
    }

    $createDlJson = $createDlBody | ConvertFrom-Json
    if ($null -eq $createDlJson) {
        throw "FAIL: /file/createDownloadUrl returned empty response body."
    }
    if ([int]$createDlJson.code -ne 200) {
        $info = [string]$createDlJson.info
        throw "FAIL: /file/createDownloadUrl code=$($createDlJson.code) info=$info"
    }

    $downloadCode = [string]$createDlJson.data
    if ([string]::IsNullOrWhiteSpace($downloadCode)) {
        throw "FAIL: /file/createDownloadUrl missing data (download code)"
    }
    Write-Output "  Download code obtained"

    Write-Output "Step 4: Download file with retry (max ${MaxRetries}s)..."
    $downloadedFile = Join-Path $tmpDir "downloaded_test.png"
    $downloadSuccess = $false
    $attempt = 0

    while ($attempt -lt $MaxRetries) {
        $attempt++
        try {
            $dlResp = Invoke-WebRequest -Uri "$BaseUrl/file/download/$downloadCode" -Method GET -OutFile $downloadedFile -PassThru
            if ([int]$dlResp.StatusCode -eq 200) {
                if ((Test-Path $downloadedFile) -and ((Get-Item $downloadedFile).Length -gt 0)) {
                    $downloadedMd5 = Get-FileMd5 -Path $downloadedFile
                    if ($downloadedMd5 -eq $fileMd5) {
                        $downloadSuccess = $true
                        Write-Output "  Download successful on attempt $attempt (MD5 match)"
                        break
                    } else {
                        Write-Output "  Attempt $attempt`: File downloaded but MD5 mismatch (source=$fileMd5, got=$downloadedMd5)"
                    }
                } else {
                    Write-Output "  Attempt $attempt`: Downloaded file is empty or missing"
                }
            } else {
                Write-Output "  Attempt $attempt`: HTTP status $([int]$dlResp.StatusCode)"
            }
        } catch {
            Write-Output "  Attempt $attempt`: Error - $($_.Exception.Message)"
        }

        if (-not $downloadSuccess -and $attempt -lt $MaxRetries) {
            Start-Sleep -Seconds $RetryIntervalSec
        }
    }

    if (-not $downloadSuccess) {
        throw "FAIL: Download verification failed after $MaxRetries attempts. Check backend/file/logs/easypan.log and MinIO status."
    }

    Write-Output "PASS: Download roundtrip verified (upload -> createDownloadUrl -> download -> MD5 match)."
} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}
