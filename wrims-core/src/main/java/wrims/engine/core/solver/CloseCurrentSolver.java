package wrims.engine.core.solver;

import wrims.engine.core.components.ControlData;
import wrims.engine.core.solver.Gurobi.GurobiSolver;

public class CloseCurrentSolver {
	public CloseCurrentSolver(String currentSolver){
		if (currentSolver.equalsIgnoreCase("XA") || currentSolver.equalsIgnoreCase("XALOG")) {
			ControlData.xasolver.close();
		}else if (currentSolver.equalsIgnoreCase("Gurobi")){
			GurobiSolver.dispose();
		}else if (currentSolver.equalsIgnoreCase("Cbc")){
			CbcSolver.close();
			if (ControlData.cbc_debug_routeXA || ControlData.cbc_debug_routeCbc) {ControlData.xasolver.close();}
		}
	}
}
