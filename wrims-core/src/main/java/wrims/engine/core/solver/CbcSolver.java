package wrims.engine.core.solver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import org.coinor.cbc.SWIGTYPE_p_std__string;
import org.coinor.cbc.SWIGTYPE_p_CbcModel;
import org.coinor.cbc.SWIGTYPE_p_CoinModel;
import org.coinor.cbc.SWIGTYPE_p_OsiClpSolverInterface;
import org.coinor.cbc.jCbc;
import org.coinor.cbc.SWIGTYPE_p_double;
import org.coinor.cbc.SWIGTYPE_p_int;

import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.Param;
import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.commondata.solverdata.*;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.Error;
import wrims.engine.core.components.IntDouble;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;
import wrims.engine.core.evaluator.EvalConstraint;
import wrims.engine.core.ilp.ILP;
import wrims.engine.core.tools.InfeasibilityAnalysis;
import wrims.engine.core.wreslplus.elements.Tools;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;



public class CbcSolver {
	
	public static LinkedHashMap <String, Double> varDoubleMap;
	
	private static SWIGTYPE_p_OsiClpSolverInterface solver; // = jCbc.new_jOsiClpSolverInterface(); //this is our LP solver!
	private static SWIGTYPE_p_CbcModel model; // = jCbc.new_jCbcModel();// This defines a new empty CbcModel
	private static SWIGTYPE_p_CoinModel modelObject; 

	private static Map<String, String> iisPossibleConstraintMap;
	private static Map<String, String> iisPossibleConstraintMap_cumulative;
	private static Set<String> iisConfirmConstraint;
	public static Set<String> prioritizeSearchTheseConstraints;
	private static boolean hasPriorityConstraints = false; // will be assigned to true if above set is not empty.
	private static Set<String> _pstc;
	private static int total_relaxed_constraints = 0;
	
	//private static LinkedHashSet<String> iisReport;
	private static LinkedHashMap<Integer, String> iisSlackMap;
	private static LinkedHashMap<String, String> iisConstraintSignMap;
	private static LinkedHashMap<String, Double> iisConstraintRHSMap;
	private static LinkedHashMap<String, int[]> iisConstraintIndexMap;
	private static LinkedHashMap<String, double[]> iisConstraintElementMap;
	private static ArrayList<String> iisSlacks;
	//private static ArrayList<String> iisDvarList;
	public static double cbcHintRelaxPenalty = 9000;
	
	private static  Map<String, Dvar> dvarMap;
	private static BiMap<Integer, String> dvBiMap;
	private static ArrayList<String> dvBiMapArray;
	private static BiMap<String, Integer> dvBiMapInverse;
	
	private static  double maxValue = 1e28; //Double.POSITIVE_INFINITY;
	//public static final double zeroTolerence =  1e-10;
	public static double solve_2_primalT =  1e-9;        // can read from config cbcTolerancePrimal
	public static double solve_2_primalT_relax =  1e-7;  // can read from config cbcTolerancePrimalRelax
	//public static final double solve_2_primalT_relax_most =  1e-4;
	private static final double solve_3_primalT =  1e-9;
	//public static final double solve_3_primalT_relax =  1e-7;
	public static double solve_whs_primalT =  1e-9;      // can read from config cbcToleranceWarmPrimal
	//public static final Integer cutoff_n =  12;
	public static double integerT =  1e-9;               // can read from config cbcToleranceInteger
	public static double integerT_check = 1e-8;          // can read from config cbcToleranceIntegerCheck
	public static Double lowerBoundZero_check = null;
	public static final double cbcWriteLpEpsilon = 1e-15; 	
	public static String cbcLibName = "jCbc";
	public static String cbcVersion = "None";
	public static int cbcHintTimeMax = 100; // can read from config CbcHintTimeMax
	public static boolean usejCbc2021 = false;
	public static boolean usejCbc2021a = false;
	public static boolean cbcViolationCheck = true;
	public static boolean cbcSolutionRounding = true;
	public static boolean cbcViolationRetry = true;
	public static int cbcLogStartDate = 999900;
	public static int cbcLogStopDate  = 999900;	
	private static boolean isLogging = false;
	public static boolean debugObjDiff = false;
	public static double  debugObjDiff_tolerance = 1E5;
	public static boolean whsScaling = true;
	public static boolean whsSafe = false;
	public static boolean debugDeviation = false;
	public static double  debugDeviationMin = 200;
	public static double  debugDeviationWeightMin = 99000;	
	public static double  debugDeviationWeightMultiply = 100;
	public static boolean  debugDeviationFindMissing = false;
	public static boolean  debugDeviationWriteWarning = false;
	private static String modelName;
	
	private static Map<String, WeightElement> wm2; 
	
	private static boolean useLpFile=false;
	
	private static int index[];
	private static double elements[];
	
	private static boolean saveWarm=false;
	private static boolean useWarm=false;
	private static final boolean saveIntVars=true;
	//private static boolean delWarm=false;

	private static LinkedHashMap<String, Integer> dvIntMap;
	private static LinkedHashMap<String, Integer> dvIntMap2021;
	private static ArrayList<String> dvIntPredict;
	private static int intVarSize = -99;
	
	private static SWIGTYPE_p_std__string names;
	private static SWIGTYPE_p_int values;
	private static int intSolSize=0;
	private static boolean warmArrayExist = false;
	
	private static SWIGTYPE_p_std__string names_eachTS;
	private static SWIGTYPE_p_int values_eachTS;
	private static int intSolSize_eachTS=0;
	private static boolean warmArrayExist_eachTS = false;
	
	private static SWIGTYPE_p_std__string names_dummy;
	private static SWIGTYPE_p_int values_dummy;

	// record max(cbc_obj - xa_obj) &  max(xa_obj - cbc_obj)
	public static double maxObjDiff=0;
	public static String maxObjDiff_id="";
	public static double maxObjDiff_minus=0;
	public static String maxObjDiff_minus_id="";
	private static HashSet originalDvarKeys=null;
	
	public static String solveName="";
	public static boolean logObj=true;
	public static boolean intLog=true;
	public static boolean intViolation=false;
	
	public static int solvFunc = 90; // default 
	public static int warm_2nd_solvFunc = 20; // default 
	
	//public static final int solv0 =  0;
	public static final int solv2 = 20;
	public static final int solv3 = 30;
	public static final int solvCallCbc = 50;
	public static final int solvFull = 90;
	public static final int solvU = 100;
	private static boolean next_possible_stuck=false;
	
	// record lp
	public static double record_if_obj_diff = 10000.0;
	public static double log_if_obj_diff = 500.0;
	
	public static final HashMap<Integer, String> solve_u_ret = new HashMap<Integer, String>() {{
	    put(6,"whs");put(5,"2__");put(4,"2_r");put(1,"inf");
	}};
	
	
	private CbcSolver(){}
	
	public static void init(boolean useLpFile, StudyDataSet sds){
		dvIntMap2021 = new LinkedHashMap<String, Integer>();
		//useLpFile=false;
		CbcSolver.useLpFile = useLpFile;
		//System.loadLibrary(cbcLibName);
		ILP.getIlpDir();
		ILP.createNoteFile();
		
		if (lowerBoundZero_check == null) {
			lowerBoundZero_check = Math.max(solve_2_primalT_relax*10, 1e-6);
		}
		ILP.writeNoteLn("cbcViolationCheck ="+cbcViolationCheck,false,false);
		ILP.writeNoteLn("lowerBoundZero_check ="+lowerBoundZero_check,false,false);
		ILP.writeNoteLn("cbcSolutionRounding ="+cbcSolutionRounding,false,false);
		ILP.writeNoteLn("cbc2021 ="+usejCbc2021,false,false);
		ILP.writeNoteLn("cbc2021a ="+usejCbc2021a,false,false);
		ILP.writeNoteLn("jCbc version:", cbcVersion);
		if (usejCbc2021a) { 
			jCbc.setWhsScaling(CbcSolver.whsScaling);
			jCbc.setWhsSafe(CbcSolver.whsSafe); 
		}
		ILP.writeNoteLn("cbcWhsScaling ="+CbcSolver.whsScaling,false,false);
		ILP.writeNoteLn("cbcWhsSafe ="+CbcSolver.whsSafe,false,false);
		//if (ControlData.useCbcWarmStart || ControlData.cbc_debug_routeCbc || ControlData.cbc_debug_routeXA){
			dvIntMap = new LinkedHashMap<String, Integer>();
			for (String d: sds.allIntDv){
				dvIntMap.put(d, 0);
			}
		//}
		
		names_dummy = jCbc.new_jarray_string(0); 
		values_dummy = jCbc.new_jarray_int(0); 
		
		if (!useLpFile) {dvBiMap = HashBiMap.create(); dvBiMapArray = new ArrayList<String>();}

		//solver = jCbc.new_jOsiClpSolverInterface(); //this is our LP solver!
	
	}

	public static void close(){

	}
	
