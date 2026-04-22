' Evidence Harbor launcher - fully hidden (no console flash)
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")
here = fso.GetParentFolderName(WScript.ScriptFullName)

' 1) Start the splash immediately (hidden PowerShell host, visible WinForms window)
sh.Run "powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File """ & here & "\splash.ps1""", 0, False

' 2) Launch Maven / JavaFX in the background - no console window
mvn = "C:\Users\CamdenPD - Deidre\Maven\apache-maven-3.9.9\bin\mvn.cmd"
cmd = "cmd /c set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot&& """ & mvn & """ javafx:run -f """ & here & "\pom.xml"""
sh.Run cmd, 0, False
