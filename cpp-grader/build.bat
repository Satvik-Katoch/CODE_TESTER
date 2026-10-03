@echo off
rem ---------------------------------------------------------------------------
rem  Build script for Satvik's C++ Code Grader (Java edition)
rem
rem    build.bat           -> uses Maven if installed, otherwise plain javac
rem    build.bat --javac   -> force the plain javac build (no Maven needed)
rem
rem  Output: target\cpp-grader.jar
rem ---------------------------------------------------------------------------
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if /i not "%~1"=="--javac" (
    where mvn >nul 2>nul
    if !errorlevel!==0 (
        echo [build] Maven found - running "mvn package"...
        call mvn -q -B package
        if errorlevel 1 (
            echo [build] Maven build FAILED.
            exit /b 1
        )
        echo [build] OK  -^> target\cpp-grader.jar
        exit /b 0
    )
    echo [build] Maven not found - falling back to javac.
)

where javac >nul 2>nul
if errorlevel 1 (
    echo [build] javac not found. Install a JDK 17+ and add it to PATH.
    exit /b 1
)

if exist target\classes rmdir /s /q target\classes
mkdir target\classes 2>nul

rem javac @argfiles need quoted paths with forward slashes (folder names contain spaces)
(for /r "src\main\java" %%f in (*.java) do (
    set "p=%%f"
    echo "!p:\=/!"
)) > target\sources.txt

echo [build] Compiling...
javac --release 17 -encoding UTF-8 -d target\classes @target\sources.txt
if errorlevel 1 (
    echo [build] Compilation FAILED.
    exit /b 1
)

if exist src\main\resources xcopy /e /y /q /i src\main\resources target\classes >nul

jar --create --file target\cpp-grader.jar --main-class com.satvik.grader.App -C target\classes .
if errorlevel 1 (
    echo [build] Packaging FAILED.
    exit /b 1
)
echo [build] OK  -^> target\cpp-grader.jar
exit /b 0
