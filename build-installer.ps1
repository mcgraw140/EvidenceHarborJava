param(
    [string]$VersionOverride = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $repoRoot

try {
    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "Maven (mvn) is not available on PATH."
    }
    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage is not available on PATH. Install JDK 21 and ensure jpackage is available."
    }

    [xml]$pom = Get-Content (Join-Path $repoRoot "pom.xml")
    $version = if ($VersionOverride -and $VersionOverride.Trim().Length -gt 0) {
        $VersionOverride.Trim()
    } else {
        $pom.project.version
    }

    Write-Host "Building shaded JAR..." -ForegroundColor Cyan
    & mvn -q clean package

    $jarName = "evidence-harbor-$version.jar"
    $jarPath = Join-Path $repoRoot (Join-Path "target" $jarName)
    if (-not (Test-Path $jarPath)) {
        $jarCandidate = Get-ChildItem (Join-Path $repoRoot "target") -Filter "evidence-harbor-*.jar" |
            Where-Object { $_.Name -notlike "original-*" } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if (-not $jarCandidate) {
            throw "Could not find packaged application jar in target/."
        }
        $jarName = $jarCandidate.Name
        $jarPath = $jarCandidate.FullName
    }

    $distRoot = Join-Path $repoRoot "dist"
    $appImageDest = Join-Path $distRoot "app-image"
    $usbBundleDir = Join-Path $distRoot "EvidenceHarbor-Installer"

    Remove-Item $appImageDest -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item $usbBundleDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $appImageDest | Out-Null
    New-Item -ItemType Directory -Path $usbBundleDir | Out-Null

    Write-Host "Creating application image with jpackage..." -ForegroundColor Cyan
    & jpackage --type app-image `
        --name "Evidence Harbor" `
        --input (Join-Path $repoRoot "target") `
        --dest $appImageDest `
        --main-jar $jarName `
        --main-class com.evidenceharbor.app.MainApp `
        --vendor "Evidence Harbor" `
        --app-version $version

    $imageDir = Join-Path $appImageDest "Evidence Harbor"
    if (-not (Test-Path $imageDir)) {
        throw "jpackage did not produce expected app image folder: $imageDir"
    }

    Copy-Item $imageDir -Destination (Join-Path $usbBundleDir "Evidence Harbor") -Recurse -Force

    $installTheseDir = Join-Path $repoRoot "Install These"
    if (Test-Path $installTheseDir) {
        Copy-Item $installTheseDir -Destination (Join-Path $usbBundleDir "Install These") -Recurse -Force
    }

    $repoDbProps = Join-Path $repoRoot "db.properties"
    $repoDbExample = Join-Path $repoRoot "db.properties.example"
    if (Test-Path $repoDbProps) {
        Copy-Item $repoDbProps -Destination (Join-Path $usbBundleDir "db.properties") -Force
    } elseif (Test-Path $repoDbExample) {
        Copy-Item $repoDbExample -Destination (Join-Path $usbBundleDir "db.properties") -Force
        Write-Host "No db.properties found — using db.properties.example as default config." -ForegroundColor Yellow
    }
    if (Test-Path $repoDbExample) {
        Copy-Item $repoDbExample -Destination (Join-Path $usbBundleDir "db.properties.example") -Force
    }

    @'
param(
    [string]$InstallDir = "$env:ProgramFiles\Evidence Harbor"
)

$ErrorActionPreference = "Stop"

$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $args = "-ExecutionPolicy Bypass -File `"$PSCommandPath`" -InstallDir `"$InstallDir`""
    Start-Process powershell -Verb RunAs -ArgumentList $args
    exit
}

$packageRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceDir = Join-Path $packageRoot "Evidence Harbor"
if (-not (Test-Path $sourceDir)) {
    throw "Installer payload folder not found: $sourceDir"
}

if (Test-Path $InstallDir) {
    Remove-Item $InstallDir -Recurse -Force
}
New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
Copy-Item (Join-Path $sourceDir "*") -Destination $InstallDir -Recurse -Force

