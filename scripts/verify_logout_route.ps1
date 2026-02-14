param(
    [string]$BaseUrl = "http://localhost:7090/api"
)

$ErrorActionPreference = "Stop"

try {
    $resp = Invoke-WebRequest -Uri "$BaseUrl/logout" -Method POST -MaximumRedirection 0 -ErrorAction Stop
} catch {
    if ($_.Exception.Response -ne $null) {
        $resp = $_.Exception.Response
    } else {
        throw
    }
}

$statusCode = [int]$resp.StatusCode
$location = $resp.Headers["Location"]
$body = ""

if ($resp.PSObject.Properties.Name -contains "Content") {
    $body = [string]$resp.Content
}

if ($statusCode -eq 302 -and $location -like "*login?logout*") {
    Write-Error "FAIL: /logout is still intercepted by Spring Security default logout. Location=$location"
    exit 1
}

if ($statusCode -ne 200) {
    Write-Error "FAIL: unexpected HTTP status: $statusCode"
    exit 1
}

if ($body -notmatch '"status"\s*:\s*"success"') {
    Write-Error "FAIL: response body is not success. body=$body"
    exit 1
}

Write-Output "PASS: /logout returns 200 success and is not redirected."
