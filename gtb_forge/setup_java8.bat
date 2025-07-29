@echo off
echo Setting up Java 8 environment for Forge 1.8.9 development
echo.

REM Check if Java 8 is installed
set "JAVA8_PATH="

REM Common Java 8 installation paths
if exist "C:\Program Files\Eclipse Adoptium\jdk-8.0.462.9-hotspot\bin\java.exe" (
    set "JAVA8_PATH=C:\Program Files\Eclipse Adoptium\jdk-8.0.462.9-hotspot"
)
if exist "C:\Program Files\Java\jdk1.8.0_*\bin\java.exe" (
    for /d %%i in ("C:\Program Files\Java\jdk1.8.0_*") do set "JAVA8_PATH=%%i"
)
if exist "C:\Program Files (x86)\Java\jdk1.8.0_*\bin\java.exe" (
    for /d %%i in ("C:\Program Files (x86)\Java\jdk1.8.0_*") do set "JAVA8_PATH=%%i"
)

if "%JAVA8_PATH%"=="" (
    echo ERROR: Java 8 not found!
    echo Please install Java 8 from: https://adoptium.net/temurin/releases/?package=jdk^&version=8
    echo After installation, run this script again.
    pause
    exit /b 1
)

echo Found Java 8 at: %JAVA8_PATH%

REM Update gradle.properties
echo Updating gradle.properties...
echo # Forge 1.8.9 specific configuration > gradle.properties
echo # Java 8 path for this project >> gradle.properties
echo org.gradle.java.home=%JAVA8_PATH:\=\\% >> gradle.properties
echo. >> gradle.properties
echo # Gradle JVM settings >> gradle.properties
echo org.gradle.jvmargs=-Xmx2G -XX:MaxMetaspaceSize=512m >> gradle.properties

echo.
echo Setup complete! You can now run:
echo   .\gradlew.bat setupDecompWorkspace
echo   .\gradlew.bat build
echo.
pause 