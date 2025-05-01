package wrimsv2.solver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.coinor.clp.SWIGTYPE_p_ClpSimplex;
import org.coinor.clp.SWIGTYPE_p_CoinBuild;
import org.coinor.clp.SWIGTYPE_p_double;
import org.coinor.clp.SWIGTYPE_p_int;
import org.coinor.clp.jClp;

import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.commondata.solverdata.*;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.EvalConstraint;

public class ClpSolver {
	
	public static int errorCode = 99;  // 12: error in dvar value
	public static Map <String, Double> varDoubleMap;
	private static SWIGTYPE_p_ClpSimplex model;
	private static SWIGTYPE_p_CoinBuild buildObject;
	private static  ArrayList<String> dvKeys;
	private static  Map<String, Dvar> dvarMap;
	
	private static  ArrayList <SWIGTYPE_p_int> rowIndex_array;
	private static  ArrayList <SWIGTYPE_p_double> rowValue_array;
	private static  double maxValue = 1.0e23;
	
	private ClpSolver(){}
	
	public static void init(boolean useLpFile){
		System.loadLibrary("jClp");
		if (useLpFile){
			model = jClp.new_jClpSimplex();
			jClp.setLogLevel(model , 0);
		}
	}

	public static void close(){
		//jClp.delete_jClpSimplex(model);
		//model=null;
	}
	
	public static void newProblem(String modelName, boolean useLpFile){
		
		ControlData.clp_cbc_objective = null;
		dvKeys = new ArrayList<String>(ControlData.currModelDataSet.dvList);
		dvKeys.addAll(ControlData.currModelDataSet.dvTimeArrayList);
		
		if (useLpFile) {
			
			// passing lp file
			jClp.readLp(model, modelName);
			

		} else {
			model = jClp.new_jClpSimplex();
			buildObject = jClp.new_jCoinBuild();
			jClp.setModelName(model, modelName);
			jClp.setLogLevel(model , 0);

			dvarMap = SolverData.getDvarMap();


			rowIndex_array = new ArrayList<SWIGTYPE_p_int>();
			rowValue_array = new ArrayList<SWIGTYPE_p_double>();

			// estimate column size, may increase later
			jClp.resize(model, 0, dvKeys.size());

			setConstraints();
			setDVars();
			setWeights();

		}
		
		int modelStatus = solve(1e-8, useLpFile);
		
		if (modelStatus==1) { 
			// re-solve with relaxed tolerance
			System.out.println("Relax tolerance to 1e-7");
			modelStatus=solve(1e-7, useLpFile);
		} 
		if (modelStatus==1) { 
			// re-solve with relaxed tolerance
			System.out.println("Relax tolerance to 5e-7");
			modelStatus=solve(5e-6, useLpFile);
		} 
//		status:		
//		 0 - optimal
//		 1 - primal infeasible
//		 2 - dual infeasible (unbounded) 
//		 3 - stopped on iterations etc
//		 4 - stopped due to errors
		if (modelStatus>0)	getSolverInformation(modelStatus);
		if (Error.error_solving.size()<1) {
			
			ControlData.clp_cbc_objective = getObjValue();
			collectDvar(); 
			assignDvar();
			//System.out.println("obj: "+getObjValue());
		}
		
		resetModel(useLpFile);
		
	}
	
	public static double getObjValue(){
		return jClp.objectiveValue(model)*-1;
	}
	
	private static void resetModel(boolean useLpFile) {
		
		if (!useLpFile) {
			
			//jClp.resize(model, 0, 0);
			jClp.delete_jCoinBuild(buildObject);
			jClp.delete_jClpSimplex(model);
			model=null;
			
			for (int i = 0; i < rowIndex_array.size(); i++) {
				jClp.delete_jarray_int(rowIndex_array.get(i));
				jClp.delete_jarray_double(rowValue_array.get(i));
			}
			rowIndex_array = null;
			rowValue_array = null;
		}
	}

	private static void getSolverInformation(int modelStatus){
		
		errorCode=modelStatus;
		System.out.println("clp error code: "+modelStatus);
		switch (modelStatus){
			case 1: Error.addSolvingError("Primal infeasible.");break; 
			case 2: Error.addSolvingError("Dual infeasible (unbounded).");break; 
			case 3: Error.addSolvingError("Stopped on iteration, etc."); break;
			case 4: Error.addSolvingError("Stopped due to errors."); break;
			default: Error.addSolvingError("Solving failed"); break;
		}
	}
	
