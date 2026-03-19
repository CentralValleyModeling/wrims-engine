package gov.ca.water.wrims.engine.core.parallel;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Timeseries;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.Evaluation;

public class ProcessTimeseries extends RecursiveTask<Integer>{
 
	private int threshold;
	private int start;
	private int end;
	private ArrayList<String> tList;
	private Map<String, Timeseries> tMap;
	
    public ProcessTimeseries(ArrayList<String> tList, Map<String, Timeseries> tMap, int start, int end) {
        this.start = start;
        this.end = end;
        this.tList=tList;
        this.tMap=tMap;
		threshold=(int) Math.ceil(tList.size()*1.0/ControlData.nThreads);
    }
 
	@Override
	protected Integer compute() {
        if (end - start < threshold) {
            return computeDirectly();
        } else {
            ArrayList<ProcessTimeseries> subTasks=new ArrayList<ProcessTimeseries>(ControlData.nThreads);
            
            for (int i=0; i<ControlData.nThreads; i++){
            	int subStart, subEnd;
            	subStart=i*threshold;
            	if (i==ControlData.nThreads-1){
            		subEnd=end;
            	}else{
            		subEnd=Math.min(end, (i+1)*threshold-1);
            	}
            	subTasks.add(new ProcessTimeseries(tList, tMap, subStart, subEnd));
            }
 
            //ProcessDvar subTask1 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, start, middle);
            //ProcessDvar subTask2 = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, middle, end);
 
            for(ProcessTimeseries subtask : subTasks){
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
    		String tsName=tList.get(ii);
			ControlData.currEvalName=tsName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing timeseries "+tsName);
			Timeseries ts=tMap.get(tsName);
			ts.setData(new IntDouble(Evaluation.timeseries(tsName),false));
		}		
    	return 1;
    }
}
