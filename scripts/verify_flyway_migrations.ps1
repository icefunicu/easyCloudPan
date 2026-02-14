param(
    [string]$DbContainer = "easypan-postgres",
    [string]$DbUser = "postgres",
    [string]$DbName = "easypan",
    [string]$ExpectedLatestVersion = "10"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker not found in PATH."
}

function Invoke-PsqlScalar([string]$Sql) {
    $out = docker exec $DbContainer psql -U $DbUser -d $DbName -At -c $Sql 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed: $out"
    }
    return ($out | Select-Object -First 1).Trim()
}

try {
    $failed = Invoke-PsqlScalar "select count(*) from flyway_schema_history where success=false;"
    if ($failed -ne "0") {
        throw "Flyway has failed migrations: $failed"
    }

    $latest = Invoke-PsqlScalar "select version from flyway_schema_history where success=true and version is not null order by installed_rank desc limit 1;"
    if ([string]::IsNullOrWhiteSpace($latest)) {
        throw "Flyway schema history is present but no successful version found."
    }

    if ($latest -ne $ExpectedLatestVersion) {
        throw "Flyway latest version mismatch. expected=$ExpectedLatestVersion actual=$latest"
    }

    Write-Output "PASS: Flyway migrations OK (latest=$latest, failed=0)."
} catch {
    Write-Output ("FAIL: " + $_.Exception.Message)
    exit 1
}

