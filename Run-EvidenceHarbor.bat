@echo off
setlocal

set "APP_DIR=%~dp0"
set "MAVEN_EXE=C:\Program Files\Maven\apache-maven-3.9.9\bin\mvn.cmd"
set "JDK_HOME=C:\Program Files\Microsoft\jdk-21.0.10.7-hotspot"
set "JAVA_HOME=%JDK_HOME%"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JDK not found at: %JAVA_HOME%
  echo Please install Java 21.
  pause
  exit /b 1
)

if not exist "%MAVEN_EXE%" (
  echo Maven not found at: %MAVEN_EXE%
  echo Please install Maven 3.9.9 or update this launcher path.
  pause
  exit /b 1
)

pushd "%APP_DIR%"
call "%MAVEN_EXE%" -q -DskipTests javafx:run
set "RUN_EXIT=%ERRORLEVEL%"
popd

if not "%RUN_EXIT%"=="0" (
  echo Application failed to start. Exit code: %RUN_EXIT%
  pause
  exit /b %RUN_EXIT%
)

exit /b 0
