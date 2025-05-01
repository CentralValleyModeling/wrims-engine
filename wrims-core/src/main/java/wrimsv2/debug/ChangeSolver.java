package wrimsv2.debug;

import java.io.File;

import wrimsv2.components.Error;
import wrimsv2.components.FilePaths;
import wrimsv2.solver.LPSolveSolver;
import wrimsv2.wreslparser.elements.StudyUtils;

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
