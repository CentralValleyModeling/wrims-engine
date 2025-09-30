package wrimsv2.scripts;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.PreEvaluator;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.GlobalData;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.Workflow;
import wrimsv2.wreslplus.elements.procedures.ToWreslData;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

final class NetworkReader {
    private static final Logger logger = Logger.getLogger(NetworkReader.class.getName());

    public record CommandLineOptions(String mainFile, String dst) {
        private static final Logger logger = Logger.getLogger(CommandLineOptions.class.getName());
        /**
         * This nested class parses and holds the data for the CLI invocation of NetworkReader
         *
         * @param args An array of strings from the command line
         */
        public static CommandLineOptions parseCLI(String[] args) {
            Options options = new Options();

            options.addOption("m", "main-file", true, "The main file to be parsed");
            options.addOption("d", "dst", true, "The output file to be created");
            options.addOption("h", "help", false, "Print this help message and exit");

            CommandLine cmd = null;
            try {
                CommandLineParser parser = new DefaultParser();
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                logger.warning(e.getMessage());
                System.exit(1);
            }

            boolean helpCalled = false;
            String mainFile = null;
            String dst = null;
            if (cmd != null) {
                if (cmd.hasOption("m")) {
                    String pathString = cmd.getOptionValue("m");
                    mainFile = Paths.get(pathString).toAbsolutePath().toString();
                    dst = Paths.get(cmd.getOptionValue("d")).toAbsolutePath().toString();
                }
                if (cmd.hasOption("h")) {
                    helpCalled = true;
                }
            }
            if ((cmd == null) || (cmd.getOptions().length == 0) || helpCalled) {
                HelpFormatter formatter = new HelpFormatter();
                String header = "WRESL+ Network Reader";
                String footer = "Please report issues at https://github.com/CentralValleyModeling/wrims-engine";
                formatter.printHelp("network-reader", header, options, footer);
                System.exit(0);
            }
            return new CommandLineOptions(mainFile, dst);
        }

    }

    /**
     * Parse the main WRESL+ file for a model, and pull out all the continuity goals
     *
     * @param topFileWRESL The "main" WRESL+ file that defines the model
     * @return A Set of Goal objects that represent the continuity goals in the model
     */
    public static Map<String, Goal> findContinuityGoals(String topFileWRESL) {
        StudyTemp studyTemplate = Workflow.checkStudy(topFileWRESL);
        StudyDataSet study = ToWreslData.convertStudy(studyTemplate);
        assert study != null;
        new PreEvaluator(study);
        HashMap<String, Goal> goals = new HashMap<>();
        Map<String, ModelDataSet> modelDataSetMap = study.getModelDataSetMap();
        for (Map.Entry<String, ModelDataSet> entry: modelDataSetMap.entrySet()) {
            String modelName = entry.getKey();
            logger.log(Level.INFO, "Finding goals in model: {}", modelName);
            ModelDataSet model = entry.getValue();
            for (String goalName: model.gMap.keySet()) {
                if (!goalName.startsWith("continuity")) {continue;}
                Goal goal = model.gMap.get(goalName);
                goals.put(goalName, goal);
            }
        }
        return goals;
    }

    /**
     * An Enumeration to hold the allowable flow types; INFLOW, OUTFLOW, and STORAGE. It also includes UNKNOWN, which is
     * the default value used when parsing. If a direction cannot be determined, it will remain as UNKNOWN.
     */
    public enum FlowDirection {
        INFLOW,
        OUTFLOW,
        STORAGE,
        UNKNOWN
    }

    /**
     * Assign a FlowDirection based on the mathematical sign that precedes the variable, and whether it appears before
     * the equals sign in the continuity equation.
     *
     * @param sign The character of the math operator
     * @param onLeftHandSide boolean of whether the operation is on the left
     * @return a value of FlowDirection
     */
    public static FlowDirection classifyFlowDirection(char sign, boolean onLeftHandSide) {
        FlowDirection flowDirection = FlowDirection.UNKNOWN;
        if (onLeftHandSide && (sign == '+')) {
            flowDirection = FlowDirection.INFLOW;
        } else if (onLeftHandSide && (sign == '-')) {
            flowDirection = FlowDirection.OUTFLOW;
        } else if (!onLeftHandSide && (sign == '+')) {
            flowDirection = FlowDirection.OUTFLOW;
        } else if (!onLeftHandSide && (sign == '-')) {
            flowDirection = FlowDirection.INFLOW;
        } else if (!onLeftHandSide) {
            flowDirection = FlowDirection.OUTFLOW;  // assume outflow for other right hand side variables
        }
        return flowDirection;
    }

