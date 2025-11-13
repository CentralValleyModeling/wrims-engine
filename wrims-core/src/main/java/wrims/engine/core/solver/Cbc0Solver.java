package wrims.engine.core.solver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.solverdata.*;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.components.IntDouble;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;
import wrims.engine.core.ilp.ILP;
import wrims.engine.core.solver.cbc.FilePassingUtils;

public class Cbc0Solver {
	
	public static int errorCode;  // 11 : error in batch // 12: error in dvar value
	public static Map <String, Double> varDoubleMap;
	private static String lpFilePath;
	private static String soluPath;
	private static String cbcPath;
	private static String tolerance1 = "-primalT 1e-008";
	private static String tolerance2 = "-primalT 1e-007";
	
	public static void init() {
		
		//set batch file path
		cbcPath = System.getenv("CBC0_PATH");
		
	}
	
    public static void setLP(String filePath){

		 // set lp file path and solution file path
    	lpFilePath = filePath;
    	soluPath = FilenameUtils.removeExtension(lpFilePath)+".sn";
    	File soluF = new File(soluPath);
    	if (soluF.exists()){
    		soluF.delete();
    	}
		  
	}

	public static void solve(){
		
		long beginT = System.currentTimeMillis();
		solve_command();
		long endT = System.currentTimeMillis();
		
		double time_second = (endT-beginT)/1000.;
		
		//System.out.println(FilenameUtils.getBaseName(soluPath));
		if (time_second > 1.0) {
			String mname = FilenameUtils.getBaseName(soluPath);
		
			ILP.writeNoteLn(mname, " time(sec): "+time_second);
			//System.out.println(ILP.getYearMonthCycle()+": "+time_second);
			System.out.println(mname+": "+time_second);
			
		}
	
	}
	
	public static void solve_command(){
	    		
		// call cbc to solve
		
		String soluName = FilenameUtils.getBaseName(soluPath)+".sn";
		String soluDir  = new File(soluPath).getParent();

		String arg_lpPath = "\""+lpFilePath+"\"";
		String arg_soluDir = "\""+soluDir+"\"";
		String arg_soluName = "\""+soluName+"\"";
		
		ControlData.clp_cbc_objective = null;
		ControlData.clp_cbc_note ="";
		
		try {

			callExe(arg_lpPath, arg_soluDir, arg_soluName, tolerance1);
			
//			if (!isOptimal) {
//				//System.out.println("msg:"+msg);
//				String note = "Retry with "+tolerance2;
//				ControlData.clp_cbc_note = note;
//				System.out.println(ILP.getYearMonthCycle()+": "+note);
//				callExe(arg_lpPath, arg_soluDir, arg_soluName, tolerance2);
//			}
			
			errorCode=FilePassingUtils.parseReturnFile(soluPath);

		} catch (Exception ex) {
			
			errorCode=11;
			Error.addSolvingError("CBC solver error code: "+errorCode + ": possible batch file error.");
		}
		
		
		if (errorCode!=0) {
			
			Error.addSolvingError("CBC solver error code: "+errorCode);
			Error.addSolvingError("CBC solver error message: "+FilePassingUtils.solverMessage);
			
		}
				
			// if no error then: 
			// 1. assign objective, 
			// 2. collect dvar, 
			// 3. feedback dvar to wrims2
		
		  if (Error.error_solving.size()==0) {
			  ControlData.clp_cbc_objective = FilePassingUtils.objective_value;
			  collectDvar();
		      assignDvar();				  
		  }	
	    	
	    }