$dbProps = Join-Path $packageRoot "db.properties"
if (Test-Path $dbProps) {
    Copy-Item $dbProps -Destination (Join-Path $InstallDir "db.properties") -Force
}

$exePath = Join-Path $InstallDir "Evidence Harbor.exe"
if (-not (Test-Path $exePath)) {
    throw "Expected executable not found after install: $exePath"
}

$desktop = [Environment]::GetFolderPath("Desktop")
$startMenu = Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs"
$shortcutTargets = @(
    Join-Path $desktop "Evidence Harbor.lnk",
    Join-Path $startMenu "Evidence Harbor.lnk"
)

$wsh = New-Object -ComObject WScript.Shell
foreach ($shortcutPath in $shortcutTargets) {
    $shortcut = $wsh.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $exePath
    $shortcut.WorkingDirectory = $InstallDir
    $shortcut.IconLocation = "$exePath,0"
    $shortcut.Save()
}

Write-Host "Installed Evidence Harbor to: $InstallDir" -ForegroundColor Green
Write-Host "Shortcuts created on Desktop and Start Menu." -ForegroundColor Green
Read-Host "Press Enter to exit"
'@ | Set-Content -Path (Join-Path $usbBundleDir "Install-EvidenceHarbor.ps1") -Encoding UTF8

    @'
param(
    [string]$InstallDir = "$env:ProgramFiles\Evidence Harbor"
)

$ErrorActionPreference = "Stop"

$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    $args = "-ExecutionPolicy Bypass -File `"$PSCommandPath`" -InstallDir `"$InstallDir`""
    Start-Process powershell -Verb RunAs -ArgumentList $args
    exit
}

if (Test-Path $InstallDir) {
    Remove-Item $InstallDir -Recurse -Force
}

$desktop = [Environment]::GetFolderPath("Desktop")
$startMenu = Join-Path $env:ProgramData "Microsoft\Windows\Start Menu\Programs"
$shortcutTargets = @(
    Join-Path $desktop "Evidence Harbor.lnk",
    Join-Path $startMenu "Evidence Harbor.lnk"
)
foreach ($shortcutPath in $shortcutTargets) {
    if (Test-Path $shortcutPath) {
        Remove-Item $shortcutPath -Force
    }
}

Write-Host "Evidence Harbor removed from: $InstallDir" -ForegroundColor Yellow
Read-Host "Press Enter to exit"
'@ | Set-Content -Path (Join-Path $usbBundleDir "Uninstall-EvidenceHarbor.ps1") -Encoding UTF8

    @'
@echo off
set SCRIPT_DIR=%~dp0
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Install-EvidenceHarbor.ps1"
'@ | Set-Content -Path (Join-Path $usbBundleDir "Install-EvidenceHarbor.bat") -Encoding ASCII

    @'
@echo off
set SCRIPT_DIR=%~dp0
powershell -ExecutionPolicy Bypass -File "%SCRIPT_DIR%Uninstall-EvidenceHarbor.ps1"
'@ | Set-Content -Path (Join-Path $usbBundleDir "Uninstall-EvidenceHarbor.bat") -Encoding ASCII

    @'
Evidence Harbor USB Installer Bundle

1) Copy this entire folder to a USB drive.
2) On each target PC, open this folder and run Install-EvidenceHarbor.bat as Administrator.
3) This installs Evidence Harbor to Program Files and creates Desktop/Start Menu shortcuts.

Included dependencies:
- Install These\mariadb-12.2.2-winx64.msi
- Install These\tailscale-setup-1.96.3.exe

Optional:
- Edit db.properties before install to set your host/user/password.
- db.properties.example shows the default format if you need to recreate it.
'@ | Set-Content -Path (Join-Path $usbBundleDir "README.txt") -Encoding ASCII

    Write-Host "" 
    Write-Host "Installer bundle created:" -ForegroundColor Green
    Write-Host $usbBundleDir -ForegroundColor Green
    Write-Host "Copy this folder to USB and run Install-EvidenceHarbor.bat on target PCs." -ForegroundColor Green
}
finally {
    Pop-Location
}
