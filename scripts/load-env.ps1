param([string]$Path = ".env")
$ErrorActionPreference = "Stop"
# Parse assignments without executing file contents. Never print values.
foreach ($line in Get-Content -LiteralPath $Path) {
    $entry = $line.Trim()
    if (!$entry -or $entry.StartsWith("#")) { continue }
    if ($entry -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw "Invalid environment assignment" }
    $name = $Matches[1]
    $value = $Matches[2].Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}
Write-Output "Environment loaded."
