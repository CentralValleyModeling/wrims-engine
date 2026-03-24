package gov.ca.water.wrims.engine.core.solver;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.commondata.solverdata.*;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;
import gov.ca.water.wrims.engine.core.ilp.ILP;
import gov.ca.water.wrims.engine.core.tools.InfeasibilityAnalysis;
import gov.ca.water.wrims.engine.core.wreslplus.elements.Tools;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
/**
 * CBC Solver Implementation updates
        * WARNING: This class uses static state and is NOT thread-safe.
        * Do not call methods of this class concurrently from multiple threads.
        * If parallel solving is required, create separate solver instances
        * or synchronize access to this class.
        *
        * Performance characteristics:
        * - High memory usage due to static model retention*
        * - Single-threaded solving only
        * - Not suitable for concurrent solving scenarios Bing 2025/12/09
*/

public class CbcSolver {

    private static final Logger logger = LoggerFactory.getLogger(CbcSolver.class);
    // Performance statistics tracking
    private static class PerformanceStats {
        private long totalSolverTime = 0;
        private long totalModelCreationTime = 0;
        private long totalConstraintSetupTime = 0;
        private long totalVariableSetupTime = 0;
        private int totalProblemsSolved = 0;

        public void recordSolverTime(long time) {
            totalSolverTime += time;
        }

        public void recordModelCreationTime(long time) {
            totalModelCreationTime += time;
        }

        public void recordConstraintSetupTime(long time) {
            totalConstraintSetupTime += time;
        }

        public void recordVariableSetupTime(long time) {
            totalVariableSetupTime += time;
        }

        public void incrementProblemsSolved() {
            totalProblemsSolved++;
        }

        public void logSummary() {
            if (totalProblemsSolved > 0) {
                logger.atDebug()
                        .setMessage("CBC performance summary: solved={}, solveMs={}, modelMs={}, constraintMs={}, variableMs={}, avgSolveMs={}")
                        .addArgument(totalProblemsSolved)
                        .addArgument(totalSolverTime)
                        .addArgument(totalModelCreationTime)
                        .addArgument(totalConstraintSetupTime)
                        .addArgument(totalVariableSetupTime)
                        .addArgument(totalSolverTime / totalProblemsSolved)
                        .log();
            }
        }
    }

    // Static instance for performance tracking
    private static PerformanceStats performanceStats = new PerformanceStats();

    // Variable declarations
    public static LinkedHashMap<String, Double> varDoubleMap;

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

    private static Map<String, Dvar> dvarMap;
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

    private static boolean useLpFile = false;

	private static int index[];
	private static double elements[];

    private static boolean saveWarm = false;
    private static boolean useWarm = false;
    private static final boolean saveIntVars = true;
	//private static boolean delWarm=false;

    private static LinkedHashMap<String, Integer> dvIntMap;
    private static LinkedHashMap<String, Integer> dvIntMap2021;
    private static ArrayList<String> dvIntPredict;
    private static int intVarSize = -99;

    private static SWIGTYPE_p_std__string names;
    private static SWIGTYPE_p_int values;
    private static int intSolSize = 0;
    private static boolean warmArrayExist = false;

    private static SWIGTYPE_p_std__string names_eachTS;
    private static SWIGTYPE_p_int values_eachTS;
    private static int intSolSize_eachTS = 0;
    private static boolean warmArrayExist_eachTS = false;

    private static SWIGTYPE_p_std__string names_dummy;
    private static SWIGTYPE_p_int values_dummy;

	// record max(cbc_obj - xa_obj) &  max(xa_obj - cbc_obj)
	public static double maxObjDiff=0;
	public static String maxObjDiff_id="";
	public static double maxObjDiff_minus=0;
	public static String maxObjDiff_minus_id="";
	private static HashSet originalDvarKeys=null;

