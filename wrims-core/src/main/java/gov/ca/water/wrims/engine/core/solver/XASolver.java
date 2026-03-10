package gov.ca.water.wrims.engine.core.solver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;

/**
 * XA solver wrapper upgraded to align with CBC-style shared performance logging:
 *
 * 1) Main XA structured logs go to xa.log
 * 2) Performance timing logs go through shared PerformanceTimer
 * 3) No XA-specific perf logger/XML is required
 * 4) Avoid heavy logging inside high-frequency loops
 * 5) Preserve legacy compatibility while allowing safer tolerant execution
 */
public class XASolver {

    // Main XA structured logger -> xa.log
    private static final Logger LOG_MAIN =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.xa.main");

    private static final int SAMPLE_LIMIT = 5;
    private static final double DEFAULT_SLACK_SURPLUS_UB = 1.0e23;

    private enum Mode {
        LEGACY,
        TOLERANT
    }

    private static Mode readMode() {
        String mode = System.getProperty("wrims.xa.mode");
        if (mode != null) {
            String m = mode.trim().toLowerCase();
            if ("tolerant".equals(m)) return Mode.TOLERANT;
            if ("legacy".equals(m)) return Mode.LEGACY;
        }

        String tolerant = System.getProperty("wrims.xa.tolerant");
        if (tolerant != null) {
            String t = tolerant.trim().toLowerCase();
            if ("true".equals(t) || "1".equals(t) || "yes".equals(t)) return Mode.TOLERANT;
        }

        return Mode.LEGACY;
    }

    private static final class RunContext {
        final Mode mode;
        final int cycleIndex1Based;
        final String dateStr;
        final String cycleName;
        final String timeStep;
        final String runId;

        RunContext(Mode mode) {
            this.mode = mode;
            this.cycleIndex1Based = ControlData.currCycleIndex + 1;
            this.dateStr = ControlData.currMonth + "/" + ControlData.currDay + "/" + ControlData.currYear;
            this.cycleName = ControlData.currCycleName;
            this.timeStep = String.valueOf(ControlData.timeStep);
            this.runId = UUID.randomUUID().toString();
        }

        String modeName() {
            return mode == null ? "legacy" : mode.name().toLowerCase();
        }

        String ctx() {
            return "date=" + dateStr
                    + " cycleIndex=" + cycleIndex1Based
                    + " cycleName=" + cycleName
                    + " timeStep=" + timeStep
                    + " run_id=" + runId;
        }
    }

    /**
     * BuildStats is used only in tolerant mode.
     * It aggregates missing/skipped items and keeps only a few samples,
     * avoiding heavy per-item logging in high-frequency loops.
     */
    private static final class BuildStats {
        int constraintsSeen = 0;
        int constraintsSet = 0;
        int constraintsSkippedMissing = 0;
        int constraintsSkippedUnknownSign = 0;

        int dvarsSeen = 0;
        int dvarsSet = 0;
        int dvarsSkippedMissing = 0;

        int weightsSeen = 0;
        int weightsSet = 0;
        int weightsSkippedMissing = 0;

        int slackSurplusSeen = 0;
        int slackSurplusSet = 0;
        int slackSurplusSkippedMissing = 0;

        long multiplierEntriesLoaded = 0;

        int dvListCount = 0;
        int dvTimeArrayCount = 0;
        int wtListCount = 0;
        int wtTimeArrayCount = 0;
        int usedSlackSurplusCount = 0;
        int gListCount = 0;
        int gTimeArrayCount = 0;

        final List<String> missingConstraintSamples = new ArrayList<String>();
        final List<String> unknownSignSamples = new ArrayList<String>();
        final List<String> missingDvarSamples = new ArrayList<String>();
        final List<String> missingWeightSamples = new ArrayList<String>();
        final List<String> missingSlackSurplusWeightSamples = new ArrayList<String>();
        final List<String> autoAddedSlackSurplusSamples = new ArrayList<String>();
    }

    int modelStatus;

