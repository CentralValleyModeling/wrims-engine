package gov.ca.water.wrims.engine.core.external;

import gov.ca.water.wrims.engine.core.components.FilePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Stack;

public abstract class ExternalFunction {
    public static final Logger LOGGER = LoggerFactory.getLogger(ExternalFunction.class);
    public static String externalDir = FilePaths.mainDirectory + File.separator + "external" + File.separator;

    public abstract void execute(Stack stack);
}
