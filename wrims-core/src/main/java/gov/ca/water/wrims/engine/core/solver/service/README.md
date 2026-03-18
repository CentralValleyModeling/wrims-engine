# Lookup Example for Solvers

## Global Singletons
Using the `Lookup.getDefault()` method provides access to the global instance singletons. The lookup
library has an integrated cache, so repeated calls to lookup the same singleton will return the same
instance. State is not maintained, however, so the calling class must ensure the same
object is in use.

## Structure
The `Solver` interface is the primary interface for the solver implementations. 
It is provided by the `SolverBroker` which finds the solver implementation based on 
the provided name.

The solver implementations live in the `solvers' package, with each solver type in its own sub-package.
This allows each solver to be included as its own JAR file, permitting independent solver development.

Shared classes are in the `shared` package, enabling the solver implementations to depend on them
without requiring knowledge of the other solvers.

## Tests
The `TestServiceLookup` class provides simple tests to show possible usages of the `SolverBroker`.

The `ModelProcess` class exists to mock a potential usage of the solver lookup functionality. it is
intended to represent the current solver instantiation implementation within the `ControllerBatch` 
class.

## Resources:
More information about the Netbeans Lookup API can be found here:
[NetBeans Lookup](https://netbeans.apache.org/wiki/main/netbeansdevelopperfaq/DevFaqLookup/).

See more about global singletons here: 
[Default Lookup](https://netbeans.apache.org/wiki/main/netbeansdevelopperfaq/DevFaqLookupDefault/).


