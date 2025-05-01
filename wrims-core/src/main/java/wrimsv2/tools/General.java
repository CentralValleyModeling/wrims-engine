package wrimsv2.tools;

import java.lang.reflect.InvocationTargetException;

import org.antlr.runtime.RecognitionException;

import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;

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
		java.lang.management.RuntimeMXBean runtime = 
			    java.lang.management.ManagementFactory.getRuntimeMXBean();
		try {
			java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");		
			jvm.setAccessible(true);
			sun.management.VMManagement mgmt =  
			(sun.management.VMManagement) jvm.get(runtime);
			java.lang.reflect.Method pid_method =  
			mgmt.getClass().getDeclaredMethod("getProcessId");
			pid_method.setAccessible(true);

			ControlData.pid = (Integer) pid_method.invoke(mgmt);
			
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
