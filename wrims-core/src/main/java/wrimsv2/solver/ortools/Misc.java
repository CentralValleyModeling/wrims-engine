package wrimsv2.solver.ortools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.EvalConstraint;
import wrimsv2.solver.mpmodel.MPModel;


public class Misc {

	
	protected static void setConstraints(MPModel m) {
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
			
				double lb = -Param.inf;
				double ub =  Param.inf;
				
				if (ec.getSign().equals("=")) {
					//ControlData.xasolver.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); 
					lb = -ec.getEvalExpression().getValue().getData().doubleValue();
					ub = -ec.getEvalExpression().getValue().getData().doubleValue();
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					//ControlData.xasolver.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); 
					ub = -ec.getEvalExpression().getValue().getData().doubleValue();
				}
				else if (ec.getSign().equals(">")){
					lb = -ec.getEvalExpression().getValue().getData().doubleValue();
				}
			
				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				
				//TODO: what if multMap is empty?? this means 0 according to wresl parser.
				// for example,      lb < 0
				if (multMap.size()==0) {
					
					if ( lb <= 0 && 0 <= ub) {
						System.out.println("Constraint: "+constraintName+" is removed.");
						continue; // skip to next constraint without adding this to lp
					} else {
						Error.addSolvingError("Infeasible solution caused by constraint named: "+constraintName); break;
					}
				}
				
				
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();
			
				LinkedHashMap<String, Double> varCoefMap = new LinkedHashMap<String, Double>();
				
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
					if (!dvarMap.containsKey(multName)) addConditionalSlackSurplusToDvarMap(dvarMap, multName);
					
					varCoefMap.put(multName, multMap.get(multName).getData().doubleValue());
					//ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
				}
				m.createConstraint(constraintName, varCoefMap, lb, ub);
			}
		}
	}
	
	protected static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);
	}
	
	protected static void assignDvar(LinkedHashMap<String, Double> solution){
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
			//double value=ControlData.xasolver.getColumnActivity(dvName);
			double value=solution.get(dvName);
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
			//System.out.println("Objective Value: "+ControlData.xasolver.getObjective());
			System.out.println("Assign Dvar Done.");
		}
	}
	
	protected static void setDVars(MPModel m){
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
					m.addIntVar(dvarName, lb, ub); }
				else {
					m.addGeneralVar(dvarName, lb, ub); }
			}
		}
	}

	protected static void setWeights(MPModel m){
		Map<String, WeightElement> weightMap = SolverData.getWeightMap();
		LinkedHashMap<String, Double> objFunction = new LinkedHashMap<String, Double>();
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
				//ControlData.xasolver.setColumnObjective(weightName, weightMap.get(weightName).getValue());
				objFunction.put(weightName, weightMap.get(weightName).getValue());
			}
		
			Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
			CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
			Iterator<String> usedWeightSlackSurplusIterator = usedWeightSlackSurplusCollection.iterator();
		
			while(usedWeightSlackSurplusIterator.hasNext()){
				String usedWeightSlackSurplusName=(String)usedWeightSlackSurplusIterator.next();
				//ControlData.xasolver.setColumnObjective(usedWeightSlackSurplusName, weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue());
				objFunction.put(usedWeightSlackSurplusName, weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue());
			}
		}
		m.objFunction = objFunction;
	}
	
	protected static void getSolverInformation(int modelStatus){
		System.out.println("Solver status: "+modelStatus);
		
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

}