	public static void newProblem(){
		long t1 = Calendar.getInstance().getTimeInMillis();
		//System.out.println(" current cycle: "+ControlData.currCycleIndex);
		dvIntPredict = new ArrayList<String>();
		if (ControlData.useCbcWarmStart) {
			if (ControlData.cycWarmStart!=null){
				if (ControlData.cycWarmStart.contains(ControlData.currCycleIndex)) {
					saveWarm = true;
				} else {
					saveWarm = false;
				}
				if (ControlData.cycWarmUse.contains(ControlData.currCycleIndex)) {
					useWarm = true;
				} else {
					useWarm = false;
				}
			}
		}
		
		
		modelName = ILP.getYearMonthCycle();
		
		ControlData.clp_cbc_objective = null;
		
		long beginT_creation = System.currentTimeMillis();	
		
		model = jCbc.new_jCbcModel();
		solver = jCbc.new_jOsiClpSolverInterface(); //this is our LP solver!
		jCbc.assignSolver(model,solver); // Assign the solver to CbcModel
		int currDate = ControlData.currYear*100 +ControlData.currMonth;
		isLogging = cbcLogStartDate <= currDate && currDate <=cbcLogStopDate;

		if (useLpFile) {
					
			jCbc.readLp(solver, ILP.cplexLpFilePath);
			
		} else {
			
			int sizeA = ControlData.currModelDataSet.dvList.size();
			int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();

			for (int i=0; i<sizeA; i++){
				dvBiMap.put(i,ControlData.currModelDataSet.dvList.get(i));
				dvBiMapArray.add(ControlData.currModelDataSet.dvList.get(i));	
			}
			for (int i=0; i<sizeB; i++){
				dvBiMap.put(i+sizeA,ControlData.currModelDataSet.dvTimeArrayList.get(i));
				dvBiMapArray.add(ControlData.currModelDataSet.dvTimeArrayList.get(i));
			}
			dvBiMapInverse = dvBiMap.inverse();
			
			originalDvarKeys = new HashSet<String>(SolverData.getDvarMap().keySet());
			
			dvarMap = SolverData.getDvarMap();
			wm2 = SolverData.getWeightSlackSurplusMap();

			modelObject = jCbc.new_jCoinModel();

			
			if (usejCbc2021 && false) { 
				setDVars2021(ControlData.cbcLogNativeLp);
			} else {           
				setDVars(ControlData.cbcLogNativeLp||isLogging, ""); 
			}
			setConstraints(ControlData.cbcLogNativeLp||isLogging, "");
			if (ControlData.cbcLogNativeLp||isLogging) {writeCbcLp("", false);}

		}
		long endT_creation = System.currentTimeMillis();	
		double time_second_creation = (endT_creation-beginT_creation)/1000.;
		ControlData.solverCreationTime_cbc += time_second_creation; 
		
		jCbc.setModelName(solver, modelName);
		
//		if (next_possible_stuck) {
//
//			reloadAndWriteLp("stuck_possible",true);
//			
//		} 
		//jCbc.writeLp1(model, "test", 1e-14, 14);
		long beginT = System.currentTimeMillis();
		//int[] modelStatus =null;
		if(usejCbc2021a){
			solve_jCbc2021a();
		} else if(usejCbc2021){
			solve_jCbc2021();
		} else {
			solve();
		}
		long endT = System.currentTimeMillis();
		
		double time_second = (endT-beginT)/1000.;
		
		if (Error.error_solving.size()<1) {
						
			ControlData.clp_cbc_objective = getObjValue();
			
			if (CbcSolver.logObj){
				ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ControlData.clp_cbc_objective, ILP._noteFile_cbc_obj);
				ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+CbcSolver.solveName+","+String.format("%8.2f",ControlData.solverTime_cbc_this), ILP._noteFile_cbc_time);			
			}
			long beginT_cd = System.currentTimeMillis();
			varDoubleMap = new LinkedHashMap<String, Double>();
			dvIntMap = new LinkedHashMap<String, Integer>();
			if (usejCbc2021) {collectDvar2021();} else {collectDvar();} 
			boolean intErr = false;
			// check integer violation
			if (cbcViolationCheck) {
			for (String k:dvIntMap.keySet()){
				//System.out.println(k);
				if (varDoubleMap.containsKey(k)){
					double v = varDoubleMap.get(k);
					if (Math.abs(v-Math.round(v))>integerT_check){
						intErr = true;
						Error.addSolvingError("int violation:::"+k+":"+v);
					} else if (cbcSolutionRounding){
						varDoubleMap.put(k, (double)Math.round(v));
					}
				}
			}
			if (intErr){reloadAndWriteLp("_intViolation", true, true);
			Error.addSolvingError("Integer Violation! Please contact developers for this issue.");}
			
			boolean lowerboundErr = false;			
			// check dv lowerbound violation (only for lowerbound =0)
			
			Map<String, Dvar> dMap = SolverData.getDvarMap();
			for (String k:dMap.keySet()){
				if (varDoubleMap.containsKey(k)){
					Dvar d = dMap.get(k);
					double v = varDoubleMap.get(k);
					if (d.lowerBoundValue.doubleValue() == 0){
						if (v<-lowerBoundZero_check){
							lowerboundErr = true;
							Error.addSolvingError("lowerbound violation:::"+k+":"+v);
						} else if (cbcSolutionRounding && v<0){
							varDoubleMap.put(k, 0.0);
						}
					}
				}
			}
			if (lowerboundErr){reloadAndWriteLp("_lbViolation", true, true);
			Error.addSolvingError("Lowerbound Violation! Please contact developers for this issue.");}
			}
			if (isLogging) {
				ILP.setVarFile();
				ILP.writeSvarValue();
				ILP.findDvarEffective();
				ILP.writeDvarValue_Clp0_Cbc0(varDoubleMap);			
			}
			
