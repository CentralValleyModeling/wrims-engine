package gov.ca.water.wrims.engine.core.solver.api;

/**
 * Extended interface for solvers that support sensitivity analysis.
 * 
 * This interface provides methods to retrieve slack, surplus, dual values,
 * and reduced costs - essential for identifying controlling constraints/goals.
 * 
 * Not all solvers support these operations:
 * - Gurobi: Full support via GRB.DoubleAttr.Slack, Pi, RC
 * - CBC: Limited support (may require JNI extension)
 * - XA: Support via XA API (needs verification)
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public interface SolverSensitivity extends Solver {
    
    /**
     * Gets the slack value for a constraint.
     * Slack = RHS - LHS for <= constraints (positive when constraint is not tight)
     * 
     * @param constraintName name of the constraint
     * @return slack value, or NaN if not available
     */
    double getSlack(String constraintName);
    
    /**
     * Gets the surplus value for a constraint.
     * Surplus = LHS - RHS for >= constraints (positive when constraint is not tight)
     * 
     * @param constraintName name of the constraint
     * @return surplus value, or NaN if not available
     */
    double getSurplus(String constraintName);
    
    /**
     * Gets the dual value (shadow price) for a constraint.
     * The dual value indicates how much the objective would improve
     * per unit relaxation of the constraint.
     * 
     * @param constraintName name of the constraint
     * @return dual value, or NaN if not available
     */
    double getDualValue(String constraintName);
    
    /**
     * Gets the reduced cost for a variable.
     * The reduced cost indicates how much the objective coefficient
     * would need to improve before the variable enters the basis.
     * 
     * @param varName name of the variable
     * @return reduced cost, or NaN if not available
     */
    double getReducedCost(String varName);
    
    /**
     * Checks if a constraint is binding (active/tight).
     * A constraint is binding if its slack/surplus is zero (or within tolerance).
     * 
     * @param constraintName name of the constraint
     * @param tolerance tolerance for considering a constraint binding
     * @return true if the constraint is binding
     */
    boolean isConstraintBinding(String constraintName, double tolerance);
    
    /**
     * Checks if a variable is at its lower bound.
     * 
     * @param varName name of the variable
     * @param tolerance tolerance for the comparison
     * @return true if variable is at lower bound
     */
    boolean isAtLowerBound(String varName, double tolerance);
    
    /**
     * Checks if a variable is at its upper bound.
     * 
     * @param varName name of the variable
     * @param tolerance tolerance for the comparison
     * @return true if variable is at upper bound
     */
    boolean isAtUpperBound(String varName, double tolerance);
    
    /**
     * Gets all slack values as a map.
     * 
     * @return map of constraint names to slack values
     */
    java.util.Map<String, Double> getAllSlackValues();
    
    /**
     * Gets all dual values as a map.
     * 
     * @return map of constraint names to dual values
     */
    java.util.Map<String, Double> getAllDualValues();
    
    /**
     * Gets all reduced costs as a map.
     * 
     * @return map of variable names to reduced costs
     */
    java.util.Map<String, Double> getAllReducedCosts();
}
