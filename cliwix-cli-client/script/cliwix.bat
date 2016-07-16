@echo off

REM
REM Cliwix CLI Client
REM Start script for Windows.
REM http://www.cliwix.com
REM

SET SCRIPT_DIR=%~dp0

if "%JAVA_HOME%" == "" goto withoutJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto withoutJavaHome
if "%JAVACMD%" == "" set JAVACMD=%JAVA_HOME%\bin\java.exe

:withoutJavaHome
if "%JAVACMD%" == "" set JAVACMD=java.exe

"%JAVACMD%" -jar "%SCRIPT_DIR%\cliwix-cli-client.jar" %*
SET exitcode=%errorlevel%
exit /B %exitcode%
