package gov.ca.water.wrims.engine.core.config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FilenameUtils;
import org.coinor.cbc.jCbc;

import gov.ca.water.wrims.engine.core.components.BuildProps;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;
import gov.ca.water.wrims.engine.core.ilp.ILP;
import gov.ca.water.wrims.engine.core.solver.CbcSolver;
import gov.ca.water.wrims.engine.core.solver.LPSolveSolver;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ParamTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ParserUtils;
import gov.ca.water.wrims.engine.core.wreslplus.grammar.WreslPlusParser;

public class ConfigUtils {

	private static Map<String, String> argsMap;

	public static LinkedHashMap<String, ParamTemp> paramMap = new LinkedHashMap<String, ParamTemp>();
	
	public static Map<String, String> configMap = new HashMap<String, String>();

	public static void loadArgs(String[] args) {

		// for Error.log header
		ControlData.currEvalTypeIndex=8;
		
		// print version number then exit
		if (args.length==1 && args[0].equalsIgnoreCase("-version") ) {
		
			System.out.println("WRIMS "+new BuildProps().getVN());
			System.exit(0);
		}
		
		
		
		argsMap = new HashMap<String, String>();

		for (int i = 0; i < args.length; i++) {

			try {
				String key = args[i].substring(0, args[i].indexOf("="));
				String val = args[i].substring(args[i].indexOf("=") + 1, args[i].length());
				val=val.replaceAll("\"", "");

				argsMap.put(key.toLowerCase(), val); 

			}
			catch (Exception e) {
				System.out.println("Example: ");
				System.out.println("-config=\"D:\\test\\example.config\"");
//				System.out.println("Example: ");
//				System.out.println("-mainwresl=\"C:\\study\\main.wresl\"");
				System.exit(1);
			}

			//System.out.println(argsMap);

		}


		
		// load config file
		if (argsMap.keySet().contains("-config")) {
			
			String configFilePath = FilenameUtils.removeExtension(argsMap.get("-config"))+".config";
			argsMap.put("-config", configFilePath);
			
			System.out.println("\nWARNING!! Config file and RUN directory must be placed in the same folder! \n");
			System.out.println("Loading config file: "+argsMap.get("-config"));
			StudyUtils.configFilePath = argsMap.get("-config");
			loadConfig(StudyUtils.configFilePath);
			if (Error.getTotalError()>0) {
				Error.writeErrorLog();
			}
			
		// compile to serial object named *.par
		} else if (argsMap.keySet().contains("-mainwresl")) {

			System.out.println("Compiling main wresl file: "+argsMap.get("-mainwresl"));
			StudyUtils.compileOnly = true;
			FilePaths.setMainFilePaths(argsMap.get("-mainwresl"));

		} else if (argsMap.keySet().contains("-mainwreslplus")) {

			System.out.println("Compiling main wresl file: "+argsMap.get("-mainwreslplus"));
			StudyUtils.compileOnly = true;
			StudyUtils.useWreslPlus=true;
			FilePaths.setMainFilePaths(argsMap.get("-mainwreslplus"));

		}else {
			System.out.println("Example: ");
			System.out.println("-config=\"D:\\test\\example.config\"");
//			System.out.println("Example: ");
//			System.out.println("-mainwresl=\"C:\\study\\main.wresl\"");
			System.exit(1);
		}

	}

