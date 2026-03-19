package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Alias;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.External;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Goal;
import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Timeseries;
import gov.ca.water.wrims.engine.core.evaluator.CondensedReferenceCacheAndRead.CondensedReferenceCache;
import gov.ca.water.wrims.engine.core.external.ExternalFunction;
import gov.ca.water.wrims.engine.core.solver.ortools.OrToolsSolver;
import hec.heclib.dss.HecDss;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.sunsetsoft.xa.Optimizer;

public class ControlData {
	public static ArrayList<Integer> currTimeStep;
	public static StudyDataSet currStudyDataSet=new StudyDataSet();
	public static ModelDataSet currModelDataSet=new ModelDataSet();
	public static LinkedHashMap<String, Svar> parameterMap=new LinkedHashMap<String, Svar>();
	public static Map<String, Svar> currSvMap=new HashMap<String, Svar>() ;
	public static Map<String, Svar> currSvFutMap=new HashMap<String, Svar>() ;
	public static Map<String, Timeseries> currTsMap=new HashMap<String, Timeseries>();
	public static Map<String, Dvar> currDvMap=new HashMap<String, Dvar>();
	public static Map<String, Dvar> currDvSlackSurplusMap=new HashMap<String, Dvar>();
	public static Map<String, Alias> currAliasMap=new HashMap<String, Alias>();
	public static Map<String, Goal> currGoalMap=new HashMap<String, Goal>();
	public static Map<String, External> currExMap = new HashMap<String, External> ();
	public static Map<String, Timeseries> allTsMap=new HashMap<String, Timeseries>();
	public static Map<String, String> allExternalFunction = new HashMap<String, String> ();
	public static Map<String, ExternalFunction> allExternalFunctionMap = new HashMap<String, ExternalFunction>();  
	public static ArrayList<String> allDll= new ArrayList<String>() ;
	public static String currCycleName;
	public static int currCycleIndex;
	public static int currEvalTypeIndex; //0=sv; 1=dv; 2=alias; 3=goal; 4=external; 5=timeseries define; 6=timeseries reading; 7=weight; 8=initial_vars; 9=conditional_include
	public static String currEvalName;
	public static int currDay=1;
	public static int currMonth;
	public static int currYear;
	public static String defaultTimeStep="1MON";
	public static String timeStep ="1MON"; 
	public static String partE="1MON";
	public static int startYear;       
	public static int startMonth;
	public static int startDay;
	public static int endYear ;
	public static int endMonth;
	public static int endDay;
	public static int cycleStartDay;
	public static int cycleStartMonth;
	public static int cycleStartYear;
	public static int cycleEndDay;
	public static int cycleEndMonth;
	public static int cycleEndYear;
	public static double cycleTimeStepPriority=0;
	public static ArrayList<Integer> totalTimeStep;
	public static Date monthlyStartTime;
	public static Date dailyStartTime;
	public static String simulationTimeFrame;
	public static String partA="";
	public static String svDvPartF="";
	public static String initPartF;
	//public static Group groupInit;
	//public static Group groupSvar;
	//public static Group groupSvar2;
	public static CondensedReferenceCache cacheInit;
	public static CondensedReferenceCache cacheSvar;
	public static CondensedReferenceCache cacheSvar2;
	//public static DSSDataWriter writer;
	public static HecDss dvDss;
	public static String solverName="cbc'";
	public static Optimizer xasolver;
	public static OrToolsSolver otsolver;
	//public static int cbcSolverOptions = 2;
	public static Double clp_cbc_objective;
	public static String clp_cbc_note="";
	public static double lpsolve_objective;
	public static double gurobi_objective;
	public static boolean isPostProcessing=false;
	public static boolean sendAliasToDvar=false;
	public static boolean outputWreslCSV=false;
	public static boolean showRunTimeMessage=false;
	public static boolean showWreslLog=true;
	public static boolean writeInitToDVOutput=true;
	public static int solverType;
	public static boolean isParseStudy=true;
	public static boolean ignoreError=false;
	public static boolean allowSvTsInit=false;
	public static boolean allRestartFiles=false;
	public static int numberRestartFiles=12;
	public static int vHecLib=6;
	public static int outputType=0;
	public static boolean isOutputCycle=false;
	public static boolean outputAllCycles=true;
	public static String selectedCycleOutput="\'\'";
	public static String[] selectedCycles=new String[0];
	public static boolean initHDF5=false;
	public static double solverTime_xa=0;
	public static double solverTime_xa_this=0;
	public static double solverTime_cbc=0;
	public static double solverTime_cbc_this=0;
	public static double solverCreationTime_cbc=0;
	public static double solverCreationTime_xa=0;
	public static double lpFileWritingTime=0;
	public static double cdTime=0;	
	public static double adTime=0;	
	public static boolean useCplexLpString=false;
	public static boolean saveCplexLpStringToFile=false;
	public static boolean useCbcWarmStart=false;
	public static boolean cbcCheckIntErr=true;	
	public static boolean cbcLogNativeLp=false;
	public static boolean writeCbcSolvingTime=false;
	public static boolean isNameSorting = false;
	//public static Map<Integer,LinkedHashSet<String>> cycIntDvMap;
	//public static LinkedHashSet<String> allIntDv;
	public static ArrayList<Integer> cycWarmStart;
	public static ArrayList<Integer> cycWarmStop;
	public static ArrayList<Integer> cycWarmUse;
	public static boolean cbc_debug_routeXA=false; //use xa solution	
	public static boolean cbc_debug_routeCbc=false;	// use cbc solution
	public static String[] watchList={};
	public static double watchList_tolerance = 1e99;
	public static double zeroTolerance =  1e-11;  // can read from config cbcToleranceZero
	public static double relationTolerance;
	public static Double xaIntegerT =  null;
	public static String xaSort = null;
	public static boolean resimDate=false;
	public static boolean resimGroundwater=false;
	public static int resimYear;
	public static int resimMonth;
	public static int resimDay;
	public static int cycleDataStartYear;
	public static int cycleDataStartMonth;
	public static int cycleDataStartDay;
	    
