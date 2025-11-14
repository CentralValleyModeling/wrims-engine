@echo off
REM PROJECT SETTINGS: PROJECT_DIR, and CONFIG_FILE variables as needed.
set PROJECT_DIR=J:\wrims\projects\dcr2023
set CONFIG_FILE=study.config

REM Set the JAVA_HOME environment variable to the path of your java 21 JDK
set JAVA_HOME="J:\java\jdk\jdk_21.0.8_temurin"

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