	private static void loadConfig(String configFile) {
		
		//StudyUtils.config_errors = 0; // reset
		String k=null;

		configMap = new HashMap<String, String>();

		configMap = checkConfigFile(configFile);
		
		String mainfile = configMap.get("mainfile").toLowerCase();
		
		String mainFilePath = "";
		
		if (mainfile.contains(":")){
			mainFilePath =  new File(mainfile).getAbsolutePath();
		} else { 
			mainFilePath =  new File(StudyUtils.configDir, mainfile).getAbsolutePath();
		}
		
		File validatedAbsFile = new File(mainFilePath).getAbsoluteFile();
		if (!validatedAbsFile.exists()) { 
			Error.addConfigError("File not found: " + mainFilePath); 
			Error.writeErrorLog();
		}
		
		if (mainfile.endsWith(".par")) {
			StudyUtils.loadParserData = true;
			StudyUtils.parserDataPath = mainFilePath;		
			FilePaths.setMainFilePaths(mainFilePath);
		}
		else if (mainfile.endsWith(".wresl")) {
			FilePaths.setMainFilePaths(mainFilePath);
		}
		else {
			Error.addConfigError("Invalid main file extension: " + configMap.get("mainfile"));
			Error.writeErrorLog();
			//System.out.println("Invalid main file extension: " + configMap.get("mainfile"));
			//System.out.println("Specify either *.wresl or *.par");
			//System.exit(1);
		}

		// FilePaths.mainDirectory = configMap.get("maindir");
		System.out.println("MainFile:       "+FilePaths.fullMainPath);
		
		// CbcLibName // default is jCbc and it's not used for version selection
		k = "cbclibname";
		if (configMap.keySet().contains(k)){
			
			CbcSolver.cbcLibName = configMap.get(k);
		}
		// need to know jCbc version to determine solving options 
		System.loadLibrary(CbcSolver.cbcLibName);
		System.out.println("CbcLibName: "+CbcSolver.cbcLibName);
		
		try {
			
			if (configMap.get("groundwaterdir").length()>0) {
				if (configMap.get("groundwaterdir").contains(":")){
					FilePaths.groundwaterDir =  new File(configMap.get("groundwaterdir")).getCanonicalPath()+File.separator;
				} else { 
					FilePaths.groundwaterDir =  new File(StudyUtils.configDir, configMap.get("groundwaterdir")).getCanonicalPath()+File.separator;
				}
				System.out.println("GroundWaterDir: "+FilePaths.groundwaterDir);
			} else {
				FilePaths.groundwaterDir = "None";
			}
			
			if (configMap.get("svarfile").contains(":")){
				FilePaths.setSvarFilePaths(new File(configMap.get("svarfile")).getCanonicalPath());
			} else {
				FilePaths.setSvarFilePaths(new File(StudyUtils.configDir, configMap.get("svarfile")).getCanonicalPath());
			}
			System.out.println("SvarFile:       "+FilePaths.fullSvarFilePath);
			
			if (configMap.get("svarfile2").equals("")){
				FilePaths.setSvarFile2Paths("");
			}else{
				if (configMap.get("svarfile2").contains(":")){
					FilePaths.setSvarFile2Paths(new File(configMap.get("svarfile2")).getCanonicalPath());
				} else {
					FilePaths.setSvarFile2Paths(new File(StudyUtils.configDir, configMap.get("svarfile2")).getCanonicalPath());
				}
				System.out.println("SvarFile2:       "+FilePaths.fullSvarFile2Path);
			}
			
			if (configMap.get("initfile").contains(":")){
				FilePaths.setInitFilePaths(new File(configMap.get("initfile")).getCanonicalPath());
			} else {
				FilePaths.setInitFilePaths(new File(StudyUtils.configDir, configMap.get("initfile")).getCanonicalPath());
			}
			System.out.println("InitFile:       "+FilePaths.fullInitFilePath);
			
			if (configMap.get("dvarfile").contains(":")) {
				FilePaths.setDvarFilePaths(new File(configMap.get("dvarfile")).getCanonicalPath());
			} else {
				FilePaths.setDvarFilePaths(new File(StudyUtils.configDir, configMap.get("dvarfile")).getCanonicalPath());
			}
			System.out.println("DvarFile:       "+FilePaths.fullDvarDssPath);
		
		} catch (IOException e){
			Error.addConfigError("Invalid file path in config file");
			Error.writeErrorLog();
			//System.out.println("Invalid file path");
			e.printStackTrace();
		}
		
		StudyUtils.configFileName = new File(configFile).getName();
		
		FilePaths.csvFolderName = "";

		if (configMap.get("showwresllog").equalsIgnoreCase("no") || configMap.get("showwresllog").equalsIgnoreCase("false")){
			ControlData.showWreslLog = false;
		}
		if (configMap.get("lookupsubdir").length()>0 ){
			FilePaths.lookupSubDirectory = configMap.get("lookupsubdir");
			System.out.println("LookupSubDir:   "+FilePaths.lookupSubDirectory);
		}
		//ControlData.showWreslLog = !(configMap.get("showwresllog").equalsIgnoreCase("no"));
		
		ControlData.svDvPartF = configMap.get("svarfpart");
		ControlData.initPartF = configMap.get("initfpart");
		ControlData.partA = configMap.get("svarapart");
		ControlData.partE = configMap.get("timestep");
		ControlData.timeStep = configMap.get("timestep");

		ControlData.startYear = Integer.parseInt(configMap.get("startyear"));
		ControlData.startMonth = Integer.parseInt(configMap.get("startmonth"));
		ControlData.startDay = Integer.parseInt(configMap.get("startday"));

		ControlData.endYear = Integer.parseInt(configMap.get("stopyear"));
		ControlData.endMonth = Integer.parseInt(configMap.get("stopmonth"));
		ControlData.endDay = Integer.parseInt(configMap.get("stopday"));

		ControlData.solverName = configMap.get("solver");
		
		ControlData.currYear = ControlData.startYear;
		ControlData.currMonth = ControlData.startMonth;
		ControlData.currDay = ControlData.startDay;
		
		System.out.println("TimeStep:       "+ControlData.timeStep);
		System.out.println("SvarAPart:      "+ControlData.partA);
		System.out.println("SvarFPart:      "+ControlData.svDvPartF);
		System.out.println("InitFPart:      "+ControlData.initPartF);
		System.out.println("StartYear:      "+ControlData.startYear);
		System.out.println("StartMonth:     "+ControlData.startMonth);
		System.out.println("StartDay:       "+ControlData.startDay);
		System.out.println("StopYear:       "+ControlData.endYear);
		System.out.println("StopMonth:      "+ControlData.endMonth);
		System.out.println("StopDay:        "+ControlData.endDay);
		System.out.println("Solver:         "+ControlData.solverName);
		
		final String[] solvers = {"xa","xalog","clp0","clp1","clp","lpsolve","gurobi","cbc0","cbc1","cbc"};

		if (!Arrays.asList(solvers).contains(ControlData.solverName.toLowerCase())){
			Error.addConfigError("Solver name not recognized: "+ControlData.solverName);
			Error.writeErrorLog();
		 } else if (ControlData.solverName.toLowerCase().contains("cbc")) {
			 CbcSolver.cbcVersion = jCbc.getVersion();
			 System.out.println("CbcVersion: "+CbcSolver.cbcVersion);
			 if (CbcSolver.cbcVersion.contains("2.9.9")) {
				 CbcSolver.usejCbc2021 = true; 
				 if (CbcSolver.cbcVersion.contains("2.9.9.2")) {
					 CbcSolver.usejCbc2021a = true;
				 }
				 CbcSolver.cbcViolationCheck = false; // can overwrite
				 ControlData.useCbcWarmStart = true; //  cannot overwrite
			 } else {
				 CbcSolver.cbcViolationCheck = true; // can overwrite
				 ControlData.useCbcWarmStart = false; //  cannot overwrite
			 }
			 System.out.println("Cbc2021: "+CbcSolver.usejCbc2021);
			 System.out.println("Cbc2021a: "+CbcSolver.usejCbc2021a);
		 }
		
		// SendAliasToDvar default is false
		if (configMap.keySet().contains("sendaliastodvar")){
			
			String s = configMap.get("sendaliastodvar");
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				ControlData.sendAliasToDvar = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				ControlData.sendAliasToDvar = false;	
			} else {
				ControlData.sendAliasToDvar  = false;	
			}
			
		}
		System.out.println("SendAliasToDvar: "+ControlData.sendAliasToDvar);
		
		
		// PrefixInitToDvarFile
		if (configMap.keySet().contains("prefixinittodvarfile")){
			
			String s = configMap.get("prefixinittodvarfile");
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				ControlData.writeInitToDVOutput = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				ControlData.writeInitToDVOutput = false;	
			} else {
				ControlData.writeInitToDVOutput = false;	
			}
		}else{
			ControlData.writeInitToDVOutput = true;
		}
		System.out.println("PrefixInitToDvarFile: "+ControlData.writeInitToDVOutput);

		// SolveCompare
		k = "solvecompare";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("xa_cbc")){
				ControlData.cbc_debug_routeXA = false;	
				ControlData.cbc_debug_routeCbc = true;	
			} else if (s.equalsIgnoreCase("cbc_xa")) {
				ControlData.cbc_debug_routeXA = true;
				ControlData.cbc_debug_routeCbc = false;	
			} else {
				ControlData.cbc_debug_routeXA = false;
				ControlData.cbc_debug_routeCbc = false;	
			}
		}else{
			ControlData.cbc_debug_routeXA = false;
			ControlData.cbc_debug_routeCbc = false;	
		}
		System.out.println("SolveCompare (use XA solution as base): "+ControlData.cbc_debug_routeXA);
		System.out.println("SolveCompare (use Cbc solution as base): "+ControlData.cbc_debug_routeCbc);

		// watch variable
		if (configMap.keySet().contains("watch")){
			
			String s = configMap.get("watch").toLowerCase();
			ControlData.watchList = s.split(",");
			
			System.out.println("Watch: "+Arrays.toString(ControlData.watchList));

		}
		
		// watch variable tolerance
		k = "watcht";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				ControlData.watchList_tolerance = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("watchT: Error reading config value");
			}
			
		}
		System.out.println("watchT: "+ControlData.watchList_tolerance);

		// CbcDebugObjDiff // default is false
		k = "cbcdebugobjdiff";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				CbcSolver.debugObjDiff = true;	
			} else {
				CbcSolver.debugObjDiff = false;	
			}
		}else{
			CbcSolver.debugObjDiff = false;	
		}
		System.out.println("CbcDebugObjDiff: "+CbcSolver.debugObjDiff);			
		
		// CbcObjLog // default is true
		k = "cbcobjlog";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				CbcSolver.logObj = true;	
			} else {
				CbcSolver.logObj = false;	
			}
		}else{
			CbcSolver.logObj = true;
		}
		System.out.println("CbcObjLog: "+CbcSolver.logObj);		
		
		// CbcLogNativeLp // default is false
		k = "cbclognativelp";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				ControlData.cbcLogNativeLp = true;	
			} else {
				ControlData.cbcLogNativeLp = false;	
			}
		}else{
			ControlData.cbcLogNativeLp = false;
		}
		System.out.println("CbcLogNativeLp: "+ControlData.cbcLogNativeLp);
		
		// CbcWarmStart // default is false
		k = "cbcwarmstart";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				ControlData.useCbcWarmStart = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				ControlData.useCbcWarmStart = false;	
			} 
		}
		// warmstart is true if cbc2021 is true
		if (CbcSolver.usejCbc2021) { 
			ControlData.useCbcWarmStart = true;
		}
		System.out.println("CbcWarmStart: "+ControlData.useCbcWarmStart);
		
		// CbcViolationCheck  default {cbc2021:false, otherwise:true}
		k = "cbcviolationcheck";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				CbcSolver.cbcViolationCheck = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				CbcSolver.cbcViolationCheck = false;	
			} 
			
		}
		System.out.println("CbcViolationCheck: "+CbcSolver.cbcViolationCheck);		

		
		// CbcSolveFunction // default is SolveFull
		k = "cbcsolvefunction";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);

			if (s.equalsIgnoreCase("solveu")){
				CbcSolver.solvFunc=CbcSolver.solvU;	
			} else if (s.equalsIgnoreCase("solve2")){
				CbcSolver.solvFunc=CbcSolver.solv2;	
			} else if (s.equalsIgnoreCase("solve3")){
				CbcSolver.solvFunc=CbcSolver.solv3;	
			} else if (s.equalsIgnoreCase("callCbc")){
				CbcSolver.solvFunc=CbcSolver.solvCallCbc;	
			} else {
				CbcSolver.solvFunc=CbcSolver.solvFull;	
			}
		}else{
			CbcSolver.solvFunc=CbcSolver.solvFull;	
		}
		System.out.println("CbcSolveFunction: "+CbcSolver.solvFunc);
				
		// CbcToleranceZero // default is 1e-11 ControlData.zeroTolerance
		k = "cbctolerancezero";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				ControlData.zeroTolerance = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcToleranceZero: Error reading config value");
			}
			
		}
		System.out.println("CbcToleranceZero: "+ControlData.zeroTolerance);
		
		
		// CbcToleranceInteger default 1e-9
		k = "cbctoleranceinteger";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.integerT = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcToleranceInteger: Error reading config value");
			}
			
		}
		System.out.println("CbcToleranceInteger: "+CbcSolver.integerT);

		// CbcLowerBoundZeroCheck default max(solve_2_primalT_relax*10, 1e-6)
		k = "cbclowerboundzerocheck";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.lowerBoundZero_check = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcLowerBoundZeroCheck: Error reading config value");
			}
			
		}
		System.out.println("CbcLowerBoundZeroCheck: "+CbcSolver.lowerBoundZero_check);		

		// CbcToleranceIntegerCheck default 1e-8
		k = "cbctoleranceintegercheck";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.integerT_check = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcToleranceIntegercheck: Error reading config value");
			}
			
		}
		System.out.println("CbcToleranceIntegercheck: "+CbcSolver.integerT_check);			
		
		// CbcSolutionRounding  default is true
		k = "cbcsolutionrounding";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				CbcSolver.cbcSolutionRounding = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				CbcSolver.cbcSolutionRounding = false;	
			} else {
				CbcSolver.cbcSolutionRounding = true;	
			}
		}else{
			CbcSolver.cbcSolutionRounding = true;
		}
		System.out.println("CbcSolutionRounding: "+CbcSolver.cbcSolutionRounding);		

		// CbcViolationRetry  default is true
		k = "cbcviolationretry";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				CbcSolver.cbcViolationRetry = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				CbcSolver.cbcViolationRetry = false;	
			} else {
				CbcSolver.cbcViolationRetry = true;	
			}
		}else{
			CbcSolver.cbcViolationRetry = true;
		}
		System.out.println("CbcViolationRetry: "+CbcSolver.cbcViolationRetry);		
		
		
