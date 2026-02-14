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

function Invoke-UploadFile {
    param($Client, [string]$BaseUrl, $FileId, $FileName, $FileMd5, $FilePath, [string]$FilePid = "0")
    
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $multipart.Add((New-Object System.Net.Http.StringContent($FileId)), "fileId")
    $multipart.Add((New-Object System.Net.Http.StringContent($FileName)), "fileName")
    $multipart.Add((New-Object System.Net.Http.StringContent($FilePid)), "filePid")
    $multipart.Add((New-Object System.Net.Http.StringContent($FileMd5)), "fileMd5")
    $multipart.Add((New-Object System.Net.Http.StringContent("0")), "chunkIndex")
    $multipart.Add((New-Object System.Net.Http.StringContent("1")), "chunks")

    $fs = [System.IO.File]::OpenRead($FilePath)
    try {
        $fileContent = New-Object System.Net.Http.StreamContent($fs)
        $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("application/octet-stream")
        $multipart.Add($fileContent, "file", $FileName)
        $resp = $Client.PostAsync("$BaseUrl/file/uploadFile", $multipart).Result
        $body = $resp.Content.ReadAsStringAsync().Result
    } finally {
        $fs.Dispose()
    }

    $json = $body | ConvertFrom-Json
    return $json
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan API Smoke Test"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_smoke_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/10] Health Check..."
    try {
        $healthResp = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method GET
        if ($healthResp.status -eq "UP" -and $healthResp.components.db.status -eq "UP" -and $healthResp.components.redis.status -eq "UP") {
            Write-TestResult "Health Check" $true
        } else {
            Write-TestResult "Health Check" $false "status not UP"
        }
    } catch {
        Write-TestResult "Health Check" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[2/10] Login..."
    $session = New-EasyPanSession
    try {
        $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId -CheckCode $CheckCode
        $token = [string]$login.token
        Write-TestResult "Login (dual token)" $true
    } catch {
        Write-TestResult "Login (dual token)" $false $_.Exception.Message
        throw "Login failed, cannot continue"
    }

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseCookies = $true
    $handler.CookieContainer = $session.Cookies
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    Write-Output ""
    Write-Output "[3/10] File Operations..."
    
    $testFile = New-TestFile -TmpDir $tmpDir -Prefix "SMOKE_"
    $uploadResult = Invoke-UploadFile -Client $client -BaseUrl $BaseUrl -FileId $testFile.FileId -FileName $testFile.FileName -FileMd5 $testFile.FileMd5 -FilePath $testFile.FilePath
    Write-TestResult "Upload File" ($uploadResult.code -eq 200) $uploadResult.info
    $uploadedFileId = [string]$uploadResult.data.fileId

    $listBody = "pageNo=1&pageSize=15&filePid=0&category=all"
    $listResp = $client.PostAsync("$BaseUrl/file/loadDataList", 
        (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $listJson = $listResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Load File List" ($listJson.code -eq 200) $listJson.info

    $newFolderBody = "filePid=0&fileName=smoke_folder_" + [guid]::NewGuid().ToString("N").Substring(0, 8)
    $folderResp = $client.PostAsync("$BaseUrl/file/newFoloder", 
        (New-Object System.Net.Http.StringContent($newFolderBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $folderJson = $folderResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "New Folder" ($folderJson.code -eq 200) $folderJson.info

    Write-Output ""
    Write-Output "[4/10] Rename File..."
    $newName = "renamed_" + [guid]::NewGuid().ToString("N").Substring(0, 6) + ".png"
    $renameBody = "fileId=$uploadedFileId&fileName=$newName"
    $renameResp = $client.PostAsync("$BaseUrl/file/rename", 
        (New-Object System.Net.Http.StringContent($renameBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $renameJson = $renameResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Rename File" ($renameJson.code -eq 200) $renameJson.info

    Write-Output ""
    Write-Output "[5/10] Download Roundtrip..."
    $dlResp = $client.PostAsync("$BaseUrl/file/createDownloadUrl/$uploadedFileId", $null).Result
    $dlJson = $dlResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    if ($dlJson.code -eq 200) {
        $dlCode = [string]$dlJson.data
        $downloadedFile = Join-Path $tmpDir "downloaded.png"
        $maxRetries = 30
        $downloadOk = $false
        for ($i = 1; $i -le $maxRetries; $i++) {
            try {
                $fileResp = Invoke-WebRequest -Uri "$BaseUrl/file/download/$dlCode" -Method GET -OutFile $downloadedFile -PassThru
                if ((Test-Path $downloadedFile) -and ((Get-Item $downloadedFile).Length -gt 0)) {
                    $downloadedMd5 = Get-FileMd5 -Path $downloadedFile
                    if ($downloadedMd5 -eq $testFile.FileMd5) {
                        $downloadOk = $true
                        break
                    }
                }
            } catch {}
            Start-Sleep -Milliseconds 500
        }
        Write-TestResult "Download Roundtrip" $downloadOk
    } else {
        Write-TestResult "Download Roundtrip" $false $dlJson.info
    }

    Write-Output ""
    Write-Output "[6/10] Delete to Recycle Bin..."
    $delBody = "fileIds=$uploadedFileId"
    $delResp = $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $delJson = $delResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Delete to Recycle" ($delJson.code -eq 200) $delJson.info

    Write-Output ""
    Write-Output "[7/10] Recycle Bin Operations..."
    $recycleBody = "pageNo=1&pageSize=15"
    $recycleResp = $client.PostAsync("$BaseUrl/recycle/loadRecycleList", 
        (New-Object System.Net.Http.StringContent($recycleBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $recycleJson = $recycleResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Load Recycle List" ($recycleJson.code -eq 200) $recycleJson.info

    $recoverBody = "fileIds=$uploadedFileId"
    $recoverResp = $client.PostAsync("$BaseUrl/recycle/recoverFile", 
        (New-Object System.Net.Http.StringContent($recoverBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $recoverJson = $recoverResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Recover File" ($recoverJson.code -eq 200) $recoverJson.info

    Write-Output ""
    Write-Output "[8/10] Share Operations..."
    $shareCode = "TEST" + [guid]::NewGuid().ToString("N").Substring(0, 4).ToUpper()
    $shareBody = "fileId=$uploadedFileId&validType=1&code=$shareCode"
    $shareResp = $client.PostAsync("$BaseUrl/share/shareFile", 
        (New-Object System.Net.Http.StringContent($shareBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $shareJson = $shareResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    $shareCreated = $shareJson.code -eq 200
    Write-TestResult "Create Share" $shareCreated $shareJson.info
    $shareId = [string]$shareJson.data.shareId

    if ($shareCreated) {
        $shareListBody = "pageNo=1&pageSize=15"
        $shareListResp = $client.PostAsync("$BaseUrl/share/loadShareList", 
            (New-Object System.Net.Http.StringContent($shareListBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $shareListJson = $shareListResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Load Share List" ($shareListJson.code -eq 200) $shareListJson.info

        $cancelBody = "shareIds=$shareId"
        $cancelResp = $client.PostAsync("$BaseUrl/share/cancelShare", 
            (New-Object System.Net.Http.StringContent($cancelBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $cancelJson = $cancelResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Cancel Share" ($cancelJson.code -eq 200) $cancelJson.info
    }

    Write-Output ""
    Write-Output "[9/10] Permanent Delete..."
    $delBody2 = "fileIds=$uploadedFileId"
    $delResp2 = $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody2, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $delJson2 = $delResp2.Content.ReadAsStringAsync().Result | ConvertFrom-Json

    $permBody = "fileIds=$uploadedFileId"
    $permResp = $client.PostAsync("$BaseUrl/recycle/delFile", 
        (New-Object System.Net.Http.StringContent($permBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $permJson = $permResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Permanent Delete" ($permJson.code -eq 200) $permJson.info

    Write-Output ""
    Write-Output "[10/10] Logout..."
    $logoutResp = $client.PostAsync("$BaseUrl/logout", $null).Result
    $logoutJson = $logoutResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    Write-TestResult "Logout" ($logoutJson.code -eq 200) $logoutJson.info

} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "SMOKE TEST SUMMARY"
Write-Output "========================================"
Write-Output "  Passed: $script:passed"
Write-Output "  Failed: $script:failed"
if ($script:failedTests.Count -gt 0) {
    Write-Output "  Failed tests:"
    foreach ($t in $script:failedTests) {
        Write-Output "    - $t"
    }
}
Write-Output ""

if ($script:failed -eq 0) {
    Write-Output "ALL TESTS PASSED"
    exit 0
} else {
    Write-Output "SOME TESTS FAILED"
    exit 1
}
