package wrimsv2.commondata.wresldata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.components.ControlData;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.EvalConstraint;
import wrimsv2.evaluator.TimeOperation;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.parallel.ParallelVars;
import wrimsv2.parallel.ProcessConstraint;
import wrimsv2.parallel.ProcessDvar;
import wrimsv2.parallel.ProcessTimeseries;
import wrimsv2.parallel.ProcessWeight;
import wrimsv2.parallel.ProcessWeightSurplusSlack;

import org.antlr.runtime.RecognitionException;

import wrimsv2.components.Error;


public class ModelDataSet implements Serializable {
	
	private static final long serialVersionUID = 1L;
	// / weight table   // <objName,  <itemName, value>>
	public ArrayList<String> wtList = new ArrayList<String>();
	public ArrayList<String> wtTimeArrayList = new ArrayList<String>();
	public ArrayList<String> wtSlackSurplusList = new ArrayList<String>();
	public CopyOnWriteArrayList<String> usedWtSlackSurplusList = new CopyOnWriteArrayList<String>();
	public CopyOnWriteArrayList<String> usedWtSlackSurplusDvList = new CopyOnWriteArrayList<String>();

//	public ArrayList<String> wtList_global = new ArrayList<String>();
//	public ArrayList<String> wtList_local = new ArrayList<String>();
	public Map<String, WeightElement> wtMap = new HashMap<String, WeightElement>();
	public Map<String, WeightElement> wtSlackSurplusMap = new HashMap<String, WeightElement>();	

	// / external function structure
	public ArrayList<String> exList = new ArrayList<String>();
	public ArrayList<String> exList_global = new ArrayList<String>();
	public ArrayList<String> exList_local = new ArrayList<String>();
	public Map<String, External> exMap = new HashMap<String, External>();
	
	
//    //  / sv, ts, and dv list
//	public ArrayList<String> svTsDvList = new ArrayList<String>();	
//	
//	//  / sv and ts list
//	public ArrayList<String> svTsList = new ArrayList<String>();
	
	// / svar timeseries data structure
	public ArrayList<String> tsList = new ArrayList<String>();
	public ArrayList<String> tsList_global = new ArrayList<String>();
	public ArrayList<String> tsList_local = new ArrayList<String>();
	public Map<String, Timeseries> tsMap = new HashMap<String, Timeseries>();
	
	// / svar data structure
	public Set<String> svSet_unknown = new HashSet<String>();
	public ArrayList<String> svList = new ArrayList<String>();
	public ArrayList<String> svList_global = new ArrayList<String>();
	public ArrayList<String> svList_local = new ArrayList<String>();
	public Map<String, Svar> svMap = new HashMap<String, Svar>();
	public Map<String, Svar> svFutMap = new HashMap<String, Svar>();

	// / dvar data structure
	public ArrayList<String> dvList = new ArrayList<String>();
	public ArrayList<String> dvList_deviationSlackSurplus = new ArrayList<String>();
	public Map<String,Double> deviationSlackSurplus_toleranceMap = new HashMap<String, Double>();
	public ArrayList<String> dvTimeArrayList = new ArrayList<String>();
	public ArrayList<String> timeArrayDvList = new ArrayList<String>();
	public ArrayList<String> dvSlackSurplusList = new ArrayList<String>();
	public ArrayList<String> dvList_global = new ArrayList<String>();
	public ArrayList<String> dvList_local = new ArrayList<String>();
	public Map<String, Dvar> dvMap = new HashMap<String, Dvar>();
	public Map<String, Dvar> dvSlackSurplusMap = new HashMap<String, Dvar>();

	// / alias data structure
	public Set<String> asSet_unknown = new HashSet<String>();
	public ArrayList<String> asList = new ArrayList<String>();
	public ArrayList<String> asList_global = new ArrayList<String>();
	public ArrayList<String> asList_local = new ArrayList<String>();
	public Map<String, Alias> asMap = new HashMap<String, Alias>();
	public Map<String, Alias> asFutMap = new HashMap<String, Alias>();
	
	// / goal data structure
	public ArrayList<String> gList = new ArrayList<String>();
	public ArrayList<String> gTimeArrayList = new ArrayList<String>();	
	public ArrayList<String> gList_global = new ArrayList<String>();
	public ArrayList<String> gList_local = new ArrayList<String>();
	public Map<String, Goal> gMap = new HashMap<String, Goal>();
	