//		// XAIntegerT
//		k = "xaintegert";
//		if (configMap.keySet().contains(k)){
//			
//			String s = configMap.get(k);			
//			
//			try {
//				ControlData.xaIntegerT = Double.parseDouble(s);
//			} catch (NumberFormatException e) {
//				System.out.println("XAIntegerT: Error reading config value");
//			}
//			
//		}
//		System.out.println("XAIntegerT: "+ControlData.xaIntegerT);
		
		
//		// xasort // default is unknown
//		k = "xasort";
//		if (configMap.keySet().contains(k)){
//			
//			String s = configMap.get(k).toLowerCase();
//			
//			if (s.equals("yes")||s.equals("no")||s.equals("col")||s.equals("row")){
//				ControlData.xaSort=s;	
//			} else {
//				System.out.println("XASort: No | Yes | Row | Col");;
//			}
//		}
//		System.out.println("XASort: "+ControlData.xaSort);

		// CbcHintTimeMax // default is 100 sec
		k = "cbchinttimemax";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.cbcHintTimeMax = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcHintTimeMax: Error reading config value");
			}
			
		}
		System.out.println("CbcHintTimeMax: "+CbcSolver.cbcHintTimeMax);
		
		// CbcHintRelaxPenalty // default is 9000
		k = "cbchintrelaxpenalty";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.cbcHintRelaxPenalty = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcHintRelaxPenalty: Error reading config value");
			}
			
		}
		System.out.println("CbcHintRelaxPenalty: "+CbcSolver.cbcHintRelaxPenalty);

		
		// CbcTolerancePrimal // default is 1e-9
		k = "cbctoleranceprimal";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.solve_2_primalT = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcTolerancePrimal: Error reading config value");
			}
			
		}
		System.out.println("CbcTolerancePrimal: "+CbcSolver.solve_2_primalT);

		// CbcTolerancePrimalRelax // default is 1e-7
		k = "cbctoleranceprimalrelax";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.solve_2_primalT_relax = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcTolerancePrimalRelax: Error reading config value");
			}
			
		}
		System.out.println("CbcTolerancePrimalRelax: "+CbcSolver.solve_2_primalT_relax);
		ControlData.relationTolerance = Math.max(CbcSolver.solve_2_primalT_relax*10, 1e-6);
		
		// CbcSolveWhsPrimalT // default is 1e-9
		k = "cbctolerancewarmprimal";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.solve_whs_primalT = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcToleranceWarmPrimal: Error reading config value");
			}
			
		}
		System.out.println("CbcToleranceWarmPrimal: "+CbcSolver.solve_whs_primalT);

		// CbcLogStartDate // default is 999900
		k = "cbclogstartdate";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.cbcLogStartDate = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcLogStartDate: Error reading config value");
			}
			
		}
		System.out.println("CbcLogStartDate: "+CbcSolver.cbcLogStartDate);

		// CbcLogStopDate // default is 999900
		k = "cbclogstopdate";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.cbcLogStopDate = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				System.out.println("CbcLogStopDate: Error reading config value");
			}
			
		}
		System.out.println("CbcLogStopDate: "+CbcSolver.cbcLogStopDate);
		
		// LogIfObjDiff
		k = "logifobjdiff";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.log_if_obj_diff = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("LogIfObjDiff: Error reading config value");
			}
			
		}
		System.out.println("LogIfObjDiff: "+CbcSolver.log_if_obj_diff);
		
		// RecordIfObjDiff
		k = "recordifobjdiff";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);			
			
			try {
				CbcSolver.record_if_obj_diff = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println("RecordIfObjDiff: Error reading config value");
			}
			
		}
		System.out.println("RecordIfObjDiff: "+CbcSolver.record_if_obj_diff);

		k = "CbcWhsScaling"; //default is true
		CbcSolver.whsScaling = readBoolean(configMap, k, true);
		System.out.println(k+": "+CbcSolver.whsScaling);

		k = "CbcWhsSafe"; //default is false
		CbcSolver.whsSafe = readBoolean(configMap, k, false);
		System.out.println(k+": "+CbcSolver.whsSafe);
		
		k = "CbcDebugDeviation"; //default is false
		CbcSolver.debugDeviation = readBoolean(configMap, k, false);
		System.out.println(k+": "+CbcSolver.debugDeviation);
		
		if (CbcSolver.debugDeviation){
			
			k = "CbcDebugDeviationMin"; // default is 200
			CbcSolver.debugDeviationMin = readDouble(configMap, k, 200);
			System.out.println(k+": "+CbcSolver.debugDeviationMin);
		
			k = "CbcDebugDeviationWeightMin"; // default is 5E5
			CbcSolver.debugDeviationWeightMin = readDouble(configMap, k, 5E5);
			System.out.println(k+": "+CbcSolver.debugDeviationWeightMin);
			
			k = "CbcDebugDeviationWeightMultiply"; // default is 100
			CbcSolver.debugDeviationWeightMultiply = readDouble(configMap, k, 100);
			System.out.println(k+": "+CbcSolver.debugDeviationWeightMultiply);
			
			k = "CbcDebugDeviationFindMissing"; //default is false
			CbcSolver.debugDeviationFindMissing = readBoolean(configMap, k, false);
			System.out.println(k+": "+CbcSolver.debugDeviationFindMissing);
		
		}
		
		// Warm2ndSolveFunction // default is Solve2
		k = "warm2ndsolvefunction";
		if (configMap.keySet().contains(k)){
			
			String s = configMap.get(k);
			
			if (s.equalsIgnoreCase("solve3")){
				CbcSolver.warm_2nd_solvFunc=CbcSolver.solv3;		
			} else {
				CbcSolver.warm_2nd_solvFunc=CbcSolver.solv2;	
			}
		}else{
			CbcSolver.warm_2nd_solvFunc=CbcSolver.solv2;		
		}
		System.out.println("Warm2ndSolveFunction: "+CbcSolver.warm_2nd_solvFunc);

		// ParserCheckVarUndefined
		if (configMap.keySet().contains("parsercheckvarundefined")){
			
			String s = configMap.get("parsercheckvarundefined");
			
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				StudyUtils.parserCheckVarUndefined = true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				StudyUtils.parserCheckVarUndefined = false;	
			} else {
				StudyUtils.parserCheckVarUndefined = true;	
			}
		}else{
			StudyUtils.parserCheckVarUndefined = true;
		}
		System.out.println("ParserCheckVarUndefined: "+StudyUtils.parserCheckVarUndefined);
		
		
		if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
			
			// LpSolveConfigFile
			if (configMap.keySet().contains("lpsolveconfigfile")) {

				String f = configMap.get("lpsolveconfigfile");

				try {

					File sf = new File(StudyUtils.configDir, f);
					if (sf.exists()) {					
						LPSolveSolver.configFile = sf.getCanonicalPath();
					} else {
						//System.out.println("#Error: LpSolveConfigFile not found: " + f);
						Error.addConfigError("LpSolveConfigFile not found: " + f);
						Error.writeErrorLog();
					}

				} catch (Exception e) {
					Error.addConfigError("LpSolveConfigFile not found: " + f);
					Error.writeErrorLog();
					//System.out.println("#Error: LpSolveConfigFile not found: " + f);
					e.printStackTrace();
				}
			} else {
				Error.addConfigError("LpSolveConfigFile not defined. ");
				Error.writeErrorLog();
				//System.out.println("#Error: LpSolveConfigFile not defined. ");
			}
		
			System.out.println("LpSolveConfigFile:   "+LPSolveSolver.configFile);
			
			
			// LpSolveNumberOfRetries default is 0 retry
			if (configMap.keySet().contains("lpsolvenumberofretries")){
				
				String s = configMap.get("lpsolvenumberofretries");
				
				try {
					LPSolveSolver.numberOfRetries = Integer.parseInt(s);
					
				} catch (Exception e) {
					//System.out.println("#Error: LpSolveNumberOfRetries not recognized: " + s);
					Error.addConfigError("LpSolveNumberOfRetries not recognized: " + s);
					Error.writeErrorLog();
					
				}				
			}
			System.out.println("LpSolveNumberOfRetries: "+LPSolveSolver.numberOfRetries);
			
			
		}
		
		// processed only for ILP
		
		// TODO: lpsolve and ilp log is binded. need to enable direct linking instead of reading file
		if(ControlData.solverName.equalsIgnoreCase("XALOG")){
			configMap.put("ilplog","yes");
			ILP.loggingCplexLp = true;
		}else if (ControlData.solverName.equalsIgnoreCase("cbc0")||ControlData.solverName.equalsIgnoreCase("cbc1")) {
			configMap.put("ilplog","yes");
			configMap.put("ilplogallcycles","yes");
			ILP.loggingCplexLp = true;
		}else if (ControlData.solverName.equalsIgnoreCase("clp0")||ControlData.solverName.equalsIgnoreCase("clp1")) {
			configMap.put("ilplog","yes");
			configMap.put("ilplogallcycles","yes");
			ILP.loggingCplexLp = true;
		}else if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
			configMap.put("ilplog","yes");
			ILP.loggingLpSolve = true;
		}else if (ControlData.solverName.equalsIgnoreCase("Gurobi")) {
			configMap.put("ilplog","yes");
			ILP.loggingCplexLp = true;
		}		
		
		String strIlpLog = configMap.get("ilplog");
		if (strIlpLog.equalsIgnoreCase("yes") || strIlpLog.equalsIgnoreCase("true")) {

			ILP.logging = true;
			// IlpLogAllCycles
			// default is false
			if (configMap.keySet().contains("ilplogallcycles")) {

				String s = configMap.get("ilplogallcycles");

				if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
					ILP.loggingAllCycles = true;
				}
				else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
					ILP.loggingAllCycles = false;
				}
				else {
					ILP.loggingAllCycles = true;
				}
			}
			// IlpLogVarValue
			// default is false
			if (configMap.keySet().contains("ilplogvarvalue")) {

				String s = configMap.get("ilplogvarvalue");

				if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
					ILP.loggingVariableValue = true;
				}
				else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
					ILP.loggingVariableValue = false;
				}
				else {
					ILP.loggingVariableValue = false;
				}
			}

			// ilplogvarvalueround
			// default is false
			if (configMap.keySet().contains("ilplogvarvalueround")) {

				String s = configMap.get("ilplogvarvalueround");

				if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
					ILP.loggingVariableValueRound = true;
				} else {
					ILP.loggingVariableValueRound = false;
				}
			}
			
			// ilplogvarvalueround10
			// default is false
			if (configMap.keySet().contains("ilplogvarvalueround10")) {

				String s = configMap.get("ilplogvarvalueround10");

				if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
					ILP.loggingVariableValueRound10 = true;
				} else {
					ILP.loggingVariableValueRound10 = false;
				}
			}
			
			// IlpLogMaximumFractionDigits
			// default is 8
			if (configMap.keySet().contains("ilplogmaximumfractiondigits")) {

				String s = configMap.get("ilplogmaximumfractiondigits");
				int d;

				try {
					d = Integer.parseInt(s);
					ILP.maximumFractionDigits = d;

				}
				catch (Exception e) {

					Error.addConfigError("IlpLogMaximumFractionDigits not recognized: " + s);
					Error.writeErrorLog();
				}
			}
			
			if (configMap.keySet().contains("ilplogformat")) {

				String s = configMap.get("ilplogformat");

				if (s.toLowerCase().contains("cplexlp")) {
					ILP.loggingCplexLp = true;
					System.out.println("IlpLogFormat:           " + "CplexLp");
				} 
				if (s.toLowerCase().contains("mpmodel")) {
					ILP.loggingMPModel = true;
					System.out.println("IlpLogFormat:           " + "MPModel");
				} 
				if (s.toLowerCase().contains("ampl")) {
					ILP.loggingAmpl = true;
					System.out.println("IlpLogFormat:           " + "Ampl");
				} 
				if (s.toLowerCase().contains("lpsolve")) {
					ILP.loggingLpSolve = true;
					System.out.println("IlpLogFormat:           " + "LpSolve");
				} 
			}

			System.out.println("IlpLog:                 " + ILP.logging);
			System.out.println("IlpLogAllCycles:        " + ILP.loggingAllCycles);
			System.out.println("IlpLogVarValue:         " + ILP.loggingVariableValue);
			System.out.println("IlpLogMaximumFractionDigits: " + ILP.maximumFractionDigits);

		}
		
		String strIlpLogUsageMemory = configMap.get("ilplogusagememory");
		if (strIlpLogUsageMemory.equalsIgnoreCase("yes") || strIlpLogUsageMemory.equalsIgnoreCase("true")) {
			ILP.loggingUsageMemeory = true;		
		}else{
			ILP.loggingUsageMemeory = false;
		}
		System.out.println("IlpLogUsageMemory:      " + ILP.loggingUsageMemeory);
		
		String s = configMap.get("wreslplus");
		
		if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
			StudyUtils.useWreslPlus = true;	
		} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
			StudyUtils.useWreslPlus = false;	
		} else {
			StudyUtils.useWreslPlus  = false;	
		}
		System.out.println("WreslPlus:         " + StudyUtils.useWreslPlus);
		
		String enableProgressLog = configMap.get("enableprogresslog");
		
		if (enableProgressLog.equalsIgnoreCase("yes") || enableProgressLog.equalsIgnoreCase("true")){
			ControlData.enableProgressLog = true;
		} 		
		System.out.println("enableProgressLog:         " + ControlData.enableProgressLog);
		
		String allowSvTsInit = configMap.get("allowsvtsinit");
		if (allowSvTsInit.equalsIgnoreCase("yes") || allowSvTsInit.equalsIgnoreCase("true")){
			ControlData.allowSvTsInit = true;	
		} else {
			ControlData.allowSvTsInit  = false;	
		}
		System.out.println("AllowSvTsInit:    " + ControlData.allowSvTsInit);
		
		String allRestartFiles = configMap.get("allrestartfiles");
		if (allRestartFiles.equalsIgnoreCase("yes") || allRestartFiles.equalsIgnoreCase("true")){
			ControlData.allRestartFiles = true;	
		} else {
			ControlData.allRestartFiles  = false;	
		}
		System.out.println("AllRestartFiles:    " + ControlData.allRestartFiles);
		
		ControlData.numberRestartFiles = Integer.parseInt(configMap.get("numberrestartfiles"));
		System.out.println("NumberRestartFiles:    " + ControlData.numberRestartFiles);
		
		ControlData.vHecLib = Integer.parseInt(configMap.get("versionhecdssoutput"));
		System.out.println("VersionHecDssOutput:    " + ControlData.vHecLib);

		ControlData.databaseURL = configMap.get("databaseurl");
		ControlData.sqlGroup = configMap.get("sqlgroup");
		ControlData.ovOption = Integer.parseInt(configMap.get("ovoption"));
		ControlData.ovFile = configMap.get("ovfile");
		System.out.println("ovOption:    " + ControlData.ovOption);
		
		String outputCycleDataToDss = configMap.get("outputcycledatatodss");
		if (outputCycleDataToDss.equalsIgnoreCase("yes") || outputCycleDataToDss.equalsIgnoreCase("true")){
			ControlData.isOutputCycle=true;
		}else{
			ControlData.isOutputCycle=false;
		}
		System.out.println("OutputCycleDataToDSS:    " + ControlData.isOutputCycle);
		
		String outputAllCycleData = configMap.get("outputallcycledata");
		if (outputAllCycleData.equalsIgnoreCase("yes") || outputAllCycleData.equalsIgnoreCase("true")){
			ControlData.outputAllCycles=true;
		}else{
			ControlData.outputAllCycles=false;
		}
		System.out.println("OutputAllCycleData:    " + ControlData.outputAllCycles);
		
		ControlData.selectedCycleOutput = configMap.get("selectedcycleoutput");
		System.out.println("SelectedOutputCycles:    " + ControlData.selectedCycleOutput);
		
		String showRunTimeMessage = configMap.get("showruntimemessage");
		if (showRunTimeMessage.equalsIgnoreCase("yes") || showRunTimeMessage.equalsIgnoreCase("true")){
			ControlData.showRunTimeMessage=true;
		}else{
			ControlData.showRunTimeMessage=false;
		}
		System.out.println("ShowRunTimeMessage:    " + ControlData.showRunTimeMessage);
		
		String printGWFuncCalls = configMap.get("printgwfunccalls");
		if (printGWFuncCalls.equalsIgnoreCase("yes") || printGWFuncCalls.equalsIgnoreCase("true")){
			ControlData.printGWFuncCalls=true;
		}else{
			ControlData.printGWFuncCalls=false;
		}
		System.out.println("PrintGWFuncCalls:    " + ControlData.printGWFuncCalls);
		
		k = "NameSorting"; //default is false
		ControlData.isNameSorting = readBoolean(configMap, k, false);
		System.out.println(k+": "+ControlData.isNameSorting);
		
		k = "YearOutputSection";
		ControlData.yearOutputSection = (int)Math.round(readDouble(configMap, k, -1));
		System.out.println(k+": "+ControlData.yearOutputSection);
		
		k = "MonthMemorySection";
		ControlData.monMemSection = (int)Math.round(readDouble(configMap, k, -1));
		System.out.println(k+": "+ControlData.monMemSection);
		
		String unchangeGWRestart = configMap.get("unchangegwrestart");
		if (unchangeGWRestart.equalsIgnoreCase("yes") || unchangeGWRestart.equalsIgnoreCase("true")){
			ControlData.unchangeGWRestart=true;
		}else{
			ControlData.unchangeGWRestart=false;
		}
		System.out.println("KeepGWRestartatStartDate:    " + ControlData.unchangeGWRestart);
		
		String genSVCatalog = configMap.get("gensvcatalog");
		if (genSVCatalog.equalsIgnoreCase("yes") || genSVCatalog.equalsIgnoreCase("true")){
			ControlData.genSVCatalog=true;
		}else{
			ControlData.genSVCatalog=false;
		}
		System.out.println("GenSVCatalog:                " + ControlData.genSVCatalog);
		
		//if (Error.getTotalError()<1) readParameter(configFile);
		