    public static String solveName = "";
    public static boolean logObj = true;
    public static boolean intLog = true;
    public static boolean intViolation = false;

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
        put(6, "whs");
        put(5, "2__");
        put(4, "2_r");
        put(1, "inf");
    }};


    private CbcSolver() {}

    public static void init(boolean useLpFile, StudyDataSet sds) {
        PerformanceTimer timer = new PerformanceTimerCbc("CbcSolver initialization");

        dvIntMap2021 = new LinkedHashMap<String, Integer>();
        CbcSolver.useLpFile = useLpFile;

        ILP.getIlpDir();
        ILP.createNoteFile();

        if (lowerBoundZero_check == null) {
            lowerBoundZero_check = Math.max(solve_2_primalT_relax * 10, 1e-6);
        }

        logger.atInfo()
          .setMessage("solver configuration: cbcViolationCheck={}, lowerBoundZero_check={}, cbcSolutionRounding={}, usejCbc2021={}, usejCbc2021a={}, cbcVersion={}, whsScaling={}, useLpFile={}, cbcLibName={}")
          .addArgument(cbcViolationCheck)
          .addArgument(lowerBoundZero_check)
          .addArgument(cbcSolutionRounding)
          .addArgument(usejCbc2021)
          .addArgument(usejCbc2021a)
          .addArgument(cbcVersion)
          .addArgument(whsScaling)
          .addArgument(whsSafe)
          .addArgument(useLpFile)
          .addArgument(cbcLibName)
          .log();

        // Write configuration notes
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

        dvIntMap = new LinkedHashMap<String, Integer>();
        int intVarCount = 0;
        for (String d : sds.allIntDv) {
            dvIntMap.put(d, 0);
            intVarCount++;
        }
        logger.atInfo().setMessage("Found {} integer variables in the study dataset").addArgument(intVarCount).log();

        names_dummy = jCbc.new_jarray_string(0);
        values_dummy = jCbc.new_jarray_int(0);

        if (!useLpFile) {
            dvBiMap = HashBiMap.create();
            dvBiMapArray = new ArrayList<String>();
        }
        timer.stop();
    }

    public static void close() {
        logger.atDebug().setMessage("CbcSolver.close() method called").log();
        performanceStats.logSummary();
    }

    public static void newProblem() {
        logger.atDebug().setMessage("==================== New Problem Solving Session ====================").log();
        long totalStartTime = System.currentTimeMillis();

        dvIntPredict = new ArrayList<String>();

        // Configure warm start settings
        if (ControlData.useCbcWarmStart) {
            if (ControlData.cycWarmStart != null) {
                if (ControlData.cycWarmStart.contains(ControlData.currCycleIndex)) {
                    saveWarm = true;
                    logger.atDebug().setMessage("Configured to save warm start solution for this cycle").log();
                } else {
                    saveWarm = false;
                }
                if (ControlData.cycWarmUse.contains(ControlData.currCycleIndex)) {
                    useWarm = true;
                    logger.atDebug().setMessage("Configured to use warm start solution for this cycle").log();
                } else {
                    useWarm = false;
                }
            }
        }

        modelName = ILP.getYearMonthCycle();
		ControlData.clp_cbc_objective = null;

        PerformanceTimer creationTimer = new PerformanceTimer("CBC Model Creation");

		model = jCbc.new_jCbcModel();
		solver = jCbc.new_jOsiClpSolverInterface(); //this is our LP solver!
		jCbc.assignSolver(model,solver); // Assign the solver to CbcModel
		int currDate = ControlData.currYear*100 +ControlData.currMonth;
		isLogging = cbcLogStartDate <= currDate && currDate <=cbcLogStopDate;

        logger.atDebug().setMessage("New Problem Created: modelName={}, useLPFile={}, currentDate={}, loggingEnabled={}")
                .addArgument(modelName).addArgument(useLpFile).addArgument(currDate).addArgument(isLogging).log();

        if (useLpFile) {
            logger.atDebug().setMessage("Loading model from LP file: {}").addArgument(ILP.cplexLpFilePath).log();
            jCbc.readLp(solver, ILP.cplexLpFilePath);
            logger.atDebug().setMessage("LP file loaded successfully").log();

        } else {
            int sizeA = ControlData.currModelDataSet.dvList.size();
            int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();

            logger.atDebug().setMessage("Configured to save warm start solution for this cycle").log();

            for (int i = 0; i < sizeA; i++) {
                dvBiMap.put(i, ControlData.currModelDataSet.dvList.get(i));
                dvBiMapArray.add(ControlData.currModelDataSet.dvList.get(i));
            }
            for (int i = 0; i < sizeB; i++) {
                dvBiMap.put(i + sizeA, ControlData.currModelDataSet.dvTimeArrayList.get(i));
                dvBiMapArray.add(ControlData.currModelDataSet.dvTimeArrayList.get(i));
            }
            dvBiMapInverse = dvBiMap.inverse();

            logger.atDebug().setMessage("Variable mapping created: total variables={}").addArgument(dvBiMap.size()).log();

            originalDvarKeys = new HashSet<String>(SolverData.getDvarMap().keySet());
            logger.atDebug().setMessage("Original variable key set size: {}").addArgument(originalDvarKeys.size()).log();

            dvarMap = SolverData.getDvarMap();
            wm2 = SolverData.getWeightSlackSurplusMap();

            modelObject = jCbc.new_jCoinModel();


            if (usejCbc2021 && false) {
                logger.atDebug().setMessage("Using standard version for variable setup").log();
                setDVars2021(ControlData.cbcLogNativeLp);
            } else {
                logger.atDebug().setMessage("Using standard version for variable setup").log();
                setDVars(ControlData.cbcLogNativeLp || isLogging, "");
            }
            setConstraints(ControlData.cbcLogNativeLp || isLogging, "");
            if (ControlData.cbcLogNativeLp || isLogging) {
                logger.atDebug().setMessage("Writing CBC LP file for debugging").log();
                writeCbcLp("", false);
            }
        }

        jCbc.setModelName(solver, modelName);
        long creationTime = creationTimer.stop();
        ControlData.solverCreationTime_cbc += creationTime / 1000.0;
        performanceStats.recordModelCreationTime(creationTime);


        // Start solving process
        logger.atDebug().setMessage("Starting solving process...").log();
        PerformanceTimer solveTimer = new PerformanceTimer("Problem Solving");

        long beginT = System.currentTimeMillis();
        int[] solveResult;

        if (usejCbc2021a) {
            logger.atDebug().setMessage("Using jCbc2021a solver").log();
            solveResult = solve_jCbc2021a();
        } else if (usejCbc2021) {
            logger.atDebug().setMessage("Using jCbc2021 solver").log();
            solveResult = solve_jCbc2021();
        } else {
            logger.atDebug().setMessage("Using standard CBC solver").log();
            solveResult = solve();
        }

        long endT = System.currentTimeMillis();

        double time_second = (endT - beginT) / 1000.0;
        long solveTime = solveTimer.stop();

        ControlData.solverTime_cbc += time_second;
        ControlData.solverTime_cbc_this = time_second;
        performanceStats.recordSolverTime(solveTime);
        performanceStats.incrementProblemsSolved();

        logger.atDebug().setMessage("Solver execution completed: {} seconds").addArgument(time_second).log();

        if (ControlData.writeCbcSolvingTime) {
            ILP.writeNoteLn(jCbc.getModelName(solver), " " + time_second);
        }

        int status = solveResult[0];
        int status2 = solveResult[1];

        if (Error.error_solving.size() < 1) {
            ControlData.clp_cbc_objective = getObjValue();

            logger.atDebug().setMessage("Solver objective value: {}").addArgument(ControlData.clp_cbc_objective).log();

            if (CbcSolver.logObj) {
                ILP.writeNoteLn(ILP.getYearMonthCycle(), "" + ControlData.clp_cbc_objective, ILP._noteFile_cbc_obj);
                ILP.writeNoteLn(ILP.getYearMonthCycle(), "" + CbcSolver.solveName + "," + String.format("%8.2f", ControlData.solverTime_cbc_this),
                        ILP._noteFile_cbc_time);
            }

            // Collect variable results
            logger.atDebug().setMessage("Collecting variable results").log();
            PerformanceTimer collectTimer = new PerformanceTimer("Variable Result Collection");

            varDoubleMap = new LinkedHashMap<String, Double>();
            dvIntMap = new LinkedHashMap<String, Integer>();
            if (usejCbc2021) {
                collectDvar2021();
            } else {
                collectDvar();
            }

            collectTimer.stop();

            // Check for violations
            if (cbcViolationCheck) {
                logger.atDebug().setMessage("Checking for variable violations").log();
                PerformanceTimer violationTimer = new PerformanceTimer("Violation Check");

                boolean intErr = false;
                boolean lowerboundErr = false;

                Map<String, Dvar> dMap = SolverData.getDvarMap();
                for (String k : dvIntMap.keySet()) {
                    if (varDoubleMap.containsKey(k)) {
                        double v = varDoubleMap.get(k);
                        double rounded = Math.round(v);
                        double diff = Math.abs(v - rounded);

                        if (diff > integerT_check) {
                            intErr = true;
                            logger.atWarn().setMessage("Integer variable violation detected: {} = {} (error={})").addArgument(k).addArgument(v).addArgument(diff).log();
                            Error.addSolvingError("int violation:::" + k + ":" + v);
                        } else if (cbcSolutionRounding) {
                            varDoubleMap.put(k, rounded);
                            logger.atTrace().setMessage("Integer variable rounded: {}: {} -> {}").addArgument(k).addArgument(v).addArgument(rounded).log();
                        }
                    }
                }
                if (intErr) {
                    reloadAndWriteLp("_intViolation", true, true);
                    Error.addSolvingError("Integer Violation! Please contact developers for this issue.");
                }
                for (String k : dMap.keySet()) {
                    if (varDoubleMap.containsKey(k)) {
                        Dvar d = dMap.get(k);
                        double v = varDoubleMap.get(k);
                        if (d.lowerBoundValue.doubleValue() == 0) {
                            if (v < -lowerBoundZero_check) {
                                lowerboundErr = true;
                                logger.atWarn().setMessage("Lower bound violation detected: {} = {} (threshold={})").addArgument(k).addArgument(v).addArgument(lowerBoundZero_check).log();
                                Error.addSolvingError("lowerbound violation:::" + k + ":" + v);
                            } else if (cbcSolutionRounding && v < 0) {
                                varDoubleMap.put(k, 0.0);
                                logger.atTrace().setMessage("Lower bound adjustment: {}: {} -> 0.0").addArgument(k).addArgument(v).log();
                            }
                        }
                    }
                }

                if (lowerboundErr) {
                    reloadAndWriteLp("_lbViolation", true, true);
                    Error.addSolvingError("Lowerbound Violation! Please contact developers for this issue.");
                }

                violationTimer.stop();
            }

            if (isLogging) {
                ILP.setVarFile();
                ILP.writeSvarValue();
                ILP.findDvarEffective();
                ILP.writeDvarValue_Clp0_Cbc0(varDoubleMap);
            }

            // Assign variable values to data structures
            logger.atDebug().setMessage("Assigning variable values to data structures").log();
            PerformanceTimer assignTimer = new PerformanceTimer("Variable Assignment");

            if (!ControlData.cbc_debug_routeXA) {assignDvar();}

            assignTimer.stop();
        }

            // Performance debugging options
            if (time_second > 10.0) {
                logger.atWarn().setMessage("CBC solve time exceeded threshold: {} sec").addArgument(time_second).log();
                reloadAndWriteLp("stuck_" + Math.round(time_second), true);
                ILP.writeNoteLn(modelName, " " + solveName + " time(sec): " + time_second);

        }

        if (debugObjDiff) {
            logger.atDebug().setMessage("Objective difference debugging enabled").log();
            Double thisObj = ControlData.clp_cbc_objective;
            reloadProblem(false, "");
            callCbc(solveName);
            if (jCbc.status(model) == 0 && jCbc.secondaryStatus(model) == 0) {
                Double cbcObj = getObjValue();
                double objDiff = Math.abs(thisObj - cbcObj);

                if (objDiff > debugObjDiff_tolerance) {
                    logger.atWarn().setMessage("CBC objective difference too large: {} > {}").addArgument(objDiff).addArgument(debugObjDiff_tolerance).log();
                    reloadProblem(false, "");
                    jCbc.callCbc("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model);
                    if (jCbc.status(model) == 0 && jCbc.secondaryStatus(model) == 0) {
                        Double rObj = getObjValue();
                        double rDiff = Math.abs(thisObj - rObj);
                        if (rDiff > debugObjDiff_tolerance) {
                            int ap = (int) Math.round(rDiff / debugObjDiff_tolerance);
                            reloadAndWriteLp("objErr_" + ap + "_", true);
                        }
                    }
                }
            }
        }

		// debug whs deviation
        if (debugDeviation && solveName == "whs") {
            logger.atDebug().setMessage("Deviation debugging enabled for whs solve").log();
            // check watchlist for goal name, convert to slack surplus,
			// if one exceed then
			// increase penalty see if obj value change
			// if not then logging
			Map<String, WeightElement> wm1 = SolverData.getWeightMap();
            Map<String, WeightElement> wm2 = SolverData.getWeightSlackSurplusMap();
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
						note_msg(missing+": weight1: "+wm1.get(missing).getValue());
                    } else if (wm2.keySet().contains(missing)){
						note_msg(missing+": weight2: "+wm2.get(missing).getValue());
                    } else {
                        note_msg("weight not found");
                    }
					itemExist = true;
				}
			}

			searchloop:
            for (String dN: varDoubleMap.keySet()){
				if (debugDeviationFindMissing){
					if (dN.equalsIgnoreCase(missing)) {
                        note_msg(missing+": is found.");
                    }
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
						if (!wm1_ori.isEmpty()) {
                            wm1.putAll(wm1_ori);
                        }
						if (!wm2_ori.isEmpty()) {
                            wm2.putAll(wm2_ori);
                        }

						WeightElement nwe = new WeightElement();
						nwe.setValue(w * debugDeviationWeightMultiply);

						if (whichMap==1){
							wm1_ori.put(dN, wm1.get(dN));
							wm1.put(dN, nwe);
						} else if (whichMap==2){
							wm2_ori.put(dN, wm2.get(dN));
							wm2.put(dN, nwe);
						}

                        reloadProblem(false,"");
						jCbc.setPrimalTolerance(model, solve_whs_primalT);
						jCbc.setIntegerTolerance(model, integerT);
						jCbc.solve_whs(model,solver,names,values,intSolSize,0);
						int statusDev = jCbc.status(model);
						int status2Dev = jCbc.secondaryStatus(model);

						if (statusDev==0 && status2Dev ==0){
							//Double tObj = getObjValue();
							LinkedHashMap<String, Double> solution = collectDvar2021_simple();
							double newV = solution.get(dN);
							if (debugDeviationFindMissing && dN.equalsIgnoreCase(missing)){
								note_msg(missing+": test feasible, newV: "+newV);
							}
							if (Math.abs(newV-v)<2.0){
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
							reloadAndWriteLp("deviation_test_infeasible_(" + dN + ")",true);
						}
					}
				}
			}
		}

        long totalEndTime = System.currentTimeMillis();
        long totalDuration = totalEndTime - totalStartTime;
        ControlData.t_cbc = ControlData.t_cbc + (int) totalDuration;

        logger.atDebug().setMessage("==================== Problem Solving Session Complete ====================").log();
        logger.atDebug().setMessage("Total processing time: {} ms").addArgument(totalDuration).log();
    }

	public static double getObjValue(){
        double objValue = jCbc.getObjValue(model) * -1;
        logger.atTrace().setMessage("Retrieved objective value: {}").addArgument(objValue).log();
        return objValue;
	}

    private static void reloadProblem(boolean isNoteCbc, String append) {
        logger.atDebug().setMessage("Reloading problem: isNoteCbc={}, append={}").addArgument(isNoteCbc).addArgument(append).log();


        try {
            SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
            int sizeA = ControlData.currModelDataSet.dvList.size();
            int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();

            dvBiMap.clear();
            dvBiMapArray = new ArrayList<String>();

            for (int i = 0; i < sizeA; i++) {
                dvBiMap.put(i, ControlData.currModelDataSet.dvList.get(i));
                dvBiMapArray.add(i, ControlData.currModelDataSet.dvList.get(i));
            }
            for (int i = 0; i < sizeB; i++) {
                dvBiMap.put(i + sizeA, ControlData.currModelDataSet.dvTimeArrayList.get(i));
                dvBiMapArray.add(ControlData.currModelDataSet.dvTimeArrayList.get(i));
            }
            dvBiMapInverse = dvBiMap.inverse();

            logger.atDebug().setMessage("Variable mapping rebuilt: total variables={}").addArgument(dvBiMap.size()).log();

            jCbc.delete_jCbcModel(model);
            model = null;
            jCbc.delete_jCoinModel(modelObject);

            model = jCbc.new_jCbcModel();
            solver = jCbc.new_jOsiClpSolverInterface();
            jCbc.assignSolver(model, solver);
            modelObject = jCbc.new_jCoinModel();

            setDVars(isNoteCbc, append);
            setConstraints(isNoteCbc, append);
            jCbc.setModelName(solver, modelName);

            logger.atDebug().setMessage("Problem reloaded successfully").log();
        } catch (Exception e) {
            logger.atError().setMessage("Failed to reload CBC problem: isNoteCbc={}, append={}").addArgument(isNoteCbc).addArgument(append).setCause(e).log();
            throw new RuntimeException("Problem reload failed", e);
        }
    }

    private static void reloadProblemConfirm(String skipThisConstraint) {
        logger.atDebug().setMessage("Reloading problem for constraint confirmation: skipThisConstraint={}").addArgument(skipThisConstraint).log();

 		// restore original state
        try {
            SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
            int sizeA = ControlData.currModelDataSet.dvList.size();
            int sizeB = ControlData.currModelDataSet.dvTimeArrayList.size();

            dvBiMap.clear();
            for (int i = 0; i < sizeA; i++) {
                dvBiMap.put(i, ControlData.currModelDataSet.dvList.get(i));
            }
            for (int i = 0; i < sizeB; i++) {
                dvBiMap.put(i + sizeA, ControlData.currModelDataSet.dvTimeArrayList.get(i));
            }
            dvBiMapInverse = dvBiMap.inverse();

            jCbc.delete_jCbcModel(model);
            model = null;
            jCbc.delete_jCoinModel(modelObject);

            model = jCbc.new_jCbcModel();
            solver = jCbc.new_jOsiClpSolverInterface();
            jCbc.assignSolver(model, solver);
            modelObject = jCbc.new_jCoinModel();

            setDVarsIIS();
            setConstraintsSkip(skipThisConstraint);
            jCbc.setModelName(solver, modelName);

            logger.atDebug().setMessage("Confirmation problem loaded successfully").log();
        } catch (Exception e) {
            logger.atError().setMessage("Failed to reload CBC confirmation problem: skipThisConstraint={}").addArgument(skipThisConstraint).setCause(e).log();
            throw new RuntimeException("Confirmation problem reload failed", e);
        }
    }

    private static void loadProblemIIS(boolean isFirstTimeRun, Set<String> enforceThisConstraint) {
        logger.atDebug().setMessage("Loading IIS problem: isFirstTimeRun={}, enforceThisConstraint size={}").addArgument(isFirstTimeRun)
              .addArgument(enforceThisConstraint.size()).log();
        try {
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

            // new model
            model = jCbc.new_jCbcModel();
            solver = jCbc.new_jOsiClpSolverInterface();
            jCbc.assignSolver(model,solver);
            modelObject = jCbc.new_jCoinModel();

            setDVarsIIS();
            setConstraintsIIS(isFirstTimeRun, enforceThisConstraint);
            jCbc.setModelName(solver, modelName+"_iis");

            logger.atDebug().setMessage("IIS problem loaded successfully").log();
        } catch (Exception e) {
            logger.atError().setMessage("Failed to load CBC IIS problem: isFirstTimeRun={}").addArgument(isFirstTimeRun).setCause(e).log();
            throw new RuntimeException("IIS problem load failed", e);
        }
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

        logger.atDebug().setMessage("CBC model reset completed").log();
    }

    private static void getSolverInformation(int status, int status2) {

		// status
//		 0 if finished (which includes the case when the algorithm is finished because it has been proved infeasible),
//		 1 if stopped by user, and
//		 2 if difficulties arose.
        logger.atError().setMessage("CBC solve abnormal status: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

        switch (status) {
            case 0:
                Error.addSolvingError("Infeasible.");
                break;
            case 1:
                Error.addSolvingError("Stopped by user.");
                break;
            case 2:
                Error.addSolvingError("Other errors.");
                break;
            default:
                Error.addSolvingError("Status:" + status);
                break;
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

        switch (status2) {
            case 1:
                Error.addSolvingError("Linear relaxation not feasible.");
                break;
            case 2:
                Error.addSolvingError("Stopped on gap.");
                break;
            case 3:
                Error.addSolvingError("Stopped on nodes.");
                break;
            case 4:
                Error.addSolvingError("Stopped on time.");
                break;
            case 5:
                Error.addSolvingError("Stopped on user event.");
                break;
            case 6:
                Error.addSolvingError("Stopped on solutions.");
                break;
            case 7:
                Error.addSolvingError("Linear relaxation unbounded.");
                break;
            default:
                Error.addSolvingError("Status2:" + status2);
                break;
        }
    }

    private static void setConstraints(boolean isNoteCbc, String append) {
        PerformanceTimer timer = new PerformanceTimerCbc("Constraint Setup");

        logger.atDebug().setMessage("CBC Solver: Setting up constraints...").log();

        try {
		    Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		    String c="quicklog version 1.0\n";
		    int rowCounter=0; // row index
            int equalityCount = 0;
            int inequalityCount = 0;

            for (int i=0; i<=1; i++){
                ArrayList<String> constraintCollection;
                if (i==0){
                    constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
                    constraintCollection.retainAll(constraintMap.keySet());
                    logger.atDebug().setMessage("Processing base constraint set, size: {}").addArgument(constraintCollection.size()).log();
                }else{
                    constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
                    logger.atDebug().setMessage("Processing time series constraint set, size: {}").addArgument(constraintCollection.size()).log();
                }
                Iterator<String> constraintIterator = constraintCollection.iterator();


                while(constraintIterator.hasNext()){

                    double GT=-999;
                    double LT= 999;


                    String constraintName=(String)constraintIterator.next();
                    EvalConstraint ec=constraintMap.get(constraintName);
                    logger.atTrace().setMessage("Processing constraint: name={}, sign={}, RHS={}").addArgument(constraintName).addArgument(ec.getSign())
                        .addArgument(ec.getEvalExpression().getValue()
                        .getData().doubleValue()).log();

                    if (ec.getSign().equals("=")) {
                        GT = -ec.getEvalExpression().getValue().getData().doubleValue();
                        if(Math.abs(GT)<ControlData.zeroTolerance) {
                            GT=0;
                        } else if (Math.abs(GT)>maxValue) {
                            GT=maxValue*Math.signum(GT);
                        }
                        LT = GT;
                        equalityCount++;
                     } else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
                        GT = -maxValue;
                        LT = -ec.getEvalExpression().getValue().getData().doubleValue();
                        if(Math.abs(LT)<ControlData.zeroTolerance) {
                            LT=0;
                        } else if (Math.abs(LT)>maxValue) {
                            LT=maxValue*Math.signum(LT);
                        }
                        inequalityCount++;
                    } else if (ec.getSign().equals(">")){
                        GT = -ec.getEvalExpression().getValue().getData().doubleValue();
                        if(Math.abs(GT)<ControlData.zeroTolerance) {
                            GT=0;
                        } else if (Math.abs(GT)>maxValue) {
                            GT=maxValue*Math.signum(GT);
                        }
                        LT = maxValue;
                        inequalityCount++;
                    }
                    else {
                        // error!!
                        logger.atError().setMessage("Invalid constraint sign in CbcSolver: {}").addArgument(ec.getSign()).log();
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
                        if(Math.abs(temp)<ControlData.zeroTolerance) {temp=0;
                        }
                        elements[j]=temp;
    					j++;
    				}

				    jCbc.addRow(modelObject,multMap.keySet().size(), index, elements, GT, LT, constraintName);

                    if (isNoteCbc) {
                        c = c + constraintName + "," + GT + "," + LT + "," + multMap.keySet().size() + ","
                                + Arrays.toString(index) + "," + Arrays.toString(elements) + "\n";
                    }
                    rowCounter++;
                    // Report progress every 1000 constraints
                    if (rowCounter % 1000 == 0) {
                        logger.atDebug().setMessage("Processed {} constraints").addArgument(rowCounter).log();
                    }
                }
            }

            jCbc.addRows(solver,modelObject);
            if (isNoteCbc) Tools.quickLog(modelName + "_" + solveName + "_" + append + ".rows", c);
            long setupTime = timer.stop();
            performanceStats.recordConstraintSetupTime(setupTime);
            logger.atDebug().setMessage("CBC constraint setup complete: total={}, equality={}, inequality={}")
                    .addArgument(rowCounter).addArgument(equalityCount).addArgument(inequalityCount).log();
        } catch (Exception e) {
            logger.atError().setMessage("Failed to set up CBC constraints").setCause(e).log();
            throw new RuntimeException("Constraint setup failed", e);
        } finally {
            timer.stop();
        }
    }

	private static void setConstraintsSkip(String skipThisConstraint) {
        logger.atDebug().setMessage("Setting up constraints with skip: skipThisConstraint={}").addArgument(skipThisConstraint).log();

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        int rowCounter = 0;
        int skippedCount = 0;

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

                if (skipThisConstraint.equalsIgnoreCase(constraintName)) {
                    skippedCount++;
                    rowCounter++;
                    continue;
                }

				EvalConstraint ec = constraintMap.get(constraintName);

				if (ec.getSign().equals("=")) {
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {
                        GT=0;
                    }
					LT = GT;
				} else if (ec.getSign().equals("<") || ec.getSign().equals("<=")){
					GT = -maxValue;
					LT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(LT)<ControlData.zeroTolerance) {
                        LT=0;
                    }
				} else if (ec.getSign().equals(">")){
					GT = -ec.getEvalExpression().getValue().getData().doubleValue();
					if(Math.abs(GT)<ControlData.zeroTolerance) {
                        GT=0;
                    }
					LT = maxValue;
                } else {
					// error!!
                    logger.atError().setMessage("Invalid constraint sign in CbcSolver: {}").addArgument(ec.getSign()).log();
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
					if(Math.abs(temp)<ControlData.zeroTolerance) {
                        temp=0;
                    }
					elements[j] = temp;
					j++;
				}

                jCbc.addRow(modelObject, multMap.keySet().size(), index, elements, GT, LT, constraintName);
                rowCounter++;
			}
        }

        jCbc.addRows(solver, modelObject);
        logger.atDebug().setMessage("Skipped constraint setup complete: total {} constraints, skipped {} constraints").addArgument(rowCounter).addArgument(skippedCount).log();
    }

	private static void setConstraintsIIS(boolean firstTimeRun, Set<String> enforceThisConstraint) {
        logger.atDebug().setMessage("Setting up IIS constraints: firstTimeRun={}, enforceThisConstraint size={}").addArgument(firstTimeRun).addArgument(enforceThisConstraint.size()).log();


        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
		int total=0;
        int enforcedCount = 0;

		for (int i=0; i<=1; i++){
			ArrayList<String> constraintCollection;
			if (i==0){
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
				constraintCollection.retainAll(constraintMap.keySet());
			} else {
				constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gTimeArrayList);
			}
			Iterator<String> constraintIterator = constraintCollection.iterator();

			while(constraintIterator.hasNext()){
                total++;

				double GT=-999;
				double LT= 999;

				String constraintName=(String)constraintIterator.next();
				EvalConstraint ec=constraintMap.get(constraintName);

                if (ec.getSign().equals("=")) {
                    GT = -ec.getEvalExpression().getValue().getData().doubleValue();
                    if (Math.abs(GT) < gov.ca.water.wrims.engine.core.components.ControlData.zeroTolerance) {
                        GT = 0;
                    }
                    LT = GT;
                } else if (ec.getSign().equals("<") || ec.getSign().equals("<=")) {
                    GT = -maxValue;
                    LT = -ec.getEvalExpression().getValue().getData().doubleValue();
                    if (Math.abs(LT) < gov.ca.water.wrims.engine.core.components.ControlData.zeroTolerance) {
                        LT = 0;
                    }
                } else if (ec.getSign().equals(">")) {
                    GT = -ec.getEvalExpression().getValue().getData().doubleValue();
                    if (Math.abs(GT) < gov.ca.water.wrims.engine.core.components.ControlData.zeroTolerance) {
                        GT = 0;
                    }
                    LT = maxValue;
                } else {
                    logger.atError().setMessage("Unknown constraint sign in CbcSolver IIS").log();
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
					if(Math.abs(temp)<ControlData.zeroTolerance) {
                        temp=0;
                    }
					elements[j] = temp;
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
					if (_pstc.contains(constraintName)){
                        coef = 1;
                    }
				}

				if (enforceThisConstraint.contains(constraintName)){
                    coef = 0;
                    enforcedCount++;
                }

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

		jCbc.addRows(solver,modelObject);
        total_relaxed_constraints = total;

        logger.atDebug().setMessage("IIS constraint setup complete: total {} constraints, enforced {} constraints, relaxation coefficient={}").addArgument(total).addArgument(enforcedCount)
            .addArgument(cbcHintRelaxPenalty).log();
	}

	private static void setDVars(boolean isNoteCbc, String append) {
        PerformanceTimer timer = new PerformanceTimerCbc("Variable Setup");

        logger.atDebug().setMessage("CBC Solver: Setting up decision variables...").log();

        try {
            Map<String, WeightElement> wm1 = SolverData.getWeightMap();
            String c = "quicklog version 1.0\n";
            int intVarCount = 0;
            int contVarCount = 0;
            double totalWeight = 0;

            logger.atDebug().setMessage("Setting up {} variables").addArgument(dvBiMap.size()).log();

            for (int i = 0; i < dvBiMap.size(); i++) {
                String dvName = dvBiMapArray.get(i);
                Dvar dvObj = dvarMap.get(dvName);

                double w = 0;
                if (wm1.keySet().contains(dvName)) {
                    w = -wm1.get(dvName).getValue();
                    totalWeight += Math.abs(w);
                }

                boolean isInteger = dvObj.integer.equalsIgnoreCase(Param.yes);
                if (isInteger) {
                    intVarCount++;
                    logger.atTrace().setMessage("Integer variable: name={}, lowerBound={}, upperBound={}, weight={}")
                        .addArgument(dvName).addArgument(dvObj.lowerBoundValue.doubleValue()).addArgument(dvObj.upperBoundValue.doubleValue())
                        .addArgument(w).log();
                } else {
                    contVarCount++;
                }

                jCbc.addCol(modelObject, dvObj.lowerBoundValue.doubleValue(),
                        dvObj.upperBoundValue.doubleValue(), w, dvName,
                        isInteger);

                if (isNoteCbc) {
                    int isInt = 0;
                    if (isInteger) isInt = 1;
                    c = c + isInt + "," + dvName + "," + w + "," + dvObj.lowerBoundValue.doubleValue() + ","
                            + dvObj.upperBoundValue.doubleValue() + "\n";
                }
            }

            if (isNoteCbc) Tools.quickLog(modelName + "_" + solveName + "_" + append + ".cols", c);

            long setupTime = timer.stop();
            performanceStats.recordVariableSetupTime(setupTime);
            logger.atDebug().setMessage("CBC variable setup complete: continuous={}, integer={}")
                    .addArgument(contVarCount).addArgument(intVarCount).log();
        } catch (Exception e) {
            logger.atError().setMessage("Failed to set up CBC decision variables").setCause(e).log();
            throw new RuntimeException("Variable setup failed", e);
        } finally {
            timer.stop();
        }
    }


    private static void setDVars2021(boolean isNoteCbc) {
        PerformanceTimer timer = new PerformanceTimerCbc("Variable Setup (2021 version)");

        logger.atDebug().setMessage("CBC Solver: Setting up decision variables (2021 version)...").log();

        try {
            int intSize = 0;
            Map<String, WeightElement> wm1 = SolverData.getWeightMap();
            String c = "quicklog version 1.0\n";
            double totalWeight = 0;

            for (int i = 0; i < dvBiMap.size(); i++) {
                String dvName = dvBiMapArray.get(i);
                Dvar dvObj = dvarMap.get(dvName);

                double w = 0;
                if (wm1.keySet().contains(dvName)) {
                    w = -wm1.get(dvName).getValue();
                    totalWeight += Math.abs(w);
                }
                double lb = 9;
                double ub = -9;
                boolean isInteger = (dvObj.integer.equalsIgnoreCase(Param.yes));
                if (isInteger) {
                    intSize = intSize + 1;
                }
                if (isInteger && dvIntMap2021.containsKey(dvName)) {
                    dvIntPredict.add(dvName);
                    lb = dvIntMap2021.get(dvName);
                    ub = lb;
                    logger.atTrace().setMessage("Integer variable warm start: name={}, fixed value={}").addArgument(dvName).addArgument(lb).log();
                } else {
                    lb = dvObj.lowerBoundValue.doubleValue();
                    ub = dvObj.upperBoundValue.doubleValue();
                }

                jCbc.addCol(modelObject, lb, ub, w, dvName, isInteger);
                if (isNoteCbc) {
                    int isInt = 0;
                    if (isInteger) isInt = 1;
                    c = c + isInt + "," + dvName + "," + w + "," + lb + "," + ub + "\n";
                }
            }

            if (isNoteCbc) Tools.quickLog(modelName + "_" + solveName + ".cols", c);
            intVarSize = intSize;

            logger.atDebug().setMessage("2021 version variable setup complete: integer variables={}, total weight={}").addArgument(intSize).addArgument(totalWeight).log();
        } catch (Exception e) {
            logger.error("Failed to set up 2021 variables", e);
            throw new RuntimeException("2021 variable setup failed", e);
        } finally {
            timer.report();
        }
    }

    private static void setDVarsIIS() {
        logger.atDebug().setMessage("Setting up IIS variables").log();

        int intVarCount = 0;
        int contVarCount = 0;

        for (int i=0; i<dvBiMap.size(); i++){
			String dvName = dvBiMap.get(i);
			Dvar dvObj = dvarMap.get(dvName);

		    jCbc.addCol(modelObject , dvObj.lowerBoundValue.doubleValue(),
                    dvObj.upperBoundValue.doubleValue(), 0, dvName,
                    dvObj.integer.equalsIgnoreCase(Param.yes));

            if (dvObj.integer.equalsIgnoreCase(Param.yes)) {
                intVarCount++;
            } else {
                contVarCount++;
            }
		}
        logger.atDebug().setMessage("IIS variable setup complete: continuous variables={}, integer variables={}").addArgument(contVarCount).addArgument(intVarCount).log();
    }

	public static int[] solve(){
    	logger.atDebug().setMessage("==================== Solving Start ====================").log();
        int ci = ControlData.currCycleIndex + 1;

        logger.atDebug().setMessage("CBC Standard Solve: modelName={}, solveFunction={}, date={}-{}-{}, cycle={} [{}]").addArgument(modelName).addArgument(solvFunc).addArgument(ControlData.currYear).addArgument(ControlData.currMonth)
            .addArgument(ControlData.currDay).addArgument(ci)
            .addArgument(ControlData.currCycleName).log();

        logger.atDebug().setMessage("CBC Solver: Solving {}/{}/{} Cycle {} [{}]")
            .addArgument(ControlData.currMonth).addArgument(ControlData.currDay)
            .addArgument(ControlData.currYear).addArgument(ci).addArgument(ControlData.currCycleName).log();

            int status = -99;
            int status2 = -99;

            PerformanceTimer solveTimer = new PerformanceTimer("Solver Execution");

            if (solvFunc == solvU){
                logger.atDebug().setMessage("Using solveU solving method").log();
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
                    logger.atDebug().setMessage("Setting warm start: {} integer variables").addArgument(intSolSize).log();

                    ret = jCbc.solve_unified(model, solver, names, values, intSolSize, 0);

                } else {
                    logger.atDebug().setMessage("Not using warm start").log();
                    ret = jCbc.solve_unified(model, solver, null, null, 0, 0);
                }

                solveName=solve_u_ret.get(ret);
                status = jCbc.status(model);
                status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("solveU completed: returnCode={}, solveName={}, status={}, secondaryStatus={}").addArgument(ret).addArgument(solveName).addArgument(status).addArgument(status2).log();


            } else if (solvFunc == solv2) {
                logger.atDebug().setMessage("Using solve2 solving method").log();
                solveName="2__";
                jCbc.setPrimalTolerance(model, solve_2_primalT);
                jCbc.setIntegerTolerance(model, integerT);
                jCbc.solve_2(model, solver, 0);
                status = jCbc.status(model);
                status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("solve2 completed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

            } else if (solvFunc == solv3) {
                logger.atDebug().setMessage("Using solve3 solving method").log();
                solveName="3__";
                jCbc.setPrimalTolerance(model, solve_3_primalT);
                jCbc.setIntegerTolerance(model, integerT);
                jCbc.solve_3(model, solver, 0);
                status = jCbc.status(model);
                status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("solve3 completed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

            } else if (solvFunc == solvCallCbc) {
                logger.atDebug().setMessage("Using callCbc solving method").log();
                solveName="cal";
                jCbc.setIntegerTolerance(model, integerT);
                jCbc.callCbc("-log 0 -solve", model);
                status = jCbc.status(model);
                status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("callCbc completed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

                } else if (solvFunc == solvFull) {
                    logger.atDebug().setMessage("Using solveFull solving method").log();

                    Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
                    a.retainAll(dvIntMap.keySet());
                    if ((useWarm || saveWarm) && a.size()>2) {
                        logger.atDebug().setMessage("Using warm start solving, integer variable count: {}").addArgument(a.size()).log();

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
                logger.atDebug().setMessage("Starting warm start solve (whs)").log();
                jCbc.solve_whs(model,solver,names,values,intSolSize,0);

				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("Warm start solve completed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

                if (status != 0 || status2 != 0) {
                    logger.atWarn().setMessage("CBC warm start solve failed; retrying with fallback solve method").log();
                    reloadProblem(true, "");
                    if (warm_2nd_solvFunc == solv2) {
                        solve_2();
                    } else if (warm_2nd_solvFunc == solv3) {
                        solve_3();
                    } else {
                        logger.atError().setMessage("Invalid CBC warm-start fallback solve function").log();
                    }
                }

			} else {
                logger.atDebug().setMessage("Not using warm start, using backup solving method").log();
                if (warm_2nd_solvFunc == solv2) {
					solve_2();
				} else if (warm_2nd_solvFunc == solv3) {
					solve_3();
				} else {
                    logger.atError().setMessage("Invalid CBC fallback solve function").log();
                }
            }

			status = jCbc.status(model);
			status2 = jCbc.secondaryStatus(model);

			if (status != 0 || status2 != 0) {
                logger.atWarn().setMessage("CBC solve failed; retrying with relaxed primal tolerance {}").addArgument(solve_2_primalT_relax).log();
                note_msg(" Solve_"+solveName+" infeasible. Use solve_2 with primalT="+solve_2_primalT_relax);
				reloadProblem(false, "");
				solve_2(solve_2_primalT_relax, "2R_");
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
			}

			////// check for violation and re-solve with v2.10a
			int Err_int = 0;
            int Err_lb = 0;
            if (status == 0 && status2 == 0) {
                logger.atDebug().setMessage("Checking solve result for violations").log();
                int ColumnSize = jCbc.getNumCols(model);
					SWIGTYPE_p_double v_ary = jCbc.getColSolution(solver);
					Map<String, Dvar> dMap = SolverData.getDvarMap();

					for (int j = 0; j < ColumnSize; j++){
						 //varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(solver),j));
						double v = jCbc.jarray_double_getitem(v_ary,j);
						String k = jCbc.getColName(model,j);

						if (jCbc.isInteger(solver,j) == 1) {
							if (Math.abs(v-Math.round(v))>integerT_check){
								Err_int = 1;
								ILP.writeNoteLn(modelName + ":" + " Solve_" + solveName + ":intViolation:::" + k + ":" + v,
                                        true, false);
							}
						}
						else {
							Dvar d = dMap.get(k);
							if (d.lowerBoundValue.doubleValue() == 0 &&  v<-lowerBoundZero_check) {
								Err_lb = 1;
								ILP.writeNoteLn(modelName + ":" + " Solve_" + solveName + ":lbViolation:::" + k + ":" + v,
                                        true, false);
							}
						}
					}
				}

				if (Err_int>0 || Err_lb>0){
                    logger.atWarn().setMessage("CBC solve returned tolerance violations; writing LP and retrying with callCbc").log();
                    reloadAndWriteLp("_lbViolation", true);
					if (cbcViolationRetry) {
						note_msg(" Solve_" + solveName + " has violations. Use callCbc");
						reloadProblem(false, "");
						callCbc();
						status = jCbc.status(model);
						status2 = jCbc.secondaryStatus(model);
					}
				}
			}

            solveTimer.stop();


			if (status != 0 || status2 != 0) {
                logger.atError().setMessage("CBC solve failed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();
				reloadAndWriteLp("_infeasible", true, true);
				getSolverInformation(status, status2);
				iis();
			}else {
                logger.atDebug().setMessage("Solve completed successfully").log();
            }
            logger.atDebug().setMessage("==================== Solving End ====================").log();
            return new int[]{status, status2};
		}


    public static int[] solve_jCbc2021() {
            logger.atDebug().setMessage("==================== 2021 Solver Start ====================").log();
            int ci = ControlData.currCycleIndex + 1;

            logger.atDebug().setMessage("CBC 2021 Solver: modelName={}, solveFunction={}, date={}-{}-{}, cycle={} [{}]")
                    .addArgument(modelName).addArgument(solvFunc).addArgument(ControlData.currYear).addArgument(ControlData.currMonth)
                    .addArgument(ControlData.currDay).addArgument(ci).addArgument(ControlData.currCycleName).log();

            logger.atDebug().setMessage("CBC Solver2021: Solving {}/{}/{} Cycle {} [{}]")
                    .addArgument(ControlData.currMonth).addArgument(ControlData.currDay)
                    .addArgument(ControlData.currYear).addArgument(ci).addArgument(ControlData.currCycleName).log();

            int status=-99;
            int status2=-99;

            PerformanceTimer solveTimer = new PerformanceTimer("2021 Solver Execution");

			Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
			a.retainAll(dvIntMap.keySet());

            logger.atDebug().setMessage("Available warm start integer variables: {}").addArgument(a.size()).log();

            if ((useWarm || saveWarm) && a.size()>2) {
                logger.atDebug().setMessage("Using warm start solving").log();

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
				for (String dvN: ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)){
					jCbc.jarray_string_setitem(names,k,dvN);
					jCbc.jarray_int_setitem(values,k,dvIntMap.get(dvN));
					k++;
				}

				solveName="whs";
				jCbc.setPrimalTolerance(model, solve_whs_primalT);
				jCbc.setIntegerTolerance(model, integerT);
                logger.atDebug().setMessage("Starting warm start solve").log();
                jCbc.solve_whs(model,solver,names,values,intSolSize,0);
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
                logger.atDebug().setMessage("Warm start solve completed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();

                if (status != 0 || status2 != 0) {
                    logger.atWarn().setMessage("CBC warm start solve failed; retrying with solve_2").log();
                    if (isLogging) {
                        note_msg(" Solve_"+solveName+" infeasible.");
                    }
					reloadProblem(false, "");
					solve_2();
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);
                } else if (violationCheck(model, modelName, solveName)) {
                    logger.atWarn().setMessage("CBC warm start solve returned violations; retrying with solve_2").log();
                    if (isLogging) {
                        note_msg(" Solve_"+solveName+" tolerance violation.");
                    }
					reloadProblem(true, "");
					solve_2();
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);
				}

			} else {
                    logger.atDebug().setMessage("Not using warm start, using solve2 directly").log();
                    solve_2();
					status = jCbc.status(model);
					status2 = jCbc.secondaryStatus(model);
			}

            if (status != 0 || status2 != 0) {
                logger.atWarn().setMessage("CBC solve_2 failed; retrying with relaxed primal tolerance {}").addArgument(solve_2_primalT_relax).log();
                note_msg(" Solve_" + solveName + " infeasible. Use solve_2 with primalT=" + solve_2_primalT_relax);
				reloadProblem(false, "");
				solve_2(solve_2_primalT_relax, "2R_");
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
			}// don't use solve2R if solve2 has violation

            if (status != 0 || status2 != 0) {
                logger.atWarn().setMessage("CBC retry failed; retrying with callCbc").log();
                note_msg(jCbc.getModelName(solver) +" Solve_" + solveName
                        + " infeasible. Use callCbc -primalT 1e-9 -integerT 1e-9 ");
				reloadProblem(false, "");
				callCbc();
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
            } else if (violationCheck(model, modelName, solveName)) {
                logger.atWarn().setMessage("CBC solve returned violations; retrying with callCbc").log();
                reloadProblem(true, "");
				callCbc();
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
			}

			if (status != 0 || status2 != 0 ) {
                logger.atWarn().setMessage("CBC callCbc failed; retrying with relaxed callCbc").log();
                note_msg(jCbc.getModelName(solver) + " Solve_"+solveName
                        + " infeasible. Use callCbc -primalT 1e-7 -integerT 1e-9 ");
				reloadProblem(false, "");
				callCbc_R();
				status = jCbc.status(model);
				status2 = jCbc.secondaryStatus(model);
				if (status==0 && status2==0){
					status2 = jCbc.Y2_simple(model,solver);
				}
			} // Don't use callCbcR if callCbc has violation

            solveTimer.stop();

            if (status != 0 || status2 != 0) {
                logger.atError().setMessage("CBC solve failed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();
				reloadAndWriteLp("_infeasible", true, true);
				getSolverInformation(status, status2);
				iis();
			} else {
                logger.atDebug().setMessage("2021 Solver completed successfully").log();
            }

            logger.atDebug().setMessage("==================== 2021 Solver End ====================").log();
            return new int[]{status, status2};
        }

    private static void iis() {
        logger.atInfo().setMessage("==================== IIS Analysis Start ====================").log();
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
            logger.atInfo().setMessage("Priority search constraint set found, size: {}").addArgument(prioritizeSearchTheseConstraints.size()).log();
        }

		boolean isFirstTimeRun = true;
		boolean success = true;
		long ts = Calendar.getInstance().getTimeInMillis();
		int tr=0;
		boolean isConflictFound = false;

		while (success) {
			tr =(int) (Calendar.getInstance().getTimeInMillis()-ts)/1000;
			if (tr>CbcSolver.cbcHintTimeMax) {
                logger.atWarn().setMessage("CBC IIS analysis stopped due to time limit: {} sec").addArgument(CbcSolver.cbcHintTimeMax).log();
                ILP.writeNoteLn("\r\nInfeasibility analysis stopped due to time limit exceeded.", true, true);
				break;
            }

			iisPossibleConstraintMap_cumulative.putAll(iisPossibleConstraintMap);

			if (hasPriorityConstraints) {
				_pstc.removeAll(iisConfirmConstraint);
				if (_pstc.size()<1) {hasPriorityConstraints = false;
                    logger.atInfo().setMessage("All priority constraints have been confirmed").log();
                }
            }
			iisPossibleConstraintMap.clear();
			iisConfirmConstraint.clear();

            logger.atDebug().setMessage("Starting IIS solve, attempt {}").addArgument(pp + 1).log();
            success = iisSolve(isFirstTimeRun, iisPossibleConstraintMap_cumulative.keySet());
			//isFirstTimeRun=false;

			if (hasPriorityConstraints) {
				if (!success || iisPossibleConstraintMap.size()<1){
                    logger.atInfo().setMessage("Priority search ended").log();
                    ILP.writeNoteLn("End priority search.",true,false);
					hasPriorityConstraints = false;
					_pstc = null;
					success = true;
					continue;
				}
			}

			if (!success || iisPossibleConstraintMap.size()<1) {
                logger.atInfo().setMessage("IIS analysis ended").log();
                ILP.writeNoteLn("Infeasibility analysis ended.", true, false);
				if (iisPossibleConstraintMap_cumulative.size()>0) {
					if (!isConflictFound) {
                        logger.atWarn().setMessage("The following constraints might cause infeasibility").log();
                        ILP.writeNoteLn("The following constraints might cause infeasibility.", true, false);
						for (String c: iisPossibleConstraintMap_cumulative.keySet()){
							ILP.writeNoteLn(Tools.findGoalLocation(c)+" "+iisPossibleConstraintMap_cumulative.get(c),true,true);
						}
					}
				}
				logger.atInfo().setMessage("==================== IIS Analysis End ====================").log();
                return;
            } else {
                logger.atInfo().setMessage("Finding constraints that cause infeasibility...").log();
                ILP.writeNoteLn("Finding constraints that cause infeasibility...", true, false);
            }

			for (String c : iisPossibleConstraintMap.keySet()){
                logger.atDebug().setMessage("Confirming constraint: {}").addArgument(c).log();
                if(iisSolveConfirm(c)){
					isConflictFound = true;
					iisConfirmConstraint.add(c); //pp++; ILP.writeNoteLn("found:"+pp,true,true);
                    logger.atInfo().setMessage("Confirmed infeasible constraint: {}").addArgument(c).log();
                }
			}

			if (iisConfirmConstraint.size()>0) {
                logger.atWarn().setMessage("Found {} infeasible constraints").addArgument(iisConfirmConstraint.size()).log();
                for (String c : iisConfirmConstraint){
						ILP.writeNoteLn(Tools.findGoalLocation(c)+" "+iisPossibleConstraintMap.get(c),true,true);
				}
			}
            isFirstTimeRun = false;
            pp++;
        }
        logger.atInfo().setMessage("==================== IIS Analysis End ====================").log();
    }

	private static boolean iisSolveConfirm(String skipThisConstraint) {
        logger.atDebug().setMessage("IIS confirmation solve, skipping constraint: {}").addArgument(skipThisConstraint).log();
        boolean success = false;

		reloadProblemConfirm(skipThisConstraint);
		if (CbcSolver.usejCbc2021a) {
			jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
		} else {
			callCbc();
		}
		int s = jCbc.status(model);
		int s2 = jCbc.secondaryStatus(model);

		if (s == 0 && s2 == 0) {
			success = true;
            logger.atDebug().setMessage("IIS confirmation solve successful").log();
        } else {
			success = false;
            logger.atDebug().setMessage("IIS confirmation solve failed: status={}, secondaryStatus={}").addArgument(s).addArgument(s2).log();
        }

		return success;
	}

	private static boolean iisSolve(boolean isFirstTimeRun, Set<String> enforceThisConstraint) {
        logger.atDebug().setMessage("IIS solve: isFirstTimeRun={}, enforceThisConstraint size={}")
                .addArgument(isFirstTimeRun).addArgument(enforceThisConstraint.size()).log();
        boolean success = false;

		loadProblemIIS(isFirstTimeRun, enforceThisConstraint);

		if (CbcSolver.usejCbc2021a) {
			jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
		} else {
			callCbc();
		}
		int s = jCbc.status(model);
		int s2 = jCbc.secondaryStatus(model);

		if (s == 0 && s2 == 0) {
            logger.atDebug().setMessage("IIS solve successful, checking slack variables").log();
            for (int j : iisSlackMap.keySet()) {
				String name = iisSlackMap.get(j);
				String name2 = jCbc.getColName(model, j);
				// check if name matches
				if (!name2.substring(0, name2.length() - 2).equalsIgnoreCase(name)) {
                    logger.atWarn().setMessage("Slack variable name mismatch: {} vs {}").addArgument(jCbc.getColName(model, j)).addArgument(name).log();
				} else {
					// check if solution not zero
					if (jCbc.jarray_double_getitem(jCbc.getColSolution(solver), j) > 0) {
                        logger.atDebug().setMessage("Found slack constraint: {}").addArgument(name).log();
						int[] index = iisConstraintIndexMap.get(name);
						double[] coeff = iisConstraintElementMap.get(name);
						String show = name + ": ";
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
            logger.atDebug().setMessage("IIS solve failed: status={}, secondaryStatus={}").addArgument(s).addArgument(s2).log();
            success = false;
		}

		return success;
	}

	private static void collectDvar() {
        PerformanceTimer timer = new PerformanceTimerCbc("Variable Collection");
        logger.atInfo().setMessage("CBC Solver: Collecting variable results...").log();

        int ColumnSize = jCbc.getNumCols(model);
		varDoubleMap = new LinkedHashMap<String, Double>();
		dvIntMap = new LinkedHashMap<String, Integer>();

        logger.atDebug().setMessage("Collecting {} variables").addArgument(ColumnSize).log();

        if (saveWarm||useWarm||saveIntVars) {
            logger.atDebug().setMessage("Saving warm start solution or integer variables").log();
			for (int j = 0; j < ColumnSize; j++) {
                String colName = jCbc.getColName(model, j);
                double value = jCbc.jarray_double_getitem(jCbc.getColSolution(solver), j);
                varDoubleMap.put(colName, value);

				 if (jCbc.isInteger(solver,j)==1) {
                     int intValue = (int) Math.round(value);
                     dvIntMap.put(colName, intValue);
                     logger.atTrace().setMessage("Integer variable: name={}, value={} (rounded: {})").addArgument(colName).addArgument(value).addArgument(intValue).log();
                 }
			}
		} else {
			for (int j = 0; j < ColumnSize; j++){
                String colName = jCbc.getColName(model, j);
                double value = jCbc.jarray_double_getitem(jCbc.getColSolution(solver), j);
                varDoubleMap.put(colName, value);
			}
		}
        timer.stop();
        logger.atInfo().setMessage("Variable collection complete: total {} variables").addArgument(varDoubleMap.size()).log();
    }

	private static void collectDvar2021() {
        PerformanceTimer timer = new PerformanceTimerCbc("Variable Collection (2021 version)");

        logger.atInfo().setMessage("CBC Solver: Collecting variable results (2021 version)...").log();

		int ColumnSize = jCbc.getNumCols(model);
		varDoubleMap = new LinkedHashMap<String, Double>();
		dvIntMap = new LinkedHashMap<String, Integer>();

        logger.atDebug().setMessage("Collecting {} variables (2021 version)").addArgument(ColumnSize).log();

        //int k = 0;
        for (int j = 0; j < ColumnSize; j++){
            String colName = jCbc.getColName(model, j);
            double value = jCbc.jarray_double_getitem(jCbc.getColSolution(model), j);
            varDoubleMap.put(colName, value);
            varDoubleMap.put(jCbc.getColName(model,j), jCbc.jarray_double_getitem(jCbc.getColSolution(model),j));
            if (jCbc.isInteger(solver, j) == 1) {
                int intValue = (int) Math.round(value);
                dvIntMap.put(colName, intValue);
logger.atTrace().setMessage("Integer variable (2021): name={}, value={} (rounded: {})").addArgument(colName).addArgument(value).addArgument(intValue).log();
            }
        }
        dvIntMap2021.putAll(dvIntMap);

        timer.stop();
        logger.atInfo().setMessage("2021 version variable collection complete: total {} variables, {} integer variables")
                .addArgument(varDoubleMap.size()).addArgument(dvIntMap.size()).log();
    }

	private static LinkedHashMap<String, Double> collectDvar2021_simple() {
        logger.atDebug().setMessage("Simple variable collection (2021 version)").log();

		int ColumnSize = jCbc.getNumCols(model);
		LinkedHashMap<String, Double> vMap = new LinkedHashMap<String, Double>();

		for (int j = 0; j < ColumnSize; j++){
			 vMap.put(jCbc.getColName(model,j),
                     jCbc.jarray_double_getitem(jCbc.getColSolution(model),j));
		}
        logger.atDebug().setMessage("Simple collection complete: {} variables").addArgument(vMap.size()).log();
        return vMap;

	}

	private static void assignDvar() {
        PerformanceTimer timer = new PerformanceTimerCbc("Variable Assignment");

        logger.atInfo().setMessage("CBC Solver: Assigning variable values...").log();

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

        logger.atDebug().setMessage("Processing {} extra variables").addArgument(extraDv.size()).log();

        for (String dvName: extraDv) {
			Dvar dvar = dvarMap.get(dvName);
			double value=0;
			if (dvar.lowerBoundValue.doubleValue()>0) {
                value = dvar.lowerBoundValue.doubleValue();
                logger.atTrace().setMessage("Extra variable using lower bound: name={}, value={}").addArgument(dvName).addArgument(value).log();
            }
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
        int assignedCount = 0;
        //TODO: weird bug. need to fix
		for (String dvName: varDoubleMap.keySet()) {
			Dvar dvar=dvarMap.get(dvName);
			double value=varDoubleMap.get(dvName);
			IntDouble id=new IntDouble(value,false);

			//TODO: weird bug. need to fix
			try {
				dvar.setData(id);
                assignedCount++;
            } catch (Exception e) {
                logger.atWarn().setMessage("CBC assignDvar fallback: creating new variable for {}").addArgument(dvName).setCause(e).log();
				dvar=new Dvar();
				dvar.upperBoundValue = maxValue;
				dvar.lowerBoundValue = 0.0;
				dvar.setData(id);
				dvarMap.put(dvName, dvar);
                assignedCount++;
            }

			if(dvarUsedByLaterCycle.contains(dvName)){
				varCycleValueMap.get(dvName).put(modelName, id);
			}else if (dvarTimeArrayUsedByLaterCycle.contains(dvName)){
				if (varTimeArrayCycleValueMap.containsKey(dvName)){
					varTimeArrayCycleValueMap.get(dvName).put(modelName, dvar.data);
				} else {
					Map<String, IntDouble> cycleValue = new HashMap<String, IntDouble>();
					cycleValue.put(modelName, dvar.data);
					varTimeArrayCycleValueMap.put(dvName, cycleValue);
				}
			}
			if (varCycleIndexList.contains(dvName) || dvarTimeArrayCycleIndexList.contains(dvName)){
				if (varCycleIndexValueMap.containsKey(dvName)){
					varCycleIndexValueMap.get(dvName).put(modelName, dvar.data);
				} else {
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

        if (assignedCount % 1000 == 0) {
            logger.atDebug().setMessage("Assigned {} variables").addArgument(assignedCount).log();
        }

	    timer.stop();
        logger.atInfo().setMessage("Variable assignment complete: total {} variables assigned").addArgument(assignedCount).log();

        logger.atInfo().setMessage("Objective Value: {}").addArgument(ControlData.clp_cbc_objective).log();
        logger.atInfo().setMessage("Variable assignment completed.").log();
}
	public static void addConditionalSlackSurplusToDvarMap(Map<String, Dvar> dvarMap, String dvName, boolean isNoteCbc, String append){

        logger.atTrace().setMessage("Adding slack/surplus variable to variable map: name={}").addArgument(dvName).log();

        String c="";
		Dvar dvar=new Dvar();
		dvar.upperBoundValue = maxValue;
		dvar.lowerBoundValue = 0.0;
		dvarMap.put(dvName, dvar);

		double w = 0;
		if (wm2.keySet().contains(dvName)){
			w = -wm2.get(dvName).getValue();
            logger.atTrace().setMessage("Slack/surplus variable weight: name={}, weight={}").addArgument(dvName).addArgument(w).log();
        }
		jCbc.addCol(modelObject , 0, maxValue, w, dvName, dvar.integer==Param.yes );
	    if (isNoteCbc) {
	    	int isInt = 0;
	    	if (dvar.integer == Param.yes) isInt = 1;
	    	c = isInt + "," + dvName + "," + w + ",0,"+ maxValue;
	    	Tools.quickLog(modelName + "_" + solveName + "_" + append + ".cols", c, true);
	    }
	}

	public static void reloadAndWriteLp(String nameAppend, boolean logMps, boolean logCbc) {
        logger.atDebug().setMessage("Reloading and writing LP file: nameAppend={}, logMps={}, logCbc={}")
                .addArgument(nameAppend).addArgument(logMps).addArgument(logCbc).log();

		String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		reloadProblem(logCbc, nameAppend);
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);
		if(logMps){
            jCbc.writeMps1(model, oPath+".mps", 1, 2);
        }

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

        logger.atInfo().setMessage("LP file written successfully: {}").addArgument(oPath).log();
    }

	public static void reloadAndWriteLp(String nameAppend, boolean logCbc) {
        logger.atDebug().setMessage("Reloading and writing LP file: nameAppend={}, logCbc={}").addArgument(nameAppend).addArgument(logCbc).log();

        String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		reloadProblem(logCbc, nameAppend);
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);

		logIntVars(label);

        logger.atDebug().setMessage("LP file written: {}").addArgument(oPath).log();

	}

	public static void writeCbcLp(String nameAppend, boolean logMps) {
        logger.atDebug().setMessage("Writing CBC LP file: nameAppend={}, logMps={}").addArgument(nameAppend).addArgument(logMps).log();

        String label = modelName + "_" + solveName + "_" + nameAppend;
		String oPath = new File(ILP.getIlpDir().getAbsoluteFile(), label).getAbsolutePath();
		//reloadProblem();
		jCbc.writeLp1(model, oPath, cbcWriteLpEpsilon, 14);
		//jCbc.writeMps(model, oPath);
		if(logMps){
            jCbc.writeMps1(model, oPath+".mps", 1, 2);
        }

		logIntVars(label);

        logger.atDebug().setMessage("CBC LP file written: {}").addArgument(oPath).log();
    }

	private static void logIntVars(String label){
        logger.atDebug().setMessage("Logging integer variables: label={}").addArgument(label).log();

        if (ControlData.currStudyDataSet.cycIntDvMap != null) {
			if (ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size() > 0) {
				String c = "";
				for (String dvN : ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex)) {
					c = c + dvN + "," + dvIntMap.get(dvN) + "\n";
				}
				Tools.quickLog(label + ".intVars", c);
                logger.atDebug().setMessage("Integer variables logged: {} variables")
                       .addArgument(ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex).size()).log();
            }
		}
	}

	public static void logCbcWatchList(StudyDataSet sds){
        logger.atDebug().setMessage("Logging watch list").log();

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
					wa_cbc = CbcSolver.varDoubleMap.get(s);
					wa_cbc_str += String.format("%14.3f", wa_cbc) + "  ";
				}
			}
			ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_cbc_str, ILP._watchFile_cbc);
            logger.atTrace().setMessage("Watch list logged").log();
        }
	}

	public static void logCbcDebug(StudyDataSet sds){

        logger.atDebug().setMessage("Logging debug information").log();

		ILP.findDvarEffective();
		ILP.setDvarFile("cbc");
		ILP.writeDvarValue_Clp0_Cbc0(CbcSolver.varDoubleMap);
		ILP.setDvarFile("xa");
		ILP.writeDvarValue_XA();

		boolean recordLP = false;
		boolean recordVar = false;
		double wa_cbc = 0;
		double wa_xa  = 0;
		if (ControlData.watchList != null) {
			String wa_cbc_str = "";
			String wa_xa_str = "";
			for (String s : ControlData.watchList) {
				if (ControlData.currModelDataSet.dvList.contains(s)){
					wa_cbc = CbcSolver.varDoubleMap.get(s);
					wa_xa  = ControlData.xasolver.getColumnActivity(s);
					wa_cbc_str += String.format("%14.3f", wa_cbc) + "  ";
					wa_xa_str += String.format("%14.3f",  wa_xa) + "  ";

                    if (Math.abs(wa_xa - wa_cbc) > gov.ca.water.wrims.engine.core.components.ControlData.watchList_tolerance) {
                        recordLP = true;
                        logger.atWarn().setMessage("CBC/XA watch variable difference exceeds tolerance: {} (cbc={}, xa={})")
                                .addArgument(s).addArgument(wa_cbc).addArgument(wa_xa).log();
                    }
                }
            }
            ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_cbc_str, ILP._watchFile_cbc);
            ILP.writeNoteLn(ILP.getYearMonthCycle(), wa_xa_str, ILP._watchFile_xa);
            logger.atTrace().setMessage("Watch variable comparison completed").log();
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
                logger.atWarn().setMessage("Watch variable difference too large, writing LP file").log();
                CbcSolver.reloadAndWriteLp("_watch_diff", true, true);
			}

			double diff = ControlData.clp_cbc_objective - ControlData.xasolver.getObjective();
			if (Math.abs(diff) > CbcSolver.record_if_obj_diff) {
                logger.atWarn().setMessage("CBC/XA objective difference too large: {}").addArgument(diff).log();
                CbcSolver.reloadAndWriteLp("_obj" + Math.round(diff), true, true);
            }
			if (Math.abs(diff) > CbcSolver.log_if_obj_diff) {
				xa_int += "  obj: " + String.format("%16.3f", diff);
				cbc_int += "  obj: " + String.format("%16.3f", diff);
				if (diff>CbcSolver.maxObjDiff){
					CbcSolver.maxObjDiff = diff;
					CbcSolver.maxObjDiff_id = ILP.getYearMonthCycle();
                    logger.atWarn().setMessage("New maximum objective difference: {} at {}").addArgument(diff).addArgument(CbcSolver.maxObjDiff_id).log();
                } else if(diff<CbcSolver.maxObjDiff_minus){
					CbcSolver.maxObjDiff_minus = diff;
					CbcSolver.maxObjDiff_minus_id = ILP.getYearMonthCycle();
                    logger.atWarn().setMessage("New minimum objective difference: {} at {}").addArgument(diff).addArgument(CbcSolver.maxObjDiff_minus_id).log();
                }
			}
		}
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ xa_int, ILP._noteFile_xa_int);
		ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ cbc_int, ILP._noteFile_cbc_int);

        logger.atDebug().setMessage("Debug information logging completed").log();
    }

	public static void main(String argv[]) {
        logger.atInfo().setMessage("CbcSolver main method starting").log();

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
        logger.atInfo().setMessage("Model name: {}").addArgument(jCbc.getModelName(solver)).log();
        logger.atInfo().setMessage("Solve status: {}, {}").addArgument(jCbc.status(model)).addArgument(jCbc.secondaryStatus(model)).log();

		long endT = System.currentTimeMillis();

		double time_second = (endT-beginT)/1000.;

        logger.atInfo().setMessage("CBC main solve status: {}, {} ({} sec)").addArgument(jCbc.status(model)).addArgument(jCbc.secondaryStatus(model)).addArgument(time_second).log();


		if (jCbc.status(model) == 0 && jCbc.secondaryStatus(model) == 0) {
			int nCols = jCbc.getNumCols(model);

			SWIGTYPE_p_double sol = jCbc.new_jarray_double(nCols);
			sol = jCbc.getColSolution(solver);

            logger.atInfo().setMessage("Solution:").log();
            logger.atInfo().setMessage("Objective Value = {}").addArgument(jCbc.getObjValue(model)).log();


			for (int j = 0; j < nCols; j++) {
                if (jCbc.isInteger(solver, j) == 1) {
                    logger.atInfo().setMessage("{} = {}").addArgument(jCbc.getColName(model, j)).addArgument(jCbc.jarray_double_getitem(sol, j)).log();
                }
            }
        } else if (jCbc.secondaryStatus(model) == 1) {
            logger.atWarn().setMessage("Model infeasible.").log();

        } else {
            logger.atError().setMessage("Solve error: {}, {}").addArgument(jCbc.status(model)).addArgument(jCbc.secondaryStatus(model)).log();
        }
        jCbc.delete_jCbcModel(model);
        logger.atInfo().setMessage("CbcSolver main method ending").log();
    }

    private static void note_msg(String msg) {
        ILP.writeNoteLn(jCbc.getModelName(solver), msg);
        logger.atDebug().setMessage("{} {}").addArgument(jCbc.getModelName(solver)).addArgument(msg).log();
    }

    private static int solve_2() {
        logger.atDebug().setMessage("Executing solve_2 method").log();
        int r = -99;
		r = solve_2(solve_2_primalT, "2__");
		return r;
    }

    private static int solve_2(double priT, String solvName) {
        logger.atDebug().setMessage("Executing solve_2 method: primalTolerance={}, solveName={}").addArgument(priT).addArgument(solvName).log();
        int r = -99;
		solveName=solvName;
		jCbc.setPrimalTolerance(model, priT);
		jCbc.setIntegerTolerance(model, integerT);
		r = jCbc.solve_2(model, solver, 0);
        logger.atDebug().setMessage("solve_2 completed: returnCode={}").addArgument(r).log();
        return r;
    }

	private static void solve_3() {
        logger.atDebug().setMessage("Executing solve_3 method").log();
        solveName="3__";
		jCbc.setPrimalTolerance(model, solve_3_primalT);
		jCbc.setIntegerTolerance(model, integerT);
		jCbc.solve_3(model, solver, 0);
        logger.atDebug().setMessage("solve_3 completed").log();
    }

	private static void callCbc() {
        logger.atDebug().setMessage("Executing callCbc method").log();
        callCbc("c__");
	}

	private static void callCbc(String solvName) {
        logger.atDebug().setMessage("Executing callCbc method: solveName={}").addArgument(solvName).log();
        solveName=solvName;
		jCbc.callCbc("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model);
        logger.atDebug().setMessage("callCbc completed").log();
    }

    private static void callCbc_R() {
        logger.atDebug().setMessage("Executing callCbc_R method").log();
        solveName = "cR_";
        jCbc.callCbc("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model);
        logger.atDebug().setMessage("callCbc_R completed").log();
    }

	public static void logIntCheck(StudyDataSet sds){
        logger.atDebug().setMessage("Logging integer variable check").log();

        String cbc_int = "";
        cbc_int +=  ","+CbcSolver.solveName;

        if (sds.cycIntDvMap != null && sds.cycIntDvMap.containsKey(ControlData.currCycleIndex)) {
            ArrayList<String> intDVs = new ArrayList<String>(sds.cycIntDvMap.get(ControlData.currCycleIndex));
            Boolean int_violation = false;
            for (String v : sds.allIntDv) {
                if (intDVs.contains(v)){
                    if (Error.getTotalError()==0) {
                        double x = CbcSolver.varDoubleMap.get(v);
                        cbc_int += ","+ x;
                        if  (Math.abs( Math.round(x)-x)>1E-7) {
                            int_violation=true;
                            logger.atWarn().setMessage("Integer variable check violation: name={}, value={}").addArgument(v).addArgument(x).log();
                        }

                    } else {
                        cbc_int += ",?";
                        int_violation=null;
                    }
                } else {
                    cbc_int += ",";
                }
            }
            cbc_int = "," + int_violation + cbc_int;
            logger.atDebug().setMessage("Integer variable check completed: int_violation={}").addArgument(int_violation).log();
        }

			ILP.writeNoteLn(ILP.getYearMonthCycle(), ""+ cbc_int, ILP._noteFile_cbc_int_log);

	}

    public static boolean violationCheck(SWIGTYPE_p_CbcModel model, String modelName, String solveName) {
        logger.atDebug().setMessage("Executing violation check: modelName={}, solveName={}").addArgument(modelName).addArgument(solveName).log();

        if (!cbcViolationCheck) {
            logger.atDebug().setMessage("Violation check disabled").log();
            return false;
        }

        boolean violation = false;
        int ColumnSize = jCbc.getNumCols(model);
		SWIGTYPE_p_double v_ary = jCbc.getColSolution(model);
		Map<String, Dvar> dMap = SolverData.getDvarMap();

        int intViolationCount = 0;
        int boundViolationCount = 0;

        for (int j = 0; j < ColumnSize; j++){
			double v = jCbc.jarray_double_getitem(v_ary,j);
			String k = jCbc.getColName(model,j);

			if (jCbc.isInteger(model,j)==1) {
				if (Math.abs(v-Math.round(v))>integerT_check){
					violation = true;
                    intViolationCount++;
                    logger.atWarn().setMessage("Integer violation check: name={}, value={} (error: {})").addArgument(k).addArgument(v).addArgument(Math.abs(v - Math.round(v))).log();
                    ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":intViolation:::"+k+":"+v, true, false);

				}
			}
			else {
				Dvar d = dMap.get(k);
				if (d.lowerBoundValue.doubleValue() == 0 &&  v<-lowerBoundZero_check){
					violation = true;
                    boundViolationCount++;
                    logger.atWarn().setMessage("Lower bound violation check: name={}, value={} < 0 (allowed: {})").addArgument(k).addArgument(v).addArgument(-lowerBoundZero_check).log();
                    ILP.writeNoteLn(modelName+":"+" Solve_"+solveName+":lbViolation:::"+k+":"+v, true, false);
				}
			}
		}
        logger.atDebug().setMessage("Violation check completed: integer violations={}, bound violations={}, total violation={}")
                .addArgument(intViolationCount).addArgument(boundViolationCount).addArgument(violation).log();
        return violation;

	}

	public static int[] solve_jCbc2021a(){
        logger.atInfo().setMessage("==================== 2021a Solver Start ====================").log();
        int ci = ControlData.currCycleIndex + 1;

        logger.atInfo().setMessage("CBC 2021a Solver: modelName={}, solveFunction={}, date={}-{}-{}, cycle={} [{}]")
                .addArgument(modelName).addArgument(solvFunc).addArgument(ControlData.currYear).addArgument(ControlData.currMonth)
                .addArgument(ControlData.currDay).addArgument(ci).addArgument(ControlData.currCycleName).log();

        logger.atInfo().setMessage("CBC Solver2021a: Solving {}/{}/{} Cycle {} [{}]")
                .addArgument(ControlData.currMonth).addArgument(ControlData.currDay)
                .addArgument(ControlData.currYear).addArgument(ci).addArgument(ControlData.currCycleName).log();

        int status=-99;
		int status2=-99;
        int rv = -99;

        PerformanceTimer solveTimer = new PerformanceTimer("2021a Solver Execution");



        Set<String> a=ControlData.currStudyDataSet.cycIntDvMap.get(ControlData.currCycleIndex);
        a.retainAll(dvIntMap.keySet());

        logger.atDebug().setMessage("Available warm start integer variables: {}").addArgument(a.size()).log();

        if ((useWarm || saveWarm) && a.size()>2) {
            logger.atDebug().setMessage("Using warm start solving").log();

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
            logger.atDebug().setMessage("Starting warm start solve").log();
            rv = jCbc.solve_whs(model,solver,names,values,intSolSize,0);

            if (rv != 0) {
                if (rv == 91 || rv == 92) {
                    logger.atWarn().setMessage("CBC warm-start solve raised exception {}; retrying with callCbc").addArgument(rv).log();
                    note_msg(" Solve_" + solveName + " exception:" + rv + ". Use callCbc.");
                    reloadProblem(true, "exception" + rv);
                } else {
                    logger.atWarn().setMessage("CBC warm-start solve failed with rv={}; retrying with callCbc").addArgument(rv).log();
                    note_msg(" Solve_" + solveName + " infeasible. Use callCbc.");
                    reloadProblem(false, "");
                }
                solveName = "c__";
                rv = jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
            }

        } else {
            logger.atDebug().setMessage("Not using warm start, using callCbc directly").log();
            solveName = "c__";
            rv = jCbc.callCbcJ("-log 0 -primalT 1e-9 -integerT 1e-9 -solve", model, solver);
        }

				// for callCbc only
        status = rv;
        status2 = rv;
        if (rv!=0){
            if (rv==91 || rv==92) {
                logger.atWarn().setMessage("CBC solve exception {}; retrying with relaxed callCbc").addArgument(rv).log();
                note_msg(" Solve_"+solveName+" exception:"+rv+". Use callCbcR.");
                reloadProblem(true, "exception"+rv);
            } else {
                logger.atWarn().setMessage("CBC solve failed with rv={}; retrying with relaxed callCbc").addArgument(rv).log();
                note_msg(" Solve_"+solveName+" infeasible. Use callCbcR.");
                reloadProblem(false, "");
            }
            solveName="cR_";
            rv=jCbc.callCbcJ("-log 0 -primalT 1e-7 -integerT 1e-9 -solve", model, solver);
            status = jCbc.status(model);
            status2 = jCbc.secondaryStatus(model);
        }
        solveTimer.stop();

        if (status != 0 || status2 != 0) {
            logger.atError().setMessage("CBC solve failed: status={}, secondaryStatus={}").addArgument(status).addArgument(status2).log();
            reloadAndWriteLp("_infeasible", true, true);
            getSolverInformation(status, status2);
            iis();
        } else {
            logger.atInfo().setMessage("2021a Solver completed successfully").log();
        }

        logger.atInfo().setMessage("==================== 2021a Solver End ====================").log();
        return new int[]{status, status2};
    }

    public static void logPerformanceSummary() {
        performanceStats.logSummary();
    }
}