	public static String USER;       
	public static String PASS;  
	public static String databaseURL;
	public static String sqlGroup="calsim";
	public static int ovOption=0;
	public static String ovFile="";
	public static Map<String, String> ovPartBC=new HashMap<String, String>();
	public static boolean isSimOutput = true;
	
	public static boolean enableProgressLog = false;
	
	public static LocalDateTime ldt = LocalDateTime.now();
	public static String dateTimeAppend = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ENGLISH).format(ldt);

	public static boolean printGWFuncCalls=false;
	
	public static int yearOutputSection=-1;
	public static int monMemSection=-1;
	public static int outputYear;
	public static int outputMonth;
	public static int outputDay;
	public static int memStartYear;
	public static int memStartMonth;
	public static int memStartDay;
	public static int prevMemYear;
	public static int prevMemMonth;
	public static int prevMemDay;
	public static int prevOutputYear;
	public static int prevOutputMonth;
	public static int prevOutputDay;
	public static Date prevMemDate;
	public static Date memStartDate;
	public static Date prevOutputDate;
	public static int nThreads=1;
	public static boolean unchangeGWRestart=false;
	public static boolean genSVCatalog=true;
	public static boolean showTimeUsage=true;
	public static int t_cbc=0;
	public static int t_xa=0;
	public static int t_ts=0;
	public static int t_svar=0;
	public static int t_dvar=0;
	public static int t_goal=0;
	public static int t_wt=0;
	public static int t_wtss=0;
	public static int t_as=0;
	public static int t_readTs=0;
	public static int t_writeDss=0;
	public static int t_cam=0;
	public static int t_parse=0;
	public static int t_ann=0;
	public static int n_ann=0;
	public static int t_annx2=0;
	public static int n_annx2=0;
	public static int t_annec=0;
	public static int n_annec=0;
	public static int t_annlinegen=0;
	public static int n_annlinegen=0;
	public static int t_anngetndo_x2=0;
	public static int n_anngetndo_x2=0;
	public static int t_anngetndo_x2_curmonndosplit=0;
	public static int n_anngetndo_x2_curmonndosplit=0;
	public static int t_annec_matchdsm2=0;
	public static int n_annec_matchdsm2=0;
	
	public static int pid=-1;
	
	public ControlData(){
	}
	
	public static String getPartE(){
		return timeStep;
	}
}
