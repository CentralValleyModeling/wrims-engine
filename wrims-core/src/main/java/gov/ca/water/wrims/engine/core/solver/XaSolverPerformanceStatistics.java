package gov.ca.water.wrims.engine.core.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XaSolverPerformanceStatistics {
    private static final Logger LOG = LoggerFactory.getLogger(XaSolverPerformanceStatistics.class);

    private long totalConstraintSetupMs = 0L;
    private long totalDvarSetupMs = 0L;
    private long totalWeightSetupMs = 0L;
    private long totalSolveMs = 0L;
    private long totalAssignMs = 0L;
    private long totalRunMs = 0L;
    private int totalRuns = 0;
    private int totalSuccessfulAssignRuns = 0;

    public void recordRun(long constraintMs, long dvarMs, long weightMs, long solveMs, long assignMs, long totalMs, boolean assignDone) {
        totalConstraintSetupMs += constraintMs;
        totalDvarSetupMs += dvarMs;
        totalWeightSetupMs += weightMs;
        totalSolveMs += solveMs;
        totalAssignMs += assignMs;
        totalRunMs += totalMs;
        totalRuns++;
        if (assignDone) {
            totalSuccessfulAssignRuns++;
        }
    }

    public int getTotalRuns() {
        return totalRuns;
    }

    public int getTotalSuccessfulAssignRuns() {
        return totalSuccessfulAssignRuns;
    }

    public long getTotalConstraintSetupMs() {
        return totalConstraintSetupMs;
    }

    public long getTotalDvarSetupMs() {
        return totalDvarSetupMs;
    }

    public long getTotalWeightSetupMs() {
        return totalWeightSetupMs;
    }

    public long getTotalSolveMs() {
        return totalSolveMs;
    }

    public long getTotalAssignMs() {
        return totalAssignMs;
    }

    public long getTotalRunMs() {
        return totalRunMs;
    }

    public long getAverageSolveMs() {
        return totalRuns > 0 ? totalSolveMs / totalRuns : 0L;
    }

    public long getAverageRunMs() {
        return totalRuns > 0 ? totalRunMs / totalRuns : 0L;
    }

    public void logSummary() {
        if (totalRuns > 0) {
            LOG.atDebug()
                    .setMessage("XA performance summary: runs={} successfulAssignRuns={} totalRunMs={} avgRunMs={} totalSolveMs={} avgSolveMs={} constraintMs={} dvarMs={} weightMs={} assignMs={}")
                    .addArgument(totalRuns)
                    .addArgument(totalSuccessfulAssignRuns)
                    .addArgument(totalRunMs)
                    .addArgument(getAverageRunMs())
                    .addArgument(totalSolveMs)
                    .addArgument(getAverageSolveMs())
                    .addArgument(totalConstraintSetupMs)
                    .addArgument(totalDvarSetupMs)
                    .addArgument(totalWeightSetupMs)
                    .addArgument(totalAssignMs)
                    .log();
        }
    }
}