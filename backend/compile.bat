@echo off
REM Set JAVA_HOME to Java 17
set JAVA_HOME=D:\software\javaenv\java17
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Java:
java -version
echo.

echo Compiling with Maven...
call mvn clean compile

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Compilation successful!
) else (
    echo.
    echo [FAILED] Compilation failed with error code %ERRORLEVEL%
    pause
)

