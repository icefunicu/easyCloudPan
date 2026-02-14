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

function New-TestImage {
    param([string]$TmpDir)
    $sourceFile = Join-Path $TmpDir "avatar_test.png"
    $pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mP8/x8AAwMB/6X8u6QAAAAASUVORK5CYII="
    [System.IO.File]::WriteAllBytes($sourceFile, [Convert]::FromBase64String($pngBase64))
    return $sourceFile
}

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan: Verify Update Avatar"
Write-Output "========================================"
Write-Output ""

$tmpDir = Join-Path $env:TEMP ("easypan_avatar_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

try {
    Write-Output "[1/4] Login..."
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
    Write-Output "[1.5/4] Get user info to get userId..."
    $userInfoResp = $client.GetAsync("$BaseUrl/getUserInfo").Result
    $userInfoJson = $userInfoResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
    if ($userInfoJson.code -ne 200) {
        throw "Failed to get user info"
    }
    $userId = [string]$userInfoJson.data.userId

    Write-Output ""
    Write-Output "[2/4] Get current avatar (before update)..."
    try {
        $avatarResp = Invoke-WebRequest -Uri "$BaseUrl/getAvatar/$userId" -Method Get -WebSession $session
        Write-TestResult "Get Avatar (before)" $true
    } catch {
        Write-TestResult "Get Avatar (before)" $false $_.Exception.Message
    }

    Write-Output ""
    Write-Output "[3/4] Update avatar..."
    $testImagePath = New-TestImage -TmpDir $tmpDir
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $fs = [System.IO.File]::OpenRead($testImagePath)
    try {
        $fileContent = New-Object System.Net.Http.StreamContent($fs)
        $fileContent.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue("image/png")
        $multipart.Add($fileContent, "avatar", "avatar_test.png")
        $updateResp = $client.PostAsync("$BaseUrl/updateUserAvatar", $multipart).Result
        $updateJson = $updateResp.Content.ReadAsStringAsync().Result | ConvertFrom-Json
        Write-TestResult "Update Avatar" ($updateJson.code -eq 200) $updateJson.info
    } finally {
        $fs.Dispose()
    }

    Write-Output ""
    Write-Output "[4/4] Get updated avatar..."
    try {
        $avatarResp2 = Invoke-WebRequest -Uri "$BaseUrl/getAvatar/$userId" -Method Get -WebSession $session
        Write-TestResult "Get Avatar (after)" $true
    } catch {
        Write-TestResult "Get Avatar (after)" $false $_.Exception.Message
    }

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
