# Evidence Harbor splash screen
# Shows a borderless window immediately on startup.
# Closes automatically when the main app creates the ready flag, or after a timeout.

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$flagFile = Join-Path $env:TEMP 'evidence-harbor-ready.flag'
# Clean any stale flag from a previous run
if (Test-Path $flagFile) { Remove-Item $flagFile -Force -ErrorAction SilentlyContinue }

$form = New-Object System.Windows.Forms.Form
$form.FormBorderStyle = 'None'
$form.StartPosition   = 'CenterScreen'
$form.Size            = New-Object System.Drawing.Size(560, 320)
$form.BackColor       = [System.Drawing.Color]::FromArgb(15, 23, 42)
$form.TopMost         = $true
$form.ShowInTaskbar   = $true
$form.Text            = 'Evidence Harbor'

# Subtle border
$form.Padding = New-Object System.Windows.Forms.Padding(2)

$title = New-Object System.Windows.Forms.Label
$title.Text      = 'Evidence Harbor'
$title.ForeColor = [System.Drawing.Color]::FromArgb(219, 234, 254)
$title.Font      = New-Object System.Drawing.Font('Segoe UI', 28, [System.Drawing.FontStyle]::Bold)
$title.AutoSize  = $false
$title.TextAlign = 'MiddleCenter'
$title.Dock      = 'Top'
$title.Height    = 110
$form.Controls.Add($title)

$sub = New-Object System.Windows.Forms.Label
$sub.Text      = 'Starting up...'
$sub.ForeColor = [System.Drawing.Color]::FromArgb(148, 163, 184)
$sub.Font      = New-Object System.Drawing.Font('Segoe UI', 12)
$sub.AutoSize  = $false
$sub.TextAlign = 'MiddleCenter'
$sub.Dock      = 'Top'
$sub.Height    = 40
$form.Controls.Add($sub)

$progress = New-Object System.Windows.Forms.ProgressBar
$progress.Style  = 'Marquee'
$progress.MarqueeAnimationSpeed = 30
$progress.Width  = 360
$progress.Height = 10
$progress.Location = New-Object System.Drawing.Point(100, 180)
$form.Controls.Add($progress)

$footer = New-Object System.Windows.Forms.Label
$footer.Text      = 'Loading Java runtime and database drivers...'
$footer.ForeColor = [System.Drawing.Color]::FromArgb(100, 116, 139)
$footer.Font      = New-Object System.Drawing.Font('Segoe UI', 9)
$footer.AutoSize  = $false
$footer.TextAlign = 'MiddleCenter'
$footer.Dock      = 'Bottom'
$footer.Height    = 36
$form.Controls.Add($footer)

# Poll for the ready flag; also bail after timeout.
$timer = New-Object System.Windows.Forms.Timer
$timer.Interval = 300
$script:ticks = 0
$script:maxTicks = 400   # 300ms * 400 = 120 seconds safety timeout
$timer.add_Tick({
    $script:ticks++
    if (Test-Path $flagFile) {
        $timer.Stop()
        Remove-Item $flagFile -Force -ErrorAction SilentlyContinue
        $form.Close()
        return
    }
    if ($script:ticks -ge $script:maxTicks) {
        $timer.Stop()
        $form.Close()
    }
})
$timer.Start()

[void]$form.ShowDialog()
