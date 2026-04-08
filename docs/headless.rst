Running the WRIMS Engine in Headless Mode
==========================================

Overview
--------

The WRIMS engine can be run **headless** (without the graphical interface) by invoking
the ``wrims-core`` JAR directly alongside its dependency JARs and native libraries.
This is useful for batch processing, automated testing, or running studies from a
CI/CD pipeline or scheduled task.

.. note::

   These instructions assume you are working from the root directory of the
   ``wrims-engine`` repository and that you have a Java 21 JDK installed.

Prerequisites
-------------

- Java 21 JDK (e.g., `Eclipse Temurin <https://adoptium.net/>`_)
- Gradle (the project includes a wrapper, so a local installation is not required)
- A WRIMS project directory containing a ``study.config`` file

Steps
-----

Step 1 — Build the ``wrims-core`` Module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Run the Gradle build task for ``wrims-core``. This compiles the module and produces
two directories:

- ``wrims-core/build/libs/`` — contains the ``wrims-core`` JAR
- ``wrims-core/build/tmp/libs/`` — contains all dependency JARs

.. code-block:: sh

   ./gradlew :wrims-core:build

Step 2 — Download Native Libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Run the ``getNatives`` task to download the required Windows DLL files. These are
placed in ``wrims-core/build/tmp/x64/`` and must be on the system ``PATH`` at
runtime.

.. code-block:: sh

   ./gradlew :wrims-core:getNatives

Step 3 — Configure the Launch Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A template batch file, ``run_wrims_study_example.bat``, is provided in the root of
the repository. Copy it and rename it for your project (e.g., ``run_project.bat``),
then edit the two project-specific variables at the top:

.. list-table::
   :widths: 25 75
   :header-rows: 1

   * - Variable
     - Description
   * - ``PROJECT_DIR``
     - Absolute path to your WRIMS project directory (e.g., ``J:\wrims\projects\dcr2023``)
   * - ``CONFIG_FILE``
     - Name of the study configuration file within that directory (e.g., ``study.config``)

You must also set ``JAVA_HOME`` to point to your Java 21 JDK installation.

The remaining variables control classpath and entry-point settings. **Do not change
these unless you have moved the script out of the repository root.**

Below is the full annotated batch file for reference:

.. code-block:: bat

   @echo off

   set PROJECT_DIR=J:\wrims\projects\dcr2023
   set CONFIG_FILE=study.config

   REM Path to your Java 21 JDK installation
   set JAVA_HOME="J:\java\jdk\jdk_21.0.8_temurin"

   set temp_wrims2=".\foo"

   REM Add native DLLs to PATH (relative to wrims-engine root — do not change)
   set PATH=%PATH%;wrims-core\build\tmp\x64

   REM Add project-specific external libraries to PATH (do not change)
   set PATH=%PATH%;%PROJECT_DIR%\Run\External

   REM Entry point for headless execution (do not change)
   set MAIN_CLASS=gov.ca.water.wrims.engine.core.components.ControllerBatch

   REM Classpath entries (do not change when running from wrims-engine root)
   set WRIMS_CORE_JAR="wrims-core\build\libs\*"
   set WRIMS_CORE_DEPENDENCIES="wrims-core\build\tmp\libs\*"

   REM Launch the engine with 4 GB heap and an extended thread stack size
   %JAVA_HOME%\bin\java -Xmx4096m -Xss1024K ^
       -cp "%WRIMS_CORE_JAR%;%WRIMS_CORE_DEPENDENCIES%" ^
       %MAIN_CLASS% ^
       -config=%PROJECT_DIR%\%CONFIG_FILE%

   pause

Step 4 — Run the Batch File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Execute the batch file from the ``wrims-engine`` root directory. You can either
double-click it in Windows Explorer or run it from a terminal:

.. code-block:: bat

   run_project.bat

The engine will start, load the study configuration, and run to completion. The
``pause`` at the end of the script keeps the terminal window open so you can review
any output or error messages before it closes.

Troubleshooting
---------------

.. list-table::
   :widths: 35 65
   :header-rows: 1

   * - Symptom
     - What to check
   * - ``java`` not found
     - Verify that ``JAVA_HOME`` points to a valid Java 21 JDK directory and that
       ``%JAVA_HOME%\bin\java.exe`` exists.
   * - Missing DLL errors on startup
     - Confirm that Step 2 completed successfully and that
       ``wrims-core\build\tmp\x64`` is populated.
   * - ``ClassNotFoundException`` for ``ControllerBatch``
     - Confirm that Step 1 completed successfully and that
       ``wrims-core\build\libs\`` and ``wrims-core\build\tmp\libs\`` are populated.
   * - Study config not found
     - Double-check that ``PROJECT_DIR`` and ``CONFIG_FILE`` together form the
       correct absolute path to your ``study.config`` file.
   * - Script must be run from the repository root
     - The classpath and DLL path entries are relative. Always launch the script
       from the ``wrims-engine`` root directory, not from a subdirectory.