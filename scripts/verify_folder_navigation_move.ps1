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
Write-Output "EasyCloudPan: Verify Folder Navigation & Move"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_folder_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/9] Login..."
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

    Write-Output ""
    Write-Output "[2/9] Upload test file to root..."
    $testFile = New-TestFile -TmpDir $tmpDir -Prefix "MOVE_"
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
        Write-TestResult "Upload Test File" ($uploadJson.code -eq 200) $uploadJson.info
        $uploadedFileId = [string]$uploadJson.data.fileId
    } finally {
        $fs.Dispose()
    }

    Write-Output ""
    Write-Output "[3/9] Create first folder (Folder A)..."
    $folderABody = "filePid=0&fileName=Folder_" + [guid]::NewGuid().ToString("N").Substring(0, 8)
    $folderAResp = $client.PostAsync("$BaseUrl/file/newFoloder", 
        (New-Object System.Net.Http.StringContent($folderABody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $folderAJson = $folderAResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Create Folder A" ($folderAJson.code -eq 200) $folderAJson.info
    $folderAId = [string]$folderAJson.data.fileId

    Write-Output ""
    Write-Output "[4/9] Create second folder (Folder B inside Folder A)..."
    $folderBBody = "filePid=$folderAId&fileName=Folder_B"
    $folderBResp = $client.PostAsync("$BaseUrl/file/newFoloder", 
        (New-Object System.Net.Http.StringContent($folderBBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $folderBJson = $folderBResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Create Folder B" ($folderBJson.code -eq 200) $folderBJson.info
    $folderBId = [string]$folderBJson.data.fileId

    Write-Output ""
    Write-Output "[5/9] Load data list (root)..."
    $listBody = "pageNo=1&pageSize=15&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listJson = $listResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Load Data List (Root)" ($listJson.code -eq 200) $listJson.info

    Write-Output ""
    Write-Output "[6/9] Load all folders (for moving)..."
    $loadAllBody = "filePid=0&currentFileIds=$uploadedFileId"
    $loadAllResp = $client.PostAsync("$BaseUrl/file/loadAllFolder", 
        (New-Object System.Net.Http.StringContent($loadAllBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $loadAllJson = $loadAllResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Load All Folders" ($loadAllJson.code -eq 200) $loadAllJson.info

    Write-Output ""
    Write-Output "[7/9] Move test file to Folder A..."
    $moveBody = "fileIds=$uploadedFileId&filePid=$folderAId"
    $moveResp = $client.PostAsync("$BaseUrl/file/changeFileFolder", 
        (New-Object System.Net.Http.StringContent($moveBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $moveJson = $moveResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Move File to Folder A" ($moveJson.code -eq 200) $moveJson.info

    Write-Output ""
    Write-Output "[8/9] Load data list from Folder A..."
    $listBodyA = "pageNo=1&pageSize=15&filePid=$folderAId&category=all"
    $listRespA = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBodyA, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listJsonA = $listRespA.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Load Data List (Folder A)" ($listJsonA.code -eq 200) $listJsonA.info

    Write-Output ""
    Write-Output "[9/9] Move test file back to root..."
    $moveBackBody = "fileIds=$uploadedFileId&filePid=0"
    $moveBackResp = $client.PostAsync("$BaseUrl/file/changeFileFolder", 
        (New-Object System.Net.Http.StringContent($moveBackBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $moveBackJson = $moveBackResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Move File Back to Root" ($moveBackJson.code -eq 200) $moveBackJson.info

    Write-Output ""
    Write-Output "Cleaning up..."
    $delBody = "fileIds=$uploadedFileId,$folderAId,$folderBId"
    $delResp = $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
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
