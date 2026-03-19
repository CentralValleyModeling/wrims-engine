package gov.ca.water.wrims.engine.core.tools;

import gov.ca.water.wrims.engine.core.components.ControlData;

public class General {
	public static boolean isSelectedCycleOutput(String strCycleI){
		if (ControlData.outputAllCycles){
			return true;
		}
		boolean foundTheCycle=false;
		for (int n=0; n<ControlData.selectedCycles.length; n++){
			if (ControlData.selectedCycles[n].equals(strCycleI)){
				foundTheCycle=true;
			}
		}
		return foundTheCycle;
	}
	
	public static void getPID(){
		ControlData.pid = (int)ProcessHandle.current().pid();
	}
}
