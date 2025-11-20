package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.components.VariableTimeStep;
import gov.ca.water.wrims.engine.core.ilp.ILP;

public class Functioninitgwsystem extends ExternalFunction{
	private final boolean DEBUG = false;

	public Functioninitgwsystem(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		String cGWPath = FilePaths.groundwaterDir;
		int iLenGWPath = cGWPath.length();
		String cOutPath = FilePaths.dvarDssDirectory;
		int iLenOutPath = cOutPath.length();
		String cTimeStep = ControlData.defaultTimeStep;
		int iNTimeSteps = VariableTimeStep.getTotalTimeStep(ControlData.defaultTimeStep);
		int iSimBeginMonth;
		int iSimBeginYear;
		int iResimulate;
		if (ControlData.resimGroundwater){
			ControlData.resimGroundwater=false;
			iSimBeginMonth = ControlData.resimMonth;
			iSimBeginYear = ControlData.resimYear;
			iResimulate=1;
		}else{
			iSimBeginMonth = ControlData.startMonth;
			iSimBeginYear = ControlData.startYear;
			iResimulate=0;
		}
		int iSimMode=0;
		
		if (ILP.logging){
			iSimMode = 1;
		}

		int iNRestartFiles=12;
		if (ControlData.allRestartFiles){
			iNRestartFiles=-1;
		}else{
			iNRestartFiles=ControlData.numberRestartFiles;
		}

		int iPrintFuncCalls=0;
		if (ControlData.printGWFuncCalls){
			iPrintFuncCalls=1;
		}
		
		float result = initgwsystem(cGWPath,iLenGWPath,cOutPath,iLenOutPath,cTimeStep,iNTimeSteps,iSimBeginMonth,iSimBeginYear,iSimMode,iNRestartFiles,iResimulate, iPrintFuncCalls);
		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float initgwsystem(String cGWPath, int iLenGWPath,String cOutPath, int iLenOutPath, String cTimeStep, int iNTimeSteps, int iSimBeginMonth, int iSimBeginYear, int iSimMode, int iNRestartFiles, int iResimulate, int iPrintFuncCalls);
}
