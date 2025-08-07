# Building WRIMS-ENGINE from Eclipse IDE
This document covers the configuration for building the WRIMS-ENGINE project from the Eclipse IDE using Gradle.

NOTE: The current WRIMS-ENGINE build is built on Java 21.
The recommended version of Eclipse uses Java 21 by default.
Setting the JAVA_HOME environment variable to the JDK 21 installation directory is recommended, but not required to build the project from Eclipse.

If you need to keep you JAVA_HOME set to java 1.8 for other projects you can manually run the gradle build by explicitly calling the java executable from the command line as described below.

# WRIMS-ENGINE Developer Build Setup - Using Gradle:
PREREQUISITES:
- Git
    - https://github.com/git-guides/install-git
- \<USER-DIR\>\\.gradle\gradle.properties configured with token for access to the CentralValleyModeling GitHub repository
    - Example: C:\\Users\\\<username\>\\.gradle\gradle.properties
    - NOTE: If you have to create the gradle.properties file, make sure it's not a "gradle.properties.txt" file.
```
...
cvmUserId=<githubUserId>
cvmPassword=<githubPersonalAccessToken>
...
```
The cvmPassword is preferably set to a GitHub personal access token that has been granted access to
the CentralValleyModeling GitHub repository with public_repo access and read:packages enabled.<br>

You can generate a personal access token here: https://github.com/settings/tokens/new
    
- Java JDK 21 installed (https://adoptium.net/en-GB/temurin/releases/?version=21)
  - NOTE: Any flavor of JDK 21 should work if you already have one installed. 

- IDE Eclipse
      - Latest Eclipse RCP download site:
      - https://www.eclipse.org/downloads/packages/release/2024-12/r/eclipse-ide-rcp-and-rap-developers

> [!WARNING]
> If you don't set the cvmUserId and cvmPassword in your gradle.properties file, you will get errors
> when you load the project into Eclipse. If that happens, you'll need to set the missing parameters in your gradle.properties file.
> Then remove and re-import the project to eclipse to clear the errors and load correctly.<br>
> Make sure your gradle.properties file is in the correct location under your user directory and not a .txt file.<br>
> <br>
> Never add your github username/token to any properties within project source code or it risks
> being committed & exposed to the public.

## 1. Pull Source from GitHub & build the project
- Clone the repository to your local machine.
    - Repository Clone URL: https://github.com/CentralValleyModeling/wrims-engine.git
- CD into the wrims-engine directory.
- Build the project with gradle from the command line

The following commands will clone the repository, and build the project from a command prompt.
Alternatively, you can use your preferred git repo manager tool to clone the wrims repo and checkout the wrims-devops branch.

### Example using Command Prompt or PowerShell on Windows
```
git clone https://github.com/CentralValleyModeling/wrims-engine.git
cd wrims-engine
.\gradlew build
```

### Common Build Errors
If you don't have the JAVA_HOME environment variable set to a JDK 21 installation directory, you may get errors like this:

```cmd
C:\GitHub\CVM2\wrims-engine>gradlew build

ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation.
```
Or if you have an older version of Java installed under you JAVA_HOME environment variable, you would see a similar java version error.
In this case you can either set the JAVA_HOME environment variable to the JDK 21 installation directory, or you can directly call the java executable from the command line like this:

/path/to/your/jdk/bin/java -jar gradle/wrapper/gradle-wrapper.jar build

```cmd example
C:\GitHub\CVM2\wrims-engine>C:\java\jdk-21.0.8.9-hotspot\bin\java -jar gradle/wrapper/gradle-wrapper.jar build
Starting a Gradle Daemon, 5 incompatible Daemons could not be reused, use --status for details

BUILD SUCCESSFUL in 29s
15 actionable tasks: 15 up-to-date
```

### HANDLING BUILD ERRORS FROM CMD or POWERSHELL: 
If you have a space in your java path you will need to wrap it in quotes like this: 
```cmd
"C:\Program Files\Java\jdk-21\bin\java" -jar gradle/wrapper/gradle-wrapper.jar build
```

If you encounter an error like: "invalid option: --release", try including the -Dorg.gradle.java.home option in your build command before for the -jar like this:

```cmd
C:\java\jdk-21\bin\java -Dorg.gradle.java.home=C:\java\jdk-21 -jar gradle/wrapper/gradle-wrapper.jar build
```

## 2. Open Eclipse with a new/clean workspace

Once you have built the wrims-engine project with Gradle from the command line, launch Eclipse.

Create a new workspace to import the project.

Using Eclipse 2024-12, the default Java version is 21, so you should not need to change the Java version in Eclipse.

NOTE: If you have an existing workspace, you can import the wrims-engine project into that workspace, but it is recommended to use a new workspace to avoid any conflicts with existing projects.
You will also need to make sure the workspace gradle and project settings are set to use the JDK 21 version you have installed.

## 3. Import the wrims-engine project into Eclipse
Close the "Welcome" window if it appears.
Click File-> Import... and select Import as an Existing Gradle Project

Select the wrims-engine project directory and click next.

Complete the import with default settings.

Click Finish to import.

Once the build is complete, you should see 0 errors in the Problems window.</br>
Gradle Tasks & Gradle Executions windows default to the bottom of the Eclipse window.

## 4. Build wrims-engine with Gradle task
From the "Gradle Tasks" window, run the "build" task on the root project.

