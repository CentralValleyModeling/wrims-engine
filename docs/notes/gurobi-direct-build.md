# Gurobi Direct Build Notes

Summary of changes:
- Added a direct-build Gurobi path that constructs the model in memory from `SolverData` instead of reading a `.lp` file.
- Made LP writing for Gurobi respect the global ILP logging flags (no forced LP writes).
- Guarded Gurobi ILP output writes when LP logging is disabled and ensured `dvar_effective` is available for value logs.
- Updated `ControllerBatch` to use the direct-build path and only write ILP files when logging is enabled.

## Feasibility Relaxation Follow-up

Summary of changes:
- Replaced the old Gurobi fallback that reran the original model with looser parameters and removed IIS output from the Gurobi path.
- Added a `feasRelax` fallback in `GurobiSolver` that runs when the first solve returns `INFEASIBLE` or `INF_OR_UNBD`.
- Kept the original objective in the relaxed solve by using `feasRelax(..., minrelax=true, ...)`.
- Allowed relaxation of both variable bounds and linear constraints.
- Filtered out Gurobi-added artificial variables (`Art*`) before feeding solution values back into WRIMS.
- Wrote one per-event report file named like `<year_month_cycle>_feasrelax.txt` into `Run\=ILP=\<config>` and logged only artificial variables above the current Gurobi feasibility tolerance.
- Added a one-line console message when `feasRelax` is invoked.
- Guarded solution attribute reads so the code only requests `X` values when `SolCount > 0`, and records `solution_count` plus `no_solution_available` in the report otherwise.
- Added a second relaxed-model solve with `DualReductions=0` when the first relaxed solve returns `INF_OR_UNBD`, so Gurobi can distinguish infeasible from unbounded.
- Applied the old backup MIP settings to the post-`feasRelax` solve:
  `FeasibilityTol=1e-4`, `IntFeasTol=1e-4`, `MIPFocus=1`, and `ZeroObjNodes=1000`.

Observed behavior and interpretation:
- The first implementation invoked `feasRelax`, but some runs failed when the code tried to read variable values before Gurobi had a solution vector available. This was fixed by checking `SolCount` before reading `X`.
- After adding the `DualReductions=0` rerun, some relaxed solves were shown to be truly `INFEASIBLE` rather than ambiguously `INF_OR_UNBD`.
- After reapplying the old backup MIP settings to the relaxed solve, the model was able to continue through the failing timestep and write the `*_feasrelax.txt` report.
- This suggests the current fallback is a hybrid recovery mechanism:
  some timesteps may only need the coarser MIP settings to recover, while others may still require genuine nonzero `Art*` relaxations.
- If a report file contains no `Art*` entries above the reported threshold, that likely means the solve recovered under the relaxed MIP settings without needing meaningful bound/constraint violations.
- If the report file contains nonzero `Art*` entries, then `feasRelax` is materially changing the model to keep the WRIMS run moving.
