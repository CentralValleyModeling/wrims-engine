package wrims.engine.core.parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;

import org.antlr.runtime.RecognitionException;

import wrims.engine.core.commondata.wresldata.TimeArray;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.evaluator.ValueEvaluatorParser;
import wrims.engine.core.evaluator.WeightEval;

public class ProcessWeightSurplusSlack extends RecursiveTask<Integer>{
 
	private int threshold;
	private int start;
	private int end;
	private CopyOnWriteArrayList<String> usedWtSlackSurplusDvList;
	private Map<String, WeightElement> wtSlackSurplusMap;
	private ConcurrentHashMap<String, WeightElement> solverWeightSlackSurplusMap;
	
    public ProcessWeightSurplusSlack(CopyOnWriteArrayList<String> usedWtSlackSurplusDvList, Map<String, WeightElement> wtSlackSurplusMap, ConcurrentHashMap<String, WeightElement> solverWeightSlackSurplusMap, int start, int end) {
        this.start = start;
        this.end = end;
        this.usedWtSlackSurplusDvList=usedWtSlackSurplusDvList;
        this.wtSlackSurplusMap=wtSlackSurplusMap;
        this.solverWeightSlackSurplusMap=solverWeightSlackSurplusMap;
		threshold=(int) Math.ceil(usedWtSlackSurplusDvList.size()*1.0/ControlData.nThreads);
    }
 
	@Override
	protected Integer compute() {
        if (end - start < threshold) {
            return computeDirectly();
        } else {
            ArrayList<ProcessWeightSurplusSlack> subTasks=new ArrayList<ProcessWeightSurplusSlack>(ControlData.nThreads);
            
            for (int i=0; i<ControlData.nThreads; i++){
            	int subStart, subEnd;
            	subStart=i*threshold;
            	if (i==ControlData.nThreads-1){
            		subEnd=end;
            	}else{
            		subEnd=Math.min(end, (i+1)*threshold-1);
            	}
            	subTasks.add(new ProcessWeightSurplusSlack(usedWtSlackSurplusDvList, wtSlackSurplusMap, solverWeightSlackSurplusMap, subStart, subEnd));
            }
 
            //ProcessDvar subTask1 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, start, middle);
            //ProcessDvar subTask2 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, middle, end);
 
            for(ProcessWeightSurplusSlack subtask : subTasks){
                subtask.fork();
            }
            
            int sum=0;
            for (int i=0; i<ControlData.nThreads; i++){
            	sum=sum+subTasks.get(i).join();
            }
            return sum;
        }
    }
 
    protected int computeDirectly() {
    	for (int ii=start; ii<=end; ii++){
    		String wtSlackSurplusName= usedWtSlackSurplusDvList.get(ii);
    		ControlData.currEvalName=wtSlackSurplusName;
    		if (ControlData.showRunTimeMessage) System.out.println("Processing weight "+wtSlackSurplusName);
   			WeightElement wtSlackSurplus=wtSlackSurplusMap.get(wtSlackSurplusName);
   			ValueEvaluatorParser evaluator=wtSlackSurplus.weightParser;
   			ParallelVars prvs = new ParallelVars();
   			evaluator.setParallelVars(prvs);
   			prvs.timeArrayIndex=0;
   			try {
   				evaluator.evaluator();
   				wtSlackSurplus.setValue(evaluator.evalValue.getData().doubleValue());
   				WeightEval.collectWtRT(wtSlackSurplusName, wtSlackSurplus);
   			} catch (RecognitionException e) {
   				Error.addEvaluationError("slack surplus weight definition has error");
   				wtSlackSurplus.setValue(0.0);
   			}
   			solverWeightSlackSurplusMap.put(wtSlackSurplusName, wtSlackSurplus);
   			evaluator.reset();
    		
   			int timeArraySize=new TimeArray().getTimeArraySize(wtSlackSurplus.timeArraySizeParser);
   			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
   				WeightElement newWtSlackSurplus=new WeightElement();
   				String newWtSlackSurplusName=wtSlackSurplusName+"__fut__"+prvs.timeArrayIndex;
   				try {
   					evaluator.evaluator();
   					newWtSlackSurplus.setValue(evaluator.evalValue.getData().doubleValue());
   					WeightEval.collectWtRT(newWtSlackSurplusName, newWtSlackSurplus);
   				} catch (RecognitionException e) {
   					Error.addEvaluationError("time array slack surplus weight definition "+newWtSlackSurplusName+" has error");
   					newWtSlackSurplus.setValue(0.0);
   				}
   				if (solverWeightSlackSurplusMap.containsKey(newWtSlackSurplusName)){
   					Error.addEvaluationError(newWtSlackSurplusName+" is duplicatedly used in both slack surplus weight and time array slack surplus weight");
   				}else{
   					solverWeightSlackSurplusMap.put(newWtSlackSurplusName,newWtSlackSurplus);
   				}
   				evaluator.reset();
   			}
    	}
		
    	return 1;
    }
}
