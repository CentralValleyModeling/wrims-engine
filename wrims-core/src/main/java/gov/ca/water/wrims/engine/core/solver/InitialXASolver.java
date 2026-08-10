package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.components.ControlData;

import com.sunsetsoft.xa.Optimizer;
import com.sunsetsoft.xa.XAException;

import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.tools.MemoryParamParser;

public class InitialXASolver {
	public static final String XA_MAX_MEMORY_PARAM_KEY = "gov.ca.water.wrims.engine.core.solver.xa.memory.max";
	private static final long MEMORY_DEFAULT = 25000L;

	public InitialXASolver(){
		Long initialMemory = MemoryParamParser.getMaxMemoryInBytes(XA_MAX_MEMORY_PARAM_KEY, MEMORY_DEFAULT);
		ControlData.xasolver=new Optimizer(initialMemory.intValue());
		ControlData.xasolver.setActivationCodes( 234416483 , 19834525 ) ;
		ControlData.xasolver.setXAMessageWindowOff();
		try{
			ControlData.xasolver.openConnection();
		}
		catch (XAException e)
	    {
	       Error.addEngineError("Missing XA Dongle or supporting license files.");
	       return;
	    }
		ControlData.xasolver.setModelSize(100, 100);
		ControlData.xasolver.setCommand("MAXIMIZE Yes MUTE yes FORCE No wait no matlist v set visible no");
		System.out.println("Initialize XA solver done");
		//ControlData.xasolver.setCommand("Basis "+FilePaths.mainDirectory+"\\xabasis.tmp");
		//ControlData.xasolver.setCommand("set sortName Yes FileName d:\\temp Output v2%d.log MatList V MPSX Yes ToRcc Yes");    //rcc code
		//ControlData.xasolver.setCommand( "FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log set sortName Yes MatList V MPSX Yes ToRcc Yes set debug Yes  ListInput Yes")    //xa debug ;
		//if (ControlData.solverName.equalsIgnoreCase("XALOG") ) ControlData.xasolver.setCommand( "FileName  "+FilePaths.mainDirectory+"  Output "+FilePaths.mainDirectory+"\\xa.log set sortName YES MPSX Yes ToRcc Yes") ;
		//ControlData.xasolver.setCommand("MultiSolver 4");
		//ControlData.xasolver.setCommand("LimitSearch (1%)); 
	}
}
