package gov.ca.water.wrims.engine.core.solver.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes solver results to identify controlling goals/constraints.
 * 
 * This class works with any solver that implements the SolverSensitivity interface
 * to determine which goals are controlling specific decision variables.
 * 
 * A goal is "controlling" a variable when:
 * 1. The constraint associated with the goal is binding (slack = 0)
 * 2. The variable appears in that constraint with a non-zero coefficient
 * 3. The dual value is non-zero (indicates shadow price)
 * 
 * Usage:
 * <pre>
 * SolverSensitivity solver = ...;
 * GoalAnalyzer analyzer = new GoalAnalyzer(solver, tolerance);
 * List&lt;ControllingInfo&gt; controlling = analyzer.findControllingGoals(variableNames);
 * </pre>
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class GoalAnalyzer {
    
    private static final double DEFAULT_TOLERANCE = 1e-6;
    
    private final SolverSensitivity solver;
    private final double tolerance;
    private final Map<String, List<String>> variableToConstraintsMap;
    
    /**
     * Creates a GoalAnalyzer with default tolerance.
     * 
     * @param solver solver that supports sensitivity analysis
     */
    public GoalAnalyzer(SolverSensitivity solver) {
        this(solver, DEFAULT_TOLERANCE);
    }
    
    /**
     * Creates a GoalAnalyzer with specified tolerance.
     * 
     * @param solver solver that supports sensitivity analysis
     * @param tolerance tolerance for considering slack as zero
     */
    public GoalAnalyzer(SolverSensitivity solver, double tolerance) {
        this.solver = solver;
        this.tolerance = tolerance;
        this.variableToConstraintsMap = new HashMap<>();
    }
    
    /**
     * Registers which constraints involve a given variable.
     * This mapping is needed to determine which constraints could be controlling.
     * 
     * @param variableName name of the variable
     * @param constraintNames list of constraint names that involve this variable
     */
    public void registerVariableConstraints(String variableName, List<String> constraintNames) {
        variableToConstraintsMap.put(variableName, new ArrayList<>(constraintNames));
    }
    
    /**
     * Finds the controlling goal for a single variable.
     * 
     * @param variableName name of the variable to analyze
     * @return ControllingInfo describing what controls this variable
     */
    public ControllingInfo findControllingGoal(String variableName) {
        double value = solver.getVariableValue(variableName);
        
        // Check if at bounds first
        if (solver.isAtLowerBound(variableName, tolerance)) {
            return ControllingInfo.atLowerBound(variableName, value, value);
        }
        if (solver.isAtUpperBound(variableName, tolerance)) {
            return ControllingInfo.atUpperBound(variableName, value, value);
        }
        
        // Check constraints that involve this variable
        List<String> constraints = variableToConstraintsMap.get(variableName);
        if (constraints != null) {
            for (String constraintName : constraints) {
                if (solver.isConstraintBinding(constraintName, tolerance)) {
                    double slack = solver.getSlack(constraintName);
                    double dual = solver.getDualValue(constraintName);
                    return ControllingInfo.controlledByGoal(variableName, value, 
                            constraintName, slack, dual);
                }
            }
        }
        
        // No binding constraint found - check all constraints via slack
        Map<String, Double> allSlacks = solver.getAllSlackValues();
        for (Map.Entry<String, Double> entry : allSlacks.entrySet()) {
            if (Math.abs(entry.getValue()) < tolerance) {
                // This constraint is binding - could be controlling
                double dual = solver.getDualValue(entry.getKey());
                if (Math.abs(dual) > tolerance) {
                    return ControllingInfo.controlledByGoal(variableName, value,
                            entry.getKey(), entry.getValue(), dual);
                }
            }
        }
        
        // Variable is free
        return ControllingInfo.free(variableName, value);
    }
    
    /**
     * Finds controlling goals for multiple variables.
     * 
     * @param variableNames list of variable names to analyze
     * @return list of ControllingInfo for each variable
     */
    public List<ControllingInfo> findControllingGoals(List<String> variableNames) {
        List<ControllingInfo> results = new ArrayList<>();
        for (String varName : variableNames) {
            results.add(findControllingGoal(varName));
        }
        return results;
    }
    
    /**
     * Finds all binding constraints in the current solution.
     * 
     * @return map of constraint names to their slack values (only binding ones)
     */
    public Map<String, Double> findBindingConstraints() {
        Map<String, Double> binding = new HashMap<>();
        Map<String, Double> allSlacks = solver.getAllSlackValues();
        
        for (Map.Entry<String, Double> entry : allSlacks.entrySet()) {
            if (Math.abs(entry.getValue()) < tolerance) {
                binding.put(entry.getKey(), entry.getValue());
            }
        }
        return binding;
    }
    
    /**
     * Finds constraints with non-zero dual values (shadow prices).
     * These are the constraints that would affect the objective if relaxed.
     * 
     * @return map of constraint names to their dual values
     */
    public Map<String, Double> findConstraintsWithShadowPrices() {
        Map<String, Double> withDuals = new HashMap<>();
        Map<String, Double> allDuals = solver.getAllDualValues();
        
        for (Map.Entry<String, Double> entry : allDuals.entrySet()) {
            if (Math.abs(entry.getValue()) > tolerance) {
                withDuals.put(entry.getKey(), entry.getValue());
            }
        }
        return withDuals;
    }
    
    /**
     * Generates a summary report of controlling goals.
     * 
     * @param variableNames variables to analyze
     * @return formatted report string
     */
    public String generateReport(List<String> variableNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Controlling Goals Report ===\n\n");
        
        List<ControllingInfo> results = findControllingGoals(variableNames);
        
        // Count by control type
        int goalControlled = 0;
        int boundControlled = 0;
        int freeVars = 0;
        
        for (ControllingInfo info : results) {
            switch (info.getControlType()) {
                case GOAL:
                    goalControlled++;
                    break;
                case UPPER_BOUND:
                case LOWER_BOUND:
                    boundControlled++;
                    break;
                case FREE:
                    freeVars++;
                    break;
                default:
                    break;
            }
        }
        
        sb.append(String.format("Total variables analyzed: %d\n", results.size()));
        sb.append(String.format("  Controlled by goals: %d\n", goalControlled));
        sb.append(String.format("  At bounds: %d\n", boundControlled));
        sb.append(String.format("  Free: %d\n\n", freeVars));
        
        sb.append("--- Details ---\n");
        for (ControllingInfo info : results) {
            sb.append(info.toOutputString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the tolerance used for comparisons.
     * 
     * @return tolerance value
     */
    public double getTolerance() {
        return tolerance;
    }
}
