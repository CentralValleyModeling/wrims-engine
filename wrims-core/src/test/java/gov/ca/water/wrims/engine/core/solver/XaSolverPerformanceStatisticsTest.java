package gov.ca.water.wrims.engine.core.solver;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XaSolverPerformanceStatisticsTest {

    @Test
    void shouldStartWithZeroValues() {
        XaSolverPerformanceStatistics stats = new XaSolverPerformanceStatistics();

        assertEquals(0, stats.getTotalRuns());
        assertEquals(0, stats.getTotalSuccessfulAssignRuns());
        assertEquals(0L, stats.getTotalConstraintSetupMs());
        assertEquals(0L, stats.getTotalDvarSetupMs());
        assertEquals(0L, stats.getTotalWeightSetupMs());
        assertEquals(0L, stats.getTotalSolveMs());
        assertEquals(0L, stats.getTotalAssignMs());
        assertEquals(0L, stats.getTotalRunMs());
        assertEquals(0L, stats.getAverageSolveMs());
        assertEquals(0L, stats.getAverageRunMs());
    }

    @Test
    void shouldRecordSingleRunCorrectly() {
        XaSolverPerformanceStatistics stats = new XaSolverPerformanceStatistics();

        stats.recordRun(10L, 20L, 30L, 40L, 50L, 150L, true);

        assertEquals(1, stats.getTotalRuns());
        assertEquals(1, stats.getTotalSuccessfulAssignRuns());
        assertEquals(10L, stats.getTotalConstraintSetupMs());
        assertEquals(20L, stats.getTotalDvarSetupMs());
        assertEquals(30L, stats.getTotalWeightSetupMs());
        assertEquals(40L, stats.getTotalSolveMs());
        assertEquals(50L, stats.getTotalAssignMs());
        assertEquals(150L, stats.getTotalRunMs());
        assertEquals(40L, stats.getAverageSolveMs());
        assertEquals(150L, stats.getAverageRunMs());
    }

    @Test
    void shouldAccumulateMultipleRunsCorrectly() {
        XaSolverPerformanceStatistics stats = new XaSolverPerformanceStatistics();

        stats.recordRun(10L, 20L, 30L, 40L, 50L, 150L, true);
        stats.recordRun(5L, 15L, 25L, 35L, 45L, 125L, false);

        assertEquals(2, stats.getTotalRuns());
        assertEquals(1, stats.getTotalSuccessfulAssignRuns());
        assertEquals(15L, stats.getTotalConstraintSetupMs());
        assertEquals(35L, stats.getTotalDvarSetupMs());
        assertEquals(55L, stats.getTotalWeightSetupMs());
        assertEquals(75L, stats.getTotalSolveMs());
        assertEquals(95L, stats.getTotalAssignMs());
        assertEquals(275L, stats.getTotalRunMs());
        assertEquals(37L, stats.getAverageSolveMs());
        assertEquals(137L, stats.getAverageRunMs());
    }

    @Test
    void shouldNotIncreaseSuccessfulAssignRunsWhenAssignDoneIsFalse() {
        XaSolverPerformanceStatistics stats = new XaSolverPerformanceStatistics();

        stats.recordRun(1L, 1L, 1L, 1L, 1L, 5L, false);

        assertEquals(1, stats.getTotalRuns());
        assertEquals(0, stats.getTotalSuccessfulAssignRuns());
    }
}