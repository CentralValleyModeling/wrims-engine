# :steam_locomotive: WRIMS Engine

The compute engine for the **W**ater **R**esource **I**ntegrated **M**odeling System.

> [!WARNING]
> This is the development repository for a pre-release version of WRIMS **3**.
> Is you are wanting to submit an issue, or feature request for WRIMS **2** (which is the latest version of WRIMS), please do that through the [WRIMS 2 Repo](https://github.com/CentralValleyModeling/wrims).

[WRIMS](https://water.ca.gov/Library/Modeling-and-Analysis/Modeling-Platforms/Water-Resource-Integrated-Modeling-System) is DWR's generalized water resources modeling system for evaluating operational alternatives of large, complex river basins. WRIMS integrates a simulation language for flexible operational criteria specification, a linear programming solver for efficient water allocation decisions, and graphics capabilities for ease of use. These combined capabilities provide a comprehensive and powerful modeling tool for water resource systems simulation.

> [!NOTE]
> This is the development repository for the WRIMS 3 engine.
> The official distribution of WRIMS is available from the [DWR Library](https://water.ca.gov/Library/Modeling-and-Analysis/Modeling-Platforms/Water-Resource-Integrated-Modeling-System)
> Additionally, the [`wrims-gui`](https://github.com/CentralValleyModeling/wrims-gui) repository contains the code, tests, and documentation for the GUI Application that most users are familiar with. `wrims-gui` uses the Java modules in this repo.

This repository contains the code, tests, and developer documentation for the Java module `wrims-core`. WRIMS-core is a Java module used to run WRESL+ based models. WRIMS-core can be run as a command-line application from a batch process or shell script.

<!--- add additional descriptions of WRIMS-engine as development of alpha continues -->
<!-- write or link to information on developer installation, trainings, etc -->

Prior to the present revison of the WRIMS build system, the equivalent of WRIMS-core was a jar file named `WRIMSv2.jar`.

## Getting Started

1. **Clone the repository:**
   ```sh
   git clone https://github.com/CentralValleyModeling/wrims-engine.git
   ```
   
# RUNNING HEADLESS WRIMS ENGINE COMPUTE
The WRIMS engine can be run headless (without the GUI) using the `wrims-core` jar along with all dependency jars and libs. 
This is useful for running batch processes or automated tests.

Here are the required steps to run a headless compute with the WRIMS engine jar directly:

1. Build the wrims-core module. This will create a jar file in the `wrims-core/build/libs` directory and the copy the dependent jars into `wrims-core/build/tmp/libs`.
   ```sh
   ./gradlew :wrims-core:build
   ```
2. Run the "getNatives" task in the wrims-core module. This will download all the required dll files into the `wrims-core/build/tmp/x64` directory. 
   ```sh
    ./gradlew :wrims-core:getNatives
   ```
3. Clone the run_wrims_study_example.bat file in the root directory of the wrims-engine folder and update the project directory and config file paths.
   ```bat
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
   ```   

4. Run the batch file from a terminal or double-click it to execute the bat file.
   ```sh
   run_project.bat
   ```
