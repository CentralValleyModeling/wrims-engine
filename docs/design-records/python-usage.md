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

## Considered Options

1. **Standardise on GraalPy** — replace JEP with GraalPy for all Python interoperability.
2. **Retain and update JEP** — keep JEP but upgrade it to a more recent Maven-available version.
3. **Maintain the status quo** — keep both integrations as-is without consolidation.

## Decision Outcome

**Chosen option: TBD** *(to be decided)*

This ADR is currently in a **Proposed** state. The recommended path is to standardise
on **GraalPy** (Option 1) when adequate test coverage can be established for
`Functionsuitablehabitat`, to avoid regression risk.

### Positive Consequences

- A single Python runtime reduces dependency complexity.
- GraalPy is already an existing Gradle dependency in the project.
- GraalPy is supported by Oracle and has a clear upgrade path.

### Negative Consequences

- `Functionsuitablehabitat` currently has **no test coverage**, making a safe
  refactor from JEP to GraalPy difficult to verify.
- Until tests are written, there is regression risk.

### Option 1: Standardise on GraalPy
GraalPy is an exploratory Python installation recently added to WRIMS-Engine as a
Gradle dependency. See: [GraalPy Documentation](https://www.graalvm.org/python/docs/).
Example usage is demonstrated in:
[Pull Request #67](https://github.com/CentralValleyModeling/wrims-engine/pull/67).

#### Pros:
- Already integrated as a Gradle dependency.
- Actively maintained by the GraalVM project.
- Only used in tests today — low impact if issues arise.

#### Cons:
- `Functionsuitablehabitat` has no tests to validate a JEP to GraalPy migration.

### Option 2: Retain and Update JEP

JEP (Java Embedded Python) is a Java library for embedding Python in the JVM.
See: [JEP Project](https://github.com/ninia/jep).

JEP is a direct dependency of
[`Functionsuitablehabitat.java`](../../wrims-core/src/main/java/wrimsv2/external/Functionsuitablehabitat.java),
which provides a native function call into the CalLite ANN (Artificial Neural Network) interface.
The initial 
[discussion](https://github.com/CentralValleyModeling/wrims-engine/discussions/82) 
reported it as unused, but that is not guaranteed, as a CalLite project could 
very well interact with this method.

#### Pros:
- Already (assumed) functional for its current use case.
- A newer version is available via Maven — low migration effort.

#### Cons:
- Requires maintaining a second Python runtime alongside GraalPy.
- Lacking test coverage to confirm refactor does not introduce regressions.
- Requires continued inclusion of an additional DLL file in the WRIMS-GUI project.

### Option 3: Maintain the Status Quo

Keep both GraalPy (for tests) and JEP (for `Functionsuitablehabitat`) without change.

#### Pros:
- Zero effort is required immediately.
- GraalPy provides modern Python functionality for future use.

#### Cons:
- Two Python runtimes increase complexity and maintenance burden.
- JEP may become stale or incompatible with future Java/Python versions. No test coverage
exists to validate JEP is currently functioning as intended.
- Requires continued inclusion of an additional DLL file in the WRIMS-GUI project.

## References
- [GitHub Discussion #82 — Python Dependency Report](https://github.com/CentralValleyModeling/wrims-engine/discussions/82)
- [GraalPy Documentation](https://www.graalvm.org/python/docs/)
- [JEP Project on GitHub](https://github.com/ninia/jep)
- [GraalPy Test Code — Pull Request #67](https://github.com/CentralValleyModeling/wrims-engine/pull/67)