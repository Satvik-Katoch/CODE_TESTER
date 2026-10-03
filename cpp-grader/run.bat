@echo off
rem Launches the grader (builds it first if the jar is missing).
rem   run.bat            -> start without a console window
rem   run.bat --console  -> start attached to this console (see exceptions)
setlocal
cd /d "%~dp0"

if not exist target\cpp-grader.jar (
    call build.bat
    if errorlevel 1 exit /b 1
)

if /i "%~1"=="--console" (
    java -jar target\cpp-grader.jar
) else (
    start "" javaw -jar target\cpp-grader.jar
)
