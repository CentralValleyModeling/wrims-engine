package gov.ca.water.wrims.engine.core.components;

import hec.heclib.dss.HecDss;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenStream;

// import com.sun.java.util.collections.Arrays;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Alias;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Timeseries;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.debug.ChangeSolver;
import gov.ca.water.wrims.engine.core.debug.FilterGoal;
import gov.ca.water.wrims.engine.core.debug.ReLoadSVDss;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssDataSet;
import gov.ca.water.wrims.engine.core.evaluator.DssDataSetFixLength;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;
import gov.ca.water.wrims.engine.core.evaluator.EvalExpression;
import gov.ca.water.wrims.engine.core.evaluator.LookUpTable;
import gov.ca.water.wrims.engine.core.evaluator.PreEvaluator;
import gov.ca.water.wrims.engine.core.evaluator.TableSeries;
import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;
import gov.ca.water.wrims.engine.core.evaluator.ValueEvaluation;
import gov.ca.water.wrims.engine.core.evaluator.ValueEvaluatorLexer;
import gov.ca.water.wrims.engine.core.evaluator.ValueEvaluatorParser;
import gov.ca.water.wrims.engine.core.ilp.ILP;
import gov.ca.water.wrims.engine.core.parallel.ParallelVars;
import gov.ca.water.wrims.engine.core.solver.CbcSolver;
import gov.ca.water.wrims.engine.core.solver.CloseCurrentSolver;
import gov.ca.water.wrims.engine.core.solver.InitialXASolver;
import gov.ca.water.wrims.engine.core.solver.SetXALog;
import gov.ca.water.wrims.engine.core.solver.Gurobi.GurobiSolver;
import gov.ca.water.wrims.engine.core.tools.Warmstart;
import gov.ca.water.wrims.engine.core.wreslparser.elements.FileParser;
import gov.ca.water.wrims.engine.core.wreslparser.elements.SimulationDataSet;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyParser;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;
import gov.ca.water.wrims.engine.core.wreslparser.grammar.WreslTreeWalker;
import gov.ca.water.wrims.engine.core.wreslplus.elements.AliasTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.DvarTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.GoalTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.IfIncItemGroup;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ModelTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.ParserUtils;
import gov.ca.water.wrims.engine.core.wreslplus.elements.StudyTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.SvarTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.Tools;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcIfIncItemGroup;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcWeight;

public class DebugInterface {
	private ServerSocket requestSocket;
	private Socket requestConnection;
	private ServerSocket eventSocket;
	private Socket eventConnection;
	private PrintWriter requestOut;
	private BufferedReader requestIn;
	private PrintWriter eventOut;
	private BufferedReader eventIn;
	public ArrayList<String> breakIndex=new ArrayList<String>();
	public ArrayList<String> breakFile=new ArrayList<String>();
	public boolean isDebugging=true;
	private ControllerDebug controllerDebug;
	private FileWriter statusFile;
	public PrintWriter fileOut;
	private boolean isStart=false;
	private String[] debugSvar;
	private String[] debugDvar;
	private String[] debugAlias;
	private String[] allDebugVariables;
	private ArrayList<String> filterGoalNames=new ArrayList<String>();
	private Map<String, FilterGoal> filterGoals=new HashMap<String, FilterGoal>();
	public DecimalFormat df = new DecimalFormat("#.####");
	public static String[] monitorVarNames=new String[0];
	public static String monitorVarTimeStep="";
	private String dataDir="data";
	private int terminateCode=0;
	
