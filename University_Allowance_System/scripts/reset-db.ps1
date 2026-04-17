# Reset the university.db using data/demo.sql (Windows PowerShell)
# Usage: .\reset-db.ps1

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$dbFile = Join-Path $projectRoot "university.db"
$demoSql = Join-Path $projectRoot "data\demo.sql"

if (-Not (Test-Path $demoSql)) {
    Write-Error "demo.sql not found at $demoSql"
    exit 1
}

if (Test-Path $dbFile) {
    $backup = $dbFile + ".bak"
    Copy-Item -Path $dbFile -Destination $backup -Force
    Write-Host "Backed up existing database to $backup"
}

# Create a new database and execute demo.sql
$sqlite = "sqlite3"
if (-Not (Get-Command $sqlite -ErrorAction SilentlyContinue)) {
    Write-Error "sqlite3 CLI not found in PATH. Please install sqlite3 or run demo.sql using a DB tool."
    exit 1
}

& $sqlite $dbFile ".read $demoSql"

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database reset using demo.sql"
} else {
    Write-Error "Failed to reset database. sqlite3 exit code: $LASTEXITCODE"
}