			long beginT_ad = System.currentTimeMillis();	
			if (!ControlData.cbc_debug_routeXA) assignDvar();
			long endT_ad = System.currentTimeMillis();	
			ControlData.cdTime += (beginT_ad-beginT_cd)/1000.;
			ControlData.adTime += (endT_ad-beginT_ad)/1000.;
		}
		
		if (time_second > 10.0) {

			reloadAndWriteLp("stuck_"+Math.round(time_second),true);			
			ILP.writeNoteLn(modelName, " "+solveName+" time(sec): "+time_second);

		}
		
		if (debugObjDiff){
			Double thisObj = ControlData.clp_cbc_objective;
			reloadProblem(false, "");
			callCbc(solveName);	
			if ( jCbc.status(model)==0 && jCbc.secondaryStatus(model)==0){
				Double cbcObj = getObjValue();
				if ( Math.abs(thisObj-cbcObj)>debugObjDiff_tolerance ) {
					reloadProblem(false, "");
					jCbc.callCbc("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model);
					if ( jCbc.status(model)==0 && jCbc.secondaryStatus(model)==0) {
						Double rObj = getObjValue();
						if ( Math.abs(thisObj-rObj)>debugObjDiff_tolerance ) {
							int ap = (int) Math.round(Math.abs(thisObj-rObj)/debugObjDiff_tolerance);
							reloadAndWriteLp("objErr_"+ap+"_",true);
						}
					}
				}
				
			}
		}
		
		// debug whs deviation
		if (debugDeviation && solveName=="whs"){

			// check watchlist for goal name, convert to slack surplus, 
			// if one exceed then
			// increase penalty see if obj value change
			// if not then logging
			Map<String, WeightElement> wm1  = SolverData.getWeightMap();
			Map<String, WeightElement> wm2  = SolverData.getWeightSlackSurplusMap();
			Map<String, WeightElement> wm1_ori = new HashMap<String, WeightElement>();
			Map<String, WeightElement> wm2_ori = new HashMap<String, WeightElement>();
			boolean firstWrite = true;
			
			String missing = "";

			boolean itemExist = false;
			if (debugDeviationFindMissing) {
				missing = ControlData.watchList[0];
				if (varDoubleMap.keySet().contains(missing)){
					note_msg(missing+": "+varDoubleMap.get(missing));
					if (wm1.keySet().contains(missing)){
						note_msg(missing+": weight1: "+wm1.get(missing).getValue());}
					else if (wm2.keySet().contains(missing)){
						note_msg(missing+": weight2: "+wm2.get(missing).getValue());}
					else { note_msg("weight not found");}
					
					itemExist = true;
					
				} 
			}
			
			searchloop: for (String dN: varDoubleMap.keySet()){
				if (debugDeviationFindMissing){ 
					if (dN.equalsIgnoreCase(missing)) {note_msg(missing+": is found.");}
				}
				int whichMap=0;
				double w=0;
				double v=0;
				if (dN.startsWith("slack_") || dN.startsWith("surplus_") ) {
					v = varDoubleMap.get(dN);	
					if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)) {
						note_msg(missing+": is slack or surplus.");
						note_msg(missing+": "+varDoubleMap.get(missing));	
					}
				}	
			
				if (v>debugDeviationMin){
									
					if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)) {
						note_msg(missing+": is deviated.");
					}
					
					if (wm1.keySet().contains(dN)){ 
						w = wm1.get(dN).getValue();
						whichMap =1;						
					}
					else if (wm2.keySet().contains(dN)){ 
						w = wm2.get(dN).getValue();
						whichMap =2;						
					}				
							
					if (Math.abs(w)>debugDeviationWeightMin){
						if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)) {
							note_msg(missing+": is weighted more than min.");
						}
						// write potential issue for once
						if (firstWrite && debugDeviationWriteWarning){
							reloadAndWriteLp("deviation_warning",true);
							note_msg("deviation warning: "+dN);
							firstWrite = false;
						}
						// load original weight, backup original weight, change weight
						if (!wm1_ori.isEmpty()) { wm1.putAll(wm1_ori);}
						if (!wm2_ori.isEmpty()) { wm2.putAll(wm2_ori);}
						
						WeightElement nwe = new WeightElement();
						nwe.setValue(w*debugDeviationWeightMultiply);						
						
						if (whichMap==1){
							wm1_ori.put(dN, wm1.get(dN));
							wm1.put(dN, nwe);
						} else if (whichMap==2){
							wm2_ori.put(dN, wm2.get(dN));
							wm2.put(dN, nwe);
						}
					
						reloadProblem(false,"");
						//reloadAndWriteLp("deviation_testing_"+dN,true);
						//solveName="whs";
						jCbc.setPrimalTolerance(model, solve_whs_primalT);
						jCbc.setIntegerTolerance(model, integerT);
						jCbc.solve_whs(model,solver,names,values,intSolSize,0);					
						int status = jCbc.status(model);
						int status2 = jCbc.secondaryStatus(model);	
						
						if (status==0 && status2 ==0){		
							//Double tObj = getObjValue();
							LinkedHashMap<String, Double> solution = collectDvar2021_simple();
							double newV = solution.get(dN);
							if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)){
								note_msg(missing+": test feasible, newV: "+newV);
							} 			
							if ( Math.abs(newV-v)<2.0){			
								// make sure deviation				
								reloadProblem(false,"");
								callCbc(solveName);	
								if ( jCbc.status(model)==0 && jCbc.secondaryStatus(model)==0){
									//Double tCbcObj = getObjValue();
									LinkedHashMap<String, Double> tCbcsolution = collectDvar2021_simple();
									double newtCbcV = tCbcsolution.get(dN);
									
									if (  Math.abs(newtCbcV)<1.0 ) {
										reloadAndWriteLp("deviation_error_("+dN+")",true);
										note_msg("deviation error: "+dN);
										break searchloop;
									}
									
								} else {
									if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)){
										note_msg(missing+": test Cbc infeasible");
									} 
									note_msg("deviation test Cbc infeasible");
									reloadAndWriteLp("deviation_test_Cbc_infeasible_("+dN+")",true);
								}
								
							}
							
						} else {
							if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)){
								note_msg(missing+": test infeasible");
							} 
							note_msg("deviation test infeasible");
							reloadAndWriteLp("deviation_test_infeasible_("+dN+")",true);
						}
						
					}				
					
				}			
				
			}
				
		}
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_cbc=ControlData.t_cbc+(int) (t2-t1);
	}
	
	public static double getObjValue(){
		return jCbc.getObjValue(model)*-1;
	}

	private static void reloadProblem(boolean isNoteCbc, String append) {


		// restore original state
		SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
		int sizeA = ControlData.currModelDataSet.dvList.size();
		int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();
		dvBiMap.clear(); dvBiMapArray = new ArrayList<String>();
		for (int i=0; i<sizeA; i++){
			dvBiMap.put(i,ControlData.currModelDataSet.dvList.get(i));	
			dvBiMapArray.add(i,ControlData.currModelDataSet.dvList.get(i));	
		}
		for (int i=0; i<sizeB; i++){
			dvBiMap.put(i+sizeA,ControlData.currModelDataSet.dvTimeArrayList.get(i));	
			dvBiMapArray.add(ControlData.currModelDataSet.dvTimeArrayList.get(i));	
		}
		dvBiMapInverse = dvBiMap.inverse();
				
		
		
		// clean up
		jCbc.delete_jCbcModel(model);
		model = null;
		jCbc.delete_jCoinModel(modelObject);
		//ControlData.clp_cbc_objective=null;
		
		
		// new model
		model = jCbc.new_jCbcModel();
		solver = jCbc.new_jOsiClpSolverInterface(); 
		jCbc.assignSolver(model,solver); 
		modelObject = jCbc.new_jCoinModel();

		setDVars(isNoteCbc, append);
		setConstraints(isNoteCbc, append);
		jCbc.setModelName(solver, modelName);
		
	}
	
	private static void reloadProblemConfirm(String skipThisConstraint) {

		// restore original state
		SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
		int sizeA = ControlData.currModelDataSet.dvList.size();
		int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();
		dvBiMap.clear();
		for (int i=0; i<sizeA; i++){
			dvBiMap.put(i,ControlData.currModelDataSet.dvList.get(i));				
		}
		for (int i=0; i<sizeB; i++){
			dvBiMap.put(i+sizeA,ControlData.currModelDataSet.dvTimeArrayList.get(i));				
		}
		dvBiMapInverse = dvBiMap.inverse();
		
		// clean up
		jCbc.delete_jCbcModel(model);
		model = null;
		jCbc.delete_jCoinModel(modelObject);
		
		// new model
		model = jCbc.new_jCbcModel();
		solver = jCbc.new_jOsiClpSolverInterface(); 
		jCbc.assignSolver(model,solver); 
		modelObject = jCbc.new_jCoinModel();

		setDVarsIIS();
		setConstraintsSkip(skipThisConstraint);
		jCbc.setModelName(solver, modelName);
		
	}
	
	private static void loadProblemIIS(boolean isFirstTimeRun, Set<String> enforceThisConstraint) {
		
		iisSlackMap = new LinkedHashMap<Integer, String>();
		// restore original state
		SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
		int sizeA = ControlData.currModelDataSet.dvList.size();
		int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();
		dvBiMap.clear();
		for (int i=0; i<sizeA; i++){
			dvBiMap.put(i,ControlData.currModelDataSet.dvList.get(i));
		}
		for (int i=0; i<sizeB; i++){
			dvBiMap.put(i+sizeA,ControlData.currModelDataSet.dvTimeArrayList.get(i));				
		}
		dvBiMapInverse = dvBiMap.inverse();
		
		
		
		// clean up
		jCbc.delete_jCbcModel(model);
		model = null;
		jCbc.delete_jCoinModel(modelObject);
		//ControlData.clp_cbc_objective=null;
		
		
		// new model
		model = jCbc.new_jCbcModel();
		solver = jCbc.new_jOsiClpSolverInterface(); 
		jCbc.assignSolver(model,solver); 
		modelObject = jCbc.new_jCoinModel();

		setDVarsIIS();
		setConstraintsIIS(isFirstTimeRun, enforceThisConstraint);
		jCbc.setModelName(solver, modelName+"_iis");
		
	}
	
	public static void resetModel() {

		jCbc.delete_jCbcModel(model);
		model = null;
				
		if (!useLpFile) {
			jCbc.delete_jCoinModel(modelObject);
			dvBiMap.clear();
			dvBiMapArray = new ArrayList<String>();
			dvBiMapInverse.clear();
		}

	}

	private static void getSolverInformation(int status, int status2){
		
		// status
//		 0 if finished (which includes the case when the algorithm is finished because it has been proved infeasible), 
//		 1 if stopped by user, and 
//		 2 if difficulties arose.	
		
		switch (status){
			case 0: Error.addSolvingError("Infeasible.");break; 
			case 1: Error.addSolvingError("Stopped by user.");break; 
			case 2: Error.addSolvingError("Other errors."); break;
			default: Error.addSolvingError("Status:"+status); break;
		}
		
		// secondaryStatus	 
//		0 search completed with solution
//		1 linear relaxation not feasible
//		2 stopped on gap
//		3 stopped on nodes
//		4 stopped on time
//		5 stopped on user event
//		6 stopped on solutions
//		7 linear relaxation unbounded
		
		switch (status2){
			case 1: Error.addSolvingError("Linear relaxation not feasible.");break; 
			case 2: Error.addSolvingError("Stopped on gap.");break; 
			case 3: Error.addSolvingError("Stopped on nodes."); break;
			case 4: Error.addSolvingError("Stopped on time."); break;
			case 5: Error.addSolvingError("Stopped on user event."); break;
			case 6: Error.addSolvingError("Stopped on solutions."); break;
			case 7: Error.addSolvingError("Linear relaxation unbounded."); break;
			default: Error.addSolvingError("Status2:"+status2); break;
		}
	}
	
	private static void setConstraints(boolean isNoteCbc, String append) {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Setting constraints");
		
		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		String c="quicklog version 1.0\n";
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
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					else if (Math.abs(GT)>maxValue) {GT=maxValue*Math.signum(GT);}
					LT = GT;
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					GT = -maxValue;
					LT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(LT)<ControlData.zeroTolerance) {LT=0;} 
					else if (Math.abs(LT)>maxValue) {LT=maxValue*Math.signum(LT);}
				}
				else if (ec.getSign().equals(">")){
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					else if (Math.abs(GT)>maxValue) {GT=maxValue*Math.signum(GT);}
					LT = maxValue;
				}
				else {
					// error!!
					System.out.println("error in CbcSolver!");
				}
			
				LinkedHashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();				
				
				index = new int[multMap.keySet().size()];	
				elements = new double[multMap.keySet().size()];	
				
				int j=0;
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
										
					if (!dvarMap.containsKey(multName)){ 
						int sizeDv = dvBiMap.size();
						dvBiMap.put(sizeDv, multName);
						dvBiMapArray.add(multName);
						dvBiMapInverse.put(multName, sizeDv);
						
						addConditionalSlackSurplusToDvarMap(dvarMap, multName, isNoteCbc, append);
					
					}					
					
					index[j]=dvBiMapInverse.get(multName);
					double temp = multMap.get(multName).getData().doubleValue();
					if(Math.abs(temp)<ControlData.zeroTolerance) {temp=0;} 
					elements[j]=temp;
					
					j++;
				}

				jCbc.addRow(modelObject,multMap.keySet().size(), index, elements, GT, LT, constraintName);
				
				
			    if (isNoteCbc) {
			    			
					    	c = c + constraintName +","+GT+","+LT+","+multMap.keySet().size()+","+Arrays.toString(index)+","+Arrays.toString(elements)+"\n"; 					 
			    }
				rowCounter++; 
				
			}
		}
		
		 jCbc.addRows(solver,modelObject);
		 if (isNoteCbc) Tools.quickLog(modelName+"_"+solveName+"_"+append+".rows", c);
	}
	
	private static void setConstraintsSkip(String skipThisConstraint) {
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
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					LT = GT;
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					GT = -maxValue;
					LT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(LT)<ControlData.zeroTolerance) {LT=0;} 
				}
				else if (ec.getSign().equals(">")){
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					LT = maxValue;
				}
				else {
					// error!!
					System.out.println("error in CbcSolver!");
				}
			
				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();				
				
				index = new int[multMap.keySet().size()];	
				elements = new double[multMap.keySet().size()];	
				
				int j=0;
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
										
					if (!dvarMap.containsKey(multName)){ 
						int sizeDv = dvBiMap.size();
						dvBiMap.put(sizeDv, multName);
						dvBiMapInverse.put(multName, sizeDv);
						
						addConditionalSlackSurplusToDvarMap(dvarMap, multName, false, "");
					
					}					
					
					index[j]=dvBiMapInverse.get(multName);
					double temp = multMap.get(multName).getData().doubleValue();
					if(Math.abs(temp)<ControlData.zeroTolerance) {temp=0;} 
					elements[j]=temp;
					
					j++;
				}
				if(!skipThisConstraint.equalsIgnoreCase(constraintName)){
					jCbc.addRow(modelObject,multMap.keySet().size(), index, elements, GT, LT, constraintName);
				}
				rowCounter++; 
				
			}
		}
		
		 jCbc.addRows(solver,modelObject);
	}
	
	private static void setConstraintsIIS(boolean firstTimeRun, Set<String> enforceThisConstraint) {
		
		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		int total=0;
		for (int i=0; i<=1; i++){
			ArrayList<String> constraintCollection;
			if (i==0){
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
				constraintCollection.retainAll(constraintMap.keySet());
			}else{
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
			}
			Iterator<String> constraintIterator = constraintCollection.iterator();
		
			
			while(constraintIterator.hasNext()){        total++;
				
				double GT=-999;
				double LT= 999;
				
				String constraintName=(String)constraintIterator.next();
				EvalConstraint ec=constraintMap.get(constraintName);
			
				if (ec.getSign().equals("=")) {
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					LT = GT;
				}
				else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					GT = -maxValue;
					LT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(LT)<ControlData.zeroTolerance) {LT=0;} 
				}
				else if (ec.getSign().equals(">")){
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {GT=0;} 
					LT = maxValue; 
				}
				else {
					// error!!
					ILP.writeNoteLn("Error in CbcSolver constraint processing!",true,true);
				}
			
				HashMap<String, IntDouble> multMap = ec.getEvalExpression().getMultiplier();
				Set multCollection = multMap.keySet();
				Iterator multIterator = multCollection.iterator();				
				
				index = new int[multMap.keySet().size()+2];	
				elements = new double[multMap.keySet().size()+2];	
				
				int j=0;
				while(multIterator.hasNext()){
					String multName=(String)multIterator.next();
										
					if (!dvarMap.containsKey(multName)){ 
						int sizeDv = dvBiMap.size();
						dvBiMap.put(sizeDv, multName);
						dvBiMapInverse.put(multName, sizeDv);
						
						addConditionalSlackSurplusToDvarMap(dvarMap, multName, false, "");
					
					}					
					
					index[j]=dvBiMapInverse.get(multName);
					double temp = multMap.get(multName).getData().doubleValue();
					if(Math.abs(temp)<ControlData.zeroTolerance) {temp=0;} 
					elements[j]=temp;
					
					j++;
				}

				if (firstTimeRun) {
					int[] newIndex = Arrays.copyOfRange(index, 0, index.length - 2);
					double[] newElements = Arrays.copyOfRange(elements, 0, elements.length - 2);
					iisConstraintIndexMap.put(constraintName, newIndex);
					iisConstraintElementMap.put(constraintName, newElements);
					iisConstraintSignMap.put(constraintName, ec.getSign());
					iisConstraintRHSMap.put(constraintName, -ec.getEvalExpression().getValue().getData().doubleValue());
				}
				// TODO: add index and elements here for IIS
				String iisNameP = constraintName + "_p";
				String iisNameN = constraintName + "_n";
				
				double coef = 1;

				if (hasPriorityConstraints && _pstc.size()>0) {
					coef = 0;
					if (_pstc.contains(constraintName)){ coef = 1; }
				}
				
				if (enforceThisConstraint.contains(constraintName)){ coef = 0; }	
					
				int z = dvBiMap.size();
				
				dvBiMap.put(z, iisNameP);
				dvBiMapInverse.put(iisNameP, z);
				index[j] = dvBiMapInverse.get(iisNameP);
				elements[j] = coef;
				j++;				
				iisSlackMap.put(z, constraintName);
				//iisC
				jCbc.addCol(modelObject, 0, maxValue, cbcHintRelaxPenalty, iisNameP, false); 
				
				dvBiMap.put(z+1, iisNameN);
				dvBiMapInverse.put(iisNameN, z+1);
				index[j] = dvBiMapInverse.get(iisNameN);
				elements[j] = -coef;
				j++;				
				iisSlackMap.put(z+1, constraintName);
				jCbc.addCol(modelObject, 0, maxValue, cbcHintRelaxPenalty, iisNameN, false); 
				
				
				jCbc.addRow(modelObject,multMap.keySet().size()+2, index, elements, GT, LT, constraintName);
				
			}
		}
		
		 jCbc.addRows(solver,modelObject); total_relaxed_constraints = total;
	}
    
	private static void setDVars(boolean isNoteCbc, String append) {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Setting dvars");
		
		Map<String, WeightElement> wm1  = SolverData.getWeightMap();
		String c="quicklog version 1.0\n";
		for (int i=0; i<dvBiMap.size(); i++){
			String dvName = dvBiMapArray.get(i);
			Dvar dvObj = dvarMap.get(dvName);
			
			double w = 0;
			if (wm1.keySet().contains(dvName)){
				w = -wm1.get(dvName).getValue();
			
			}
			//System.out.println("weight of: "+dvName+"="+w);			
		    jCbc.addCol(modelObject , dvObj.lowerBoundValue.doubleValue(), dvObj.upperBoundValue.doubleValue(), w, dvName, dvObj.integer.equalsIgnoreCase(Param.yes)); 
		    if (isNoteCbc) {

		    	int isInt=0;
		    	if (dvObj.integer.equalsIgnoreCase(Param.yes)) isInt=1;
		    	c = c + isInt+","+dvName+","+w+","+dvObj.lowerBoundValue.doubleValue()+","+dvObj.upperBoundValue.doubleValue()+"\n"; 
		    
		    }
		}
		if (isNoteCbc) Tools.quickLog(modelName+"_"+solveName+"_"+append+".cols", c);
		
	}
	
	private static void setDVars2021(boolean isNoteCbc) {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Setting dvars");
		int intSize = 0;
		Map<String, WeightElement> wm1  = SolverData.getWeightMap();
		String c="quicklog version 1.0\n";
		for (int i=0; i<dvBiMap.size(); i++){
			String dvName = dvBiMapArray.get(i);
			Dvar dvObj = dvarMap.get(dvName);
			
			double w = 0;
			if (wm1.keySet().contains(dvName)){
				w = -wm1.get(dvName).getValue();
//				if (Math.abs(w)==0) {
//					w=0;
//				} else if (Math.abs(w)>1E5) {
//					double de = Math.abs(w)-1E5;
//					w=Math.signum(w)*(1E5+de/1000);
//				} else if (Math.abs(w)<0.1) {
//					double de = 0.1-Math.abs(w);
//					w=Math.signum(w)*(0.1-de/10);
//				}
			
			}
			double lb=9; double ub=-9; boolean isInteger=(dvObj.integer.equalsIgnoreCase(Param.yes));
			if (isInteger) {intSize = intSize+1;}
			if (isInteger && dvIntMap2021.containsKey(dvName)){
				dvIntPredict.add(dvName);
				lb = dvIntMap2021.get(dvName);ub=lb;
			} else {
				lb = dvObj.lowerBoundValue.doubleValue();
				ub = dvObj.upperBoundValue.doubleValue();
			}

		    jCbc.addCol(modelObject , lb, ub, w, dvName, isInteger ); 
		    if (isNoteCbc) {

		    	int isInt=0;
		    	if (isInteger) isInt=1;
		    	c = c + isInt+","+dvName+","+w+","+lb+","+ub+"\n"; 
		    
		    }
		}
		if (isNoteCbc) Tools.quickLog(modelName+"_"+solveName+".cols", c);
		intVarSize = intSize;
		
	}
	
	private static void setDVarsIIS() {
		
		for (int i=0; i<dvBiMap.size(); i++){
			String dvName = dvBiMap.get(i);
			Dvar dvObj = dvarMap.get(dvName);
			
		    jCbc.addCol(modelObject , dvObj.lowerBoundValue.doubleValue(), dvObj.upperBoundValue.doubleValue(), 0, dvName, dvObj.integer.equalsIgnoreCase(Param.yes)); 
			
		}
		
	}
	
	public static int[] solve(){
			int ci=ControlData.currCycleIndex+1;
			if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Solving "+ControlData.currMonth+"/"+ControlData.currDay+"/"+ControlData.currYear+" Cycle "+ci+" ["+ControlData.currCycleName+"]");
			
			int status=-99;
			int status2=-99;
			
			long beginT = System.currentTimeMillis();
		    	
			if (solvFunc == solvU){
				//solveName="U__";
				int ret = 0;
				if (useWarm || saveWarm) {
					int ii = ControlData.currCycleIndex + 1;
					//System.out.println(ii + ": use warm");
	
					if (warmArrayExist) {
	
						jCbc.delete_jarray_int(values);
						jCbc.delete_jarray_string(names);
						intSolSize = 0;
						values = null;
						names = null;
						warmArrayExist = false;
					}
	
					intSolSize = ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size();
					names = jCbc.new_jarray_string(intSolSize);
					values = jCbc.new_jarray_int(intSolSize);
					warmArrayExist = true;
					int k = 0;
					for (String dvN : ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)) {
						jCbc.jarray_string_setitem(names, k, dvN);
						jCbc.jarray_int_setitem(values, k, dvIntMap.get(dvN));
						k++;
					}
	
					ret = jCbc.solve_unified(model, solver, names, values, intSolSize, 0);
					
				} else {
					//intSolSize = 0;
					ret = jCbc.solve_unified(model, solver, null, null, 0, 0);
				}
	
				//jCbc.setIntegerTolerance(model, integerT);
				//writeCbcLp("", true);
				//int ret = jCbc.solve_unified(model, solver, names, values, intSolSize, 0);
				solveName=solve_u_ret.get(ret);
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
			
			} else if (solvFunc == solv2){
				solveName="2__";
				jCbc.setPrimalTolerance(model, solve_2_primalT);
				jCbc.setIntegerTolerance(model, integerT);
				jCbc.solve_2(model, solver, 0);	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				
			} else if (solvFunc == solv3){
				solveName="3__";
				jCbc.setPrimalTolerance(model, solve_3_primalT);
				jCbc.setIntegerTolerance(model, integerT);
				jCbc.solve_3(model, solver, 0);		
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				
			} else if (solvFunc == solvCallCbc){
				solveName="cal";
				jCbc.setIntegerTolerance(model, integerT);
				jCbc.callCbc("-log 0 -solve", model);
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				
			} else if (solvFunc == solvFull){
				
			Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
			a.retainAll(dvIntMap.keySet());
			if ((useWarm || saveWarm) && a.size()>2) {
	//			int ii = ControlData.currCycleIndex+1;
	//			System.out.println(ii+": useWarm:"+useWarm);
	//			System.out.println(ii+": saveWarm:"+saveWarm);		
	//			System.out.println(ii+":dvIntMap size: "+dvIntMap.size());
				if (warmArrayExist) {
	
					jCbc.delete_jarray_int(values);
					jCbc.delete_jarray_string(names);
					intSolSize = 0;
					values = null;
					names = null;
					warmArrayExist = false;
				}			
				
				intSolSize = ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size();
				names = jCbc.new_jarray_string(intSolSize);
				values = jCbc.new_jarray_int(intSolSize);
				warmArrayExist = true;
				int k=0;
				for (String dvN: ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)){
					jCbc.jarray_string_setitem(names,k,dvN);
	//				System.out.println("values:"+values);
	//				System.out.println("k:"+k);
	//				System.out.println("dvN:"+dvN);
	//				System.out.println("dvIntMap.keySet():"+dvIntMap.keySet());
	//				for (String x:dvIntMap.keySet()) {
	//					System.out.println(x+":"+dvIntMap.get(x));
	//				}
					jCbc.jarray_int_setitem(values,k,dvIntMap.get(dvN));
					k++;
				}
				
				solveName="whs";
				jCbc.setPrimalTolerance(model, solve_whs_primalT);
				jCbc.setIntegerTolerance(model, integerT);
				jCbc.solve_whs(model,solver,names,values,intSolSize,0);
							
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);		
				
				if (status != 0 || status2 != 0) {
					
					//note_msg(jCbc.getModelName(solver), " Warmstart infeasible");
					reloadProblem(true, "");	
					if (warm_2nd_solvFunc == solv2){
						//note_msg(jCbc.getModelName(solver), " Use solve_2");
						solve_2();
					} else if (warm_2nd_solvFunc == solv3){
						//note_msg(jCbc.getModelName(solver), " Use solve_3");
						solve_3();
					} else {
						System.out.println("Error in warm 2nd solve function.");
					}	
				}	
				
			} else {
				
				if (warm_2nd_solvFunc == solv2){
					solve_2();
				} else if (warm_2nd_solvFunc == solv3){
					solve_3();
				} else {
					System.out.println("Error in warm 2nd solve function.");
				}
			}
	
			status = jCbc.status(model);
			status2 = jCbc.secondaryStatus(model);
	
			if (status != 0 || status2 != 0) {
				note_msg(" Solve_"+solveName+" infeasible. Use solve_2 with primalT="+solve_2_primalT_relax);
				reloadProblem(false, "");
				solve_2(solve_2_primalT_relax, "2R_");	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
			}	
	
	//		if (status != 0 || status2 != 0) {
	//			//note_msg(jCbc.getModelName(solver), " Solve_"+solveName+" infeasible. Use solve_2 with primalT="+solve_2_primalT_relax_most);
	//			reloadProblem();
	//			solve_2(solve_2_primalT_relax);	
	//			status = jCbc.status(model);
	//			status2 = jCbc.secondaryStatus(model);
	//		}	
			
	//		if (status != 0 || status2 != 0) {
	//			note_msg(jCbc.getModelName(solver), " Solve_"+solveName+" infeasible. Use callCbc -primalT 1e-9 -integerT 1e-9 ");
	//			reloadProblem(false);
	//			callCbc();	
	//			status = jCbc.status(model);
	//			status2 = jCbc.secondaryStatus(model);
	//		}	
			
	//		if (status != 0 || status2 != 0) {
	//			note_msg(jCbc.getModelName(solver), " Solve_"+solveName+" infeasible. Use callCbc -primalT 1e-7 -integerT 1e-9 ");
	//			reloadProblem(false);
	//			callCbc_R();	
	//			status = jCbc.status(model);
	//			status2 = jCbc.secondaryStatus(model);
	//		}	
			
			
			////// check for violation and re-solve with v2.10a
			int Err_int = 0; int Err_lb = 0;		
				if (status == 0 && status2 == 0) {
	
					int ColumnSize = jCbc.getNumCols(model);
					SWIGTYPE_p_double v_ary = jCbc.getColSolution(solver);
					Map<String, Dvar> dMap = SolverData.getDvarMap();
					
					for (int j = 0; j < ColumnSize; j++){
						 //varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));
						double v = jCbc.jarray_double_getitem(v_ary,j);
						String k = jCbc.getColName(model,j);
						
						if (jCbc.isInteger(solver,j)==1) {	 	
							if (Math.abs(v-Math.round(v))>integerT_check){	
								Err_int = 1; 
								ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":intViolation:::"+k+":"+v, true, false);
								//reloadAndWriteLp("_intViolation", true); 
								//break;
							} 
						} 
						else {
			
							Dvar d = dMap.get(k);												
							if (d.lowerBoundValue.doubleValue() == 0 &&  v<-lowerBoundZero_check){
								Err_lb = 1;
								ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":lbViolation:::"+k+":"+v, true, false);
								//reloadAndWriteLp("_lbViolation", true); 
								//break;
							} 						
							
						}					 
							 
					}					
					
				}
				
				if (Err_int>0 || Err_lb>0){
									
					reloadAndWriteLp("_lbViolation", true); 
					if (cbcViolationRetry) {
						note_msg(" Solve_"+solveName+" has violations. Use callCbc");
						reloadProblem(false, "");
						callCbc();	
						status = jCbc.status(model);
						status2 = jCbc.secondaryStatus(model);
					}
				}	
			}
		  
			
			long endT = System.currentTimeMillis();
	
			double time_second = (endT - beginT) / 1000.;
			ControlData.solverTime_cbc += time_second;
			ControlData.solverTime_cbc_this = time_second;
			if (ControlData.writeCbcSolvingTime) ILP.writeNoteLn(jCbc.getModelName(solver), " "+ time_second);
			
			if (status != 0 || status2 != 0) {
				//System.out.println("hello");
				reloadAndWriteLp("_infeasible", true, true);
				getSolverInformation(status, status2);
				iis();
			}
	
			return new int[]{status, status2};
		    	
		}

	
	public static int[] solve_jCbc2021(){
			int ci=ControlData.currCycleIndex+1;
			if (ControlData.showRunTimeMessage) System.out.println("CBC Solver2021: Solving "+ControlData.currMonth+"/"+ControlData.currDay+"/"+ControlData.currYear+" Cycle "+ci+" ["+ControlData.currCycleName+"]");
			
			int status=-99;
			int status2=-99;
			
			long beginT = System.currentTimeMillis();
		    	
//			if (dvIntPredict.size()>6 && dvIntPredict.size()>intVarSize-2){
//				
//				callCbc("cp_");	
//				status = jCbc.status(model);
//				status2 = jCbc.secondaryStatus(model);
//				isViolated = violationCheck(model, modelName, solveName);									
//				
//				if (status != 0 || status2 != 0) {
//					note_msg(" Solve_"+solveName+" infeasible.");
//
//				}
//				
//			}

				
			Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
			a.retainAll(dvIntMap.keySet());
			if ((useWarm || saveWarm) && a.size()>2) {
	//			int ii = ControlData.currCycleIndex+1;
	//			System.out.println(ii+": useWarm:"+useWarm);
	//			System.out.println(ii+": saveWarm:"+saveWarm);		
	//			System.out.println(ii+":dvIntMap size: "+dvIntMap.size());
				if (warmArrayExist) {
	
					jCbc.delete_jarray_int(values);
					jCbc.delete_jarray_string(names);
					intSolSize = 0;
					values = null;
					names = null;
					warmArrayExist = false;
				}			
				
				intSolSize = ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size();
				names = jCbc.new_jarray_string(intSolSize);
				values = jCbc.new_jarray_int(intSolSize);
				warmArrayExist = true;
				int k=0;
				for (String dvN: ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)){
					jCbc.jarray_string_setitem(names,k,dvN);
	//				System.out.println("values:"+values);
	//				System.out.println("k:"+k);
	//				System.out.println("dvN:"+dvN);
	//				System.out.println("dvIntMap.keySet():"+dvIntMap.keySet());
	//				for (String x:dvIntMap.keySet()) {
	//					System.out.println(x+":"+dvIntMap.get(x));
	//				}
					jCbc.jarray_int_setitem(values,k,dvIntMap.get(dvN));
					k++;
				}
			
				solveName="whs";
				jCbc.setPrimalTolerance(model, solve_whs_primalT);
				jCbc.setIntegerTolerance(model, integerT);
				jCbc.solve_whs(model,solver,names,values,intSolSize,0);					
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);	
				
				if (status != 0 || status2 != 0 ) {				
					if (isLogging) { note_msg(" Solve_"+solveName+" infeasible."); }
					reloadProblem(false, "");	
					solve_2();	
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);	
				} else if (violationCheck(model, modelName, solveName)) {
					if (isLogging) { note_msg(" Solve_"+solveName+" tolerance violation."); }
					reloadProblem(true, "");
					solve_2();	
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);	
				}

				
			} else {
					solve_2();
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);	
			}
	

			
			if (status != 0 || status2 != 0 ) {
				note_msg(" Solve_"+solveName+" infeasible. Use solve_2 with primalT="+solve_2_primalT_relax);
				reloadProblem(false, "");
				solve_2(solve_2_primalT_relax, "2R_");	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);	
			}// don't use solve2R if solve2 has violation
	
			if (status != 0 || status2 != 0 ) {
				note_msg(jCbc.getModelName(solver)+" Solve_"+solveName+" infeasible. Use callCbc -primalT 1e-9 -integerT 1e-9 ");
				reloadProblem(false, "");
				callCbc();	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);	
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
			} else if (violationCheck(model, modelName, solveName)) {
				reloadProblem(true, "");
				callCbc();	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);	
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
			}
			
			if (status != 0 || status2 != 0 ) {
				note_msg(jCbc.getModelName(solver)+" Solve_"+solveName+" infeasible. Use callCbc -primalT 1e-7 -integerT 1e-9 ");
				reloadProblem(false, "");
				callCbc_R();	
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
			} // Don't use callCbcR if callCbc has violation
	
					
				
