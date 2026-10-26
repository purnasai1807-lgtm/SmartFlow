@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK not found. Install a JDK and ensure java and javac are on PATH.
  pause
  exit /b 1
)
if not exist "lib\sqlite-jdbc-3.45.1.0.jar" (
  echo Missing SQLite JDBC dependency in lib\.
  pause
  exit /b 1
)
if not exist "bin" mkdir "bin"
echo Compiling SmartFlow...
javac -encoding UTF-8 -cp "lib/*" -d "bin" src\com\smartflow\Main.java src\com\smartflow\dao\*.java src\com\smartflow\exception\*.java src\com\smartflow\gui\*.java src\com\smartflow\model\*.java src\com\smartflow\service\*.java src\com\smartflow\thread\*.java src\com\smartflow\util\*.java
if errorlevel 1 (
  echo Compilation failed. Read the errors above.
  pause
  exit /b 1
)
echo Starting SmartFlow...
java --enable-native-access=ALL-UNNAMED -cp "bin;lib/*" com.smartflow.Main
if errorlevel 1 pause
