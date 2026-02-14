param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [switch]$ExpectDebugHeader,
    [switch]$ShowDebugCode
)

$ErrorActionPreference = "Stop"

$tmp = Join-Path $env:TEMP ("easypan_checkcode_" + [guid]::NewGuid().ToString("N") + ".bin")

$resp = Invoke-WebRequest -Uri "$BaseUrl/checkCode?type=0" -Method GET -OutFile $tmp -PassThru
$statusCode = [int]$resp.StatusCode
$contentType = [string]$resp.Headers["Content-Type"]

if ($statusCode -ne 200) {
    Write-Error "FAIL: /checkCode status=$statusCode"
    exit 1
}

if ($contentType -notmatch '^image/png') {
    Write-Error "FAIL: /checkCode Content-Type is not image/png. contentType=$contentType"
    exit 1
}

$bytes = [System.IO.File]::ReadAllBytes($tmp)
if ($bytes.Length -lt 8) {
    Write-Error "FAIL: /checkCode response too small: $($bytes.Length) bytes"
    exit 1
}

$pngSig = @(0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A)
for ($i = 0; $i -lt 8; $i++) {
    if ($bytes[$i] -ne $pngSig[$i]) {
        $magic = ($bytes[0..7] | ForEach-Object { $_.ToString("X2") }) -join " "
        Write-Error "FAIL: /checkCode magic mismatch. magic8=$magic"
        exit 1
    }
}

Write-Output "PASS: /checkCode returns image/png and PNG signature matches."

if ($ExpectDebugHeader) {
    $code = [string]$resp.Headers["X-EasyPan-CheckCode"]
    if ([string]::IsNullOrWhiteSpace($code)) {
        Write-Error "FAIL: /checkCode debug header missing: X-EasyPan-CheckCode"
        exit 1
    }
    Write-Output "PASS: /checkCode debug header is present (X-EasyPan-CheckCode)."
    if ($ShowDebugCode) {
        Write-Output ("checkCode=" + $code)
    }
}
