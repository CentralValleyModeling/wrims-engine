package wrimsv2.config;

import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.FilenameUtils;
import org.coinor.cbc.jCbc;
import wrimsv2.components.BuildProps;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.TimeOperation;
import wrimsv2.ilp.ILP;
import wrimsv2.logging.WrimsLogger;
import wrimsv2.solver.CbcSolver;
import wrimsv2.solver.LPSolveSolver;
import wrimsv2.wreslparser.elements.StudyUtils;
import wrimsv2.wreslplus.elements.ParamTemp;
import wrimsv2.wreslplus.elements.ParserUtils;
import wrimsv2.wreslplus.grammar.WreslPlusParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigUtils {

    public static LinkedHashMap<String, ParamTemp> paramMap = new LinkedHashMap<String, ParamTemp>();
    public static Map<String, String> configMap = new HashMap<String, String>();
    private static Map<String, String> argsMap;
    private static final Logger logger = WrimsLogger.getLogger(ConfigUtils.class.getName());

    public static void loadArgs(String[] args) {
        // for Error.log header
        ControlData.currEvalTypeIndex = 8;

        // print version number then exit
        if (args.length == 1 && args[0].equalsIgnoreCase("-version")) {
            logger.info("WRIMS " + new BuildProps().getVN());
            System.exit(0);
        }

        argsMap = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            try {
                String key = args[i].substring(0, args[i].indexOf("="));
                String val = args[i].substring(args[i].indexOf("=") + 1);
                val = val.replaceAll("\"", "");
                argsMap.put(key.toLowerCase(), val);
            } catch (Exception e) {
                logger.info("Example: ");
                logger.info("-config=\"D:\\test\\example.config\"");
                System.exit(1);
            }
        }

        // load config file
        if (argsMap.containsKey("-config")) {
            String configFilePath = FilenameUtils.removeExtension(argsMap.get("-config")) + ".config";
            argsMap.put("-config", configFilePath);
            logger.info("\nWARNING!! Config file and RUN directory must be placed in the same folder! \n");
            logger.info("Loading config file: " + argsMap.get("-config"));
            StudyUtils.configFilePath = argsMap.get("-config");
            loadConfig(StudyUtils.configFilePath);
            if (Error.getTotalError() > 0) {
                Error.writeErrorLog();
            }
            // compile to serial object named *.par
        } else if (argsMap.containsKey("-mainwresl")) {
            logger.info("Compiling main wresl file: " + argsMap.get("-mainwresl"));
            StudyUtils.compileOnly = true;
            FilePaths.setMainFilePaths(argsMap.get("-mainwresl"));
        } else if (argsMap.containsKey("-mainwreslplus")) {
            logger.info("Compiling main wresl file: " + argsMap.get("-mainwreslplus"));
            StudyUtils.compileOnly = true;
            StudyUtils.useWreslPlus = true;
            FilePaths.setMainFilePaths(argsMap.get("-mainwreslplus"));
        } else {
            logger.info("Couldn't interpret configuration given:");
            logger.log(Level.INFO, () -> configMap.toString());  // don't execute if logging level is lower than INFO
            logger.info("Example of how to specify a configuration: ");
            logger.info("-config=\"D:\\test\\example.config\"");
            System.exit(1);
        }
    }

    private static void loadConfig(String configFile) {
        //StudyUtils.config_errors = 0; // reset
        String k = null;
        configMap = new HashMap<String, String>();
        configMap = checkConfigFile(configFile);

        String mainfile = configMap.get("mainfile").toLowerCase();
        String mainFilePath = "";
        if (mainfile.contains(":")) {
            mainFilePath = new File(mainfile).getAbsolutePath();
        } else {
            mainFilePath = new File(StudyUtils.configDir, mainfile).getAbsolutePath();
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
        } else if (mainfile.endsWith(".wresl")) {
            FilePaths.setMainFilePaths(mainFilePath);
        } else {
            Error.addConfigError("Invalid main file extension: " + configMap.get("mainfile"));
            Error.writeErrorLog();
        }

        // FilePaths.mainDirectory = configMap.get("maindir");
        logger.info("MainFile:       " + FilePaths.fullMainPath);

        // CbcLibName // default is jCbc and it's not used for version selection
        k = "cbclibname";
        if (configMap.containsKey(k)) {
            CbcSolver.cbcLibName = configMap.get(k);
        }
        // need to know jCbc version to determine solving options
        System.loadLibrary(CbcSolver.cbcLibName);
        logger.info("CbcLibName: " + CbcSolver.cbcLibName);

        try {
            if (configMap.get("groundwaterdir").length() > 0) {
                if (configMap.get("groundwaterdir").contains(":")) {
                    FilePaths.groundwaterDir = new File(configMap.get("groundwaterdir")).getCanonicalPath() + File.separator;
                } else {
                    FilePaths.groundwaterDir = new File(StudyUtils.configDir, configMap.get("groundwaterdir")).getCanonicalPath() + File.separator;
                }
                logger.info("GroundWaterDir: " + FilePaths.groundwaterDir);
            } else {
                FilePaths.groundwaterDir = "None";
            }

            if (configMap.get("svarfile").contains(":")) {
                FilePaths.setSvarFilePaths(new File(configMap.get("svarfile")).getCanonicalPath());
            } else {
                FilePaths.setSvarFilePaths(new File(StudyUtils.configDir, configMap.get("svarfile")).getCanonicalPath());
            }
            logger.info("SvarFile:       " + FilePaths.fullSvarFilePath);

            if (configMap.get("svarfile2").equals("")) {
                FilePaths.setSvarFile2Paths("");
            } else {
                if (configMap.get("svarfile2").contains(":")) {
                    FilePaths.setSvarFile2Paths(new File(configMap.get("svarfile2")).getCanonicalPath());
                } else {
                    FilePaths.setSvarFile2Paths(new File(StudyUtils.configDir, configMap.get("svarfile2")).getCanonicalPath());
                }
                logger.info("SvarFile2:       " + FilePaths.fullSvarFile2Path);
            }

            if (configMap.get("initfile").contains(":")) {
                FilePaths.setInitFilePaths(new File(configMap.get("initfile")).getCanonicalPath());
            } else {
                FilePaths.setInitFilePaths(new File(StudyUtils.configDir, configMap.get("initfile")).getCanonicalPath());
            }
            logger.info("InitFile:       " + FilePaths.fullInitFilePath);

            if (configMap.get("dvarfile").contains(":")) {
                FilePaths.setDvarFilePaths(new File(configMap.get("dvarfile")).getCanonicalPath());
            } else {
                FilePaths.setDvarFilePaths(new File(StudyUtils.configDir, configMap.get("dvarfile")).getCanonicalPath());
            }
            logger.info("DvarFile:       " + FilePaths.fullDvarDssPath);

        } catch (IOException e) {
            Error.addConfigError("Invalid file path in config file");
            Error.writeErrorLog();
            e.printStackTrace();
        }

        StudyUtils.configFileName = new File(configFile).getName();

        FilePaths.csvFolderName = "";

        if (configMap.get("showwresllog").equalsIgnoreCase("no") || configMap.get("showwresllog").equalsIgnoreCase("false")) {
            ControlData.showWreslLog = false;
        }
        if (configMap.get("lookupsubdir").length() > 0) {
            FilePaths.lookupSubDirectory = configMap.get("lookupsubdir");
            logger.info("LookupSubDir:   " + FilePaths.lookupSubDirectory);
        }

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

        logger.info("TimeStep:       " + ControlData.timeStep);
        logger.info("SvarAPart:      " + ControlData.partA);
        logger.info("SvarFPart:      " + ControlData.svDvPartF);
        logger.info("InitFPart:      " + ControlData.initPartF);
        logger.info("StartYear:      " + ControlData.startYear);
        logger.info("StartMonth:     " + ControlData.startMonth);
        logger.info("StartDay:       " + ControlData.startDay);
        logger.info("StopYear:       " + ControlData.endYear);
        logger.info("StopMonth:      " + ControlData.endMonth);
        logger.info("StopDay:        " + ControlData.endDay);
        logger.info("Solver:         " + ControlData.solverName);

        final String[] solvers = {"xa", "xalog", "clp0", "clp1", "clp", "lpsolve", "gurobi", "cbc0", "cbc1", "cbc"};

        if (!Arrays.asList(solvers).contains(ControlData.solverName.toLowerCase())) {
            Error.addConfigError("Solver name not recognized: " + ControlData.solverName);
            Error.writeErrorLog();
        } else if (ControlData.solverName.toLowerCase().contains("cbc")) {
            CbcSolver.cbcVersion = jCbc.getVersion();
            logger.info("CbcVersion: " + CbcSolver.cbcVersion);
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
            logger.info("Cbc2021: " + CbcSolver.usejCbc2021);
            logger.info("Cbc2021a: " + CbcSolver.usejCbc2021a);
        }

        // SendAliasToDvar default is false
        if (configMap.containsKey("sendaliastodvar")) {
            String s = configMap.get("sendaliastodvar");
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                ControlData.sendAliasToDvar = true;
            } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                ControlData.sendAliasToDvar = false;
            } else {
                ControlData.sendAliasToDvar = false;
            }
        }
        logger.info("SendAliasToDvar: " + ControlData.sendAliasToDvar);

        // PrefixInitToDvarFile
        if (configMap.containsKey("prefixinittodvarfile")) {
            String s = configMap.get("prefixinittodvarfile");
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                ControlData.writeInitToDVOutput = true;
            } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                ControlData.writeInitToDVOutput = false;
            } else {
                ControlData.writeInitToDVOutput = false;
            }
        } else {
            ControlData.writeInitToDVOutput = true;
        }
        logger.info("PrefixInitToDvarFile: " + ControlData.writeInitToDVOutput);

        // SolveCompare
        k = "solvecompare";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("xa_cbc")) {
                ControlData.cbc_debug_routeXA = false;
                ControlData.cbc_debug_routeCbc = true;
            } else if (s.equalsIgnoreCase("cbc_xa")) {
                ControlData.cbc_debug_routeXA = true;
                ControlData.cbc_debug_routeCbc = false;
            } else {
                ControlData.cbc_debug_routeXA = false;
                ControlData.cbc_debug_routeCbc = false;
            }
        } else {
            ControlData.cbc_debug_routeXA = false;
            ControlData.cbc_debug_routeCbc = false;
        }
        logger.info("SolveCompare (use XA solution as base): " + ControlData.cbc_debug_routeXA);
        logger.info("SolveCompare (use Cbc solution as base): " + ControlData.cbc_debug_routeCbc);

        // watch variable
        if (configMap.containsKey("watch")) {
            String s = configMap.get("watch").toLowerCase();
            ControlData.watchList = s.split(",");
            logger.info("Watch: " + Arrays.toString(ControlData.watchList));
        }

        // watch variable tolerance
        k = "watcht";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                ControlData.watchList_tolerance = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("watchT: Error reading config value");
            }
        }
        logger.info("watchT: " + ControlData.watchList_tolerance);

        // CbcDebugObjDiff // default is false
        k = "cbcdebugobjdiff";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            CbcSolver.debugObjDiff = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
        } else {
            CbcSolver.debugObjDiff = false;
        }
        logger.info("CbcDebugObjDiff: " + CbcSolver.debugObjDiff);

        // CbcObjLog // default is true
        k = "cbcobjlog";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            CbcSolver.logObj = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
        } else {
            CbcSolver.logObj = true;
        }
        logger.info("CbcObjLog: " + CbcSolver.logObj);

        // CbcLogNativeLp // default is false
        k = "cbclognativelp";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            ControlData.cbcLogNativeLp = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
        } else {
            ControlData.cbcLogNativeLp = false;
        }
        logger.info("CbcLogNativeLp: " + ControlData.cbcLogNativeLp);

        // CbcWarmStart // default is false
        k = "cbcwarmstart";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                ControlData.useCbcWarmStart = true;
            } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                ControlData.useCbcWarmStart = false;
            }
        }
        // warmstart is true if cbc2021 is true
        if (CbcSolver.usejCbc2021) {
            ControlData.useCbcWarmStart = true;
        }
        logger.info("CbcWarmStart: " + ControlData.useCbcWarmStart);

        // CbcViolationCheck  default {cbc2021:false, otherwise:true}
        k = "cbcviolationcheck";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                CbcSolver.cbcViolationCheck = true;
            } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                CbcSolver.cbcViolationCheck = false;
            }
        }
        logger.info("CbcViolationCheck: " + CbcSolver.cbcViolationCheck);


        // CbcSolveFunction // default is SolveFull
        k = "cbcsolvefunction";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("solveu")) {
                CbcSolver.solvFunc = CbcSolver.solvU;
            } else if (s.equalsIgnoreCase("solve2")) {
                CbcSolver.solvFunc = CbcSolver.solv2;
            } else if (s.equalsIgnoreCase("solve3")) {
                CbcSolver.solvFunc = CbcSolver.solv3;
            } else if (s.equalsIgnoreCase("callCbc")) {
                CbcSolver.solvFunc = CbcSolver.solvCallCbc;
            } else {
                CbcSolver.solvFunc = CbcSolver.solvFull;
            }
        } else {
            CbcSolver.solvFunc = CbcSolver.solvFull;
        }
        logger.info("CbcSolveFunction: " + CbcSolver.solvFunc);

        // CbcToleranceZero // default is 1e-11 ControlData.zeroTolerance
        k = "cbctolerancezero";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                ControlData.zeroTolerance = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcToleranceZero: Error reading config value");
            }
        }
        logger.info("CbcToleranceZero: " + ControlData.zeroTolerance);


        // CbcToleranceInteger default 1e-9
        k = "cbctoleranceinteger";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.integerT = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcToleranceInteger: Error reading config value");
            }
        }
        logger.info("CbcToleranceInteger: " + CbcSolver.integerT);

        // CbcLowerBoundZeroCheck default max(solve_2_primalT_relax*10, 1e-6)
        k = "cbclowerboundzerocheck";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.lowerBoundZero_check = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcLowerBoundZeroCheck: Error reading config value");
            }
        }
        logger.info("CbcLowerBoundZeroCheck: " + CbcSolver.lowerBoundZero_check);

        // CbcToleranceIntegerCheck default 1e-8
        k = "cbctoleranceintegercheck";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.integerT_check = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcToleranceIntegercheck: Error reading config value");
            }
        }
        logger.info("CbcToleranceIntegercheck: " + CbcSolver.integerT_check);

        // CbcSolutionRounding  default is true
        k = "cbcsolutionrounding";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                CbcSolver.cbcSolutionRounding = true;
            } else CbcSolver.cbcSolutionRounding = !s.equalsIgnoreCase("no") && !s.equalsIgnoreCase("false");
        } else {
            CbcSolver.cbcSolutionRounding = true;
        }
        logger.info("CbcSolutionRounding: " + CbcSolver.cbcSolutionRounding);

        // CbcViolationRetry  default is true
        k = "cbcviolationretry";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                CbcSolver.cbcViolationRetry = true;
            } else CbcSolver.cbcViolationRetry = !s.equalsIgnoreCase("no") && !s.equalsIgnoreCase("false");
        } else {
            CbcSolver.cbcViolationRetry = true;
        }
        logger.info("CbcViolationRetry: " + CbcSolver.cbcViolationRetry);

        // CbcHintTimeMax // default is 100 sec
        k = "cbchinttimemax";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.cbcHintTimeMax = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.info("CbcHintTimeMax: Error reading config value");
            }
        }
        logger.info("CbcHintTimeMax: " + CbcSolver.cbcHintTimeMax);

        // CbcHintRelaxPenalty // default is 9000
        k = "cbchintrelaxpenalty";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.cbcHintRelaxPenalty = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcHintRelaxPenalty: Error reading config value");
            }
        }
        logger.info("CbcHintRelaxPenalty: " + CbcSolver.cbcHintRelaxPenalty);

        // CbcTolerancePrimal // default is 1e-9
        k = "cbctoleranceprimal";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.solve_2_primalT = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcTolerancePrimal: Error reading config value");
            }
        }
        logger.info("CbcTolerancePrimal: " + CbcSolver.solve_2_primalT);

        // CbcTolerancePrimalRelax // default is 1e-7
        k = "cbctoleranceprimalrelax";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.solve_2_primalT_relax = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcTolerancePrimalRelax: Error reading config value");
            }
        }
        logger.info("CbcTolerancePrimalRelax: " + CbcSolver.solve_2_primalT_relax);
        ControlData.relationTolerance = Math.max(CbcSolver.solve_2_primalT_relax * 10, 1e-6);

        // CbcSolveWhsPrimalT // default is 1e-9
        k = "cbctolerancewarmprimal";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.solve_whs_primalT = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("CbcToleranceWarmPrimal: Error reading config value");
            }
        }
        logger.info("CbcToleranceWarmPrimal: " + CbcSolver.solve_whs_primalT);

        // CbcLogStartDate // default is 999900
        k = "cbclogstartdate";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.cbcLogStartDate = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.info("CbcLogStartDate: Error reading config value");
            }
        }
        logger.info("CbcLogStartDate: " + CbcSolver.cbcLogStartDate);

        // CbcLogStopDate // default is 999900
        k = "cbclogstopdate";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.cbcLogStopDate = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.info("CbcLogStopDate: Error reading config value");
            }
        }
        logger.info("CbcLogStopDate: " + CbcSolver.cbcLogStopDate);

        // LogIfObjDiff
        k = "logifobjdiff";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.log_if_obj_diff = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("LogIfObjDiff: Error reading config value");
            }
        }
        logger.info("LogIfObjDiff: " + CbcSolver.log_if_obj_diff);

        // RecordIfObjDiff
        k = "recordifobjdiff";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            try {
                CbcSolver.record_if_obj_diff = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info("RecordIfObjDiff: Error reading config value");
            }
        }
        logger.info("RecordIfObjDiff: " + CbcSolver.record_if_obj_diff);

        k = "CbcWhsScaling"; //default is true
        CbcSolver.whsScaling = readBoolean(configMap, k, true);
        logger.info(k + ": " + CbcSolver.whsScaling);

        k = "CbcWhsSafe"; //default is false
        CbcSolver.whsSafe = readBoolean(configMap, k, false);
        logger.info(k + ": " + CbcSolver.whsSafe);

        k = "CbcDebugDeviation"; //default is false
        CbcSolver.debugDeviation = readBoolean(configMap, k, false);
        logger.info(k + ": " + CbcSolver.debugDeviation);

        if (CbcSolver.debugDeviation) {
            k = "CbcDebugDeviationMin"; // default is 200
            CbcSolver.debugDeviationMin = readDouble(configMap, k, 200);
            logger.info(k + ": " + CbcSolver.debugDeviationMin);

            k = "CbcDebugDeviationWeightMin"; // default is 5E5
            CbcSolver.debugDeviationWeightMin = readDouble(configMap, k, 5E5);
            logger.info(k + ": " + CbcSolver.debugDeviationWeightMin);

            k = "CbcDebugDeviationWeightMultiply"; // default is 100
            CbcSolver.debugDeviationWeightMultiply = readDouble(configMap, k, 100);
            logger.info(k + ": " + CbcSolver.debugDeviationWeightMultiply);

            k = "CbcDebugDeviationFindMissing"; //default is false
            CbcSolver.debugDeviationFindMissing = readBoolean(configMap, k, false);
            logger.info(k + ": " + CbcSolver.debugDeviationFindMissing);
        }

        // Warm2ndSolveFunction // default is Solve2
        k = "warm2ndsolvefunction";
        if (configMap.containsKey(k)) {
            String s = configMap.get(k);
            if (s.equalsIgnoreCase("solve3")) {
                CbcSolver.warm_2nd_solvFunc = CbcSolver.solv3;
            } else {
                CbcSolver.warm_2nd_solvFunc = CbcSolver.solv2;
            }
        } else {
            CbcSolver.warm_2nd_solvFunc = CbcSolver.solv2;
        }
        logger.info("Warm2ndSolveFunction: " + CbcSolver.warm_2nd_solvFunc);

        // ParserCheckVarUndefined
        if (configMap.containsKey("parsercheckvarundefined")) {
            String s = configMap.get("parsercheckvarundefined");
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                StudyUtils.parserCheckVarUndefined = true;
            } else StudyUtils.parserCheckVarUndefined = !s.equalsIgnoreCase("no") && !s.equalsIgnoreCase("false");
        } else {
            StudyUtils.parserCheckVarUndefined = true;
        }
        logger.info("ParserCheckVarUndefined: " + StudyUtils.parserCheckVarUndefined);

        if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
            // LpSolveConfigFile
            if (configMap.containsKey("lpsolveconfigfile")) {
                String f = configMap.get("lpsolveconfigfile");
                try {
                    File sf = new File(StudyUtils.configDir, f);
                    if (sf.exists()) {
                        LPSolveSolver.configFile = sf.getCanonicalPath();
                    } else {
                        Error.addConfigError("LpSolveConfigFile not found: " + f);
                        Error.writeErrorLog();
                    }
                } catch (Exception e) {
                    Error.addConfigError("LpSolveConfigFile not found: " + f);
                    Error.writeErrorLog();
                    e.printStackTrace();
                }
            } else {
                Error.addConfigError("LpSolveConfigFile not defined. ");
                Error.writeErrorLog();
            }
            logger.info("LpSolveConfigFile:   " + LPSolveSolver.configFile);

            // LpSolveNumberOfRetries default is 0 retry
            if (configMap.containsKey("lpsolvenumberofretries")) {
                String s = configMap.get("lpsolvenumberofretries");
                try {
                    LPSolveSolver.numberOfRetries = Integer.parseInt(s);
                } catch (Exception e) {
                    Error.addConfigError("LpSolveNumberOfRetries not recognized: " + s);
                    Error.writeErrorLog();
                }
            }
            logger.info("LpSolveNumberOfRetries: " + LPSolveSolver.numberOfRetries);
        }

        // processed only for ILP
        // TODO: lpsolve and ilp log is binded. need to enable direct linking instead of reading file
        if (ControlData.solverName.equalsIgnoreCase("XALOG")) {
            configMap.put("ilplog", "yes");
            ILP.loggingCplexLp = true;
        } else if (ControlData.solverName.equalsIgnoreCase("cbc0") || ControlData.solverName.equalsIgnoreCase("cbc1")) {
            configMap.put("ilplog", "yes");
            configMap.put("ilplogallcycles", "yes");
            ILP.loggingCplexLp = true;
        } else if (ControlData.solverName.equalsIgnoreCase("clp0") || ControlData.solverName.equalsIgnoreCase("clp1")) {
            configMap.put("ilplog", "yes");
            configMap.put("ilplogallcycles", "yes");
            ILP.loggingCplexLp = true;
        } else if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
            configMap.put("ilplog", "yes");
            ILP.loggingLpSolve = true;
        } else if (ControlData.solverName.equalsIgnoreCase("Gurobi")) {
            configMap.put("ilplog", "yes");
            ILP.loggingCplexLp = true;
        }

        String strIlpLog = configMap.get("ilplog");
        if (strIlpLog.equalsIgnoreCase("yes") || strIlpLog.equalsIgnoreCase("true")) {
            ILP.logging = true;
            // IlpLogAllCycles
            // default is false
            if (configMap.containsKey("ilplogallcycles")) {
                String s = configMap.get("ilplogallcycles");
                if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                    ILP.loggingAllCycles = true;
                } else ILP.loggingAllCycles = !s.equalsIgnoreCase("no") && !s.equalsIgnoreCase("false");
            }
            // IlpLogVarValue
            // default is false
            if (configMap.containsKey("ilplogvarvalue")) {
                String s = configMap.get("ilplogvarvalue");
                if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                    ILP.loggingVariableValue = true;
                } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                    ILP.loggingVariableValue = false;
                } else {
                    ILP.loggingVariableValue = false;
                }
            }

            // ilplogvarvalueround
            // default is false
            if (configMap.containsKey("ilplogvarvalueround")) {
                String s = configMap.get("ilplogvarvalueround");
                ILP.loggingVariableValueRound = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
            }

            // ilplogvarvalueround10
            // default is false
            if (configMap.containsKey("ilplogvarvalueround10")) {
                String s = configMap.get("ilplogvarvalueround10");
                ILP.loggingVariableValueRound10 = s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true");
            }

            // IlpLogMaximumFractionDigits
            // default is 8
            if (configMap.containsKey("ilplogmaximumfractiondigits")) {
                String s = configMap.get("ilplogmaximumfractiondigits");
                int d;

                try {
                    d = Integer.parseInt(s);
                    ILP.maximumFractionDigits = d;
                } catch (Exception e) {
                    Error.addConfigError("IlpLogMaximumFractionDigits not recognized: " + s);
                    Error.writeErrorLog();
                }
            }

            if (configMap.containsKey("ilplogformat")) {
                String s = configMap.get("ilplogformat");
                if (s.toLowerCase().contains("cplexlp")) {
                    ILP.loggingCplexLp = true;
                    logger.info("IlpLogFormat:           " + "CplexLp");
                }
                if (s.toLowerCase().contains("mpmodel")) {
                    ILP.loggingMPModel = true;
                    logger.info("IlpLogFormat:           " + "MPModel");
                }
                if (s.toLowerCase().contains("ampl")) {
                    ILP.loggingAmpl = true;
                    logger.info("IlpLogFormat:           " + "Ampl");
                }
                if (s.toLowerCase().contains("lpsolve")) {
                    ILP.loggingLpSolve = true;
                    logger.info("IlpLogFormat:           " + "LpSolve");
                }
            }
            logger.info("IlpLog:                 " + ILP.logging);
            logger.info("IlpLogAllCycles:        " + ILP.loggingAllCycles);
            logger.info("IlpLogVarValue:         " + ILP.loggingVariableValue);
            logger.info("IlpLogMaximumFractionDigits: " + ILP.maximumFractionDigits);
        }

        String strIlpLogUsageMemory = configMap.get("ilplogusagememory");
        ILP.loggingUsageMemeory = strIlpLogUsageMemory.equalsIgnoreCase("yes") || strIlpLogUsageMemory.equalsIgnoreCase("true");
        logger.info("IlpLogUsageMemory:      " + ILP.loggingUsageMemeory);

        String s = configMap.get("wreslplus");

        if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
            StudyUtils.useWreslPlus = true;
        } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
            StudyUtils.useWreslPlus = false;
        } else {
            StudyUtils.useWreslPlus = false;
        }
        logger.info("WreslPlus:         " + StudyUtils.useWreslPlus);

        String enableProgressLog = configMap.get("enableprogresslog");

        if (enableProgressLog.equalsIgnoreCase("yes") || enableProgressLog.equalsIgnoreCase("true")) {
            ControlData.enableProgressLog = true;
        }
        logger.info("enableProgressLog:         " + ControlData.enableProgressLog);

        String allowSvTsInit = configMap.get("allowsvtsinit");
        ControlData.allowSvTsInit = allowSvTsInit.equalsIgnoreCase("yes") || allowSvTsInit.equalsIgnoreCase("true");
        logger.info("AllowSvTsInit:    " + ControlData.allowSvTsInit);

        String allRestartFiles = configMap.get("allrestartfiles");
        ControlData.allRestartFiles = allRestartFiles.equalsIgnoreCase("yes") || allRestartFiles.equalsIgnoreCase("true");
        logger.info("AllRestartFiles:    " + ControlData.allRestartFiles);

        ControlData.numberRestartFiles = Integer.parseInt(configMap.get("numberrestartfiles"));
        logger.info("NumberRestartFiles:    " + ControlData.numberRestartFiles);

        ControlData.vHecLib = 6;  // Integer.parseInt(configMap.get("versionhecdssoutput"));
        logger.info("VersionHecDssOutput:    " + ControlData.vHecLib);

        ControlData.databaseURL = configMap.get("databaseurl");
        ControlData.sqlGroup = configMap.get("sqlgroup");
        ControlData.ovOption = Integer.parseInt(configMap.get("ovoption"));
        ControlData.ovFile = configMap.get("ovfile");
        logger.info("ovOption:    " + ControlData.ovOption);

        String outputCycleDataToDss = configMap.get("outputcycledatatodss");
        ControlData.isOutputCycle = outputCycleDataToDss.equalsIgnoreCase("yes") || outputCycleDataToDss.equalsIgnoreCase("true");
        logger.info("OutputCycleDataToDSS:    " + ControlData.isOutputCycle);

        String outputAllCycleData = configMap.get("outputallcycledata");
        ControlData.outputAllCycles = outputAllCycleData.equalsIgnoreCase("yes") || outputAllCycleData.equalsIgnoreCase("true");
        logger.info("OutputAllCycleData:    " + ControlData.outputAllCycles);

        ControlData.selectedCycleOutput = configMap.get("selectedcycleoutput");
        logger.info("SelectedOutputCycles:    " + ControlData.selectedCycleOutput);

        String showRunTimeMessage = configMap.get("showruntimemessage");
        ControlData.showRunTimeMessage = showRunTimeMessage.equalsIgnoreCase("yes") || showRunTimeMessage.equalsIgnoreCase("true");
        logger.info("ShowRunTimeMessage:    " + ControlData.showRunTimeMessage);

        String printGWFuncCalls = configMap.get("printgwfunccalls");
        ControlData.printGWFuncCalls = printGWFuncCalls.equalsIgnoreCase("yes") || printGWFuncCalls.equalsIgnoreCase("true");
        logger.info("PrintGWFuncCalls:    " + ControlData.printGWFuncCalls);

        k = "NameSorting"; //default is false
        ControlData.isNameSorting = readBoolean(configMap, k, false);
        logger.info(k + ": " + ControlData.isNameSorting);

        k = "YearOutputSection";
        ControlData.yearOutputSection = (int) Math.round(readDouble(configMap, k, -1));
        logger.info(k + ": " + ControlData.yearOutputSection);

        k = "MonthMemorySection";
        ControlData.monMemSection = (int) Math.round(readDouble(configMap, k, -1));
        logger.info(k + ": " + ControlData.monMemSection);

        String unchangeGWRestart = configMap.get("unchangegwrestart");
        ControlData.unchangeGWRestart = unchangeGWRestart.equalsIgnoreCase("yes") || unchangeGWRestart.equalsIgnoreCase("true");
        logger.info("KeepGWRestartatStartDate:    " + ControlData.unchangeGWRestart);

        String genSVCatalog = configMap.get("gensvcatalog");
        ControlData.genSVCatalog = genSVCatalog.equalsIgnoreCase("yes") || genSVCatalog.equalsIgnoreCase("true");
        logger.info("GenSVCatalog:                " + ControlData.genSVCatalog);
    }

    private static Map<String, String> checkConfigFile(String configFilePath) {
        final File configFile = new File(configFilePath);

        try {
            StudyUtils.configFileCanonicalPath = configFile.getCanonicalPath();
            StudyUtils.configDir = configFile.getParentFile().getCanonicalPath();
        } catch (Exception e) {
            Error.addConfigError("Config file not found: " + configFilePath);
        } finally {
            if (!configFile.exists()) {
                Error.addConfigError("Config file not found: " + configFilePath);
            }
        }

        Map<String, String> configMap = setDefaultConfig();
        try (Scanner scanner = new Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                line = line.trim();
                line = line.replace('\t', ' ');
                if (line.contains("#")) {
                    line = line.substring(0, line.indexOf("#"));
                    line = line.trim();
                }
                if (!line.contains(" ")) continue;
                if (line.lastIndexOf(" ") + 1 >= line.length()) continue;
                if (line.length() < 5) continue;
                String key = line.substring(0, line.indexOf(" "));
                String value = line.substring(key.length());

                value = value.trim();
                value = value + " ";
                if (value.startsWith("\"")) {
                    value = value.substring(1, value.lastIndexOf("\""));
                    value = value.replace("\"", "");
                } else {
                    value = value.substring(0, value.indexOf(" "));
                    value = value.replace("\"", "");
                }

                // break at the line "End Config"
                if (key.equalsIgnoreCase("end") & value.equalsIgnoreCase("config")) break;
                configMap.put(key.toLowerCase(), value);
            }
        } catch (Exception e) {
            Error.addConfigError("Invalid Config File: " + configFilePath);
        }

        // check missing fields
        String[] requiredFields = {"MainFile", "Solver", "DvarFile", "SvarFile", "SvarAPart", "SvarFPart", "InitFile", "InitFPart", "TimeStep", "StartYear", "StartMonth"};
        for (String k : requiredFields) {
            if (!configMap.containsKey(k.toLowerCase())) {
                Error.addConfigError("Config file missing field: " + k);
                Error.writeErrorLog();
            }
        }

        // convert number of steps to end date
        int bYr = Integer.parseInt(configMap.get("startyear"));
        int bMon = Integer.parseInt(configMap.get("startmonth"));

        if (configMap.containsKey("numberofsteps")) {
            int nsteps = Integer.parseInt(configMap.get("numberofsteps"));

            int iBegin = bYr * 12 + bMon;
            int iEnd = iBegin + nsteps - 1;

            int eYr = iEnd / 12;
            int eMon = iEnd % 12;

            configMap.put("stopyear", Integer.toString(eYr));
            configMap.put("stopmonth", Integer.toString(eMon));
        } else {
            // check missing fields
            String[] endDateFields = {"StopYear", "StopMonth"};
            for (String k : endDateFields) {
                if (!configMap.containsKey(k.toLowerCase())) {
                    Error.addConfigError("Config file missing field: " + k);
                    Error.writeErrorLog();
                }
            }
        }

        // if start day and end day are not specified, fill-in start day and end day
        if (!configMap.containsKey("startday")) {
            configMap.put("startday", "1");
        }

        int endYr = Integer.parseInt(configMap.get("stopyear"));
        int endMon = Integer.parseInt(configMap.get("stopmonth"));
        int endday = TimeOperation.numberOfDays(endMon, endYr);

        if (!configMap.containsKey("stopday")) {
            configMap.put("stopday", Integer.toString(endday));
        }

        ControlData.defaultTimeStep = configMap.get("timestep").toUpperCase();

        // TODO: duplcate codes. clean it up!
        File mf = null;
        String mainfile = configMap.get("mainfile");

        if (mainfile.contains(":")) {
            mf = new File(mainfile);
        } else {
            mf = new File(StudyUtils.configDir, mainfile);
        }

        try {
            mf.getCanonicalPath();
        } catch (IOException e) {
            Error.addConfigError("Main file not found: " + mf.getAbsolutePath());
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
        configMap.put("SelectedCycleOutput".toLowerCase(), "''");
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

        try (Scanner scanner = new Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                line = line.trim();
                line = line.replace('\t', ' ');
                if (line.indexOf("#") > -1) {
                    line = line.substring(0, line.indexOf("#"));
                    line = line.trim();
                }
                if (line.indexOf(" ") < 0) continue;
                if (line.lastIndexOf(" ") + 1 >= line.length()) continue;
                if (line.length() < 5) continue;

                String key = line.substring(0, line.indexOf(" "));
                String value = line.substring(key.length());
                value = value.trim();
                value = value + " ";
                if (value.startsWith("\"")) {
                    value = value.substring(1, value.lastIndexOf("\""));
                    value = value.replace("\"", "");
                } else {
                    value = value.substring(0, value.indexOf(" "));
                    value = value.replace("\"", "");
                }

                // break at the line "End Parameter"
                if (key.equalsIgnoreCase("end") && value.equalsIgnoreCase("initial")) break;

                if (key.equalsIgnoreCase("begin") && value.equalsIgnoreCase("initial")) {
                    isParameter = true;
                    logger.info("--------------------------------------------");
                    continue;
                }

                if (isParameter) {
                    if (paramMap.containsKey(key.toLowerCase())) {
                        Error.addConfigError("Initial variable [" + key + "] is redefined");
                    }

                    ParamTemp pt = new ParamTemp();
                    pt.id = key;
                    pt.expression = value.toLowerCase();

                    logger.info("Initial variable::  " + pt.id.toLowerCase() + ": " + pt.expression);
                    try {
                        Float.parseFloat(pt.expression);
                    } catch (Exception e) {
                        Error.addConfigError("Initial variable [" + key + "] in Config file must be assigned with a number, but it's [" + pt.expression + "]");
                    }

                    paramMap.put(key.toLowerCase(), pt);
                }
            }
        } catch (Exception e) {
            Error.addConfigError("Exception in parsing [Initial] section: " + configFilePath);
        }
    }

    private static Set<String> checkExpression(String text) throws RecognitionException {
        WreslPlusParser parser = ParserUtils.initParserSimple(text);
        parser.expression_simple();
        return parser.dependants;
    }

    public static boolean readBoolean(Map<String, String> cM, String name, boolean defaultV) {
        String l = name.toLowerCase();
        if (cM.containsKey(l)) {
            String s = cM.get(l);
            if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")) {
                return true;
            } else if (s.equalsIgnoreCase("no") || s.equalsIgnoreCase("false")) {
                return false;
            }
        }
        return defaultV;
    }

    public static double readDouble(Map<String, String> cM, String name, double defaultV) {
        double returnV = defaultV;
        String l = name.toLowerCase();
        if (cM.containsKey(l)) {
            String s = cM.get(l);
            try {
                returnV = Double.parseDouble(s);
            } catch (NumberFormatException e) {
                logger.info(name + ": Error reading config value");
            }
        }
        return returnV;
    }
}
