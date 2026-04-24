<#
    build-installer.ps1

    Builds a professional Windows installer (setup.exe) for Evidence Harbor using
    the JDK's `jpackage` tool. The resulting installer behaves like any normal
    commercial Windows application:

      * Real setup wizard (welcome, license, install-dir chooser, progress, finish)
      * Application icon baked in
      * Desktop and Start Menu shortcuts (user-toggleable at install time)
      * Registered in Add/Remove Programs, with its own uninstaller
      * Per-version upgrade UUID so future installers upgrade in place
      * Bundled JRE (no separate Java install required on target PCs)

    Prerequisites on the build machine:
      * JDK 21+ on PATH (jpackage.exe)
      * Maven on PATH
      * WiX Toolset 3.11+ on PATH (candle.exe / light.exe)
            Install via:  winget install --id WiXToolset.WiXToolset
            or download:  https://github.com/wixtoolset/wix3/releases

    Usage:
        .\build-installer.ps1                # uses version from pom.xml
        .\build-installer.ps1 -Version 1.2.3
        .\build-installer.ps1 -Type msi      # produce .msi instead of .exe
        .\build-installer.ps1 -SkipBundle    # skip the USB bundle folder
#>

[CmdletBinding()]
param(
    [Alias('VersionOverride')]
    [string]$Version = "",

    [ValidateSet('exe','msi')]
    [string]$Type = 'exe',

    [switch]$SkipBundle
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $repoRoot

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok  ($msg) { Write-Host "    $msg" -ForegroundColor Green }
function Write-Warn2($msg){ Write-Host "    $msg" -ForegroundColor Yellow }

# -------------------------------------------------------------------------
# Convert a PNG to a multi-resolution .ico file (pure PowerShell, no deps).
# -------------------------------------------------------------------------
function Convert-PngToIco {
    param(
        [Parameter(Mandatory)] [string]$PngPath,
        [Parameter(Mandatory)] [string]$IconPath
    )
    Add-Type -AssemblyName System.Drawing

    $sizes = @(256, 128, 64, 48, 32, 16)
    $src   = [System.Drawing.Image]::FromFile((Resolve-Path $PngPath))
    $pngs  = @{}
    try {
        foreach ($s in $sizes) {
            $bmp = New-Object System.Drawing.Bitmap($s, $s, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $g   = [System.Drawing.Graphics]::FromImage($bmp)
            try {
                $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $g.Clear([System.Drawing.Color]::Transparent)
                $g.DrawImage($src, 0, 0, $s, $s)
            } finally { $g.Dispose() }

            $ms = New-Object System.IO.MemoryStream
            $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
            $pngs[$s] = $ms.ToArray()
            $ms.Dispose()
            $bmp.Dispose()
        }
    } finally { $src.Dispose() }

    if (Test-Path $IconPath) { Remove-Item $IconPath -Force }
    $fs = [System.IO.File]::Create($IconPath)
    $bw = New-Object System.IO.BinaryWriter($fs)
    try {
        # ICONDIR
        $bw.Write([UInt16]0)                # reserved
        $bw.Write([UInt16]1)                # type = icon
        $bw.Write([UInt16]$sizes.Count)     # image count

        $offset = 6 + (16 * $sizes.Count)
        foreach ($s in $sizes) {
            $bytes = $pngs[$s]
            $w = if ($s -ge 256) { 0 } else { [byte]$s }
            $h = if ($s -ge 256) { 0 } else { [byte]$s }
            $bw.Write([byte]$w)
            $bw.Write([byte]$h)
            $bw.Write([byte]0)              # color count
            $bw.Write([byte]0)              # reserved
            $bw.Write([UInt16]1)            # color planes
            $bw.Write([UInt16]32)           # bpp
            $bw.Write([UInt32]$bytes.Length)
            $bw.Write([UInt32]$offset)
            $offset += $bytes.Length
        }
        foreach ($s in $sizes) { $bw.Write($pngs[$s]) }
    } finally {
        $bw.Flush(); $bw.Dispose(); $fs.Dispose()
    }
}

try {
    # ---------------------------------------------------------------------
    # 0. Prereq checks
    # ---------------------------------------------------------------------
    Write-Step "Checking build prerequisites"

    if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
        throw "Maven (mvn) is not on PATH. Install Maven and try again."
    }
    if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
        throw "jpackage is not on PATH. Install JDK 21 and make sure JAVA_HOME\bin is on PATH."
    }

    $wixFound = $false
    if (Get-Command candle.exe -ErrorAction SilentlyContinue) { $wixFound = $true }
    if (-not $wixFound) {
        $pf86 = ${env:ProgramFiles(x86)}
        foreach ($v in @('v3.14','v3.11','v3.10')) {
            $wixBin = Join-Path $pf86 "WiX Toolset $v\bin"
            if (Test-Path (Join-Path $wixBin 'candle.exe')) {
                $env:Path = "$wixBin;$env:Path"
                $wixFound = $true
                break
            }
        }
    }
    if (-not $wixFound) {
        throw @"
WiX Toolset 3.x was not found. jpackage requires WiX to build Windows installers.

Install it one of these ways, then re-run this script:

  winget install --id WiXToolset.WiXToolset
  choco install wixtoolset
  Download: https://github.com/wixtoolset/wix3/releases  (wix311-binaries.zip)

If you already installed it, make sure the 'bin' folder (containing candle.exe
and light.exe) is on your PATH.
"@
    }
    Write-Ok "Maven, jpackage, and WiX Toolset detected."

    # ---------------------------------------------------------------------
    # 1. Resolve version
    # ---------------------------------------------------------------------
    [xml]$pom = Get-Content (Join-Path $repoRoot "pom.xml")
    if ($Version -and $Version.Trim().Length -gt 0) {
        $appVersion = $Version.Trim()
    } else {
        $appVersion = $pom.project.version
    }
    # jpackage requires numeric version (N[.N[.N]]); strip -SNAPSHOT etc.
    $appVersion = ($appVersion -replace '[^\d\.]','').Trim('.')
    if (-not $appVersion) { $appVersion = '1.0.0' }
    Write-Ok "Building version $appVersion"

    # ---------------------------------------------------------------------
    # 2. Build the shaded JAR
    # ---------------------------------------------------------------------
    Write-Step "Building shaded JAR (mvn clean package)"
    & mvn -q clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }

    $jarCandidate = Get-ChildItem (Join-Path $repoRoot "target") -Filter "evidence-harbor-*.jar" |
        Where-Object { $_.Name -notlike "original-*" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $jarCandidate) { throw "Could not find packaged application jar in target/." }
    $jarName = $jarCandidate.Name
    Write-Ok "Using jar: $jarName"

    # Strip any stray files from target\ so jpackage --input folder only holds jars
    $inputDir = Join-Path $repoRoot "target\installer-input"
    Remove-Item $inputDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item $jarCandidate.FullName -Destination $inputDir -Force

    # ---------------------------------------------------------------------
    # 3. Prepare icon + license + build dir
    # ---------------------------------------------------------------------
    Write-Step "Preparing installer assets"

    $buildDir = Join-Path $repoRoot "target\installer-build"
    Remove-Item $buildDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $buildDir | Out-Null

    $iconPath = Join-Path $buildDir "EvidenceHarbor.ico"
    $existingIco = Join-Path $repoRoot "src\main\EvidenceHarbor.ico"
    if (Test-Path $existingIco) {
        Copy-Item $existingIco $iconPath -Force
        Write-Ok "Using existing icon: $existingIco"
    } else {
        $pngIcon = Join-Path $repoRoot "src\main\ICON 2.png"
        if (-not (Test-Path $pngIcon)) {
            $pngIcon = Get-ChildItem (Join-Path $repoRoot "src\main") -Filter "*.png" -Recurse -ErrorAction SilentlyContinue |
                Sort-Object Length -Descending | Select-Object -First 1 | ForEach-Object FullName
        }
        if ($pngIcon -and (Test-Path $pngIcon)) {
            Write-Ok "Converting $pngIcon -> EvidenceHarbor.ico"
            Convert-PngToIco -PngPath $pngIcon -IconPath $iconPath
        } else {
            Write-Warn2 "No icon source found (src\main\ICON 2.png). Installer will use jpackage default."
            $iconPath = $null
        }
    }

    # License file for the installer wizard
    $licenseSrc = Join-Path $repoRoot "LICENSE"
    $licenseForInstaller = $null
    if (Test-Path $licenseSrc) {
        $licenseForInstaller = Join-Path $buildDir "LICENSE.txt"
        Copy-Item $licenseSrc $licenseForInstaller -Force
    }

    # ---------------------------------------------------------------------
    # 4. Run jpackage
    # ---------------------------------------------------------------------
    $outDir = Join-Path $repoRoot "dist"
    Remove-Item $outDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null

    # Stable UUID so future installers upgrade in place instead of installing
    # side-by-side. DO NOT change this value between releases.
    $upgradeUuid = '6e6f2b8e-7d3b-4b2d-9a2f-0e7c0b1e9a01'

    $jpackageArgs = @(
        '--type',          $Type
        '--name',          'Evidence Harbor'
        '--app-version',   $appVersion
        '--vendor',        'Evidence Harbor'
        '--description',   'Law-enforcement evidence management, impound tracking, and quartermaster workflows.'
        '--copyright',     "Copyright (c) $((Get-Date).Year) Evidence Harbor"
        '--input',         $inputDir
        '--dest',          $outDir
        '--main-jar',      $jarName
        '--main-class',    'com.evidenceharbor.app.Launcher'
        '--java-options',  '-Dfile.encoding=UTF-8'
        '--win-dir-chooser'
        '--win-menu'
        '--win-menu-group','Evidence Harbor'
        '--win-shortcut'
        '--win-shortcut-prompt'
        '--win-upgrade-uuid', $upgradeUuid
    )
    if ($iconPath)             { $jpackageArgs += @('--icon',         $iconPath) }
    if ($licenseForInstaller)  { $jpackageArgs += @('--license-file', $licenseForInstaller) }

    Write-Step "Running jpackage (this can take a minute or two)"
    & jpackage @jpackageArgs
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE." }

    # --------------------------------------------------------------------
    # Sanity check: confirm the shaded jar's manifest Main-Class is the
    # Launcher (not MainApp). If MainApp is used, the JVM's JavaFX module
    # check fires at startup and the installed app fails with
    # "JavaFX runtime components are missing".
    # --------------------------------------------------------------------
    $manifestJar = $jarCandidate.FullName
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($manifestJar)
    try {
        $entry = $zip.GetEntry('META-INF/MANIFEST.MF')
        if ($entry) {
            $sr = New-Object System.IO.StreamReader($entry.Open())
            $manifestText = $sr.ReadToEnd(); $sr.Dispose()
            if ($manifestText -match 'Main-Class:\s*(\S+)') {
                $jarMain = $Matches[1]
                if ($jarMain -ne 'com.evidenceharbor.app.Launcher') {
                    throw "Shaded jar Main-Class is '$jarMain' (expected 'com.evidenceharbor.app.Launcher'). " +
                          "This will cause 'JavaFX runtime components are missing' at launch. " +
                          "Check maven-shade-plugin's ManifestResourceTransformer in pom.xml."
                }
                Write-Ok "Jar manifest Main-Class: $jarMain"
            }
        }
    } finally { $zip.Dispose() }

    $installer = Get-ChildItem $outDir -Filter "Evidence Harbor-*.$Type" |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $installer) { throw "jpackage did not produce an installer in $outDir." }

    $finalName    = "EvidenceHarbor-Setup-$appVersion.$Type"
    $finalInstaller = Join-Path $outDir $finalName
    Remove-Item $finalInstaller -Force -ErrorAction SilentlyContinue
    Move-Item $installer.FullName $finalInstaller -Force
    Write-Ok "Installer built: $finalInstaller"

    # ---------------------------------------------------------------------
    # 5. Optional USB bundle (installer + MariaDB/Tailscale prereqs)
    # ---------------------------------------------------------------------
    if (-not $SkipBundle) {
        Write-Step "Building USB bundle folder"

        $bundleDir = Join-Path $outDir "EvidenceHarbor-Installer-$appVersion"
        Remove-Item $bundleDir -Recurse -Force -ErrorAction SilentlyContinue
        New-Item -ItemType Directory -Path $bundleDir | Out-Null

        Copy-Item $finalInstaller -Destination (Join-Path $bundleDir $finalName) -Force

        # Ship the standalone uninstaller scripts alongside the installer.
        $uninstallPs1 = Join-Path $repoRoot "uninstall.ps1"
        $uninstallBat = Join-Path $repoRoot "uninstall.bat"
        if (Test-Path $uninstallPs1) {
            Copy-Item $uninstallPs1 -Destination (Join-Path $bundleDir "uninstall.ps1") -Force
        }
        if (Test-Path $uninstallBat) {
            Copy-Item $uninstallBat -Destination (Join-Path $bundleDir "uninstall.bat") -Force
        }

        $installThese = Join-Path $repoRoot "Install These"
        if (Test-Path $installThese) {
            Copy-Item $installThese -Destination (Join-Path $bundleDir "Install These") -Recurse -Force
        }

        $repoDbProps   = Join-Path $repoRoot "db.properties"
        $repoDbExample = Join-Path $repoRoot "db.properties.example"
        if (Test-Path $repoDbExample) {
            Copy-Item $repoDbExample -Destination (Join-Path $bundleDir "db.properties.example") -Force
        }
        if (Test-Path $repoDbProps) {
            Copy-Item $repoDbProps -Destination (Join-Path $bundleDir "db.properties") -Force
        }

        $readme = @"
Evidence Harbor $appVersion - Installer Bundle
==============================================

To install on a workstation:

  1. Double-click  $finalName
  2. Follow the setup wizard (license, install folder, shortcuts).
  3. On first launch, enter your MariaDB server details.

Optional dependencies (for the database server / remote sites):

  Install These\mariadb-*.msi       - Install on the machine that will host the DB
  Install These\tailscale-setup-*.exe - Optional VPN for multi-site deployments

Pre-seeding database config:

  Drop a customized db.properties into %USERPROFILE%\EvidenceHarbor\
  before first launch to skip the setup wizard. An example is included.

Uninstalling:

  Use Windows "Settings > Apps" or "Add or Remove Programs" and remove
  "Evidence Harbor" like any other application.

  Or run  uninstall.bat  from this bundle for a scripted/silent removal:
      uninstall.bat            (prompts before removing)
      uninstall.bat -Force     (no prompt)
      uninstall.bat -Quiet     (no UI, scripted rollouts)
"@
        Set-Content -Path (Join-Path $bundleDir "README.txt") -Value $readme -Encoding ASCII
        Write-Ok "Bundle: $bundleDir"
    }

    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host " DONE - Professional installer ready." -ForegroundColor Green
    Write-Host " Installer: $finalInstaller" -ForegroundColor Green
    if (-not $SkipBundle) {
        Write-Host " Bundle:    $(Join-Path $outDir "EvidenceHarbor-Installer-$appVersion")" -ForegroundColor Green
    }
    Write-Host "================================================================" -ForegroundColor Green
}
finally {
    Pop-Location
}
