@echo off
REM Launches Evidence Harbor via VBScript so there are zero visible console windows.
start "" wscript.exe "%~dp0launch.vbs"
exit /b
