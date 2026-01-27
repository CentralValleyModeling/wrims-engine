package gov.ca.water.wrims.engine.core.solver.api.adapters;

import java.util.HashMap;
import java.util.Map;

import gov.ca.water.wrims.engine.core.solver.api.Solver;
import gov.ca.water.wrims.engine.core.solver.api.SolverSensitivity;
import gov.ca.water.wrims.engine.core.solver.api.SolverException;
import gov.ca.water.wrims.engine.core.solver.api.SolverResult;
import gov.ca.water.wrims.engine.core.solver.Gurobi.GurobiSolver;
import gov.ca.water.wrims.engine.core.components.ControlData;

import com.gurobi.gurobi.*;

/**
 * Adapter that wraps GurobiSolver to implement the unified Solver interface.
 * 
 * Gurobi provides full sensitivity analysis support via:
 * - GRB.DoubleAttr.Slack - constraint slack values
 * - GRB.DoubleAttr.Pi - dual values (shadow prices)
 * - GRB.DoubleAttr.RC - reduced costs
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class GurobiSolverAdapter implements SolverSensitivity {
    
    private static final String SOLVER_NAME = "Gurobi";
    
    private GRBEnv env;
    private GRBModel model;
    private int modelStatus = 0;
    private double objectiveValue = Double.NaN;
    private Map<String, Double> variableValues = new HashMap<>();
    private Map<String, GRBVar> varMap = new HashMap<>();
    private Map<String, GRBConstr> constrMap = new HashMap<>();
    private boolean initialized = false;
    
    @Override
    public void initialize() throws SolverException {
        try {
            env = new GRBEnv();
            env.set(GRB.IntParam.LogToConsole, 0);
            initialized = true;
        } catch (GRBException e) {
            throw new SolverException("Failed to initialize Gurobi environment", SOLVER_NAME, e.getErrorCode(), e);
        }
    }
    
    @Override
    public void loadNewModel() {
        variableValues.clear();
        varMap.clear();
        constrMap.clear();
        modelStatus = 0;
        objectiveValue = Double.NaN;
    }
    
    /**
     * Loads a model from an LP file.
     * 
     * @param lpFilePath path to the CPLEX LP format file
     * @throws SolverException if file cannot be loaded
     */
    public void loadFromLpFile(String lpFilePath) throws SolverException {
        try {
            model = new GRBModel(env, lpFilePath);
            
            // Build var and constraint maps for later lookup
            varMap.clear();
            for (GRBVar var : model.getVars()) {
                varMap.put(var.get(GRB.StringAttr.VarName), var);
            }
            
            constrMap.clear();
            for (GRBConstr constr : model.getConstrs()) {
                constrMap.put(constr.get(GRB.StringAttr.ConstrName), constr);
            }
        } catch (GRBException e) {
            throw new SolverException("Failed to load LP file: " + lpFilePath, SOLVER_NAME, e.getErrorCode(), e);
        }
    }
    
    @Override
    public void setDVars(Map<String, ?> dvarMap) {
        // For LP file-based solving, variables are defined in the file
        // This method would be used for direct API-based model building
    }
    
    @Override
    public void setConstraints(Map<String, ?> constraintMap) {
        // For LP file-based solving, constraints are defined in the file
        // This method would be used for direct API-based model building
    }
    
    @Override
    public void setWeights(Map<String, ?> weightMap) {
        // For LP file-based solving, weights are defined in the file
        // This method would be used for direct API-based model building
    }
    
    @Override
    public SolverResult solve() throws SolverException {
        long startTime = System.currentTimeMillis();
        
        try {
            model.optimize();
            
            int optimStatus = model.get(GRB.IntAttr.Status);
            modelStatus = optimStatus;
            
            SolverResult.Builder builder = new SolverResult.Builder()
                    .rawStatusCode(optimStatus);
            
            if (optimStatus == GRB.Status.OPTIMAL) {
                objectiveValue = model.get(GRB.DoubleAttr.ObjVal);
                collectVariableValues();
                collectSensitivityData(builder);
                
                builder.status(SolverResult.Status.OPTIMAL)
                       .objectiveValue(objectiveValue)
                       .variableValues(variableValues);
                       
            } else if (optimStatus == GRB.Status.INFEASIBLE) {
                builder.status(SolverResult.Status.INFEASIBLE)
                       .message("Model is infeasible");
                       
            } else if (optimStatus == GRB.Status.UNBOUNDED) {
                builder.status(SolverResult.Status.UNBOUNDED)
                       .message("Model is unbounded");
                       
            } else if (optimStatus == GRB.Status.INF_OR_UNBD) {
                // Try with presolve disabled
                model.getEnv().set(GRB.IntParam.Presolve, 0);
                model.optimize();
                optimStatus = model.get(GRB.IntAttr.Status);
                
                if (optimStatus == GRB.Status.INFEASIBLE) {
                    builder.status(SolverResult.Status.INFEASIBLE)
                           .message("Model is infeasible (determined after disabling presolve)");
                } else if (optimStatus == GRB.Status.UNBOUNDED) {
                    builder.status(SolverResult.Status.UNBOUNDED)
                           .message("Model is unbounded (determined after disabling presolve)");
                }
            } else {
                builder.status(SolverResult.Status.ERROR)
                       .message("Optimization stopped with status: " + optimStatus);
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            builder.solveTimeMs(elapsed);
            
            return builder.build();
            
        } catch (GRBException e) {
            throw new SolverException("Gurobi solve failed: " + e.getMessage(), SOLVER_NAME, e.getErrorCode(), e);
        }
    }
    
    @Override
    public SolverResult solveWithInfeasibilityAnalysis(String outputTarget) throws SolverException {
        SolverResult result = solve();
        
        if (result.isInfeasible()) {
            try {
                model.computeIIS();
                // Could write IIS to file if needed
                // model.write(outputTarget + "_infeasible.ilp");
            } catch (GRBException e) {
                // Log but don't fail - IIS computation is optional
            }
        }
        
        return result;
    }
    
    private void collectVariableValues() throws GRBException {
        variableValues.clear();
        GRBVar[] vars = model.getVars();
        String[] names = model.get(GRB.StringAttr.VarName, vars);
        double[] values = model.get(GRB.DoubleAttr.X, vars);
        
        for (int i = 0; i < names.length; i++) {
            variableValues.put(names[i], values[i]);
        }
    }
    
    private void collectSensitivityData(SolverResult.Builder builder) throws GRBException {
        // Collect slack values
        Map<String, Double> slacks = new HashMap<>();
        for (GRBConstr constr : model.getConstrs()) {
            String name = constr.get(GRB.StringAttr.ConstrName);
            double slack = constr.get(GRB.DoubleAttr.Slack);
            slacks.put(name, slack);
        }
        builder.slackValues(slacks);
        
        // Collect dual values (shadow prices)
        Map<String, Double> duals = new HashMap<>();
        for (GRBConstr constr : model.getConstrs()) {
            String name = constr.get(GRB.StringAttr.ConstrName);
            double pi = constr.get(GRB.DoubleAttr.Pi);
            duals.put(name, pi);
        }
        builder.dualValues(duals);
        
        // Collect reduced costs
        Map<String, Double> reducedCosts = new HashMap<>();
        for (GRBVar var : model.getVars()) {
            String name = var.get(GRB.StringAttr.VarName);
            double rc = var.get(GRB.DoubleAttr.RC);
            reducedCosts.put(name, rc);
        }
        builder.reducedCosts(reducedCosts);
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
        return true;
    }
    
    @Override
    public String getSolverName() {
        return SOLVER_NAME;
    }
    
    @Override
    public void close() {
        if (model != null) {
            try {
                model.dispose();
            } catch (GRBException e) {
                // Ignore dispose errors
            }
            model = null;
        }
    }
    
    @Override
    public void dispose() {
        close();
        if (env != null) {
            try {
                env.dispose();
            } catch (GRBException e) {
                // Ignore dispose errors
            }
            env = null;
        }
        initialized = false;
    }
    
    // SolverSensitivity methods
    
    @Override
    public double getSlack(String constraintName) {
        try {
            GRBConstr constr = constrMap.get(constraintName);
            if (constr != null) {
                return constr.get(GRB.DoubleAttr.Slack);
            }
        } catch (GRBException e) {
            // Return NaN on error
        }
        return Double.NaN;
    }
    
    @Override
    public double getSurplus(String constraintName) {
        // In Gurobi, slack is negative for >= constraints when binding
        // Surplus is essentially |slack| for >= constraints
        double slack = getSlack(constraintName);
        return slack < 0 ? -slack : 0.0;
    }
    
    @Override
    public double getDualValue(String constraintName) {
        try {
            GRBConstr constr = constrMap.get(constraintName);
            if (constr != null) {
                return constr.get(GRB.DoubleAttr.Pi);
            }
        } catch (GRBException e) {
            // Return NaN on error
        }
        return Double.NaN;
    }
    
    @Override
    public double getReducedCost(String varName) {
        try {
            GRBVar var = varMap.get(varName);
            if (var != null) {
                return var.get(GRB.DoubleAttr.RC);
            }
        } catch (GRBException e) {
            // Return NaN on error
        }
        return Double.NaN;
    }
    
    @Override
    public boolean isConstraintBinding(String constraintName, double tolerance) {
        double slack = getSlack(constraintName);
        return !Double.isNaN(slack) && Math.abs(slack) < tolerance;
    }
    
    @Override
    public boolean isAtLowerBound(String varName, double tolerance) {
        try {
            GRBVar var = varMap.get(varName);
            if (var != null) {
                double value = var.get(GRB.DoubleAttr.X);
                double lb = var.get(GRB.DoubleAttr.LB);
                return Math.abs(value - lb) < tolerance;
            }
        } catch (GRBException e) {
            // Return false on error
        }
        return false;
    }
    
    @Override
    public boolean isAtUpperBound(String varName, double tolerance) {
        try {
            GRBVar var = varMap.get(varName);
            if (var != null) {
                double value = var.get(GRB.DoubleAttr.X);
                double ub = var.get(GRB.DoubleAttr.UB);
                return Math.abs(value - ub) < tolerance;
            }
        } catch (GRBException e) {
            // Return false on error
        }
        return false;
    }
    
    @Override
    public Map<String, Double> getAllSlackValues() {
        Map<String, Double> slacks = new HashMap<>();
        try {
            for (GRBConstr constr : model.getConstrs()) {
                String name = constr.get(GRB.StringAttr.ConstrName);
                double slack = constr.get(GRB.DoubleAttr.Slack);
                slacks.put(name, slack);
            }
        } catch (GRBException e) {
            // Return partial results on error
        }
        return slacks;
    }
    
    @Override
    public Map<String, Double> getAllDualValues() {
        Map<String, Double> duals = new HashMap<>();
        try {
            for (GRBConstr constr : model.getConstrs()) {
                String name = constr.get(GRB.StringAttr.ConstrName);
                double pi = constr.get(GRB.DoubleAttr.Pi);
                duals.put(name, pi);
            }
        } catch (GRBException e) {
            // Return partial results on error
        }
        return duals;
    }
    
    @Override
    public Map<String, Double> getAllReducedCosts() {
        Map<String, Double> costs = new HashMap<>();
        try {
            for (GRBVar var : model.getVars()) {
                String name = var.get(GRB.StringAttr.VarName);
                double rc = var.get(GRB.DoubleAttr.RC);
                costs.put(name, rc);
            }
        } catch (GRBException e) {
            // Return partial results on error
        }
        return costs;
    }
}
