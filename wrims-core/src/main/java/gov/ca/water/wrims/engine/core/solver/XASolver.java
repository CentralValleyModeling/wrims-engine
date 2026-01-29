package gov.ca.water.wrims.engine.core.solver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.commondata.solverdata.*;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;

public class XASolver {

    private static final Logger LOG_MAIN   = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.xa.main");
    private static final Logger LOG_PERF   = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.xa.perf");
    private static final Logger LOG_DETAIL = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.xa.detail");
    private static final Logger LOG_ERROR  = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.xa.error");

    @SuppressWarnings("unused")

    private static final Logger logger = LoggerFactory.getLogger(XASolver.class);
    // ============================================================
    // Mode switch (default legacy)
    // ============================================================
    private enum Mode { LEGACY, TOLERANT }

    private static Mode readMode() {
        // Priority:
        // 1) -Dwrims.xa.mode=tolerant|legacy
        // 2) -Dwrims.xa.tolerant=true|false
        // Default: LEGACY
        String mode = System.getProperty("wrims.xa.mode");
        if (mode != null) {
            String m = mode.trim().toLowerCase();
            if ("tolerant".equals(m)) return Mode.TOLERANT;
            if ("legacy".equals(m))   return Mode.LEGACY;
        }
        String tol = System.getProperty("wrims.xa.tolerant");
        if (tol != null) {
            String t = tol.trim().toLowerCase();
            if ("true".equals(t) || "1".equals(t) || "yes".equals(t)) return Mode.TOLERANT;
        }
        return Mode.LEGACY;
    }

    // ============================================================
    // Context
    // ============================================================
    private static final class RunContext {
        final Mode mode;
        final int cycleIndex1Based;
        final String dateStr;
        final String cycleName;
        final String timeStep;
        final String runId;

        RunContext(Mode mode) {
            this.mode = mode;
            this.cycleIndex1Based = gov.ca.water.wrims.engine.core.components.ControlData.currCycleIndex + 1;
            this.dateStr = gov.ca.water.wrims.engine.core.components.ControlData.currMonth + "/" + gov.ca.water.wrims.engine.core.components.ControlData.currDay + "/" + gov.ca.water.wrims.engine.core.components.ControlData.currYear;
            this.cycleName = gov.ca.water.wrims.engine.core.components.ControlData.currCycleName;
            this.timeStep = String.valueOf(gov.ca.water.wrims.engine.core.components.ControlData.timeStep);
            this.runId = buildRunId();
        }

        String ctx() {
            return "date=" + dateStr +
                    " cycleIndex=" + cycleIndex1Based +
                    " cycleName=" + cycleName +
                    " timeStep=" + timeStep +
                    " run_id=" + runId;
        }

        String modeName() {
            return (mode == null) ? "legacy" : mode.name().toLowerCase();
        }

        private static String buildRunId() {
            // stable within a single solver call, good for joining main/perf/detail
            return UUID.randomUUID().toString();
        }
    }

    private static void putMdc(RunContext rc) {
        // Not every layout prints MDC, but when it does, this becomes very powerful.
        try {
            MDC.put("run_id", rc.runId);
            MDC.put("cycleName", rc.cycleName);
            MDC.put("timeStep", rc.timeStep);
            MDC.put("solver", "XA");
            MDC.put("mode", rc.modeName());
        } catch (Exception ignore) {}
    }

    private static void clearMdc() {
        try { MDC.clear(); } catch (Exception ignore) {}
    }

    // ============================================================
    // Perf timer
    // ============================================================
    private static class PerfTimer {
        private final String step;
        private final long startMs;
        private final RunContext rc;

        PerfTimer(String step, RunContext rc) {
            this.step = step;
            this.rc = rc;
            this.startMs = System.currentTimeMillis();
            if (LOG_PERF.isDebugEnabled()) {
                LOG_PERF.atDebug()
                        .setMessage("event=step_start solver=XA step={} mode={} {}")
                        .addArgument(step)
                        .addArgument(rc.modeName())
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "step_start")
                        .addKeyValue("step", step)
                        .addKeyValue("mode", rc.modeName())
                        .log();
            }
        }

