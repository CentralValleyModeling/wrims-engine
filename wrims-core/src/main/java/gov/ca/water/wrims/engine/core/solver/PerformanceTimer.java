package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.components.ControlData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.UUID;

/**
 * Shared lightweight performance timer for both CBC and XA.
 *
 * Routes perf events to different loggers based on MDC "solver":
 *   solver=CBC -> gov.ca.water.wrims.engine.core.solver.perf.cbc
 *   solver=XA  -> gov.ca.water.wrims.engine.core.solver.perf.xa
 * fallback     -> gov.ca.water.wrims.engine.core.solver.perf.unknown
 */
class PerformanceTimer {
    private static final Logger CBC_LOGGER =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.cbc");
    private static final Logger XA_LOGGER =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.xa");
    private static final Logger UNKNOWN_LOGGER =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.unknown");

    private final String id;
    private final String operation;
    private final long startTimeMs;

    private final String cycle;
    private final String date;
    private final String timeStep;

    private long durationMs;
    private State state;

    public PerformanceTimer(String operation) {
        this.id = UUID.randomUUID().toString();
        this.operation = operation;
        this.startTimeMs = System.currentTimeMillis();
        this.cycle = safe(ControlData.currCycleName);
        this.date = buildDate();
        this.timeStep = String.valueOf(ControlData.timeStep);
        this.durationMs = 0L;
        this.state = State.RUNNING;
        report("start");
    }

    public void report() {
        report("report");
    }

    public long stop() {
        updateDuration();
        this.state = State.STOPPED;
        report("stop");
        return durationMs;
    }

    public void report(String event) {
        updateDuration();

        Logger logger = selectLogger();
        LoggingEventBuilder b;

        if ("error".equalsIgnoreCase(event)) {
            b = logger.atError();
        } else {
            b = logger.atDebug();
        }

        b.setMessage("event={} timer={} operation={} duration_ms={} state={} model_date={} cycle={} timeStep={} run_id={} mode={} solver={}")
                .addArgument(safe(event))
                .addArgument(id)
                .addArgument(safe(operation))
                .addArgument(durationMs)
                .addArgument(state.name())
                .addArgument(date)
                .addArgument(cycle)
                .addArgument(timeStep)
                .addArgument(mdc("run_id"))
                .addArgument(mdc("mode"))
                .addArgument(mdc("solver"))
                .addKeyValue("event", safe(event))
                .addKeyValue("timer", id)
                .addKeyValue("operation", safe(operation))
                .addKeyValue("duration_ms", durationMs)
                .addKeyValue("state", state.name())
                .addKeyValue("model_date", date)
                .addKeyValue("cycle", cycle)
                .addKeyValue("timeStep", timeStep)
                .addKeyValue("run_id", mdc("run_id"))
                .addKeyValue("mode", mdc("mode"))
                .addKeyValue("solver", mdc("solver"))
                .log();
    }

    @Override
    public String toString() {
        updateDuration();
        return String.format(
                "%s(uuid=\"%s\", model_date=\"%s\", cycle=\"%s\", timeStep=\"%s\", duration_ms=\"%s\", state=\"%s\", operation=\"%s\", run_id=\"%s\", mode=\"%s\", solver=\"%s\")",
                this.getClass().getSimpleName(),
                this.id,
                this.date,
                this.cycle,
                this.timeStep,
                this.durationMs,
                this.state,
                this.operation,
                mdc("run_id"),
                mdc("mode"),
                mdc("solver")
        );
    }

    private Logger selectLogger() {
        String solver = mdc("solver");
        if ("CBC".equalsIgnoreCase(solver)) {
            return CBC_LOGGER;
        }
        if ("XA".equalsIgnoreCase(solver)) {
            return XA_LOGGER;
        }
        return UNKNOWN_LOGGER;
    }

    private void updateDuration() {
        this.durationMs = System.currentTimeMillis() - this.startTimeMs;
    }

    private static String buildDate() {
        try {
            return String.format("%s-%s-%s", ControlData.currYear, ControlData.currMonth, ControlData.currDay);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String mdc(String key) {
        try {
            String v = MDC.get(key);
            return v == null ? "na" : v;
        } catch (Exception e) {
            return "na";
        }
    }

    private static String safe(String s) {
        return s == null ? "unknown" : s;
    }

    private enum State {
        RUNNING,
        STOPPED
    }
}