//			}
			
	  
			
			long endT = System.currentTimeMillis();
	
			double time_second = (endT - beginT) / 1000.;
			ControlData.solverTime_cbc += time_second;
			ControlData.solverTime_cbc_this = time_second;
			if (ControlData.writeCbcSolvingTime) ILP.writeNoteLn(jCbc.getModelName(solver), " "+ time_second);
			
			if (status != 0 || status2 != 0) {

				reloadAndWriteLp("_infeasible", true, true);
				getSolverInformation(status, status2);
				iis();
			}
	
			return new int[]{status, status2};
		    	
		}

	private static void iis() {
		// test
		// final String[] SET_VALUES = new String[]{ "from_omr_np", "compare_omr_p"  };
		// prioritizeSearchTheseConstraints = new LinkedHashSet<String>(Arrays.asList(SET_VALUES));
		// end test
int pp=0;
		iisConstraintIndexMap = new LinkedHashMap<String, int[]>();
		iisConstraintElementMap = new LinkedHashMap<String, double[]>();
		iisConstraintSignMap = new LinkedHashMap<String, String>();
		iisConstraintRHSMap = new LinkedHashMap<String, Double>();
		iisSlacks = new ArrayList<String>();
		iisPossibleConstraintMap = new LinkedHashMap<String, String>();
		iisPossibleConstraintMap_cumulative = new LinkedHashMap<String, String>();
		iisConfirmConstraint = new LinkedHashSet<String>();
		
		InfeasibilityAnalysis.procIfsFile();
		prioritizeSearchTheseConstraints=InfeasibilityAnalysis.constraintSet;
		if (prioritizeSearchTheseConstraints!=null && prioritizeSearchTheseConstraints.size()>0) {
			hasPriorityConstraints = true;
			_pstc = new LinkedHashSet<String>(prioritizeSearchTheseConstraints);
		}
	

		boolean isFirstTimeRun = true;
		boolean success = true;
		long ts = Calendar.getInstance().getTimeInMillis();
		int tr=0;
		boolean isConflictFound = false;
		
		while (success) {
			
			tr =(int) (Calendar.getInstance().getTimeInMillis()-ts)/1000;
			if (tr>CbcSolver.cbcHintTimeMax) {
				ILP.writeNoteLn("\r\nInfeasibility analysis stopped due to time limit exceeded.", true, true);
				break;}
			
			iisPossibleConstraintMap_cumulative.putAll(iisPossibleConstraintMap);

			if (hasPriorityConstraints) {				
				_pstc.removeAll(iisConfirmConstraint);
				if (_pstc.size()<1) {hasPriorityConstraints = false;}
				}
			iisPossibleConstraintMap.clear();
			iisConfirmConstraint.clear();
//					System.out.println(hasPriorityConstraints);
//					System.out.println(_pstc);					
			success = iisSolve(isFirstTimeRun, iisPossibleConstraintMap_cumulative.keySet());
			//isFirstTimeRun=false;
			
			if (hasPriorityConstraints) {
				if (!success || iisPossibleConstraintMap.size()<1){
					ILP.writeNoteLn("End priority search.",true,false);
					hasPriorityConstraints = false;
					_pstc = null;
					success = true;
					continue;
				}
			}
				
			if (!success || iisPossibleConstraintMap.size()<1) {
				ILP.writeNoteLn("Infeasibility analysis ended.", true, false);
				if (iisPossibleConstraintMap_cumulative.size()>0) {
					//if (true) {
					if (!isConflictFound) {
						// output unconfirmed hints
						ILP.writeNoteLn("The following constraints might cause infeasibility.", true, false);
						for (String c: iisPossibleConstraintMap_cumulative.keySet()){
							ILP.writeNoteLn(Tools.findGoalLocation(c)+" "+iisPossibleConstraintMap_cumulative.get(c),true,true);
						}
					}	
				}
				return;
			} else {
				ILP.writeNoteLn("Finding constraints that cause infeasibility...", true, false);
//				System.out.println("iisPossibleConstraintMap_cumulative.size():"+iisPossibleConstraintMap_cumulative.size());
//				System.out.println("iisPossibleConstraintMap.size():"+iisPossibleConstraintMap.size());
//				System.out.println("total_relaxed:"+total_relaxed_constraints);
			}
			
	
			for (String c : iisPossibleConstraintMap.keySet()){
				if(iisSolveConfirm(c)){
					isConflictFound = true;
					iisConfirmConstraint.add(c); //pp++; ILP.writeNoteLn("found:"+pp,true,true);
				}
			}
			
			if (iisConfirmConstraint.size()>0) {
				for (String c : iisConfirmConstraint){
						ILP.writeNoteLn(Tools.findGoalLocation(c)+" "+iisPossibleConstraintMap.get(c),true,true);
				}
			}
//			// test
//			if (iisPossibleConstraintMap.keySet().size()>0) {
//				ILP.writeNoteLn("\n@@all_relaxed_contraints:\n");
//				for (String c :  iisPossibleConstraintMap.keySet()){
//					ILP.writeNoteLn(iisPossibleConstraintMap.get(c),true,true);
//				}
//				ILP.writeNoteLn("",false,false);
//				for (String c :  iisPossibleConstraintMap.keySet()){
//					ILP.writeNoteLn(Tools.findGoalLocation(c)+" "+iisPossibleConstraintMap.get(c),true,true);
//				}	
//				ILP.writeNoteLn("",false,false);
//			}
//			// end test
			
		}
	}

	private static boolean iisSolveConfirm(String skipThisConstraint) {
		boolean success = false;

		reloadProblemConfirm(skipThisConstraint);
		//writeCbcLp("iisSolveConfirm", false);
		//solve_3();
		if (CbcSolver.usejCbc2021a) {
			jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
		} else {
			callCbc();
		}
		int s = jCbc.status(model);
		int s2 = jCbc.secondaryStatus(model);

		if (s == 0 && s2 == 0) {
			success = true;
		} else {
			success = false;
		}

		return success;
	}
	
	private static boolean iisSolve(boolean isFirstTimeRun, Set<String> enforceThisConstraint) {
		boolean success = false;

		loadProblemIIS(isFirstTimeRun, enforceThisConstraint);
		//writeCbcLp("iisSolve", false);
		
		//solve_2();
		if (CbcSolver.usejCbc2021a) {
			jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
		} else {
			callCbc();
		}
		int s = jCbc.status(model);
		int s2 = jCbc.secondaryStatus(model);

		if (s == 0 && s2 == 0) {
			for (int j : iisSlackMap.keySet()) {
				String name = iisSlackMap.get(j);
				String name2 = jCbc.getColName(model, j);
				// check if name matches
				if (!name2.substring(0, name2.length() - 2).equalsIgnoreCase(name)) {
					System.out.println("@@ " + jCbc.getColName(model, j));
					System.out.println("@@ " + name);
				} else {
					// check if solution not zero
					if (jCbc.jarray_double_getitem(jCbc.getColSolution(solver), j) > 0) {
						// print this constraint
						int[] index = iisConstraintIndexMap.get(name);
						double[] coeff = iisConstraintElementMap.get(name);
						String show = name + ": ";
						// for (int r=index.length-1;r>-1;r--){
						for (int r = 0; r < index.length; r++) {
							int k = index[r];
							String var = dvBiMap.get(k);
							double coef = coeff[r];
							if (r != 0) {
								if (coef >= 0) {
									show = show + " +";
								} else {
									show = show + " ";
								}
							}
							
							String coefPrint = "";

							if(coef==-1){
								coefPrint = "-";
							} else if(coef==1){
								coefPrint = "";
							} else {
								coefPrint = Tools.noZerofmt(coef);
							}
							show = show + coefPrint + " " + var;
						}
						show += " " + iisConstraintSignMap.get(name);
						show += " " + Tools.noZerofmt(iisConstraintRHSMap.get(name));
						iisPossibleConstraintMap.put(name, show);
					}
				}

			}
			success = true;
		} else {
			success = false;
		}

		return success;
	}

	private static void collectDvar() {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Collecting dvars");
		
		int ColumnSize = jCbc.getNumCols(model);
		varDoubleMap = new LinkedHashMap<String, Double>();
		dvIntMap = new LinkedHashMap<String, Integer>();
				
		if (saveWarm||useWarm||saveIntVars) {
			//int ii = ControlData.currCycleIndex + 1;
			//System.out.println(ii + ": save warm");


			//int k = 0;
			for (int j = 0; j < ColumnSize; j++){
				 varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));
				 if (jCbc.isInteger(solver,j)==1) {
					dvIntMap.put(jCbc.getColName(model,j), (int)Math.round(jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j)));
					// System.out.println(jCbc.getColName(model,j)+":"+jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));				 	//k++; 
				 }
			}
		} else {

			for (int j = 0; j < ColumnSize; j++){
				varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));
	
			}
		}

	}

	private static void collectDvar2021() {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Collecting dvars");
		
		int ColumnSize = jCbc.getNumCols(model);
		varDoubleMap = new LinkedHashMap<String, Double>();
		dvIntMap = new LinkedHashMap<String, Integer>();
				
//		if (saveWarm||useWarm||saveIntVars) {
			//int ii = ControlData.currCycleIndex + 1;
			//System.out.println(ii + ": save warm");


			//int k = 0;
			for (int j = 0; j < ColumnSize; j++){
				 varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(model),j));
				 if (jCbc.isInteger(solver,j)==1) {
					dvIntMap.put(jCbc.getColName(model,j), (int)Math.round(jCbc.jarray_double_getitem(jCbc.getColSolution(model),j))); 
				 }
			}
			dvIntMap2021.putAll(dvIntMap);
