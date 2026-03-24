package gov.ca.water.wrims.engine.core.solver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.commondata.solverdata.*;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;

public class XASolver {
    private static final Logger LOG = LoggerFactory.getLogger(XASolver.class);

	int modelStatus;
	int assignedDvarCount;
	double objectiveValue = Double.NaN;

    private static XaSolverPerformanceStatistics performanceStats = new XaSolverPerformanceStatistics();

	private long lastSetConstraintsMs = 0L;
	private long lastSetDVarsMs = 0L;
	private long lastSetWeightsMs = 0L;
	private int lastIntegerDvarCount = 0;
	private int lastContinuousDvarCount = 0;
	private int lastEqualityConstraintCount = 0;
	private int lastInequalityConstraintCount = 0;

	public XASolver(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		long tLoadModel = 0L;
		long tSetConstraints = 0L;
		long tSetDVars = 0L;
		long tSetWeights = 0L;
		long tSolve = 0L;
		long tAssign = 0L;
		long s;

		boolean assignDone = false;
		String solverStatus = null;
		String solvePath = "solveWithInfeasibleAnalysis";
		int ci=ControlData.currCycleIndex+1;
		int dvarCount = 0;
		int integerDvarCount = 0;
		int continuousDvarCount = 0;
		int weightCount = 0;
		int constraintCount = 0;
		int equalityConstraintCount = 0;
		int inequalityConstraintCount = 0;

		PerformanceTimer totalTimer = new PerformanceTimerXa("XA.total");
		PerformanceTimer solveTimer = null;

		if (ControlData.currModelDataSet != null) {
			if (ControlData.currModelDataSet.dvList != null) dvarCount += ControlData.currModelDataSet.dvList.size();
			if (ControlData.currModelDataSet.dvTimeArrayList != null) dvarCount += ControlData.currModelDataSet.dvTimeArrayList.size();
			if (ControlData.currModelDataSet.wtList != null) weightCount += ControlData.currModelDataSet.wtList.size();
			if (ControlData.currModelDataSet.wtTimeArrayList != null) weightCount += ControlData.currModelDataSet.wtTimeArrayList.size();
			if (ControlData.currModelDataSet.usedWtSlackSurplusList != null) weightCount += ControlData.currModelDataSet.usedWtSlackSurplusList.size();
			if (ControlData.currModelDataSet.gList != null) constraintCount += ControlData.currModelDataSet.gList.size();
			if (ControlData.currModelDataSet.gTimeArrayList != null) constraintCount += ControlData.currModelDataSet.gTimeArrayList.size();
		}

		LOG.atDebug().setMessage("==================== XA Problem Solving Session Start ====================").log();

		s = Calendar.getInstance().getTimeInMillis();
		ControlData.xasolver.loadNewModel();
		tLoadModel = Calendar.getInstance().getTimeInMillis() - s;

		setConstraints();
		tSetConstraints = lastSetConstraintsMs;
		equalityConstraintCount = lastEqualityConstraintCount;
		inequalityConstraintCount = lastInequalityConstraintCount;
		LOG.atDebug().setMessage("XA constraint setup complete: total={} equality={} inequality={}").addArgument(constraintCount).addArgument(equalityConstraintCount).addArgument(inequalityConstraintCount).log();

		setDVars();
		tSetDVars = lastSetDVarsMs;
		integerDvarCount = lastIntegerDvarCount;
		continuousDvarCount = lastContinuousDvarCount;
		LOG.atDebug().setMessage("XA dvar setup complete: total={} continuous={} integer={}").addArgument(dvarCount).addArgument(continuousDvarCount).addArgument(integerDvarCount).log();

		setWeights();
		tSetWeights = lastSetWeightsMs;
		LOG.atDebug().setMessage("XA weight setup complete: total={}").addArgument(weightCount).log();

		LOG.atDebug().setMessage("New XA Problem Created: cycle={} [{}], date={}/{}/{}, solvePath={}").addArgument(ci).addArgument(ControlData.currCycleName).addArgument(ControlData.currMonth).addArgument(ControlData.currDay).addArgument(ControlData.currYear).addArgument(solvePath).log();

		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("XA Solver: Solving {}/{}/{} Cycle {} [{}]").addArgument(ControlData.currMonth).addArgument(ControlData.currDay).addArgument(ControlData.currYear).addArgument(ci).addArgument(ControlData.currCycleName).log();
		LOG.atDebug().setMessage("Starting XA solving process...").log();

		solveTimer = new PerformanceTimerXa("XA.solve");
		s = Calendar.getInstance().getTimeInMillis();
		ControlData.xasolver.solveWithInfeasibleAnalysis("Output console:");
		tSolve = Calendar.getInstance().getTimeInMillis() - s;
		try {
			solveTimer.stop();
		} catch (Exception e) {
			LOG.atDebug().setCause(e).setMessage("XA Solver solveTimer stop failed").log();
		}
		solveTimer = null;

		modelStatus=ControlData.xasolver.getModelStatus();
		try {
			solverStatus=String.valueOf(ControlData.xasolver.getSolverStatus());
		} catch (Exception e) {
			solverStatus = "unknown";
			LOG.atDebug().setCause(e).setMessage("XA Solver failed to fetch solverStatus after solve").log();
		}

		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("Model status: {}").addArgument(modelStatus).log();
		LOG.atDebug().setMessage("XA solve completed: cycle={} [{}] solvePath={} modelStatus={} solverStatus={} solveMs={}").addArgument(ci).addArgument(ControlData.currCycleName).addArgument(solvePath).addArgument(modelStatus).addArgument(solverStatus).addArgument(tSolve).log();

		if (modelStatus>=2) getSolverInformation();
		if (Error.error_solving.size()<1) {
			LOG.atDebug().setMessage("Assigning XA variable values to data structures").log();
			s = Calendar.getInstance().getTimeInMillis();
			assignDvar();
			tAssign = Calendar.getInstance().getTimeInMillis() - s;
			assignDone = true;
			LOG.atDebug().setMessage("XA variable assignment complete: total {} variables assigned").addArgument(assignedDvarCount).log();
		} else {
			LOG.atDebug().setMessage("XA Solver skipped assignDvar because solvingErrors={}").addArgument(Error.error_solving.size()).log();
		}

		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_xa=ControlData.t_xa+(int) (t2-t1);

		if (solverStatus == null) {
			try {
				solverStatus = String.valueOf(ControlData.xasolver.getSolverStatus());
			} catch (Exception e) {
				solverStatus = "unknown";
			}
		}
		if (Double.isNaN(objectiveValue)) {
			try {
				objectiveValue = ControlData.xasolver.getObjective();
			} catch (Exception e) {
				objectiveValue = Double.NaN;
			}
		}

		try {
			totalTimer.stop();
		} catch (Exception e) {
			LOG.atDebug().setCause(e).setMessage("XA Solver totalTimer stop failed").log();
		}

		performanceStats.recordRun(tSetConstraints, tSetDVars, tSetWeights, tSolve, tAssign, t2-t1, assignDone);

		LOG.atDebug().setMessage("XA total processing time: {} ms").addArgument(t2-t1).log();
		LOG.atDebug().setMessage("XA Solver summary: date={}/{}/{} cycle={} [{}] totalMs={} loadModelMs={} setConstraintsMs={} setDVarsMs={} setWeightsMs={} solveMs={} assignMs={} modelStatus={} solverStatus={} solvingErrors={} assignDone={} dvarCount={} integerDvarCount={} continuousDvarCount={} weightCount={} constraintCount={} equalityConstraintCount={} inequalityConstraintCount={} assignedDvarCount={} objective={} solvePath={} cumulativeRuns={} avgSolveMs={} avgRunMs={}")
			.addArgument(ControlData.currMonth).addArgument(ControlData.currDay).addArgument(ControlData.currYear).addArgument(ci).addArgument(ControlData.currCycleName)
			.addArgument(t2-t1).addArgument(tLoadModel).addArgument(tSetConstraints).addArgument(tSetDVars).addArgument(tSetWeights).addArgument(tSolve).addArgument(tAssign)
			.addArgument(modelStatus).addArgument(solverStatus).addArgument(Error.error_solving.size()).addArgument(assignDone)
			.addArgument(dvarCount).addArgument(integerDvarCount).addArgument(continuousDvarCount).addArgument(weightCount).addArgument(constraintCount)
			.addArgument(equalityConstraintCount).addArgument(inequalityConstraintCount).addArgument(assignedDvarCount).addArgument(objectiveValue)
			.addArgument(solvePath).addArgument(performanceStats.getTotalRuns()).addArgument(performanceStats.getAverageSolveMs()).addArgument(performanceStats.getAverageRunMs()).log();
		LOG.atDebug().setMessage("==================== XA Problem Solving Session Complete ====================").log();
	}

