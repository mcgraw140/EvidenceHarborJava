@echo off
setlocal

set "APP_DIR=%~dp0"
set "JAR_PATH=%APP_DIR%target\evidence-harbor-1.0.0.jar"
set "JAVA_EXE=javaw"
set "MAVEN_EXE=C:\Program Files\Maven\apache-maven-3.9.9\bin\mvn.cmd"
set "JDK_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"

if exist "C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\javaw.exe" (
  set "JAVA_EXE=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot\bin\javaw.exe"
  set "JAVA_HOME=%JDK_HOME%"
)

if not exist "%JAR_PATH%" (
  echo First-time setup: building app jar...
  if not exist "%MAVEN_EXE%" (
    echo Maven not found at: %MAVEN_EXE%
    exit /b 1
  )
  pushd "%APP_DIR%"
  call "%MAVEN_EXE%" -q -DskipTests package
  set "BUILD_EXIT=%ERRORLEVEL%"
  popd
  if not "%BUILD_EXIT%"=="0" (
    echo Build failed.
    exit /b 1
  )
)

if not exist "%JAR_PATH%" (
  echo Could not find jar: %JAR_PATH%
  exit /b 1
)

start "EvidenceHarbor" "%JAVA_EXE%" -jar "%JAR_PATH%"
exit /b 0