	public DebugInterface(int requestPort, int eventPort, String args[]){
		try{	
			statusFile = new FileWriter("DebugStatus.txt");
		}catch (IOException e){
			e.printStackTrace();
		}
		fileOut = new PrintWriter(statusFile);
		try {
			requestSocket = new ServerSocket(requestPort);
			eventSocket = new ServerSocket(eventPort);
			requestConnection=requestSocket.accept();
			eventConnection=eventSocket.accept();
			requestOut=new PrintWriter(requestConnection.getOutputStream());
			requestOut.flush();
			requestIn=new BufferedReader(new InputStreamReader(requestConnection.getInputStream()));
			eventOut=new PrintWriter(eventConnection.getOutputStream());
			eventOut.flush();
			eventIn=new BufferedReader(new InputStreamReader(eventConnection.getInputStream()));
			String message="";
			controllerDebug=new ControllerDebug(args, this);
			do {
				try{
					message=requestIn.readLine();
					handleRequest (message);
				}catch (Exception e){
					e.printStackTrace();
				}
			}while (isDebugging);
		} catch (IOException e){
			System.out.println(e.getMessage());
		}		
		
		finally{
			try{
				fileOut.close();
				requestIn.close();
				requestOut.close();
				requestSocket.close();
				eventSocket.close();
				System.exit(terminateCode);
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}

	}
		
	public void handleRequest(String request) {
		String dataString="";
		String goalString="";
		if (request.equals("start")) {
			controllerDebug.start();
			isStart=true;
			try {
				sendRequest("started");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (request.equals("resume")) {
			controllerDebug.safeResume();
			try {
				sendRequest("resumed");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.equals("data")){
			try {
				dataString=getDataString();
				sendRequest(dataString);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("data:")){
			ControlData.isParseStudy=false;
			int index=request.indexOf(":");
			try {
				if (request.equals("data:")){
					sendRequest("");
				}else{
					String fileName=request.substring(index+1);
					if (StudyUtils.useWreslPlus){
						dataString=getVariableInOneFileWP(fileName);
					}else{
						dataString=getVariableInOneFile(fileName);
					}
					sendRequest(dataString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ControlData.isParseStudy=true;
		}else if (request.startsWith("tsdetail:")){
			int index=request.indexOf(":");
			try {
				if (request.equals("tsdetail:")){
					sendRequest("");
				}else{
					String variableNames=request.substring(index+1);
					dataString=getTimeseriesDetail(variableNames);
					sendRequest(dataString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("futdetail:")){
			int index=request.indexOf(":");
			try {
				if (request.equals("futdetail:")){
					sendRequest("");
				}else{
					String variableName=request.substring(index+1);
					dataString=getFutureValueDetail(variableName);
					sendRequest(dataString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("cycledetail:")){
			int index=request.indexOf(":");
			try {
				if (request.equals("cycledetail:")){
					sendRequest("");
				}else{
					String variableName=request.substring(index+1);
					dataString=getCycleValueDetail(variableName);
					sendRequest(dataString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("watch:")){
			int index=request.indexOf(":");
			try{
				if (request.equals("watch:")){
					sendRequest("");
				}else{
					String vGNameString=request.substring(index+1);
					dataString=getWatch(vGNameString);
					sendRequest(dataString);
				}
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.equals("alldata")){
			try{
				dataString=getAllVariableFiles();
				sendRequest(dataString);
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("goal:")){
			ControlData.isParseStudy=false;
			int index=request.indexOf(":");
			try {
				if (request.equals("goal:")){
					sendRequest("");
				}else{
					String fileName=request.substring(index+1);
					if (StudyUtils.useWreslPlus){
						goalString=getGoalInOneFileWP(fileName);
					}else{
						goalString=getGoalInOneFile(fileName);
					}
					sendRequest(goalString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ControlData.isParseStudy=true;
		}else if (request.equals("allgoals")){
			try{
				goalString=getAllGoalFile();
				sendRequest(goalString);
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.equals("allcontrolgoals")){
			try{
				String goalName=getAllControlGoalName();
				sendRequest(goalName);
			}catch (IOException e){
				e.printStackTrace();
			}
			
		}else if (request.startsWith("filtergoal:")){
			int index=request.indexOf(":");
			try {
				if (request.equals("filtergoal:")){
					sendRequest("");
				}else{
					String fileName=request.substring(index+1);
					goalString=getFilterGoal(fileName);
					sendRequest(goalString);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("time")){
			String [] requestParts=request.split(":");
			String[] yearMonthDayCycle=requestParts[1].split("/");
			controllerDebug.debugYear=Integer.parseInt(yearMonthDayCycle[0]);
			controllerDebug.debugMonth=Integer.parseInt(yearMonthDayCycle[1]);
			controllerDebug.debugDay=Integer.parseInt(yearMonthDayCycle[2]);
			controllerDebug.debugCycle=Integer.parseInt(yearMonthDayCycle[3]);
			try {
				sendRequest("time defined");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("variables:")){
			String [] requestParts=request.split(":");
			allDebugVariables=requestParts[1].split("#");
			try {
				sendRequest("variables defined");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.equals("terminate")) {
            System.out.println("Terminate requested");
			controllerDebug.requestTerminate();
			if (isDebugging){
                System.out.println("Close Solvers");
				if (ControlData.solverName.equalsIgnoreCase("XA") || ControlData.solverName.equalsIgnoreCase("XALOG")) {
					ControlData.xasolver.close();
				}else if (ControlData.solverName.equalsIgnoreCase("Cbc")){
					CbcSolver.close();
				}
                System.out.println("Save Data");
                try {
                    DssOperation.saveInitialData(ControlData.dvDss, FilePaths.fullDvarDssPath);
                    DssOperation.saveDvarData(ControlData.dvDss, FilePaths.fullDvarDssPath);
                } catch (Exception exception) {
                    System.out.println("Exception ocurred while saving data during termination");
                    exception.printStackTrace();
                }
                System.out.println("Close Dss Files");
				ControlData.dvDss.close();
				terminateCode=1;
			}
			System.out.println("Terminated");
			try {
				sendRequest("terminated");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isDebugging=false;
		}else if (request.equals("suspend")) {
   controllerDebug.requestPause();
			System.out.println("suspended");
			try {
				sendRequest("suspended");
				sendEvent("suspended");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.equals("pause")) {
			controllerDebug.debugYear=ControlData.currYear;
			controllerDebug.debugMonth=ControlData.currMonth;
			controllerDebug.debugDay=ControlData.currDay;
			controllerDebug.debugCycle=ControlData.currCycleIndex+1;
			try {
				sendRequest("paused");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("modify_timeseries:")){
			String[] requestParts=request.split(":");
			boolean isModified=modifyTimeSeries(requestParts);
			try {
				if (isModified){
					sendRequest("modified");
				}else{
					sendRequest("not_modified");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("modify_cycle:")){
			String[] requestParts=request.split(":");
			boolean isModified=modifyCycle(requestParts);
			try {
				if (isModified){
					sendRequest("modified");
				}else{
					sendRequest("not_modified");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("resim_cycle:")){
			String[] requestParts=request.split(":");
			Error.clear();
			if (requestParts[1].equals("loadsv")){
				new ReLoadSVDss(ControlData.currStudyDataSet);
			}
			if (requestParts[3].equals("loadtable")){
				TableSeries.tableSeries=new HashMap<String, LookUpTable> ();
			}
			controllerDebug.modelIndex=Integer.parseInt(requestParts[2])-2;
			controllerDebug.safeResume();
			try {
				sendRequest("resumed");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("resim_date:")){
			String[] requestParts=request.split(":");
			Error.clear();
			ControlData.resimDate=true;
			ControlData.resimGroundwater=true;
			if (requestParts[1].equals("recompile")){
				StudyDataSet sds;
				try {
					sds = controllerDebug.parse();
					if (StudyParser.total_errors==0){
						ControlData.currStudyDataSet=sds;
						controllerDebug.totalCycles = sds.getModelList().size();
						sendEvent("totalcycle#"+controllerDebug.totalCycles);
						new PreEvaluator(sds);
					}else{
						System.out.println("The change of your code has errors.");
						sendEvent("terminate");
					}
				} catch (RecognitionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (requestParts[2].equals("loadsv")){
				new ReLoadSVDss(ControlData.currStudyDataSet);
			}
			if (requestParts[6].equals("loadtable")){
				TableSeries.tableSeries=new HashMap<String, LookUpTable> ();
			}
			controllerDebug.modelIndex=ControlData.currStudyDataSet.getModelList().size()-1;
			ControlData.resimYear=Integer.parseInt(requestParts[3]);
			ControlData.resimMonth=Integer.parseInt(requestParts[4]);
			ControlData.resimDay=Integer.parseInt(requestParts[5]);
			ControlData.currYear=ControlData.cycleEndYear;
			ControlData.currMonth=ControlData.cycleEndMonth;
			ControlData.currDay=ControlData.cycleEndDay;
			if (ControlData.isOutputCycle){
				Date resimDate=new Date(ControlData.resimYear-1900, ControlData.resimMonth-1, ControlData.resimDay);
				Date cycleDataDate = new Date(ControlData.cycleDataStartYear-1900, ControlData.cycleDataStartMonth-1, ControlData.cycleDataStartDay);
				if (resimDate.before(cycleDataDate)){
					ControlData.cycleDataStartYear=ControlData.resimYear;
					ControlData.cycleDataStartMonth=ControlData.resimMonth;
					ControlData.cycleDataStartDay=ControlData.resimDay;
				}
			}

			controllerDebug.safeResume();
			try {
				sendRequest("resumed");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("solveroption:")){
			String[] requestParts=request.split(":");
			setSolverOptions(requestParts);
			try {
				sendRequest("solveroptionset");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(request.equals("OutputCycleDataToDssOn")){
			if (!ControlData.isOutputCycle) initOutputCycleDataToDss();
			try {
				sendRequest("OutputCycleDataToDssOn");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Output cycle data to Dss file is turned on");
		}else if(request.equals("OutputCycleDataToDssOff")){
			ControlData.isOutputCycle=false;
			try {
				sendRequest("OutputCycleDataToDssOff");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Output cycle data to Dss file is turned off");
		}else if(request.equals("OutputAllCyclesOn")){
			ControlData.outputAllCycles=true;
			try {
				sendRequest("OutputAllCyclesOn");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("All cycles are selected");
		}else if(request.equals("OutputAllCyclesOff")){
			ControlData.outputAllCycles=false;
			try {
				sendRequest("OutputAllCyclesOff");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(request.startsWith("SelectedCycleOutput:")){
			int index=request.indexOf(":");
			ControlData.selectedCycleOutput=request.substring(index+1);
			try {
				sendRequest("Selected output cycles are set");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!ControlData.outputAllCycles) System.out.println("Cycles "+ControlData.selectedCycleOutput.replace("\'", "")+" are selected");
			setSelectedOutputCycles();
		}else if(request.startsWith("ShowRunTimeMessage:")){
			int index=request.indexOf(":");
			String showRunTimeMessage=request.substring(index+1);
			if (showRunTimeMessage.equalsIgnoreCase("True")){
				ControlData.showRunTimeMessage=true;
				try {
					sendRequest("Run time messages shown");
					System.out.println("Run time messages shown");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				ControlData.showRunTimeMessage=false;
				try {
					sendRequest("Run time messages disabled");
					System.out.println("Run time messages disabled");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if(request.startsWith("PrintGWFuncCalls:")){
			int index=request.indexOf(":");
			String printGWFuncCalls=request.substring(index+1);
			if (printGWFuncCalls.equalsIgnoreCase("True")){
				ControlData.printGWFuncCalls=true;
				try {
					sendRequest("Print groundwater function calls on");
					System.out.println("Print groundwater function calls on");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				ControlData.printGWFuncCalls=false;
				try {
					sendRequest("Print groundwater function calls off");
					System.out.println("Print groundwater function calls off");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if(request.startsWith("TrackMemoryUsage:")){
			int index=request.indexOf(":");
			String ilpLogUsageMemory=request.substring(index+1);
			if (ilpLogUsageMemory.equalsIgnoreCase("True")){
				ILP.loggingUsageMemeory=true;
				try {
					sendRequest("Track memory usage on");
					System.out.println("Track memory usage on");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				ILP.loggingUsageMemeory=false;
				try {
					sendRequest("Track memory usage off");
					System.out.println("Track memory usage off");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}else if (request.startsWith("savesvdss:")){
			int index=request.indexOf(":");
			String fileName=request.substring(index+1);
			boolean saveSucceed=saveSvDss(fileName);
			try {
				if (saveSucceed) {
					sendRequest("svardsssaved");
				}else{
					sendRequest("dsssavefailed");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("savedvdss:")){
			int index=request.indexOf(":");
			String fileName=request.substring(index+1);
			boolean saveSucceed=saveDvDss(fileName);
			try {
				if (saveSucceed) {
					sendRequest("dvardsssaved");
				}else{
					sendRequest("dsssavefailed");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(request.startsWith("saveinitdss:")){
			int index=request.indexOf(":");
			String fileName=request.substring(index+1);
			boolean saveSucceed=saveInitDss(fileName);
			try {
				if (saveSucceed) {
					sendRequest("initdsssaved");
				}else{
					sendRequest("dsssavefailed");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("conditional_breakpoint:")){
			setConditionalBreakpoint(request);
			try {
				sendRequest("conditioal breakpoint set");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcTolerancePrimal:")){
			String[] requestParts=request.split(":");
			CbcSolver.solve_2_primalT = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcTolerancePrimalRelax:")){
			String[] requestParts=request.split(":");
			CbcSolver.solve_2_primalT_relax = Double.parseDouble(requestParts[1]);
			ControlData.relationTolerance = Math.max(CbcSolver.solve_2_primalT_relax*10, 1e-6);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcToleranceWarmPrimal:")){
			String[] requestParts=request.split(":");
			CbcSolver.solve_whs_primalT = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcToleranceInteger:")){
			String[] requestParts=request.split(":");
			CbcSolver.integerT = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcToleranceIntegerCheck:")){
			String[] requestParts=request.split(":");
			CbcSolver.integerT_check = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcToleranceZero:")){
			String[] requestParts=request.split(":");
			ControlData.zeroTolerance = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcHintRelaxPenalty:")){
			String[] requestParts=request.split(":");
			CbcSolver.cbcHintRelaxPenalty = Double.parseDouble(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if (request.startsWith("cbcHintTimeMax:")){
			String[] requestParts=request.split(":");
			CbcSolver.cbcHintTimeMax = Integer.parseInt(requestParts[1]);
			try {
				sendRequest(request+" set");
				System.out.println(request+" set");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void sendRequest(String request) throws IOException {
		synchronized (requestConnection){
			try{
				requestOut.println(request);
				requestOut.flush();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}	
	
	public void sendEvent(String event) throws IOException {
		synchronized (eventConnection){
			try{
				eventOut.println(event);
				eventOut.flush();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public String getVariableInOneFile(String fileFullPath){
		String dataString="";
		String modelName=ControlData.currStudyDataSet.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds=ControlData.currStudyDataSet.getModelDataSetMap().get(modelName);
		Map<String, Timeseries> tsMap = mds.tsMap;
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, Svar> svMap = mds.svMap;
		Map<String, Svar> svFutMap = mds.svFutMap;
		Map<String, Alias> asMap = mds.asMap;
		Map<String, String> partsMap=new HashMap<String, String>(); 
		try {
			WreslTreeWalker walker = FileParser.parseOneFileForDebug(fileFullPath);
			if (walker==null) return dataString;
			SimulationDataSet fileDataSet = walker.thisFileDataSet;
			ArrayList<String> dvList = fileDataSet.dvList;
			ArrayList<String> svList = fileDataSet.svList;
			ArrayList<String> asList = fileDataSet.asList;
			ArrayList<String> tsList = fileDataSet.tsList;
			ArrayList<String> wtList = fileDataSet.wtList;
			asList.removeAll(dvList);
			ArrayList<String> sortedList=new ArrayList<String>();
			sortedList.addAll(dvList);
			sortedList.addAll(svList);
			sortedList.addAll(tsList);
			sortedList.addAll(asList);
			sortedList.addAll(wtList);
			
			ArrayList<String> gList = fileDataSet.gList;
			for (String gName:gList){
				Set<String> dependants = fileDataSet.gMap.get(gName).expressionDependants;
				Iterator<String> iterator = dependants.iterator();
				while (iterator.hasNext()){
					String varName=iterator.next();
					if (!sortedList.contains(varName)){
						sortedList.add(varName);
					}
				}
			}
			for (String svName:svList){
				Set<String> dependants = fileDataSet.svMap.get(svName).dependants;
				Iterator<String> iterator = dependants.iterator();
				while (iterator.hasNext()){
					String varName=iterator.next();
					if (!sortedList.contains(varName)){
						sortedList.add(varName);
					}
				}
			}
			for (String asName:asList){
				if (fileDataSet.asMap.containsKey(asName)){
					Set<String> dependants = fileDataSet.asMap.get(asName).dependants;
					Iterator<String> iterator = dependants.iterator();
					while (iterator.hasNext()){
						String varName=iterator.next();
						if (!sortedList.contains(varName)){
							sortedList.add(varName);
						}
					}
				}
			}
			
			IntDouble intDouble;
			Collections.sort(sortedList, String.CASE_INSENSITIVE_ORDER);
			for (String variable: sortedList){
				if (svMap.containsKey(variable)){
					Svar sv = svMap.get(variable);
					intDouble=sv.getData();
					if (intDouble != null) {
						dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
						partsMap.put(variable, sv.kind+":"+ControlData.timeStep);
					}
				}else if (dvMap.containsKey(variable)){
					Dvar dv = dvMap.get(variable);
					intDouble=dv.getData();
					if (intDouble != null) {
						dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
						partsMap.put(variable, dv.kind+":"+ControlData.timeStep);
					}
				}else if (tsMap.containsKey(variable)){
					Timeseries ts = tsMap.get(variable);
					intDouble=ts.getData();
					if (intDouble != null) {
						dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
						partsMap.put(variable, ts.kind+":"+ControlData.timeStep);
					}
				}else if (asMap.containsKey(variable)){
					Alias as = asMap.get(variable);
					intDouble=as.getData();
					if (intDouble != null) {
						dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
						partsMap.put(variable, as.kind+":"+ControlData.timeStep);
					}
				}
			}
			if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
			dataString=dataString+"!";
			for (String variable: partsMap.keySet()){
				dataString=dataString+variable+":"+partsMap.get(variable)+"#";
			}
			if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return dataString;
	}
	
	public String getVariableInOneFileWP(String fileFullPath){
		String dataString="";
		StudyDataSet sds = ControlData.currStudyDataSet;
		String modelName=sds.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds=sds.getModelDataSetMap().get(modelName);
		Map<String, Svar> parameterMap = sds.getParameterMap();
		Map<String, Timeseries> tsMap = mds.tsMap;
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, Svar> svMap = mds.svMap;
		Map<String, Svar> svFutMap = mds.svFutMap;
		Map<String, Alias> asMap = mds.asMap;
		Map<String, String> partsMap=new HashMap<String, String>();
		ArrayList<String> parameterList = new ArrayList<String>();
		ArrayList<String> dvList = new ArrayList<String>();
		ArrayList<String> svList = new ArrayList<String>();
		ArrayList<String> asList = new ArrayList<String>();
		ArrayList<String> tsList = new ArrayList<String>();
		ArrayList<String> wtList = new ArrayList<String>();
		ArrayList<String> gList = new ArrayList<String>();
		ArrayList<String> sortedList = new ArrayList<String>();
		ModelTemp modelTemp;
		fileFullPath=fileFullPath.replace("/", "\\");
		if (fileFullPath.equalsIgnoreCase(FilePaths.fullMainPath)){
			StudyTemp studyTemp = ParserUtils.parseWreslMain(fileFullPath);
			if (studyTemp==null) return dataString;
			parameterList = studyTemp.parameterList;
			sortedList.addAll(parameterList);
			Map<String, ModelTemp> modelMap = studyTemp.modelMap;
			ArrayList<String> modelList = studyTemp.modelList;
			for (String model: modelList){
				modelTemp=modelMap.get(model);
				dvList = modelTemp.dvList;
				svList = modelTemp.svList;
				asList = modelTemp.asList;
				tsList = modelTemp.tsList;
				gList = modelTemp.glList;
				ProcWeight.collectWeightVar(modelTemp);
				wtList = modelTemp.wvList_post;
				asList.removeAll(dvList);
				sortedList.addAll(dvList);
				sortedList.addAll(svList);
				sortedList.addAll(tsList);
				sortedList.addAll(asList);
				sortedList.addAll(wtList);
				for (String gName:gList){
					if (modelTemp.glMap.containsKey(gName)){
						Set<String> dependants = modelTemp.glMap.get(gName).dependants;
						Iterator<String> iterator = dependants.iterator();
						while (iterator.hasNext()){
							String varName=iterator.next();
							if (!sortedList.contains(varName)){
								sortedList.add(varName);
							}
						}
					}
				}
				for (String svName:svList){
					if (modelTemp.svMap.containsKey(svName)){
						Set<String> dependants = modelTemp.svMap.get(svName).dependants;
						Iterator<String> iterator = dependants.iterator();
						while (iterator.hasNext()){
							String varName=iterator.next();
							if (!sortedList.contains(varName)){
								sortedList.add(varName);
							}
						}
					}
				}
				for (String asName:asList){
					if (modelTemp.asMap.containsKey(asName)){
						Set<String> dependants = modelTemp.asMap.get(asName).dependants;
						Iterator<String> iterator = dependants.iterator();
						while (iterator.hasNext()){
							String varName=iterator.next();
							if (!sortedList.contains(varName)){
								sortedList.add(varName);
							}
						}
					}
				}
				ArrayList<String> varIfInc = getVariableIfInc(modelTemp);
				sortedList.remove(varIfInc);
				sortedList.addAll(varIfInc);
			}
		}else{
			modelTemp = ParserUtils.parseWreslFile(fileFullPath);
			if (modelTemp==null) return dataString;
			dvList = modelTemp.dvList;
			svList = modelTemp.svList;
			asList = modelTemp.asList;
			tsList = modelTemp.tsList;
			gList = modelTemp.glList;
			ProcWeight.collectWeightVar(modelTemp);
			wtList = modelTemp.wvList_post;
			sortedList = new ArrayList<String>();
			asList.removeAll(dvList);
			sortedList.addAll(dvList);
			sortedList.addAll(svList);
			sortedList.addAll(tsList);
			sortedList.addAll(asList);
			sortedList.addAll(wtList);
			for (String gName:gList){
				if (modelTemp.glMap.containsKey(gName)){
					Set<String> dependants = modelTemp.glMap.get(gName).dependants;
					Iterator<String> iterator = dependants.iterator();
					while (iterator.hasNext()){
						String varName=iterator.next();
						if (!sortedList.contains(varName)){
							sortedList.add(varName);
						}
					}
				}
			}
			for (String svName:svList){
				if (modelTemp.svMap.containsKey(svName)){
					Set<String> dependants = modelTemp.svMap.get(svName).dependants;
					Iterator<String> iterator = dependants.iterator();
					while (iterator.hasNext()){
						String varName=iterator.next();
						if (!sortedList.contains(varName)){
							sortedList.add(varName);
						}
					}
				}
			}
			for (String asName:asList){
				if (modelTemp.asMap.containsKey(asName)){
					Set<String> dependants = modelTemp.asMap.get(asName).dependants;
					Iterator<String> iterator = dependants.iterator();
					while (iterator.hasNext()){
						String varName=iterator.next();
						if (!sortedList.contains(varName)){
							sortedList.add(varName);
						}
					}
				}
			}
			ArrayList<String> varIfInc = getVariableIfInc(modelTemp);
			sortedList.remove(varIfInc);
			sortedList.addAll(varIfInc);
		}
		IntDouble intDouble;
		Collections.sort(sortedList, String.CASE_INSENSITIVE_ORDER);
		for (String variable: sortedList){
			variable=variable.toLowerCase();
			if (svMap.containsKey(variable)){
				Svar sv = svMap.get(variable);
				intDouble=sv.getData();
				if (intDouble != null) {
					dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
					partsMap.put(variable, sv.kind+":"+ControlData.timeStep);
				}
			}else if (dvMap.containsKey(variable)){
				Dvar dv = dvMap.get(variable);
				intDouble=dv.getData();
				if (intDouble != null) {
					dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
					partsMap.put(variable, dv.kind+":"+ControlData.timeStep);
				}
			}else if (tsMap.containsKey(variable)){
				Timeseries ts = tsMap.get(variable);
				intDouble=ts.getData();
				if (intDouble != null) {
					dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
					partsMap.put(variable, ts.kind+":"+ControlData.timeStep);
				}
			}else if (asMap.containsKey(variable)){
				Alias as = asMap.get(variable);
				intDouble=as.getData();
				if (intDouble != null) {
					dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
					partsMap.put(variable, as.kind+":"+ControlData.timeStep);
				}
			}else if (parameterMap.containsKey(variable)){
				Svar sv = parameterMap.get(variable);
				intDouble=sv.getData();
				if (intDouble != null){
					dataString=dataString+variable+":"+df.format(intDouble.getData())+"#";
					partsMap.put(variable, sv.kind+":"+ControlData.timeStep);
				}
			}
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		dataString=dataString+"!";
		for (String variable: partsMap.keySet()){
			dataString=dataString+variable+":"+partsMap.get(variable)+"#";
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		
		return dataString;
	}
	
	public ArrayList<String> getVariableIfInc(ModelTemp modelTemp){
		ArrayList<String> varList= new ArrayList<String>();
		ArrayList<String> ifIncItemGroupIDList = modelTemp.ifIncItemGroupIDList;
		Map<String, IfIncItemGroup> ifIncItemGroupMap = modelTemp.ifIncItemGroupMap;
		for (int i=0; i<ifIncItemGroupIDList.size(); i++){
			IfIncItemGroup ifIncItemGroup=ifIncItemGroupMap.get(ifIncItemGroupIDList.get(i));
			varList.removeAll(ifIncItemGroup.dependants);
			varList.addAll(ifIncItemGroup.dependants);
			ArrayList<Boolean> conditionValueList = ProcIfIncItemGroup.evaluateConditions(Tools.allToLowerCase(ifIncItemGroup.conditionList));
			int j=0;
			int cn=conditionValueList.size();
			while (j<cn && !conditionValueList.get(j)){
				j=j+1;
			}
			
			if (j<cn){
				ArrayList<HashMap<String, SvarTemp>> inc_svar_map_list = ifIncItemGroup.inc_svar_map_list;
				HashMap<String, SvarTemp> inc_svar_map = inc_svar_map_list.get(j);
				Set<String> svarNames = inc_svar_map.keySet();
				varList.addAll(svarNames);
				Iterator<String> iterator = svarNames.iterator();
				while (iterator.hasNext()){
					String svarName = iterator.next();
					SvarTemp svar = inc_svar_map.get(svarName);
					Set<String> dependants = svar.dependants;
					varList.removeAll(dependants);
					varList.addAll(dependants);
				}
							
				ArrayList<HashMap<String, DvarTemp>> inc_dvar_map_list = ifIncItemGroup.inc_dvar_map_list;
				HashMap<String, DvarTemp> inc_dvar_map = inc_dvar_map_list.get(j);
				Set<String> dvarNames = inc_dvar_map.keySet();
				varList.addAll(dvarNames);
				iterator = dvarNames.iterator();
				while (iterator.hasNext()){
					String dvarName = iterator.next();
					DvarTemp dvar = inc_dvar_map.get(dvarName);
					Set<String> dependants = dvar.dependants;
					varList.removeAll(dependants);
					varList.addAll(dependants);
				}
			
				ArrayList<LinkedHashMap<String, AliasTemp>> inc_alias_map_list = ifIncItemGroup.inc_alias_map_list;
				HashMap<String, AliasTemp> inc_alias_map = inc_alias_map_list.get(j);
				Set<String> aliasNames = inc_alias_map.keySet();
				varList.addAll(aliasNames);
				iterator = aliasNames.iterator();
				while (iterator.hasNext()){
					String aliasName = iterator.next();
					AliasTemp alias = inc_alias_map.get(aliasName);
					Set<String> dependants = alias.dependants;
					varList.removeAll(dependants);
					varList.addAll(dependants);
				}
			
				ArrayList<HashMap<String, GoalTemp>> inc_goalSimple_map_list = ifIncItemGroup.inc_goalSimple_map_list;
				HashMap<String, GoalTemp> inc_goalSimple_map = inc_goalSimple_map_list.get(j);
				Set<String> goalSimpleNames = inc_goalSimple_map.keySet();
				iterator = goalSimpleNames.iterator();
				while (iterator.hasNext()){
					String goalSimpleName = iterator.next();
					GoalTemp goalSimple = inc_goalSimple_map.get(goalSimpleName);
					Set<String> dependants = goalSimple.dependants;
					varList.removeAll(dependants);
					varList.addAll(dependants);
				}
			
				ArrayList<HashMap<String, GoalTemp>> inc_goalComplex_map_list = ifIncItemGroup.inc_goalComplex_map_list;
				HashMap<String, GoalTemp> inc_goalComplex_map = inc_goalComplex_map_list.get(j);
				Set<String> goalComplexNames = inc_goalComplex_map.keySet();
				iterator = goalComplexNames.iterator();
				while (iterator.hasNext()){
					String goalComplexName = iterator.next();
					GoalTemp goalComplex = inc_goalComplex_map.get(goalComplexName);
					Set<String> dependants = goalComplex.dependants;
					varList.removeAll(dependants);
					varList.addAll(dependants);
				}
			
				varList.addAll(ifIncItemGroup.inc_timeseries_map_list.get(j).keySet());
			}
		}
		return varList;
	}
	
	public String getGoalInOneFile(String fileFullPath){
		String goalString="";
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		ArrayList<String> controlGoals=new ArrayList<String>();
		
		try {
			WreslTreeWalker walker = FileParser.parseOneFileForDebug(fileFullPath);
			if (walker==null) return goalString;
			SimulationDataSet fileDataSet = walker.thisFileDataSet;	
			ArrayList<String> sortedGoalList = fileDataSet.gList;
			
			Collections.sort(sortedGoalList, String.CASE_INSENSITIVE_ORDER);
			for (int i=0; i<sortedGoalList.size(); i++){
				double lhs=0;
				String goalName=sortedGoalList.get(i);
				if (gMap.containsKey(goalName)){
					goalString=goalString+goalName+":";
					EvalConstraint ec=gMap.get(goalName);
					if (ec!=null){
						EvalExpression ee=ec.getEvalExpression();
						Map<String, IntDouble> multiplier = ee.getMultiplier();
						Set<String> mKeySet = multiplier.keySet();
						Iterator<String> mi = mKeySet.iterator();
						boolean hasData = true;
						while (mi.hasNext()){
							String variable=mi.next();
							Number value=multiplier.get(variable).getData();
							double value1=value.doubleValue();
							if (goalString.endsWith(":")){
								if (value1==1.0){
									goalString=goalString+variable;
								}else if (value1==-1.0){
									goalString=goalString+"-"+variable;
								}else{
									goalString=goalString+df.format(value)+variable;
								}
							}else{
								if (value1==1.0){
									goalString=goalString+"+"+variable;
								}else if (value1 == -1.0){
									goalString=goalString+"-"+variable;
								}else if(value1>=0){
									goalString=goalString+"+"+df.format(value)+variable;
								}else{
									goalString=goalString+df.format(value)+variable;
								}
							}
							if (!(variable.startsWith("surplus__") || variable.startsWith("slack__"))){
								IntDouble id = dvMap.get(variable).getData();
								if (id==null){
									hasData=false;
								}else{
									double variableValue=id.getData().doubleValue();
									lhs=lhs+value1*variableValue;
								}
							}
						}
						Number value=ee.getValue().getData();
						double value1=value.doubleValue();
						if (value1>0){
							goalString=goalString+"+"+df.format(value)+ec.getSign()+"0#";
						}else if(value1<0){
							goalString=goalString+df.format(value)+ec.getSign()+"0#";
						}else{
							goalString=goalString+ec.getSign()+"0#";
						}
						lhs=lhs+value1;
						if (Math.abs(lhs)<=0.00001 && hasData){
							controlGoals.add(goalName);
						}
					}
				}
			}
			if (goalString.endsWith("#")) goalString=goalString.substring(0, goalString.length()-1);
		} catch (RecognitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		goalString=goalString+"!";
		for (String controlGoal:controlGoals){
			goalString=goalString+controlGoal+":";
		}
		if (goalString.endsWith(":")) goalString=goalString.substring(0, goalString.length()-1);
		return goalString;
	}
	
	public String getGoalInOneFileWP(String fileFullPath){
		String goalString="";
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		ArrayList<String> controlGoals=new ArrayList<String>();
		ArrayList<String> sortedGoalList=new ArrayList<String>();
		
		fileFullPath=fileFullPath.replace("/", "\\");
		if (fileFullPath.equalsIgnoreCase(FilePaths.fullMainPath)){
			StudyTemp studyTemp = ParserUtils.parseWreslMain(fileFullPath);
			if (studyTemp==null) return goalString;
			Map<String, ModelTemp> modelMap = studyTemp.modelMap;
			ArrayList<String> modelList = studyTemp.modelList;
			for (String model: modelList){
				ModelTemp modelTemp = modelMap.get(model);
				sortedGoalList.addAll(modelTemp.glList);
				ArrayList<String> ifIncItemGroupIDList = modelTemp.ifIncItemGroupIDList;
				Map<String, IfIncItemGroup> ifIncItemGroupMap = modelTemp.ifIncItemGroupMap;
				for (int i=0; i<ifIncItemGroupIDList.size(); i++){
					IfIncItemGroup ifIncItemGroup=ifIncItemGroupMap.get(ifIncItemGroupIDList.get(i));
					ArrayList<Boolean> conditionValueList = ProcIfIncItemGroup.evaluateConditions(Tools.allToLowerCase(ifIncItemGroup.conditionList));
					int j=0;
					int cn=conditionValueList.size();
					while (j<cn && !conditionValueList.get(j)){
						j=j+1;
					}
					if (j<cn){
						HashMap<String, GoalTemp> simpleGoals = ifIncItemGroup.inc_goalSimple_map_list.get(j);
						HashMap<String, GoalTemp> complexGoals = ifIncItemGroup.inc_goalComplex_map_list.get(j);
						sortedGoalList.addAll(simpleGoals.keySet());
						sortedGoalList.addAll(complexGoals.keySet());
					}
				}
			}
		}else{
			ModelTemp modelTemp = ParserUtils.parseWreslFile(fileFullPath);
			if (modelTemp==null) return goalString;	
			sortedGoalList = modelTemp.glList;
			ArrayList<String> ifIncItemGroupIDList = modelTemp.ifIncItemGroupIDList;
			Map<String, IfIncItemGroup> ifIncItemGroupMap = modelTemp.ifIncItemGroupMap;
			for (int i=0; i<ifIncItemGroupIDList.size(); i++){
				IfIncItemGroup ifIncItemGroup=ifIncItemGroupMap.get(ifIncItemGroupIDList.get(i));
				ArrayList<Boolean> conditionValueList = ProcIfIncItemGroup.evaluateConditions(Tools.allToLowerCase(ifIncItemGroup.conditionList));
				int j=0;
				int cn=conditionValueList.size();
				while (j<cn && !conditionValueList.get(j)){
					j=j+1;
				}
				if (j<cn){
					HashMap<String, GoalTemp> simpleGoals = ifIncItemGroup.inc_goalSimple_map_list.get(j);
					HashMap<String, GoalTemp> complexGoals = ifIncItemGroup.inc_goalComplex_map_list.get(j);
					sortedGoalList.addAll(simpleGoals.keySet());
					sortedGoalList.addAll(complexGoals.keySet());
				}
			}
		}
		
		Collections.sort(sortedGoalList, String.CASE_INSENSITIVE_ORDER);
		for (int i=0; i<sortedGoalList.size(); i++){
			double lhs=0;
			String goalName=sortedGoalList.get(i);
			goalName=goalName.toLowerCase();
			if (gMap.containsKey(goalName)){
				goalString=goalString+goalName+":";
				EvalConstraint ec=gMap.get(goalName);
				if (ec!=null){
					EvalExpression ee=ec.getEvalExpression();
					Map<String, IntDouble> multiplier = ee.getMultiplier();
					Set<String> mKeySet = multiplier.keySet();
					Iterator<String> mi = mKeySet.iterator();
					boolean hasData = true;
					while (mi.hasNext()){
						String variable=mi.next();
						Number value=multiplier.get(variable).getData();
						double value1=value.doubleValue();
						if (goalString.endsWith(":")){
							if (value1==1.0){
								goalString=goalString+variable;
							}else if (value1==-1.0){
								goalString=goalString+"-"+variable;
							}else{
								goalString=goalString+df.format(value)+variable;
							}
						}else{
							if (value1==1.0){
								goalString=goalString+"+"+variable;
							}else if (value1 == -1.0){
								goalString=goalString+"-"+variable;
							}else if(value1>=0){
								goalString=goalString+"+"+df.format(value)+variable;
							}else{
								goalString=goalString+df.format(value)+variable;
							}
						}
						if (!(variable.startsWith("surplus__") || variable.startsWith("slack__"))){
							IntDouble id = dvMap.get(variable).getData();
							if (id==null){
								hasData=false;
							}else{
								double variableValue=id.getData().doubleValue();
								lhs=lhs+value1*variableValue;
							}
						}
					}
					Number value=ee.getValue().getData();
					double value1=value.doubleValue();
					if (value1>0){
						goalString=goalString+"+"+df.format(value)+ec.getSign()+"0#";
					}else if(value1<0){
						goalString=goalString+df.format(value)+ec.getSign()+"0#";
					}else{
						goalString=goalString+ec.getSign()+"0#";
					}
					lhs=lhs+value1;
					if (Math.abs(lhs)<=0.00001 && hasData){
						controlGoals.add(goalName);
					}
				}
			}
		}
		if (goalString.endsWith("#")) goalString=goalString.substring(0, goalString.length()-1);
		goalString=goalString+"!";
		for (String controlGoal:controlGoals){
			goalString=goalString+controlGoal+":";
		}
		if (goalString.endsWith(":")) goalString=goalString.substring(0, goalString.length()-1);
		return goalString;
	}

	public String getFilterGoal(String fileName){
		String filtergoal="filteredgoalretrieved";
		if (procFilterFile(fileName)){
			Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
			Map<String, Dvar> dvMap = SolverData.getDvarMap();
			Set<String> goalKeySet=filterGoals.keySet();
								
			try{
				File dataFolder= new File(dataDir);
				dataFolder.mkdirs();
				File filterGoalFile = new File(dataFolder, "filtergoals.dat");
				FileOutputStream filterGoalStream = new FileOutputStream(filterGoalFile);
				OutputStreamWriter filterGoalWriter = new OutputStreamWriter(filterGoalStream);    
				BufferedWriter filterGoalBuffer = new BufferedWriter(filterGoalWriter);
			
				for (int i=0; i<filterGoalNames.size(); i++){
					String goalName=filterGoalNames.get(i);
					String goalString=goalName+";";
					double lhs=0.0;
					if (gMap.containsKey(goalName)){
						EvalConstraint ec=gMap.get(goalName);
						if (ec!=null){
							EvalExpression ee=ec.getEvalExpression();
							Map<String, IntDouble> multiplier = ee.getMultiplier();
							Set<String> mKeySet = multiplier.keySet();
							Iterator<String> mi = mKeySet.iterator();
							boolean hasData = true;
							while (mi.hasNext()){
								String variable=mi.next();
								Number value=multiplier.get(variable).getData();
								double value1=value.doubleValue();
								if (goalString.endsWith(":")){
									if (value1==1.0){
										goalString=goalString+variable;
									}else if (value1==-1.0){
										goalString=goalString+"-"+variable;
									}else{
										goalString=goalString+df.format(value)+variable;
									}
								}else{
									if (value1==1.0){
										goalString=goalString+"+"+variable;
									}else if (value1 == -1.0){
										goalString=goalString+"-"+variable;
									}else if(value1>=0){
										goalString=goalString+"+"+df.format(value)+variable;
									}else{
										goalString=goalString+df.format(value)+variable;
									}
								}
								if (!(variable.startsWith("surplus__") || variable.startsWith("slack__"))){
									IntDouble id = dvMap.get(variable).getData();
									if (id==null){
										hasData=false;
									}else{
										double variableValue=id.getData().doubleValue();
										lhs=lhs+value1*variableValue;
									}
								}
							}
							Number value=ee.getValue().getData();
							double value1=value.doubleValue();
							if (value1>0){
								goalString=goalString+"+"+df.format(value)+ec.getSign()+"0";
							}else if(value1<0){
								goalString=goalString+df.format(value)+ec.getSign()+"0";
							}else{
								goalString=goalString+ec.getSign()+"0";
							}
							lhs=lhs+value1;
							FilterGoal fg = filterGoals.get(goalName);
							double tolerance=Double.parseDouble(fg.getTolerance());
							if (Math.abs(lhs)<=tolerance && hasData){
								goalString=goalString+";"+"y"+System.getProperty("line.separator");
							}else{
								goalString=goalString+";"+"n"+System.getProperty("line.separator");
							}
							filterGoalBuffer.write(goalString);
						}
					}
				}
				filterGoalBuffer.close();
			}catch (IOException e){
				return "failed";
			}
		}else{
			return "failed";
		}
		return filtergoal;
	}
	
	public boolean procFilterFile(String filterName){
		boolean doFilter=true;
		filterGoals=new HashMap<String, FilterGoal>();
		filterGoalNames=new ArrayList<String>();
		File filterFile=new File(filterName);
		if (filterFile.exists()){
			doFilter=true;
			try {
				FileReader fr = new FileReader(filterFile);
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				if (line != null){
					while ((line = br.readLine()) != null) {
						line=line.trim().toLowerCase();
						String[] filterParts=line.split(",");
						int length=filterParts.length;
						FilterGoal fg=new FilterGoal();
						if (length==3){
							fg.setAlias(filterParts[1]);
							fg.setTolerance(filterParts[2]);
						}else if (length==2){
							fg.setAlias(filterParts[1]);
						}
						filterGoals.put(filterParts[0], fg);
						filterGoalNames.add(filterParts[0]);
					}
				}
				fr.close();
			} catch (Exception e) {
				e.printStackTrace();
				doFilter=false;
			}
		}else{
			doFilter=false;
		}
		return doFilter;
	}
	
	public String getAllVariableString(){
		String dataString="";
		ArrayList<String> allDataNames=new ArrayList<String>();
		ArrayList<String> allTsNames=new ArrayList<String>();
		ArrayList<String> allDvNames=new ArrayList<String>();
		ArrayList<String> allSvNames=new ArrayList<String>();
		ArrayList<String> allAsNames=new ArrayList<String>();
		ArrayList<String> allParameterNames=new ArrayList<String>();
		Map<String, Number> allData=new HashMap<String, Number>();
		StudyDataSet sds = ControlData.currStudyDataSet;
		String modelName=sds.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds=sds.getModelDataSetMap().get(modelName);
		Map<String, Timeseries> tsMap = mds.tsMap;
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, Svar> svMap = mds.svMap;
		Map<String, Svar> svFutMap = mds.svFutMap;
		Map<String, Alias> asMap = mds.asMap;
		Map<String, Svar> parameterMap = sds.getParameterMap();
		Map<String, String> partsMap=new HashMap<String, String>(); 

		IntDouble intDouble;
		
		Set<String> tsKeySet = tsMap.keySet();
		Iterator<String> tsIterator = tsKeySet.iterator();
		while (tsIterator.hasNext()){
			String tsName=tsIterator.next();
			if (!allDataNames.contains(tsName)){
				Timeseries ts = tsMap.get(tsName);
				intDouble=ts.getData();
				if (intDouble != null){
					allDataNames.add(tsName);
					allTsNames.add(tsName);
					allData.put(tsName, intDouble.getData());
					partsMap.put(tsName, ts.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> dvKeySet = dvMap.keySet();
		Iterator<String> dvIterator = dvKeySet.iterator();
		while (dvIterator.hasNext()){
			String dvName=dvIterator.next();
			if (!allDataNames.contains(dvName)){
				Dvar dv = dvMap.get(dvName);
				intDouble=dv.getData();
				if (intDouble !=null){
					allDataNames.add(dvName);
					allDvNames.add(dvName);
					allData.put(dvName, intDouble.getData());
					partsMap.put(dvName, dv.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> svKeySet = svMap.keySet();
		Iterator<String> svIterator = svKeySet.iterator();
		while (svIterator.hasNext()){
			String svName=svIterator.next();
			if (!allDataNames.contains(svName)){
				Svar sv = svMap.get(svName);
				intDouble=sv.getData();
				if (intDouble!=null){
					allDataNames.add(svName);
					allSvNames.add(svName);
					allData.put(svName, intDouble.getData());
					partsMap.put(svName, sv.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> svFutKeySet = svFutMap.keySet();
		Iterator<String> svFutIterator = svFutKeySet.iterator();
		while (svFutIterator.hasNext()){
			String svFutName=svFutIterator.next();
			if (!allDataNames.contains(svFutName)){
				Svar svFut = svFutMap.get(svFutName);
				intDouble=svFut.getData();
				if (intDouble!=null){
					allDataNames.add(svFutName);
					allSvNames.add(svFutName);
					allData.put(svFutName, intDouble.getData());
					partsMap.put(svFutName, svFut.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> asKeySet = asMap.keySet();
		Iterator<String> asIterator = asKeySet.iterator();
		while (asIterator.hasNext()){
			String asName=asIterator.next();
			if (!allDataNames.contains(asName)){
				Alias as = asMap.get(asName);
				intDouble=as.getData();
				if (intDouble!=null){
					allDataNames.add(asName);
					allData.put(asName, intDouble.getData());
					partsMap.put(asName, as.kind+":"+ControlData.timeStep);
				}
			}
			if (!allAsNames.contains(asName)){
				allAsNames.add(asName);
			}
		}
		
		Set<String> parameterKeySet = parameterMap.keySet();
		Iterator<String> parameterIterator = parameterKeySet.iterator();
		while (parameterIterator.hasNext()){
			String parameterName=parameterIterator.next();
			if (!allDataNames.contains(parameterName)){
				Svar parameter = parameterMap.get(parameterName);
				intDouble=parameter.getData();
				if (intDouble!=null){
					allDataNames.add(parameterName);
					allParameterNames.add(parameterName);
					allData.put(parameterName, intDouble.getData());
					partsMap.put(parameterName, parameter.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Collections.sort(allDataNames, String.CASE_INSENSITIVE_ORDER);
		for (String variable: allDataNames){
			dataString=dataString+variable+":"+df.format(allData.get(variable))+"#";
		}
		
		dataString=dataString+"&";
		for (String variable: partsMap.keySet()){
			dataString=dataString+variable+":"+partsMap.get(variable)+"#";
		}
		
		dataString=dataString+"&";
		
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
				dataString=dataString+weightName+":"+df.format(weightMap.get(weightName).getValue())+"#";
			}
		}
		Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
		CopyOnWriteArrayList<String>  usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
		Iterator<String> usedWeightSlackSurplusIterator = usedWeightSlackSurplusCollection.iterator();
	
		while(usedWeightSlackSurplusIterator.hasNext()){
			String usedWeightSlackSurplusName=(String)usedWeightSlackSurplusIterator.next();
			dataString=dataString+usedWeightSlackSurplusName+":"+df.format(weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue())+"#";
		}
		
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		return dataString;
	}
	
	public String getAllVariableFiles(){
		String allDataString="alldataretrieved";
		ArrayList<String> allDataNames=new ArrayList<String>();
		ArrayList<String> allTsNames=new ArrayList<String>();
		ArrayList<String> allDvNames=new ArrayList<String>();
		ArrayList<String> allSvNames=new ArrayList<String>();
		ArrayList<String> allAsNames=new ArrayList<String>();
		ArrayList<String> allParameterNames=new ArrayList<String>();
		Map<String, Number> allData=new HashMap<String, Number>();
		StudyDataSet sds = ControlData.currStudyDataSet;
		String modelName=sds.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds=sds.getModelDataSetMap().get(modelName);
		Map<String, Timeseries> tsMap = mds.tsMap;
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, Svar> svMap = mds.svMap;
		Map<String, Svar> svFutMap = mds.svFutMap;
		Map<String, Alias> asMap = mds.asMap;
		Map<String, Svar> parameterMap = sds.getParameterMap();
		Map<String, String> partsMap=new HashMap<String, String>(); 

		IntDouble intDouble;
		
		Set<String> tsKeySet = tsMap.keySet();
		Iterator<String> tsIterator = tsKeySet.iterator();
		while (tsIterator.hasNext()){
			String tsName=tsIterator.next();
			if (!allDataNames.contains(tsName)){
				Timeseries ts = tsMap.get(tsName);
				intDouble=ts.getData();
				if (intDouble != null){
					allDataNames.add(tsName);
					allTsNames.add(tsName);
					allData.put(tsName, intDouble.getData());
					partsMap.put(tsName, ts.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> dvKeySet = dvMap.keySet();
		Iterator<String> dvIterator = dvKeySet.iterator();
		while (dvIterator.hasNext()){
			String dvName=dvIterator.next();
			if (!allDataNames.contains(dvName)){
				Dvar dv = dvMap.get(dvName);
				intDouble=dv.getData();
				if (intDouble !=null){
					allDataNames.add(dvName);
					allDvNames.add(dvName);
					allData.put(dvName, intDouble.getData());
					partsMap.put(dvName, dv.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> svKeySet = svMap.keySet();
		Iterator<String> svIterator = svKeySet.iterator();
		while (svIterator.hasNext()){
			String svName=svIterator.next();
			if (!allDataNames.contains(svName)){
				Svar sv = svMap.get(svName);
				intDouble=sv.getData();
				if (intDouble!=null){
					allDataNames.add(svName);
					allSvNames.add(svName);
					allData.put(svName, intDouble.getData());
					partsMap.put(svName, sv.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> svFutKeySet = svFutMap.keySet();
		Iterator<String> svFutIterator = svFutKeySet.iterator();
		while (svFutIterator.hasNext()){
			String svFutName=svFutIterator.next();
			if (!allDataNames.contains(svFutName)){
				Svar svFut = svFutMap.get(svFutName);
				intDouble=svFut.getData();
				if (intDouble!=null){
					allDataNames.add(svFutName);
					allSvNames.add(svFutName);
					allData.put(svFutName, intDouble.getData());
					partsMap.put(svFutName, svFut.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		Set<String> asKeySet = asMap.keySet();
		Iterator<String> asIterator = asKeySet.iterator();
		while (asIterator.hasNext()){
			String asName=asIterator.next();
			if (!allDataNames.contains(asName)){
				Alias as = asMap.get(asName);
				intDouble=as.getData();
				if (intDouble!=null){
					allDataNames.add(asName);
					allData.put(asName, intDouble.getData());
					partsMap.put(asName, as.kind+":"+ControlData.timeStep);
				}
			}
			if (!allAsNames.contains(asName)){
				allAsNames.add(asName);
			}
		}
		
		Set<String> parameterKeySet = parameterMap.keySet();
		Iterator<String> parameterIterator = parameterKeySet.iterator();
		while (parameterIterator.hasNext()){
			String parameterName=parameterIterator.next();
			if (!allDataNames.contains(parameterName)){
				Svar parameter = parameterMap.get(parameterName);
				intDouble=parameter.getData();
				if (intDouble!=null){
					allDataNames.add(parameterName);
					allParameterNames.add(parameterName);
					allData.put(parameterName, intDouble.getData());
					partsMap.put(parameterName, parameter.kind+":"+ControlData.timeStep);
				}
			}
		}
		
		try{
			File dataFolder= new File(dataDir);
			dataFolder.mkdirs();
			File allDataFile = new File(dataFolder, "allvariables.dat");
			FileOutputStream allDataStream = new FileOutputStream(allDataFile);
			OutputStreamWriter allDataWriter = new OutputStreamWriter(allDataStream);    
			BufferedWriter allDataBuffer = new BufferedWriter(allDataWriter);

			Collections.sort(allDataNames, String.CASE_INSENSITIVE_ORDER);
			for (String variable: allDataNames){
				allDataBuffer.write(variable+":"+df.format(allData.get(variable))+System.getProperty("line.separator"));
			}
			allDataBuffer.close();
		
			dataFolder.mkdirs();
			File allPartFile = new File(dataFolder, "allparts.dat");
        	FileOutputStream allPartStream = new FileOutputStream(allPartFile);
        	OutputStreamWriter allPartWriter = new OutputStreamWriter(allPartStream);    
        	BufferedWriter allPartBuffer = new BufferedWriter(allPartWriter);

			for (String variable: partsMap.keySet()){
				allPartBuffer.write(variable+":"+partsMap.get(variable)+System.getProperty("line.separator"));
			}
			allPartBuffer.close();
		
			File allWeightFile = new File(dataFolder, "allweights.dat");
        	FileOutputStream allWeightStream = new FileOutputStream(allWeightFile);
        	OutputStreamWriter allWeightWriter = new OutputStreamWriter(allWeightStream);    
        	BufferedWriter allWeightBuffer = new BufferedWriter(allWeightWriter);

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
					allWeightBuffer.write(weightName+":"+df.format(weightMap.get(weightName).getValue())+System.getProperty("line.separator"));
				}
			}
			Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
			CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
			Iterator<String> usedWeightSlackSurplusIterator = usedWeightSlackSurplusCollection.iterator();
	
			while(usedWeightSlackSurplusIterator.hasNext()){
				String usedWeightSlackSurplusName=(String)usedWeightSlackSurplusIterator.next();
				allWeightBuffer.write(usedWeightSlackSurplusName+":"+df.format(weightSlackSurplusMap.get(usedWeightSlackSurplusName).getValue())+System.getProperty("line.separator"));
			}
			allWeightBuffer.close();
		}catch (IOException e){
			allDataString="failed";
		}
		return allDataString;
	}
	
	public String getAllGoalString(){
		String goalString="";
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Set<String> goalKeySet=gMap.keySet();
		ArrayList<String> gKeyArrayList=new ArrayList<String>();
		Iterator ki=goalKeySet.iterator();
		while (ki.hasNext()){
			gKeyArrayList.add((String) ki.next());
		}
		Collections.sort(gKeyArrayList, String.CASE_INSENSITIVE_ORDER);
		for (int i=0; i<gKeyArrayList.size(); i++){
			String goalName=gKeyArrayList.get(i);
			goalString=goalString+goalName+":";
			EvalConstraint ec=gMap.get(goalName);
			if (ec!=null){
				EvalExpression ee=ec.getEvalExpression();
				Map<String, IntDouble> multiplier = ee.getMultiplier();
				Set<String> mKeySet = multiplier.keySet();
				Iterator<String> mi = mKeySet.iterator();
				while (mi.hasNext()){
					String variable=mi.next();
					Number value=multiplier.get(variable).getData();
					double value1=value.doubleValue();
					if (goalString.endsWith(":")){
						if (value1==1.0){
							goalString=goalString+variable;
						}else if (value1==-1.0){
							goalString=goalString+"-"+variable;
						}else{
							goalString=goalString+df.format(value)+variable;
						}
					}else{
						if (value1==1.0){
							goalString=goalString+"+"+variable;
						}else if (value1 == -1.0){
							goalString=goalString+"-"+variable;
						}else if(value1>=0){
							goalString=goalString+"+"+df.format(value)+variable;
						}else{
							goalString=goalString+df.format(value)+variable;
						}
					}
				}
				Number value=ee.getValue().getData();
				double value1=value.doubleValue();
				if (value1>0){
					goalString=goalString+"+"+df.format(value)+ec.getSign()+"0#";
				}else if(value1<0){
					goalString=goalString+df.format(value)+ec.getSign()+"0#";
				}else{
					goalString=goalString+ec.getSign()+"0#";
				}
			}
		}
		if (goalString.endsWith("#")) goalString=goalString.substring(0, goalString.length()-1);
		return goalString;
	}
	
	public String getAllGoalFile(){
		String allgoal="allgoalretrieved";
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Set<String> goalKeySet=gMap.keySet();
		ArrayList<String> gKeyArrayList=new ArrayList<String>();
		Iterator ki=goalKeySet.iterator();
		while (ki.hasNext()){
			gKeyArrayList.add((String) ki.next());
		}
		Collections.sort(gKeyArrayList, String.CASE_INSENSITIVE_ORDER);
		
		try{
			File dataFolder= new File(dataDir);
			dataFolder.mkdirs();
			File allGoalFile = new File(dataFolder, "allgoals.dat");
			FileOutputStream allGoalStream = new FileOutputStream(allGoalFile);
			OutputStreamWriter allGoalWriter = new OutputStreamWriter(allGoalStream);    
			BufferedWriter allGoalBuffer = new BufferedWriter(allGoalWriter);
			
			for (int i=0; i<gKeyArrayList.size(); i++){
				String goalName=gKeyArrayList.get(i);
				String goalString=goalName+":";
				EvalConstraint ec=gMap.get(goalName);
				if (ec!=null){
					EvalExpression ee=ec.getEvalExpression();
					Map<String, IntDouble> multiplier = ee.getMultiplier();
					Set<String> mKeySet = multiplier.keySet();
					Iterator<String> mi = mKeySet.iterator();
					while (mi.hasNext()){
						String variable=mi.next();
						Number value=multiplier.get(variable).getData();
						double value1=value.doubleValue();
						if (goalString.endsWith(":")){
							if (value1==1.0){
								goalString=goalString+variable;
							}else if (value1==-1.0){
								goalString=goalString+"-"+variable;
							}else{
								goalString=goalString+df.format(value)+variable;
							}
						}else{
							if (value1==1.0){
								goalString=goalString+"+"+variable;
							}else if (value1 == -1.0){
								goalString=goalString+"-"+variable;
							}else if(value1>=0){
								goalString=goalString+"+"+df.format(value)+variable;
							}else{
								goalString=goalString+df.format(value)+variable;
							}
						}
					}
					Number value=ee.getValue().getData();
					double value1=value.doubleValue();
					if (value1>0){
						goalString=goalString+"+"+df.format(value)+ec.getSign()+"0"+System.getProperty("line.separator");
					}else if(value1<0){
						goalString=goalString+df.format(value)+ec.getSign()+"0"+System.getProperty("line.separator");
					}else{
						goalString=goalString+ec.getSign()+"0"+System.getProperty("line.separator");
					}
					allGoalBuffer.write(goalString);
				}
			}
			allGoalBuffer.close();
		}catch (IOException e){
			return "failed";
		}
		return allgoal;
	}
	
	public String getWatch(String vGNameString){
		String dataString="";
		StudyDataSet sds=ControlData.currStudyDataSet;
		String modelName=sds.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds=sds.getModelDataSetMap().get(modelName);
		Map<String, Svar> parameterMap = sds.getParameterMap();
		Map<String, Timeseries> tsMap = mds.tsMap;
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, Svar> svMap = mds.svMap;
		Map<String, Svar> svFutMap = mds.svFutMap;
		Map<String, Alias> asMap = mds.asMap;
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Map<String, String> partsMap=new HashMap<String, String>();
		
		String[] vGNames;
		if (vGNameString.contains("#")){
			vGNames=vGNameString.split("#");
		}else{
			vGNames=new String[1];
			vGNames[0]=vGNameString;
		}
		ArrayList<String> controlGoals=new ArrayList<String>();
		IntDouble intDouble;
		for (int i=0; i<vGNames.length; i++){
			String vGName=vGNames[i];
			if (svMap.containsKey(vGName)){
				Svar sv = svMap.get(vGName);
				intDouble=sv.getData();
				if (intDouble != null) {
					dataString=dataString+vGName+":"+df.format(intDouble.getData())+"#";
					partsMap.put(vGName, sv.kind+":"+ControlData.timeStep);
				}
			}else if (dvMap.containsKey(vGName)){
				Dvar dv = dvMap.get(vGName);
				intDouble=dv.getData();
				if (intDouble != null) {
					dataString=dataString+vGName+":"+df.format(intDouble.getData())+"#";
					partsMap.put(vGName, dv.kind+":"+ControlData.timeStep);
				}
			}else if (tsMap.containsKey(vGName)){
				Timeseries ts = tsMap.get(vGName);
				intDouble=ts.getData();
				if (intDouble != null) {
					dataString=dataString+vGName+":"+df.format(intDouble.getData())+"#";
					partsMap.put(vGName, ts.kind+":"+ControlData.timeStep);
				}
			}else if (asMap.containsKey(vGName)){
				Alias as = asMap.get(vGName);
				intDouble=as.getData();
				if (intDouble != null) {
					dataString=dataString+vGName+":"+df.format(intDouble.getData())+"#";
					partsMap.put(vGName, as.kind+":"+ControlData.timeStep);
				}
			}else if(parameterMap.containsKey(vGName)){
				Svar sv=parameterMap.get(vGName);
				intDouble=sv.getData();
				if (intDouble !=null) {
					dataString=dataString+vGName+":"+df.format(intDouble.getData())+"#";
					partsMap.put(vGName, sv.kind+":"+ControlData.timeStep);
				}
			}else if (gMap.containsKey(vGName)){
				double lhs=0;
				dataString=dataString+vGName+":";
				EvalConstraint ec=gMap.get(vGName);
				if (ec!=null){
					EvalExpression ee=ec.getEvalExpression();
					Map<String, IntDouble> multiplier = ee.getMultiplier();
					Set<String> mKeySet = multiplier.keySet();
					Iterator<String> mi = mKeySet.iterator();
					boolean hasData = true;
					while (mi.hasNext()){
						String variable=mi.next();
						Number value=multiplier.get(variable).getData();
						double value1=value.doubleValue();
						if (dataString.endsWith(":")){
							if (value1==1.0){
								dataString=dataString+variable;
							}else if (value1==-1.0){
								dataString=dataString+"-"+variable;
							}else{
								dataString=dataString+df.format(value)+variable;
							}
						}else{
							if (value1==1.0){
								dataString=dataString+"+"+variable;
							}else if (value1 == -1.0){
								dataString=dataString+"-"+variable;
							}else if(value1>=0){
								dataString=dataString+"+"+df.format(value)+variable;
							}else{
								dataString=dataString+df.format(value)+variable;
							}
						}
						if (!(variable.startsWith("surplus__") || variable.startsWith("slack__"))){
							IntDouble id = dvMap.get(variable).getData();
							if (id==null){
								hasData=false;
							}else{
								double variableValue=id.getData().doubleValue();
								lhs=lhs+value1*variableValue;
							}
						}
					}
					Number value=ee.getValue().getData();
					double value1=value.doubleValue();
					if (value1>0){
						dataString=dataString+"+"+df.format(value)+ec.getSign()+"0#";
					}else if(value1<0){
						dataString=dataString+df.format(value)+ec.getSign()+"0#";
					}else{
						dataString=dataString+ec.getSign()+"0#";
					}
					lhs=lhs+value1;
					if (Math.abs(lhs)<=0.00001 && hasData){
						controlGoals.add(vGName);
					}
				}
			}else{
				dataString=dataString+vGName+":"+"N/A#";
			}
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		
		dataString=dataString+"!";
		for (String controlGoal:controlGoals){
			dataString=dataString+controlGoal+":";
		}
		if (dataString.endsWith(":")) dataString=dataString.substring(0, dataString.length()-1);
		
		dataString=dataString+"&";
		for (String variable: partsMap.keySet()){
			dataString=dataString+variable+":"+partsMap.get(variable)+"#";
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		
		return dataString;
	}
	
	public String getAllControlGoalName(){
		String goalNames="";
		Map<String, EvalConstraint> gMap = SolverData.getConstraintDataMap();
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Set<String> goalKeySet=gMap.keySet();
		Iterator ki=goalKeySet.iterator();
		while (ki.hasNext()){
			double lhs=0.0;
			String goalName=(String)ki.next();
			EvalConstraint ec=gMap.get(goalName);
			if (ec!=null){
				EvalExpression ee=ec.getEvalExpression();
				Map<String, IntDouble> multiplier = ee.getMultiplier();
				Set<String> mKeySet = multiplier.keySet();
				Iterator<String> mi = mKeySet.iterator();
				boolean hasData = true;
				while (mi.hasNext()){
					String variable=mi.next();
					double value1=multiplier.get(variable).getData().doubleValue();
					if (!(variable.startsWith("surplus__") || variable.startsWith("slack__"))){
						IntDouble id = dvMap.get(variable).getData();
						if (id ==null){
							hasData=false;
						}else{
							double variableValue=id.getData().doubleValue();
							lhs=lhs+variableValue*value1;
						}
					}
				}
				double value1=ee.getValue().getData().doubleValue();
				lhs=lhs+value1;
				if (Math.abs(lhs)<=0.00001 && hasData) goalNames=goalNames+goalName+":";
			}
		}
		if (goalNames.endsWith(":")) goalNames=goalNames.substring(0, goalNames.length()-1);
		return goalNames;
	}
	
	public String getTimeseriesDetail(String variableNames){
		monitorVarNames=variableNames.split("#");
		monitorVarTimeStep=ControlData.timeStep;
		controllerDebug.updateVarMonitor();
		
		String dataString="";
		String variableName=monitorVarNames[0];
		String entryName=DssOperation.entryNameTS(variableName, ControlData.timeStep);
		HashMap<String, DssDataSetFixLength> dvAliasTSMap = DataTimeSeries.dvAliasTS;

		if (dvAliasTSMap.containsKey(entryName)){
			DssDataSetFixLength ddsf = dvAliasTSMap.get(entryName);
			double[] dataArray = ddsf.getData();
			ParallelVars prvs = TimeOperation.findTime(0);
			int currIndex;
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)){
				currIndex=ValueEvaluation.timeSeriesIndex(ddsf, prvs);
			}else{
				currIndex=ValueEvaluation.timeSeriesIndex(ddsf, prvs)-1;
			}		
			for (int i=0; i<=currIndex; i++){
				double value;
				value=dataArray[i];
				if (!(value==-901.0 || value==-902.0)){
					int timestepListed=i-currIndex;
					prvs = TimeOperation.findTime(timestepListed);
					dataString=dataString+timestepListed+":"+prvs.dataMonth+"-"+prvs.dataDay+"-"+prvs.dataYear+":"+df.format(value)+"#";
				}
			}
		}else{
			HashMap<String, DssDataSet> svTSMap = DataTimeSeries.svTS;
			if (svTSMap.containsKey(entryName)){
				DssDataSet dds = svTSMap.get(entryName);
				ArrayList<Double> dataArrayList = dds.getData();
				ParallelVars prvs = TimeOperation.findTime(0);
				int currIndex=ValueEvaluation.timeSeriesIndex(dds, prvs);
				for (int i=0; i<dataArrayList.size(); i++){
					double value=dataArrayList.get(i);
					if (!(value==-901.0 || value==-902.0)){
						int timestepListed=i-currIndex;
						prvs = TimeOperation.findTime(timestepListed);
						dataString=dataString+timestepListed+":"+prvs.dataMonth+"-"+prvs.dataDay+"-"+prvs.dataYear+":"+df.format(value)+"#";
					}
				}
			}
		}
		
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		return dataString;
	}
	
	public String getFutureValueDetail(String variableName){
		String dataString="";
		StudyDataSet sds = ControlData.currStudyDataSet;
		String cycleName = sds.getModelList().get(ControlData.currCycleIndex);
		ModelDataSet mds = sds.getModelDataSetMap().get(cycleName);
		ArrayList<Integer> indexList=new ArrayList<Integer>();
		Map<Integer, Number> futureArray= new HashMap<Integer, Number>();
		if (mds.svList.contains(variableName)){
			Map<String, Svar> svFutMap = mds.svFutMap;
			Set<String> keySet=svFutMap.keySet();
			for (String key:keySet){
				if (key.startsWith(variableName+"__fut__")){
					String[] splittedKey=key.split("__fut__");
					if (splittedKey.length>1){
						int index = Integer.parseInt(splittedKey[1]);
						IntDouble intDouble=svFutMap.get(key).getData();
						if (intDouble != null) {
							indexList.add(index);
							futureArray.put(index, intDouble.getData());
						}
					}
				}
			}
		}else if (mds.dvList.contains(variableName)){
			Map<String, Dvar> dvMap = SolverData.getDvarMap();
			Set<String> keySet=dvMap.keySet();
			for (String key:keySet){
				if (key.startsWith(variableName+"__fut__")){
					String[] splittedKey=key.split("__fut__");
					if (splittedKey.length>1){
						int index = Integer.parseInt(splittedKey[1]);
						IntDouble intDouble=dvMap.get(key).getData();
						if (intDouble != null){
							indexList.add(index);
							futureArray.put(index, intDouble.getData());
						}
					}
				}
			}
		}
		Collections.sort(indexList);
		for (int i=0; i<indexList.size(); i++){
			Integer index=indexList.get(i);
			ParallelVars prvs = TimeOperation.findTime(index);
			dataString=dataString+index+":"+prvs.dataMonth+"-"+prvs.dataDay+"-"+prvs.dataYear+":"+df.format(futureArray.get(index))+"#";
		}
		
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		return dataString;
	}
	
	public String getCycleValueDetail(String variableName){
		String dataString="";
		StudyDataSet sds = ControlData.currStudyDataSet;
		ArrayList<String> modelTimeStepList = sds.getModelTimeStepList();
		ArrayList<String> ml = sds.getModelList();
		ArrayList<HashMap<String, DssDataSetFixLength>> dvAliasTSCycles = DataTimeSeries.dvAliasTSCycles;
		boolean variableFound=false;
		for (int cycleIndex=1; cycleIndex<=ControlData.currCycleIndex+1; cycleIndex++){
			String entryNameTS=DssOperation.entryNameTS(variableName, modelTimeStepList.get(cycleIndex-1));
			HashMap<String, DssDataSetFixLength> dvAliasTSCycle = dvAliasTSCycles.get(cycleIndex-1);
			if (dvAliasTSCycle.containsKey(entryNameTS)){
				variableFound=true;
				DssDataSetFixLength dds = dvAliasTSCycle.get(entryNameTS);
				int index=ControlData.currTimeStep.get(cycleIndex-1);
				if (dds!=null){
					double[] data=dds.getData();
					String cycle=ml.get(cycleIndex-1);
					if (cycleIndex==ControlData.currCycleIndex+1){
						dataString=dataString+cycleIndex+":"+cycle+":"+df.format(data[index])+"#";;
					}else{
						if (index>=1){
							dataString=dataString+cycleIndex+":"+cycle+":"+df.format(data[index-1])+"#";;
						}
					}
				}
			}
		}
		if (!variableFound){
			Map<String, Map<String, IntDouble>> varCycleValue = sds.getVarCycleValueMap();
			Map<String, Map<String, IntDouble>> varCycleIndexValue = sds.getVarCycleIndexValueMap();
			if (varCycleValue.containsKey(variableName)){
				Map<String, IntDouble> cycleValue = varCycleValue.get(variableName);
				int cycleIndex=1;
				for (String cycle: ml){
					if (cycleIndex<=ControlData.currCycleIndex+1){
						if (cycleValue.containsKey(cycle)){
							IntDouble id=cycleValue.get(cycle);
							if (id!=null) dataString=dataString+cycleIndex+":"+cycle+":"+df.format(id.getData())+"#";
						}else if (varCycleIndexValue.containsKey(variableName)){
							Map<String, IntDouble> cycleIndexValue = varCycleIndexValue.get(variableName);
							if (cycleIndexValue.containsKey(cycle)){
								IntDouble id=cycleIndexValue.get(cycle);
								if (id!=null) dataString=dataString+cycleIndex+":"+cycle+":"+df.format(id.getData())+"#";
							}
						}
					}
					cycleIndex=cycleIndex+1;
				}
			}else{
				Map<String, Map<String, IntDouble>> varTimeArrayCycleValue = sds.getVarTimeArrayCycleValueMap();
				if (varTimeArrayCycleValue.containsKey(variableName)){
					Map<String, IntDouble> cycleValue = varTimeArrayCycleValue.get(variableName);
					int cycleIndex=1;
					for (String cycle: ml){
						if (cycleIndex<=ControlData.currCycleIndex+1){
							if (cycleValue.containsKey(cycle)){
								IntDouble id=cycleValue.get(cycle);
								if (id!=null) dataString=dataString+cycleIndex+":"+cycle+":"+df.format(id.getData())+"#";
							}else if (varCycleIndexValue.containsKey(variableName)){
								Map<String, IntDouble> cycleIndexValue = varCycleIndexValue.get(variableName);
								if (cycleIndexValue.containsKey(cycle)){
									IntDouble id=cycleIndexValue.get(cycle);
									if (id!=null) dataString=dataString+cycleIndex+":"+cycle+":"+df.format(id.getData())+"#";
								}
							}
						}
						cycleIndex=cycleIndex+1;
					}
				}else{
					if (varCycleIndexValue.containsKey(variableName)){
						Map<String, IntDouble> cycleValue = varCycleIndexValue.get(variableName);
						int cycleIndex=1;
						for (String cycle: ml){
							if (cycleValue.containsKey(cycle) && cycleIndex<=ControlData.currCycleIndex+1){
								IntDouble id=cycleValue.get(cycle);
								if (id!=null) dataString=dataString+cycleIndex+":"+cycle+":"+df.format(id.getData())+"#";
							}
							cycleIndex=cycleIndex+1;
						}
					}
				}
			}
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		return dataString;
	}
	
	public boolean modifyTimeSeries(String[] requestParts){
		boolean isModified=false;
		String[] varStrings=requestParts[1].split("#");
		String varName=varStrings[0];
		int index=Integer.parseInt(varStrings[1]);
		double value=Double.parseDouble(varStrings[2]);
		String entryName=DssOperation.entryNameTS(varName, monitorVarTimeStep);
		HashMap<String, DssDataSetFixLength> dvAliasTSMap = DataTimeSeries.dvAliasTS;
		if (dvAliasTSMap.containsKey(entryName)){
			DssDataSetFixLength ddsf = dvAliasTSMap.get(entryName);
			double[] dataArray = ddsf.getData();
			ParallelVars prvs = TimeOperation.findTime(index);
			int tsIndex=ValueEvaluation.timeSeriesIndex(ddsf, prvs)-1;
			dataArray[tsIndex]=value;
			isModified=true;
		}else{
			HashMap<String, DssDataSet> svTSMap = DataTimeSeries.svTS;
			if (svTSMap.containsKey(entryName)){
				DssDataSet dds = svTSMap.get(entryName);
				ArrayList<Double> dataArrayList = dds.getData();
				ParallelVars prvs = TimeOperation.findTime(index);
				int tsIndex=ValueEvaluation.timeSeriesIndex(dds, prvs);
				dataArrayList.set(tsIndex, value);
				isModified=true;
			}
		}
		return isModified;
	}
	
	public boolean modifyCycle(String[] requestParts){
		boolean isModified=false;
		String[] varStrings=requestParts[1].split("#");
		String varName=varStrings[0];
		String cycle=varStrings[1];
		double value=Double.parseDouble(varStrings[2]);
		StudyDataSet sds = ControlData.currStudyDataSet;
		Map<String, Map<String, IntDouble>> varCycleValue = sds.getVarCycleValueMap();
		if (varCycleValue.containsKey(varName)){
			Map<String, IntDouble> cycleValue = varCycleValue.get(varName);
			if (cycleValue.containsKey(cycle)){
				cycleValue.put(cycle, new IntDouble(value, false));
				isModified=true;
			}
		}else{
			Map<String, Map<String, IntDouble>> varTimeArrayCycleValue = sds.getVarTimeArrayCycleValueMap();
			if (varTimeArrayCycleValue.containsKey(varName)){
				Map<String, IntDouble> cycleValue = varTimeArrayCycleValue.get(varName);
				cycleValue.put(cycle, new IntDouble(value, false));
				isModified=true;
			}
		}
		return isModified;
	}
	
	public String getDataString(){
		String dataString="";
		for (int i=0; i<allDebugVariables.length; i++){
			String variable=allDebugVariables[i];
			if (ControlData.currSvMap.containsKey(variable)){
				Number value=ControlData.currSvMap.get(variable).getData().getData();
				dataString=dataString+variable+":"+value+"#";
			}
			if (ControlData.currDvMap.containsKey(variable)){
				Number value=ControlData.currDvMap.get(variable).getData().getData();
				dataString=dataString+variable+":"+value+"#";
			}
			if (ControlData.currAliasMap.containsKey(variable)){
				Number value=ControlData.currAliasMap.get(variable).getData().getData();
				dataString=dataString+variable+":"+value+"#";
			}
		}
		if (dataString.endsWith("#")) dataString=dataString.substring(0, dataString.length()-1);
		return dataString;
	}
	
	public void setSolverOptions(String[] requestParts){
		String solverName=requestParts[1];
		String log=requestParts[2];
		new CloseCurrentSolver(ControlData.solverName);
		System.out.println("Change solver to "+solverName);
		if (solverName.equals("XA")){
			new InitialXASolver();
			if (log.equals("None")){
				SetXALog.disableXALog();
				ILP.logging=false;
				ILP.loggingLpSolve=false;
				ILP.loggingCplexLp=false;
				ILP.loggingVariableValue=false;
				ControlData.solverName="XA";
				System.out.println("Log file turn off");
			}else if (log.equals("Log")){
				SetXALog.enableXALog();
				ILP.logging=true;
				ILP.loggingLpSolve=false;
				ILP.loggingCplexLp=true;
				ILP.loggingVariableValue=true;
				ControlData.solverName="XALOG";
				ILP.initializeIlp();
				System.out.println("Log file turn on");
			}		
		}else if(solverName.equals("LPSolve")){
			ControlData.solverName="LPSolve";
			ILP.loggingLpSolve=true;
			ILP.loggingCplexLp=false;
			if (log.equals("None")){
				ILP.logging=false;
				ILP.loggingVariableValue=false;
				System.out.println("Log file turn off");
			}else if (log.equals("Log")){
				ILP.logging=true;
				ILP.loggingVariableValue=true;
				System.out.println("Log file turn on");
			}
			ILP.initializeIlp();
			ChangeSolver.loadLPSolveConfigFile();
		}else if(solverName.equals("Gurobi")){
			ControlData.solverName="Gurobi";
			ILP.loggingCplexLp=true;
			ILP.loggingLpSolve=false;
			if (log.equals("None")){
				ILP.logging=false;
				ILP.loggingVariableValue=false;
				System.out.println("Log file turn off");
			}else if (log.equals("Log")){
				ILP.logging=true;
				ILP.loggingVariableValue=true;
				System.out.println("Log file turn on");
			}
			ILP.getIlpDir();
			ILP.initializeIlp();
			GurobiSolver.initialize();
		}else if (solverName.startsWith("CBC")){
			if (solverName.equalsIgnoreCase("CBC2.9.8")){
				CbcSolver.cbcLibName="jCbc";
			}else if (solverName.equalsIgnoreCase("CBC2.10")){
				CbcSolver.cbcLibName="jCbc_v2.10";
			}else{
				CbcSolver.cbcLibName="jCbc";
			} 
			ControlData.solverName="CBC";
			ILP.loggingLpSolve=false;
			ILP.loggingCplexLp=true;
			ILP.loggingAllCycles=true;
			if (log.equals("None")){
				ILP.logging=false;
				ILP.loggingVariableValue=false;
				ControlData.cbc_debug_routeXA = false;
				ControlData.cbc_debug_routeCbc = false;
				System.out.println("Log file turned off");
			}else if (log.equals("Log")){
				ILP.logging=true;
				ILP.loggingVariableValue=true;
				ControlData.cbc_debug_routeXA = false;
				ControlData.cbc_debug_routeCbc = false;
				System.out.println("Log file turned on");
			}else if (log.equals("xa_cbc")){
				ILP.logging=true;
				ILP.loggingVariableValue=true;
				ControlData.cbc_debug_routeXA = false;	
				ControlData.cbc_debug_routeCbc = true;
				new InitialXASolver();
				System.out.println("CBC route turned on");
			}else if (log.equals("cbc_xa")){
				ILP.logging=true;
				ILP.loggingVariableValue=true;
				ControlData.cbc_debug_routeXA = true;
				ControlData.cbc_debug_routeCbc = false;
				new InitialXASolver();
				System.out.println("XA route turned on");
			}
			if (ControlData.useCbcWarmStart || ControlData.cbcCheckIntErr || ControlData.cbc_debug_routeCbc || ControlData.cbc_debug_routeXA){
				if (ControlData.solverName.equalsIgnoreCase("Cbc")  || ControlData.solverName.equalsIgnoreCase("Cbc1")){
					
					Warmstart.collectIntegerDV_2(ControlData.currStudyDataSet);			
				
				}
			}
			ILP.getIlpDir();
			ILP.initializeIlp();
			CbcSolver.init(false, ControlData.currStudyDataSet);
		}
	}
	
	public boolean saveInitDss(String fileName){
		/*
		DSSDataWriter writer = new DSSDataWriter(fileName);
		try {
			writer.openDSSFile();
		} catch (Exception e) {
			writer.closeDSSFile();
			return false;
		}
		*/
		HecDss dss;
		try {
			dss = HecDss.open(fileName);
		} catch (Exception e) {
			return false;
		}
		DssOperation.saveDVInitialData(dss, fileName);
		DssOperation.saveSVInitialData(dss, fileName);
		dss.close();
		return true;
	}
	
	public boolean saveSvDss(String fileName){
		HecDss dss;
		try {
			dss = HecDss.open(fileName);
			DssOperation.saveSvarTSData(dss, fileName);
			dss.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
		
	public boolean saveDvDss(String fileName){
		if (fileName.equalsIgnoreCase(FilePaths.fullDvarDssPath)){
			DssOperation.saveInitialData(ControlData.dvDss, fileName);
			DssOperation.saveDvarData(ControlData.dvDss, fileName);
			return true;
		}else{
			/*DSSDataWriter writer = new DSSDataWriter(fileName);
			try {
				writer.openDSSFile();
			} catch (Exception e) {
				writer.closeDSSFile();
				return false;
			}
			*/
			HecDss dss;
			try {
				dss = HecDss.open(fileName);
			} catch (Exception e) {
				return false;
			}
			DssOperation.saveInitialData(dss, fileName);
			DssOperation.saveDvarData(dss, fileName);
			dss.close();
			//writer.closeDSSFile();
			return true;
		}
	}
	
	private void setConditionalBreakpoint(String request) {
		String evalString;
		if (request.equals("conditional_breakpoint:")){
			controllerDebug.conditionalBreakpoint="";
			evalString="c: ";
		}else{
			String[] data=request.split(":");
			controllerDebug.conditionalBreakpoint=data[1].toLowerCase();
			evalString="c:"+controllerDebug.conditionalBreakpoint;
		}
		ANTLRStringStream stream = new ANTLRStringStream(evalString);
		ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
		TokenStream tokenStream = new CommonTokenStream(lexer);
		controllerDebug.conditionalBreakpointParser=new ValueEvaluatorParser(tokenStream);
	}	
	
	private void initOutputCycleDataToDss(){
		ControlData.isOutputCycle=true;
		ControlData.cycleDataStartYear=ControlData.currYear;
		ControlData.cycleDataStartMonth=ControlData.currMonth;
		ControlData.cycleDataStartDay=ControlData.currDay;
		//int totalCycleNumber=ControlData.currStudyDataSet.getModelList().size();
		//DataTimeSeries.dvAliasTSCycles=new ArrayList<HashMap<String, DssDataSetFixLength>>(totalCycleNumber);
		//for (int i=0; i<totalCycleNumber; i++){
		//	HashMap<String, DssDataSetFixLength> dvAliasTSCycle = new HashMap<String, DssDataSetFixLength>();
		//	DataTimeSeries.dvAliasTSCycles.add(dvAliasTSCycle);
		//}
	}
	
	public void setSelectedOutputCycles(){
		String strSelectedCycleOutput = ControlData.selectedCycleOutput.replace("\'", "");
		ControlData.selectedCycles = strSelectedCycleOutput.split(",");
	}
	
	public static void main(String[] args){
		int argsSize=args.length;
		String[] modArgs=new String[argsSize-2];
		for (int i=0; i<=argsSize-3; i++){
			modArgs[i]=args[i+2];
		}
		int requestPort=Integer.parseInt(args[0]);
		int eventPort=Integer.parseInt(args[1]);
		new DebugInterface(requestPort, eventPort, modArgs);
	}
}
