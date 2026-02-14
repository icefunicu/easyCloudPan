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
Write-Output "EasyCloudPan: Verify Admin"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_admin_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/12] Login (admin user)..."
    $session = New-EasyPanSession
    try {
        $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
        $token = [string]$login.token
        Write-TestResult "Login (Admin User)" $true
    } catch {
        Write-TestResult "Login (Admin User)" $false $_.Exception.Message
        throw "Login failed, cannot continue"
    }

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseCookies = $true
    $handler.CookieContainer = $session.Cookies
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    $uploadedFileIds = @()
    $uploadedUserId = ""
    Write-Output ""
    Write-Output "[2/12] Upload test file (for admin to manage)..."
    $testFile = New-TestFile -TmpDir $tmpDir -Prefix "ADMIN_" -FileName "admin_test.png"
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
    Write-TestResult "Upload Test File (Admin)" ($uploadedFileIds.Count -eq 1) "Uploaded $($uploadedFileIds.Count)/1 files"

    if ($uploadedFileIds.Count -ge 1) {
        $testFileId = $uploadedFileIds[0]
        Write-Output ""
        Write-Output "[3/12] Get user info (to get userId)..."
        $userInfoResp = $client.GetAsync("$BaseUrl/getUserInfo").Result
        $userInfoJson = $userInfoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Get User Info" ($userInfoJson.code -eq 200) $userInfoJson.info
        if ($userInfoJson.code -eq 200) {
            $uploadedUserId = [string]$userInfoJson.data.userId
            Write-Output "  User ID: $uploadedUserId"
        }

        Write-Output ""
        Write-Output "[4/12] Admin: Get system settings..."
        $sysSettingsResp = $client.GetAsync("$BaseUrl/admin/getSysSettings").Result
        $sysSettingsJson = $sysSettingsResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Admin: Get Sys Settings" ($sysSettingsJson.code -eq 200) $sysSettingsJson.info

        Write-Output ""
        Write-Output "[5/12] Admin: Save system settings..."
        $saveSysBody = "registerEmailTitle=Welcome&registerEmailContent=Hello&userInitUseSpace=1073741824"
        $saveSysResp = $client.PostAsync("$BaseUrl/admin/saveSysSettings", 
            (New-Object System.Net.Http.StringContent($saveSysBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $saveSysJson = $saveSysResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Admin: Save Sys Settings" ($saveSysJson.code -eq 200) $saveSysJson.info

        Write-Output ""
        Write-Output "[6/12] Admin: Load user list..."
        $loadUserBody = "pageNo=1&pageSize=10"
        $loadUserResp = $client.PostAsync("$BaseUrl/admin/loadUserList", 
            (New-Object System.Net.Http.StringContent($loadUserBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $loadUserJson = $loadUserResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Admin: Load User List" ($loadUserJson.code -eq 200) $loadUserJson.info

        Write-Output ""
        Write-Output "[7/12] Admin: Load file list..."
        $loadFileBody = "pageNo=1&pageSize=10"
        $loadFileResp = $client.PostAsync("$BaseUrl/admin/loadFileList", 
            (New-Object System.Net.Http.StringContent($loadFileBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $loadFileJson = $loadFileResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Admin: Load File List" ($loadFileJson.code -eq 200) $loadFileJson.info

        Write-Output ""
        Write-Output "[8/12] Admin: Update user status (no change)..."
        if (-not [string]::IsNullOrWhiteSpace($uploadedUserId)) {
            $updateStatusBody = "userId=$uploadedUserId&status=1"
            $updateStatusResp = $client.PostAsync("$BaseUrl/admin/updateUserStatus", 
                (New-Object System.Net.Http.StringContent($updateStatusBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $updateStatusJson = $updateStatusResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Admin: Update User Status" ($updateStatusJson.code -eq 200) $updateStatusJson.info
        } else {
            Write-Output "  [SKIP] No user ID available"
        }

        Write-Output ""
        Write-Output "[9/12] Admin: Update user space (add 1GB)..."
        if (-not [string]::IsNullOrWhiteSpace($uploadedUserId)) {
            $updateSpaceBody = "userId=$uploadedUserId&changeSpace=1073741824"
            $updateSpaceResp = $client.PostAsync("$BaseUrl/admin/updateUserSpace", 
                (New-Object System.Net.Http.StringContent($updateSpaceBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $updateSpaceJson = $updateSpaceResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Admin: Update User Space" ($updateSpaceJson.code -eq 200) $updateSpaceJson.info
        } else {
            Write-Output "  [SKIP] No user ID available"
        }

        Write-Output ""
        Write-Output "[10/12] Admin: Get folder info..."
        $getFolderInfoBody = "path=/"
        $getFolderInfoResp = $client.PostAsync("$BaseUrl/admin/getFolderInfo", 
            (New-Object System.Net.Http.StringContent($getFolderInfoBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $getFolderInfoJson = $getFolderInfoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Admin: Get Folder Info" ($getFolderInfoJson.code -eq 200) $getFolderInfoJson.info

        Write-Output ""
        Write-Output "[11/12] Admin: Create download URL for test file..."
        if (-not [string]::IsNullOrWhiteSpace($uploadedUserId) -and -not [string]::IsNullOrWhiteSpace($testFileId)) {
            $createDownloadResp = $client.GetAsync("$BaseUrl/admin/createDownloadUrl/$uploadedUserId/$testFileId").Result
            $createDownloadJson = $createDownloadResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Admin: Create Download URL" ($createDownloadJson.code -eq 200) $createDownloadJson.info
        } else {
            Write-Output "  [SKIP] No user/file ID available"
        }

        Write-Output ""
        Write-Output "[12/12] Admin: Delete test file..."
        if (-not [string]::IsNullOrWhiteSpace($uploadedUserId) -and -not [string]::IsNullOrWhiteSpace($testFileId)) {
            $delFileBody = "fileIdAndUserIds=$($testFileId)_$($uploadedUserId)"
            $delFileResp = $client.PostAsync("$BaseUrl/admin/delFile", 
                (New-Object System.Net.Http.StringContent($delFileBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
            $delFileJson = $delFileResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
            Write-TestResult "Admin: Del File" ($delFileJson.code -eq 200) $delFileJson.info
        } else {
            Write-Output "  [SKIP] No user/file ID available"
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
