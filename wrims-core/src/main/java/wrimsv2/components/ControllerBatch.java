package wrimsv2.components;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;

import mil.army.usace.hec.metadata.Interval;
import mil.army.usace.hec.metadata.IntervalFactory;
import org.antlr.runtime.RecognitionException;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.config.ConfigUtils;
import wrimsv2.evaluator.AssignPastCycleVariable;
import wrimsv2.evaluator.CsvOperation;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.evaluator.PreEvaluator;
import wrimsv2.evaluator.TimeOperation;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.evaluator.WeightEval;
import wrimsv2.hdf5.HDF5Writer;
import wrimsv2.ilp.ILP;
import wrimsv2.launch.LaunchConfiguration;
import wrimsv2.solver.Cbc0Solver;
import wrimsv2.solver.CbcSolver;
import wrimsv2.solver.Clp0Solver;
import wrimsv2.solver.ClpSolver;
import wrimsv2.solver.InitialClpSolver;
import wrimsv2.solver.LPSolveSolver;
import wrimsv2.solver.XASolver;
import wrimsv2.solver.SetXALog;
import wrimsv2.solver.InitialXASolver;
import wrimsv2.solver.Gurobi.GurobiSolver;
import wrimsv2.solver.mpmodel.MPModel;
import wrimsv2.solver.ortools.OrToolsSolver;
import wrimsv2.sql.DataBaseProfile;
import wrimsv2.sql.MySQLCWriter;
import wrimsv2.sql.MySQLRWriter;
import wrimsv2.sql.SQLServerRWriter;
import wrimsv2.tools.General;
import wrimsv2.tools.Warmstart;
import wrimsv2.wreslparser.elements.StudyUtils;
import wrimsv2.wreslplus.elements.ParserUtils;
import wrimsv2.wreslplus.elements.procedures.ErrorCheck;
import wrimsv2.wreslplus.elements.Tools;

public class ControllerBatch {
    private int infeasCyclIndex= -100;
    public boolean enableProgressLog = false;
    public boolean enableConfigProgress = false;
    private boolean runCompleted = false;
    private MySQLCWriter mySQLCWriter;
    private MySQLRWriter mySQLRWriter;
    private SQLServerRWriter sqlServerRWriter;

    public ControllerBatch() {} // do nothing

