package gov.ca.water.wrims.engine.core.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PerformanceTimerXa extends PerformanceTimer {
    private static final Logger XA_LOGGER =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.xa");

    PerformanceTimerXa(String operation) {
        super(operation);
    }

    @Override
    protected Logger getLogger() {
        return XA_LOGGER;
    }
}