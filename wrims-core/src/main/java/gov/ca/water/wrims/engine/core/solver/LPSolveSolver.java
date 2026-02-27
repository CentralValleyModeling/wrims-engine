package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class LPSolveSolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(LPSolveSolver.class);
    public static Map<String, Double> varDoubleMap;
    public static String configFile = null;
    public static int numberOfRetries = 0;  // default is zero
    private static LpSolve solver;
    private static String lpFilePath;
    private static ArrayList<String> origcolName;

    public static void setLP(String filePath) {

        try {
            // TODO: remove this test

            solver = LpSolve.readLp(filePath, LpSolve.CRITICAL, "test_prob");
            try {
                solver.readParams(configFile, "-h Default");
            } catch (Exception e) {
                Error.addSolvingError("Header \"Default\" not found in LpSolve config file");
            }

            lpFilePath = filePath;
            origcolName = new ArrayList<String>();
            for (int i = 1; i <= solver.getNorigColumns(); i++) {
                origcolName.add(solver.getOrigcolName(i));
            }

        } catch (LpSolveException e) {
            e.printStackTrace();
        }
    }

    public static void solve() {
        int i = 1;
        int modelStatus = -777;
        try {
            modelStatus = solver.solve();
            while ((modelStatus != LpSolve.OPTIMAL) && (i <= numberOfRetries)) {
                solver.deleteLp();
                solver = LpSolve.readLp(lpFilePath, LpSolve.CRITICAL, "test_prob");
                try {
                    LOGGER.atInfo().setMessage("! Retry with LpSolve config named: Retry" + i).log();
                    solver.readParams(configFile, "-h Retry" + i);
                    modelStatus = solver.solve();
                    i++;
                } catch (Exception e) {
                    Error.addSolvingError("Header \"Retry" + i + "\" not found in LpSolve config file");
                    break;
                }
            }

            if (modelStatus != LpSolve.OPTIMAL) {
                getSolverInformation(modelStatus);
            }

            if (Error.error_solving.size() == 0) {
                ControlData.lpsolve_objective = solver.getObjective(); // for other processes
                collectDvar();
                assignDvar();
            }
            // delete the problem and free memory
            solver.deleteLp();

        } catch (LpSolveException e) {
            Error.addSolvingError("LpSolveSolver error.");
            e.printStackTrace();
        }

    }

    public static void getSolverInformation(int modelStatus) {

        Error.addSolvingError(solver.getStatustext(modelStatus));
        Error.addSolvingError(lpFilePath);
    }

    private static void collectDvar() throws LpSolveException {
        varDoubleMap = new HashMap<String, Double>();
        int rn = solver.getNorigRows();
        int cn = solver.getNorigColumns();
        for (int i = 1; i <= cn; i++) {
            varDoubleMap.put(origcolName.get(i - 1), solver.getVarPrimalresult(i + rn));
        }
    }

    private static void assignDvar() throws LpSolveException {
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

        while (dvarIterator.hasNext()) {
            String dvName = (String) dvarIterator.next();
            Dvar dvar = dvarMap.get(dvName);

            double value = -77777777;
            try {
                value = varDoubleMap.get(dvName);
            } catch (Exception e) {
                //value = 0;  // TODO: warning!! needs work here!!

                //continue;
                try {
                    value = (Double) dvar.getData().getData(); // use whatever is in the container.
                } catch (Exception e2) {
                    value = -77777777; // TODO: if this value is used, then this is probably an error in the wresl code. need to give warning.
                }
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
            LOGGER.atInfo().setMessage("Objective Value: " + ControlData.lpsolve_objective).log();
            LOGGER.atInfo().setMessage("Assign Dvar Done.").log();
        }
    }

    public static void setDefaultOption() {
        solver.setSimplextype(LpSolve.SIMPLEX_DUAL_PRIMAL);
        solver.setImprove(LpSolve.IMPROVE_DUALFEAS | LpSolve.IMPROVE_THETAGAP);
        solver.setAntiDegen(LpSolve.ANTIDEGEN_FIXEDVARS | LpSolve.ANTIDEGEN_STALLING);
        solver.setPivoting(LpSolve.PRICER_DEVEX | LpSolve.PRICE_ADAPTIVE);
        solver.setScaling(LpSolve.SCALE_GEOMETRIC | LpSolve.SCALE_EQUILIBRATE | LpSolve.SCALE_INTEGERS);
        solver.setBbFloorfirst(LpSolve.BRANCH_AUTOMATIC);
        solver.setBbRule(LpSolve.NODE_GREEDYMODE | LpSolve.NODE_DYNAMICMODE | LpSolve.NODE_RCOSTFIXING | LpSolve.NODE_PSEUDONONINTSELECT);
        solver.setTimeout(5);
        solver.setEpsint(2E-7);
        solver.setEpsel(1E-11);
        solver.setPresolve(LpSolve.PRESOLVE_ROWS | LpSolve.PRESOLVE_COLS, solver.getPresolveloops());
    }

    public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName) {
        Dvar dvar = new Dvar();
        dvar.upperBoundValue = 1.0e23;
        dvar.lowerBoundValue = 0.0;
        dvarMap.put(multName, dvar);
    }
}
