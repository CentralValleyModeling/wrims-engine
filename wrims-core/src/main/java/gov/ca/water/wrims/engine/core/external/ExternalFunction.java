package gov.ca.water.wrims.engine.core.external;

import java.io.File;
import java.util.*;

import gov.ca.water.wrims.engine.core.components.FilePaths;

public abstract class ExternalFunction{

	public static String externalDir=FilePaths.mainDirectory+File.separator+"external"+File.separator;

	public abstract void execute(Stack stack);
}
