Set WshShell = CreateObject("WScript.Shell")
Set oShortcut = WshShell.CreateShortcut(WshShell.SpecialFolders("Desktop") & "\Evidence Harbor.lnk")
oShortcut.TargetPath       = "C:\Users\CamdenPD - Deidre\Desktop\EvidenceHarborJava\EvidenceHarborJava\launch.bat"
oShortcut.WorkingDirectory = "C:\Users\CamdenPD - Deidre\Desktop\EvidenceHarborJava\EvidenceHarborJava"
oShortcut.Description      = "Launch Evidence Harbor"
oShortcut.WindowStyle      = 7   ' Minimized (hides console window)
oShortcut.Save
MsgBox "Desktop shortcut 'Evidence Harbor' created!", 64, "Evidence Harbor"
