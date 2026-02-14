param(
    [string]$BaseUrl = "http://localhost:7090/api",
    [string]$Email,
    [string]$Password,
    [string]$TenantId = "default"
)

$ErrorActionPreference = "Continue"

if ([string]::IsNullOrWhiteSpace($Email)) {
    $Email = $env:EASYPAN_EMAIL
}
if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = $env:EASYPAN_PASSWORD
}

$scripts = @(
    "verify_health_check.ps1",
    "verify_checkcode_content_type.ps1",
    "verify_login_dual_token.ps1",
    "verify_get_user_info.ps1",
    "verify_refresh_token.ps1",
    "verify_logout_blacklist.ps1",
    "verify_minio_rw.ps1",
    "verify_redis_rw.ps1",
    "verify_upload_small_file.ps1",
    "verify_new_folder.ps1",
    "verify_rename_file.ps1",
    "verify_download_roundtrip.ps1",
    "verify_delete_to_recycle.ps1",
    "verify_recover_file.ps1",
    "verify_permanent_delete.ps1",
    "verify_share_file.ps1",
    "verify_share_visitor.ps1",
    "verify_admin.ps1",
    "verify_observability.ps1"
)

$passed = 0
$failed = 0
$results = @()

Write-Output ""
Write-Output "========================================"
Write-Output "EasyCloudPan Backend Verification Suite"
Write-Output "========================================"
Write-Output ""

foreach ($s in $scripts) {
    $path = Join-Path $PSScriptRoot $s
    if (Test-Path $path) {
        Write-Output "Running $s..."
        $output = & powershell -ExecutionPolicy Bypass -File $path 2>&1
        $exitCode = $LASTEXITCODE
        if ($exitCode -eq 0) {
            $passed++
            $results += "[PASS] $s"
            Write-Output "  Result: PASS"
        } else {
            $failed++
            $results += "[FAIL] $s"
            Write-Output "  Result: FAIL"
            # Keep output for diagnostics in the console while not relying on it for status.
            $output | ForEach-Object { Write-Output ("    " + $_) }
        }
    } else {
        Write-Output "  Skipped: $s not found"
    }
}

Write-Output ""
Write-Output "========================================"
Write-Output "BACKEND VERIFICATION SUMMARY"
Write-Output "========================================"
foreach ($r in $results) {
    Write-Output $r
}
Write-Output ""
Write-Output "Total: Passed=$passed, Failed=$failed"

if ($failed -eq 0) {
    Write-Output ""
    Write-Output "PASS"
    exit 0
} else {
    Write-Output ""
    Write-Output "FAIL"
    exit 1
}
