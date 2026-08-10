package gov.ca.water.wrims.engine.core.solver.Gurobi;

import com.gurobi.gurobi.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.commondata.solverdata.*;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;
import gov.ca.water.wrims.engine.core.ilp.ILP;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Tools;

public class GurobiSolver {
    private static class GurobiSolutionSnapshot {
        private final String[] varNames;
        private final double[] varValues;

        private GurobiSolutionSnapshot(String[] varNames, double[] varValues) {
            this.varNames = varNames;
            this.varValues = varValues;
        }
    }

    public static Map<String, Double> varDoubleMap;
    static GRBEnv env;
    static GRBModel model;

    public static void initialize() {

        try {
            //env   = new GRBEnv("TestGurobi.log");
            env = new GRBEnv();
            env.set(GRB.IntParam.LogToConsole, 0);
            //env.set(GRB.IntParam.Presolve, 0);
        } catch (GRBException e) {
            e.printStackTrace();
        }

    }

    public static void setLp(String CplexLpFilePath) {

        try {
            env.resetParams(); // reset Gurobi default values (may not be needed)
            env.set(GRB.IntParam.LogToConsole, 0);
            env.set(GRB.IntParam.Threads, 1);
            model = new GRBModel(env, CplexLpFilePath);
        } catch (GRBException e) {
            Error.addSolvingError("File not found: " + CplexLpFilePath);
            //e.printStackTrace();
        }

    }