	private static void callExe(String arg_lpPath, String arg_soluDir, String arg_soluName, String tolerance) throws IOException, InterruptedException {
		
		//boolean isOptimal = false;
		
		Runtime rt = Runtime.getRuntime();

		//String directCall = cbcPath+" "+arg_lpPath+" "+tolerance+" -solv -directory "+arg_soluDir+" -solu "+arg_soluName ;
		String directCall = cbcPath+" "+arg_lpPath+" "+" -solv -directory "+arg_soluDir+" -solu "+arg_soluName ;
		//System.out.println(directCall);
		Process proc = rt.exec(directCall);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		String allLines ="";
		String line;
		String lastLine="";
		String last2Line="";
		String last3Line="";
		String last4Line="";
		while ((line = reader.readLine()) != null){
			//if (line.isEmpty()) continue;
			//allLines = allLines+"\n"+line;
			//if (line.toLowerCase().contains("optimal")) {
			//	isOptimal=true;
			//}
//			System.out.println("line: "+line);	
//			last4Line=last3Line;
//			last3Line=last2Line;
//			last2Line=lastLine;
//			lastLine=line;
			continue;
		}

		
//		if (!isOptimal) {
//			System.out.println("============== Error ===============");
//			System.out.println(allLines);
//		}
		//int exitVal = proc.waitFor();  // TODO: exitVal not works

		//return last2Line.toLowerCase();
		//return isOptimal;
	}	
	
	
	private static void collectDvar() {
		
		varDoubleMap = FilePassingUtils.varDoubleMap;
		
	}

	public static void setDefaultOption() {
		
	}

	public static void getSolverInformation(int modelStatus){

//		Error.addSolvingError(solver.getStatustext(modelStatus)); 
//		Error.addSolvingError(lpFilePath); 		
	}
	
	
	private static void assignDvar() {
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
		Set<String> dvarUsedByLaterCycle = ControlData.currModelDataSet.dvarUsedByLaterCycle;
		Set<String> dvarTimeArrayUsedByLaterCycle = ControlData.currModelDataSet.dvarTimeArrayUsedByLaterCycle;
		ArrayList<String> timeArrayDvList = ControlData.currModelDataSet.timeArrayDvList;
		String model=ControlData.currCycleName;
		
		StudyDataSet sds = ControlData.currStudyDataSet;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();
		
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		Set dvarCollection = dvarMap.keySet();
		Iterator dvarIterator = dvarCollection.iterator();
			
		while(dvarIterator.hasNext()){ 
			String dvName=(String)dvarIterator.next();
			Dvar dvar=dvarMap.get(dvName);
			
			double value = -77777777;
			try {
				if ( varDoubleMap.keySet().contains(dvName) ) {
					value=varDoubleMap.get(dvName);
				} else {
					value = 0;  // if not listed in clp solution file then it's 0
					varDoubleMap.put(dvName, value);
				}
				
			} catch (Exception e) {
				errorCode=12;
				Error.addSolvingError("CBC solution dvar value error. Dvar: "+dvName+" Value: "+varDoubleMap.get(dvName));
				
			}
			IntDouble id=new IntDouble(value,false);
			dvar.setData(id);
			if(dvarUsedByLaterCycle.contains(dvName)){
				varCycleValueMap.get(dvName).put(model, id);
			}else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)){
				if (varTimeArrayCycleValueMap.containsKey(dvName)){
					varTimeArrayCycleValueMap.get(dvName).put(model, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(model, dvar.data);
					varTimeArrayCycleValueMap.put(dvName, cycleValue);
				}
			}
			if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)){
				if (varCycleIndexValueMap.containsKey(dvName)){
					varCycleIndexValueMap.get(dvName).put(model, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(model, dvar.data);
					varCycleIndexValueMap.put(dvName, cycleValue);
				}
			}
			String entryNameTS=DssOperation.entryNameTS(dvName, ControlData.timeStep);
			DataTimeSeries.saveDataToTimeSeries(dvName, entryNameTS, value, dvar);
			if (timeArrayDvList.contains(dvName)){
				entryNameTS=DssOperation.entryNameTS(dvName+"__fut__0", ControlData.timeStep);
				DataTimeSeries.saveDataToTimeSeries(entryNameTS, value, dvar, 0);
			}
		}
		
		if (ControlData.showRunTimeMessage) {
			System.out.println("Objective Value: "+ControlData.clp_cbc_objective);
			System.out.println("Assign Dvar Done.");
		}
	}

}
