@echo off
REM PROJECT SETTINGS: PROJECT_DIR, and CONFIG_FILE variables as needed.
set PROJECT_DIR=C:\Users\hzamanis\Documents\Projects\Eli_work_DSM2Suurogate_Phase_II\calsurrogate-test
set CONFIG_FILE=__study.config


REM Optional: study-specific jars that must take precedence over the engine's bundled versions
REM (e.g. the calsurrogate build the study's Run\External classes were compiled against).
REM Leave empty to use the engine's bundled dependencies.
set "STUDY_LIBS=%~dp0external-lib\calsurrogate.jar"

REM Set the JAVA_HOME environment variable to the path of your java 21 JDK
set JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"

set temp_wrims2=".\foo"

REM Add the required DLLs to the PATH. Do not change if this if run from the wrims-engine root directory.
set PATH=%PATH%;wrims-core\build\tmp\x64

REM Add the external libraries to the PATH. Do not change.
set PATH=%PATH%;%PROJECT_DIR%\Run\External

REM Set the main class to run. This is the entry point for the WRIMS application. Do not change when running from wrims-engine root.
set MAIN_CLASS=gov.ca.water.wrims.engine.core.components.ControllerBatch
set WRIMS_CORE_JAR=wrims-core\build\libs\*
set WRIMS_CORE_DEPENDENCIES=wrims-core\build\tmp\libs\*
set "CLASSPATH=%PROJECT_DIR%\Run\External;%STUDY_LIBS%;%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%"

%JAVA_HOME%\bin\java -Xmx4096m -Xss1024K -Dproject.dir="%PROJECT_DIR%" -cp "%CLASSPATH%;%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%" %MAIN_CLASS% -config=%PROJECT_DIR%\%CONFIG_FILE%
pause