@echo off
setlocal

REM Set PROJECT_DIR in the environment or pass the study directory as the first argument.
if not defined PROJECT_DIR if not "%~1"=="" set "PROJECT_DIR=%~1"
if not defined PROJECT_DIR (
    echo Set PROJECT_DIR or pass the study directory as the first argument.
    exit /b 1
)

if not defined CONFIG_FILE set "CONFIG_FILE=__study.config"
if not defined JAVA_HOME (
    echo Set JAVA_HOME to a Java 21 JDK.
    exit /b 1
)

cd /d "%~dp0"
if errorlevel 1 exit /b 1

set "STUDY_LIBS=%~dp0external-lib\calsurrogate.jar"
if not exist "%STUDY_LIBS%" (
    echo Missing study dependency: "%STUDY_LIBS%"
    exit /b 1
)

set "PATH=%PATH%;%~dp0wrims-core\build\tmp\x64;%PROJECT_DIR%\Run\External"

REM Set the main class to run. This is the entry point for the WRIMS application. Do not change when running from wrims-engine root.
set "MAIN_CLASS=gov.ca.water.wrims.engine.core.components.ControllerBatch"
set "WRIMS_CORE_JAR=wrims-core\build\libs\*"
set "WRIMS_CORE_DEPENDENCIES=wrims-core\build\tmp\libs\*"
set "CLASSPATH=%PROJECT_DIR%\Run\External;%STUDY_LIBS%;%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%"

if not exist "%PROJECT_DIR%\%CONFIG_FILE%" (
    echo Missing study config: "%PROJECT_DIR%\%CONFIG_FILE%"
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -Xmx4096m -Xss1024K -Dproject.dir="%PROJECT_DIR%" -Djava.library.path="%PROJECT_DIR%\Run\External;%~dp0wrims-core\build\tmp\x64" -cp "%CLASSPATH%" %MAIN_CLASS% -config="%PROJECT_DIR%\%CONFIG_FILE%"
set "WRIMS_EXIT_CODE=%ERRORLEVEL%"
pause
exit /b %WRIMS_EXIT_CODE%