	private static void setConstraints() {
		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		
		int rowCounter=0; // row index
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
				
				double GT=-999;
				double LT= 999;
				
				
				String constraintName=(String)constraintIterator.next();
				EvalConstraint ec=constraintMap.get(constraintName);
			
				if (ec.getSign().equals("=")) {
					//ClpSolverJNI.setRowFix(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					LT = GT;
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					//ClpSolverJNI.setRowMax(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
					GT = -maxValue;
					LT = -ec.getEvalExpression().getValue().getData().doubleValue();
				}
				else if (ec.getSign().equals(">")){
					//ClpSolverJNI.setRowMin(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue()); //string constraint name
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					LT = maxValue;
				}
				else {
					// error!!
					
				}
			
				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();				
				
				rowIndex_array.add(jClp.new_jarray_int(multMap.keySet().size()));
				rowValue_array.add(jClp.new_jarray_double(multMap.keySet().size()));
				
				//SWIGTYPE_p_int rowIndex = jClp.new_jarray_int(multMap.keySet().size());
				//SWIGTYPE_p_double rowValue = jClp.new_jarray_double(multMap.keySet().size());
				
				int j=0;
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
					
					// check for inefficient zero coef  
					//if (multMap.get(multName).getData().doubleValue()==0.) System.out.println("zero coef found in setting up constraint!");
					
					if (!dvarMap.containsKey(multName)){ 
						dvKeys.add(multName);
						jClp.resize(model, jClp.getNumRows(model), dvKeys.size());
						addConditionalSlackSurplusToDvarMap(dvarMap, multName);
					
					}					
					
					jClp.jarray_int_setitem(rowIndex_array.get(rowCounter),j,dvKeys.indexOf(multName));
					jClp.jarray_double_setitem(rowValue_array.get(rowCounter),j,multMap.get(multName).getData().doubleValue());
					j++;
				}

