package wrimsv2.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.EvalConstraint;

public class CheckWeightSlackSurplus {
	
	public CheckWeightSlackSurplus(){
		CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
		Map<String, WeightElement> weightSlackSurplusMap=SolverData.getWeightSlackSurplusMap();
		
		Map<String, EvalConstraint> constraintMap=SolverData.getConstraintDataMap();
		ArrayList<String> constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
		constraintCollection.retainAll(constraintMap.keySet());
		Iterator<String> constraintIterator = constraintCollection.iterator();
		
		while(constraintIterator.hasNext()){                          
			String constraintName=(String)constraintIterator.next();
			EvalConstraint ec=constraintMap.get(constraintName);
			
			Map<String, IntDouble> multipliers=ec.getEvalExpression().getMultiplier();
			Set keySet=multipliers.keySet();
			Iterator multipliersIterator=keySet.iterator();
			while (multipliersIterator.hasNext()){
				String multiplierName=(String)multipliersIterator.next();
				if (usedWeightSlackSurplusCollection.contains(multiplierName)){
					usedWeightSlackSurplusCollection.remove(multiplierName);
				}else{
					if (weightSlackSurplusMap.containsKey(multiplierName)){
						if (multiplierName.startsWith("surlus__") || multiplierName.startsWith("slack__")){
							System.out.println(multiplierName+" is not in weight table!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
						}
					}
				}
			}
		}
		if (usedWeightSlackSurplusCollection.size()>0){
			System.out.println(usedWeightSlackSurplusCollection+" is not used in constraint!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		}
	}
}
