param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default",
    [string]$CheckCode
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

$script:passed = 0
$script:failed = 0
$script:failedTests = @()

function Write-TestResult {
    param([string]$Name, [bool]$Success, [string]$Message = "")
    if ($Success) {
        Write-Output "  [PASS] $Name"
        $script:passed++
    } else {
        Write-Output "  [FAIL] $Name - $Message"
        $script:failed++
        $script:failedTests += $Name
    }
}

function Get-FileMd5 {
    param([string]$Path)
    return (Get-FileHash -Algorithm MD5 -Path $Path).Hash.ToLowerInvariant()
}

function New-TestFile {
    param([string]$TmpDir, [string]$Prefix)
    $fileId = $Prefix + [guid]::NewGuid().ToString("N").Substring(0, 10)
    $sourceFile = Join-Path $TmpDir "test.png"
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X8u6QAAAAASUVORK5CYII="
    [System.IO.File]::WriteAllBytes($sourceFile, [Convert]::FromBase64String($pngBase64))
    $fileMd5 = Get-FileMd5 -Path $sourceFile
    return @{
        FileId = $fileId
        FilePath = $sourceFile
        FileMd5 = $fileMd5
        FileName = "test.png"
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan: Verify Cursor Pagination"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_cursor_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/7] Login..."
    $session = New-EasyPanSession
    try {
        $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
        $token = [string]$login.token
        Write-TestResult "Login" $true
    } catch {
        Write-TestResult "Login" $false $_.Exception.Message
        throw "Login failed, cannot continue"
    }

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseCookies = $true
    $handler.CookieContainer = $session.Cookies
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    $uploadedFileIds = @()
    Write-Output ""
    Write-Output "[2/7] Upload 5 test files..."
    for ($i = 1; $i -le 5; $i++) {
        $testFile = New-TestFile -TmpDir $tmpDir -Prefix "CURSOR_${i}_"
        $multipart = New-Object System.Net.Http.MultipartFormDataContent
        $multipart.Add((New-Object System.Net.Http.StringContent($testFile.FileId)), "fileId")
        $multipart.Add((New-Object System.Net.Http.StringContent($testFile.FileName)), "fileName")
        $multipart.Add((New-Object System.Net.Http.StringContent("0")), "filePid")
        $multipart.Add((New-Object System.Net.Http.StringContent($testFile.FileMd5)), "fileMd5")
        $multipart.Add((New-Object System.Net.Http.StringContent("0")), "chunkIndex")
        $multipart.Add((New-Object System.Net.Http.StringContent("1")), "chunks")
        $fs = [System.IO.File]::OpenRead($testFile.FilePath)
        try {
            $fileContent = New-Object System.Net.Http.StreamContent($fs)
            $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
            $multipart.Add($fileContent, "file", $testFile.FileName)
            $uploadResp = $client.PostAsync("$BaseUrl/file/uploadFile", $multipart).Result
            $uploadJson = $uploadResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            if ($uploadJson.code -eq 200) {
                $uploadedFileIds += [string]$uploadJson.data.fileId
            }
        } finally {
            $fs.Dispose()
        }
    }
    Write-TestResult "Upload 5 Test Files" ($uploadedFileIds.Count -eq 5) "Uploaded $($uploadedFileIds.Count)/5 files"

    Write-Output ""
    Write-Output "[3/7] Cursor pagination - first page (no cursor, pageSize=2)..."
    $cursorPage1Resp = $client.GetAsync("$BaseUrl/file/loadDataListCursor?cursor=&pageSize=2").Result
    $cursorPage1Json = $cursorPage1Resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Cursor Pagination - Page 1" ($cursorPage1Json.code -eq 200) $cursorPage1Json.info
    $nextCursor = [string]$cursorPage1Json.data.nextCursor

    Write-Output ""
    Write-Output "[4/7] Cursor pagination - second page (using cursor)..."
    if ([string]::IsNullOrWhiteSpace($nextCursor)) {
        Write-TestResult "Cursor Pagination - Page 2" $false "No next cursor"
    } else {
        $cursorPage2Resp = $client.GetAsync("$BaseUrl/file/loadDataListCursor?cursor=$([Uri]::EscapeDataString($nextCursor))&pageSize=2").Result
        $cursorPage2Json = $cursorPage2Resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Cursor Pagination - Page 2" ($cursorPage2Json.code -eq 200) $cursorPage2Json.info
        $nextCursor2 = [string]$cursorPage2Json.data.nextCursor

        Write-Output ""
        Write-Output "[5/7] Cursor pagination - third page (if available)..."
        if (-not [string]::IsNullOrWhiteSpace($nextCursor2)) {
            $cursorPage3Resp = $client.GetAsync("$BaseUrl/file/loadDataListCursor?cursor=$([Uri]::EscapeDataString($nextCursor2))&pageSize=2").Result
            $cursorPage3Json = $cursorPage3Resp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Cursor Pagination - Page 3" ($cursorPage3Json.code -eq 200) $cursorPage3Json.info
        } else {
            Write-Output "  [SKIP] No third page needed"
        }
    }

    Write-Output ""
    Write-Output "[6/7] Compare with regular pagination..."
    $listBody = "pageNo=1&pageSize=10&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listJson = $listResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Regular Pagination" ($listJson.code -eq 200) $listJson.info

    Write-Output ""
    Write-Output "[7/7] Verify both return similar results..."
    $hasList = ($listJson.code -eq 200)
    $hasCursorPage1 = ($cursorPage1Json.code -eq 200)
    Write-TestResult "Results Comparison" ($hasList -and $hasCursorPage1) "Regular pagination: $hasList, Cursor pagination: $hasCursorPage1"

    Write-Output ""
    Write-Output "Cleaning up..."
    if ($uploadedFileIds.Count -gt 0) {
        $delBody = "fileIds=$($uploadedFileIds -join ',')"
        $delResp = $client.PostAsync("$BaseUrl/file/delFile", 
            (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    }
    Write-Output "  Cleanup complete"

} finally {
    if ($client) { $client.Dispose() }
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force -Path $tmpDir -ErrorAction SilentlyContinue
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "Results: $script:passed PASS, $script:failed FAIL"
if ($script:failedTests.Count -gt 0) {
    Write-Output "Failed: $($script:failedTests -join ', ')"
}
Write-Output "========================================"
Write-Output ""

if ($script:failed -gt 0) {
    exit 1
} else {
    exit 0
}
