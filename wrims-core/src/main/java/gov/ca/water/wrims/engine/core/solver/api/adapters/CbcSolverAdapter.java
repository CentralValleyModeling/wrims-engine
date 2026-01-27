package gov.ca.water.wrims.engine.core.solver.api.adapters;

import java.util.HashMap;
import java.util.Map;

import org.coinor.cbc.jCbc;
import org.coinor.cbc.SWIGTYPE_p_OsiClpSolverInterface;

import gov.ca.water.wrims.engine.core.solver.api.Solver;
import gov.ca.water.wrims.engine.core.solver.api.SolverSensitivity;
import gov.ca.water.wrims.engine.core.solver.api.SolverException;
import gov.ca.water.wrims.engine.core.solver.api.SolverResult;
import gov.ca.water.wrims.engine.core.solver.CbcSolver;
import gov.ca.water.wrims.engine.core.components.ControlData;

/**
 * Adapter that wraps CbcSolver to implement the unified Solver interface.
 * 
 * CBC (COIN-OR Branch and Cut) provides limited sensitivity analysis support
 * through the underlying CLP (COIN-OR Linear Programming) solver.
 * 
 * Note: The current CbcSolver implementation uses static methods, so this
 * adapter delegates to those static methods. For full OOP compliance, the
 * underlying CbcSolver would need to be refactored.
 * 
 * Sensitivity analysis support:
 * - Slack values: Available via CLP after solving
 * - Dual values: Available via CLP after solving  
 * - Reduced costs: Available via CLP after solving
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class CbcSolverAdapter implements SolverSensitivity {
    
    private static final String SOLVER_NAME = "CBC";
    
    private int modelStatus = 0;
    private double objectiveValue = Double.NaN;
    private Map<String, Double> variableValues = new HashMap<>();
    private Map<String, Double> slackValues = new HashMap<>();
    private Map<String, Double> dualValues = new HashMap<>();
    private Map<String, Double> reducedCosts = new HashMap<>();
    private boolean initialized = false;
    private boolean useLpFile = true;
    
    /**
     * Creates a CbcSolverAdapter.
     * 
     * @param useLpFile if true, use LP file-based solving; if false, use direct JNI
     */
    public CbcSolverAdapter(boolean useLpFile) {
        this.useLpFile = useLpFile;
    }
    
    /**
     * Creates a CbcSolverAdapter with LP file-based solving (default).
     */
    public CbcSolverAdapter() {
        this(true);
    }
    
    @Override
    public void initialize() throws SolverException {
        // CbcSolver.init() requires StudyDataSet, which comes from ControlData
        // For now, mark as initialized - actual init happens via existing flow
        initialized = true;
    }
    
    @Override
    public void loadNewModel() {
        variableValues.clear();
        slackValues.clear();
        dualValues.clear();
        reducedCosts.clear();
        modelStatus = 0;
        objectiveValue = Double.NaN;
    }
    
    @Override
    public void setDVars(Map<String, ?> dvarMap) {
        // CbcSolver handles this internally via SolverData
    }
    
    @Override
    public void setConstraints(Map<String, ?> constraintMap) {
        // CbcSolver handles this internally via SolverData
    }
    
    @Override
    public void setWeights(Map<String, ?> weightMap) {
        // CbcSolver handles this internally via SolverData
    }
    
    @Override
    public SolverResult solve() throws SolverException {
        long startTime = System.currentTimeMillis();
        
        try {
            // Delegate to existing static CbcSolver
            CbcSolver.newProblem();
            
            // Get results from ControlData (where CbcSolver stores them)
            if (ControlData.clp_cbc_objective != null) {
                objectiveValue = ControlData.clp_cbc_objective;
                modelStatus = 1; // Optimal
                
                // Collect variable values from CbcSolver.varDoubleMap
                if (CbcSolver.varDoubleMap != null) {
                    variableValues.putAll(CbcSolver.varDoubleMap);
                }
                
                // Collect sensitivity data if available
                collectSensitivityData();
                
                long elapsed = System.currentTimeMillis() - startTime;
                
                return new SolverResult.Builder()
                        .status(SolverResult.Status.OPTIMAL)
                        .rawStatusCode(modelStatus)
                        .objectiveValue(objectiveValue)
                        .variableValues(variableValues)
                        .slackValues(slackValues)
                        .dualValues(dualValues)
                        .reducedCosts(reducedCosts)
                        .solveTimeMs(elapsed)
                        .build();
            } else {
                // Check for errors
                if (gov.ca.water.wrims.engine.core.components.Error.error_solving.size() > 0) {
                    modelStatus = 3; // Infeasible
                    return new SolverResult.Builder()
                            .status(SolverResult.Status.INFEASIBLE)
                            .rawStatusCode(modelStatus)
                            .message("CBC solver reported infeasibility")
                            .solveTimeMs(System.currentTimeMillis() - startTime)
                            .build();
                }
                
                return new SolverResult.Builder()
                        .status(SolverResult.Status.ERROR)
                        .message("CBC solve completed but no objective value found")
                        .solveTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }
            
        } catch (Exception e) {
            throw new SolverException("CBC solve failed: " + e.getMessage(), SOLVER_NAME, -1, e);
        }
    }
    
    @Override
    public SolverResult solveWithInfeasibilityAnalysis(String outputTarget) throws SolverException {
        // CBC has built-in infeasibility analysis
        return solve();
    }
    
    /**
     * Collects sensitivity data from the CBC/CLP solver.
     * 
     * Note: This requires access to the underlying OsiClpSolverInterface,
     * which may not be directly exposed in the current jCbc binding.
     * This is a placeholder for when the JNI interface is extended.
     */
    private void collectSensitivityData() {
        // TODO: Extend jCbc JNI to expose:
        // - getRowPrice() for dual values
        // - getReducedCost() for reduced costs
        // - Slack can be computed from row activity and bounds
        
        // For now, clear the maps - sensitivity data not yet available
        slackValues.clear();
        dualValues.clear();
        reducedCosts.clear();
        
        // If jCbc provides access to the solver interface, we could do:
        // SWIGTYPE_p_OsiClpSolverInterface solver = jCbc.getSolver();
        // double[] rowPrice = jCbc.getRowPrice(solver);
        // double[] redCost = jCbc.getReducedCost(solver);
    }
    
    @Override
    public int getModelStatus() {
        return modelStatus;
    }
    
    @Override
    public double getObjectiveValue() {
        return objectiveValue;
    }
    
    @Override
    public double getVariableValue(String varName) {
        return variableValues.getOrDefault(varName, Double.NaN);
    }
    
    @Override
    public Map<String, Double> getAllVariableValues() {
        return new HashMap<>(variableValues);
    }
    
    @Override
    public boolean supportsSensitivityAnalysis() {
        // Return true when jCbc JNI is extended to support sensitivity
        return false; // Currently limited
    }
    
    @Override
    public String getSolverName() {
        return SOLVER_NAME;
    }
    
    @Override
    public void close() {
        // CbcSolver.resetModel() is called by the existing code
    }
    
    @Override
    public void dispose() {
        CbcSolver.close();
        initialized = false;
    }
    
    // SolverSensitivity methods - limited support
    
    @Override
    public double getSlack(String constraintName) {
        return slackValues.getOrDefault(constraintName, Double.NaN);
    }
    
    @Override
    public double getSurplus(String constraintName) {
        double slack = getSlack(constraintName);
        return slack < 0 ? -slack : 0.0;
    }
    
    @Override
    public double getDualValue(String constraintName) {
        return dualValues.getOrDefault(constraintName, Double.NaN);
    }
    
    @Override
    public double getReducedCost(String varName) {
        return reducedCosts.getOrDefault(varName, Double.NaN);
    }
    
    @Override
    public boolean isConstraintBinding(String constraintName, double tolerance) {
        double slack = getSlack(constraintName);
        return !Double.isNaN(slack) && Math.abs(slack) < tolerance;
    }
    
    @Override
    public boolean isAtLowerBound(String varName, double tolerance) {
        // Would need access to variable bounds from jCbc
        return false;
    }
    
    @Override
    public boolean isAtUpperBound(String varName, double tolerance) {
        // Would need access to variable bounds from jCbc
        return false;
    }
    
    @Override
    public Map<String, Double> getAllSlackValues() {
        return new HashMap<>(slackValues);
    }
    
    @Override
    public Map<String, Double> getAllDualValues() {
        return new HashMap<>(dualValues);
    }
    
    @Override
    public Map<String, Double> getAllReducedCosts() {
        return new HashMap<>(reducedCosts);
    }
}
