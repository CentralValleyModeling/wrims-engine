package gov.ca.water.wrims.engine.core.debug;

import java.io.File;

import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.solver.LPSolveSolver;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;

public class ChangeSolver {
	
	public static void loadLPSolveConfigFile(){
		//To Do: temporary set up
		String f="callite.lpsolve";
		
		try {

			File sf = new File(StudyUtils.configDir, f);
			if (sf.exists()) {					
				LPSolveSolver.configFile = sf.getCanonicalPath();
			} else {
				Error.addConfigError("LpSolveConfigFile not found: " + f);
				Error.writeErrorLog();
			}

		} catch (Exception e) {
			Error.addConfigError("LpSolveConfigFile not found: " + f);
			Error.writeErrorLog();
			e.printStackTrace();
		}
	}
}