    /**
     * Determine the flow direction for a varible from an expression it's used in.
     * @param variable The variable to focus on
     * @param goalExpression The math expression to resolve
     * @return The FlowDirection determined for the variable
     */
    private static FlowDirection getVariableFlowDirectionFromExpression(String variable, String goalExpression)
            throws ArithmeticException {
        int idx = goalExpression.indexOf(variable);
        String[] possibleEq = {"=", ">", "<"};
        int idxEq = -1;  // -1 representing null here
        for (String eqStr : possibleEq){
            if (goalExpression.contains(eqStr)) {
                idxEq = goalExpression.indexOf(eqStr);
                break;
            }
        }
        if (idxEq == -1) {
            throw new ArithmeticException(String.format("I don't know how to parse this expression: '%s'", goalExpression));
        }
        boolean onLeft = idx < idxEq;
        FlowDirection flowDirection;
        int numberOfVariableUses = goalExpression.split(variable, -1).length - 1;
        if (numberOfVariableUses != 1) {
            // assume if it's used multiple times in the same continuity, that it's storage
            flowDirection = FlowDirection.STORAGE;
        } else {
            // otherwise, we need to look at whether it's added or subtracted
            // since parsing removed the spaces in the expression, we know that the character right before the
            // variable should be what we need
            char sign;
            if (idx == 0) {
                sign = '+'; // if variable index is 0, implicit + sign assumed
            } else {
                sign = goalExpression.charAt(idx - 1);
            }
            flowDirection = classifyFlowDirection(sign, onLeft);
        }
        return flowDirection;
    }

    /**
     * Assign a FlowDirection to all variables used in a Goals expression.
     * This assumes the goal is expressible in a single case statement.
     *
     * @param goal A WRESL+ Goal object
     * @return A mapping of FlowDirections to a list of variable names of that flow type
     */
    public static Map<FlowDirection, List<String>> classifyFlowDirections(Goal goal) {
        HashMap<String, FlowDirection> flowDirections = new HashMap<>();
        String goalExpression = goal.caseExpression.getFirst(); // assume that continuity goals only have one case
        logger.log(Level.INFO, "Classifying flow directions for Expression: {0}", goalExpression);
        for (String variable : goal.expressionDependants){
            FlowDirection flowDirection = getVariableFlowDirectionFromExpression(variable, goalExpression);
            String[] logArray = {variable, flowDirection.toString()};
            logger.log(Level.INFO, "{0}: {1}", logArray);
            flowDirections.put(variable, flowDirection);
        }
        // the current mapping is variable -> direction, and we need to invert this to `direction -> array of variable`
        // first, figure out how many elements are in each array category
        Map<FlowDirection, List<String>> flowDirectionsInverted = new EnumMap<>(FlowDirection.class);
        for (Map.Entry<String, FlowDirection> entry : flowDirections.entrySet()) {
            flowDirectionsInverted
                    // make sure there is an empty list if the FlowDirectionValue isn't present
                    .computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    // then append the variable name
                    .add(entry.getKey());
        }
        return flowDirectionsInverted;
    }



    /**
     * Assign a FlowDirection to all variables used in a mapping of many Goals.
     * This assumes the goal is expressible in a single case statement.
     *
     * @param goals A mapping of goal names to Goal objects
     * @return A mapping of goal names to a nested mapping of FlowDirections to a list of variables of that flow type
     */
    public static Map<String, Map<FlowDirection, List<String>>> classifyFlowDirections(Map<String, Goal> goals) {
        HashMap<String, Map<FlowDirection, List<String>>> flowDirections = new HashMap<>();
        for (Map.Entry<String, Goal> entry: goals.entrySet()) {
            String goalName = entry.getKey();
            logger.log(Level.INFO, "Classifying flow directions for Goal: {0}", goalName);
            Goal goal = entry.getValue();
            flowDirections.put(goalName, classifyFlowDirections(goal));
        }
        return flowDirections;
    }

    /**
     * Write a network specification to a .yaml formatted file.
     *
     * @param network The nested mapping that specify the network connectivity on a node by node basis
     * @param dst The destination of the yaml file
     * @throws IOException Thrown on error when writing the file
     */
    public static void writeNetworkToYAML(Map<String, Map<FlowDirection, List<String>>> network, String dst) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dst))) {
            for (Map.Entry<String, Map<FlowDirection, List<String>>> nodeEntry : network.entrySet()) {
                String nodeName = nodeEntry.getKey();
                writer.write(nodeName + ":");
                writer.newLine();
                for (Map.Entry<FlowDirection, List<String>> dirEntry : nodeEntry.getValue().entrySet()) {
                    FlowDirection direction = dirEntry.getKey();
                    writer.write("  " + direction.name() + ":");
                    writer.newLine();
                    for (String variableName : dirEntry.getValue()) {
                        writer.write("    - " + variableName);
                        writer.newLine();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // read the args
        CommandLineOptions argsObject = CommandLineOptions.parseCLI(args);
        // Below is some needed WRIMS initialization to read the source files
        GlobalData.runDir = Paths.get(argsObject.mainFile).getParent().toString();  // set the run dir to the temp files dir
        FilePaths.setMainFilePaths(argsObject.mainFile);
        Path logFile = Files.createTempFile("tmp", ".log");  // create a log file in the temp dir too
        LogUtils.setLogFile(logFile.toString());
        // Now go ahead
        // Start by finding all the continuity goals defined in the model, currently uses heuristics to find things
        Map<String, Goal> goals = NetworkReader.findContinuityGoals(argsObject.mainFile);
        // Now categorize all the flows in those goals
        Map<String, Map<FlowDirection, List<String>>> network = NetworkReader.classifyFlowDirections(goals);
        // Write that to a yaml file
        writeNetworkToYAML(network, argsObject.dst);
    }
}