	public ArrayList<String> incFileList=new ArrayList<String>();
	public ArrayList<String> incFileList_global=new ArrayList<String>();
	public ArrayList<String> incFileList_local=new ArrayList<String>();
	
	public Set<String> varUsedByLaterCycle = new HashSet<String>();
	
	public Set<String> dvarUsedByLaterCycle = new HashSet<String>();
	public Set<String> dvarTimeArrayUsedByLaterCycle = new HashSet<String>();
	public Set<String> svarUsedByLaterCycle = new HashSet<String>();
	public Set<String> aliasUsedByLaterCycle = new HashSet<String>();
		
	public void processModel(){
		resetSlackSurplusWeight(); // this clears slack and surplus vars
		long t1 = Calendar.getInstance().getTimeInMillis();
		processTimeseries();
		if (ControlData.showRunTimeMessage) System.out.println("Process Timeseries Done.");
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_ts=ControlData.t_ts+(int) (t2-t1);
		processSvar();
		if (ControlData.showRunTimeMessage) System.out.println("Process Svar Done.");
		long t3 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_svar=ControlData.t_svar+(int) (t3-t2);
		processDvar();	
		if (ControlData.showRunTimeMessage) System.out.println("Process Dvar Done.");
		long t4 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_dvar=ControlData.t_dvar+(int) (t4-t3);
		processGoal();	
		if (ControlData.showRunTimeMessage) System.out.println("Process Goal Done.");
		long t5 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_goal=ControlData.t_goal+(int) (t5-t4);
		processWeight();
		if (ControlData.showRunTimeMessage) System.out.println("Process Weight Done.");
		long t6 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_wt=ControlData.t_wt+(int) (t6-t5);
		processWeightSlackSurplus();
		if (ControlData.showRunTimeMessage) System.out.println("Process Weight Slack Surplus Done.");
		long t7 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_wtss=ControlData.t_wtss+(int) (t7-t6);
	}

	public void resetSlackSurplusWeight(){
		usedWtSlackSurplusList = new CopyOnWriteArrayList<String>();
	}
	
	public void processWeight(){
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> wtList = mds.wtList;
		Map<String, WeightElement> wtMap =mds.wtMap;
		SolverData.clearWeightMap();
		ConcurrentHashMap<String, WeightElement> solverWtMap=SolverData.getWeightMap();
		ControlData.currEvalTypeIndex=7;
		wtTimeArrayList = new ArrayList<String>();
		ProcessWeight pw = new ProcessWeight(wtList, wtMap, solverWtMap, wtTimeArrayList, 0, wtList.size()-1);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(pw);
	}
	
	public void processWeightSlackSurplus(){
		ModelDataSet mds=ControlData.currModelDataSet;
		CopyOnWriteArrayList<String> usedWtSlackSurplusList = mds.usedWtSlackSurplusList;
		Map<String, WeightElement> wtSlackSurplusMap =mds.wtSlackSurplusMap;
		SolverData.clearWeightSlackSurplusMap();
		ConcurrentHashMap<String, WeightElement> solverWeightSlackSurplusMap=SolverData.getWeightSlackSurplusMap();
		ControlData.currEvalTypeIndex=7;
		ProcessWeightSurplusSlack pwss = new ProcessWeightSurplusSlack(usedWtSlackSurplusDvList, wtSlackSurplusMap, solverWeightSlackSurplusMap, 0, usedWtSlackSurplusDvList.size()-1);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(pwss);
	}
	
