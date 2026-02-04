/**
 * Solver API package providing a unified interface for all WRIMS solvers.
 * 
 * <h2>Overview</h2>
 * This package defines a common API that all solver implementations must follow,
 * enabling the model coordinator to work with any solver without knowing
 * solver-specific details. This supports the creation of "controlling goals 
 * output files" by providing uniform access to sensitivity analysis data.
 * 
 * <h2>Key Classes</h2>
 * <ul>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.Solver} - 
 *       Base interface for all solvers</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.SolverSensitivity} - 
 *       Extended interface for sensitivity analysis (slack, surplus, dual values)</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.SolverResult} - 
 *       Encapsulates solve results</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.SolverFactory} - 
 *       Factory for creating solver instances</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.GoalAnalyzer} - 
 *       Analyzes results to find controlling goals</li>
 *   <li>{@link gov.ca.water.wrims.engine.core.solver.api.ControllingInfo} - 
 *       Information about what controls a variable</li>
 * </ul>
 * 
 * <h2>Architecture</h2>
 * <pre>
 *                    ┌─────────────────────┐
 *                    │   &lt;&lt;interface&gt;&gt;     │
 *                    │      Solver         │
 *                    └─────────┬───────────┘
 *                              │
 *                              │ extends
 *                              ▼
 *                    ┌─────────────────────┐
 *                    │   &lt;&lt;interface&gt;&gt;     │
 *                    │  SolverSensitivity  │
 *                    └─────────┬───────────┘
 *                              │
 *          ┌───────────────────┼───────────────────┐
 *          │                   │                   │
 *          ▼                   ▼                   ▼
 *   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
 *   │ CbcAdapter  │    │GurobiAdapter│    │  XAAdapter  │
 *   └─────────────┘    └─────────────┘    └─────────────┘
 * </pre>
 * 
 * <h2>Usage Example</h2>
 * <pre>
 * // Create solver via factory
 * Solver solver = SolverFactory.createSolver("CBC");
 * solver.initialize();
 * 
 * // Solve problem
 * SolverResult result = solver.solve();
 * 
 * // Analyze controlling goals (if sensitivity is supported)
 * if (solver instanceof SolverSensitivity) {
 *     GoalAnalyzer analyzer = new GoalAnalyzer((SolverSensitivity) solver);
 *     List&lt;ControllingInfo&gt; controlling = analyzer.findControllingGoals(varNames);
 *     // Write to output file...
 * }
 * 
 * solver.dispose();
 * </pre>
 * 
 * <h2>Sensitivity Analysis Support</h2>
 * <table border="1">
 *   <caption>Solver Sensitivity Analysis Support Matrix</caption>
 *   <tr><th>Solver</th><th>Slack/Surplus</th><th>Dual Values</th><th>Reduced Cost</th></tr>
 *   <tr><td>Gurobi</td><td>✓ Full</td><td>✓ Full</td><td>✓ Full</td></tr>
 *   <tr><td>CBC</td><td>✓ Limited</td><td>✓ Limited</td><td>✓ Limited</td></tr>
 *   <tr><td>XA</td><td>? TBD</td><td>? TBD</td><td>? TBD</td></tr>
 *   <tr><td>CLP</td><td>✓</td><td>✓</td><td>✓</td></tr>
 * </table>
 * 
 * @since 2.0
 * @see gov.ca.water.wrims.engine.core.solver.api.Solver
 * @see gov.ca.water.wrims.engine.core.solver.api.GoalAnalyzer
 */
package gov.ca.water.wrims.engine.core.solver.api;