    public ControllerBatch(String[] args) {
        long startTimeInMillis = Calendar.getInstance().getTimeInMillis();
        try {
            new DataBaseProfile(args);
            processArgs(args);
            if (ILP.loggingUsageMemeory) General.getPID();
            connectToDataBase();
            if (enableConfigProgress) {
                try {
                    FileWriter progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                    PrintWriter pw = new PrintWriter(progressFile);
                    pw.println("Parsing and preprocessing the model ...");
                    pw.close();
                    progressFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            StudyDataSet sds = parse();
            long afterParsing = Calendar.getInstance().getTimeInMillis();
            ControlData.t_parse=(int) (afterParsing-startTimeInMillis);
            System.out.println("Parsing Time is "+ControlData.t_parse/60000+"min"+Math.round((ControlData.t_parse/60000.0-ControlData.t_parse/60000)*60)+"sec");

            if (StudyUtils.total_errors+Error.getTotalError()==0 && !StudyUtils.compileOnly){
                if (!StudyUtils.loadParserData && !FilePaths.fullMainPath.endsWith(".par")){
                    StudyUtils.writeObj(sds, FilePaths.mainDirectory+File.separator+StudyUtils.configFileName+".par");
                }
                new PreEvaluator(sds);
                new PreRunModel(sds);
                //generateStudyFile();
                long check = Calendar.getInstance().getTimeInMillis();

                ILP.getIlpDir();
                ILP.setVarDir();
                ILP.createNoteFile();
                ILP.setMaximumFractionDigits();

                runModel(sds);
                if (ControlData.showTimeUsage) TimeUsage.showTimeUsage();
                long endTimeInMillis = Calendar.getInstance().getTimeInMillis();
                int runPeriod=(int) (endTimeInMillis-startTimeInMillis);
                System.out.println("=================Run Time is "+runPeriod/60000+"min"+Math.round((runPeriod/60000.0-runPeriod/60000)*60)+"sec====");
                ILP.writeNoteLn("Total time", "(sec): "+                        Math.round(runPeriod/1000.0));
                ILP.writeNoteLn("Total time", "(min): "+                        Math.round(runPeriod/1000.0/60));
                ILP.writeNoteLn("Total time", "(sec): "+                        Math.round(runPeriod/1000.0), ILP._noteFile_timeusage);
                ILP.writeNoteLn("Total time", "(min): "+                        Math.round(runPeriod/1000.0/60), ILP._noteFile_timeusage);
                runCompleted = true;
            } else {
                System.out.println("=================Run ends with errors=================");
            }

        } catch (RecognitionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunCompleted() {
        return runCompleted;
    }

    public void processArgs(String[] args){

        if(args[0].startsWith("-")) {
            if (args[0].toLowerCase().startsWith("-launch")){
                procLaunch(args);
            }else{
                ConfigUtils.loadArgs(args);
            }
            if (args[0].toLowerCase().endsWith(".launch.config")){
                enableConfigProgress=true;
            }
            if (ControlData.enableProgressLog){
                enableProgressLog=true;
            }
        } else {
            setControlData(args);
        }

    }

    public void setControlData(String[] args){
        FilePaths.groundwaterDir=args[0];
        FilePaths.setMainFilePaths(args[1]);
        FilePaths.setSvarFilePaths(args[2]);
        FilePaths.setInitFilePaths(args[3]);
        FilePaths.setDvarFilePaths(args[4]);
        ControlData.svDvPartF=args[5];
        ControlData.initPartF=args[6];
        ControlData.partA = args[7];
        ControlData.defaultTimeStep = args[8];
        ControlData.startYear=Integer.parseInt(args[9]);
        ControlData.startMonth=Integer.parseInt(args[10]);
        ControlData.startDay=Integer.parseInt(args[11]);
        ControlData.endYear=Integer.parseInt(args[12]);
        ControlData.endMonth=Integer.parseInt(args[13]);
        ControlData.endDay=Integer.parseInt(args[14]);
        ControlData.solverName=args[15];
        FilePaths.csvFolderName = args[16];
        ControlData.currYear=ControlData.startYear;
        ControlData.currMonth=ControlData.startMonth;
        ControlData.currDay=ControlData.startDay;
    }

    public void generateStudyFile(){
        String outPath=System.getenv("temp_wrims2")+"\\study.sty";
        FileWriter outstream;
        try {
            outstream = new FileWriter(outPath);
            BufferedWriter out = new BufferedWriter(outstream);
            out.write("Study File: Generated by WRIMS. Do Not Edit!\n");
            out.write("Study Name\n");
            out.write("Author\n");
            out.write("Time\n");
            out.write("Note\n");
            out.write("Version\n");
            out.write(FilePaths.groundwaterDir+"\n");
            out.write("StudyFileFullPath\n");
            out.write(FilePaths.fullMainPath+"\n");
            out.write(FilePaths.fullSvarFilePath+"\n");
            out.write(FilePaths.fullDvarDssPath+"\n");
            out.write(FilePaths.fullInitFilePath+"\n");
            out.write(ControlData.defaultTimeStep+"\n");
            out.write(VariableTimeStep.getTotalTimeStep(ControlData.defaultTimeStep)+"\n");
            out.write(ControlData.startDay+"\n");
            out.write(ControlData.startMonth+"\n");
            out.write(ControlData.startYear+"\n");
            out.write("SLP\n");
            out.write("CycleNumber\n");
            out.write("FALSE\n");
            out.write("NONE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write(" \n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("FALSE\n");
            out.write("CALSIM\n");
            out.write(ControlData.initPartF+"\n");
            out.write(ControlData.svDvPartF+"\n");
            out.write("FALSE\n");
            out.write("TRUE\n");
            out.write("FALSE\n");
            out.write("SINGLE\n");
            out.write("NODEBUG\n");
            out.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public StudyDataSet parse()throws RecognitionException, IOException{

        if (Error.getTotalError()>0){

            System.out.println("============================================");
            System.out.println("Total errors in the config file: "+Error.getTotalError());
            System.out.println("============================================");

            return null;

        } else if(StudyUtils.loadParserData) {
            String canonicalMainFilePath = Tools.getCanonicalLowCasePath(FilePaths.fullMainPath);
            ParserUtils.setRunDir(new File(canonicalMainFilePath).getParent());

            StudyDataSet sds = StudyUtils.loadObject(StudyUtils.parserDataPath);
            LoadParameter.process(sds);
            if (ControlData.useCbcWarmStart || ControlData.cbcCheckIntErr || ControlData.cbc_debug_routeCbc || ControlData.cbc_debug_routeXA){
                if (ControlData.solverName.equalsIgnoreCase("Cbc")  || ControlData.solverName.equalsIgnoreCase("Cbc1")){

                    Warmstart.collectIntegerDV_3(sds);

                }
            }
            return sds;

        } else if(StudyUtils.compileOnly) {

            return StudyUtils.compileStudy(FilePaths.fullMainPath);

        } else {

            return StudyUtils.checkStudy(FilePaths.fullMainPath);

        }
    }


    public void runModel(StudyDataSet sds){
        System.out.println("==============Run Study Start============");

        if (ControlData.solverName.equalsIgnoreCase("Gurobi")){
            runModelGurobi(sds);
        } else if (ControlData.solverName.equalsIgnoreCase("Glpk")){
            runModelOrTools(sds, "GLPK_MIXED_INTEGER_PROGRAMMING");
        } else if (ControlData.solverName.equalsIgnoreCase("Clp")){
            runModelClp(sds);
        } else if (ControlData.solverName.equalsIgnoreCase("Cbc")&&(ControlData.cbc_debug_routeXA||ControlData.cbc_debug_routeCbc)){
            runModelCbc(sds);
        } else if (ILP.logging){
            runModelILP(sds);
        } else if (ControlData.solverName.equalsIgnoreCase("Cbc")){
            runModelCbc(sds);
        } else if (ControlData.solverName.equalsIgnoreCase("XA") || ControlData.solverName.equalsIgnoreCase("XALOG") ){
            runModelXA(sds);
        } else {
            Error.addConfigError("Solver name not recognized: "+ControlData.solverName);
            Error.writeErrorLog();
        }

        WeightEval.outputWtTableAR();

        if (Error.getTotalError()>0){
            System.out.println("=================Run ends with errors====");
            System.exit(1);
        } else {
            System.out.println("=================Run ends!================");
        }
    }
    public void runModelXA(StudyDataSet sds){
        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        new InitialXASolver();
        if (Error.getTotalError()>0){
            System.out.println("Model run exits due to error.");
            System.exit(1);
        }

        TimeOperation.initOutputDate(ControlData.yearOutputSection);
        TimeOperation.initMemDate(ControlData.monMemSection);

        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);
        int sectionI=0;
        time_marching:
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){
            if (ControlData.solverName.equalsIgnoreCase("XALOG")) SetXALog.enableXALog();
            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){
                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();
                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.writeErrorLog();
                            noError=false;break time_marching;
                        }
                        new XASolver();

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        int cycleI=i+1;
                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=true;
                            ILP.loggingCplexLp=false;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            }else if(enableProgressLog){
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        int cycleI=i+1;
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            Date date1= new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
            Date date2= new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
            Date date3= new Date(ControlData.endYear-1900, ControlData.endMonth-1, ControlData.endDay);
            if (ControlData.yearOutputSection>0 && (date1.after(date2) ||date1.after(date3))){
                if (ControlData.writeInitToDVOutput && sectionI==0){
                    DssOperation.writeInitDvarAliasToDSS();
                }
                sectionI++;
                DssOperation.writeDVAliasToDSS();
                TimeOperation.setMemDate(ControlData.monMemSection);
                DssOperation.shiftData();
                TimeOperation.setOutputDate(ControlData.yearOutputSection);
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        ControlData.xasolver.close();

        if (ControlData.yearOutputSection<0 && ControlData.writeInitToDVOutput) DssOperation.writeInitDvarAliasToDSS();
        if (ControlData.yearOutputSection<0) DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeOutputDssEveryTenYears(){
        if (ControlData.currMonth==12 && ControlData.currYear%10==0){
            if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                DssOperation.writeDVAliasToDSS();
            }else if(ControlData.timeStep.equals("1DAY") && ControlData.currDay==31){
                DssOperation.writeDVAliasToDSS();
            }
        }
    }

    public static void main(String[] args){
        if((new ControllerBatch(args)).isRunCompleted()) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    public void runModelILP(StudyDataSet sds){

        ILP.initializeIlp();

        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        if (ControlData.solverName.equalsIgnoreCase("clp0")) {
            ControlData.solverType = Param.SOLVER_CLP0;
            // initiate clp0
            Clp0Solver.init();
        } else if (ControlData.solverName.equalsIgnoreCase("clp1")) {
            ControlData.solverType = Param.SOLVER_CLP1;
            // initiate clp
            ClpSolver.init(true);
        } else if (ControlData.solverName.equalsIgnoreCase("clp")) {
            ControlData.solverType = Param.SOLVER_CLP;
            // initiate clp
            ClpSolver.init(false);
        } else if (ControlData.solverName.equalsIgnoreCase("cbc0")) {
            ControlData.solverType = Param.SOLVER_CBC0;
            // initiate cbc0
            Cbc0Solver.init();
        } else if (ControlData.solverName.equalsIgnoreCase("cbc1")) {
            ControlData.solverType = Param.SOLVER_CBC1;
            // initiate cbc file passing jni
            CbcSolver.init(true, sds);
        } else if (ControlData.solverName.equalsIgnoreCase("cbc")) {
            ControlData.solverType = Param.SOLVER_CBC;
            // initiate cbc file passing jni
            CbcSolver.init(false, sds);
        } else if (ControlData.solverName.equalsIgnoreCase("lpsolve")) {
            ControlData.solverType = Param.SOLVER_LPSOLVE;
            // initiate lpsolve
        } else if (ControlData.solverName.toLowerCase().contains("xa")) {
            ControlData.solverType = Param.SOLVER_XA; //default
            new InitialXASolver();
        } else {
            Error.addConfigError("Solver name not recognized: "+ControlData.solverName);
            Error.writeErrorLog();
        }

        TimeOperation.initOutputDate(ControlData.yearOutputSection);
        TimeOperation.initMemDate(ControlData.monMemSection);

        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);
        int sectionI=0;
        time_marching:
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){
            if (ControlData.solverType == Param.SOLVER_XA && ControlData.solverName.toLowerCase().contains("xalog")) SetXALog.enableXALog();
            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){
                int cycleI=i+1;
                String strCycleI=cycleI+"";
                boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){
                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();

                        boolean isLastCycle = (i == modelList.size() - 1);

                        if (ILP.logging && (isSelectedCycleOutput || ILP.loggingAllCycles)) {

                            long beginT = System.currentTimeMillis();
                            ILP.setIlpFile();
                            ILP.writeIlp();
                            long endT = System.currentTimeMillis();
                            double time_second = (endT-beginT)/1000.;
                            ControlData.lpFileWritingTime += time_second;


                            if (ILP.loggingVariableValue) {
                                ILP.setVarFile();
                                ILP.writeSvarValue();
                            }
                        }

                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.writeErrorLog();
                            noError=false;break time_marching;
                        }

                        // choose solver to solve. TODO: this is not efficient. need to be done outside ILP
                        if (ControlData.solverType == Param.SOLVER_LPSOLVE.intValue()) {
                            LPSolveSolver.setLP(ILP.lpSolveFilePath);
                            LPSolveSolver.solve();
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {
                                    ILP.writeObjValue_LPSOLVE();
                                    if (ILP.loggingVariableValue) ILP.writeDvarValue_LPSOLVE();
                                }
                            }
                            // for cbc0
                        } else if (ControlData.solverType == Param.SOLVER_CBC0.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both wrims and ilp

                            // send lp file path to cbc
                            Cbc0Solver.setLP(ILP.cplexLpFilePath);

                            // call cbc solve
                            Cbc0Solver.solve();



                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging) {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(Cbc0Solver.varDoubleMap);
                                    }
                                }
                            }
                            // for clp
                        } else if (ControlData.solverType == Param.SOLVER_CLP0.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both wrims and ilp

                            // send lp file path to clp
                            Clp0Solver.setLP(ILP.cplexLpFilePath);

                            // call clp solve
                            Clp0Solver.solve();



                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(Clp0Solver.varDoubleMap);
                                    }
                                }
                            }
                        } else if (ControlData.solverType == Param.SOLVER_CBC1.intValue()||ControlData.solverType == Param.SOLVER_CBC.intValue()){

                            if(!ControlData.useCplexLpString) ILP.closeCplexLpFile(); // prevent double-locked by both wrims and ilp


                            CbcSolver.newProblem();

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    if(!ControlData.useCplexLpString) ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if(ControlData.saveCplexLpStringToFile) ILP.saveCplexLpStringToFile();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(CbcSolver.varDoubleMap);
                                    }
                                }
                            }

                        } else if (ControlData.solverType == Param.SOLVER_CLP1.intValue()){

                            ILP.closeCplexLpFile(); // prevent double-locked by both wrims and ilp

                            // send lp file path to clp
                            ClpSolver.newProblem(ILP.cplexLpFilePath, true);

                            // check solving errors and put them in Error.error_solving
                            if (Error.error_solving.size()<1) {
                                if (ILP.logging)  {

                                    ILP.reOpenCplexLpFile(true);
                                    // write objValue in lp file
                                    ILP.writeObjValue_Clp0_Cbc0();
                                    if (ILP.loggingVariableValue) {
                                        // TODO: write solution
                                        ILP.writeDvarValue_Clp0_Cbc0(ClpSolver.varDoubleMap);
                                    }
                                }
                            }

                        } else {

                            new XASolver();

                            if (ILP.logging) {
                                ILP.writeObjValue_XA();
                                if (ILP.loggingVariableValue) ILP.writeDvarValue_XA();
                            }
                        }



                        ILP.closeIlpFile();

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);


                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            //Error.writeSolvingErrorFile("Error_solving.txt");
                            //Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=false;
                            ILP.loggingCplexLp=true;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            }else if(enableProgressLog){
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        if (CbcSolver.intLog && ControlData.solverType == Param.SOLVER_CBC.intValue()) {
                            CbcSolver.logIntCheck(sds);
                        }

                        if (ControlData.solverType == Param.SOLVER_CBC1.intValue()||ControlData.solverType == Param.SOLVER_CBC.intValue()) { CbcSolver.resetModel();}

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            Date date1= new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
            Date date2= new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
            Date date3= new Date(ControlData.endYear-1900, ControlData.endMonth-1, ControlData.endDay);
            if (ControlData.yearOutputSection>0 && (date1.after(date2) || date1.after(date3))){
                if (ControlData.writeInitToDVOutput && sectionI==0){
                    DssOperation.writeInitDvarAliasToDSS();
                }
                sectionI++;
                DssOperation.writeDVAliasToDSS();
                TimeOperation.setMemDate(ControlData.monMemSection);
                DssOperation.shiftData();
                TimeOperation.setOutputDate(ControlData.yearOutputSection);
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        if (ControlData.solverType == Param.SOLVER_LPSOLVE) {
            //ControlData.lpssolver.deleteLp();
        } else if (ControlData.solverType == Param.SOLVER_CLP0) {
            // close clp exe
        } else if (ControlData.solverType == Param.SOLVER_CBC0) {
            // close cbc exe
        } else if (ControlData.solverType == Param.SOLVER_CBC || ControlData.solverType == Param.SOLVER_CBC1) {
            CbcSolver.close();
        } else if (ControlData.solverType == Param.SOLVER_CLP1 || ControlData.solverType == Param.SOLVER_CLP) {
            ClpSolver.close();
        } else {
            ControlData.xasolver.close();
        }

        if (ControlData.yearOutputSection<0 && ControlData.writeInitToDVOutput) DssOperation.writeInitDvarAliasToDSS();
        if (ControlData.yearOutputSection<0) DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runModelGurobi(StudyDataSet sds){
        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);

        ILP.initializeIlp();
        GurobiSolver.initialize();

        TimeOperation.initOutputDate(ControlData.yearOutputSection);
        TimeOperation.initMemDate(ControlData.monMemSection);

        int sectionI=0;
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){

            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){
                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();

                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.addSolvingError("evaluation error(s)");
                            Error.writeErrorLog();
                            noError=false;
                        } else {
                            ILP.setIlpFile();
                            ILP.writeIlp();
                            if (ILP.loggingVariableValue) {
                                ILP.setVarFile();
                                ILP.writeSvarValue();
                            }
                            GurobiSolver.setLp(ILP.cplexLpFilePath);
                            GurobiSolver.solve();
                        }

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        int cycleI=i+1;
                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            //Error.writeSolvingErrorFile("Error_solving.txt");
                            //Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=false;
                            ILP.loggingCplexLp=true;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            }else if(enableProgressLog){
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        int cycleI=i+1;
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            Date date1= new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
            Date date2= new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
            Date date3= new Date(ControlData.endYear-1900, ControlData.endMonth-1, ControlData.endDay);
            if (ControlData.yearOutputSection>0 && (date1.after(date2) || date1.after(date3))){
                if (ControlData.writeInitToDVOutput && sectionI==0){
                    DssOperation.writeInitDvarAliasToDSS();
                }
                sectionI++;
                DssOperation.writeDVAliasToDSS();
                TimeOperation.setMemDate(ControlData.monMemSection);
                DssOperation.shiftData();
                TimeOperation.setOutputDate(ControlData.yearOutputSection);
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        GurobiSolver.dispose();

        if (ControlData.yearOutputSection<0 && ControlData.writeInitToDVOutput) DssOperation.writeInitDvarAliasToDSS();
        if (ControlData.yearOutputSection<0) DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runModelGurobiTest(StudyDataSet sds){


        File ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");
        File ilpDir = new File(ilpRootDir, StudyUtils.configFileName);
        File cplexLpDir = new File(ilpDir,"cplexlp_input");
        PrintWriter objValueFile = null;
        try {
            objValueFile = Tools.openFile(ilpDir.getAbsolutePath(), "ObjValues.log");
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        GurobiSolver.initialize();

        //while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){
        for (int year = ControlData.startYear; year <= ControlData.endYear; year++) {

            for (int month = 1; month <= 12; month++) {

                for (int cycle = 1; cycle <= sds.getModelList().size(); cycle++) {

                    String twoDigitMonth = String.format("%02d", month);
                    String twoDigitCycle = String.format("%02d", cycle);
                    String lpFileName = year + "_" + twoDigitMonth + "_c" + twoDigitCycle + ".lp";

                    String msg = year + "_" + twoDigitMonth + "_c" + twoDigitCycle;

                    File lpFile = new File(cplexLpDir, lpFileName);

                    if (lpFile.exists()){

                        GurobiSolver.setLp(lpFile.getAbsolutePath());
                        GurobiSolver.solve();

                        double objValue = ControlData.gurobi_objective;
                        String objValueStr = Double.toString(objValue);

                        ILP.writeObjValueLog(msg, objValueStr, objValueFile);

                    }

                }
            }
        }

        GurobiSolver.dispose();

    }

    public void runModelOrTools(StudyDataSet sds, String mpSolverType){
        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);

        if (ILP.logging) ILP.initializeIlp();
        OrToolsSolver.initialize();
        ControlData.otsolver = new OrToolsSolver(mpSolverType);

        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){

            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){
                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();

                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.addSolvingError("evaluation error(s)");
                            Error.writeErrorLog();
                            noError=false;
                        } else {

                            MPModel m = ControlData.otsolver.createModel();

                            if (Error.error_solving.size()>=1){
                                Error.writeErrorLog();
                                noError=false;
                                break;
                            }


                            if (ILP.logging) {
                                ILP.setIlpFile();
                                if (ILP.loggingMPModel) ILP.writeMPModelFile(m);
                                ILP.writeIlp();
                                if (ILP.loggingVariableValue) {
                                    ILP.setVarFile();
                                    ILP.writeSvarValue();
                                }
                            }

                            ControlData.otsolver.run();
                        }

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        int cycleI=i+1;
                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            //Error.writeSolvingErrorFile("Error_solving.txt");
                            //Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=false;
                            ILP.loggingCplexLp=true;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        int cycleI=i+1;
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        ControlData.otsolver.delete();

        if (ControlData.writeInitToDVOutput){
            DssOperation.writeInitDvarAliasToDSS();
        }
        DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }
    }

    public void runModelClp(StudyDataSet sds){
        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        ClpSolver.init(false);

        if (Error.getTotalError()>0){
            System.out.println("Model run exits due to error.");
            System.exit(1);
        }
        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){

            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);

                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){

                    if (ControlData.currMonth==1){

                    }

                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();
                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.writeErrorLog();
                            noError=false;
                        }
                        String lpProblemlName = ControlData.currYear+"_"+ControlData.currMonth+"_c"+(ControlData.currCycleIndex+1);

                        ClpSolver.newProblem(lpProblemlName, false);

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        int cycleI=i+1;
                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            //Error.writeSolvingErrorFile("Error_solving.txt");
                            //Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=false;
                            ILP.loggingCplexLp=true;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            }else if(enableProgressLog){
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        int cycleI=i+1;
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);

        }
        ClpSolver.close();

        if (ControlData.writeInitToDVOutput){
            DssOperation.writeInitDvarAliasToDSS();
        }
        DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runModelCbc(StudyDataSet sds){

        ArrayList<String> modelList=sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap=sds.getModelDataSetMap();

        CbcSolver.init(false, sds); 	if (ControlData.cbc_debug_routeXA || ControlData.cbc_debug_routeCbc) {new InitialXASolver();}
        if (Error.getTotalError()>0){
            System.out.println("Model run exits due to error.");
            System.exit(1);
        }

        TimeOperation.initOutputDate(ControlData.yearOutputSection);
        TimeOperation.initMemDate(ControlData.monMemSection);

        ArrayList<ValueEvaluatorParser> modelConditionParsers=sds.getModelConditionParsers();
        boolean noError=true;
        VariableTimeStep.initialCurrTimeStep(modelList);
        VariableTimeStep.initialCycleStartDate();
        VariableTimeStep.setCycleEndDate(sds);
        int sectionI=0;
        while (VariableTimeStep.checkEndDate(ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear, ControlData.endDay, ControlData.endMonth, ControlData.endYear)<=0 && noError){

            ClearValue.clearValues(modelList, modelDataSetMap);
            sds.clearVarTimeArrayCycleValueMap();
            sds.clearVarCycleIndexByTimeStep();
            int i=0;
            while (i<modelList.size()  && noError){

                String model=modelList.get(i);
                ModelDataSet mds=modelDataSetMap.get(model);
                ControlData.currModelDataSet=mds;
                ControlData.currCycleName=model;
                ControlData.currCycleIndex=i;
                VariableTimeStep.setCycleTimeStep(sds);
                VariableTimeStep.setCurrentDate(sds, ControlData.cycleStartDay, ControlData.cycleStartMonth, ControlData.cycleStartYear);
                time_marching:
                while(VariableTimeStep.checkEndDate(ControlData.currDay, ControlData.currMonth, ControlData.currYear, ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear)<0 && noError){
                    ValueEvaluatorParser modelCondition=modelConditionParsers.get(i);
                    boolean condition=false;
                    try{
                        modelCondition.evaluator();
                        condition=modelCondition.evalCondition;
                    }catch (Exception e){
                        Error.addEvaluationError("Model condition evaluation has error.");
                        condition=false;
                    }
                    modelCondition.reset();

                    if (condition){
                        ClearValue.clearCycleLoopValue(modelList, modelDataSetMap);
                        ControlData.currSvMap=mds.svMap;
                        ControlData.currSvFutMap=mds.svFutMap;
                        ControlData.currDvMap=mds.dvMap;
                        ControlData.currDvSlackSurplusMap=mds.dvSlackSurplusMap;
                        ControlData.currAliasMap=mds.asMap;
                        ControlData.currGoalMap=mds.gMap;
                        ControlData.currTsMap=mds.tsMap;
                        ControlData.isPostProcessing=false;
                        mds.processModel();
                        if (Error.error_evaluation.size()>=1){
                            Error.writeEvaluationErrorFile("Error_evaluation.txt");
                            Error.writeErrorLog();
                            noError=false;break time_marching;
                        }

                        HashSet<String> originalDvarKeys=null;

                        if (ControlData.cbc_debug_routeCbc || ControlData.cbc_debug_routeXA) {
                            originalDvarKeys = new LinkedHashSet<String>(SolverData.getDvarMap().keySet());
//							ILP.getIlpDir();
//							ILP.setVarDir();
                            ILP.setSvarFile();
//							ILP.setMaximumFractionDigits();
                            // write svar
                            ILP.writeSvarValue();
                        }

                        if (ControlData.cbc_debug_routeCbc) {
                            new XASolver();
                            SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
                            if (Error.error_solving.size() > 0) {
                                String msg = "XA solving error.";
                                ILP.writeNoteLn("", msg);
                                System.out.println("" + msg);
                                Error.writeErrorLog();
                                Error.error_solving.clear();
                            }

                        }

                        CbcSolver.newProblem();

                        if (ControlData.cbc_debug_routeXA) {
                            SolverData.getDvarMap().keySet().retainAll(originalDvarKeys);
                            new XASolver();
                        }

                        // check monitored dvar list. they are slack and surplus generated automatically
                        // from the weight group deviation penalty
                        // give error if they are not zero or greater than a small tolerance.
                        noError = !ErrorCheck.checkDeviationSlackSurplus(mds.deviationSlackSurplus_toleranceMap, mds.dvMap);

                        if (ControlData.showRunTimeMessage) System.out.println("Solving Done.");

                        int cycleI=i+1;
                        if (Error.error_solving.size()<1){
                            ControlData.isPostProcessing=true;
                            mds.processAlias();
                            if (ControlData.showRunTimeMessage) System.out.println("Assign Alias Done.");
                        }else if (infeasCyclIndex==i) {
                            //Error.writeSolvingErrorFile("Error_solving.txt");
                            //Error.writeErrorLog();
                            noError=false;
                        }else{
                            Error.writeSolvingErrorFile("Error_solving.txt");
                            Error.writeErrorLog();
                            noError=true;
                            infeasCyclIndex=i;
                            i=-1;
                            Error.error_solving=new ArrayList();
                            ILP.loggingLpSolve=false;
                            ILP.loggingCplexLp=true;
                            ILP.loggingAllCycles=true;
                            ILP.logging=true;
                            ILP.loggingVariableValue=true;
                        }
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.writeOneCycleSv(mds, cycleI);
                            }
                        }
                        if (ILP.loggingUsageMemeory) ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        //ILP.logUsageMemory(ControlData.currYear, ControlData.currMonth, ControlData.currDay, ControlData.currCycleIndex);
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Done. ("+model+")");
                        if (Error.error_evaluation.size()>=1) noError=false;
                        try{
                            if (enableConfigProgress) {
                                FileWriter progressFile = new FileWriter(StudyUtils.configFilePath+".prgss");
                                PrintWriter pw = new PrintWriter(progressFile);
                                pw.println("Run to "+ControlData.currYear +"/"+ ControlData.currMonth +"/"+ ControlData.currDay);
                                pw.close();
                                progressFile.close();
                            }else if(enableProgressLog){
                                FileWriter progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt");
                                PrintWriter pw = new PrintWriter(progressFile);
                                int cy = 0;
                                if (ControlData.currYear > cy) {
                                    cy = ControlData.currYear;
                                    pw.println(ControlData.startYear + " " + ControlData.endYear + " " + ControlData.currYear +" "+ ControlData.currMonth);
                                    pw.close();
                                    progressFile.close();
                                }
                            }
                        }catch(IOException e){
                            e.printStackTrace();
                        }

                        //if (CbcSolver.intLog && (!ControlData.cbc_debug_routeXA && !ControlData.cbc_debug_routeCbc)) {
                        if (CbcSolver.intLog) {
                            CbcSolver.logIntCheck(sds);
                        }

                        if (ControlData.cbc_debug_routeXA ||ControlData.cbc_debug_routeCbc ) {
                            CbcSolver.logCbcDebug(sds);
                        } else if (ControlData.watchList!=null) {
                            CbcSolver.logCbcWatchList(sds);
                        }

                        CbcSolver.resetModel();

                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }else{
                        int cycleI=i+1;
                        String strCycleI=cycleI+"";
                        boolean isSelectedCycleOutput=General.isSelectedCycleOutput(strCycleI);
                        if (ControlData.outputType==1){
                            if (ControlData.isOutputCycle && isSelectedCycleOutput){
                                HDF5Writer.skipOneCycle(mds, cycleI);
                            }
                        }
                        System.out.println("Cycle "+cycleI+" in "+ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+" Skipped. ("+model+")");
                        new AssignPastCycleVariable();
                        ControlData.currTimeStep.set(ControlData.currCycleIndex, ControlData.currTimeStep.get(ControlData.currCycleIndex)+1);
                        if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
                            VariableTimeStep.currTimeAddOneMonth();
                        }else{
                            VariableTimeStep.currTimeAddOneDay();
                        }
                    }
                }
                i=i+1;
            }
            Date date1= new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
            Date date2= new Date(ControlData.outputYear-1900, ControlData.outputMonth-1, ControlData.outputDay);
            Date date3= new Date(ControlData.endYear-1900, ControlData.endMonth-1, ControlData.endDay);
            if (ControlData.yearOutputSection>0 && (date1.after(date2) || date1.after(date3))){
                if (ControlData.writeInitToDVOutput && sectionI==0){
                    DssOperation.writeInitDvarAliasToDSS();
                }
                sectionI++;
                DssOperation.writeDVAliasToDSS();
                TimeOperation.setMemDate(ControlData.monMemSection);
                DssOperation.shiftData();
                TimeOperation.setOutputDate(ControlData.yearOutputSection);
            }
            VariableTimeStep.setCycleStartDate(ControlData.cycleEndDay, ControlData.cycleEndMonth, ControlData.cycleEndYear);
            VariableTimeStep.setCycleEndDate(sds);
        }
        CbcSolver.close(); if (ControlData.cbc_debug_routeXA || ControlData.cbc_debug_routeCbc) {ControlData.xasolver.close();}

        if (ControlData.yearOutputSection<0 && ControlData.writeInitToDVOutput) DssOperation.writeInitDvarAliasToDSS();
        if (ControlData.yearOutputSection<0) DssOperation.writeDVAliasToDSS();
        ControlData.dvDss.close();
        if (ControlData.outputType==1){
            HDF5Writer.createDvarAliasLookup();
            HDF5Writer.writeTimestepData();
            HDF5Writer.writeCyclesDvAlias();
            HDF5Writer.closeDataStructure();
        }else if (ControlData.outputType==2){
            mySQLCWriter.process();
        }else if (ControlData.outputType==3){
            mySQLRWriter.process();
        }else if (ControlData.outputType==4){
            sqlServerRWriter.process();
        }else if (ControlData.outputType==5){
            CsvOperation co = new CsvOperation();
            co.ouputCSV(FilePaths.fullCsvPath, 0);
        }

        // write complete or fail
        if (enableProgressLog || enableConfigProgress) {
            try {
                FileWriter progressFile;
                if (enableConfigProgress){
                    progressFile= new FileWriter(StudyUtils.configFilePath+".prgss");
                }else{
                    progressFile= new FileWriter(FilePaths.mainDirectory + "progress.txt", true);
                }
                PrintWriter pw = new PrintWriter(progressFile);
                if (Error.getTotalError() > 0) {
                    pw.println("Run failed.");
                } else {
                    pw.println("Run completed.");
                }
                pw.close();
                progressFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectToDataBase(){
        if (ControlData.outputType==2){
            mySQLCWriter=new MySQLCWriter();
        }else if (ControlData.outputType==3){
            mySQLRWriter=new MySQLRWriter();
        }else if (ControlData.outputType==4){
            sqlServerRWriter=new SQLServerRWriter();
        }
    }

    public void procLaunch(String[] args){
        String launchFilePath = args[0].substring(args[0].indexOf("=") + 1, args[0].length());
        new LaunchConfiguration(launchFilePath);
    }
}