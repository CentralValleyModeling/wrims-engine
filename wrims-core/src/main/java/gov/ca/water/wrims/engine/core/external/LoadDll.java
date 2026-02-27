package gov.ca.water.wrims.engine.core.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadDll {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadDll.class);

    public LoadDll(String dllName) {
        LOGGER.atDebug().setMessage("loading dll: {}").addArgument(dllName).log();
        System.loadLibrary(dllName.replace(".dll", ""));
    }
}
