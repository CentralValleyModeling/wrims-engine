package gov.ca.water.wrims.engine.core.solver.api;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.solver.api.adapters.CbcSolverAdapter;
import gov.ca.water.wrims.engine.core.solver.api.adapters.GurobiSolverAdapter;
import gov.ca.water.wrims.engine.core.solver.api.adapters.XASolverAdapter;

/**
 * Factory for creating solver instances.
 * 
 * This factory centralizes solver creation and allows the model coordinator
 * to work with solvers without knowing their implementation details.
 * 
 * Usage:
 * <pre>
 * Solver solver = SolverFactory.createSolver("CBC");
 * solver.initialize();
 * // ... use solver
 * solver.dispose();
 * </pre>
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class SolverFactory {
    
    /**
     * Supported solver types.
     */
    public enum SolverType {
        XA("XA", Param.SOLVER_XA),
        CBC("CBC", Param.SOLVER_CBC),
        CBC0("CBC0", Param.SOLVER_CBC0),
        CBC1("CBC1", Param.SOLVER_CBC1),
        CLP("CLP", Param.SOLVER_CLP),
        CLP0("CLP0", Param.SOLVER_CLP0),
        CLP1("CLP1", Param.SOLVER_CLP1),
        GUROBI("Gurobi", Param.SOLVER_GUROBI),
        LPSOLVE("LPSolve", Param.SOLVER_LPSOLVE);
        
        private final String name;
        private final Integer paramValue;
        
        SolverType(String name, Integer paramValue) {
            this.name = name;
            this.paramValue = paramValue;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getParamValue() {
            return paramValue;
        }
        
        public static SolverType fromName(String name) {
            for (SolverType type : values()) {
                if (type.name.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            // Handle XA variants (xa, xalog, etc.)
            if (name.toLowerCase().contains("xa")) {
                return XA;
            }
            throw new IllegalArgumentException("Unknown solver type: " + name);
        }
        
        public static SolverType fromParamValue(Integer value) {
            for (SolverType type : values()) {
                if (type.paramValue.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown solver param value: " + value);
        }
    }
    
    /**
     * Creates a solver instance based on solver name.
     * 
     * Note: The actual solver adapter classes need to be implemented.
     * This method currently throws UnsupportedOperationException
     * until adapters are created.
     * 
     * @param solverName name of the solver (e.g., "CBC", "Gurobi", "XA")
     * @return Solver instance
     * @throws SolverException if solver cannot be created
     */
    public static Solver createSolver(String solverName) throws SolverException {
        SolverType type = SolverType.fromName(solverName);
        return createSolver(type);
    }
    
    /**
     * Creates a solver instance based on solver type.
     * 
     * @param type solver type
     * @return Solver instance
     * @throws SolverException if solver cannot be created
     */
    public static Solver createSolver(SolverType type) throws SolverException {
        switch (type) {
            case CBC:
                return new CbcSolverAdapter(false); // JNI mode
                
            case CBC0:
            case CBC1:
                return new CbcSolverAdapter(true); // LP file mode
                
            case CLP:
            case CLP0:
            case CLP1:
                // CLP uses similar adapter pattern - for now use CBC adapter
                // TODO: Create dedicated ClpSolverAdapter if needed
                return new CbcSolverAdapter(true);
                
            case GUROBI:
                return new GurobiSolverAdapter();
                
            case XA:
                return new XASolverAdapter();
                
            case LPSOLVE:
                // TODO: Create LPSolveSolverAdapter
                throw new SolverException("LPSolveSolverAdapter not yet implemented", "LPSolve", 0);
                
            default:
                throw new SolverException("Unknown solver type: " + type, "Unknown", 0);
        }
    }
    
    /**
     * Checks if a solver type supports sensitivity analysis.
     * 
     * @param type solver type
     * @return true if sensitivity analysis is supported
     */
    public static boolean supportsSensitivityAnalysis(SolverType type) {
        switch (type) {
            case GUROBI:
                return true; // Full support via Gurobi API
            case CBC:
            case CBC1:
                return true; // Limited support via jCbc
            case XA:
                return true; // Support via XA API (needs verification)
            case CLP:
            case CLP1:
                return true; // Support via CLP API
            default:
                return false;
        }
    }
    
    /**
     * Gets all available solver types.
     * 
     * @return array of solver types
     */
    public static SolverType[] getAvailableSolverTypes() {
        return SolverType.values();
    }
    
    /**
     * Checks if a solver is available (libraries loaded, etc.)
     * 
     * @param type solver type to check
     * @return true if solver is available
     */
    public static boolean isSolverAvailable(SolverType type) {
        // TODO: Implement actual availability checks
        // This would check if native libraries are loaded, etc.
        switch (type) {
            case XA:
                // Check if XA license is available
                return true; // Assume available for now
            case CBC:
            case CBC0:
            case CBC1:
                // Check if jCbc native library is loaded
                return true;
            case GUROBI:
                // Check if Gurobi license is available
                try {
                    Class.forName("com.gurobi.gurobi.GRBEnv");
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            default:
                return true;
        }
    }
}
