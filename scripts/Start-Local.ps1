param([switch]$LoadOnly)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Security
$secretPath = Join-Path $env:LOCALAPPDATA 'RecommendiA\secrets.dpapi.json'
if (-not (Test-Path -LiteralPath $secretPath)) { throw 'Configure JWT_SECRET, AI_KEY_ENCRYPTION_SECRET e DB_PASSWORD no ambiente ou provisione o arquivo DPAPI local.' }
$saved = Get-Content -Raw -LiteralPath $secretPath | ConvertFrom-Json
foreach ($entry in $saved.PSObject.Properties) {
    if ($entry.Name -notin @('JWT_SECRET', 'AI_KEY_ENCRYPTION_SECRET', 'DB_PASSWORD')) { continue }
    $bytes = [Security.Cryptography.ProtectedData]::Unprotect([Convert]::FromBase64String($entry.Value), $null, [Security.Cryptography.DataProtectionScope]::CurrentUser)
    [Environment]::SetEnvironmentVariable($entry.Name, [Text.Encoding]::UTF8.GetString($bytes), 'Process')
    [Array]::Clear($bytes, 0, $bytes.Length)
}
if ($LoadOnly) { return }
if (-not $env:DB_USERNAME) { throw 'Defina DB_USERNAME com a conta de banco apropriada antes de iniciar.' }
Push-Location (Split-Path $PSScriptRoot -Parent)
try { & .\mvnw.cmd spring-boot:run } finally { Pop-Location }