	public static void logPerformanceSummary(){
		performanceStats.logSummary();
	}

	public void getSolverInformation(){
		LOG.atError().setMessage("XA Solver failure: modelStatus={} solverStatus={}").addArgument(modelStatus).addArgument(ControlData.xasolver.getSolverStatus()).log();
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
		long s = Calendar.getInstance().getTimeInMillis();
		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("XA Solver: Setting dvars").log();

		lastIntegerDvarCount = 0;
		lastContinuousDvarCount = 0;

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
					ControlData.xasolver.setColumnInteger(dvarName, lb, ub);
					lastIntegerDvarCount++;
				}
				else {
					ControlData.xasolver.setColumnMinMax(dvarName, lb, ub);
					lastContinuousDvarCount++;
				}
			}
		}
		lastSetDVarsMs = Calendar.getInstance().getTimeInMillis() - s;
	}

	public void setWeights(){
		long s = Calendar.getInstance().getTimeInMillis();
		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("XA Solver: Setting weights").log();

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
		lastSetWeightsMs = Calendar.getInstance().getTimeInMillis() - s;
	}

	private void setConstraints() {
		long s = Calendar.getInstance().getTimeInMillis();
		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("XA Solver: Setting constraints").log();

		lastEqualityConstraintCount = 0;
		lastInequalityConstraintCount = 0;

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
					ControlData.xasolver.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
					lastEqualityConstraintCount++;
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					ControlData.xasolver.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
					lastInequalityConstraintCount++;
				}
				else if (ec.getSign().equals(">")){
					ControlData.xasolver.setRowMin(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
					lastInequalityConstraintCount++;
				}

				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set<String> multCollection = multMap.keySet();
				Iterator<String> multIterator = multCollection.iterator();

				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
					if (!dvarMap.containsKey(multName)) addConditionalSlackSurplusToDvarMap(dvarMap, multName);
					ControlData.xasolver.loadToCurrentRow(multName, multMap.get(multName).getData().doubleValue());
				}
			}
		}
		lastSetConstraintsMs = Calendar.getInstance().getTimeInMillis() - s;
	}

	public void assignDvar(){
		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("XA Solver: Assigning dvars' values").log();

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

		assignedDvarCount = 0;
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
			assignedDvarCount++;
		}

		try {
			objectiveValue = ControlData.xasolver.getObjective();
		} catch (Exception e) {
			objectiveValue = Double.NaN;
			LOG.atDebug().setCause(e).setMessage("XA Solver failed to fetch objective after assignDvar").log();
		}

		if (ControlData.showRunTimeMessage) LOG.atDebug().setMessage("Objective Value: {}. Assign Dvar Done.").addArgument(objectiveValue).log();
	}

	public void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);
	}
}