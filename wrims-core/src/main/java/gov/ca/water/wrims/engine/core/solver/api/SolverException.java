package gov.ca.water.wrims.engine.core.solver.api;

/**
 * Exception thrown by solver operations.
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class SolverException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    private final int errorCode;
    private final String solverName;
    
    /**
     * Creates a new SolverException with a message.
     * 
     * @param message error message
     */
    public SolverException(String message) {
        super(message);
        this.errorCode = -1;
        this.solverName = "Unknown";
    }
    
    /**
     * Creates a new SolverException with a message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public SolverException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = -1;
        this.solverName = "Unknown";
    }
    
    /**
     * Creates a new SolverException with full details.
     * 
     * @param message error message
     * @param solverName name of the solver that threw the exception
     * @param errorCode solver-specific error code
     */
    public SolverException(String message, String solverName, int errorCode) {
        super(message);
        this.solverName = solverName;
        this.errorCode = errorCode;
    }
    
    /**
     * Creates a new SolverException with full details and cause.
     * 
     * @param message error message
     * @param solverName name of the solver that threw the exception
     * @param errorCode solver-specific error code
     * @param cause underlying cause
     */
    public SolverException(String message, String solverName, int errorCode, Throwable cause) {
        super(message, cause);
        this.solverName = solverName;
        this.errorCode = errorCode;
    }
    
    /**
     * Gets the solver-specific error code.
     * 
     * @return error code, or -1 if not available
     */
    public int getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the name of the solver that threw the exception.
     * 
     * @return solver name
     */
    public String getSolverName() {
        return solverName;
    }
    
    @Override
    public String toString() {
        return String.format("SolverException[solver=%s, code=%d]: %s", 
                solverName, errorCode, getMessage());
    }
}