//		if (Error.getTotalError()<1 && paramMap.size()>0) { 
//			System.out.println("--------------------------------------------");
//			for (String k: paramMap.keySet()){	
//				ParamTemp pt = paramMap.get(k);
//				System.out.println("Parameter::   "+k+": "+pt.expression );
//			}
//		}
		
		
		
		
//		if (configMap.keySet().contains("lpsolveparamheader")){
//			
//			String s = configMap.get("lpsolveparamheader");
//			
//			String delimiter = "[\\+]";  
//			String [] pheaders =  s.split(delimiter);
//			
//			//LPSolveSolver.paramHeader = new ArrayList<String>();
//			
//			System.out.println( "s:"+s);
//			
//			for (int i=1;i<=pheaders.length;i++){
//				System.out.println( pheaders[i-1]);
//			}
//			
//			for (int i=1;i<=pheaders.length;i++){
//				
//				String h = pheaders[i-1];		
//				
//				LPSolveSolver.paramHeader.add(h);
//				
//				//System.out.println("LpSolveParamFile: "+pf);
//				switch (i){
//					case 1 : System.out.println("default LpSolveParamHeader: "+h); break;
//					case 2 : System.out.println("2nd try LpSolveParamHeader: "+h); break;
//					case 3 : System.out.println("3rd try LpSolveParamHeader: "+h); break;
//					default : System.out.println( i+"th try LpSolveParamHeader: "+h); break;
//				}
//				
//			}
//			
//
//		}
		//System.out.println("Ignored... SetAmplOption:   option presolve_eps 1e-13;");
		
		
		
		// System.out.println("gw: "+FilePaths.groundwaterDir);
		// System.out.println("svd: "+FilePaths.svarDssDirectory);
		// System.out.println("sv: "+FilePaths.svarDssFile);
		// System.out.println("initD: "+FilePaths.initDssDirectory);
		// System.out.println("init: "+FilePaths.initDssFile);
		//
		//
		// System.out.println(FilePaths.mainDirectory);
		// System.out.println(FilePaths.groundwaterDir);
		// System.out.println(FilePaths.csvFolderName);
		//
		// System.out.println(ControlData.svDvPartF);
		// System.out.println(ControlData.initPartF);
		// System.out.println(ControlData.partA);
		// System.out.println(ControlData.partE);
		// System.out.println(ControlData.timeStep);
		//
		// System.out.println(ControlData.startYear);
		// System.out.println(ControlData.startMonth);
		// System.out.println(ControlData.startDay);
		//
		// System.out.println(ControlData.endYear);
		// System.out.println(ControlData.endMonth);
		// System.out.println(ControlData.endDay);
		//
		// System.out.println(ControlData.solverName);
		// System.out.println(ControlData.currYear);
		// System.out.println(ControlData.currMonth);
		// System.out.println(ControlData.currDay);
		// System.out.println(ControlData.writeDssStartYear);
		// System.out.println(ControlData.writeDssStartMonth);
		// System.out.println(ControlData.writeDssStartDay);
		//
		// System.out.println(ControlData.totalTimeStep);
		// System.exit(0);

	}

	private static Map<String, String> checkConfigFile(String configFilePath) {

		final File configFile = new File(configFilePath);

		
		try {
			StudyUtils.configFileCanonicalPath = configFile.getCanonicalPath();
			StudyUtils.configDir = configFile.getParentFile().getCanonicalPath();
		} catch (Exception e) {

			Error.addConfigError("Config file not found: " + configFilePath);

		} finally {
			if (!configFile.exists()){

				Error.addConfigError("Config file not found: " + configFilePath);
				
			}			
		}		

		Map<String, String> configMap = setDefaultConfig();

		try {

			Scanner scanner = new Scanner(configFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				line = line.trim();
				line = line.replace('\t', ' ');
				//System.out.println(line);
				if (line.indexOf("#") > -1) {
					line = line.substring(0, line.indexOf("#"));
					line = line.trim();
				}
				if (line.indexOf(" ") < 0) continue;
				if (line.lastIndexOf(" ") + 1 >= line.length()) continue;
				if (line.length() < 5) continue;
				
				//System.out.println(line);

				String key = line.substring(0, line.indexOf(" "));

				String value = line.substring(key.length(), line.length());

				value = value.trim();
				value = value + " ";
				if (value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
					value = value.replace("\"", "");
				}
				else {
					value = value.substring(0, value.indexOf(" "));
					value = value.replace("\"", "");
				}
				
				// break at the line "End Config"
				if (key.equalsIgnoreCase("end") & value.equalsIgnoreCase("config") ) break;
			
				configMap.put(key.toLowerCase(), value);
			}

		}
		catch (Exception e) {

			Error.addConfigError("Invalid Config File: " + configFilePath);

		}

		
		// check missing fields
		String[] requiredFields = { "MainFile", "Solver", "DvarFile", "SvarFile", "SvarAPart",
				"SvarFPart", "InitFile", "InitFPart", "TimeStep", "StartYear", "StartMonth" };

		for (String k : requiredFields) {
			if (!configMap.keySet().contains(k.toLowerCase())) {

				Error.addConfigError("Config file missing field: " + k);
				Error.writeErrorLog();

			}
		}	
		
		// convert number of steps to end date
		int bYr= Integer.parseInt(configMap.get("startyear"));
		int bMon= Integer.parseInt(configMap.get("startmonth"));
		
		if (configMap.keySet().contains("numberofsteps")){
			
			int nsteps = Integer.parseInt(configMap.get("numberofsteps"));
			
			int iBegin = bYr*12 + bMon;
			int iEnd = iBegin + nsteps -1 ; 
					
			int eYr = iEnd/12;
			int eMon = iEnd%12;
			
			configMap.put("stopyear", Integer.toString(eYr));
			configMap.put("stopmonth", Integer.toString(eMon));
			
		} else {
			// check missing fields
			String[] endDateFields = {"StopYear", "StopMonth"};

			for (String k : endDateFields) {
				if (!configMap.keySet().contains(k.toLowerCase())) {

					Error.addConfigError("Config file missing field: " + k);
					Error.writeErrorLog();

				}
			}			
		}
		
		// if start day and end day are not specified, fill-in start day and end day
		
		if (!configMap.containsKey("startday")) {
			configMap.put("startday", "1");
		}
		
		int endYr= Integer.parseInt(configMap.get("stopyear"));
		int endMon= Integer.parseInt(configMap.get("stopmonth"));
		int endday= TimeOperation.numberOfDays(endMon, endYr);
		
		if (!configMap.containsKey("stopday")) {
			configMap.put("stopday", Integer.toString(endday));	
		}
		
		ControlData.defaultTimeStep=configMap.get("timestep").toUpperCase();
		
		// TODO: duplcate codes. clean it up!
		
		File mf = null;
		String mainfile = configMap.get("mainfile");
		
		if (mainfile.contains(":")){
			mf = new File(mainfile);
		} else { 
			mf = new File(StudyUtils.configDir, mainfile);
		}
		
		
		try {
			mf.getCanonicalPath();
		}
		catch (IOException e) {

			Error.addConfigError("Main file not found: "+mf.getAbsolutePath());
			Error.writeErrorLog();
	
		}
		
		return configMap;

	}

	private static Map<String, String> setDefaultConfig() {

		Map<String, String> configMap = new HashMap<String, String>();

		configMap.put("saveparserdata".toLowerCase(), "No");
		configMap.put("solver".toLowerCase(), "XA");
		configMap.put("timestep".toLowerCase(), "1MON");
		configMap.put("showwresllog".toLowerCase(), "yes");
		configMap.put("groundwaterdir".toLowerCase(), "");
		configMap.put("lookupsubdir".toLowerCase(), "");
		configMap.put("IlpLog".toLowerCase(), "no");
		configMap.put("IlpLogVarValue".toLowerCase(), "no");
		configMap.put("IlpLogUsageMemory".toLowerCase(), "no");
		configMap.put("WreslPlus".toLowerCase(), "no");
		configMap.put("AllowSvTsInit".toLowerCase(), "no");
		configMap.put("DatabaseURL".toLowerCase(), "none");
		configMap.put("SQLGroup".toLowerCase(), "calsim");
		configMap.put("ovOption".toLowerCase(), "0");
		configMap.put("enableProgressLog".toLowerCase(), "No");
		configMap.put("OutputCycleDataToDss".toLowerCase(), "No");
		configMap.put("outputAllCycleData".toLowerCase(), "yes");
		configMap.put("SelectedCycleOutput".toLowerCase(), "\'\'");
		configMap.put("ShowRunTimeMessage".toLowerCase(), "No");
		configMap.put("svarfile2".toLowerCase(), "");
		configMap.put("AllRestartFiles".toLowerCase(), "No");
		configMap.put("NumberRestartFiles".toLowerCase(), "12");
		configMap.put("PrintGWfuncCalls".toLowerCase(), "No");
		configMap.put("NameSorting".toLowerCase(), "No");
		configMap.put("YearOutputSection".toLowerCase(), "-1");
		configMap.put("MonthMemorySection".toLowerCase(), "-1");
		configMap.put("unchangeGWRestart".toLowerCase(), "no");
		configMap.put("GenSVCatalog".toLowerCase(), "yes");
		configMap.put("vHecLib".toLowerCase(), "6");
		return configMap;

	}

	public static void readParameter(String configFilePath) {
	
		final File configFile = new File(configFilePath);	
	
		boolean isParameter = false;
		
		paramMap = new LinkedHashMap<String, ParamTemp>();
	
		try {
	
			Scanner scanner = new Scanner(configFile);
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
	
				line = line.trim();
				line = line.replace('\t', ' ');
				//System.out.println(line);
				if (line.indexOf("#") > -1) {
					line = line.substring(0, line.indexOf("#"));
					line = line.trim();
				}
				if (line.indexOf(" ") < 0) continue;
				if (line.lastIndexOf(" ") + 1 >= line.length()) continue;
				if (line.length() < 5) continue;
				
				//System.out.println(line);
	
				String key = line.substring(0, line.indexOf(" "));
	
				String value = line.substring(key.length(), line.length());
	
				value = value.trim();
				value = value + " ";
				if (value.startsWith("\"")) {
					value = value.substring(1, value.lastIndexOf("\""));
					value = value.replace("\"", "");
				}
				else {
					value = value.substring(0, value.indexOf(" "));
					value = value.replace("\"", "");
				}
				
				// break at the line "End Parameter"
				if (key.equalsIgnoreCase("end") & value.equalsIgnoreCase("initial") ) break;
			
				if (key.equalsIgnoreCase("begin") & value.equalsIgnoreCase("initial") ) {
					isParameter = true;
					System.out.println("--------------------------------------------");
					continue;
				}
				
				if (isParameter) {
					
					if (paramMap.keySet().contains(key.toLowerCase())) {
						
						Error.addConfigError("Initial variable ["+key+"] is redefined");
						
					}
					
					ParamTemp pt = new ParamTemp();
					pt.id = key;
					pt.expression = value.toLowerCase();
					
					System.out.println("Initial variable::  "+pt.id.toLowerCase()+": "+pt.expression );
					
					try {
						//pt.dependants = checkExpression(pt.expression);
						Float.parseFloat(pt.expression);
					} catch (Exception e) {
						Error.addConfigError("Initial variable ["+key+"] in Config file must be assigned with a number, but it's ["+pt.expression+"]");
					}
					
					paramMap.put(key.toLowerCase(), pt);
				}
			}
	
		}
		catch (Exception e) {
	
			Error.addConfigError("Exception in parsing [Initial] section: " + configFilePath);
	
		}
	
		//return paramMap;
	
	}

	private static Set<String> checkExpression(String text) throws RecognitionException {
			
			WreslPlusParser parser = ParserUtils.initParserSimple(text);
			
			parser.expression_simple();
			
			return parser.dependants;
		
	}
	
	public static boolean readBoolean(Map<String, String> cM, String name, boolean defaultV){
		
		String l = name.toLowerCase();
		if (cM.keySet().contains(l)) {		
			String s = cM.get(l);
			if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")){
				return true;	
			} else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")){
				return false;	
			}
		}		
		return defaultV;
	}
	
	public static double readDouble(Map<String, String> cM, String name, double defaultV){
		
		double returnV = defaultV;
		String l = name.toLowerCase();
		if (cM.keySet().contains(l)){
			String s = cM.get(l);			
			try {
				returnV = Double.parseDouble(s);
			} catch (NumberFormatException e) {
				System.out.println(name+": Error reading config value");
			}
		}	
		return returnV;
	}

	// private static void loadConfig2(String loadFile) throws IOException {
	//
	// try {
	// XmlDocument doc = XmlDocument.createXmlDocument(
	// new FileInputStream(loadFile), false);
	// fromXml(doc.getDocumentElement());
	// } catch (Exception e) {
	// e.printStackTrace();
	// throw new IOException("Invalid config file: " + loadFile);
	// }
	//
	// }

	// private static void fromXml(Element se) {
	//
	// _parserdataversion = se.getAttribute("parserdataversion");
	// _groundwaterfolder = se.getAttribute("groundwaterfolder");
	// _wreslfile = se.getAttribute("wreslfile");
	// _svfile = se.getAttribute("svfile");
	// _dvfile = se.getAttribute("dvfile");
	// _initfile = se.getAttribute("initfile");
	//
	// _svfileapart = se.getAttribute("svfileapart");
	// _svfilefpart = se.getAttribute("svfilefpart");
	// _initfilefpart = se.getAttribute("initfilefpart");
	//
	// String startMonth = se.getAttribute("startmo");
	// String startYear = se.getAttribute("startyr");
	// String stopMonth = se.getAttribute("stopmo");
	// String stopYear = se.getAttribute("stopyr");
	//
	// try {
	//
	// _startmo = TimeOperation.monthValue(startMonth.toLowerCase());
	// _startyr = Integer.parseInt(startYear);
	// _stopmo = TimeOperation.monthValue(stopMonth.toLowerCase());
	// _stopyr = Integer.parseInt(stopYear);
	//
	//
	// } catch (Exception e) {
	// System.out.println(e.getMessage());
	// }
	//
	//
	// FilePaths.setMainFilePaths(_wreslfile);
	// FilePaths.groundwaterDir= _groundwaterfolder;
	// FilePaths.setSvarDssPaths(_svfile);
	// FilePaths.setInitDssPaths(_initfile);
	// FilePaths.setDvarDssPaths(_dvfile);
	// FilePaths.csvFolderName = "";
	//
	// ControlData.svDvPartF = _svfilefpart;
	// ControlData.initPartF = _initfilefpart;
	// ControlData.partA = _svfileapart;
	// ControlData.partE = _ts;
	// ControlData.timeStep = _ts;
	//
	// ControlData.startYear = _startyr;
	// ControlData.startMonth = _startmo;
	// ControlData.startDay = TimeOperation.numberOfDays(_startmo, _startyr);
	//
	// ControlData.endYear = _stopyr;
	// ControlData.endMonth = _stopmo;
	// ControlData.endDay = TimeOperation.numberOfDays(_stopmo, _stopyr);
	//
	// ControlData.solverName= _solver;
	// ControlData.currYear=ControlData.startYear;
	// ControlData.currMonth=ControlData.startMonth;
	// ControlData.currDay=ControlData.startDay;
	// ControlData.writeDssStartYear=ControlData.startYear;
	// ControlData.writeDssStartMonth=ControlData.startMonth;
	// ControlData.writeDssStartDay=ControlData.startDay;
	//
	// ControlData.totalTimeStep = ControllerBatch.getTotalTimeStep();
	// }

}