	public void processSvar(){
		StudyDataSet sds = ControlData.currStudyDataSet;
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> svList = mds.svList;
		Map<String, Svar> svMap =mds.svMap;
		ControlData.currEvalTypeIndex=0;
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
		Set<String> svarUsedByLaterCycle = mds.svarUsedByLaterCycle;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();
		String model=ControlData.currCycleName;
		for (String svName: svList){
			ControlData.currEvalName=svName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing svar "+svName);
			Svar svar=svMap.get(svName);
			ArrayList<ValueEvaluatorParser> caseConditions=svar.caseConditionParsers;
			ParallelVars prvs = new ParallelVars();
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
				ArrayList<ValueEvaluatorParser> caseExpressions=svar.caseExpressionParsers;
				ValueEvaluatorParser caseExpression=caseExpressions.get(i);
				caseExpression.setParallelVars(prvs);
				try {
					caseExpression.evaluator();
					IntDouble evalValue=caseExpression.evalValue.copyOf();
					svar.setData(evalValue);
					if (svarUsedByLaterCycle.contains(svName)){
						varCycleValueMap.get(svName).put(model, evalValue);
					}
					if (varCycleIndexList.contains(svName)){
						if (varCycleIndexValueMap.containsKey(svName)){
							varCycleIndexValueMap.get(svName).put(model, evalValue);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, evalValue);
							varCycleIndexValueMap.put(svName, cycleValue);
						}
					}
				} catch (RecognitionException e) {
					Error.addEvaluationError("Case expression evaluation has error.");
					IntDouble evalValue=new IntDouble(1.0, false);
					svar.setData(evalValue);
					if (svarUsedByLaterCycle.contains(svName)){
						varCycleValueMap.get(svName).put(model, evalValue);
					}
					if (varCycleIndexList.contains(svName)){
						if (varCycleIndexValueMap.containsKey(svName)){
							varCycleIndexValueMap.get(svName).put(model, evalValue);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, evalValue);
							varCycleIndexValueMap.put(svName, cycleValue);
						}
					}
				}
				caseExpression.reset();
			}else{
				Error.addEvaluationError("None of the case conditions is satisfied.");
				svar.setData(new IntDouble(1.0, false));
			}
			
			int timeArraySize=new TimeArray().getTimeArraySize(svar.timeArraySizeParser);
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				condition=false;
				i=-1;
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
					ArrayList<ValueEvaluatorParser> caseExpressions=svar.caseExpressionParsers;
					ValueEvaluatorParser caseExpression=caseExpressions.get(i);
					caseExpression.setParallelVars(prvs);
					try {
						caseExpression.evaluator();
						IntDouble evalValue=caseExpression.evalValue.copyOf();
						Svar newSvar=new Svar();
						String newSvName=svName+"__fut__"+prvs.timeArrayIndex;
						newSvar.setData(evalValue);
						svFutMap.put(newSvName, newSvar);
						if (svarUsedByLaterCycle.contains(svName)){
							if (varTimeArrayCycleValueMap.containsKey(newSvName)){
								varTimeArrayCycleValueMap.get(newSvName).put(model, evalValue);
							}else{
								Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
								cycleValue.put(model, evalValue);
								varTimeArrayCycleValueMap.put(newSvName, cycleValue);
							}
						}
						if (varCycleIndexList.contains(svName)){
							if (varCycleIndexValueMap.containsKey(newSvName)){
								varCycleIndexValueMap.get(newSvName).put(model, evalValue);
							}else{
								Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
								cycleValue.put(model, evalValue);
								varCycleIndexValueMap.put(newSvName, cycleValue);
							}
						}
					} catch (RecognitionException e) {
						Error.addEvaluationError("Case expression evaluation has error.");
						IntDouble evalValue=new IntDouble(1.0, false);
						Svar newSvar=new Svar();
						String newSvName=svName+"__fut__"+prvs.timeArrayIndex;
						newSvar.setData(evalValue);
						svFutMap.put(newSvName, newSvar);
						if (svarUsedByLaterCycle.contains(svName)){
							if (varTimeArrayCycleValueMap.containsKey(newSvName)){
								varTimeArrayCycleValueMap.get(newSvName).put(model, evalValue);
							}else{
								Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
								cycleValue.put(model, evalValue);
								varTimeArrayCycleValueMap.put(newSvName, cycleValue);
							}
						}
						if (varCycleIndexList.contains(svName)){
							if (varCycleIndexValueMap.containsKey(newSvName)){
								varCycleIndexValueMap.get(newSvName).put(model, evalValue);
							}else{
								Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
								cycleValue.put(model, evalValue);
								varCycleIndexValueMap.put(newSvName, cycleValue);
							}
						}
					}
					caseExpression.reset();
				}else{
					Error.addEvaluationError("None of the case conditions is satisfied.");
					IntDouble evalValue=new IntDouble(1.0, false);
					Svar newSvar=new Svar();
					String newSvName=svName+"__fut__"+prvs.timeArrayIndex;
					newSvar.setData(evalValue);
					svFutMap.put(newSvName, newSvar);
				}
			}
		}
	}
		
	public void processTimeseries(){
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> tsList = mds.tsList;
		Map<String, Timeseries> tsMap =mds.tsMap;
		ControlData.currEvalTypeIndex=5;
		ProcessTimeseries pt = new ProcessTimeseries(tsList, tsMap, 0, tsList.size()-1);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(pt);
	}
	
	public void processDvar(){
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> dvList = mds.dvList;
		Map<String, Dvar> dvMap =mds.dvMap;
		SolverData.clearDvarMap();
		ConcurrentHashMap<String, Dvar> solverDvarMap=SolverData.getDvarMap();
		ControlData.currDvMap=dvMap;
		ControlData.currEvalTypeIndex=1;
		dvTimeArrayList = new ArrayList<String>();
		timeArrayDvList = new ArrayList<String>();
		dvarTimeArrayUsedByLaterCycle = new HashSet<String>();
		StudyDataSet sds = ControlData.currStudyDataSet;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
		ProcessDvar pd = new ProcessDvar(dvList, dvMap, solverDvarMap, timeArrayDvList, dvTimeArrayList, dvarUsedByLaterCycle, dvarTimeArrayUsedByLaterCycle, varCycleIndexList, dvarTimeArrayCycleIndexList, 0, dvList.size()-1);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(pd);
	}
	
	public void processGoal(){
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> gList = mds.gList;
		Map<String, Goal> gMap =mds.gMap;
		ControlData.currEvalTypeIndex=3;
		SolverData.clearConstraintDataMap();
		ConcurrentHashMap<String, EvalConstraint> solverGMap=SolverData.getConstraintDataMap();
		gTimeArrayList = new ArrayList<String>();
		ProcessConstraint pc = new ProcessConstraint(gList, gMap, solverGMap, gTimeArrayList, usedWtSlackSurplusList, usedWtSlackSurplusDvList, 0, gList.size()-1);
		ForkJoinPool pool = new ForkJoinPool();
		pool.invoke(pc);
	}
	
	public void processAlias(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		StudyDataSet sds = ControlData.currStudyDataSet;
		ModelDataSet mds=ControlData.currModelDataSet;
		ArrayList<String> asList = mds.asList;
		Map<String, Alias> asMap =mds.asMap;
		ControlData.currEvalTypeIndex=2;
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
		Set<String> aliasUsedByLaterCycle = mds.aliasUsedByLaterCycle;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();
		String model=ControlData.currCycleName;
		for (String asName: asList){
			ControlData.currEvalName=asName;
			if (ControlData.showRunTimeMessage) System.out.println("Processing alias "+asName);
			Alias alias=asMap.get(asName);
			
			ValueEvaluatorParser evaluator = alias.expressionParser;
			ParallelVars prvs = new ParallelVars();
			evaluator.setParallelVars(prvs);
			prvs.timeArrayIndex=0;
			try {
				evaluator.evaluator();
				IntDouble id=evaluator.evalValue;
				alias.data=id.copyOf();
				if (aliasUsedByLaterCycle.contains(asName)){
					varCycleValueMap.get(asName).put(model, alias.data);
				}
				if (varCycleIndexList.contains(asName)){
					if (varCycleIndexValueMap.containsKey(asName)){
						varCycleIndexValueMap.get(asName).put(model, alias.data);
					}else{
						Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
						cycleValue.put(model, alias.data);
						varCycleIndexValueMap.put(asName, cycleValue);
					}
				}
				String entryNameTS=DssOperation.entryNameTS(asName, ControlData.timeStep);
				if (!DataTimeSeries.dvAliasTS.containsKey(entryNameTS)){
					DssDataSetFixLength dds=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
					//double[] data=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
					//dds.setData(data);
					dds.setTimeStep(ControlData.partE);
					dds.setStartTime(ControlData.memStartDate);
					dds.setUnits(alias.units);
					dds.setKind(alias.kind);
					DataTimeSeries.dvAliasTS.put(entryNameTS,dds);
				}
				DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
				double[] dataList=ddsfl.getData();
				//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=id.getData().doubleValue();
				//Date memStartDate = new Date(ControlData.memStartYear-1900, ControlData.memStartMonth-1, ControlData.memStartDay);
				Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
				int index=TimeOperation.getNumberOfTimestep(ControlData.memStartDate, currDate, ddsfl.getTimeStep())-1;
				dataList[index]=id.getData().doubleValue();
				
				//if (ControlData.outputCycleToDss){
				HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
				if (!dvAliasTSCycle.containsKey(entryNameTS)){
					DssDataSetFixLength dds1=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
					//double[] data1=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
					//dds1.setData(data1);
					dds1.setTimeStep(ControlData.partE);
					dds1.setStartTime(ControlData.memStartDate);
					dds1.setUnits(alias.units);
					dds1.setKind(alias.kind);
					dvAliasTSCycle.put(entryNameTS,dds1);
				}
				double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
				//dataList1[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=id.getData().doubleValue();
				dataList1[index]=id.getData().doubleValue();
				//}
			} catch (RecognitionException e) {
				Error.addEvaluationError("Alias evaluation has error.");
				IntDouble id=new IntDouble(-901.0,false);
				alias.data=id;
				if (aliasUsedByLaterCycle.contains(asName)){
					varCycleValueMap.get(asName).put(model, id);
				}
				if (varCycleIndexList.contains(asName)){
					if (varCycleIndexValueMap.containsKey(asName)){
						varCycleIndexValueMap.get(asName).put(model, alias.data);
					}else{
						Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
						cycleValue.put(model, alias.data);
						varCycleIndexValueMap.put(asName, cycleValue);
					}
				}
				String entryNameTS=DssOperation.entryNameTS(asName, ControlData.timeStep);
				DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
				double[] dataList=ddsfl.getData();
				//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=-901.0;
				//Date memStartDate = new Date(ControlData.memStartYear-1900, ControlData.memStartMonth-1, ControlData.memStartDay);
				Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
				int index=TimeOperation.getNumberOfTimestep(ControlData.memStartDate, currDate, ddsfl.getTimeStep())-1;
				dataList[index]=-901.0;
				
				//if (ControlData.outputCycleToDss){
				HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
				double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
				//dataList1[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=-901.0;
				dataList1[index]=-901.0;
				//}
			}
			evaluator.reset();
			
			if (!alias.timeArraySize.equals("0")){
				String newAsName=asName+"__fut__0";
				String entryNameTS=DssOperation.entryNameTS(newAsName, ControlData.timeStep);
				if (!DataTimeSeries.dvAliasTS.containsKey(entryNameTS)){
					DssDataSetFixLength dds=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
					//double[] data=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
					//dds.setData(data);
					dds.setTimeStep(ControlData.partE);
					dds.setStartTime(ControlData.memStartDate);
					dds.setUnits(alias.units);
					dds.setKind(alias.kind);
					DataTimeSeries.dvAliasTS.put(entryNameTS,dds);
				}
				DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
				double[] dataList=ddsfl.getData();
				//Date memStartDate = new Date(ControlData.memStartYear-1900, ControlData.memStartMonth-1, ControlData.memStartDay);
				Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
				int index=TimeOperation.getNumberOfTimestep(ControlData.memStartDate, currDate, ddsfl.getTimeStep())-1;
				//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=alias.data.getData().doubleValue();
				dataList[index]=alias.data.getData().doubleValue();
				
				//if (ControlData.outputCycleToDss){
				HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
				if (!dvAliasTSCycle.containsKey(entryNameTS)){
					DssDataSetFixLength dds1=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
					//double[] data1=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
					//dds1.setData(data1);
					dds1.setTimeStep(ControlData.partE);
					dds1.setStartTime(ControlData.memStartDate);
					dds1.setUnits(alias.units);
					dds1.setKind(alias.kind);
					dvAliasTSCycle.put(entryNameTS,dds1);
				}
				double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
				//dataList1[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=alias.data.getData().doubleValue();
				dataList1[index]=alias.data.getData().doubleValue();
				//}
			}
			
			int timeArraySize=new TimeArray().getTimeArraySize(alias.timeArraySizeParser);
			for (prvs.timeArrayIndex=1; prvs.timeArrayIndex<=timeArraySize; prvs.timeArrayIndex++){
				String newAsName=asName+"__fut__"+prvs.timeArrayIndex;
				try {
					evaluator.evaluator();
					IntDouble id=evaluator.evalValue;
					Alias newAlias=new Alias();
					newAlias.data=id.copyOf();
					asFutMap.put(newAsName, newAlias);
					if (aliasUsedByLaterCycle.contains(asName)){
						if (varTimeArrayCycleValueMap.containsKey(newAsName)){
							varTimeArrayCycleValueMap.get(newAsName).put(model, newAlias.data);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, newAlias.data);
							varTimeArrayCycleValueMap.put(newAsName, cycleValue);
						}
					}
					if (varCycleIndexList.contains(asName)){
						if (varCycleIndexValueMap.containsKey(newAsName)){
							varCycleIndexValueMap.get(newAsName).put(model, newAlias.data);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, newAlias.data);
							varCycleIndexValueMap.put(newAsName, cycleValue);
						}
					}
					String entryNameTS=DssOperation.entryNameTS(newAsName, ControlData.timeStep);
					if (!DataTimeSeries.dvAliasTS.containsKey(entryNameTS)){
						DssDataSetFixLength dds=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
						//double[] data=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
						//dds.setData(data);
						dds.setTimeStep(ControlData.partE);
						dds.setStartTime(ControlData.memStartDate);
						dds.setUnits(alias.units);
						dds.setKind(alias.kind);
						DataTimeSeries.dvAliasTS.put(entryNameTS,dds);
					}
					DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
					double[] dataList=ddsfl.getData();
					//Date memStartDate = new Date(ControlData.memStartYear-1900, ControlData.memStartMonth-1, ControlData.memStartDay);
					Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
					int indext=TimeOperation.getNumberOfTimestep(ControlData.memStartDate, currDate, ddsfl.getTimeStep())-1;
					//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=id.getData().doubleValue();
					dataList[indext]=id.getData().doubleValue();
					
					//if (ControlData.outputCycleToDss){
					HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
					if (!dvAliasTSCycle.containsKey(entryNameTS)){
						DssDataSetFixLength dds1=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
						//double[] data1=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
						//dds1.setData(data1);
						dds1.setTimeStep(ControlData.partE);
						dds1.setStartTime(ControlData.memStartDate);
						dds1.setUnits(alias.units);
						dds1.setKind(alias.kind);
						dvAliasTSCycle.put(entryNameTS,dds1);
					}
					double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
					//dataList1[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=id.getData().doubleValue();
					dataList1[indext]=id.getData().doubleValue();
					//}
					
					String asEntryNameTS=DssOperation.entryNameTS(asName, ControlData.timeStep);
					double[] asDataList=DataTimeSeries.dvAliasTS.get(asEntryNameTS).getData();
					//int index=ControlData.currTimeStep.get(ControlData.currCycleIndex)+ParallelVars.timeArrayIndex;
					int index=indext+prvs.timeArrayIndex;
					if (index<asDataList.length) asDataList[index]=id.getData().doubleValue();
					
					//if (ControlData.outputCycleToDss){
					//dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
					double[] asDataList1=dvAliasTSCycle.get(asEntryNameTS).getData();
					//int index1=ControlData.currTimeStep.get(ControlData.currCycleIndex)+ParallelVars.timeArrayIndex;
					int index1=indext+prvs.timeArrayIndex;
					if (index1<asDataList1.length) asDataList1[index1]=id.getData().doubleValue();
					//}
				} catch (RecognitionException e) {
					Error.addEvaluationError("Alias evaluation has error.");
					IntDouble id=new IntDouble(-901.0,false);
					Alias newAlias=new Alias();
					newAlias.data=id;
					if (aliasUsedByLaterCycle.contains(asName)){
						if (varTimeArrayCycleValueMap.containsKey(newAsName)){
							varTimeArrayCycleValueMap.get(newAsName).put(model, id);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, id);
							varTimeArrayCycleValueMap.put(newAsName, cycleValue);
						}
					}
					if (varCycleIndexList.contains(asName)){
						if (varCycleIndexValueMap.containsKey(newAsName)){
							varCycleIndexValueMap.get(newAsName).put(model, id);
						}else{
							Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
							cycleValue.put(model, id);
							varCycleIndexValueMap.put(newAsName, cycleValue);
						}
					}
					String entryNameTS=DssOperation.entryNameTS(newAsName, ControlData.timeStep);
					DssDataSetFixLength ddsfl = DataTimeSeries.dvAliasTS.get(entryNameTS);
					double[] dataList=ddsfl.getData();
					//Date memStartDate = new Date(ControlData.memStartYear-1900, ControlData.memStartMonth-1, ControlData.memStartDay);
					Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
					int indext=TimeOperation.getNumberOfTimestep(ControlData.memStartDate, currDate, ddsfl.getTimeStep())-1;
					//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=-901.0;
					dataList[indext]=-901.0;
					
					//if (ControlData.outputCycleToDss){
					HashMap<String, DssDataSetFixLength> dvAliasTSCycle = DataTimeSeries.dvAliasTSCycles.get(ControlData.currCycleIndex);
					double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
					//dataList1[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=-901.0;
					dataList1[indext]=-901.0;
					//}
				}
				evaluator.reset();
			}
		}
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_as=ControlData.t_as+(int) (t2-t1);
	}
	
	public void clearFutureSvMap(){
		svFutMap = new HashMap<String, Svar>();
	}
	
	public void clearFutureAsMap(){
		asFutMap = new HashMap<String, Alias>();
	}
}

