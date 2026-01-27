package gov.ca.water.wrims.engine.core.solver.api;

/**
 * Information about a controlling constraint or bound.
 * 
 * A controlling constraint is one that is "binding" or "active" - 
 * meaning it directly limits the value of a decision variable.
 * 
 * This class is used by the GoalAnalyzer to report which goals/constraints
 * are controlling specific decision variables.
 * 
 * @author WRIMS Team
 * @since 2.0
 */
public class ControllingInfo {
    
    /**
     * Type of controlling element.
     */
    public enum ControlType {
        /** Controlled by a goal/constraint */
        GOAL,
        /** Controlled by upper bound */
        UPPER_BOUND,
        /** Controlled by lower bound */
        LOWER_BOUND,
        /** Variable is free (not at a binding constraint) */
        FREE,
        /** Unknown or could not be determined */
        UNKNOWN
    }
    
    private final String variableName;
    private final double variableValue;
    private final ControlType controlType;
    private final String controllingElementName;
    private final double slack;
    private final double dualValue;
    private final String description;
    
    /**
     * Builder for ControllingInfo.
     */
    public static class Builder {
        private String variableName;
        private double variableValue = Double.NaN;
        private ControlType controlType = ControlType.UNKNOWN;
        private String controllingElementName = "";
        private double slack = Double.NaN;
        private double dualValue = Double.NaN;
        private String description = "";
        
        public Builder variableName(String name) {
            this.variableName = name;
            return this;
        }
        
        public Builder variableValue(double value) {
            this.variableValue = value;
            return this;
        }
        
        public Builder controlType(ControlType type) {
            this.controlType = type;
            return this;
        }
        
        public Builder controllingElementName(String name) {
            this.controllingElementName = name;
            return this;
        }
        
        public Builder slack(double slack) {
            this.slack = slack;
            return this;
        }
        
        public Builder dualValue(double dualValue) {
            this.dualValue = dualValue;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public ControllingInfo build() {
            return new ControllingInfo(this);
        }
    }
    
    private ControllingInfo(Builder builder) {
        this.variableName = builder.variableName;
        this.variableValue = builder.variableValue;
        this.controlType = builder.controlType;
        this.controllingElementName = builder.controllingElementName;
        this.slack = builder.slack;
        this.dualValue = builder.dualValue;
        this.description = builder.description;
    }
    
    /**
     * Creates a ControllingInfo for a variable controlled by a goal.
     */
    public static ControllingInfo controlledByGoal(String varName, double value, 
            String goalName, double slack, double dualValue) {
        return new Builder()
                .variableName(varName)
                .variableValue(value)
                .controlType(ControlType.GOAL)
                .controllingElementName(goalName)
                .slack(slack)
                .dualValue(dualValue)
                .description("Controlled by goal: " + goalName)
                .build();
    }
    
    /**
     * Creates a ControllingInfo for a variable at its upper bound.
     */
    public static ControllingInfo atUpperBound(String varName, double value, double bound) {
        return new Builder()
                .variableName(varName)
                .variableValue(value)
                .controlType(ControlType.UPPER_BOUND)
                .controllingElementName("upper_bound")
                .description(String.format("At upper bound: %.6f", bound))
                .build();
    }
    
    /**
     * Creates a ControllingInfo for a variable at its lower bound.
     */
    public static ControllingInfo atLowerBound(String varName, double value, double bound) {
        return new Builder()
                .variableName(varName)
                .variableValue(value)
                .controlType(ControlType.LOWER_BOUND)
                .controllingElementName("lower_bound")
                .description(String.format("At lower bound: %.6f", bound))
                .build();
    }
    
    /**
     * Creates a ControllingInfo for a free variable.
     */
    public static ControllingInfo free(String varName, double value) {
        return new Builder()
                .variableName(varName)
                .variableValue(value)
                .controlType(ControlType.FREE)
                .description("Variable is free (not at a binding constraint)")
                .build();
    }
    
    // Getters
    
    public String getVariableName() {
        return variableName;
    }
    
    public double getVariableValue() {
        return variableValue;
    }
    
    public ControlType getControlType() {
        return controlType;
    }
    
    public String getControllingElementName() {
        return controllingElementName;
    }
    
    public double getSlack() {
        return slack;
    }
    
    public double getDualValue() {
        return dualValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isControlledByGoal() {
        return controlType == ControlType.GOAL;
    }
    
    public boolean isAtBound() {
        return controlType == ControlType.UPPER_BOUND || controlType == ControlType.LOWER_BOUND;
    }
    
    public boolean isFree() {
        return controlType == ControlType.FREE;
    }
    
    @Override
    public String toString() {
        return String.format("ControllingInfo[var=%s, value=%.6f, type=%s, controlling=%s]",
                variableName, variableValue, controlType, controllingElementName);
    }
    
    /**
     * Returns a formatted string suitable for output files.
     */
    public String toOutputString() {
        StringBuilder sb = new StringBuilder();
        sb.append(variableName).append(",");
        sb.append(String.format("%.6f", variableValue)).append(",");
        sb.append(controlType).append(",");
        sb.append(controllingElementName);
        if (!Double.isNaN(slack)) {
            sb.append(",slack=").append(String.format("%.6f", slack));
        }
        if (!Double.isNaN(dualValue)) {
            sb.append(",dual=").append(String.format("%.6f", dualValue));
        }
        return sb.toString();
    }
}
