package wrims.engine.core.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import wrims.engine.core.commondata.solverdata.SolverData;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.solverdata.*;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.components.IntDouble;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;
import lpsolve.*;

public class LPSolveSolver {
	
	public static Map <String, Double> varDoubleMap;
	private static LpSolve solver;
	private static String lpFilePath;
	private static ArrayList<String> origcolName;
	//public static ArrayList<String> paramFiles;
	public static String configFile = null;
	//public static ArrayList<String> paramHeader;
	public static int numberOfRetries = 0;  // default is zero
	//public static boolean overwriteDefaultSetting = false; // default is false
	
    public static void setLP(String filePath){

		  try {
			  // TODO: remove this test
			  //filePath = "D:\\cvwrsm\\trunk\\callite\\CalLite\\run\\=ILP=\\=LpSolve=\\demo.config\\lpsolve\\1933_06_c01.lps";
			  
			  solver = LpSolve.readLp(filePath, LpSolve.CRITICAL, "test_prob");  
	  
     
			  try {
				  solver.readParams(configFile, "-h Default");
     
			  } catch (Exception e) {				  
				  Error.addSolvingError("Header \"Default\" not found in LpSolve config file");
			  }

			  
		      lpFilePath = filePath;
		      
				
		      origcolName = new ArrayList<String>();
				for (int i = 1; i <= solver.getNorigColumns(); i++) {
					
					//System.out.println(i + ":"+solver.getOrigcolName(i));
					//varDoubleMap.put(solver.getOrigcolName(i), solver.getVarPrimalresult(i+rn));
					
					origcolName.add(solver.getOrigcolName(i));
					//System.out.println(solver.getOrigcolName(i)+" : "+solver.getVarPrimalresult(i+rn));
			    }
		      
		    }
		    catch (LpSolveException e) {
		       e.printStackTrace();
		    }
		  

 
	}
	
	public static void solve(){
	    	
			  int i = 1;
			  int modelStatus = -777;
		
			  try {
				  
			      modelStatus = solver.solve();		      
			      
				  while ( (modelStatus!=LpSolve.OPTIMAL) && (i<=numberOfRetries) )	{
					  
					  //System.out.println("LpSolve Error: "+solver.getStatustext(modelStatus));
					  
					  
					  //for (int i=1;i<=numberOfRetries; i++) {  
					  
						  solver.deleteLp();
						  solver = LpSolve.readLp(lpFilePath, LpSolve.CRITICAL, "test_prob");
						  
						  try {
							  System.out.println("! Retry with LpSolve config named: Retry"+ i);
							  solver.readParams(configFile, "-h Retry"+i);
							  modelStatus = solver.solve();
							  i++;
						  } catch (Exception e) {
							  Error.addSolvingError("Header \"Retry" + i + "\" not found in LpSolve config file");
							  break;
						  }  
					  
				  }
				  
				  if (modelStatus!= LpSolve.OPTIMAL)	{
					  getSolverInformation(modelStatus);
				  }
				  
				  if (Error.error_solving.size()==0) {
					  ControlData.lpsolve_objective = solver.getObjective(); // for other processes
					  collectDvar();
				      assignDvar();				  
				  }	
				  
			      // delete the problem and free memory
			      solver.deleteLp(); 
			      		      
			    }
			    catch (LpSolveException e) {
			    	Error.addSolvingError("LpSolveSolver error.");
			    	e.printStackTrace();
			    }
	    	
	    }

	public static void setDefaultOption() {
		
		  solver.setSimplextype(LpSolve.SIMPLEX_DUAL_PRIMAL);
		  solver.setImprove(LpSolve.IMPROVE_DUALFEAS | LpSolve.IMPROVE_THETAGAP );
		  solver.setAntiDegen(LpSolve.ANTIDEGEN_FIXEDVARS | LpSolve.ANTIDEGEN_STALLING);
	      solver.setPivoting(LpSolve.PRICER_DEVEX | LpSolve.PRICE_ADAPTIVE);
	      solver.setScaling(LpSolve.SCALE_GEOMETRIC | LpSolve.SCALE_EQUILIBRATE | LpSolve.SCALE_INTEGERS);
		  solver.setBbFloorfirst(LpSolve.BRANCH_AUTOMATIC);
		  solver.setBbRule(LpSolve.NODE_GREEDYMODE | LpSolve.NODE_DYNAMICMODE | LpSolve.NODE_RCOSTFIXING | LpSolve.NODE_PSEUDONONINTSELECT);
		  solver.setTimeout(5);
		  solver.setEpsint(2E-7);
		  solver.setEpsel(1E-11);
		  
	      //solver.setVerbose(LpSolve.IMPORTANT);
		  //solver.setPresolve(LpSolve.PRESOLVE_ROWS , 200);
		  //solver.setPresolve(LpSolve.PRESOLVE_ROWS|LpSolve.PRESOLVE_COLS|LpSolve.PRESOLVE_PROBEFIX|LpSolve.PRESOLVE_PROBEREDUCE , solver.getPresolveloops());
		  solver.setPresolve(LpSolve.PRESOLVE_ROWS|LpSolve.PRESOLVE_COLS , solver.getPresolveloops());
	}

	public static void getSolverInformation(int modelStatus){

		Error.addSolvingError(solver.getStatustext(modelStatus)); 
		Error.addSolvingError(lpFilePath); 		
	}
	
	private static void collectDvar() throws LpSolveException{
		//Map<String, Dvar> dvarMap=SolverData.getDvarMap();
		
		varDoubleMap = new HashMap<String, Double>();
		
		int rn = solver.getNorigRows();
		int cn = solver.getNorigColumns();
		
		//System.out.println("solver.getNorigColumns():" + cn);
		//System.out.println("solver.getNorigRows():" + rn);
		
		for (int i = 1; i <= cn; i++) {
			
			//System.out.println(i + ":"+solver.getOrigcolName(i));
			//varDoubleMap.put(solver.getOrigcolName(i), solver.getVarPrimalresult(i+rn));
			varDoubleMap.put(origcolName.get(i-1), solver.getVarPrimalresult(i+rn));
			//System.out.println(solver.getOrigcolName(i)+" : "+solver.getVarPrimalresult(i+rn));
			
			// TODO: add the following line before sending the problem to the solver using direct link. 
			// it's too late here. need to assign value.
			// if (!dvarMap.containsKey(origcolName.get(i-1))) addConditionalSlackSurplusToDvarMap(dvarMap, origcolName.get(i-1));
	    }		
	
	}
	
	private static void assignDvar() throws LpSolveException{
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
				value=varDoubleMap.get(dvName);
			} catch (Exception e) {
				//value = 0;  // TODO: warning!! needs work here!!
				
				//System.out.println(" This dvName not found: "+ dvName);
				//continue;
				try {
					value = (Double) dvar.getData().getData(); // use whatever is in the container.
				} catch (Exception e2) {
					value=-77777777; // TODO: if this value is used, then this is probably an error in the wresl code. need to give warning.
				}
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
			System.out.println("Objective Value: "+ControlData.lpsolve_objective);
			System.out.println("Assign Dvar Done.");
		}
	}
	public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);
	}
}
