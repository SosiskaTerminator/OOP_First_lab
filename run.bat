@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

rem ==== settings ====
rem Entry point. Change it to ui.ConsoleUI once the console menu is ready.
set "MAIN=demo.OrderDemo"

set "ROOT=%~dp0"
set "LIB=%ROOT%lib\sqlite-jdbc.jar"
set "OUT=%ROOT%out"
set "SOURCES=%ROOT%sources.txt"

rem ==== checks ====
where javac >nul 2>nul
if errorlevel 1 (
    echo [ERROR] javac not found. Install JDK 17+ and add it to PATH.
    exit /b 1
)

if not exist "%LIB%" (
    echo [ERROR] Driver not found: %LIB%
    exit /b 1
)

rem ==== compile ====
echo [1/2] Compiling...
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

if exist "%SOURCES%" del "%SOURCES%"
for /r "%ROOT%src" %%f in (*.java) do (
    set "p=%%~ff"
    set "p=!p:\=/!"
    >>"%SOURCES%" echo !p!
)

javac -encoding UTF-8 -d "%OUT%" -cp "%LIB%" @"%SOURCES%"
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    del "%SOURCES%"
    exit /b 1
)
del "%SOURCES%"

rem ==== run ====
echo [2/2] Running %MAIN%...
java -Dfile.encoding=UTF-8 -cp "%OUT%;%LIB%" %MAIN%
if errorlevel 1 (
    echo [ERROR] Program exited with an error.
    exit /b 1
)

echo Done.
endlocal
