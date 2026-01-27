package gov.ca.water.wrims.engine.core.solver.api;

import java.util.Map;

/**
 * Common interface for all WRIMS solvers.
 * 
 * This interface defines the contract that all solver implementations must follow,
 * allowing the model coordinator to work with any solver without knowing 
 * solver-specific details.
 * 
 * Implementations: CbcSolverAdapter, GurobiSolverAdapter, XASolverAdapter, etc.
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public interface Solver {
    
    /**
     * Initializes the solver environment.
     * Should be called once before any solve operations.
     * 
     * @throws SolverException if initialization fails
     */
    void initialize() throws SolverException;
    
    /**
     * Loads a new model/problem into the solver.
     * Clears any previous problem data.
     */
    void loadNewModel();
    
    /**
     * Sets the decision variables for the current problem.
     * 
     * @param dvarMap map of variable names to their definitions
     */
    void setDVars(Map<String, ?> dvarMap);
    
    /**
     * Sets the constraints for the current problem.
     * 
     * @param constraintMap map of constraint names to their definitions
     */
    void setConstraints(Map<String, ?> constraintMap);
    
    /**
     * Sets the objective function weights.
     * 
     * @param weightMap map of variable names to their weights
     */
    void setWeights(Map<String, ?> weightMap);
    
    /**
     * Solves the current problem.
     * 
     * @return SolverResult containing the solution status and values
     * @throws SolverException if solving fails
     */
    SolverResult solve() throws SolverException;
    
    /**
     * Solves with infeasibility analysis if the problem is infeasible.
     * 
     * @param outputTarget where to write infeasibility analysis output
     * @return SolverResult containing the solution status, values, and any infeasibility info
     * @throws SolverException if solving fails
     */
    SolverResult solveWithInfeasibilityAnalysis(String outputTarget) throws SolverException;
    
    /**
     * Gets the current model status.
     * 
     * @return status code (solver-specific, but 1 typically means optimal)
     */
    int getModelStatus();
    
    /**
     * Gets the objective function value of the current solution.
     * 
     * @return objective value, or NaN if no solution exists
     */
    double getObjectiveValue();
    
    /**
     * Gets the value of a specific decision variable.
     * 
     * @param varName name of the variable
     * @return variable value, or NaN if not found
     */
    double getVariableValue(String varName);
    
    /**
     * Gets all variable values as a map.
     * 
     * @return map of variable names to their values
     */
    Map<String, Double> getAllVariableValues();
    
    /**
     * Checks if the solver supports sensitivity analysis.
     * 
     * @return true if sensitivity methods are available
     */
    boolean supportsSensitivityAnalysis();
    
    /**
     * Gets the name of this solver.
     * 
     * @return solver name (e.g., "CBC", "Gurobi", "XA")
     */
    String getSolverName();
    
    /**
     * Releases resources and cleans up the solver.
     * Should be called when done with the solver.
     */
    void close();
    
    /**
     * Disposes of the solver environment.
     * Called at the end of a study run.
     */
    void dispose();
}