//		} else {
//
//			for (int j = 0; j < ColumnSize; j++){
//				varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(model),j));
//	
//			}
//		}

	}
	
	private static LinkedHashMap<String, Double> collectDvar2021_simple() {

		int ColumnSize = jCbc.getNumCols(model);
		LinkedHashMap<String, Double> vMap = new LinkedHashMap<String, Double>();
		
		for (int j = 0; j < ColumnSize; j++){
			 vMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(model),j));

		}
		return vMap;
	
	}

	private static void assignDvar() {
		if (ControlData.showRunTimeMessage) System.out.println("CBC Solver: Assigning dvars\' values");
		
		Map<String, Map<String, IntDouble>> varCycleValueMap=ControlData.currStudyDataSet.getVarCycleValueMap();
		Map<String, Map<String, IntDouble>> varTimeArrayCycleValueMap=ControlData.currStudyDataSet.getVarTimeArrayCycleValueMap();
		Set<String> dvarUsedByLaterCycle = ControlData.currModelDataSet.dvarUsedByLaterCycle;
		Set<String> dvarTimeArrayUsedByLaterCycle = ControlData.currModelDataSet.dvarTimeArrayUsedByLaterCycle;
		ArrayList<String> timeArrayDvList = ControlData.currModelDataSet.timeArrayDvList;
		String modelName=ControlData.currCycleName;
		
		StudyDataSet sds = ControlData.currStudyDataSet;
		ArrayList<String> varCycleIndexList = sds.getVarCycleIndexList();
		ArrayList<String> dvarTimeArrayCycleIndexList = sds.getDvarTimeArrayCycleIndexList();
		Map<String, Map<String, IntDouble>> varCycleIndexValueMap = sds.getVarCycleIndexValueMap();
		
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
			
		HashSet<String> extraDv = new HashSet<String>(dvarMap.keySet());
		extraDv.removeAll(varDoubleMap.keySet());

		for (String dvName: extraDv) {
			Dvar dvar = dvarMap.get(dvName);
			double value=0;
			if (dvar.lowerBoundValue.doubleValue()>0) { value = dvar.lowerBoundValue.doubleValue();}
			varDoubleMap.put(dvName, value);
			IntDouble id = new IntDouble(value, false);
			dvar.setData(id);
			if(dvarUsedByLaterCycle.contains(dvName)){
				varCycleValueMap.get(dvName).put(modelName, id);
			}else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)){
				if (varTimeArrayCycleValueMap.containsKey(dvName)){
					varTimeArrayCycleValueMap.get(dvName).put(modelName, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(modelName, dvar.data);
					varTimeArrayCycleValueMap.put(dvName, cycleValue);
				}
			}
			if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)){
				if (varCycleIndexValueMap.containsKey(dvName)){
					varCycleIndexValueMap.get(dvName).put(modelName, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(modelName, dvar.data);
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
		
		//TODO: weird bug. need to fix		
		for (String dvName: varDoubleMap.keySet()) {
			Dvar dvar=dvarMap.get(dvName);		
			double value=varDoubleMap.get(dvName);
			IntDouble id=new IntDouble(value,false);

			//TODO: weird bug. need to fix
			try {
				dvar.setData(id);
			} catch (Exception e) {
				//System.out.println("dvName: "+dvName);
				dvar=new Dvar();
				dvar.upperBoundValue = maxValue;
				dvar.lowerBoundValue = 0.0;
				dvar.setData(id);
				dvarMap.put(dvName, dvar);
			}

			if(dvarUsedByLaterCycle.contains(dvName)){
				varCycleValueMap.get(dvName).put(modelName, id);
			}else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)){
				if (varTimeArrayCycleValueMap.containsKey(dvName)){
					varTimeArrayCycleValueMap.get(dvName).put(modelName, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(modelName, dvar.data);
					varTimeArrayCycleValueMap.put(dvName, cycleValue);
				}
			}
			if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)){
				if (varCycleIndexValueMap.containsKey(dvName)){
					varCycleIndexValueMap.get(dvName).put(modelName, dvar.data);
				}else{
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(modelName, dvar.data);
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
	
	public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String dvName, boolean isNoteCbc, String append){
		String c="";
		Dvar dvar=new Dvar();
		dvar.upperBoundValue = maxValue;
		dvar.lowerBoundValue = 0.0;
		dvarMap.put(dvName, dvar);
		
		double w = 0;
		if (wm2.keySet().contains(dvName)){
			w = -wm2.get(dvName).getValue();
		}
		//System.out.println(dvName+":"+w);
		jCbc.addCol(modelObject , 0, maxValue, w, dvName, dvar.integer==Param.yes ); 
	    if (isNoteCbc) {

	    	int isInt=0;
	    	if (dvar.integer==Param.yes) isInt=1;
	    	c = isInt+","+dvName+","+w+",0,"+maxValue; 
	    	Tools.quickLog(modelName+"_"+solveName+"_"+append+".cols", c, true);
	    }
	

	}
	
	public static void reloadAndWriteLp(String nameAppend, boolean logMps, boolean logCbc) {
		
		String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		reloadProblem(logCbc, nameAppend);
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);
		//jCbc.writeMps(model, oPath);
		if(logMps){jCbc.writeMps1(model, oPath+".mps", 1, 2);}
		
		logIntVars(label);
		
		ILP.writeNoteLn("CbcTolerancePrimal: "+CbcSolver.solve_2_primalT);
		ILP.writeNoteLn("CbcTolerancePrimalRelax: "+CbcSolver.solve_2_primalT_relax);
		ILP.writeNoteLn("CbcToleranceInteger: "+CbcSolver.integerT);
		ILP.writeNoteLn("CbcToleranceIntegercheck: "+CbcSolver.integerT_check);	
		ILP.writeNoteLn("CbcToleranceWarmPrimal: "+CbcSolver.solve_whs_primalT);
		ILP.writeNoteLn("CbcToleranceZero: "+ControlData.zeroTolerance);
		ILP.writeNoteLn("CbcHintTimeMax: "+CbcSolver.cbcHintTimeMax);
		ILP.writeNoteLn("CbcHintRelaxPenalty: "+CbcSolver.cbcHintRelaxPenalty);
		ILP.writeNoteLn("CbcSolutionRounding: "+CbcSolver.cbcSolutionRounding);
	}
	
	public static void reloadAndWriteLp(String nameAppend, boolean logCbc) {
		
		String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		reloadProblem(logCbc, nameAppend);
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);
		
		logIntVars(label);

	}
	
	public static void writeCbcLp(String nameAppend, boolean logMps) {
		
		String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		//reloadProblem();
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);
		//jCbc.writeMps(model, oPath);
		if(logMps){jCbc.writeMps1(model, oPath+".mps", 1, 2);}
		
		logIntVars(label);

	}
	
	private static void logIntVars(String label){
		
		if (ControlData.currStudyDataSet.cycIntDvMap != null) {
			if (ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size() > 0) {

				String c = "";
				for (String dvN : ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)) {
					c = c + dvN + "," + dvIntMap.get(dvN) + "\n";
				}
				Tools.quickLog(label + ".intVars", c);
			}
		}	
	}

	public static void logCbcWatchList(StudyDataSet sds){
				
		// write watch var values
		boolean recordLP = false;
		boolean recordVar = false;
		double wa_cbc = 0;
		double wa_xa  = 0;
		if (ControlData.watchList != null) {
			String wa_cbc_str = "";
			String wa_xa_str = "";
			for (String s : ControlData.watchList) {
				if (ControlData.currModelDataSet.dvList.contains(s)){
				
					//System.out.println(s);
					wa_cbc = CbcSolver.varDoubleMap.get(s);

					wa_cbc_str += String.format("%14.3f", wa_cbc) + "  ";

					

				}
			}
			ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_cbc_str, ILP._watchFile_cbc);
			
		}
		
	
	}
	
	
	public static void logCbcDebug(StudyDataSet sds){
			
//		ILP.getIlpDir();
//		ILP.setVarDir();
		//ILP.setSvarFile();
//		ILP.setMaximumFractionDigits();
		// write svar
		//ILP.writeSvarValue();
		// write dvar
		ILP.findDvarEffective();
		//System.out.println("write cbc dvar");
		ILP.setDvarFile("cbc");
		ILP.writeDvarValue_Clp0_Cbc0(CbcSolver.varDoubleMap);
		//System.out.println("write xa dvar");
		ILP.setDvarFile("xa");
		ILP.writeDvarValue_XA();
		
		
		// write watch var values
		boolean recordLP = false;
		boolean recordVar = false;
		double wa_cbc = 0;
		double wa_xa  = 0;
		if (ControlData.watchList != null) {
			String wa_cbc_str = "";
			String wa_xa_str = "";
			for (String s : ControlData.watchList) {
				if (ControlData.currModelDataSet.dvList.contains(s)){
				
					//System.out.println(s);
					wa_cbc = CbcSolver.varDoubleMap.get(s);
					wa_xa  = ControlData.xasolver.getColumnActivity(s);
					wa_cbc_str += String.format("%14.3f", wa_cbc) + "  ";
					wa_xa_str += String.format("%14.3f",  wa_xa) + "  ";
					
					if (Math.abs(wa_xa-wa_cbc)>ControlData.watchList_tolerance) recordLP=true; 
				}
			}
			ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_cbc_str, ILP._watchFile_cbc);
			ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_xa_str, ILP._watchFile_xa);
			
		}
		
		// write int value, time, and obj diff
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ControlData.xasolver.getObjective(), ILP._noteFile_xa_obj);
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ControlData.clp_cbc_objective, ILP._noteFile_cbc_obj);
		
		String xa_int = "";
		String cbc_int = "";
		if (sds.cycIntDvMap != null) {
			ArrayList<String> intDVs = new ArrayList<String>(sds.cycIntDvMap.get(ControlData.currCycleIndex));
			for (String v :sds.allIntDv) {
				if (intDVs.contains(v)){
					xa_int  += " "+ Math.round(ControlData.xasolver.getColumnActivity(v));
					if (Error.getTotalError()==0) {
						cbc_int += " "+ Math.round(CbcSolver.varDoubleMap.get(v));
					} else {
						cbc_int += " ?";
					}
				} else {
					xa_int  += "  ";
					cbc_int += "  ";
				}
			}
		}
		
		// write solve name
		cbc_int +=  "  "+CbcSolver.solveName;
		xa_int  +=  "  "+CbcSolver.solveName;
		
		if (Error.getTotalError() == 0) {
			if (recordLP) {
				CbcSolver.reloadAndWriteLp("_watch_diff", true, true);
			}
			
			double diff = ControlData.clp_cbc_objective - ControlData.xasolver.getObjective();
			if (Math.abs(diff) > CbcSolver.record_if_obj_diff) {
				CbcSolver.reloadAndWriteLp("_obj"+Math.round(diff), true, true);
			}
			if (Math.abs(diff) > CbcSolver.log_if_obj_diff) {
				xa_int += "  obj: " + String.format("%16.3f", diff);
				cbc_int += "  obj: " + String.format("%16.3f", diff);
				if (diff>CbcSolver.maxObjDiff){
					CbcSolver.maxObjDiff = diff;
					CbcSolver.maxObjDiff_id = ILP.getYearMonthCycle();
				} else if(diff<CbcSolver.maxObjDiff_minus){
					CbcSolver.maxObjDiff_minus = diff;
					CbcSolver.maxObjDiff_minus_id = ILP.getYearMonthCycle();										
				}
			}
		}
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ xa_int, ILP._noteFile_xa_int);
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ cbc_int, ILP._noteFile_cbc_int);		
	}
	
	public static void main(String argv[]) {

		System.loadLibrary(cbcLibName); 

		SWIGTYPE_p_OsiClpSolverInterface solver = jCbc.new_jOsiClpSolverInterface(); 
		SWIGTYPE_p_CbcModel model = jCbc.new_jCbcModel();
		jCbc.assignSolver(model, solver); 

		

		//jCbc.readLp(solver, "1961_01_c01.lp");
		jCbc.readLp(solver, "1961_01_c06.lp");
		jCbc.setModelName(solver, "TestName");

		
		long beginT = System.currentTimeMillis();
		
		jCbc.setPrimalTolerance(model, 1e-06);
		jCbc.solve_2(model, solver, 3);
		System.out.println("model name: "+jCbc.getModelName(solver));
		System.out.println("status: "+jCbc.status(model)+" "+jCbc.secondaryStatus(model));
		
		long endT = System.currentTimeMillis();
		
		double time_second = (endT-beginT)/1000.;

		System.out.println("time(sec): "+time_second);

		if (jCbc.status(model) == 0 && jCbc.secondaryStatus(model) == 0) {

			int nCols = jCbc.getNumCols(model);

			SWIGTYPE_p_double sol = jCbc.new_jarray_double(nCols);
			sol = jCbc.getColSolution(solver);

			System.out.println("Solution:\n");
			System.out.println("Objective Value = " + jCbc.getObjValue(model)
					+ "\n");

			for (int j = 0; j < nCols; j++) {
				if (jCbc.isInteger(solver, j) == 1)
					System.out.println(jCbc.getColName(model, j) + " = "
							+ jCbc.jarray_double_getitem(sol, j));
			}
		} else if (jCbc.secondaryStatus(model) == 1) {
			System.out.println("Model Infeasible.");

		} else {
			System.out.println("Errors!! "+jCbc.status(model)+" "+jCbc.secondaryStatus(model));
		}
		jCbc.delete_jCbcModel(model);

	}
	
	private static void note_msg(String msg) {
		ILP.writeNoteLn(jCbc.getModelName(solver), msg);
		System.out.println(jCbc.getModelName(solver)+ msg);
	}
	
	private static int solve_2() {
		int r = -99;
		r = solve_2(solve_2_primalT, "2__");
		return r;
	}
	
	private static int solve_2(double priT, String solvName) {
		int r = -99;
		solveName=solvName;
		jCbc.setPrimalTolerance(model, priT);
		jCbc.setIntegerTolerance(model, integerT);
		r = jCbc.solve_2(model, solver, 0);
		return r;
	}
	
	private static void solve_3() {
		solveName="3__";
		jCbc.setPrimalTolerance(model, solve_3_primalT);
		jCbc.setIntegerTolerance(model, integerT);
		jCbc.solve_3(model, solver, 0);
	}
	
	private static void callCbc() {
		callCbc("c__");
	}
	
	private static void callCbc(String solvName) {
		solveName=solvName;
		//jCbc.setIntegerTolerance(model, integerT);
		//jCbc.callCbc("-log 0 -preprocess off -presolve off -cutsOnOff off -primalT 1e-4 -integerT 1e-7 -solve", model);
		//jCbc.callCbc("-log 0 -preprocess off -presolve on -cutsOnOff on -primalT 1e-5 -integerT 1e-8 -solve", model);
		//jCbc.callCbc("-log 0 -preprocess off -presolve on -cutsOnOff on -primalT "+solve_2_primalT_relax+" -integerT "+integerT+" -solve", model);
		jCbc.callCbc("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model);
	}

	private static void callCbc_R() {
		solveName="cR_";
		jCbc.callCbc("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model);
	}
	
	public static void logIntCheck(StudyDataSet sds){
			
			String cbc_int = "";
			
			// write solve name
			cbc_int +=  ","+CbcSolver.solveName;
			
			// write solve time
			//cbc_int +=  ","+String.format("%8.2f",ControlData.solverTime_cbc_this);
			
			if (sds.cycIntDvMap != null && sds.cycIntDvMap.containsKey(ControlData.currCycleIndex)) {
				ArrayList<String> intDVs = new ArrayList<String>(sds.cycIntDvMap.get(ControlData.currCycleIndex));
				Boolean int_violation = false;
				for (String v : sds.allIntDv) {
					if (intDVs.contains(v)){
						//xa_int  += " "+ Math.round(ControlData.xasolver.getColumnActivity(v));
						if (Error.getTotalError()==0) {
							//cbc_int += " "+ Math.round(CbcSolver.varDoubleMap.get(v));
							double x = CbcSolver.varDoubleMap.get(v);
							cbc_int += ","+ x;
							if   (Math.abs( Math.round(x)-x)>1E-7) {int_violation=true;}
							
						} else {
							cbc_int += ",?";
							int_violation=null;
						}
					} else {
						//xa_int  += "  ";
						cbc_int += ",";
					}
				}
				cbc_int = "," + int_violation + cbc_int;
			}
	
			ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ cbc_int, ILP._noteFile_cbc_int_log);					
		
	}
	
	public static boolean violationCheck(SWIGTYPE_p_CbcModel model, String modelName, String solveName) {
		boolean violation = false;
		if (!cbcViolationCheck) {return false;}
		int ColumnSize = jCbc.getNumCols(model);
		SWIGTYPE_p_double v_ary = jCbc.getColSolution(model);
		Map<String, Dvar> dMap = SolverData.getDvarMap();
		
		for (int j = 0; j < ColumnSize; j++){
			 //varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));
			double v = jCbc.jarray_double_getitem(v_ary,j);
			String k = jCbc.getColName(model,j);
			
			if (jCbc.isInteger(model,j)==1) {	 	
				if (Math.abs(v-Math.round(v))>integerT_check){	
					violation = true; 
					ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":intViolation:::"+k+":"+v, true, false);

				} 
			} 
			else {

				Dvar d = dMap.get(k);												
				if (d.lowerBoundValue.doubleValue() == 0 &&  v<-lowerBoundZero_check){
					violation = true; 
					ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":lbViolation:::"+k+":"+v, true, false);

				} 						
			}					 				 
		}
		return violation;					
		
	}

	public static int[] solve_jCbc2021a(){
				int ci=ControlData.currCycleIndex+1;
				if (ControlData.showRunTimeMessage) System.out.println("CBC Solver2021a: Solving "+ControlData.currMonth+"/"+ControlData.currDay+"/"+ControlData.currYear+" Cycle "+ci+" ["+ControlData.currCycleName+"]");
				
				//int status=-99;
				//int status2=-99;
				int rv = -99;
				
				long beginT = System.currentTimeMillis();
	
					
				Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
				a.retainAll(dvIntMap.keySet());

				if ((useWarm || saveWarm) && a.size()>2) {
		//			int ii = ControlData.currCycleIndex+1;
		//			System.out.println(ii+": useWarm:"+useWarm);
		//			System.out.println(ii+": saveWarm:"+saveWarm);		
		//			System.out.println(ii+":dvIntMap size: "+dvIntMap.size());
					if (warmArrayExist) {
		
						jCbc.delete_jarray_int(values);
						jCbc.delete_jarray_string(names);
						intSolSize = 0;
						values = null;
						names = null;
						warmArrayExist = false;
					}			
					
					intSolSize = ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size();
					names = jCbc.new_jarray_string(intSolSize);
					values = jCbc.new_jarray_int(intSolSize);
					warmArrayExist = true;
					int k=0;
					for (String dvN: ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)){
						jCbc.jarray_string_setitem(names,k,dvN);
						jCbc.jarray_int_setitem(values,k,dvIntMap.get(dvN));
						k++;
					}
				
					solveName="whs";
					jCbc.setPrimalTolerance(model, solve_whs_primalT);
					jCbc.setIntegerTolerance(model, integerT);
					rv=jCbc.solve_whs(model,solver,names,values,intSolSize,0);					
					//status = jCbc.status(model);
					//status2 = jCbc.secondaryStatus(model);	
					if (rv!=0){
						if (rv==91 || rv==92) {
							note_msg(" Solve_"+solveName+" exception:"+rv+". Use callCbc.");
							reloadProblem(true, "exception"+rv);
						} else {
							//note_msg(" Solve_"+solveName+" infeasible. Use callCbc.");
							reloadProblem(false, "");
						} 
						solveName="c__";
		                rv = jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
						//if (rv==0) { rv = jCbc.Y2_simple(model,solver); }
					}

//					if (rv==91 || rv==92) {
//						note_msg(" Solve_"+solveName+" exception:"+rv+".");
//						reloadProblem(true, "exception"+rv);
//						rv=jCbc.solve_whs(model,solver,names,values,intSolSize,0);	
//					}
//					if (rv==91 || rv==92) {
//						note_msg(" Solve_"+solveName+" exception:"+rv+".");
//						reloadProblem(true, "exception"+rv);
//						rv=jCbc.solve_whs(model,solver,names,values,intSolSize,0);	
//						if (rv==91 || rv==92) {
//							note_msg(" Solve_"+solveName+" exception:"+rv+". Continue.");
//						}
//					}
//					if (rv!=0) {				
//						if (isLogging) { note_msg(" Solve_"+solveName+" infeasible."); }
//						reloadProblem(false, "");	
//						rv=solve_2();	
//					} 
	
					
				} else {
					
					solveName="c__";
	                rv = jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
					//if (rv==0) { rv = jCbc.Y2_simple(model,solver); }
				}
					
				// for callCbc only
				int status=rv;
				int status2=rv;
				if (rv!=0){
					if (rv==91 || rv==92) {
						note_msg(" Solve_"+solveName+" exception:"+rv+". Use callCbcR.");
						reloadProblem(true, "exception"+rv);
					} else {
						note_msg(" Solve_"+solveName+" infeasible. Use callCbcR.");
						reloadProblem(false, "");
					} 
	                solveName="cR_";
	                rv=jCbc.callCbcJ("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model, solver);
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);
					//if (rv==0) { status2 = jCbc.Y2_simple(model,solver); }
				}
				

		
				//TODO: need to integrate callCbc status with whs and solve2 return value
			
					
	//			}
				
		  
				
				long endT = System.currentTimeMillis();
		
				double time_second = (endT - beginT) / 1000.;
				ControlData.solverTime_cbc += time_second;
				ControlData.solverTime_cbc_this = time_second;
				if (ControlData.writeCbcSolvingTime) ILP.writeNoteLn(jCbc.getModelName(solver), " "+ time_second);
				
				if (status != 0 || status2 != 0) {
	
					reloadAndWriteLp("_infeasible", true, true);
					getSolverInformation(status, status2);
					iis();
				}
		
				return new int[]{status, status2};
			    	
			}
	
}
