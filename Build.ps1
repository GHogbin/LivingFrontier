param([switch]$SkipGameTests)

$ErrorActionPreference = 'Stop'
$project = $PSScriptRoot
$portable = Get-ChildItem -LiteralPath (Join-Path $project '.tools') -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
    Select-Object -First 1
if ($portable) {
    $env:JAVA_HOME = $portable.FullName
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    throw 'Install a Java 17 JDK and set JAVA_HOME before building.'
}
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$versionLine = Select-String -LiteralPath (Join-Path $project 'gradle.properties') -Pattern '^mod_version=(.+)$'
if (@($versionLine).Count -ne 1) {
    throw 'Expected exactly one mod_version in gradle.properties.'
}
$version = $versionLine.Matches[0].Groups[1].Value.Trim()
$hasher = [Security.Cryptography.SHA256]::Create()
try {
    $identity = ([BitConverter]::ToString($hasher.ComputeHash(
        [Text.Encoding]::UTF8.GetBytes($project))) -replace '-', '').Substring(0, 12)
} finally {
    $hasher.Dispose()
}
$build = Join-Path $env:LOCALAPPDATA "LivingFrontierForge\build\$identity\$version"
$tasks = @('clean', 'build')
if (-not $SkipGameTests) {
    $tasks += 'runGameTestServer'
}
Push-Location -LiteralPath $project
try {
    & '.\gradlew.bat' --no-daemon --console=plain "-PfrontierBuildDir=$build" @tasks
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed with exit code $LASTEXITCODE"
    }
    $artifact = "living-frontier-1.20.1-forge-$version.jar"
    $destination = Join-Path $project 'dist'
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $build "libs\$artifact") -Destination $destination -Force
    Write-Output "Mod ready: $(Join-Path $destination $artifact)"
} finally {
    Pop-Location
}
