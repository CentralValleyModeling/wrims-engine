package wrims.engine.core.solver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import wrims.engine.core.commondata.solverdata.SolverData;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.commondata.solverdata.*;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.IntDouble;
import wrims.engine.core.components.Error;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;
import wrims.engine.core.evaluator.EvalConstraint;

public class XASolver {
	int modelStatus;
	
	public XASolver(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		ControlData.xasolver.loadNewModel();
		setConstraints();
		setDVars();
		setWeights();
		
		int ci=ControlData.currCycleIndex+1;
		if (ControlData.showRunTimeMessage) System.out.println("XA Solver: Solving "+ControlData.currMonth+"/"+ControlData.currDay+"/"+ControlData.currYear+" Cycle "+ci+" ["+ControlData.currCycleName+"]");
		ControlData.xasolver.solveWithInfeasibleAnalysis("Output console:");
		
		modelStatus=ControlData.xasolver.getModelStatus();
		if (ControlData.showRunTimeMessage) System.out.println("Model status: "+modelStatus);
		if (modelStatus>=2)	getSolverInformation();
		if (Error.error_solving.size()<1) assignDvar(); 
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_xa=ControlData.t_xa+(int) (t2-t1);
	}
	
	public void getSolverInformation(){
		System.out.println("Solver status: "+ControlData.xasolver.getSolverStatus());
		//System.out.println("Exception: "+ControlData.xasolver.getExceptionCode());
		//System.out.println("Message: "+ControlData.xasolver.getMessage());
		//System.out.println("Return code: "+ControlData.xasolver.getRc());
		
		switch (modelStatus){
		    case 2: Error.addSolvingError("Integer Solution (not proven the optimal integer solution).");break; 
			case 3: Error.addSolvingError("Unbounded solution."); break;
			case 4: Error.addSolvingError("Infeasible solution."); break;
			case 5: Error.addSolvingError("Callback function indicates Infeasible solution."); break;
			case 6: Error.addSolvingError("Intermediate infeasible solution."); break;
			case 7: Error.addSolvingError("Intermediate nonoptimal solution."); break;
			case 9: Error.addSolvingError("Intermediate Non-integer solution."); break;
			case 10: Error.addSolvingError("Integer Infeasible."); break;
			case 13: Error.addSolvingError("More memory required to load/solve model. Increase memory request in XAINIT call."); break;
			case 32: Error.addSolvingError("Integer branch and bound process currently active, model has not completed solving."); break;
			case 99: Error.addSolvingError("Currently solving model, model has not completed solving."); break;
			default: Error.addSolvingError("Solving failed"); break;
		}
	}
	
	public void setDVars(){
		if (ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting dvars");
		
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		for (int i=0; i<=1; i++){
			ArrayList<String> dvarCollection;
			if (i==0){
				dvarCollection = ControlData.currModelDataSet.dvList;
			}else{
				dvarCollection = ControlData.currModelDataSet.dvTimeArrayList;
			}
			Iterator<String> dvarIterator=dvarCollection.iterator();
		
			while(dvarIterator.hasNext()){                          
				String dvarName=(String)dvarIterator.next();
				Dvar dvar=dvarMap.get(dvarName);

				double lb = dvar.lowerBoundValue.doubleValue();
				double ub = dvar.upperBoundValue.doubleValue();
			
				if (dvar.integer.equals("y")){
					ControlData.xasolver.setColumnInteger(dvarName, lb, ub); }
				else {
					ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);}
			}
		}
	}

	public void setWeights(){
		if (ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting weights");
		
		Map<String, WeightElement> weightMap = SolverData.getWeightMap();
		for (int i=0; i<=1; i++){
			ArrayList<String> weightCollection;
			if (i==0){
				weightCollection = ControlData.currModelDataSet.wtList;
			}else{
				weightCollection = ControlData.currModelDataSet.wtTimeArrayList;
			}
			Iterator<String> weightIterator = weightCollection.iterator();
		
			while(weightIterator.hasNext()){
				String weightName=(String)weightIterator.next();
				ControlData.xasolver.setColumnObjective(weightName, weightMap.get(weightName).getValue());
			}
		}
		Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
		CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
		Iterator<String> usedWeightSlackSurplusIterator = usedWeightSlackSurplusCollection.iterator();
	
		while(usedWeightSlackSurplusIterator.hasNext()){
			String usedWeightSlackSurplusName=(String)usedWeightSlackSurplusIterator.next();
			ControlData.xasolver.setColumnObjective(usedWeightSlackSurplusName, weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue());
		}
	}
	
	private void setConstraints() {
		if (ControlData.showRunTimeMessage) System.out.println("XA Solver: Setting constraints");
		
		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		Map<String, Dvar> dvarMap=SolverData.getDvarMap();
		for (int i=0; i<=1; i++){
			ArrayList<String> constraintCollection;
			if (i==0){
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
				constraintCollection.retainAll(constraintMap.keySet());
			}else{
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
			}
			Iterator<String> constraintIterator = constraintCollection.iterator();
		
			while(constraintIterator.hasNext()){                          
				String constraintName=(String)constraintIterator.next();
				EvalConstraint ec=constraintMap.get(constraintName);
			
				if (ec.getSign().equals("=")) {
					ControlData.xasolver.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					ControlData.xasolver.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
				}
				else if (ec.getSign().equals(">")){
					ControlData.xasolver.setRowMin(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
				}
			
				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();
			
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
					if (!dvarMap.containsKey(multName)) addConditionalSlackSurplusToDvarMap(dvarMap, multName);
					ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
				}
			}
		}
	}
	
	public void assignDvar(){
		if (ControlData.showRunTimeMessage) System.out.println("XA Solver: Assigning dvars\' values");
		
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
			double value=ControlData.xasolver.getColumnActivity(dvName);
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
			System.out.println("Objective Value: "+ControlData.xasolver.getObjective());
			System.out.println("Assign Dvar Done.");
		}
	}
	
	public void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);
	}
}
