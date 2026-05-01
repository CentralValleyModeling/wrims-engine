package gov.ca.water.wrims.engine.core.solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PerformanceTimerCbc extends PerformanceTimer {
    private static final Logger CBC_LOGGER =
            LoggerFactory.getLogger("gov.ca.water.wrims.engine.core.solver.perf.cbc");

    PerformanceTimerCbc(String operation) {
        super(operation);
    }

    @Override
    protected Logger getLogger() {
        return CBC_LOGGER;
    }
}