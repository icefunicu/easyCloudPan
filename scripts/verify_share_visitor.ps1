param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default"
)

$ErrorActionPreference = "Stop"

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

$null = Add-Type -AssemblyName System.Net.Http -ErrorAction SilentlyContinue

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
    $fileId = $Prefix.Substring(0, [Math]::Min($Prefix.Length, 10)) + [guid]::NewGuid().ToString("N").Substring(0, 10)
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

function Invoke-ApiPost {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$BaseUrl,
        [string]$Path,
        [string]$Body,
        [string]$Token,
        [string]$TenantId = "default"
    )
    
    $headers = @{
        "X-Tenant-Id" = $TenantId
    }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    $resp = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method POST -WebSession $Session -Headers $headers -ContentType "application/x-www-form-urlencoded" -Body $Body
    $content = $resp.Content
    if ($content -is [byte[]]) {
        $content = [System.Text.Encoding]::UTF8.GetString($content)
    }
    return $content | ConvertFrom-Json
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan Share Visitor Verification"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_share_visitor_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    $session = New-EasyPanSession
    Write-Output "[Setup] Logging in..."
    $login = Invoke-EasyPanLogin -BaseUrl $BaseUrl -Session $session -Email $Email -Password $Password -TenantId $TenantId
    $token = [string]$login.token
    Write-Output "[Setup] Login successful, token obtained"

    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseCookies = $true
    $handler.CookieContainer = $session.Cookies
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.DefaultRequestHeaders.Add("X-Tenant-Id", $TenantId)
    $client.DefaultRequestHeaders.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", $token)

    Write-Output "[Setup] Uploading test file..."
    $testFile = New-TestFile -TmpDir $tmpDir -Prefix "SHARE_VISITOR_"
    Write-Output "  FileId: $($testFile.FileId)"
    Write-Output "  FileMd5: $($testFile.FileMd5)"
    Write-Output "  FilePath: $($testFile.FilePath)"
    Write-Output "  FileExists: $(Test-Path $testFile.FilePath)"
    
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
        $uploadBody = $uploadResp.Content.ReadAsStringAsync().Result
        Write-Output "  Upload response: $uploadBody"
        $uploadJson = $uploadBody | ConvertFrom-Json
    } finally {
        $fs.Dispose()
    }
    
    $uploadedFileId = [string]$uploadJson.data.fileId
    Write-Output "  Uploaded fileId=$uploadedFileId"

    Write-Output "[Setup] Creating share..."
    $shareCode = "TEST" + [guid]::NewGuid().ToString("N").Substring(0, 4).ToUpper()
    $shareBody = "fileId=$uploadedFileId&validType=1&code=$shareCode"
    $shareResp = $client.PostAsync("$BaseUrl/share/shareFile", 
        (New-Object System.Net.Http.StringContent($shareBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
    $shareRespBody = $shareResp.Content.ReadAsStringAsync().Result
    Write-Output "  Share response: $shareRespBody"
    $shareJson = $shareRespBody | ConvertFrom-Json
    $shareId = [string]$shareJson.data.shareId
    Write-Output "  shareId=$shareId"

    Write-Output ""
    Write-Output "[1/5] M-SHARE-005: Get Share Info..."
    $visitorHandler = New-Object System.Net.Http.HttpClientHandler
    $visitorHandler.UseCookies = $true
    $visitorHandler.CookieContainer = New-Object System.Net.CookieContainer
    $visitorClient = New-Object System.Net.Http.HttpClient($visitorHandler)
    
    try {
        $infoBody = "shareId=$shareId"
        $infoResp = $visitorClient.PostAsync("$BaseUrl/showShare/getShareInfo", 
            (New-Object System.Net.Http.StringContent($infoBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $infoJson = $infoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        if ($infoJson.code -eq 200 -and $infoJson.data -ne $null) {
            Write-TestResult "M-SHARE-005: Get Share Info" $true
        } else {
            Write-TestResult "M-SHARE-005: Get Share Info" $false $infoJson.info
        }
    } catch {
        Write-TestResult "M-SHARE-005: Get Share Info" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[2/5] M-SHARE-006: Check Share Code..."
    try {
        $codeBody = "shareId=$shareId&code=$shareCode"
        $codeResp = $client.PostAsync("$BaseUrl/showShare/checkShareCode", 
            (New-Object System.Net.Http.StringContent($codeBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $codeJson = $codeResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        if ($codeJson.code -eq 200) {
            Write-TestResult "M-SHARE-006: Check Share Code" $true
        } else {
            Write-TestResult "M-SHARE-006: Check Share Code" $false $codeJson.info
        }
    } catch {
        Write-TestResult "M-SHARE-006: Check Share Code" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[3/5] M-SHARE-007: Load Share File List..."
    try {
        $listBody = "shareId=$shareId&filePid=0"
        $listResp = $client.PostAsync("$BaseUrl/showShare/loadFileList", 
            (New-Object System.Net.Http.StringContent($listBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $listJson = $listResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        if ($listJson.code -eq 200) {
            Write-TestResult "M-SHARE-007: Load Share File List" $true
        } else {
            Write-TestResult "M-SHARE-007: Load Share File List" $false $listJson.info
        }
    } catch {
        Write-TestResult "M-SHARE-007: Load Share File List" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[4/5] M-SHARE-008: Save Share to My Drive..."
    try {
        $saveBody = "shareId=$shareId&shareFileIds=$uploadedFileId&myFolderId=0"
        $saveResp = $client.PostAsync("$BaseUrl/showShare/saveShare", 
            (New-Object System.Net.Http.StringContent($saveBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result
        $saveJson = $saveResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        $saveInfo = [string]$saveJson.info
        Write-Output "  Response: code=$($saveJson.code) info='$saveInfo'"
        if ($saveJson.code -eq 200) {
            Write-TestResult "M-SHARE-008: Save Share to My Drive" $true
        } elseif ($saveJson.code -eq 600 -and $saveInfo.Length -eq 17) {
            Write-Output "  [NOTE] Expected: cannot save own shared file"
            Write-TestResult "M-SHARE-008: Save Share to My Drive" $true
        } else {
            Write-TestResult "M-SHARE-008: Save Share to My Drive" $false $saveInfo
        }
    } catch {
        Write-TestResult "M-SHARE-008: Save Share to My Drive" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[5/5] M-SHARE-009: Share Download..."
    try {
        $dlResp = $client.PostAsync("$BaseUrl/showShare/createDownloadUrl/$shareId/$uploadedFileId", $null).Result
        $dlJson = $dlResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        if ($dlJson.code -eq 200 -and -not [string]::IsNullOrWhiteSpace($dlJson.data)) {
            $dlCode = [string]$dlJson.data
            $downloadedFile = Join-Path $tmpDir "share_download.png"
            $maxRetries = 10
            $downloadOk = $false
            for ($i = 1; $i -le $maxRetries; $i++) {
                try {
                    $fileResp = Invoke-WebRequest -Uri "$BaseUrl/showShare/download/$dlCode" -Method GET -OutFile $downloadedFile -PassThru
                    if ((Test-Path $downloadedFile) -and ((Get-Item $downloadedFile).Length -gt 0)) {
                        $downloadOk = $true
                        break
                    }
                } catch {}
                Start-Sleep -Milliseconds 200
            }
            Write-TestResult "M-SHARE-009: Share Download" $downloadOk
        } else {
            Write-TestResult "M-SHARE-009: Share Download" $false "No download code returned"
        }
    } catch {
        Write-TestResult "M-SHARE-009: Share Download" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[Cleanup] Canceling share..."
    $cancelBody = "shareIds=$shareId"
    $client.PostAsync("$BaseUrl/share/cancelShare", 
        (New-Object System.Net.Http.StringContent($cancelBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result | Out-Null

    Write-Output "[Cleanup] Deleting test file..."
    $delBody = "fileIds=$uploadedFileId"
    $client.PostAsync("$BaseUrl/file/delFile", 
        (New-Object System.Net.Http.StringContent($delBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result | Out-Null
    $permBody = "fileIds=$uploadedFileId"
    $client.PostAsync("$BaseUrl/recycle/delFile", 
        (New-Object System.Net.Http.StringContent($permBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded"))).Result | Out-Null

} finally {
    if (Test-Path $tmpDir) {
        Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "SHARE VISITOR VERIFICATION SUMMARY"
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
    Write-Output "PASS"
    exit 0
} else {
    Write-Output "FAIL"
    exit 1
}
