@echo off
REM PROJECT SETTINGS: PROJECT_DIR, and CONFIG_FILE variables as needed.
set "PROJECT_DIR=C:\Users\hzamanis\Documents\Projects\calsim3-dcr-main_base"
set CONFIG_FILE=__study.config

REM Set the JAVA_HOME environment variable to the path of your java 21 JDK
set JAVA_HOME="C:\Program Files\Java\jdk-21"

set temp_wrims2=".\foo"

REM Add the required DLLs to the PATH. Do not change if this if run from the wrims-engine root directory.
set PATH=%PATH%;wrims-core\build\tmp\x64

REM Add the external libraries to the PATH. Do not change.
set PATH=%PATH%;%PROJECT_DIR%\Run\External

REM Set the main class to run. This is the entry point for the WRIMS application. Do not change when running from wrims-engine root.
set MAIN_CLASS=wrimsv2.components.ControllerBatch
set WRIMS_CORE_JAR="wrims-core\build\libs\*"
set WRIMS_CORE_DEPENDENCIES="wrims-core\build\tmp\libs\*"

%JAVA_HOME%\bin\java -Xmx4096m -Xss1024K -cp "%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%" %MAIN_CLASS% -config=%PROJECT_DIR%\%CONFIG_FILE%
pause