' Evidence Harbor silent launcher
' Runs the batch file without showing a CMD prompt window.
Dim WshShell, scriptDir
Set WshShell = CreateObject("WScript.Shell")
scriptDir = Left(WScript.ScriptFullName, InStrRev(WScript.ScriptFullName, "\"))
WshShell.Run Chr(34) & scriptDir & "Evidence Harbor.bat" & Chr(34), 0, False
Set WshShell = Nothing
