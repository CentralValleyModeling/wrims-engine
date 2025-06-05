# WRIMS-core: the compute engine for WRIMS

WRIMS is DWR's generalized water resources modeling system for evaluating operational alternatives of large, complex river basins.
[WRIMS](https://water.ca.gov/Library/Modeling-and-Analysis/Modeling-Platforms/Water-Resource-Integrated-Modeling-System) integrates a simulation language for flexible operational criteria specification, a linear programming solver for efficient water allocation decisions, and graphics capabilities for ease of use. These combined capabilities provide a comprehensive and powerful modeling tool for water resource systems simulation.

> [!NOTE]
> This repository contains the code, tests, and developer documentation for `wrims-core`, the computation engine for WRIMS. The [`wrims-gui`](https://github.com/CentralValleyModeling/wrims-gui) repository contains the code, tests, and documentation for the GUI Application of WRIMS. `wrims-gui` uses the build artifacts created by this repo.

WRIMS-core is a Java package that reads a WRIMS input file, processes the data, and writes the results to an output file.
WRIMS-core can be run as a command-line application from a batch process or shell scirpt.

Prior to the present revison of the WRIMS build system, the equivalent of WRIMS-core was a jar file named `WRIMSv2.jar`.