        long stop() {
            long ms = System.currentTimeMillis() - startMs;
            if (LOG_PERF.isDebugEnabled()) {
                LOG_PERF.atDebug()
                        .setMessage("event=step_end solver=XA step={} elapsed_ms={} mode={} {}")
                        .addArgument(step)
                        .addArgument(ms)
                        .addArgument(rc.modeName())
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "step_end")
                        .addKeyValue("step", step)
                        .addKeyValue("elapsed_ms", ms)
                        .addKeyValue("mode", rc.modeName())
                        .log();
            }
            return ms;
        }
    }

    // ============================================================
    // Tolerant build stats (only used in tolerant mode)
    // ============================================================
    private static class BuildStats {
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
    }

    int modelStatus;

    public XASolver() {
        final long totalStart = System.currentTimeMillis();
        final Mode mode = readMode();
        final RunContext rc = new RunContext(mode);

        putMdc(rc);

        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) {
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

        // Perf buckets
        long msLoadModel = 0;
        long msSetConstraints = 0;
        long msSetDvars = 0;
        long msSetWeights = 0;
        long msSolve = 0;
        long msAssign = 0;

        String solverStatusStr = null;
        double objective = Double.NaN;

        // Only used in tolerant mode
        BuildStats stats = (mode == Mode.TOLERANT) ? new BuildStats() : null;

        boolean assignAttempted = false;
        boolean assignSucceeded = false;
        String assignSkipReason = null;

        try {
            // 1) load model
            PerfTimer t1 = new PerfTimer("loadModel", rc);
            gov.ca.water.wrims.engine.core.components.ControlData.xasolver.loadNewModel();
            msLoadModel = t1.stop();

            // 2) build (constraints/dvars/weights) - dual mode
            PerfTimer t2 = new PerfTimer("setConstraints", rc);
            if (mode == Mode.TOLERANT) setConstraints_Tolerant(rc, stats);
            else setConstraints_Legacy(rc);
            msSetConstraints = t2.stop();

            // 3) dvars (tolerant)
            PerfTimer t3 = new PerfTimer("setDVars", rc);
            if (mode == Mode.TOLERANT) {
                setDVars_Tolerant(rc, stats);
            } else {
                setDVars_Legacy(rc);
            }
            msSetDvars = t3.stop();

            // 4) weights (tolerant)
            PerfTimer t4 = new PerfTimer("setWeights", rc);
            if (mode == Mode.TOLERANT) {
                setWeights_Tolerant(rc, stats);
            } else {
                setWeights_Legacy(rc);
            }
            msSetWeights = t4.stop();

            // detail stats (tolerant only)
            if (mode == Mode.TOLERANT && LOG_DETAIL.isDebugEnabled()) {
                LOG_DETAIL.atDebug()
                        .setMessage(
                                "event=build_stats solver=XA mode=tolerant {} " +
                                        "constraintsSeen={} constraintsSet={} constraintsSkippedMissing={} constraintsSkippedUnknownSign={} " +
                                        "dvarsSeen={} dvarsSet={} dvarsSkippedMissing={} " +
                                        "weightsSeen={} weightsSet={} weightsSkippedMissing={} " +
                                        "slackSurplusSeen={} slackSurplusSet={} slackSurplusSkippedMissing={} " +
                                        "multiplierEntriesLoaded={} " +
                                        "dvListCount={} dvTimeArrayCount={} wtListCount={} wtTimeArrayCount={} usedSlackSurplusCount={} gListCount={} gTimeArrayCount={}"
                        )
                        .addArgument(rc.ctx())
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
                        .addArgument(stats.multiplierEntriesLoaded)
                        .addArgument(stats.dvListCount)
                        .addArgument(stats.dvTimeArrayCount)
                        .addArgument(stats.wtListCount)
                        .addArgument(stats.wtTimeArrayCount)
                        .addArgument(stats.usedSlackSurplusCount)
                        .addArgument(stats.gListCount)
                        .addArgument(stats.gTimeArrayCount)
                        .addKeyValue("event", "build_stats")
                        .addKeyValue("mode", "tolerant")
                        .addKeyValue("constraintsSeen", stats.constraintsSeen)
                        .addKeyValue("dvarsSeen", stats.dvarsSeen)
                        .addKeyValue("weightsSeen", stats.weightsSeen)
                        .log();
            }

            // 5) solve
            if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) {
                System.out.println("XA Solver: Calling solveWithInfeasibleAnalysis ...");
            }

            PerfTimer t5 = new PerfTimer("solve", rc);
            gov.ca.water.wrims.engine.core.components.ControlData.xasolver.solveWithInfeasibleAnalysis("Output console:");
            msSolve = t5.stop();

            modelStatus = gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getModelStatus();
            solverStatusStr = safeToString(gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getSolverStatus());
            objective = safeGetObjective();

            LOG_MAIN.atInfo()
                    .setMessage(
                            "event=solve_done solver=XA mode={} " +
                                    "modelStatus={} solverStatus={} solve_ms={} objective={} {}"
                    )
                    .addArgument(rc.modeName())
                    .addArgument(modelStatus)
                    .addArgument(solverStatusStr)
                    .addArgument(msSolve)
                    .addArgument(formatDouble(objective))
                    .addArgument(rc.ctx())
                    .addKeyValue("event", "solve_done")
                    .addKeyValue("mode", rc.modeName())
                    .addKeyValue("modelStatus", modelStatus)
                    .addKeyValue("solverStatus", solverStatusStr)
                    .addKeyValue("solve_ms", msSolve)
                    .addKeyValue("objective", formatDouble(objective))
                    .log();

            // failure mapping
            if (modelStatus >= 2) {
                getSolverInformation_CBCStyle(rc, modelStatus, solverStatusStr);
                diagnosticsOnFailure(rc, modelStatus, solverStatusStr);
            }

            // 6) assign
            assignAttempted = true;
            if (mode == Mode.TOLERANT) {
                // safer in tolerant mode (as per updated doc)
                if (gov.ca.water.wrims.engine.core.components.Error.error_solving.size() < 1 && modelStatus < 2) {
                    PerfTimer t6 = new PerfTimer("assign", rc);
                    assignDvar(rc);
                    msAssign = t6.stop();
                    assignSucceeded = true;
                } else {
                    assignSucceeded = false;
                    assignSkipReason = (gov.ca.water.wrims.engine.core.components.Error.error_solving.size() >= 1)
                            ? "solvingErrors=" + gov.ca.water.wrims.engine.core.components.Error.error_solving.size()
                            : "modelStatus=" + modelStatus;
                    LOG_MAIN.atWarn()
                            .setMessage("event=assign_skip solver=XA mode=tolerant reason={} {}")
                            .addArgument(assignSkipReason)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "assign_skip")
                            .addKeyValue("mode", "tolerant")
                            .addKeyValue("reason", assignSkipReason)
                            .log();
                }
            } else {
                // legacy behavior: only check Error list (keep semantics)
                if (gov.ca.water.wrims.engine.core.components.Error.error_solving.size() < 1) {
                    PerfTimer t6 = new PerfTimer("assign", rc);
                    assignDvar(rc);
                    msAssign = t6.stop();
                    assignSucceeded = true;
                } else {
                    assignSucceeded = false;
                    assignSkipReason = "solvingErrors=" + gov.ca.water.wrims.engine.core.components.Error.error_solving.size();
                    LOG_MAIN.atWarn()
                            .setMessage("event=assign_skip solver=XA mode=legacy reason={} {}")
                            .addArgument(assignSkipReason)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "assign_skip")
                            .addKeyValue("mode", "legacy")
                            .addKeyValue("reason", assignSkipReason)
                            .log();
                }
            }

        } catch (Exception e) {
            LOG_ERROR.atError()
                    .setMessage("event=exception solver=XA mode={} message={} {}")
                    .addArgument(rc.modeName())
                    .addArgument(e.getMessage())
                    .addArgument(rc.ctx())
                    .setCause(e)
                    .addKeyValue("event", "exception")
                    .addKeyValue("mode", rc.modeName())
                    .addKeyValue("run_id", rc.runId)
                    .log();
            try { gov.ca.water.wrims.engine.core.components.Error.addSolvingError("Exception during XA solve: " + e.getMessage()); } catch (Exception ignore) {}
            try { modelStatus = gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getModelStatus(); } catch (Exception ignore) { modelStatus = 99; }

        } finally {
            final long totalMs = System.currentTimeMillis() - totalStart;

            if (Double.isNaN(objective)) objective = safeGetObjective();
            if (solverStatusStr == null) {
                try {
                    solverStatusStr = safeToString(gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getSolverStatus());
                } catch (Exception ignore) {
                }
            }

            // perf summary (debug)
            if (LOG_PERF.isDebugEnabled()) {
                LOG_PERF.atDebug()
                        .setMessage(
                                "event=perf_summary solver=XA mode={} " +
                                        "total_ms={} load_ms={} constraints_ms={} dvars_ms={} weights_ms={} solve_ms={} assign_ms={} {}"
                        )
                        .addArgument(rc.modeName())
                        .addArgument(totalMs)
                        .addArgument(msLoadModel)
                        .addArgument(msSetConstraints)
                        .addArgument(msSetDvars)
                        .addArgument(msSetWeights)
                        .addArgument(msSolve)
                        .addArgument(msAssign)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "perf_summary")
                        .addKeyValue("mode", rc.modeName())
                        .addKeyValue("total_ms", totalMs)
                        .addKeyValue("load_ms", msLoadModel)
                        .addKeyValue("constraints_ms", msSetConstraints)
                        .addKeyValue("dvars_ms", msSetDvars)
                        .addKeyValue("weights_ms", msSetWeights)
                        .addKeyValue("solve_ms", msSolve)
                        .addKeyValue("assign_ms", msAssign)
                        .log();
            }

            String assignInfo;
            if (!assignAttempted) assignInfo = "assign=not_attempted";
            else if (assignSucceeded) assignInfo = "assign=done";
            else assignInfo = "assign=skipped reason=" + (assignSkipReason == null ? "unknown" : assignSkipReason);

            if (mode == Mode.TOLERANT && stats != null) {
                LOG_MAIN.atInfo()
                        .setMessage(
                                "event=summary solver=XA mode=tolerant " +
                                        "total_ms={} modelStatus={} solverStatus={} objective={} solvingErrors={} " +
                                        "constraintsSeen={} constraintsSet={} constraintsSkippedMissing={} constraintsSkippedUnknownSign={} " +
                                        "dvarsSeen={} dvarsSet={} dvarsSkippedMissing={} " +
                                        "weightsSeen={} weightsSet={} weightsSkippedMissing={} " +
                                        "slackSurplusSeen={} slackSurplusSet={} slackSurplusSkippedMissing={} " +
                                        "{} {}"
                        )
                        .addArgument(totalMs)
                        .addArgument(modelStatus)
                        .addArgument(solverStatusStr)
                        .addArgument(formatDouble(objective))
                        .addArgument(gov.ca.water.wrims.engine.core.components.Error.error_solving.size())
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
                        .addArgument(assignInfo)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "summary")
                        .addKeyValue("mode", "tolerant")
                        .addKeyValue("total_ms", totalMs)
                        .addKeyValue("modelStatus", modelStatus)
                        .addKeyValue("solverStatus", solverStatusStr)
                        .addKeyValue("objective", formatDouble(objective))
                        .addKeyValue("solvingErrors", gov.ca.water.wrims.engine.core.components.Error.error_solving.size())
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
                        .addKeyValue("assignInfo", assignInfo)
                        .log();
            } else {
                LOG_MAIN.atInfo()
                        .setMessage(
                                "event=summary solver=XA mode=legacy " +
                                        "total_ms={} modelStatus={} solverStatus={} objective={} solvingErrors={} {} {}"
                        )
                        .addArgument(totalMs)
                        .addArgument(modelStatus)
                        .addArgument(solverStatusStr)
                        .addArgument(formatDouble(objective))
                        .addArgument(gov.ca.water.wrims.engine.core.components.Error.error_solving.size())
                        .addArgument(assignInfo)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "summary")
                        .addKeyValue("mode", "legacy")
                        .addKeyValue("total_ms", totalMs)
                        .addKeyValue("modelStatus", modelStatus)
                        .addKeyValue("solverStatus", solverStatusStr)
                        .addKeyValue("objective", formatDouble(objective))
                        .addKeyValue("solvingErrors", gov.ca.water.wrims.engine.core.components.Error.error_solving.size())
                        .addKeyValue("assignInfo", assignInfo)
                        .log();
            }

            // keep ControlData.t_xa behavior
            long t2 = Calendar.getInstance().getTimeInMillis();
            ControlData.t_xa = ControlData.t_xa + (int) (t2 - totalStart);

            clearMdc();

        }
    }

    // ============================================================
    // LEGACY implementations (behavior compatibility)
    // ============================================================
    private void setDVars_Legacy(RunContext rc) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting dvars");
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> dvarCollection;
            if (i == 0) {
                dvarCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvList;
            } else {
                dvarCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvTimeArrayList;
            }

            Iterator<String> it = dvarCollection.iterator();
            while (it.hasNext()) {
                String dvarName = it.next();
                Dvar dvar = dvarMap.get(dvarName);

                // legacy behavior: may throw if missing
                double lb = dvar.lowerBoundValue.doubleValue();
                double ub = dvar.upperBoundValue.doubleValue();

                if ("y".equals(dvar.integer)) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnInteger(dvarName, lb, ub);
                } else {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);
                }
            }
        }
    }

    private void setWeights_Legacy(RunContext rc) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting weights");

        Map<String, WeightElement> weightMap = SolverData.getWeightMap();
        for (int i = 0; i <= 1; i++) {
            ArrayList<String> weightCollection;
            if (i == 0) {
                weightCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.wtList;
            } else {
                weightCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.wtTimeArrayList;
            }

            Iterator<String> it = weightCollection.iterator();
            while (it.hasNext()) {
                String weightName = it.next();
                // legacy behavior: may throw if missing
                gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnObjective(weightName, weightMap.get(weightName).getValue());
            }
        }

        Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
        CopyOnWriteArrayList<String> used = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.usedWtSlackSurplusList;
        Iterator<String> it2 = used.iterator();
        while (it2.hasNext()) {
            String name = it2.next();
            // legacy behavior: may throw if missing
            gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnObjective(name, weightSlackSurplusMap.get(name).getValue());
        }
    }

    private void setConstraints_Legacy(RunContext rc) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting constraints");

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> constraintCollection;
            if (i == 0) {
                constraintCollection = new ArrayList<String>(gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.gList);
                // legacy critical behavior: silent drop missing constraints
                constraintCollection.retainAll(constraintMap.keySet());
            } else {
                constraintCollection = new ArrayList<String>(gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.gTimeArrayList);
            }

            Iterator<String> it = constraintCollection.iterator();
            while (it.hasNext()) {
                String constraintName = it.next();
                EvalConstraint ec = constraintMap.get(constraintName);

                // legacy behavior: assumes ec != null
                if (ec.getSign().equals("=")) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                } else if (ec.getSign().equals("<") || ec.getSign().equals("<=")) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                } else if (ec.getSign().equals(">")) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowMin(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
                }

                HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
                Set multCollection = multMap.keySet();
                Iterator multIterator = multCollection.iterator();

                while (multIterator.hasNext()) {
                    String multName = (String) multIterator.next();
                    if (!dvarMap.containsKey(multName)) addConditionalSlackSurplusToDvarMap(dvarMap, multName);
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
                }
            }
        }
    }

    // ============================================================
    // TOLERANT implementations
    // ============================================================
    private void setDVars_Tolerant(RunContext rc, BuildStats stats) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting dvars");

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();
        for (int i = 0; i <= 1; i++) {
            ArrayList<String> dvarCollection;
            String listName;
            if (i == 0) {
                dvarCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvList;
                listName = "dvList";
                stats.dvListCount = safeSize(dvarCollection);
            } else {
                dvarCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvTimeArrayList;
                listName = "dvTimeArrayList";
                stats.dvTimeArrayCount = safeSize(dvarCollection);
            }

            Iterator<String> it = dvarCollection.iterator();
            while (it.hasNext()) {
                String dvarName = it.next();
                stats.dvarsSeen++;

                Dvar dvar = (dvarMap == null) ? null : dvarMap.get(dvarName);
                if (dvar == null) {
                    stats.dvarsSkippedMissing++;
                    LOG_MAIN.atWarn()
                            .setMessage("event=missing_dvar solver=XA mode=tolerant list={} name={} action=skip_set {}")
                            .addArgument(listName)
                            .addArgument(dvarName)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "missing_dvar")
                            .addKeyValue("list", listName)
                            .addKeyValue("name", dvarName)
                            .addKeyValue("action", "skip_set")
                            .log();
                    continue;
                }

                double lb = dvar.lowerBoundValue.doubleValue();
                double ub = dvar.upperBoundValue.doubleValue();

                if ("y".equals(dvar.integer)) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnInteger(dvarName, lb, ub);
                } else {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);
                }
                stats.dvarsSet++;
            }
        }
    }
    // ============================================================
    // Tolerant: setWeights
    // ============================================================
    private void setWeights_Tolerant(RunContext rc, BuildStats stats) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting weights");

        Map<String, WeightElement> weightMap = SolverData.getWeightMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> weightCollection;
            String listName;
            if (i == 0) {
                weightCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.wtList;
                listName = "wtList";
                stats.wtListCount = safeSize(weightCollection);
            } else {
                weightCollection = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.wtTimeArrayList;
                listName = "wtTimeArrayList";
                stats.wtTimeArrayCount = safeSize(weightCollection);
            }

            Iterator<String> it = weightCollection.iterator();
            while (it.hasNext()) {
                String weightName = it.next();
                stats.weightsSeen++;

                WeightElement we = (weightMap == null) ? null : weightMap.get(weightName);
                if (we == null) {
                    stats.weightsSkippedMissing++;
                    LOG_MAIN.atWarn()
                            .setMessage("event=missing_weight solver=XA mode=tolerant list={} name={} action=skip_set {}")
                            .addArgument(listName)
                            .addArgument(weightName)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "missing_weight")
                            .addKeyValue("list", listName)
                            .addKeyValue("name", weightName)
                            .addKeyValue("action", "skip_set")
                            .log();
                    continue;
                }

                gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnObjective(weightName, we.getValue());
                stats.weightsSet++;
            }
        }

        Map<String, WeightElement> slackMap = SolverData.getWeightSlackSurplusMap();
        CopyOnWriteArrayList<String> usedSlack = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.usedWtSlackSurplusList;
        stats.usedSlackSurplusCount = safeSize(usedSlack);

        Iterator<String> it2 = usedSlack.iterator();
        while (it2.hasNext()) {
            String name = it2.next();
            stats.slackSurplusSeen++;

            WeightElement we = (slackMap == null) ? null : slackMap.get(name);
            if (we == null) {
                stats.slackSurplusSkippedMissing++;
                LOG_MAIN.atWarn()
                        .setMessage("event=missing_slack_surplus_weight solver=XA mode=tolerant name={} action=skip_set {}")
                        .addArgument(name)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "missing_slack_surplus_weight")
                        .addKeyValue("name", name)
                        .addKeyValue("action", "skip_set")
                        .log();
                continue;
            }
            gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setColumnObjective(name, we.getValue());
            stats.slackSurplusSet++;
        }
    }
    // ============================================================
    // Tolerant: setConstraints
    // ============================================================
    private void setConstraints_Tolerant(RunContext rc, BuildStats stats) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting constraints");

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        Map<String, Dvar> dvarMap = SolverData.getDvarMap();

        for (int i = 0; i <= 1; i++) {
            ArrayList<String> constraintCollection;
            String listName;

            if (i == 0) {
                constraintCollection = new ArrayList<String>(gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.gList);
                listName = "gList";
                stats.gListCount = safeSize(constraintCollection);
            } else {
                constraintCollection = new ArrayList<String>(gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.gTimeArrayList);
                listName = "gTimeArrayList";
                stats.gTimeArrayCount = safeSize(constraintCollection);
            }

            Iterator<String> it = constraintCollection.iterator();
            while (it.hasNext()) {
                String constraintName = it.next();
                stats.constraintsSeen++;

                EvalConstraint ec = (constraintMap == null) ? null : constraintMap.get(constraintName);
                if (ec == null) {
                    stats.constraintsSkippedMissing++;
                    LOG_MAIN.atWarn()
                            .setMessage("event=missing_constraint solver=XA mode=tolerant list={} name={} action=skip_set {}")
                            .addArgument(listName)
                            .addArgument(constraintName)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "missing_constraint")
                            .addKeyValue("list", listName)
                            .addKeyValue("name", constraintName)
                            .addKeyValue("action", "skip_set")
                            .log();
                    continue;
                }

                String sign = ec.getSign();
                if (!"=".equals(sign) && !"<".equals(sign) && !"<=".equals(sign) && !">".equals(sign)) {
                    stats.constraintsSkippedUnknownSign++;
                    LOG_MAIN.atWarn()
                            .setMessage("event=unknown_constraint_sign solver=XA mode=tolerant list={} name={} sign={} action=skip_set {}")
                            .addArgument(listName)
                            .addArgument(constraintName)
                            .addArgument(sign)
                            .addArgument(rc.ctx())
                            .addKeyValue("event", "unknown_constraint_sign")
                            .addKeyValue("list", listName)
                            .addKeyValue("name", constraintName)
                            .addKeyValue("sign", sign)
                            .addKeyValue("action", "skip_set")
                            .log();
                    continue;
                }

                double rhs = -ec.getEvalExpression().getValue().getData().doubleValue();
                if ("=".equals(sign)) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowFix(constraintName, rhs);
                } else if ("<".equals(sign) || "<=".equals(sign)) {
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowMax(constraintName, rhs);
                } else { // ">"
                    gov.ca.water.wrims.engine.core.components.ControlData.xasolver.setRowMin(constraintName, rhs);
                }

                HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
                if (multMap != null) {
                    stats.multiplierEntriesLoaded += multMap.size();
                    Set keys = multMap.keySet();
                    Iterator mit = keys.iterator();
                    while (mit.hasNext()) {
                        String multName = (String) mit.next();
                        if (!dvarMap.containsKey(multName)) {
                            //slack/surplus
                            addConditionalSlackSurplusToDvarMap(dvarMap, multName);
                            if (LOG_DETAIL.isDebugEnabled()) {
                                LOG_DETAIL.atDebug()
                                        .setMessage("event=auto_add_slack_surplus_dvar solver=XA mode=tolerant multName={} {}")
                                        .addArgument(multName)
                                        .addArgument(rc.ctx())
                                        .addKeyValue("event", "auto_add_slack_surplus_dvar")
                                        .addKeyValue("multName", multName)
                                        .log();
                            }
                        }
                        gov.ca.water.wrims.engine.core.components.ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
                    }
                }

                stats.constraintsSet++;
            }
        }
    }

    // ============================================================
    // failure mapping / diagnostics
    // ============================================================
    private void getSolverInformation_CBCStyle(RunContext rc, int modelStatus, String solverStatusStr) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) {
            System.out.println("Solver status: " + gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getSolverStatus());
        }

        LOG_MAIN.atError()
                .setMessage("event=failure solver=XA modelStatus={} solverStatus={} {}")
                .addArgument(modelStatus)
                .addArgument(solverStatusStr)
                .addArgument(rc.ctx())
                .addKeyValue("event", "failure")
                .addKeyValue("modelStatus", modelStatus)
                .addKeyValue("solverStatus", solverStatusStr)
                .log();

        LOG_ERROR.atError()
                .setMessage("event=failure solver=XA modelStatus={} solverStatus={} {}")
                .addArgument(modelStatus)
                .addArgument(solverStatusStr)
                .addArgument(rc.ctx())
                .addKeyValue("event", "failure")
                .addKeyValue("modelStatus", modelStatus)
                .addKeyValue("solverStatus", solverStatusStr)
                .log();

        String mapped;
        switch (modelStatus) {
            case 2:  mapped = "Integer Solution (not proven optimal integer solution)."; break;
            case 3:  mapped = "Unbounded solution."; break;
            case 4:  mapped = "Infeasible solution."; break;
            case 5:  mapped = "Callback indicates infeasible solution."; break;
            case 6:  mapped = "Intermediate infeasible solution."; break;
            case 7:  mapped = "Intermediate nonoptimal solution."; break;
            case 9:  mapped = "Intermediate non-integer solution."; break;
            case 10: mapped = "Integer infeasible."; break;
            case 13: mapped = "More memory required. Increase memory request in XAINIT."; break;
            case 32: mapped = "Branch-and-bound active; model not completed solving."; break;
            case 99: mapped = "Currently solving model; model not completed solving."; break;
            default: mapped = "Solving failed."; break;
        }

        gov.ca.water.wrims.engine.core.components.Error.addSolvingError(mapped);

        LOG_MAIN.atError()
                .setMessage("event=failure_reason solver=XA reason={} {}")
                .addArgument(mapped)
                .addArgument(rc.ctx())
                .addKeyValue("event", "failure_reason")
                .addKeyValue("reason", mapped)
                .log();

        LOG_ERROR.atError()
                .setMessage("event=failure_reason solver=XA reason={} {}")
                .addArgument(mapped)
                .addArgument(rc.ctx())
                .addKeyValue("event", "failure_reason")
                .addKeyValue("reason", mapped)
                .log();
    }
    private void diagnosticsOnFailure(RunContext rc, int modelStatus, String solverStatusStr) {
        try {
            if (LOG_DETAIL.isDebugEnabled()) {
                LOG_DETAIL.atDebug()
                        .setMessage("event=diag_failure solver=XA modelStatus={} solverStatus={} {}")
                        .addArgument(modelStatus)
                        .addArgument(solverStatusStr)
                        .addArgument(rc.ctx())
                        .addKeyValue("event", "diag_failure")
                        .addKeyValue("modelStatus", modelStatus)
                        .addKeyValue("solverStatus", solverStatusStr)
                        .log();
            }
        } catch (Exception ignore) {}
    }
    // ============================================================
    // assign:
    // ============================================================
    public void assignDvar(RunContext rc) {
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage)
            System.out.println("XA Solver: Assigning dvars' values");
        LOG_MAIN.atInfo()
                .setMessage("event=assign_start solver=XA {}")
                .addArgument(rc.ctx())
                .addKeyValue("event", "assign_start")
                .log();

        Map<String, Map<String, IntDouble>> varCycleValueMap = gov.ca.water.wrims.engine.core.components.ControlData.currStudyDataSet.getVarCycleValueMap();
        Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap = gov.ca.water.wrims.engine.core.components.ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
        Set<String> dvarUsedByLaterCycle = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvarUsedByLaterCycle;
        Set<String> dvarTimeArrayUsedByLaterCycle = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.dvarTimeArrayUsedByLaterCycle;
        ArrayList<String> timeArrayDvList = gov.ca.water.wrims.engine.core.components.ControlData.currModelDataSet.timeArrayDvList;
        String model = gov.ca.water.wrims.engine.core.components.ControlData.currCycleName;

        StudyDataSet sds = gov.ca.water.wrims.engine.core.components.ControlData.currStudyDataSet;
        ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
        ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
        Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();

        Map<String, Dvar> dvarMap = SolverData.getDvarMap();
        Set keys = dvarMap.keySet();
        Iterator it = keys.iterator();

        int assignedCount = 0;

        while (it.hasNext()) {
            String dvName = (String) it.next();
            Dvar dvar = dvarMap.get(dvName);
            double value = gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getColumnActivity(dvName);

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

            String entryNameTS = DssOperation.entryNameTS(dvName, gov.ca.water.wrims.engine.core.components.ControlData.timeStep);
            DataTimeSeries.saveDataToTimeSeries(dvName, entryNameTS, value, dvar);

            if (timeArrayDvList.contains(dvName)) {
                entryNameTS = DssOperation.entryNameTS(dvName + "__fut__0", gov.ca.water.wrims.engine.core.components.ControlData.timeStep);
                DataTimeSeries.saveDataToTimeSeries(entryNameTS, value, dvar, 0);
            }

            assignedCount++;
        }

        double obj = safeGetObjective();
        if (gov.ca.water.wrims.engine.core.components.ControlData.showRunTimeMessage) {
            System.out.println("Objective Value: " + obj);
            System.out.println("Assign Dvar Done.");
        }

        LOG_MAIN.atInfo()
                .setMessage("event=assign_done solver=XA assignedDvars={} objective={} {}")
                .addArgument(assignedCount)
                .addArgument(formatDouble(obj))
                .addArgument(rc.ctx())
                .addKeyValue("event", "assign_done")
                .addKeyValue("assignedDvars", assignedCount)
                .addKeyValue("objective", formatDouble(obj))
                .log();
    }

        public void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName) {
        Dvar dvar = new Dvar();
        dvar.upperBoundValue = 1.0e23;
        dvar.lowerBoundValue = 0.0;
        dvarMap.put(multName, dvar);
    }

    // ============================================================
    // Helpers
    // ============================================================
    private static int safeSize(java.util.Collection<?> c) {
        return (c == null) ? 0 : c.size();
    }

    private static String safeToString(Object o) {
        return (o == null) ? "null" : String.valueOf(o);
    }

    private double safeGetObjective() {
        try {
            return gov.ca.water.wrims.engine.core.components.ControlData.xasolver.getObjective();
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private static String formatDouble(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "+Inf" : "-Inf";
        return String.valueOf(v);
    }

}
