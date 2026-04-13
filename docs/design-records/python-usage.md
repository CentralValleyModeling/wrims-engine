# Python Runtime Integration in WRIMS-Engine

## Status

Proposed

## Context and Problem Statement

WRIMS-Engine requires Python interoperability for certain external native functions
(e.g., ANN/CalLite integrations). Two Python runtime integrations are currently
present in the project: **JEP (Java Embedded Python)** and **GraalPy**. There is
a need to decide which integration should be standardized going forward and whether
the legacy JEP dependency should be retained, refactored, or removed.

Previous dependency analysis is documented in:
[GitHub Discussion #82](https://github.com/CentralValleyModeling/wrims-engine/discussions/82).

> Note: For Python usage in WRIMS-GUI, see the equivalent documentation in that repository.

## Decision Drivers

- Maintainability: prefer a single, well-supported Python integration.
- Test coverage: any replacement must not introduce untested regressions.
- Dependency management: prefer dependencies available via Maven/Gradle.
- Future extensibility: prefer the integration that offers the broadest future applicability.


## Evaluation boundary

This ADR evaluates Python integration within the WRIMS-Engine runtime only.

### Specifically

**In scope:**

- Python execution within WRIMS-Engine
- Integration mechanisms between Java and Python
- Dependencies introduced into the WRIMS-Engine runtime environment

**Out of scope (but considered as impacts):**
- WRIMS-GUI packaging and distribution concerns
- External modeling workflows (e.g., CalLite usage patterns)
- Client-side deployment environments

Impacts to WRIMS-GUI and deployment are considered as secondary consequences, not primary decision drivers.

## Open Questions and Validation Gaps

The following gaps in information introduce uncertainty into this decision and should be addressed prior to final adoption:

### Test Coverage Gaps
- `Functionsuitablehabitat` lacks automated test coverage  
- Current behavior under JEP is not formally verified  

### Runtime Compatibility
- It is not confirmed whether GraalPy can fully support the existing Python dependencies used by `Functionsuitablehabitat` (e.g., ANN/CalLite interactions)  

### Active Usage
- It is unclear whether `Functionsuitablehabitat` is actively used in production workflows  
- The impact of changes on existing CalLite workflows is not well understood  

### Integration Behavior
- Differences between JEP (CPython) and GraalPy (JVM-based Python) may affect numerical results or library compatibility  

### Operational Constraints
- Deployment and runtime constraints across environments have not been fully validated for both approaches  

### Recommended Validation Steps
- Add automated tests covering `Functionsuitablehabitat`  
- Validate GraalPy compatibility with required Python libraries  
- Confirm actual usage of JEP-dependent functionality with stakeholders  
- Perform side-by-side result comparison between JEP and GraalPy where applicable  

## Considered Options

1. **Standardise on GraalPy** — adopt GraalPy as the integration approach for Python execution within WRIMS-Engine runtime.
2. **Retain and update JEP** — keep JEP but upgrade it to a more recent Maven-available version.
3. **Maintain the status quo** — keep JEP integration as-is without change.

## Recommendations

**Chosen option: TBD** *(to be decided)*

This ADR is currently in a **Proposed** state. The recommended path is to standardize
on **GraalPy** (Option 1) once adequate test coverage is established for
`Functionsuitablehabitat` to avoid regression risk. Note that `Functionsuitablehabitat` is a current usage validation point 
that extends out to be a runtime architecture option for Python integration.

### Positive Consequences

- A single Python runtime reduces dependency complexity.
- GraalPy is already an existing Gradle dependency in the project.
- GraalPy is supported by Oracle and has a clear upgrade path with regular releases.

### Negative Consequences

- Lack of test coverage introduces uncertainty in validating a JEP to GraalPy migration.

## Downstream Impacts

The following impacts are outside the WRIMS-Engine decision boundary but are relevant for downstream systems.

These impacts are secondary and do not drive the runtime integration decision.

### WRIMS-GUI Packaging
- JEP requires distribution of native Python libraries and platform-specific dependencies (e.g., DLLs)  
- GraalPy reduces or eliminates native dependency packaging by remaining within the JVM ecosystem  

### Deployment Complexity
- JEP introduces tighter coupling to system-level Python environments  
- GraalPy enables more self-contained deployments  

### Option 1: Standardise on GraalPy

GraalPy is an exploratory Python installation recently added to WRIMS-Engine as a Gradle dependency. 
In this option, Python execution and Java/Python interoperability are handled within the WRIMS-Engine runtime 
using GraalPy as the in-process integration mechanism.

See: [GraalPy Documentation](https://www.graalvm.org/python/docs/).
Example usage is demonstrated in:
[Pull Request #67](https://github.com/CentralValleyModeling/wrims-engine/pull/67).

#### Pros:
- Actively maintained by the GraalVM project.
- Java-Python interoperability—GraalPy runs on the JVM, providing a seamless integration 
without separate processes or native DLLs required.
- Does not require additional platform-specific DLLs (unlike JEP).
- Fully managed by Gradle, simplifying dependency management and updates.
- Sandboxing is supported, allowing for restriction of Python's access to resources and
reducing potential security risks.
- Supports Python 3+.

#### Cons:
- Lack of test coverage introduces uncertainty in validating migration from JEP to GraalPy.
- Requires additional effort to migrate existing code.

### Option 2: Retain and Update JEP

JEP (Java Embedded Python) is a Java library for embedding Python in the JVM.
See: [JEP Project](https://github.com/ninia/jep).

JEP is the current Python integration mechanism used within WRIMS-Engine runtime for in-process Python execution and Java/Python interoperability. 
[`Functionsuitablehabitat.java`](../../wrims-core/src/main/java/wrimsv2/external/Functionsuitablehabitat.java) represents a current usage validation point 
for this integration in that it provides a native function call into the CalLite ANN (Artificial Neural Network) interface.
The initial [discussion](https://github.com/CentralValleyModeling/wrims-engine/discussions/82) reported it as unused, 
but that is not guaranteed, as a CalLite project could very well interact with this method.

#### Pros:
- Retains the existing in-process Python execution model within WRIMS-Engine runtime
- Java/Python interoperability remains unchanged via JEP
- Preserves compatibility with existing CalLite models (no loss of current usage)
- Already (assumed) functional for its current use case
- Upgrade path available via Maven-supported versions
- Lower migration effort compared to adopting a new runtime
- Directly embeds CPython into the JVM with Python 3.10+ support

#### Cons:
- Requires updating JEP, which has breaking API changes in the latest version.
- Lack of test coverage introduces uncertainty in validating changes and current behavior.
- Introduces/retains native runtime dependencies (CPython + JNI bindings)
- Ongoing maintenance burden for native integration
- Breaking API changes in newer JEP versions require refactor effort


### Option 3: Maintain the Status Quo

Retain the current JEP-based Python integration within WRIMS-Engine runtime without modification, 
preserving its use in `Functionsuitablehabitat` as the existing Python execution and interoperability mechanism.

#### Pros:
- No changes to current WRIMS-Engine runtime behavior for Python execution  
- Preserves existing Java/Python integration via JEP  
- No immediate development effort required  
- Preserves compatibility with existing CalLite models  
- Existing support for required Python libraries is maintained  
- Avoids introducing new regression risk from changes; however, current behavior remains unverified due to lack of test coverage  

#### Cons:
- Retains existing native dependency model and associated maintenance burden
- No modernization of Python integration approach
- Potential for future incompatibility with evolving Java/Python ecosystems
- Lack of test coverage leaves current behavior unverified

## References
- [GitHub Discussion #82 — Python Dependency Report](https://github.com/CentralValleyModeling/wrims-engine/discussions/82)
- [GraalPy Documentation](https://www.graalvm.org/python/docs/)
- [JEP Project on GitHub](https://github.com/ninia/jep)
- [GraalPy Test Code — Pull Request #67](https://github.com/CentralValleyModeling/wrims-engine/pull/67)
