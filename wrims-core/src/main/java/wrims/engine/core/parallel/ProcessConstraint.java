package wrims.engine.core.parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

import org.antlr.runtime.RecognitionException;

// import com.sun.java.util.collections.Collection;
// import com.sun.java.util.collections.Collections;

import wrims.engine.core.commondata.wresldata.Goal;
import wrims.engine.core.commondata.wresldata.TimeArray;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.evaluator.EvalConstraint;
import wrims.engine.core.evaluator.EvaluatorParser;
import wrims.engine.core.evaluator.ValueEvaluatorParser;

public class ProcessConstraint extends RecursiveTask<Integer>{
 
	private int threshold;
	private int start;
	private int end;
	private ArrayList<String> gList;
	private Map<String, Goal> gMap;
	private ConcurrentHashMap<String, EvalConstraint> solverGMap;
    private ArrayList<String> timeArrayDvList;
    private ArrayList<String> gTimeArrayList;
	private CopyOnWriteArrayList<String> usedWtSlackSurplusList = new CopyOnWriteArrayList<String>();
	private CopyOnWriteArrayList<String> usedWtSlackSurplusDvList = new CopyOnWriteArrayList<String>();
	
    public ProcessConstraint(ArrayList<String> gList, Map<String, Goal> gMap, ConcurrentHashMap<String, EvalConstraint> solverGMap, ArrayList<String> gTimeArrayList, CopyOnWriteArrayList<String> usedWtSlackSurplusList, CopyOnWriteArrayList<String> usedWtSlackSurplusDvList, int start, int end) {
        this.start = start;
        this.end = end;
        this.gList=gList;
        this.gMap=gMap;
        this.solverGMap=solverGMap;
        this.gTimeArrayList=gTimeArrayList;
        this.usedWtSlackSurplusList=usedWtSlackSurplusList;
        this.usedWtSlackSurplusDvList=usedWtSlackSurplusDvList;
		threshold=(int) Math.ceil(gList.size()*1.0/ControlData.nThreads);
    }
 
	@Override
	protected Integer compute() {
        if (end - start < threshold) {
            return computeDirectly();
        } else {
            ArrayList<ProcessConstraint> subTasks=new ArrayList<ProcessConstraint>(ControlData.nThreads);
            
            for (int i=0; i<ControlData.nThreads; i++){
            	int subStart, subEnd;
            	subStart=i*threshold;
            	if (i==ControlData.nThreads-1){
            		subEnd=end;
            	}else{
            		subEnd=Math.min(end, (i+1)*threshold-1);
            	}
            	subTasks.add(new ProcessConstraint(gList, gMap, solverGMap, gTimeArrayList, usedWtSlackSurplusList, usedWtSlackSurplusDvList, subStart, subEnd));
            }
            
            for(ProcessConstraint subtask : subTasks){
                subtask.fork();
            }
 
            //ProcessDvar subTask1 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, start, middle);
            //ProcessDvar subTask2 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, middle, end);

            int sum=0;
            for (int i=0; i<ControlData.nThreads; i++){
            	sum=sum+subTasks.get(i).join();
            }
            return sum;
        }
    }
 
    protected int computeDirectly() {
    	for (int ii=start; ii<=end; ii++){
    		String goalName=gList.get(ii);
			ControlData.currEvalName=goalName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing constraint "+goalName);
			Goal goal=gMap.get(goalName);
			ArrayList<ValueEvaluatorParser> caseConditions=goal.caseConditionParsers;
			
			int timeArraySize=new TimeArray().getTimeArraySize(goal.timeArraySizeParser);
			ParallelVars prvs = new ParallelVars();
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				Goal newGoal=new Goal();
				String newGoalName=goalName+"__fut__"+prvs.timeArrayIndex;
				
				boolean condition=false;
				int i=-1;
				while(!condition && i<=caseConditions.size()-2){
					i=i+1;
					ValueEvaluatorParser caseCondition=caseConditions.get(i);
					caseCondition.setParallelVars(prvs);
					try{
						caseCondition.evaluator();
						condition=caseCondition.evalCondition;
					}catch (Exception e){
						Error.addEvaluationError("Case condition evaluation of time array constraint "+newGoalName+" has error.");
						condition=false;
					}
					caseCondition.reset();
				}
				if (condition){		
					ArrayList<EvaluatorParser> caseExpressions=goal.caseExpressionParsers;
					EvaluatorParser caseExpression=caseExpressions.get(i);
					caseExpression.setParallelVars(prvs);
					try {
						caseExpression.evaluator();
						if (solverGMap.containsKey(newGoalName)){
							Error.addEvaluationError(newGoalName+" is duplicatedly used in both goal and time array goal");
						}else{	
							solverGMap.put(newGoalName,caseExpression.evalConstraint.copyOf());
							gTimeArrayList.add(newGoalName);
						}
					} catch (RecognitionException e) {
						Error.addEvaluationError("Case expression evaluation has error.");
					}
					caseExpression.reset();
					
					// add slack or surplus as dvar and weight
					if (goal.dvarWeightMapList.size()>i && goal.dvarWeightMapList.get(i)!=null ){
						ArrayList<String> dwl = goal.dvarSlackSurplusList.get(i);
						for (int j=0; j<dwl.size();j++){
							String dwlItem=dwl.get(j);
							String usedWtSlackSurplusName=dwlItem+"__fut__"+prvs.timeArrayIndex;
							usedWtSlackSurplusList.add(usedWtSlackSurplusName);
							if (!usedWtSlackSurplusDvList.contains(dwlItem)){
								usedWtSlackSurplusDvList.add(dwlItem);
							}
						}
					}
				}
			}
			
			prvs.timeArrayIndex=0;
			boolean condition=false;
			int i=-1;
			while(!condition && i<=caseConditions.size()-2){
				i=i+1;
				ValueEvaluatorParser caseCondition=caseConditions.get(i);
				caseCondition.setParallelVars(prvs);
				try{
					caseCondition.evaluator();
					condition=caseCondition.evalCondition;
				}catch (Exception e){
					Error.addEvaluationError("Case condition evaluation has error.");
					condition=false;
				}
				caseCondition.reset();
			}
			if (condition){		
				ArrayList<EvaluatorParser> caseExpressions=goal.caseExpressionParsers;
				EvaluatorParser caseExpression=caseExpressions.get(i);	
				caseExpression.setParallelVars(prvs);
				try {
					caseExpression.evaluator();
					solverGMap.put(goalName,caseExpression.evalConstraint);
				} catch (RecognitionException e) {
					Error.addEvaluationError("Case expression evaluation has error.");
				}
				caseExpression.reset();
				
				// add slack or surplus as dvar and weight
				if (goal.dvarWeightMapList.size()>i && goal.dvarWeightMapList.get(i)!=null ){
					ArrayList<String> dwl = goal.dvarSlackSurplusList.get(i);
					usedWtSlackSurplusList.addAll(dwl);
					for (int j=0; j<dwl.size();j++){
						String dwlItem=dwl.get(j);
						if (!usedWtSlackSurplusDvList.contains(dwlItem)){
							usedWtSlackSurplusDvList.add(dwlItem);
						}
					}
					
				}
			}
		}
		
    	return 1;
    }
}
