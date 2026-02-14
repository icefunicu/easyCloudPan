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
    param([string]$TmpDir, [string]$Prefix, [string]$FileName)
    $fileId = $Prefix + [guid]::NewGuid().ToString("N").Substring(0, 10)
    $sourceFile = Join-Path $TmpDir $FileName
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMB/6X8u6QAAAAASUVORK5CYII="
    [System.IO.File]::WriteAllBytes($sourceFile, [Convert]::FromBase64String($pngBase64))
    $fileMd5 = Get-FileMd5 -Path $sourceFile
    return @{
        FileId = $fileId
        FilePath = $sourceFile
        FileMd5 = $fileMd5
        FileName = $FileName
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan: Verify Web Share"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_share_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/13] Login (main user)..."
    $session = New-EasyPanSession
    try {
        $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
        $token = [string]$login.token
        Write-TestResult "Login (Main User)" $true
    } catch {
        Write-TestResult "Login (Main User)" $false $_.Exception.Message
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
    Write-Output "[2/13] Upload test file..."
    $testFile = New-TestFile -TmpDir $tmpDir -Prefix "SHARE_" -FileName "share_test.png"
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
    Write-TestResult "Upload Test File" ($uploadedFileIds.Count -eq 1) "Uploaded $($uploadedFileIds.Count)/1 files"

    $shareId = ""
    if ($uploadedFileIds.Count -ge 1) {
        $testFileId = $uploadedFileIds[0]
        Write-Output ""
        Write-Output "[3/11] Create share (with code)..."
        $shareCodeBody = "fileId=$testFileId&validType=0&code=1234"
        $shareCodeResp = $client.PostAsync("$BaseUrl/share/shareFile", 
            (New-Object System.Net.Http.StringContent($shareCodeBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $shareCodeJson = $shareCodeResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Create Share (With Code)" ($shareCodeJson.code -eq 200) $shareCodeJson.info
        $shareId = ""
        if ($shareCodeJson.code -eq 200) {
            $shareId = [string]$shareCodeJson.data.shareId
            Write-Output "  Share ID: $shareId"
        }

        if (-not [string]::IsNullOrWhiteSpace($shareId)) {
            $visitorSession = New-EasyPanSession
            $visitorHandler = New-Object System.Net.Http.HttpClientHandler
            $visitorHandler.UseCookies = $true
            $visitorHandler.CookieContainer = $visitorSession.Cookies
            $visitorClient = New-Object System.Net.Http.HttpClient($visitorHandler)
            $visitorClient.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)

            Write-Output ""
            Write-Output "[4/11] Get share info (visitor)..."
            $shareInfoResp = $visitorClient.GetAsync("$BaseUrl/showShare/getShareInfo?shareId=$shareId").Result
            $shareInfoJson = $shareInfoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Get Share Info (Visitor)" ($shareInfoJson.code -eq 200) $shareInfoJson.info

            Write-Output ""
            Write-Output "[5/11] Get share login info (visitor)..."
            $shareLoginResp = $visitorClient.GetAsync("$BaseUrl/showShare/getShareLoginInfo?shareId=$shareId").Result
            $shareLoginJson = $shareLoginResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Get Share Login Info (Visitor)" ($shareLoginJson.code -eq 200) $shareLoginJson.info

            Write-Output ""
            Write-Output "[6/11] Check share code (incorrect)..."
            $wrongCodeBody = "shareId=$shareId&code=9999"
            $wrongCodeResp = $visitorClient.PostAsync("$BaseUrl/showShare/checkShareCode", 
                (New-Object System.Net.Http.StringContent($wrongCodeBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $wrongCodeJson = $wrongCodeResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Check Share Code (Incorrect)" ($wrongCodeJson.code -ne 200) "Expected failure, got code: $($wrongCodeJson.code)"

            Write-Output ""
            Write-Output "[7/11] Check share code (correct)..."
            $correctCodeBody = "shareId=$shareId&code=1234"
            $correctCodeResp = $visitorClient.PostAsync("$BaseUrl/showShare/checkShareCode", 
                (New-Object System.Net.Http.StringContent($correctCodeBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $correctCodeJson = $correctCodeResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Check Share Code (Correct)" ($correctCodeJson.code -eq 200) $correctCodeJson.info

            Write-Output ""
            Write-Output "[8/11] Load file list (after check)..."
            $listBody = "shareId=$shareId&filePid=0"
            $listResp = $visitorClient.PostAsync("$BaseUrl/showShare/loadFileList", 
                (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $listJson = $listResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Load File List (Visitor)" ($listJson.code -eq 200) $listJson.info

            Write-Output ""
            Write-Output "[9/11] Load user's share list (owner)..."
            $ownerShareListBody = "pageNo=1&pageSize=10"
            $ownerShareListResp = $client.PostAsync("$BaseUrl/share/loadShareList", 
                (New-Object System.Net.Http.StringContent($ownerShareListBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $ownerShareListJson = $ownerShareListResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Load Share List (Owner)" ($ownerShareListJson.code -eq 200) $ownerShareListJson.info

            Write-Output ""
            Write-Output "[10/11] Cancel share (owner)..."
            if (-not [string]::IsNullOrWhiteSpace($shareId)) {
                $cancelBody = "shareIds=$shareId"
                $cancelResp = $client.PostAsync("$BaseUrl/share/cancelShare", 
                    (New-Object System.Net.Http.StringContent($cancelBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
                $cancelJson = $cancelResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
                Write-TestResult "Cancel Share (Owner)" ($cancelJson.code -eq 200) $cancelJson.info
            }

            Write-Output ""
            Write-Output "[11/11] Get user info (owner)..."
            $userInfoResp = $client.GetAsync("$BaseUrl/getUserInfo").Result
            $userInfoJson = $userInfoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Get User Info (Owner)" ($userInfoJson.code -eq 200) $userInfoJson.info
        }
    }

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
