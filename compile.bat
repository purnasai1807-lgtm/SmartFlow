@echo off
title Compile SmartFlow
if not exist "bin" mkdir bin
echo Compiling SmartFlow sources...
javac -encoding UTF-8 -cp "lib/*" -d "bin" src/com/smartflow/*.java src/com/smartflow/model/*.java src/com/smartflow/dao/*.java src/com/smartflow/service/*.java src/com/smartflow/exception/*.java src/com/smartflow/thread/*.java src/com/smartflow/gui/*.java src/com/smartflow/util/*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful! Classes stored in bin/
) else (
    echo Compilation failed with error code %ERRORLEVEL%
)
pause
