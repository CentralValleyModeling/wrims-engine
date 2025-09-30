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
   
# Dependabot Configuration
This repository uses Dependabot to keep dependencies up to date. The configuration file is located at `.github/dependabot.yml`. 

The current configuration checks for updates to dependencies weekly.

Dependabot will automatically create pull requests for dependency updates, which can then be reviewed and merged by the specified reviewers.
It is currently configured to check for updates to Gradle dependencies and GitHub Actions workflows.

The default reviewers for dependency update pull requests are configured in the `.github/CODEOWNERS` file.

More information about configuring Dependabot can be found in the [Dependabot configuration documentation](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuring-dependabot-version-updates).

## Understanding Dependabot Scheduling and PR Timing

---

### 1. Dependabot schedules are *best-effort*, not precise

* When you set `schedule.interval` and `schedule.time`, that’s treated as “run sometime around this time,” not a strict cron.
* GitHub’s docs note that it may not start exactly at the scheduled time—there’s a job queue that can be earlier or later depending on load.
* Seeing 9:20, 10:20, etc. is pretty normal—it’s the system spreading out runs.

---

### 2. Multiple runs at once

Dependabot sometimes makes multiple attempts in quick succession when:

* It detects multiple ecosystems (npm, gradle, docker, etc.) → one job per ecosystem.
* It encounters errors and retries.
* It had a backlog (maybe your repo was newly enabled) and caught up with several updates in bursts.

---

### 3. PR creation is **delayed from the update check**

Dependabot does two phases:

1. **Update check** (scans for new versions, resolves manifests).
2. **PR creation/update** (opens or rebases PRs).

The scan might happen in the morning, but PR creation often lags until later (sometimes hours). 
GitHub queues PR creation separately, and sometimes updates don’t get turned into visible PRs until 
the following day (especially for large dependency graphs).

---

### 4. Midnight PRs

That lines up with GitHub’s backend batching → Dependabot may only generate or finalize PRs after 
internal processing finishes. You’ll often see the “Dependabot created a PR” timestamp not match when the update job ran.

---

# SonarQube in-IDE Plugin Setup

To set up SonarQube analysis in your IDE, follow these steps:
1. **Install SonarLint Plugin:**
   - For IntelliJ IDEA: Go to `File > Settings > Plugins`, search for "SonarQube for IDE", and install it.
   - For Eclipse: Go to `Help > Eclipse Marketplace`, search for "SonarQube for IDE", and install it.
2. **Configure SonarQube Connection:**
   - Open the SonarQube for IDE plugin settings in your IDE.
     - For IntelliJ IDEA: `File > Settings > Tools > SonarQube for IDE`.
     - For Eclipse: `Window > Show View > Other > SonarQube > SonarQube Bindings`. 
   - Create a token for the DWR-CVM organization. 
     - Add Connection to SonarQube organization. 
       - For IntelliJ IDEA: Go to `File > Settings > Tools > SonarQube for IDE` and press the `+` button in the `Connections to SonarQube` section.
       - For Eclipse: Use the SonarQube Bindings panel to add a new connection by right-clicking in the view and selecting "New Connection...".
     - Select "SonarQube Cloud" on the left, and name the connection whatever you want to (recommended: `dwr-cvm`). Click "Next"
     - Click "Create Token" ("Generate token" in Eclipse). This sends you to SonarQube, and if you have an account, will automatically create an authentication token for you if you allow the connection. Log in with your GitHub credentials if prompted.
       - If using Eclipse, click "Next".
     - Connect to the `dwr-cvm` organization. 
       - For IntelliJ IDEA: Click "Select another organization" if it doesn't already show up. Type in `dwr-cvm` into the window that appears. That should add it to the list of "Your Organizations". Click "Next".
       - For Eclipse: The organization name will already be populated. Click "Next". Name the connection whatever you want to (recommended: `dwr-cvm`) and click "Next".
     - Choose whether you want to receive the SonarQube Cloud notifications. It is recommended to do so. Click "Next".
     - Finally,
       - For IntelliJ IDEA: Click "Create".
       - For Eclipse: Click "Finish".
3. **Bind the Project:**
   - After configuring the connection, bind your local project to the SonarQube project.
     - For IntelliJ IDEA: To do so, open the SonarQube for IDE plugin settings in your IDE.
       - `File > Settings > Tools > SonarQube for IDE > Project Settings`. The "Project Settings" option may be hidden by a dropdown arrow next to the "SonarQube for IDE" option.
       - Then select the "Bind Project to SonarCloud" option.
       - Choose the connection you created in the previous steps in the "Connection" dropdown.
       - Next, select the "Search in list..." option to choose the project. If you don't see the WRIMS-Engine project, you can enter the project key manually in the "Project key" entry box. The project key for WRIMS-Engine is `CentralValleyModeling_wrims-engine`.
       - Click "Apply", then "OK" to close the settings window.
     - For Eclipse: The binding menu is presented immediately after configuring the connection to the organization. 
       - Click "Add..." and select the project you want to bind. Then, click "Next". 
       - Choose the SonarQube project to bind to. This should be the project key for WRIMS-Engine, which is `CentralValleyModeling_wrims-engine`. Click "Finish".
   - This will enable SonarQube analysis for your project in the IDE.
4. **Run Analysis:**
   - You can now run SonarQube analysis directly from your IDE.
   - The plugin will provide real-time feedback on code quality and potential issues as you write code.