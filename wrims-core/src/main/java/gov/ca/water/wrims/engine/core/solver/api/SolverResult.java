package gov.ca.water.wrims.engine.core.solver.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the result of a solver execution.
 * 
 * Contains the solution status, objective value, variable values,
 * and optionally sensitivity information.
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class SolverResult {
    
    /**
     * Solution status codes.
     */
    public enum Status {
        /** Optimal solution found */
        OPTIMAL(1),
        /** Feasible solution found (may not be optimal) */
        FEASIBLE(2),
        /** Problem is infeasible */
        INFEASIBLE(3),
        /** Problem is unbounded */
        UNBOUNDED(4),
        /** Solver encountered an error */
        ERROR(5),
        /** Solver timed out */
        TIMEOUT(6),
        /** Unknown status */
        UNKNOWN(0);
        
        private final int code;
        
        Status(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static Status fromCode(int code) {
            for (Status s : values()) {
                if (s.code == code) return s;
            }
            return UNKNOWN;
        }
    }
    
    private final Status status;
    private final int rawStatusCode;
    private final double objectiveValue;
    private final Map<String, Double> variableValues;
    private final Map<String, Double> slackValues;
    private final Map<String, Double> dualValues;
    private final Map<String, Double> reducedCosts;
    private final String message;
    private final long solveTimeMs;
    
    /**
     * Builder for SolverResult.
     */
    public static class Builder {
        private Status status = Status.UNKNOWN;
        private int rawStatusCode = 0;
        private double objectiveValue = Double.NaN;
        private Map<String, Double> variableValues = new HashMap<>();
        private Map<String, Double> slackValues = new HashMap<>();
        private Map<String, Double> dualValues = new HashMap<>();
        private Map<String, Double> reducedCosts = new HashMap<>();
        private String message = "";
        private long solveTimeMs = 0;
        
        public Builder status(Status status) {
            this.status = status;
            return this;
        }
        
        public Builder rawStatusCode(int code) {
            this.rawStatusCode = code;
            return this;
        }
        
        public Builder objectiveValue(double value) {
            this.objectiveValue = value;
            return this;
        }
        
        public Builder variableValues(Map<String, Double> values) {
            this.variableValues = new HashMap<>(values);
            return this;
        }
        
        public Builder slackValues(Map<String, Double> values) {
            this.slackValues = new HashMap<>(values);
            return this;
        }
        
        public Builder dualValues(Map<String, Double> values) {
            this.dualValues = new HashMap<>(values);
            return this;
        }
        
        public Builder reducedCosts(Map<String, Double> values) {
            this.reducedCosts = new HashMap<>(values);
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder solveTimeMs(long time) {
            this.solveTimeMs = time;
            return this;
        }
        
        public SolverResult build() {
            return new SolverResult(this);
        }
    }
    
    private SolverResult(Builder builder) {
        this.status = builder.status;
        this.rawStatusCode = builder.rawStatusCode;
        this.objectiveValue = builder.objectiveValue;
        this.variableValues = Collections.unmodifiableMap(builder.variableValues);
        this.slackValues = Collections.unmodifiableMap(builder.slackValues);
        this.dualValues = Collections.unmodifiableMap(builder.dualValues);
        this.reducedCosts = Collections.unmodifiableMap(builder.reducedCosts);
        this.message = builder.message;
        this.solveTimeMs = builder.solveTimeMs;
    }
    
    /**
     * Creates a simple optimal result.
     */
    public static SolverResult optimal(double objectiveValue, Map<String, Double> variableValues) {
        return new Builder()
                .status(Status.OPTIMAL)
                .objectiveValue(objectiveValue)
                .variableValues(variableValues)
                .build();
    }
    
    /**
     * Creates an infeasible result.
     */
    public static SolverResult infeasible(String message) {
        return new Builder()
                .status(Status.INFEASIBLE)
                .message(message)
                .build();
    }
    
    /**
     * Creates an error result.
     */
    public static SolverResult error(String message) {
        return new Builder()
                .status(Status.ERROR)
                .message(message)
                .build();
    }
    
    // Getters
    
    public Status getStatus() {
        return status;
    }
    
    public int getRawStatusCode() {
        return rawStatusCode;
    }
    
    public boolean isOptimal() {
        return status == Status.OPTIMAL;
    }
    
    public boolean isFeasible() {
        return status == Status.OPTIMAL || status == Status.FEASIBLE;
    }
    
    public boolean isInfeasible() {
        return status == Status.INFEASIBLE;
    }
    
    public double getObjectiveValue() {
        return objectiveValue;
    }
    
    public Map<String, Double> getVariableValues() {
        return variableValues;
    }
    
    public double getVariableValue(String varName) {
        return variableValues.getOrDefault(varName, Double.NaN);
    }
    
    public Map<String, Double> getSlackValues() {
        return slackValues;
    }
    
    public double getSlackValue(String constraintName) {
        return slackValues.getOrDefault(constraintName, Double.NaN);
    }
    
    public Map<String, Double> getDualValues() {
        return dualValues;
    }
    
    public double getDualValue(String constraintName) {
        return dualValues.getOrDefault(constraintName, Double.NaN);
    }
    
    public Map<String, Double> getReducedCosts() {
        return reducedCosts;
    }
    
    public double getReducedCost(String varName) {
        return reducedCosts.getOrDefault(varName, Double.NaN);
    }
    
    public String getMessage() {
        return message;
    }
    
    public long getSolveTimeMs() {
        return solveTimeMs;
    }
    
    public boolean hasSensitivityData() {
        return !slackValues.isEmpty() || !dualValues.isEmpty() || !reducedCosts.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("SolverResult[status=%s, objective=%.6f, vars=%d, time=%dms]",
                status, objectiveValue, variableValues.size(), solveTimeMs);
    }
}
