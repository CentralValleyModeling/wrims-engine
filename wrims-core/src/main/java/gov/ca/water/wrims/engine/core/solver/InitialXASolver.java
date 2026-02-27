package gov.ca.water.wrims.engine.core.solver;

import com.sunsetsoft.xa.Optimizer;
import com.sunsetsoft.xa.XAException;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitialXASolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialXASolver.class);

    public InitialXASolver() {
        ControlData.xasolver = new Optimizer(25000);
        ControlData.xasolver.setActivationCodes(234416483, 19834525);
        ControlData.xasolver.setXAMessageWindowOff();
        try {
            ControlData.xasolver.openConnection();
        } catch (XAException e) {
            Error.addEngineError("Missing XA Dongle or supporting license files.");
            return;
        }
        ControlData.xasolver.setModelSize(100, 100);
        ControlData.xasolver.setCommand("MAXIMIZE Yes MUTE yes FORCE No wait no matlist v set visible no");
        LOGGER.atInfo().setMessage("Initialize XA solver done").log();
    }
}