				//jClp.addRow(model,multMap.keySet().size(), rowIndex_array.get(rowCounter), rowValue_array.get(rowCounter), GT, LT);
				jClp.addRow_build(buildObject,multMap.keySet().size(), rowIndex_array.get(rowCounter), rowValue_array.get(rowCounter), GT, LT);
				//jClp.setRowName(model,rowCounter,constraintName);
				rowCounter++; 
				
				
				// to be deleted later
				//rowIndex_array.add(rowIndex);
				//rowValue_array.add(rowValue);
				
			}
		}
		
		 //add all rows to model at once
		 jClp.addRows(model,buildObject);
	}

    private static void setWeights(){
   	 
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
				
				//if (weightMap.get(weightName).getValue()==0.) System.out.println("zero coef found in obj function!");
				
				jClp.setObjectiveCoefficient(model, dvKeys.indexOf(weightName), weightMap.get(weightName).getValue());
			}
		}
		Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
		CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
		Iterator<String> usedWeightSlackSurplusIterator = usedWeightSlackSurplusCollection.iterator();
	
		while(usedWeightSlackSurplusIterator.hasNext()){
			String usedWeightSlackSurplusName=(String)usedWeightSlackSurplusIterator.next();
			jClp.setObjectiveCoefficient(model, dvKeys.indexOf(usedWeightSlackSurplusName), weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue());
		}
	}
    
	private static void setDVars() {
				
		for (int i=0; i<dvKeys.size(); i++){
			String dvName = dvKeys.get(i);
			Dvar dvObj = dvarMap.get(dvName);
			jClp.setColumnUpper(model, i , dvObj.upperBoundValue.doubleValue());
			jClp.setColumnLower(model, i , dvObj.lowerBoundValue.doubleValue());
			jClp.setColumnName(model,i,dvName);
			
		}
		
	}
	
	public static int solve(double tolerance, boolean useLpFile){
	    		
//		status:		
//		 0 - optimal
//		 1 - primal infeasible
//		 2 - dual infeasible (unbounded) 
//		 3 - stopped on iterations etc
//		 4 - stopped due to errors
		
		
		 //tuning
		 //System.out.println("Tuning \n\nDefault primal Tolerance is "+jClp.primalTolerance(model)+"\n");
		 jClp.setPrimalTolerance(model,tolerance);
		 //System.out.println("Primal Tolerance Changed to "+jClp.primalTolerance(model)+"\n");
		 //System.out.println("Max Seconds: "+jClp.maximumSeconds(model) + "\t" + "Max Iterations: "+ jClp.maximumIterations(model)+"\n");
		 //System.out.println("model Status is "+ jClp.problemStatus(model)+"\n");
	     
		 
		 
		 if (!useLpFile) jClp.setOptimizationDirection(model, -1); //change minimization or maximization
		 //jClp.scaling(model,1); // turn scaling on or off
		 
		 //System.out.println("crash = "+jClp.crash(model,2.0,0)+"\n"); 
		 
		 //jClp.setFactorizationFrequency(model,200); //set factorization frequency
		 //System.out.println("dual bound = "+ jClp.dualBound(model) +"\n"); //get dual bound
		 //System.out.println("infeasibility cost = "+ jClp.infeasibilityCost(model) +"\n");
		 
		 //jClp.setFactorizationFrequency(model,200); //set factorization frequency	 
		 //System.out.println("fact. freq. = "+jClp.factorizationFrequency(model) +"\n");
		 
		// jClp.setPrimalColumnAlgorithmSteepest(model,1); //pricing method
		
		 jClp.optimize(model);
		 //jClp.presolve(model);
		
		 
		 //jClp.writeLp(model,jClp.getModelName(model));
		 
		 //jClp.primal(model); // solve using primal simplex
		 
		 
		 return jClp.problemStatus(model);

	    	
	    }
	
	
	private static void collectDvar() {
		//int test_add_size = 20;
		int ColumnSize = jClp.getNumCols(model);
		varDoubleMap = new HashMap<String, Double>();
		//SWIGTYPE_p_double sol = jClp.new_jarray_double(ColumnSize); // seems to be deleted when model is deleted
		//sol = jClp.getColSolution(model);
		
		for (int i = 0; i < ColumnSize; i++){
			//System.out.println(jClp.getColumnName(model,i)+" = "+jClp.jarray_double_getitem(sol,i));
			varDoubleMap.put(jClp.getColumnName(model,i), jClp.jarray_double_getitem(jClp.getColSolution(model),i));
//			if (jClp.getColumnName(model,i).equalsIgnoreCase("Putah_WYT_sv")) {
//				
//				System.out.println("Putah_WYT_sv:" + jClp.jarray_double_getitem(sol,i) );
//				//System.out.println("Putah_WYT_sv:" + (float)jClp.jarray_double_getitem(sol,i) );
//				//double n = (float)jClp.jarray_double_getitem(sol,i);
//				//System.out.println("Putah_WYT_sv:" + n );
//				//varDoubleMap.put(jClp.getColumnName(model,i), n);
//			}
		}

	}

	public void setDefaultOption() {
		
	}
	
	
	private static void assignDvar() {
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
			
			double value = -77777777;
			try {
				if ( varDoubleMap.keySet().contains(dvName) ) {
					value=varDoubleMap.get(dvName);
				} else {
					value = 0;  // if not listed in clp solution file then it's 0
					varDoubleMap.put(dvName, value);
				}
				
			} catch (Exception e) {
				errorCode=12;
				Error.addSolvingError("CLP solution dvar value error. Dvar: "+dvName+" Value: "+varDoubleMap.get(dvName));
				
			}
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
			System.out.println("Objective Value: "+ControlData.clp_cbc_objective);
			System.out.println("Assign Dvar Done.");
		}
	}
	
	public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String multName){
		
		Dvar dvar=new Dvar();
		dvar.upperBoundValue=1.0e23;
		dvar.lowerBoundValue=0.0;
		dvarMap.put(multName, dvar);

	}
	
    public static void main(String argv[]) {
        System.loadLibrary("jClp"); // load the dll library
   	 
        SWIGTYPE_p_ClpSimplex Mo = jClp.new_jClpSimplex(); 
        
        jClp.readLp(Mo, "infeasible_1924_3_c8.lp");
        
		jClp.primal(Mo); // solve using primal simplex

        //jClp.printSolution(Mo); //print solution 

   	 
   	}

}