    public static void solveDirect() {

        try {
            if (env == null) {
                System.err.println("Gurobi direct build called with null environment.");
                Error.addSolvingError("Gurobi environment not initialized.");
                return;
            }
            env.resetParams();
            env.set(GRB.IntParam.LogToConsole, 0);
            env.set(GRB.IntParam.Threads, 1);
            model = new GRBModel(env);
            buildDirectModel();
            solve();
        } catch (GRBException e) {
            Error.addSolvingError("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }

    }

    public static void dispose() {

        try {
            model.dispose();
            //env.release(); // release license
            env.dispose();
        } catch (GRBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void solve() {

        LpResult result = new LpResult();
        boolean triedBackupSolve = false;

        try {
            if (model == null) {
                System.err.println("Gurobi solve called with null model.");
                Error.addSolvingError("Gurobi model not initialized.");
                return;
            }

            long beginT = System.currentTimeMillis();
            model.optimize();
            long endT = System.currentTimeMillis();
            ILP.writeNoteLn(ILP.getYearMonthCycle(), String.format("%9.6f", (endT - beginT) / 1000.0), ILP._noteFile_gurobi_time);

            int optimstatus = model.get(GRB.IntAttr.Status);
            result.status = model.get(GRB.IntAttr.Status);

            if (optimstatus == GRB.Status.INFEASIBLE || optimstatus == GRB.Status.INF_OR_UNBD) {
                triedBackupSolve = true;
                optimstatus = solveWithBackupParams(optimstatus);
                result.status = optimstatus;
            }

            if (hasUsableSolution(optimstatus)) {
                updateCurrentObjectiveFromModel();
                populateResultFromCurrentModel(result, getCurrentSolutionSnapshot());
                collectDvar(result);
                assignDvar();
            } else if (optimstatus == GRB.Status.INFEASIBLE || optimstatus == GRB.Status.INF_OR_UNBD) {
                optimstatus = solveWithFeasRelax(optimstatus);
                result.status = optimstatus;

                if (hasUsableSolution(optimstatus)) {
                    updateCurrentObjectiveFromModel();
                    populateResultFromCurrentModel(result, getCurrentSolutionSnapshot());
                    collectDvar(result);
                    assignDvar();
                } else if (optimstatus == GRB.Status.UNBOUNDED) {
                    Error.addSolvingError("Model is unbounded after Gurobi feasRelax");
                } else {
                    Error.addSolvingError("Model is " + getGurobiStatusText(optimstatus) + " after Gurobi feasRelax");
                }
            } else if (optimstatus == GRB.Status.UNBOUNDED) {
                if (triedBackupSolve) {
                    Error.addSolvingError("Model is unbounded after Gurobi backup solve");
                } else {
                    Error.addSolvingError("Model is unbounded");
                }
            } else {
                Error.addSolvingError("Optimization was stopped with status = " + optimstatus);
            }

            // Dispose of model
            model.dispose();

        } catch (GRBException e) {

            Error.addSolvingError("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
        //return result;
    }

    public static void assignDvar() {
        Map<String, Map<String, IntDouble>> varCycleValueMap = ControlData.currStudyDataSet.getVarCycleValueMap();
        Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap = ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
        Set<String> dvarUsedByLaterCycle = ControlData.currModelDataSet.dvarUsedByLaterCycle;
        Set<String> dvarTimeArrayUsedByLaterCycle = ControlData.currModelDataSet.dvarTimeArrayUsedByLaterCycle;
        ArrayList<String> timeArrayDvList = ControlData.currModelDataSet.timeArrayDvList;
        String model = ControlData.currCycleName;

        StudyDataSet sds = ControlData.currStudyDataSet;
        ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
        ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
        Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();
        Set dvarCollection = dvarMap.keySet();
        Iterator dvarIterator = dvarCollection.iterator();

        // Debugging
        // HashSet<String> extraDv1 = new HashSet<String>(dvarMap.keySet());
        // extraDv1.removeAll(varDoubleMap.keySet());
        // HashSet<String> extraDv2 = new HashSet<String>(varDoubleMap.keySet());
        // extraDv2.removeAll(dvarMap.keySet());

        // System.out.println("Variables in dvarMap not in varDoubleMap: ");
        // System.out.println(extraDv1);
        // System.out.println("Variables in varDoubleMap not in dVarMap: ");
        // System.out.println(extraDv2);

        // JH Dec 2025: edits to try to patch like CBC does
        // add missing keys to varDoubleMap with default value 0 or lower bound
        HashSet<String> extraDv = new HashSet<String>(dvarMap.keySet());
        extraDv.removeAll(varDoubleMap.keySet());

        for (String dvName: extraDv) {
            Dvar dvar = dvarMap.get(dvName);
            double value=0;
            if (dvar.lowerBoundValue.doubleValue()>0) { value = dvar.lowerBoundValue.doubleValue();}
            varDoubleMap.put(dvName, value);
        }
        // end patch

        while (dvarIterator.hasNext()) {
            String dvName = (String) dvarIterator.next();
            Dvar dvar = dvarMap.get(dvName);

            double value = -77777777;
            try {
                value = varDoubleMap.get(dvName);
            } catch (Exception e) {
                System.err.println("Error: dvName not found in varDoubleMap: " + dvName);
                System.exit(1); // Exit with an error status

                //value = 0;  // TODO: warning!! needs work here!!
                //System.out.println(" This dvName not found: "+ dvName);
                //continue;
                // try {
                //     value = (Double) dvar.getData().getData(); // use whatever is in the container.
                // } catch (Exception e2) {
                //     value = -77777777; // TODO: if this value is used, then this is probably an error in the wresl code. need to give warning.
                // }
            }
            IntDouble id = new IntDouble(value, false);
            dvar.setData(id);
            if (dvarUsedByLaterCycle.contains(dvName)) {
                varCycleValueMap.get(dvName).put(model, id);
            } else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)) {
                if (varTimeArrayCycleValueMap.containsKey(dvName)) {
                    varTimeArrayCycleValueMap.get(dvName).put(model, dvar.data);
                } else {
                    Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
                    cycleValue.put(model, dvar.data);
                    varTimeArrayCycleValueMap.put(dvName, cycleValue);
                }
            }
            if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)) {
                if (varCycleIndexValueMap.containsKey(dvName)) {
                    varCycleIndexValueMap.get(dvName).put(model, dvar.data);
                } else {
                    Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
                    cycleValue.put(model, dvar.data);
                    varCycleIndexValueMap.put(dvName, cycleValue);
                }
            }
            String entryNameTS = DssOperation.entryNameTS(dvName, ControlData.timeStep);
            DataTimeSeries.saveDataToTimeSeries(dvName, entryNameTS, value, dvar);
            if (timeArrayDvList.contains(dvName)) {
                entryNameTS = DssOperation.entryNameTS(dvName + "__fut__0", ControlData.timeStep);
                DataTimeSeries.saveDataToTimeSeries(entryNameTS, value, dvar, 0);
            }
        }

        if (ControlData.showRunTimeMessage) {
            System.out.println("Objective Value: " + ControlData.lpsolve_objective);
            System.out.println("Assign Dvar Done.");
        }
    }

    private static void collectDvar(LpResult lpResult) {

        //Map<String, Dvar> dvarMap=SolverData.getDvarMap();
        varDoubleMap = new HashMap<String, Double>();

        for (int i = 0; i < lpResult.varValues.length; i++) {

            //System.out.println(lpResult.varNames[i]+":"+lpResult.varValues[i]);
            varDoubleMap.put(lpResult.varNames[i], lpResult.varValues[i]);

            // TODO: add the following line before sending the problem to the solver using direct link.
            // it's too late here. need to assign value.
            //if (!dvarMap.containsKey(lpResult.varNames[i])) addConditionalSlackSurplusToDvarMap(dvarMap, lpResult.varNames[i]);

        }

    }

    private static int solveWithBackupParams(int originalStatus) throws GRBException {
        System.out.println("Gurobi backup solve invoked for " + ILP.getYearMonthCycle() + " after status " + originalStatus);
        ILP.writeNoteLn(ILP.getYearMonthCycle(), "Gurobi backup solve invoked after status " + originalStatus, ILP._noteFile);

        applyBackupMipParams();
        model.optimize();
        return disambiguateStatus(model.get(GRB.IntAttr.Status));
    }

    private static int solveWithFeasRelax(int originalStatus) throws GRBException {

        System.out.println("Gurobi feasRelax invoked for " + ILP.getYearMonthCycle() + " after status " + originalStatus);
        ILP.writeNoteLn(ILP.getYearMonthCycle(), "Gurobi feasRelax invoked after status " + originalStatus, ILP._noteFile);

        // feasRelax(relaxobjtype, minrelax, vrelax, crelax):
        // 0 uses the default relaxation cost metric, true enforces minimum relaxation first,
        // true allows variable bound relaxation, and true allows constraint relaxation.
        double relaxObjective = model.feasRelax(0, true, true, true);
        applyBackupMipParams();
        model.optimize();

        int relaxedStatus = disambiguateStatus(model.get(GRB.IntAttr.Status));
        int solutionCount = model.get(GRB.IntAttr.SolCount);
        GurobiSolutionSnapshot solutionSnapshot = solutionCount > 0 ? getCurrentSolutionSnapshot() : null;
        writeFeasRelaxReport(originalStatus, relaxedStatus, solutionCount, relaxObjective, solutionSnapshot);
        return relaxedStatus;
    }

    private static void applyBackupMipParams() throws GRBException {
        model.set(GRB.DoubleParam.FeasibilityTol, 1e-4);
        model.set(GRB.DoubleParam.IntFeasTol, 1e-4);
        model.set(GRB.IntParam.MIPFocus, 1);
        model.set(GRB.IntParam.ZeroObjNodes, 1000);
    }

    private static int disambiguateStatus(int status) throws GRBException {
        if (status == GRB.Status.INF_OR_UNBD) {
            model.set(GRB.IntParam.DualReductions, 0);
            model.optimize();
            status = model.get(GRB.IntAttr.Status);
        }
        return status;
    }

    private static void updateCurrentObjectiveFromModel() throws GRBException {
        double objval = model.get(GRB.DoubleAttr.ObjVal);
        ControlData.gurobi_objective = objval;
    }

    private static boolean hasUsableSolution(int status) throws GRBException {
        return status == GRB.Status.OPTIMAL || model.get(GRB.IntAttr.SolCount) > 0;
    }

    private static void populateResultFromCurrentModel(LpResult result, GurobiSolutionSnapshot solutionSnapshot) {
        if (solutionSnapshot == null) {
            throw new IllegalArgumentException("solutionSnapshot must not be null when populating Gurobi results");
        }
        ArrayList<String> originalNames = new ArrayList<String>();
        ArrayList<Double> originalValues = new ArrayList<Double>();
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i < solutionSnapshot.varNames.length; i++) {
            if (!isFeasRelaxArtificialVar(solutionSnapshot.varNames[i], dvarMap)) {
                originalNames.add(solutionSnapshot.varNames[i]);
                originalValues.add(solutionSnapshot.varValues[i]);
            }
        }

        result.varNames = originalNames.toArray(new String[0]);
        result.varValues = new double[originalValues.size()];
        for (int i = 0; i < originalValues.size(); i++) {
            result.varValues[i] = originalValues.get(i);
        }
    }

    private static GurobiSolutionSnapshot getCurrentSolutionSnapshot() throws GRBException {
        GRBVar[] allVars = model.getVars();
        String[] allVarNames = model.get(GRB.StringAttr.VarName, allVars);
        double[] allValues = model.get(GRB.DoubleAttr.X, allVars);
        return new GurobiSolutionSnapshot(allVarNames, allValues);
    }

    private static void writeFeasRelaxReport(int originalStatus, int relaxedStatus, int solutionCount, double relaxObjective,
                                             GurobiSolutionSnapshot solutionSnapshot) throws GRBException {
        PrintWriter outFile = null;
        try {
            File ilpDir = new File(ILP.getIlpDir(), "feasrelax");
            String reportFileName = ILP.getYearMonthCycle() + "_feasrelax.txt";
            outFile = Tools.openFile(ilpDir.getAbsolutePath(), reportFileName);
            double reportThreshold = model.get(GRB.DoubleParam.FeasibilityTol);
            Map<String, Dvar> dvarMap = SolverData.getDvarMap();

            ILP.writeNoteLn("original_status", Integer.toString(originalStatus), outFile);
            ILP.writeNoteLn("original_status_text", getGurobiStatusText(originalStatus), outFile);
            ILP.writeNoteLn("relaxed_status", Integer.toString(relaxedStatus), outFile);
            ILP.writeNoteLn("relaxed_status_text", getGurobiStatusText(relaxedStatus), outFile);
            ILP.writeNoteLn("solution_count", Integer.toString(solutionCount), outFile);
            ILP.writeNoteLn("relax_objective", Double.toString(relaxObjective), outFile);
            if (solutionCount > 0) {
                ILP.writeNoteLn("objective_mode", "original objective on the minimally relaxed model", outFile);
                ILP.writeNoteLn("objective", Double.toString(model.get(GRB.DoubleAttr.ObjVal)), outFile);
            }
            ILP.writeNoteLn("threshold", Double.toString(reportThreshold), outFile);
            ILP.writeNoteLn("relaxations", outFile);

            ArrayList<String> relaxEntries = new ArrayList<String>();

            if (solutionSnapshot != null) {
                for (int i = 0; i < solutionSnapshot.varNames.length; i++) {
                    if (isFeasRelaxArtificialVar(solutionSnapshot.varNames[i], dvarMap)
                            && Math.abs(solutionSnapshot.varValues[i]) > reportThreshold) {
                        relaxEntries.add(solutionSnapshot.varNames[i] + ": " + solutionSnapshot.varValues[i]);
                    }
                }
            }

            Collections.sort(relaxEntries);
            if (solutionSnapshot == null) {
                ILP.writeNoteLn("no_solution_available", outFile);
            } else if (relaxEntries.isEmpty()) {
                ILP.writeNoteLn("none_above_threshold", outFile);
            } else {
                for (String relaxEntry : relaxEntries) {
                    ILP.writeNoteLn(relaxEntry, outFile);
                }
            }
        } catch (IOException e) {
            Error.addSolvingError("Unable to write Gurobi feasRelax report: " + e.getMessage());
        } finally {
            if (outFile != null) {
                outFile.close();
            }
        }
    }

    private static boolean isFeasRelaxArtificialVar(String varName, Map<String, Dvar> dvarMap) {
        return varName != null && varName.startsWith("Art") && !dvarMap.containsKey(varName);
    }

    private static String getGurobiStatusText(int status) {
        switch (status) {
            case GRB.Status.LOADED:
                return "LOADED";
            case GRB.Status.OPTIMAL:
                return "OPTIMAL";
            case GRB.Status.INFEASIBLE:
                return "INFEASIBLE";
            case GRB.Status.INF_OR_UNBD:
                return "INF_OR_UNBD";
            case GRB.Status.UNBOUNDED:
                return "UNBOUNDED";
            case GRB.Status.CUTOFF:
                return "CUTOFF";
            case GRB.Status.ITERATION_LIMIT:
                return "ITERATION_LIMIT";
            case GRB.Status.NODE_LIMIT:
                return "NODE_LIMIT";
            case GRB.Status.TIME_LIMIT:
                return "TIME_LIMIT";
            case GRB.Status.SOLUTION_LIMIT:
                return "SOLUTION_LIMIT";
            case GRB.Status.INTERRUPTED:
                return "INTERRUPTED";
            case GRB.Status.NUMERIC:
                return "NUMERIC";
            case GRB.Status.SUBOPTIMAL:
                return "SUBOPTIMAL";
            case GRB.Status.INPROGRESS:
                return "INPROGRESS";
            case GRB.Status.USER_OBJ_LIMIT:
                return "USER_OBJ_LIMIT";
            case GRB.Status.WORK_LIMIT:
                return "WORK_LIMIT";
            case GRB.Status.MEM_LIMIT:
                return "MEM_LIMIT";
            default:
                return "STATUS_" + status;
        }
    }

    private static void buildDirectModel() throws GRBException {

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();
        ensureSlackSurplusDvars(dvarMap);
        Map<String, WeightElement> activeWeightMap = getActiveWeightMap();
        Map<String, GRBVar> directVarMap = new HashMap<String, GRBVar>();

        for (String dvarName : dvarMap.keySet()) {
            Dvar dvar = dvarMap.get(dvarName);
            double lb = dvar.lowerBoundValue.doubleValue();
            double ub = dvar.upperBoundValue.doubleValue();
            char vtype = dvar.integer.equalsIgnoreCase(Param.yes) ? GRB.INTEGER : GRB.CONTINUOUS;
            double obj = 0.0;
            WeightElement weightElement = activeWeightMap.get(dvarName);
            if (weightElement != null) {
                obj = weightElement.getValue();
            }
            GRBVar gv = model.addVar(lb, ub, obj, vtype, dvarName);
            directVarMap.put(dvarName, gv);
        }

        model.set(GRB.IntAttr.ModelSense, GRB.MAXIMIZE);
        model.update();
        addDirectConstraints(directVarMap);
    }

    private static void addDirectConstraints(Map<String, GRBVar> directVarMap) throws GRBException {

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        ArrayList<String> sortedConstraint = new ArrayList<String>(constraintMap.keySet());
        Collections.sort(sortedConstraint);

        for (String constraintName : sortedConstraint) {
            EvalConstraint ec = constraintMap.get(constraintName);
            if (ec.getEvalExpression().isNumeric()) {
                continue; // skip trivial constraint
            }

            GRBLinExpr expr = new GRBLinExpr();
            ArrayList<String> sortedTerm = new ArrayList<String>(ec.getEvalExpression().getMultiplier().keySet());
            Collections.sort(sortedTerm);

            for (String multName : sortedTerm) {
                double coef = ec.getEvalExpression().getMultiplier().get(multName).getData().doubleValue();
                GRBVar var = directVarMap.get(multName);
                if (var == null) {
                    String multNameNorm = multName.trim();
                    String multNameLower = multNameNorm.toLowerCase();
                    if (multNameLower.startsWith("slack__") || multNameLower.startsWith("surplus__")) {
                        var = model.addVar(0.0, 1.0e23, 0.0, GRB.CONTINUOUS, multName);
                        directVarMap.put(multName, var);
                    } else {
                        Error.addSolvingError("Missing variable in Gurobi direct build: " + multName);
                        continue;
                    }
                }
                expr.addTerm(coef, var);
            }

            String sign = ec.getSign();
            double rhs = -ec.getEvalExpression().getValue().getData().doubleValue();
            if (sign.equals("=")) {
                model.addConstr(expr, GRB.EQUAL, rhs, constraintName);
            } else if (sign.equals("<") || sign.equals("<=")) {
                model.addConstr(expr, GRB.LESS_EQUAL, rhs, constraintName);
            } else if (sign.equals(">") || sign.equals(">=")) {
                model.addConstr(expr, GRB.GREATER_EQUAL, rhs, constraintName);
            }
        }
    }

    private static Map<String, WeightElement> getActiveWeightMap() {

        Map<String, WeightElement> activeWeightMap = new HashMap<String, WeightElement>();

        for (String usedSlackSurplus : ControlData.currModelDataSet.usedWtSlackSurplusList) {
            activeWeightMap.put(usedSlackSurplus, SolverData.getWeightSlackSurplusMap().get(usedSlackSurplus));
        }

        activeWeightMap.putAll(SolverData.getWeightMap());

        return activeWeightMap;
    }

    private static void ensureSlackSurplusDvars(Map<String, Dvar> dvarMap) {
        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        for (String constraintName : constraintMap.keySet()) {
            EvalConstraint ec = constraintMap.get(constraintName);
            if (ec.getEvalExpression().isNumeric()) {
                continue;
            }
            for (String multName : ec.getEvalExpression().getMultiplier().keySet()) {
                String multNameNorm = multName.trim();
                String multNameLower = multNameNorm.toLowerCase();
                if ((multNameLower.startsWith("slack__") || multNameLower.startsWith("surplus__"))
                        && !dvarMap.containsKey(multName)) {
                    addConditionalSlackSurplusToDvarMap(dvarMap, multName);
                }
            }
        }
    }

    public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName) {
        Dvar dvar = new Dvar();
        dvar.upperBoundValue = 1.0e23;
        dvar.lowerBoundValue = 0.0;
        dvarMap.put(multName, dvar);
    }
}
