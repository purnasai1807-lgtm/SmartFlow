$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Error 'Java JDK not found. Install a JDK and ensure java and javac are on PATH.'
}
if (-not (Test-Path 'lib/sqlite-jdbc-3.45.1.0.jar')) {
    Write-Error 'Missing SQLite JDBC dependency in lib/.'
}
New-Item -ItemType Directory -Force -Path 'bin' | Out-Null
$files = Get-ChildItem -Path 'src' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
Write-Host 'Compiling SmartFlow...'
& javac -encoding UTF-8 -cp 'lib/*' -d 'bin' $files
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Read the errors above.' }
Write-Host 'Starting SmartFlow...'
& java --enable-native-access=ALL-UNNAMED -cp 'bin;lib/*' com.smartflow.Main