    public XASolver() {
        final long totalStart = System.currentTimeMillis();
        final Mode mode = readMode();
        final RunContext rc = new RunContext(mode);

        putMdc(rc);

        long msLoadModel = 0L;
        long msSetConstraints = 0L;
        long msSetDVars = 0L;
        long msSetWeights = 0L;
        long msSolve = 0L;
        long msAssign = 0L;

        boolean assignAttempted = false;
        boolean assignSucceeded = false;
        String assignSkipReason = null;

        String solverStatusStr = null;
        double objective = Double.NaN;

        BuildStats stats = (mode == Mode.TOLERANT) ? new BuildStats() : null;

        // Shared total timer -> solver-perf.log via PerformanceTimer logger
        PerformanceTimer totalTimer = new PerformanceTimer("XA.total");

        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Solving " + rc.ctx() + " mode=" + rc.modeName());
        }

        LOG_MAIN.atInfo()
                .setMessage("event=enter solver=XA mode={} {}")
                .addArgument(rc.modeName())
                .addArgument(rc.ctx())
                .addKeyValue("event", "enter")
                .addKeyValue("solver", "XA")
                .addKeyValue("mode", rc.modeName())
                .addKeyValue("cycleName", rc.cycleName)
                .addKeyValue("timeStep", rc.timeStep)
                .addKeyValue("run_id", rc.runId)
                .log();

        try {
            PerformanceTimer tLoad = new PerformanceTimer("XA.loadModel");
            ControlData.xasolver.loadNewModel();
            msLoadModel = tLoad.stop();

            PerformanceTimer tConstraints = new PerformanceTimer("XA.setConstraints");
            if (mode == Mode.TOLERANT) {
                setConstraints_Tolerant(rc, stats);
            } else {
                setConstraints_Legacy();
            }
            msSetConstraints = tConstraints.stop();

            PerformanceTimer tDvars = new PerformanceTimer("XA.setDVars");
            if (mode == Mode.TOLERANT) {
                setDVars_Tolerant(rc, stats);
            } else {
                setDVars_Legacy();
            }
            msSetDVars = tDvars.stop();

            PerformanceTimer tWeights = new PerformanceTimer("XA.setWeights");
            if (mode == Mode.TOLERANT) {
                setWeights_Tolerant(rc, stats);
            } else {
                setWeights_Legacy();
            }
            msSetWeights = tWeights.stop();

            PerformanceTimer tSolve = new PerformanceTimer("XA.solve");
            if (ControlData.showRunTimeMessage) {
                System.out.println("XA Solver: Calling solveWithInfeasibleAnalysis ...");
            }
            ControlData.xasolver.solveWithInfeasibleAnalysis("Output console:");
            msSolve = tSolve.stop();

            modelStatus = ControlData.xasolver.getModelStatus();
            solverStatusStr = safeToString(ControlData.xasolver.getSolverStatus());
            objective = safeGetObjective();

            LOG_MAIN.atInfo()
                    .setMessage("event=solve_done solver=XA mode={} modelStatus={} solverStatus={} solve_ms={} objective={} {}")
                    .addArgument(rc.modeName())
                    .addArgument(modelStatus)
                    .addArgument(solverStatusStr)
                    .addArgument(msSolve)
                    .addArgument(formatDouble(objective))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "solve_done")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("mode", rc.modeName())
                    .addKeyValue("modelStatus", modelStatus)
                    .addKeyValue("solverStatus", solverStatusStr)
                    .addKeyValue("solve_ms", msSolve)
                    .addKeyValue("objective", formatDouble(objective))
                    .addKeyValue("run_id", rc.runId)
                    .log();

            if (modelStatus >= 2) {
                getSolverInformation_CBCStyle(rc, modelStatus, solverStatusStr);
            }

