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
