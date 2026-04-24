<#
    uninstall.ps1

    Silently uninstalls Evidence Harbor from this workstation by locating the
    MSI product code under HKLM Uninstall keys and invoking msiexec /x.

    Usage:
        .\uninstall.ps1              # interactive confirmation
        .\uninstall.ps1 -Force       # no prompt
        .\uninstall.ps1 -Quiet       # no UI at all (for scripted rollouts)
#>

[CmdletBinding()]
param(
    [switch]$Force,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'
$appName = 'Evidence Harbor'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok  ($msg) { Write-Host "    $msg" -ForegroundColor Green }
function Write-Warn2($msg){ Write-Host "    $msg" -ForegroundColor Yellow }

# Must run as admin to uninstall per-machine MSI installs
$currentId = [System.Security.Principal.WindowsIdentity]::GetCurrent()
$isAdmin   = (New-Object System.Security.Principal.WindowsPrincipal($currentId)).IsInRole(
    [System.Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warn2 "Re-launching with administrator privileges..."
    $argList = @('-NoProfile','-ExecutionPolicy','Bypass','-File',"`"$PSCommandPath`"")
    if ($Force) { $argList += '-Force' }
    if ($Quiet) { $argList += '-Quiet' }
    Start-Process -FilePath 'powershell.exe' -ArgumentList $argList -Verb RunAs -Wait
    exit $LASTEXITCODE
}

Write-Step "Locating installed '$appName'"

$uninstallPaths = @(
    'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKLM:\SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*',
    'HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*'
)
$entries = Get-ItemProperty $uninstallPaths -ErrorAction SilentlyContinue |
    Where-Object { $_.DisplayName -eq $appName -and $_.PSChildName -match '^\{[0-9A-Fa-f-]{36}\}$' }

if (-not $entries) {
    Write-Warn2 "'$appName' is not installed on this machine. Nothing to do."
    exit 0
}

foreach ($e in $entries) {
    $guid = $e.PSChildName
    $ver  = $e.DisplayVersion
    Write-Ok "Found: $appName $ver  $guid"

    if (-not $Force -and -not $Quiet) {
        $ans = Read-Host "Uninstall $appName $ver ? [y/N]"
        if ($ans -notmatch '^(y|yes)$') { Write-Warn2 "Skipped."; continue }
    }

    $log = Join-Path $env:TEMP "eh-uninstall-$($guid.Trim('{}')).log"
    $msiArgs = @('/x', $guid, '/L*V', "`"$log`"")
    if ($Quiet) { $msiArgs += '/qn' } else { $msiArgs += '/qb' }

    Write-Step "Running: msiexec $($msiArgs -join ' ')"
    $p = Start-Process -FilePath 'msiexec.exe' -ArgumentList $msiArgs -Wait -PassThru
    if ($p.ExitCode -eq 0) {
        Write-Ok "Uninstalled cleanly. Log: $log"
    } elseif ($p.ExitCode -eq 1605) {
        Write-Warn2 "Product already removed (MSI 1605). Log: $log"
    } else {
        Write-Host "    msiexec exited with $($p.ExitCode). See log: $log" -ForegroundColor Red
        exit $p.ExitCode
    }
}

# Clean up per-user data folder (optional; keep the DB config by default)
$userData = Join-Path $env:USERPROFILE 'EvidenceHarbor'
if (Test-Path $userData) {
    if ($Force -or $Quiet) {
        Write-Warn2 "Leaving user data intact: $userData"
    } else {
        $ans = Read-Host "Also delete user data folder '$userData' (contains db.properties)? [y/N]"
        if ($ans -match '^(y|yes)$') {
            Remove-Item $userData -Recurse -Force
            Write-Ok "Deleted $userData"
        } else {
            Write-Warn2 "Kept $userData"
        }
    }
}

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host " DONE - $appName has been uninstalled." -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green