            assignAttempted = true;
            if (mode == Mode.TOLERANT) {
                if (Error.error_solving.size() < 1 && modelStatus < 2) {
                    PerformanceTimer tAssign = new PerformanceTimer("XA.assign");
                    assignDvar(rc);
                    msAssign = tAssign.stop();
                    assignSucceeded = true;
                } else {
                    assignSucceeded = false;
                    assignSkipReason = (Error.error_solving.size() >= 1)
                            ? "solvingErrors=" + Error.error_solving.size()
                            : "modelStatus=" + modelStatus;
                    LOG_MAIN.atWarn()
                            .setMessage("event=assign_skip solver=XA mode=tolerant reason={} {}")
                            .addArgument(assignSkipReason)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "assign_skip")
                            .addKeyValue("solver", "XA")
                            .addKeyValue("mode", "tolerant")
                            .addKeyValue("reason", assignSkipReason)
                            .addKeyValue("run_id", rc.runId)
                            .log();
                }
            } else {
                if (Error.error_solving.size() < 1) {
                    PerformanceTimer tAssign = new PerformanceTimer("XA.assign");
                    assignDvar(rc);
                    msAssign = tAssign.stop();
                    assignSucceeded = true;
                } else {
                    assignSucceeded = false;
                    assignSkipReason = "solvingErrors=" + Error.error_solving.size();
                    LOG_MAIN.atWarn()
                            .setMessage("event=assign_skip solver=XA mode=legacy reason={} {}")
                            .addArgument(assignSkipReason)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "assign_skip")
                            .addKeyValue("solver", "XA")
                            .addKeyValue("mode", "legacy")
                            .addKeyValue("reason", assignSkipReason)
                            .addKeyValue("run_id", rc.runId)
                            .log();
                }
            }

        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            LOG_MAIN.atError()
                    .setMessage("event=exception solver=XA mode={} message={} {}")
                    .addArgument(rc.modeName())
                    .addArgument(msg)
                    .addArgument(rc.ctx())
                    .setCause(e)
                    .addKeyValue("event", "exception")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("mode", rc.modeName())
                    .addKeyValue("run_id", rc.runId)
                    .log();

            try {
                Error.addSolvingError("Exception during XA solve: " + msg);
            } catch (Exception ignore) {
            }

            try {
                modelStatus = ControlData.xasolver.getModelStatus();
            } catch (Exception ignore) {
                modelStatus = 99;
            }

        } finally {
            long totalMs = System.currentTimeMillis() - totalStart;

            if (Double.isNaN(objective)) {
                objective = safeGetObjective();
            }
            if (solverStatusStr == null) {
                try {
                    solverStatusStr = safeToString(ControlData.xasolver.getSolverStatus());
                } catch (Exception ignore) {
                    solverStatusStr = "unknown";
                }
            }

            maybeLogBuildGaps(rc, stats);

            String assignInfo;
            if (!assignAttempted) {
                assignInfo = "assign=not_attempted";
            } else if (assignSucceeded) {
                assignInfo = "assign=done";
            } else {
                assignInfo = "assign=skipped reason=" + (assignSkipReason == null ? "unknown" : assignSkipReason);
            }

            if (mode == Mode.TOLERANT && stats != null) {
                LOG_MAIN.atInfo()
                        .setMessage("event=summary solver=XA mode=tolerant total_ms={} modelStatus={} solverStatus={} objective={} solvingErrors={} constraintsSeen={} constraintsSet={} constraintsSkippedMissing={} constraintsSkippedUnknownSign={} dvarsSeen={} dvarsSet={} dvarsSkippedMissing={} weightsSeen={} weightsSet={} weightsSkippedMissing={} slackSurplusSeen={} slackSurplusSet={} slackSurplusSkippedMissing={} load_ms={} constraints_ms={} dvars_ms={} weights_ms={} solve_ms={} assign_ms={} {} {}")
                        .addArgument(totalMs)
                        .addArgument(modelStatus)
                        .addArgument(solverStatusStr)
                        .addArgument(formatDouble(objective))
                        .addArgument(Error.error_solving.size())
                        .addArgument(stats.constraintsSeen)
                        .addArgument(stats.constraintsSet)
                        .addArgument(stats.constraintsSkippedMissing)
                        .addArgument(stats.constraintsSkippedUnknownSign)
                        .addArgument(stats.dvarsSeen)
                        .addArgument(stats.dvarsSet)
                        .addArgument(stats.dvarsSkippedMissing)
                        .addArgument(stats.weightsSeen)
                        .addArgument(stats.weightsSet)
                        .addArgument(stats.weightsSkippedMissing)
                        .addArgument(stats.slackSurplusSeen)
                        .addArgument(stats.slackSurplusSet)
                        .addArgument(stats.slackSurplusSkippedMissing)
                        .addArgument(msLoadModel)
                        .addArgument(msSetConstraints)
                        .addArgument(msSetDVars)
                        .addArgument(msSetWeights)
                        .addArgument(msSolve)
                        .addArgument(msAssign)
                        .addArgument(assignInfo)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "summary")
                        .addKeyValue("solver", "XA")
                        .addKeyValue("mode", "tolerant")
                        .addKeyValue("total_ms", totalMs)
                        .addKeyValue("modelStatus", modelStatus)
                        .addKeyValue("solverStatus", solverStatusStr)
                        .addKeyValue("objective", formatDouble(objective))
                        .addKeyValue("solvingErrors", Error.error_solving.size())
                        .addKeyValue("constraintsSeen", stats.constraintsSeen)
                        .addKeyValue("constraintsSet", stats.constraintsSet)
                        .addKeyValue("constraintsSkippedMissing", stats.constraintsSkippedMissing)
                        .addKeyValue("constraintsSkippedUnknownSign", stats.constraintsSkippedUnknownSign)
                        .addKeyValue("dvarsSeen", stats.dvarsSeen)
                        .addKeyValue("dvarsSet", stats.dvarsSet)
                        .addKeyValue("dvarsSkippedMissing", stats.dvarsSkippedMissing)
                        .addKeyValue("weightsSeen", stats.weightsSeen)
                        .addKeyValue("weightsSet", stats.weightsSet)
                        .addKeyValue("weightsSkippedMissing", stats.weightsSkippedMissing)
                        .addKeyValue("slackSurplusSeen", stats.slackSurplusSeen)
                        .addKeyValue("slackSurplusSet", stats.slackSurplusSet)
                        .addKeyValue("slackSurplusSkippedMissing", stats.slackSurplusSkippedMissing)
                        .addKeyValue("load_ms", msLoadModel)
                        .addKeyValue("constraints_ms", msSetConstraints)
                        .addKeyValue("dvars_ms", msSetDVars)
                        .addKeyValue("weights_ms", msSetWeights)
                        .addKeyValue("solve_ms", msSolve)
                        .addKeyValue("assign_ms", msAssign)
                        .addKeyValue("assignInfo", assignInfo)
                        .addKeyValue("run_id", rc.runId)
                        .log();
            } else {
                LOG_MAIN.atInfo()
                        .setMessage("event=summary solver=XA mode=legacy total_ms={} modelStatus={} solverStatus={} objective={} solvingErrors={} load_ms={} constraints_ms={} dvars_ms={} weights_ms={} solve_ms={} assign_ms={} {} {}")
                        .addArgument(totalMs)
                        .addArgument(modelStatus)
                        .addArgument(solverStatusStr)
                        .addArgument(formatDouble(objective))
                        .addArgument(Error.error_solving.size())
                        .addArgument(msLoadModel)
                        .addArgument(msSetConstraints)
                        .addArgument(msSetDVars)
                        .addArgument(msSetWeights)
                        .addArgument(msSolve)
                        .addArgument(msAssign)
                        .addArgument(assignInfo)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "summary")
                        .addKeyValue("solver", "XA")
                        .addKeyValue("mode", "legacy")
                        .addKeyValue("total_ms", totalMs)
                        .addKeyValue("modelStatus", modelStatus)
                        .addKeyValue("solverStatus", solverStatusStr)
                        .addKeyValue("objective", formatDouble(objective))
                        .addKeyValue("solvingErrors", Error.error_solving.size())
                        .addKeyValue("load_ms", msLoadModel)
                        .addKeyValue("constraints_ms", msSetConstraints)
                        .addKeyValue("dvars_ms", msSetDVars)
                        .addKeyValue("weights_ms", msSetWeights)
                        .addKeyValue("solve_ms", msSolve)
                        .addKeyValue("assign_ms", msAssign)
                        .addKeyValue("assignInfo", assignInfo)
                        .addKeyValue("run_id", rc.runId)
                        .log();
            }

            totalTimer.stop();

            long end = Calendar.getInstance().getTimeInMillis();
            ControlData.t_xa = ControlData.t_xa + (int) (end - totalStart);

            clearMdc();
        }
    }

    // ============================================================
    // LEGACY implementations (behavior compatibility)
    // ============================================================

    private void setDVars_Legacy() {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting dvars");
        }

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();
        for (int i = 0; i <= 1; i++) {
            ArrayList<String> dvarCollection = (i == 0)
                    ? ControlData.currModelDataSet.dvList
                    : ControlData.currModelDataSet.dvTimeArrayList;

            Iterator<String> dvarIterator = dvarCollection.iterator();
            while (dvarIterator.hasNext()) {
                String dvarName = dvarIterator.next();
                Dvar dvar = dvarMap.get(dvarName);

                double lb = dvar.lowerBoundValue.doubleValue();
                double ub = dvar.upperBoundValue.doubleValue();

                if ("y".equals(dvar.integer)) {
                    ControlData.xasolver.setColumnInteger(dvarName, lb, ub);
                } else {
                    ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);
                }
            }
        }
    }

    private void setWeights_Legacy() {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting weights");
        }

        Map<String, WeightElement> weightMap = SolverData.getWeightMap();
        for (int i = 0; i <= 1; i++) {
            ArrayList<String> weightCollection = (i == 0)
                    ? ControlData.currModelDataSet.wtList
                    : ControlData.currModelDataSet.wtTimeArrayList;

            Iterator<String> weightIterator = weightCollection.iterator();
            while (weightIterator.hasNext()) {
                String weightName = weightIterator.next();
                ControlData.xasolver.setColumnObjective(weightName, weightMap.get(weightName).getValue());
            }
        }

        Map<String, WeightElement> slackMap = SolverData.getWeightSlackSurplusMap();
        CopyOnWriteArrayList<String> used = ControlData.currModelDataSet.usedWtSlackSurplusList;
        Iterator<String> it = used.iterator();
        while (it.hasNext()) {
            String name = it.next();
            ControlData.xasolver.setColumnObjective(name, slackMap.get(name).getValue());
        }
    }

    private void setConstraints_Legacy() {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting constraints");
        }

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> constraintCollection;
            if (i == 0) {
                constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
                constraintCollection.retainAll(constraintMap.keySet());
            } else {
                constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
            }

            Iterator<String> constraintIterator = constraintCollection.iterator();
            while (constraintIterator.hasNext()) {
                String constraintName = constraintIterator.next();
                EvalConstraint ec = constraintMap.get(constraintName);

                if ("=".equals(ec.getSign())) {
                    ControlData.xasolver.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                } else if ("<".equals(ec.getSign()) || "<=".equals(ec.getSign())) {
                    ControlData.xasolver.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                } else if (">".equals(ec.getSign())) {
                    ControlData.xasolver.setRowMin(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                }

                HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
                Set<String> multCollection = multMap.keySet();
                Iterator<String> multIterator = multCollection.iterator();

                while (multIterator.hasNext()) {
                    String multName = multIterator.next();
                    if (!dvarMap.containsKey(multName)) {
                        addConditionalSlackSurplusToDvarMap(dvarMap, multName);
                    }
                    ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
                }
            }
        }
    }

    // ============================================================
    // TOLERANT implementations
    // ============================================================

    private void setDVars_Tolerant(RunContext rc, BuildStats stats) {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting dvars");
        }

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> dvarCollection;
            if (i == 0) {
                dvarCollection = ControlData.currModelDataSet.dvList;
                stats.dvListCount = safeSize(dvarCollection);
            } else {
                dvarCollection = ControlData.currModelDataSet.dvTimeArrayList;
                stats.dvTimeArrayCount = safeSize(dvarCollection);
            }

            Iterator<String> it = dvarCollection.iterator();
            while (it.hasNext()) {
                String dvarName = it.next();
                stats.dvarsSeen++;

                Dvar dvar = dvarMap == null ? null : dvarMap.get(dvarName);
                if (dvar == null) {
                    stats.dvarsSkippedMissing++;
                    recordSample(stats.missingDvarSamples, dvarName);
                    continue;
                }

                double lb = dvar.lowerBoundValue.doubleValue();
                double ub = dvar.upperBoundValue.doubleValue();

                if ("y".equals(dvar.integer)) {
                    ControlData.xasolver.setColumnInteger(dvarName, lb, ub);
                } else {
                    ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);
                }
                stats.dvarsSet++;
            }
        }
    }

    private void setWeights_Tolerant(RunContext rc, BuildStats stats) {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting weights");
        }

        Map<String, WeightElement> weightMap = SolverData.getWeightMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> weightCollection;
            if (i == 0) {
                weightCollection = ControlData.currModelDataSet.wtList;
                stats.wtListCount = safeSize(weightCollection);
            } else {
                weightCollection = ControlData.currModelDataSet.wtTimeArrayList;
                stats.wtTimeArrayCount = safeSize(weightCollection);
            }

            Iterator<String> it = weightCollection.iterator();
            while (it.hasNext()) {
                String weightName = it.next();
                stats.weightsSeen++;

                WeightElement we = weightMap == null ? null : weightMap.get(weightName);
                if (we == null) {
                    stats.weightsSkippedMissing++;
                    recordSample(stats.missingWeightSamples, weightName);
                    continue;
                }

                ControlData.xasolver.setColumnObjective(weightName, we.getValue());
                stats.weightsSet++;
            }
        }

        Map<String, WeightElement> slackMap = SolverData.getWeightSlackSurplusMap();
        CopyOnWriteArrayList<String> used = ControlData.currModelDataSet.usedWtSlackSurplusList;
        stats.usedSlackSurplusCount = safeSize(used);

        Iterator<String> it2 = used.iterator();
        while (it2.hasNext()) {
            String name = it2.next();
            stats.slackSurplusSeen++;

            WeightElement we = slackMap == null ? null : slackMap.get(name);
            if (we == null) {
                stats.slackSurplusSkippedMissing++;
                recordSample(stats.missingSlackSurplusWeightSamples, name);
                continue;
            }

            ControlData.xasolver.setColumnObjective(name, we.getValue());
            stats.slackSurplusSet++;
        }
    }

    private void setConstraints_Tolerant(RunContext rc, BuildStats stats) {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Setting constraints");
        }

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> constraintCollection;
            if (i == 0) {
                constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
                stats.gListCount = safeSize(constraintCollection);
            } else {
                constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
                stats.gTimeArrayCount = safeSize(constraintCollection);
            }

            Iterator<String> it = constraintCollection.iterator();
            while (it.hasNext()) {
                String constraintName = it.next();
                stats.constraintsSeen++;

                EvalConstraint ec = constraintMap == null ? null : constraintMap.get(constraintName);
                if (ec == null) {
                    stats.constraintsSkippedMissing++;
                    recordSample(stats.missingConstraintSamples, constraintName);
                    continue;
                }

                String sign = ec.getSign();
                double rhs = -ec.getEvalExpression().getValue().getData().doubleValue();

                if ("=".equals(sign)) {
                    ControlData.xasolver.setRowFix(constraintName, rhs);
                } else if ("<".equals(sign) || "<=".equals(sign)) {
                    ControlData.xasolver.setRowMax(constraintName, rhs);
                } else if (">".equals(sign)) {
                    ControlData.xasolver.setRowMin(constraintName, rhs);
                } else {
                    stats.constraintsSkippedUnknownSign++;
                    recordSample(stats.unknownSignSamples, constraintName + ":" + sign);
                    continue;
                }

                HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
                if (multMap != null) {
                    stats.multiplierEntriesLoaded += multMap.size();

                    Set<String> multCollection = multMap.keySet();
                    Iterator<String> multIterator = multCollection.iterator();
                    while (multIterator.hasNext()) {
                        String multName = multIterator.next();

                        if (!dvarMap.containsKey(multName)) {
                            addConditionalSlackSurplusToDvarMap(dvarMap, multName);
                            recordSample(stats.autoAddedSlackSurplusSamples, multName);
                        }

                        ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
                    }
                }

                stats.constraintsSet++;
            }
        }
    }

    // ============================================================
    // Failure mapping / diagnostics
    // ============================================================

    private void getSolverInformation_CBCStyle(RunContext rc, int modelStatus, String solverStatusStr) {
        if (ControlData.showRunTimeMessage) {
            System.out.println("Solver status: " + ControlData.xasolver.getSolverStatus());
        }

        String mapped;
        switch (modelStatus) {
            case 2:
                mapped = "Integer Solution (not proven optimal integer solution).";
                break;
            case 3:
                mapped = "Unbounded solution.";
                break;
            case 4:
                mapped = "Infeasible solution.";
                break;
            case 5:
                mapped = "Callback indicates infeasible solution.";
                break;
            case 6:
                mapped = "Intermediate infeasible solution.";
                break;
            case 7:
                mapped = "Intermediate nonoptimal solution.";
                break;
            case 9:
                mapped = "Intermediate non-integer solution.";
                break;
            case 10:
                mapped = "Integer infeasible.";
                break;
            case 13:
                mapped = "More memory required. Increase memory request in XAINIT.";
                break;
            case 32:
                mapped = "Branch-and-bound active; model not completed solving.";
                break;
            case 99:
                mapped = "Currently solving model; model not completed solving.";
                break;
            default:
                mapped = "Solving failed.";
                break;
        }

        Error.addSolvingError(mapped);

        LOG_MAIN.atError()
                .setMessage("event=failure solver=XA modelStatus={} solverStatus={} reason={} {}")
                .addArgument(modelStatus)
                .addArgument(solverStatusStr)
                .addArgument(mapped)
                .addArgument(rc.ctx())
                .addKeyValue("event", "failure")
                .addKeyValue("solver", "XA")
                .addKeyValue("modelStatus", modelStatus)
                .addKeyValue("solverStatus", solverStatusStr)
                .addKeyValue("reason", mapped)
                .addKeyValue("run_id", rc.runId)
                .log();
    }

    // ============================================================
    // assign
    // ============================================================

    public void assignDvar(RunContext rc) {
        if (ControlData.showRunTimeMessage) {
            System.out.println("XA Solver: Assigning dvars' values");
        }

        LOG_MAIN.atInfo()
                .setMessage("event=assign_start solver=XA {}")
                .addArgument(rc.ctx())
                .addKeyValue("event", "assign_start")
                .addKeyValue("solver", "XA")
                .addKeyValue("run_id", rc.runId)
                .log();

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
        Set<String> dvarCollection = dvarMap.keySet();
        Iterator<String> dvarIterator = dvarCollection.iterator();

        int assignedCount = 0;

        while (dvarIterator.hasNext()) {
            String dvName = dvarIterator.next();
            Dvar dvar = dvarMap.get(dvName);
            double value = ControlData.xasolver.getColumnActivity(dvName);
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

            assignedCount++;
        }

        double obj = safeGetObjective();
        if (ControlData.showRunTimeMessage) {
            System.out.println("Objective Value: " + obj);
            System.out.println("Assign Dvar Done.");
        }

        LOG_MAIN.atInfo()
                .setMessage("event=assign_done solver=XA assignedDvars={} objective={} {}")
                .addArgument(assignedCount)
                .addArgument(formatDouble(obj))
                .addArgument(rc.ctx())
                .addKeyValue("event", "assign_done")
                .addKeyValue("solver", "XA")
                .addKeyValue("assignedDvars", assignedCount)
                .addKeyValue("objective", formatDouble(obj))
                .addKeyValue("run_id", rc.runId)
                .log();
    }

    public void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName) {
        Dvar dvar = new Dvar();
        dvar.upperBoundValue = DEFAULT_SLACK_SURPLUS_UB;
        dvar.lowerBoundValue = 0.0;
        dvarMap.put(multName, dvar);
    }

    // ============================================================
    // Lightweight aggregated warnings for tolerant mode
    // ============================================================

    private void maybeLogBuildGaps(RunContext rc, BuildStats stats) {
        if (stats == null) return;

        if (stats.constraintsSkippedMissing > 0 || stats.constraintsSkippedUnknownSign > 0) {
            LOG_MAIN.atWarn()
                    .setMessage("event=build_gap solver=XA area=constraints missingCount={} unknownSignCount={} missingSamples={} unknownSignSamples={} {}")
                    .addArgument(stats.constraintsSkippedMissing)
                    .addArgument(stats.constraintsSkippedUnknownSign)
                    .addArgument(sampleList(stats.missingConstraintSamples))
                    .addArgument(sampleList(stats.unknownSignSamples))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "build_gap")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("area", "constraints")
                    .addKeyValue("missingCount", stats.constraintsSkippedMissing)
                    .addKeyValue("unknownSignCount", stats.constraintsSkippedUnknownSign)
                    .addKeyValue("run_id", rc.runId)
                    .log();
        }

        if (stats.dvarsSkippedMissing > 0) {
            LOG_MAIN.atWarn()
                    .setMessage("event=build_gap solver=XA area=dvars missingCount={} samples={} {}")
                    .addArgument(stats.dvarsSkippedMissing)
                    .addArgument(sampleList(stats.missingDvarSamples))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "build_gap")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("area", "dvars")
                    .addKeyValue("missingCount", stats.dvarsSkippedMissing)
                    .addKeyValue("run_id", rc.runId)
                    .log();
        }

        if (stats.weightsSkippedMissing > 0) {
            LOG_MAIN.atWarn()
                    .setMessage("event=build_gap solver=XA area=weights missingCount={} samples={} {}")
                    .addArgument(stats.weightsSkippedMissing)
                    .addArgument(sampleList(stats.missingWeightSamples))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "build_gap")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("area", "weights")
                    .addKeyValue("missingCount", stats.weightsSkippedMissing)
                    .addKeyValue("run_id", rc.runId)
                    .log();
        }

        if (stats.slackSurplusSkippedMissing > 0) {
            LOG_MAIN.atWarn()
                    .setMessage("event=build_gap solver=XA area=slack_surplus_weights missingCount={} samples={} {}")
                    .addArgument(stats.slackSurplusSkippedMissing)
                    .addArgument(sampleList(stats.missingSlackSurplusWeightSamples))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "build_gap")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("area", "slack_surplus_weights")
                    .addKeyValue("missingCount", stats.slackSurplusSkippedMissing)
                    .addKeyValue("run_id", rc.runId)
                    .log();
        }

        if (!stats.autoAddedSlackSurplusSamples.isEmpty()) {
            LOG_MAIN.atInfo()
                    .setMessage("event=auto_add_slack_surplus solver=XA sampleCount={} samples={} {}")
                    .addArgument(stats.autoAddedSlackSurplusSamples.size())
                    .addArgument(sampleList(stats.autoAddedSlackSurplusSamples))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "auto_add_slack_surplus")
                    .addKeyValue("solver", "XA")
                    .addKeyValue("sampleCount", stats.autoAddedSlackSurplusSamples.size())
                    .addKeyValue("run_id", rc.runId)
                    .log();
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static void putMdc(RunContext rc) {
        try {
            MDC.put("run_id", rc.runId);
            MDC.put("cycleName", rc.cycleName);
            MDC.put("timeStep", rc.timeStep);
            MDC.put("solver", "XA");
            MDC.put("mode", rc.modeName());
        } catch (Exception ignore) {
        }
    }

    private static void clearMdc() {
        try {
            MDC.clear();
        } catch (Exception ignore) {
        }
    }

    private static int safeSize(java.util.Collection<?> c) {
        return c == null ? 0 : c.size();
    }

    private static String safeToString(Object o) {
        return o == null ? "null" : String.valueOf(o);
    }

    private double safeGetObjective() {
        try {
            return ControlData.xasolver.getObjective();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static String formatDouble(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "+Inf" : "-Inf";
        return String.valueOf(v);
    }

    private static void recordSample(List<String> samples, String value) {
        if (samples.size() < SAMPLE_LIMIT) {
            samples.add(value);
        }
    }

    private static String sampleList(List<String> samples) {
        return samples == null || samples.isEmpty() ? "[]" : samples.toString();
    }
}