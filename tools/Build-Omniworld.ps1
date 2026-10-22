param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:VERSION_ROOT = '1.2'
$env:BUILD_NUMBER = '0'
Push-Location $repository
try {
    $buildArgs = @(':HMCL:makeExecutables', '--console=plain')
    if ($Offline) { $buildArgs += '--offline' }
    & .\gradlew.bat @buildArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
