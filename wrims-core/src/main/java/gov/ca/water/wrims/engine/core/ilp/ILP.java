package gov.ca.water.wrims.engine.core.ilp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.components.BuildProps;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.Error;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;
import gov.ca.water.wrims.engine.core.solver.CbcSolver;
import gov.ca.water.wrims.engine.core.solver.LPSolveSolver;
import gov.ca.water.wrims.engine.core.solver.Gurobi.GurobiSolver;
import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModel;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;
import gov.ca.water.wrims.engine.core.wreslparser.elements.Tools;

// for LpSolve select 1.Rows 2.Cols 3.Elimeq2 in Presolve

public class ILP {

	private static String _mpModelDir;
	private static String _lpSolveDir;
	private static String _cplexLpDir;
	private static String _amplDir;
	public static String mpModelFilePath;
	public static String lpSolveFilePath;
	public static String cplexLpFilePath;
	public static String amplFilePath;
	private static ObjectOutputStream  _mpModelFile;
	private static PrintWriter _lpSolveFile;
	private static PrintWriter _cplexLpFile;
	private static StringWriter _cplexLpString;
	private static String cplexLpFileName;
	private static PrintWriter _amplFile;
	private static PrintWriter _svarFile;
	private static PrintWriter _dvarFile;
	private static PrintWriter _objValueFile;
	public static PrintWriter _noteFile;
	public static PrintWriter _noteFile_xa_obj;
	public static PrintWriter _noteFile_xa_int;
	public static PrintWriter _noteFile_cbc_obj;
	public static PrintWriter _noteFile_cbc_time;
	public static PrintWriter _noteFile_cbc_int;
	public static PrintWriter _noteFile_cbc_int_log;
	public static PrintWriter _noteFile_memory;
	public static PrintWriter _noteFile_timeusage;
	public static PrintWriter _watchFile_xa;
	public static PrintWriter _watchFile_cbc;
	private static Set<String> dvar_effective;
	private static final String lpSolve_comment_Symbol = "//";
	private static final String ampl_comment_Symbol = "#";
	private static final String cplexLp_comment_Symbol = "\\";
	private static DecimalFormat df;
	public static int maximumFractionDigits = 16;
	public static boolean loggingVariableValue = false;
	public static boolean loggingVariableValueRound = false;
	public static boolean loggingVariableValueRound10 = false;
	public static boolean loggingAllCycles = true;
	public static boolean logging = false;
	public static boolean loggingAmpl = false;
	public static boolean loggingCplexLp = false; // special LP problem file format designed by Cplex
	public static boolean loggingLpSolve = false; // special LP problem file format designed by LpSolve
	public static boolean loggingMPModel = false; // special LP problem binary file format designed for OR-Tools
	public static boolean loggingUsageMemeory = false;
	
	private static File _ilpRootDir;  
	private static File _ilpDir; 
	private static File _svarDir;
	private static File _dvarDir;
	
	public static ArrayList<String> intList = null;
	
	protected ILP() {
		
	}

	public static File getIlpDir() {
		_ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");  
	    _ilpDir = new File(_ilpRootDir.getAbsolutePath(), StudyUtils.configFileName); 
		return _ilpDir;
	}
	
