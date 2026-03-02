package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.components.ControlData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.UUID;

// Performance timer for internal operations and debug
class PerformanceTimer {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTimer.class);
    private final String operation;
    private final String id;
    private final long startTime;
    private final String cycle;
    private final String date;
    private long duration;
    private State state;

    public PerformanceTimer(String operation) {
        this.id = UUID.randomUUID().toString();
        this.operation = operation;
        this.startTime = System.currentTimeMillis();
        this.cycle = ControlData.currCycleName;
        this.date = String.format("%s-%s-%s", ControlData.currYear, ControlData.currMonth, ControlData.currDay);
        this.duration = 0;
        this.state = State.RUNNING;
        report("start");
    }

    public void report(String event) {
        updateDuration();
        LoggingEventBuilder builder;
        if (event.equalsIgnoreCase("error")) {
            builder = logger.atError();
        } else {
            builder = logger.atInfo();
        }
        builder.setMessage("event=\"{}\", {}").addArgument(event).addArgument(this.toString()).log();
    }

    private void updateDuration() {
        this.duration = System.currentTimeMillis() - this.startTime;
    }

    public String toString() {
        updateDuration();
        return String.format(
                "%s(uuid=\"%s\", model_date=\"%s\", cycle=\"%s\", duration=\"%s\", state=\"%s\", operation=\"%s\")",
                this.getClass().getSimpleName(),
                this.id,
                this.date,
                this.cycle,
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

    private enum State {
        NOT_STARTED,
        RUNNING,
        STOPPED,
    }
}
