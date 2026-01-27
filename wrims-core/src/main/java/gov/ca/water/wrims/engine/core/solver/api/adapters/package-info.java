/**
 * Solver adapter implementations that wrap existing WRIMS solvers
 * to implement the unified Solver API.
 * 
 * <h2>Available Adapters</h2>
 * <ul>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.adapters.GurobiSolverAdapter} - 
 *       Full sensitivity support via Gurobi API</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.adapters.CbcSolverAdapter} - 
 *       Limited sensitivity support (jCbc JNI extension needed)</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.adapters.XASolverAdapter} - 
 *       Sensitivity support pending XA API verification</li>
 * </ul>
 * 
 * <h2>Sensitivity Analysis Support Matrix</h2>
 * <pre>
 * +------------------+-------+--------+--------+
 * | Feature          | Gurobi| CBC    | XA     |
 * +------------------+-------+--------+--------+
 * | Slack values     | ✓     | Pending| Pending|
 * | Dual values      | ✓     | Pending| Pending|
 * | Reduced costs    | ✓     | Pending| Pending|
 * | Bound checking   | ✓     | Pending| Pending|
 * +------------------+-------+--------+--------+
 * </pre>
 * 
 * @since 2.0
 * @see gov.ca.water.wrims.engine.core.solver.api.Solver
 * @see gov.ca.water.wrims.engine.core.solver.api.SolverSensitivity
 */
package gov.ca.water.wrims.engine.core.solver.api.adapters;
