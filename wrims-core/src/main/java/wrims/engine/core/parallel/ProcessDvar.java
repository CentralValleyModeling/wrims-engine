package wrims.engine.core.parallel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

import org.antlr.runtime.RecognitionException;

import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.TimeArray;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.evaluator.ValueEvaluatorParser;

public class ProcessDvar extends RecursiveTask<Integer>{
 
	private int threshold;
	private int start;
	private int end;
	private ArrayList<String> dvList;
	private Map<String, Dvar> dvMap;
	private ConcurrentHashMap<String, Dvar> solverDvarMap;
    private ArrayList<String> timeArrayDvList;
    private ArrayList<String> dvTimeArrayList;
    private Set<String> dvarUsedByLaterCycle = new HashSet<String>();
    private Set<String> dvarTimeArrayUsedByLaterCycle;
	private ArrayList<String> varCycleIndexList;
	private ArrayList<String> dvarTimeArrayCycleIndexList;
	
    public ProcessDvar(ArrayList<String> dvList, Map<String, Dvar> dvMap, ConcurrentHashMap<String, Dvar> solverDvarMap, ArrayList<String> timeArrayDvList, ArrayList<String> dvTimeArrayList2, Set<String> dvarUsedByLaterCycle, Set<String> dvarTimeArrayUsedByLaterCycle, ArrayList<String> varCycleIndexList, ArrayList<String> dvarTimeArrayCycleIndexList, int start, int end) {
        this.start = start;
        this.end = end;
        this.dvList=dvList;
        this.dvMap=dvMap;
        this.solverDvarMap=solverDvarMap;
        this.timeArrayDvList=timeArrayDvList;
        this.dvTimeArrayList = dvTimeArrayList2;
        this.dvarUsedByLaterCycle=dvarUsedByLaterCycle;
        this.dvarTimeArrayUsedByLaterCycle=dvarTimeArrayUsedByLaterCycle;
        this.varCycleIndexList=varCycleIndexList;
        this.dvarTimeArrayCycleIndexList=dvarTimeArrayCycleIndexList;
		threshold=(int) Math.ceil(dvList.size()*1.0/ControlData.nThreads);
    }
 
	@Override
	protected Integer compute() {
        if (end - start < threshold) {
            return computeDirectly();
        } else {
            ArrayList<ProcessDvar> subTasks=new ArrayList<ProcessDvar>(ControlData.nThreads);
            
            for (int i=0; i<ControlData.nThreads; i++){
            	int subStart, subEnd;
            	subStart=i*threshold;
            	if (i==ControlData.nThreads-1){
            		subEnd=end;
            	}else{
            		subEnd=Math.min(end, (i+1)*threshold-1);
            	}
            	subTasks.add(new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, subStart, subEnd));
            }
 
            //ProcessDvar subTask1 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, start, middle);
            //ProcessDvar subTask2 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, middle, end);
 
            for(ProcessDvar subtask : subTasks){
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
			String dvName = dvList.get(ii);
			ControlData.currEvalName=dvName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing dvar "+dvName);
			Dvar dvar=dvMap.get(dvName);
		
			ValueEvaluatorParser evaluator=dvar.lowerBoundParser;
			ParallelVars prvs = new ParallelVars();
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				dvar.lowerBoundValue=evaluator.evalValue.getData().doubleValue();
			} catch (RecognitionException e) {
				Error.addEvaluationError("Lowerbound evaluation has error.");
				dvar.lowerBoundValue=-901.0;
			}
			evaluator.reset();
			
			evaluator =dvar.upperBoundParser;
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				dvar.upperBoundValue=evaluator.evalValue.getData().doubleValue();
			} catch (RecognitionException e) {
				Error.addEvaluationError("Lowerbound evaluation has error.");
				dvar.lowerBoundValue=-901.0;
			}
			evaluator.reset();
			solverDvarMap.put(dvName, dvar);
			
			int timeArraySize=new TimeArray().getTimeArraySize(dvar.timeArraySizeParser);
			if (!dvar.timeArraySize.equals("0") && !timeArrayDvList.contains(dvName)){
				timeArrayDvList.add(dvName);
			
				for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
					Dvar newDvar=new Dvar();
					String newDvarName = dvName+"__fut__"+prvs.timeArrayIndex;
					newDvar.kind=dvar.kind;
					newDvar.units=dvar.units;
					newDvar.integer=dvar.integer;
			
					evaluator=dvar.lowerBoundParser;
					evaluator.setParallelVars(prvs);
					try {
						evaluator.evaluator();
						newDvar.lowerBoundValue=evaluator.evalValue.getData().doubleValue();
					} catch (RecognitionException e) {
						Error.addEvaluationError("Lowerbound evaluation of time array dvar has error.");
						newDvar.lowerBoundValue=-901.0;
					}
					evaluator.reset();
			
					evaluator =dvar.upperBoundParser;
					evaluator.setParallelVars(prvs);
					try {
						evaluator.evaluator();
						newDvar.upperBoundValue=evaluator.evalValue.getData().doubleValue();
					} catch (RecognitionException e) {
						Error.addEvaluationError("Lowerbound evaluation of time array dvar has error.");
						newDvar.lowerBoundValue=-901.0;
					}
					evaluator.reset();
					if (solverDvarMap.containsKey(newDvarName)){
						Error.addEvaluationError(newDvarName+" is duplicatedly used in both dvar and time array dvar");
					}else{
						solverDvarMap.put(newDvarName, newDvar);
						dvTimeArrayList.add(newDvarName);
					}
			
					if (dvarUsedByLaterCycle.contains(dvName)){
						dvarTimeArrayUsedByLaterCycle.add(newDvarName);
					}
					if (varCycleIndexList.contains(dvName) && !dvarTimeArrayCycleIndexList.contains(newDvarName)){
						dvarTimeArrayCycleIndexList.add(newDvarName);
					}
				}
			}
		}
		
    	return 1;
    }
}
