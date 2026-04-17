@echo off
title Evidence Harbor — Starting...
set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot
set MVN=C:\Users\CamdenPD - Deidre\Maven\apache-maven-3.9.9\bin\mvn.cmd
cd /d "%~dp0"
echo Starting Evidence Harbor...
"%MVN%" javafx:run -f pom.xml