	public static void createNoteFile() {
		try {
			_noteFile = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_"+ControlData.dateTimeAppend+".log");
			writeNoteLn("wrims2 revision:", new BuildProps().getVN());
			writeNoteLn("jCbc dll name:", CbcSolver.cbcLibName);
			if (ControlData.cbc_debug_routeXA || ControlData.cbc_debug_routeCbc) {
				_noteFile_xa_obj = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_xa_obj.log");
				_noteFile_cbc_obj = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_obj.log");
				_noteFile_cbc_time = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_time.log");
				_noteFile_xa_int = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_xa_int.log");
				_noteFile_cbc_int = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_int.log");
				
				if (ControlData.currStudyDataSet.cycIntDvMap!=null) {
					ILP.writeNote("cyc ", _noteFile_cbc_int);
					ILP.writeNote("cyc ", _noteFile_xa_int);
					for (String s:ControlData.currStudyDataSet.allIntDv ){
						ILP.writeNote( s+" ", _noteFile_cbc_int);
						ILP.writeNote( s+" ", _noteFile_xa_int);
					}
					ILP.writeNote("\r\n", _noteFile_cbc_int);
					ILP.writeNote("\r\n", _noteFile_xa_int);
				}
				
				if (ControlData.watchList!=null) {
					_watchFile_xa = Tools.openFile(_ilpDir.getAbsolutePath(), "Watch_XA.log");
					_watchFile_cbc = Tools.openFile(_ilpDir.getAbsolutePath(), "Watch_CBC.log");
					ILP.writeNote( "time  ", _watchFile_xa);
					ILP.writeNote( "time  ", _watchFile_cbc);
					for (String s:ControlData.watchList) {
						ILP.writeNote( s+"  ", _watchFile_xa);
						ILP.writeNote( s+"  ", _watchFile_cbc);
					}
					ILP.writeNote("\r\n", _watchFile_xa);
					ILP.writeNote("\r\n", _watchFile_cbc);
				}
				
			} else if (CbcSolver.logObj && ControlData.solverName.equalsIgnoreCase("Cbc")){
				_noteFile_cbc_obj = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_obj.log");
				_noteFile_cbc_time = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_time.log");
				_watchFile_cbc = Tools.openFile(_ilpDir.getAbsolutePath(), "Watch_CBC.log");
				ILP.writeNote( "time  ", _watchFile_cbc);
				for (String s:ControlData.watchList) {
					ILP.writeNote( s+"  ", _watchFile_cbc);
				}
				ILP.writeNote("\r\n", _watchFile_cbc);
			}  
			
			if (CbcSolver.intLog && ControlData.solverName.equalsIgnoreCase("Cbc")){

				_noteFile_cbc_int_log = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_cbc_int_check_"+ControlData.dateTimeAppend+".csv");
				if (ControlData.currStudyDataSet.cycIntDvMap!=null) {
					ILP.writeNote("cyc,int_violation,solver,", _noteFile_cbc_int_log);
					//ILP.writeNote("cyc ", _noteFile_xa_int);
					for (String s:ControlData.currStudyDataSet.allIntDv ){
						ILP.writeNote( s+",", _noteFile_cbc_int_log);
						//ILP.writeNote( s+" ", _noteFile_xa_int);
					}
					ILP.writeNote("\r\n", _noteFile_cbc_int_log);
					//ILP.writeNote("\r\n", _noteFile_xa_int);
				}
			}

			//new File(_ilpDir.getAbsolutePath()+"\\Note_memory.log").createNewFile();
			_noteFile_memory = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_memory.log");
			
			_noteFile_timeusage = Tools.openFile(_ilpDir.getAbsolutePath(), "Note_timeusage.log");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void initializeIlp() {
		
		_ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");  
	    _ilpDir = new File(_ilpRootDir.getAbsolutePath(), StudyUtils.configFileName); 
	    
		_mpModelDir = new File(_ilpDir, "mpmodel").getAbsolutePath();
		_lpSolveDir = new File(_ilpDir, "lpsolve").getAbsolutePath();
		_cplexLpDir = new File(_ilpDir, "cplexlp").getAbsolutePath();
		_amplDir = new File(_ilpDir, "ampl").getAbsolutePath();
		// TODO: write ampl command file "option presolve_eps 1e-13;"

		setVarDir();
		if (ILP.loggingVariableValue) setVarDir();
		setMaximumFractionDigits();
		createNoteFile();
	    try {
			_objValueFile = Tools.openFile(_ilpDir.getAbsolutePath(), "ObjValues.log");
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void setMaximumFractionDigits() {
		
		df = new DecimalFormat();
		//df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(maximumFractionDigits);
	}
	
	public static void setIlpFile() {

		if (ILP.loggingMPModel) setMPModelFile();
		if (ILP.loggingLpSolve) setLpSolveFile();
		if (ILP.loggingCplexLp) {
			if (ControlData.useCplexLpString && ControlData.solverType != Param.SOLVER_CBC0.intValue()
					&& ControlData.solverType != Param.SOLVER_CBC1.intValue()){
				setCplexLpString();
			} else {
				setCplexLpFile();
			}
		}
		if (ILP.loggingAmpl)    setAmplFile();		
	}

	public static void writeIlp() {
		
//		Set<String> dvar_inConstraint = findDvarInConstraint();
//		Set<String> dvar_weighted = findWeightedDvar();
//		//Set<String> dvar_unWeighted = findUnWeightedDvarInConstraint(dvar_inConstraint, dvar_weighted);
//		
//		dvar_effective = new HashSet<String>();
//		dvar_effective.addAll(dvar_weighted);
//		dvar_effective.addAll(dvar_inConstraint);
		
		findDvarEffective();
		//if (ILP.loggingMPModel) writeMPModelFile();
		if (ILP.loggingLpSolve) writeLpSolveFile();
		if (ILP.loggingCplexLp) writeCplexLpFile();
		if (ILP.loggingAmpl)    writeAmplFile();
		
	}

	public static void findDvarEffective(){
		
		Set<String> dvar_inConstraint = findDvarInConstraint();
		Set<String> dvar_weighted = findWeightedDvar();
		//Set<String> dvar_unWeighted = findUnWeightedDvarInConstraint(dvar_inConstraint, dvar_weighted);
		
		dvar_effective = new HashSet<String>();
		dvar_effective.addAll(dvar_weighted);
		dvar_effective.addAll(dvar_inConstraint);
		
	}
	
	public static void writeObjValue_XA() {
		
		double objValue = ControlData.xasolver.getObjective();
		String objValueStr = Double.toString(objValue);
		
		if (ILP.loggingLpSolve) writeObjValue(objValueStr, _lpSolveFile, lpSolve_comment_Symbol);
		if (ILP.loggingCplexLp) writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		if (ILP.loggingAmpl)    writeObjValue(objValueStr, _amplFile, ampl_comment_Symbol);
	    
		writeObjValueLog(getYearMonthCycle(), objValueStr, _objValueFile);
		
	}
	
	public static void writeObjValue_LPSOLVE() {

		double objValue = ControlData.lpsolve_objective;
		String objValueStr = Double.toString(objValue);

		writeObjValue(objValueStr, _lpSolveFile, lpSolve_comment_Symbol);
		if (ILP.loggingCplexLp) writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		if (ILP.loggingAmpl) writeObjValue(objValueStr, _amplFile, ampl_comment_Symbol);
		
		writeObjValueLog(getYearMonthCycle(), objValueStr, _objValueFile);

	}

	public static void writeObjValue_Clp0_Cbc0() {

		double objValue = ControlData.clp_cbc_objective;
		String objValueStr = Double.toString(objValue);

		writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		//if (ILP.loggingCplexLp) writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		//if (ILP.loggingAmpl) writeObjValue(objValueStr, _amplFile, ampl_comment_Symbol);
		
		writeObjValueLog(getYearMonthCycle(), objValueStr, _objValueFile);
		if (!ControlData.clp_cbc_note.isEmpty()) writeNoteLn(getYearMonthCycle(), ControlData.clp_cbc_note, _noteFile);

	}
	
	public static void writeObjValue_Gurobi() {

		double objValue = ControlData.gurobi_objective;
		String objValueStr = Double.toString(objValue);
		
		writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		if (ILP.loggingLpSolve) writeObjValue(objValueStr, _lpSolveFile, lpSolve_comment_Symbol);
		if (ILP.loggingAmpl)    writeObjValue(objValueStr, _amplFile, ampl_comment_Symbol);

		writeObjValueLog(getYearMonthCycle(), objValueStr, _objValueFile);

	}
	
	public static void writeObjValue_OrTools() {

		double objValue = ControlData.otsolver.solver.objectiveValue();
		String objValueStr = Double.toString(objValue);
		
		if (ILP.loggingCplexLp) writeObjValue(objValueStr, _cplexLpFile, cplexLp_comment_Symbol);
		if (ILP.loggingLpSolve) writeObjValue(objValueStr, _lpSolveFile, lpSolve_comment_Symbol);
		if (ILP.loggingAmpl)    writeObjValue(objValueStr, _amplFile, ampl_comment_Symbol);

		writeObjValueLog(getYearMonthCycle(), objValueStr, _objValueFile);

	}
	
	public static void writeMPModelFile(MPModel m) {
				
		try {
			_mpModelFile.writeObject(m);
			_mpModelFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	}
	private static void writeLpSolveFile() {
		
		_lpSolveFile.print(findHeaderStr(lpSolve_comment_Symbol));
		
		Map<String, WeightElement> activeWeightMap = findActiveWeightMap();
		
		LpSolveWriter.writeObj(_lpSolveFile, activeWeightMap);
		LpSolveWriter.writeConstraint(_lpSolveFile);
		
//		Set<String> dvar_inConstraint = findDvarInConstraint();
//		Set<String> dvar_weighted = findWeightedDvar();
//		//Set<String> dvar_unWeighted = findUnWeightedDvarInConstraint(dvar_inConstraint, dvar_weighted);
//		
//		dvar_effective = new HashSet<String>();
//		dvar_effective.addAll(dvar_weighted);
//		dvar_effective.addAll(dvar_inConstraint);
		
		//LpSolveWriter.writeDvar(_lpSolveFile, dvar_weighted, dvar_unWeighted);
		LpSolveWriter.writeDvar(_lpSolveFile, dvar_effective);	
		_lpSolveFile.flush();
	}
	private static void writeCplexLpFile() {
		
		_cplexLpFile.print(findHeaderStr(cplexLp_comment_Symbol));
		
		Map<String, WeightElement> activeWeightMap = findActiveWeightMap();
		
		CplexLpWriter.writeObj(_cplexLpFile, activeWeightMap);
		CplexLpWriter.writeConstraint(_cplexLpFile);
		
//		Set<String> dvar_inConstraint = findDvarInConstraint();
//		Set<String> dvar_weighted = findWeightedDvar();
//		//Set<String> dvar_unWeighted = findUnWeightedDvarInConstraint(dvar_inConstraint, dvar_weighted);
//		
//		dvar_effective = new HashSet<String>();
//		dvar_effective.addAll(dvar_weighted);
//		dvar_effective.addAll(dvar_inConstraint);
		
		CplexLpWriter.writeDvar(_cplexLpFile, dvar_effective);	
		_cplexLpFile.println("\nEnd");
		_cplexLpFile.flush();
		
	}
	private static void writeAmplFile() {
		
		_amplFile.print(findHeaderStr(ampl_comment_Symbol));
		
		Map<String, WeightElement> activeWeightMap = findActiveWeightMap();
		
//		Set<String> dvar_inConstraint = findDvarInConstraint();
//		Set<String> dvar_weighted = findWeightedDvar();
//		//Set<String> dvar_unWeighted = findUnWeightedDvarInConstraint(dvar_inConstraint, dvar_weighted);
//		
//		dvar_effective = new HashSet<String>();
//		dvar_effective.addAll(dvar_weighted);
//		dvar_effective.addAll(dvar_inConstraint);
		
		AmplWriter.writeDvar(_amplFile, dvar_effective);	
		AmplWriter.writeConstraint(_amplFile);
		AmplWriter.writeObj(_amplFile, activeWeightMap);
		_amplFile.flush();
	}
	
	public static void writeDvarValue_XA() {
		
		writeDvarValue_XA(_dvarFile, dvar_effective);
		_dvarFile.flush();
		
	}
	public static void writeDvarValue_LPSOLVE() {
		
		writeDvarValue_LPSOLVE(_dvarFile, dvar_effective);
		_dvarFile.flush();
		
	}
	public static void writeDvarValue_Clp0_Cbc0(Map <String, Double> varDoubleMap) {
		
		writeDvarValue_Clp0_Cbc0(_dvarFile, dvar_effective, varDoubleMap);
		_dvarFile.flush();
		
	}
	public static void writeDvarValue_Gurobi() {
		
		writeDvarValue_Gurobi(_dvarFile, dvar_effective);
		_dvarFile.flush();
		
	}
	public static void writeDvarValue_OrTools() {
		
		writeDvarValue_OrTools(_dvarFile, dvar_effective);
		_dvarFile.flush();
		
	}
	public static void writeSvarValue() {
		
		writeSvarValue(_svarFile);
		_svarFile.flush();
	}
	
	public static void closeIlpFile() {
	
		try {
			if (ILP.loggingMPModel) _mpModelFile.close();
			if (ILP.loggingLpSolve) _lpSolveFile.close();
			if (ILP.loggingCplexLp) _cplexLpFile.close();
			if (ILP.loggingAmpl)    _amplFile.close();
			if (ILP.loggingVariableValue) {
				_svarFile.close();
				_dvarFile.close();
			}
		} catch (Exception e) {

			// ignore
		}
	}

	private static void setMPModelFile() {
		
		String mpModelFileName;
		String twoDigitMonth = String.format("%02d", ControlData.currMonth);
		String twoDigitCycle = String.format("%02d", ControlData.currCycleIndex+1);
		
		mpModelFileName = ControlData.currYear + "_" + twoDigitMonth + "_c" + twoDigitCycle + ".mpm";		

		try {

			File dir = new File(_mpModelDir);
			dir.mkdirs();
			mpModelFilePath = new File(_mpModelDir, mpModelFileName).getAbsolutePath(); // for public access
			_mpModelFile = new ObjectOutputStream(new FileOutputStream(mpModelFilePath));

		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
	
	private static void setLpSolveFile() {
	
		String lpSolveFileName;
		String twoDigitMonth = String.format("%02d", ControlData.currMonth);
		String twoDigitCycle = String.format("%02d", ControlData.currCycleIndex+1);
		
		lpSolveFileName = ControlData.currYear + "_" + twoDigitMonth + "_c" + twoDigitCycle + ".lps";		

		try {
			
			_lpSolveFile = Tools.openFile(_lpSolveDir, lpSolveFileName);
			lpSolveFilePath = new File(_lpSolveDir, lpSolveFileName).getAbsolutePath(); // for public access
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
	
	public static void setVarDir(){
				
		_svarDir = new File(_ilpDir, "svar");
		_dvarDir = new File(_ilpDir, "dvar");
		
	}
	
	public static String getYearMonthCycle(){
		
		String twoDigitMonth = String.format("%02d", ControlData.currMonth);
		String twoDigitCycle = String.format("%02d", ControlData.currCycleIndex+1);
		String ret = ControlData.currYear + "_" + twoDigitMonth + "_c" + twoDigitCycle;
		
		return ret;
		
	}
	
	public static void setObjValueFile(){
				
		
		try {
			
			_objValueFile = Tools.openFile(_ilpDir.getAbsolutePath(), "objectiveValues.log");

		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static void setVarFile(){
		
		String svarFileName = getYearMonthCycle()+ ".svar";
		String dvarFileName = getYearMonthCycle()+ ".dvar";
		
		
		try {
			
			_svarFile = Tools.openFile(_svarDir.getAbsolutePath(), svarFileName);
			_dvarFile = Tools.openFile(_dvarDir.getAbsolutePath(), dvarFileName);
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static void setSvarFile(){
		
		String svarFileName = getYearMonthCycle()+ ".svar";		
		
		try {
			//_svarDir = new File(_ilpDir, "svar");
			_svarFile = Tools.openFile(_svarDir.getAbsolutePath(), svarFileName);
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	public static void setDvarFile(String append){
		
		String dvarFileName = getYearMonthCycle()+ ".dvar_"+append;
		
		try {
			
			_dvarFile = Tools.openFile(_dvarDir.getAbsolutePath(), dvarFileName);
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}	
	
	private static void setCplexLpFile() {
		
		cplexLpFileName = getYearMonthCycle()+".lp";		

		try {
			
			_cplexLpFile = Tools.openFile(_cplexLpDir, cplexLpFileName);
			cplexLpFilePath = new File(_cplexLpDir, cplexLpFileName).getAbsolutePath(); // for public access

		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
	
	private static void setCplexLpString() {
		
	
			
		//_cplexLpFile = Tools.openFile(_cplexLpDir, cplexLpFileName);
		_cplexLpString = new StringWriter();
		_cplexLpFile = new PrintWriter(new BufferedWriter(_cplexLpString));
		//cplexLpFilePath = new File(_cplexLpDir, cplexLpFileName).getAbsolutePath(); // for public access
	
	}

	public static void saveCplexLpStringToFile() {
		
		
		cplexLpFileName = getYearMonthCycle()+".lp";		

		try {
			
			_cplexLpFile = Tools.openFile(_cplexLpDir, cplexLpFileName);
			_cplexLpFile.print(_cplexLpString);
			_cplexLpFile.close();
			cplexLpFilePath = new File(_cplexLpDir, cplexLpFileName).getAbsolutePath(); // for public access

		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
	
	public static void closeCplexLpFile() {
		
		try {
			
			_cplexLpFile.close();

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			//ignore
		}
	
	}
	
	public static void reOpenCplexLpFile(boolean isAppend) {
		
		try {
			
			_cplexLpFile = Tools.openFile(_cplexLpDir, cplexLpFileName, isAppend);

		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
	
	private static void setAmplFile() {
		
		String amplFileName = getYearMonthCycle()+ ".mod";				

		try {
		
			_amplFile = Tools.openFile(_amplDir, amplFileName);	
			amplFilePath = new File(_amplDir, amplFileName).getAbsolutePath(); // for public access
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}


	private static String findHeaderStr(String commentSymbol) {
		
		String headerStr = "";
		
		String twoDigitMonth = String.format("%02d", ControlData.currMonth);
		String twoDigitCycle = String.format("%02d", ControlData.currCycleIndex+1);
		
		String CurrDate = Integer.toString(ControlData.currYear)+"."
		                 + twoDigitMonth;
		
		headerStr = headerStr + commentSymbol + " Date: "+CurrDate + "\n";
		headerStr = headerStr + commentSymbol + " Cycle: "+twoDigitCycle + "\n";;
		headerStr = headerStr + commentSymbol + " Solver: "+ControlData.solverName + "\n\n";
		
		return headerStr;
	}
	
	private static Set<String> findWeightedDvar() {
		
		Set<String> dvar_weighted = new HashSet<String>();
		
		dvar_weighted.addAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		dvar_weighted.addAll(SolverData.getWeightMap().keySet());
	
		return dvar_weighted;		
	}
	
	private static Set<String> findDvarInConstraint() {
	
		Set<String> dvar_inConstraint = new HashSet<String>();

		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
	
		for (String constraintName : constraintMap.keySet()) {	
	
			if (!constraintMap.get(constraintName).getEvalExpression().isNumeric()) {
	
				dvar_inConstraint.addAll(constraintMap.get(constraintName).getEvalExpression().getMultiplier().keySet());
			}	
		}
		return dvar_inConstraint;
	}

	private static Set<String> findUnWeightedDvarInConstraint( Set<String> dvar_inConstraint, Set<String> dvar_weighted ) {
		
		Set<String> dvar_unWeighted = new HashSet<String>();
		
		dvar_unWeighted.addAll(dvar_inConstraint);

		dvar_unWeighted.removeAll(dvar_weighted);
	
		return dvar_unWeighted;		
	}

	private static Map<String, WeightElement> findActiveWeightMap() {
	
		Map<String, WeightElement> activeWeightMap = new HashMap<String, WeightElement>();
		
		for (String usedSlackSurplus: ControlData.currModelDataSet.usedWtSlackSurplusList){
		
			activeWeightMap.put(usedSlackSurplus, SolverData.getWeightSlackSurplusMap().get(usedSlackSurplus));	
			
		}
		
		activeWeightMap.putAll(SolverData.getWeightMap());
	
		return activeWeightMap;
	}

	private static String findSvarValueStr() {
		
		String svarValueStr = "";
		
		Map<String, Svar> svMap = ControlData.currSvMap;
		
		ArrayList<String> sortedTerm = new ArrayList<String>(svMap.keySet());
		Collections.sort(sortedTerm);
		
		for (String s : sortedTerm){
			String svName = String.format("%-35s", s);
			svarValueStr = svarValueStr + ( svName + ":  " + svMap.get(s).getData().getData() +"\n"  );
		}
		
		return svarValueStr;
	}

	private static String findDvarValueStr_XA(Set<String> dvar) {
		
		ArrayList<String> dvar_sorted = new ArrayList<String>(dvar);
		
		Collections.sort(dvar_sorted);		
		
		String dvarValueStr = "";
		
		for (String s : dvar_sorted){
			String dvName = String.format("%-35s", s);
			dvarValueStr = dvarValueStr + (dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
		}
		
		return dvarValueStr;
	}

	public static void writeObjValueLog(String msg, String objValueStr, PrintWriter outFile) {		
			
		if (Error.getTotalError()==0) {
			outFile.println(msg+": "+objValueStr);
		} else {
			outFile.println(msg+": "+"Error! ");	
		}
		outFile.flush();
	}

	public static void writeNoteLn(String msg, boolean printToScreen, boolean isErr) {		
		
		writeNoteLn(msg, _noteFile);
		if (printToScreen) {
			if (isErr) { System.err.println(msg);}
			else {System.out.println(msg);};
		}
	}
	
	public static void writeNoteLn(String msg) {		
		
		writeNoteLn(msg, false, false);
	}
	
	public static void writeNoteLn(String msg, String noteStr) {		
		
		writeNoteLn(msg, noteStr, _noteFile);
	}
	
	public static void writeNoteLn(String msg, String noteStr, PrintWriter outFile) {		
		
//		if (Error.getTotalError()==0) {
			outFile.println(msg+": "+noteStr);
//		} else {
//			outFile.println(msg+": "+"Error! ");	
//		}
		outFile.flush();
	}

	public static void writeNoteLn(String msg, PrintWriter outFile) {		
		
		outFile.println(msg);
		outFile.flush();
	}
	
	public static void writeNote(String msg, PrintWriter outFile) {		
		
		outFile.print(msg);
		outFile.flush();
	}
	
	private static void writeObjValue(String objValueStr, PrintWriter outFile, String commentSym) {		

		//double objValue = ControlData.xasolver.getObjective();
		
		outFile.print("\n\n\n\n");		
		if (Error.getTotalError()==0) {
			outFile.println(commentSym+" objective value:   "+objValueStr);
		} else {
			outFile.println(commentSym+" objective value:   Error! ");	
		}
		outFile.flush();
	}

	// returns <dvar_weighted, dvar_unweighted>
	private static ArrayList<ArrayList<String>> prepareDvarToWrite(Set<String> dvar_effective){
		
		ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
		
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, WeightElement> wtMap = SolverData.getWeightMap();
		//Map<String, WeightElement> wtSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
		
		ArrayList<String> dvar_weighted = new ArrayList<String>(wtMap.keySet());
		dvar_weighted.addAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		
		//TODO: remove weight of 0 from dvar_weighted.
		
		
		ArrayList<String> dvar_unweighted = new ArrayList<String>(dvMap.keySet());
		//dvar_unweighted.removeAll(wtMap.keySet());
		//dvar_unweighted.removeAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		dvar_unweighted.removeAll(dvar_weighted);
		
		dvar_unweighted.retainAll(dvar_effective);
		Collections.sort(dvar_weighted);
		Collections.sort(dvar_unweighted);
		
		ret.add(dvar_weighted);
		ret.add(dvar_unweighted);
		
		return ret;
		
	}
	private static void writeDvarValue_XA(PrintWriter dvarFile, Set<String> dvar_effective) {
		
		ArrayList<ArrayList<String>> sortedDvar = prepareDvarToWrite(dvar_effective);
		ArrayList<String> dvar_weighted = sortedDvar.get(0);
		ArrayList<String> dvar_unweighted = sortedDvar.get(1);
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			double v = ControlData.xasolver.getColumnActivity(s);
			if (loggingVariableValueRound) v = Math.round(v);
			if (loggingVariableValueRound10 && !intList.contains(s)) {
				v = Math.round(v/10)*10;
			}
			try{
				if (v!=0){
					dvarFile.print(dvName + ":  " + String.format("%23s", df.format(v)) +"\n"  );
				} else {
					dvarFile.print(dvName + ":  " + String.format("%23s", "0") +"\n"  );
				}
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + String.format("%23s", v) +"\n"  );
			}
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			double v = ControlData.xasolver.getColumnActivity(s);
			if (loggingVariableValueRound) v = Math.round(v);
			if (loggingVariableValueRound10 && !intList.contains(s)) {
				v = Math.round(v/10)*10;
			}
			try{
				if (v!=0){
					dvarFile.print(dvName + ":  " + String.format("%23s", df.format(v)) +"\n"  );
				} else {
					dvarFile.print(dvName + ":  " + String.format("%23s", "0") +"\n"  );
				}
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + String.format("%23s", v) +"\n"  );
			}
		}
	}

	private static void writeDvarValue_LPSOLVE(PrintWriter dvarFile, Set<String> dvar_effective) {		
		
		ArrayList<ArrayList<String>> sortedDvar = prepareDvarToWrite(dvar_effective);	
		ArrayList<String> dvar_weighted = sortedDvar.get(0);
		ArrayList<String> dvar_unweighted = sortedDvar.get(1);
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			//dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
			try{
				double v = LPSolveSolver.varDoubleMap.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}

			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + LPSolveSolver.varDoubleMap.get(s) +"\n"  );
			}
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			try{
				double v = LPSolveSolver.varDoubleMap.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}
				
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + LPSolveSolver.varDoubleMap.get(s) +"\n"  );
			}
		}
	}
	
	private static void writeDvarValue_Clp0_Cbc0(PrintWriter dvarFile, Set<String> dvar_effective, Map <String, Double> varDoubleMap) {		
		
		ArrayList<ArrayList<String>> sortedDvar = prepareDvarToWrite(dvar_effective);	
		ArrayList<String> dvar_weighted = sortedDvar.get(0);
		ArrayList<String> dvar_unweighted = sortedDvar.get(1);
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			//dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
			try{
				double v = varDoubleMap.get(s);
				if (loggingVariableValueRound) v = Math.round(v);
				if (loggingVariableValueRound10 && !intList.contains(s)) {
					v = Math.round(v/10)*10;
				}
				// TODO: improve speed
				if (v!=0) {
					dvarFile.print(dvName + ":  " + String.format("%23s", df.format(v)) +"\n"  );		
				} else {
					dvarFile.print(dvName + ":  " + String.format("%23s", "0") +"\n"  );				
				}

			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + varDoubleMap.get(s) +"\n"  );
			}
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			try{
				double v = varDoubleMap.get(s);
				if (loggingVariableValueRound) v = Math.round(v);
				if (loggingVariableValueRound10 && !intList.contains(s)) {
					v = Math.round(v/10)*10;
				}
				// TODO: improve speed
				if (v!=0) {
					dvarFile.print(dvName + ":  " + String.format("%23s", df.format(v)) +"\n"  );				
				} else {
					dvarFile.print(dvName + ":  " + String.format("%23s", "0") +"\n"  );				
				}
				
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + varDoubleMap.get(s) +"\n"  );
			}
		}
	}
	
	private static void writeDvarValue_Gurobi(PrintWriter dvarFile, Set<String> dvar_effective) {

		
		ArrayList<ArrayList<String>> sortedDvar = prepareDvarToWrite(dvar_effective);		
		ArrayList<String> dvar_weighted = sortedDvar.get(0);
		ArrayList<String> dvar_unweighted = sortedDvar.get(1);
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			//dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
			try{
				double v = GurobiSolver.varDoubleMap.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}
	
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + GurobiSolver.varDoubleMap.get(s) +"\n"  );
			}
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			try{
				double v = GurobiSolver.varDoubleMap.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}
				
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + GurobiSolver.varDoubleMap.get(s) +"\n"  );
			}
		}
	}

	private static void writeDvarValue_OrTools(PrintWriter dvarFile, Set<String> dvar_effective) {

		
		ArrayList<ArrayList<String>> sortedDvar = prepareDvarToWrite(dvar_effective);		
		ArrayList<String> dvar_weighted = sortedDvar.get(0);
		ArrayList<String> dvar_unweighted = sortedDvar.get(1);
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			//dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
			try{
				double v = ControlData.otsolver.solution.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}
	
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + ControlData.otsolver.solution.get(s) +"\n"  );
			}
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			try{
				double v = ControlData.otsolver.solution.get(s);
				// TODO: improve speed
				if (!df.format(v).equals("-0")) {
					dvarFile.print(dvName + ":  " + df.format(v) +"\n"  );					
				} else {
					dvarFile.print(dvName + ":  0" +"\n"  );				
				}
				
			} catch (Exception e) {
				dvarFile.print(dvName + ":  " + ControlData.otsolver.solution.get(s) +"\n"  );
			}
		}
	}	
	
	private static void writeSvarValue(PrintWriter svarFile) {
		
		Map<String, Svar> svMap = ControlData.currSvMap;
		
		ArrayList<String> sortedTerm = new ArrayList<String>(svMap.keySet());
		Collections.sort(sortedTerm);
		


		
		for (String s : sortedTerm){
			String svName = String.format("%-35s", s);
			double v =svMap.get(s).getData().getData().doubleValue();
			if (loggingVariableValueRound) v = Math.round(v);
			if (loggingVariableValueRound10) {v = Math.round(v/10)*10;}
			// TODO: improve speed
//			System.out.println("svarFile:"+svarFile.toString());
//			System.out.println("svName:"+svName);
//			System.out.println("v:"+v);
//			System.out.println("df:"+df.toString());
			if (v!=0) {
				svarFile.print(svName + ":  " + String.format("%23s", df.format(v)) +"\n"  );
			} else {
				svarFile.print(svName + ":  " + String.format("%23s", "0") +"\n" );
			}
			
			
//			svarFile.print(svName + ":  ");
//			svarFile.print(df.format(svMap.get(s).getData().getData()));
//			svarFile.print("\n"  );
			
		}

	}
	
	public static void setLPSolveDir(String dir){
		_lpSolveDir=dir;
	}
	
	public static void setCplxLpDir(String dir){
		_cplexLpDir=dir;
	}
	
	public static void logUsageMemory(int year, int month, int day, int cycleIndex){
		Thread thread = new Thread(){
			public void run(){
				try {
					//System.out.println("tasklist.exe /fi \"PID eq "+ControlData.pid+"\" > \""+ _ilpDir.getAbsolutePath()+"\\Note_memory.log\"");
					ProcessBuilder builder = new ProcessBuilder("tasklist.exe", "/nh", "/fi", "\"PID", "eq", ControlData.pid+"\"");
					Process process = builder.start();
					InputStream stdout = process.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
					BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

			        String line = null;
			        while ((line = br.readLine()) != null) {
			        	if (!line.equals("")){
			        		String twoDigitMonth = String.format("%02d", month);
			        		String twoDigitCycle = String.format("%02d", cycleIndex+1);
			        		String ret = year + "_" + twoDigitMonth + "_c" + twoDigitCycle;
			        		ILP.writeNoteLn(ret, line, ILP._noteFile_memory);
			        	}
			        }

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

	    thread.start();
	}
}
