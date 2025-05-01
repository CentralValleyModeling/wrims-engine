package wrimsv2.parallel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReentrantLock;

import org.antlr.runtime.RecognitionException;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.TimeArray;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.evaluator.WeightEval;
import wrimsv2.tools.General;

public class ProcessWeight extends RecursiveTask<Integer>{
 
	private int threshold;
	private int start;
	private int end;
	private ArrayList<String> wtList;
	private Map<String, WeightElement> wtMap;
	private ConcurrentHashMap<String, WeightElement> solverWtMap;
    private ArrayList<String> wtTimeArrayList;
	
    public ProcessWeight(ArrayList<String> wtList, Map<String, WeightElement> wtMap, ConcurrentHashMap<String, WeightElement> solverWtMap, ArrayList<String> wtTimeArrayList, int start, int end) {
        this.start = start;
        this.end = end;
        this.wtList=wtList;
        this.wtMap=wtMap;
        this.solverWtMap=solverWtMap;
        this.wtTimeArrayList=wtTimeArrayList;
		threshold=(int) Math.ceil(wtList.size()*1.0/ControlData.nThreads);
    }
 
	@Override
	protected Integer compute() {
        if (end - start < threshold) {
            return computeDirectly();
        } else {
            ArrayList<ProcessWeight> subTasks=new ArrayList<ProcessWeight>(ControlData.nThreads);
            
            for (int i=0; i<ControlData.nThreads; i++){
            	int subStart, subEnd;
            	subStart=i*threshold;
            	if (i==ControlData.nThreads-1){
            		subEnd=end;
            	}else{
            		subEnd=Math.min(end, (i+1)*threshold-1);
            	}
            	subTasks.add(new ProcessWeight(wtList, wtMap, solverWtMap, wtTimeArrayList, subStart, subEnd));
            }
 
            //ProcessDvar subTask1 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, start, middle);
            //ProcessDvar subTask2 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, middle, end);
 
            for(ProcessWeight subtask : subTasks){
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
    		String wtName=wtList.get(ii);
			ControlData.currEvalName=wtName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing weight "+wtName);
			WeightElement wt=wtMap.get(wtName);
			ValueEvaluatorParser evaluator=wt.weightParser;
			ParallelVars prvs = new ParallelVars();
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				wt.setValue(evaluator.evalValue.getData().doubleValue());
				WeightEval.collectWtRT(wtName, wt);
			} catch (RecognitionException e) {
				Error.addEvaluationError("weight definition has error");
				wt.setValue(0.0);
			}
			solverWtMap.put(wtName,wt);
			evaluator.reset();
			
			int timeArraySize=new TimeArray().getTimeArraySize(wt.timeArraySizeParser);
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				WeightElement newWt=new WeightElement();
				String newWtName=wtName+"__fut__"+prvs.timeArrayIndex;
				try {
					evaluator.evaluator();
					newWt.setValue(evaluator.evalValue.getData().doubleValue());
					WeightEval.collectWtRT(newWtName, newWt);
				} catch (RecognitionException e) {
					Error.addEvaluationError("time array weight definition "+newWtName+" has error");
					newWt.setValue(0.0);
				}
				if (solverWtMap.containsKey(newWtName)){
					Error.addEvaluationError(newWtName+" is duplicatedly used in both weight and time array weight");
				}else{
					solverWtMap.put(newWtName,newWt);
					wtTimeArrayList.add(newWtName);
				}
				evaluator.reset();
			}
		}
		
    	return 1;
    }
}
