package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.components.ControlData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.UUID;

// Performance timer for internal operations and debug
class PerformanceTimer {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTimer.class);
    private static final Logger cbcLogger = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.cbc");
    private static final Logger xaLogger = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.xa");
    private static final Logger unknownLogger = LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.unknown");

    private final String operation;
    private final String id;
    private final long startTime;
    private final String cycle;
    private final String date;
    private final String timeStep;
    private long duration;
    private State state;

    public PerformanceTimer(String operation) {
        this.id = UUID.randomUUID().toString();
        this.operation = operation;
        this.startTime = System.currentTimeMillis();
        this.cycle = ControlData.currCycleName;
        this.date = String.format("%s-%s-%s", ControlData.currYear, ControlData.currMonth, ControlData.currDay);
        this.timeStep = String.valueOf(ControlData.timeStep);
        this.duration = 0;
        this.state = State.RUNNING;
        report("start");
    }

    public void report(String event) {
        updateDuration();
        Logger out = unknownLogger;
        String op = safe(operation);

        if (op.regionMatches(true, 0, "CBC", 0, 3)
                || op.regionMatches(true, 0, "CbcSolver", 0, 9)) {
            out = cbcLogger;
        } else if (op.regionMatches(true, 0, "XA", 0, 2)
                || op.regionMatches(true, 0, "XASolver", 0, 8)) {
            out = xaLogger;
        } else {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement e : stack) {
                String className = e.getClassName();
                if (className == null || className.equals(PerformanceTimer.class.getName())) {
                    continue;
                }
                if (className.endsWith(".CbcSolver")) {
                    out = cbcLogger;
                    break;
                }
                if (className.endsWith(".XASolver")) {
                    out = xaLogger;
                    break;
                }
            }
        }

        LoggingEventBuilder builder;
        if (event.equalsIgnoreCase("error")) {
            builder = out.atError();
        } else {
            builder = out.atDebug();
        }
        builder.setMessage("event={} timer={} operation={} duration_ms={} state={} model_date={} cycle={} timeStep={}").addArgument(safe(event)).addArgument(this.id).addArgument(safe(this.operation)).addArgument(this.duration).addArgument(this.state.name()).addArgument(this.date).addArgument(this.cycle).addArgument(this.timeStep).log();
    }

    private void updateDuration() {
        this.duration = System.currentTimeMillis() - this.startTime;
    }

    public String toString() {
        updateDuration();
        return String.format(
                "%s(uuid=\"%s\", model_date=\"%s\", cycle=\"%s\", timeStep=\"%s\", duration_ms=\"%s\", state=\"%s\", operation=\"%s\")",
                this.getClass().getSimpleName(),
                this.id,
                this.date,
                this.cycle,
                this.timeStep,
                this.duration,
                this.state,
                this.operation
        );
    }

    public void report() {
        report("report");
    }

    public long stop() {
        updateDuration();
        this.state = State.STOPPED;
        report("stop");
        return duration;
    }

    private static String buildDate() {
        try {
            return String.format("%s-%s-%s", ControlData.currYear, ControlData.currMonth, ControlData.currDay);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String safe(String s) {
        return s == null ? "unknown" : s;
    }

    private enum State {
        NOT_STARTED,
        RUNNING,
        STOPPED,
    }
}