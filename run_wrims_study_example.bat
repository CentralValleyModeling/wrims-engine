@echo off
setlocal EnableExtensions

set "WRIMS_ENGINE_DIR=C:\Users\bingl\IdeaProjects\main\wrims-engine"
cd /d "%WRIMS_ENGINE_DIR%"

set "PROJECT_DIR=C:\Users\bingl\9.3.1_danube_adj"
set "CONFIG_FILE=study.config"

set "JAVA_HOME=C:\Users\bingl\Downloads\jdk-21.0.8"

set "PATH=%PATH%;%WRIMS_ENGINE_DIR%\wrims-core\build\tmp\x64"
set "PATH=%PATH%;%PROJECT_DIR%\Run\External"

set "MAIN_CLASS=gov.ca.water.wrims.engine.core.components.ControllerBatch"
set "WRIMS_CORE_JAR=%WRIMS_ENGINE_DIR%\wrims-core\build\libs\*"
set "WRIMS_CORE_DEPENDENCIES=%WRIMS_ENGINE_DIR%\wrims-core\build\tmp\libs\*"

"%JAVA_HOME%\bin\java" -Xmx4096m -Xss1024K ^
  -Dproject.dir="%PROJECT_DIR%" ^
  -cp "%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%" ^
  %MAIN_CLASS% -config="%PROJECT_DIR%\%CONFIG_FILE%"

pause
