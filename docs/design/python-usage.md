# Python Usage within WRIMS-Engine

## Libraries

Throughout WRIMS, there are a number of Python installations that are present.
These have different purposes and contexts for their presence in the project.
For WRIMS-GUI, see the equivalent documentation in the repository of that project.

## Previous Dependency Report
The information collected in previous reports on the usage of Python within WRIMS-Engine 
can be found here:
[GitHub Discussion](https://github.com/CentralValleyModeling/wrims-engine/discussions/82).

### Python (GraalPy)
This is the exploratory Python installation that was recently implemented for WRIMS-Engine. It is
provided by GraalPy, which was added as a Gradle dependency to the project. There is test code
representing the usage of this installation that can be found here:
[GraalPy Test Code Pull Request](https://github.com/CentralValleyModeling/wrims-engine/pull/67).

Further usage of Python should be done through the GraalPy installation if possible. More
information on GraalPy can be found here:
[GraalPy Documentation](https://www.graalvm.org/python/docs/).

### JEP (Java Embedded Python)
This is the Python installation that is currently used within WRIMS-Engine. It is provided by JEP,
which is a Java library that allows Python to be embedded within Java. More information on JEP
can be found here:
[JEP Project](https://github.com/ninia/jep).

JEP is included directly as a dependency for the 
[Functionsuitablehabitat.java](../../wrims-core/src/main/java/wrimsv2/external/Functionsuitablehabitat.java) 
external native function class. The initial 
[discussion](https://github.com/CentralValleyModeling/wrims-engine/discussions/82) 
reported it as unused, but that is not guaranteed, as a CalLite project could 
very well interact with this method.

This class provides a native function call into the CalLite ANN (Artificial Neural Network) 
interface and has no test coverage. This could be replaced with GraalVM Python calls, 
as that is already incorporated into the WRIMS Engine project, but there are no currently 
existing tests to confirm that a refactor would not introduce regressions.

Alternatively, this dependency could be retained and updated to a more recent version
available via Maven.