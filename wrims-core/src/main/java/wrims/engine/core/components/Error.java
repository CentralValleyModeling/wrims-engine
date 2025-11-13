package wrims.engine.core.components;

import java.util.ArrayList;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import wrims.engine.core.commondata.wresldata.Alias;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.External;
import wrims.engine.core.commondata.wresldata.Goal;
import wrims.engine.core.commondata.wresldata.ModelDataSet;
import wrims.engine.core.commondata.wresldata.Svar;
import wrims.engine.core.commondata.wresldata.Timeseries;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.ilp.ILP;
import wrims.engine.core.wreslparser.elements.StudyUtils;
import wrims.engine.core.wreslparser.elements.Tools;

public class Error {
	public static ArrayList<String>   error_grammer = new ArrayList<String> ();
	public static ArrayList<String>   error_evaluation= new ArrayList<String> ();
	public static ArrayList<String>   error_solving=new ArrayList<String>();
	public static ArrayList<String>   error_engine=new ArrayList<String>();
	public static ArrayList<String>   error_config=new ArrayList<String>();
	public static ArrayList<String>   error_initial=new ArrayList<String>();
	public static ArrayList<String>   error_deviation=new ArrayList<String>();
	
	public static void writeGrammerErrorFile(String fileName){
		
		String errorFileFullPath=FilePaths.mainDirectory+fileName;
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error_grammer.size(); i++){
				out.println(error_grammer.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void writeEvaluationErrorFile(String fileName){
		
		String errorFileFullPath=FilePaths.mainDirectory+fileName;
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error_evaluation.size(); i++){
				out.println(error_evaluation.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void writeSolvingErrorFile(String fileName){
		
		String errorFileFullPath=FilePaths.mainDirectory+fileName;
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error_solving.size(); i++){
				out.println(error_solving.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void writeConfigErrorFile(String fileName){
		
		String errorFileFullPath=FilePaths.mainDirectory+fileName;
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error_config.size(); i++){
				out.println(error_config.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void writeDeviationErrorFile(String fileName){
		
		String errorFileFullPath=FilePaths.mainDirectory+fileName;
		try{
			FileWriter errorFile = new FileWriter(errorFileFullPath);
			PrintWriter out = new PrintWriter(errorFile);

			for (int i=0; i<error_deviation.size(); i++){
				out.println(error_deviation.get(i));
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
	
	public static void writeErrorLog(){
		
	    File ilpRootDir = new File(FilePaths.mainDirectory, "=ILP=");  
	    File ilpDir = new File(ilpRootDir, StudyUtils.configFileName); 
	    PrintWriter errorLogFile;
	    try {
			errorLogFile = Tools.openFile(ilpDir.getAbsolutePath(), "Error_"+ControlData.dateTimeAppend+".log");
			
			errorLogFile.println("==================");
			if (ControlData.currEvalTypeIndex!=8){
				errorLogFile.println(ILP.getYearMonthCycle());
			} else {
				errorLogFile.println("Initial processing");
			}
			errorLogFile.println("==================");
			for (int i=0; i<error_config.size(); i++){
				errorLogFile.println(error_config.get(i));
			}
			for (int i=0; i<error_initial.size(); i++){
				errorLogFile.println(error_initial.get(i));
			}
			for (int i=0; i<error_evaluation.size(); i++){
				errorLogFile.println(error_evaluation.get(i));
			}
			for (int i=0; i<error_solving.size(); i++){
				errorLogFile.println(error_solving.get(i));
			}
			for (int i=0; i<error_deviation.size(); i++){
				errorLogFile.println(error_deviation.get(i));
			}

			errorLogFile.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void addEvaluationError(String error){
		ModelDataSet mds = ControlData.currModelDataSet;
		String sourceLocation="";
		if (ControlData.currEvalTypeIndex == 0){
			if (mds.svMap.containsKey(ControlData.currEvalName)){
				Svar svar=mds.svMap.get(ControlData.currEvalName);
				sourceLocation="("+svar.fromWresl+":"+svar.line+")";
			}
			error_evaluation.add(sourceLocation+getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in svar definition: "+error);
		}else if (ControlData.currEvalTypeIndex == 1){
			if (mds.dvMap.containsKey(ControlData.currEvalName)){
				Dvar dvar=mds.dvMap.get(ControlData.currEvalName);
				sourceLocation="("+dvar.fromWresl+":"+dvar.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in dvar definition: "+error);
		}else if (ControlData.currEvalTypeIndex == 2){
			if (mds.asMap.containsKey(ControlData.currEvalName)){
				Alias as=mds.asMap.get(ControlData.currEvalName);
				sourceLocation="("+as.fromWresl+":"+as.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in alias definition: "+error);
		}else if (ControlData.currEvalTypeIndex == 3){
			if (mds.gMap.containsKey(ControlData.currEvalName)){
				Goal goal=mds.gMap.get(ControlData.currEvalName);
				sourceLocation="("+goal.fromWresl+":"+goal.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in constraint definition: "+error);
		}else if (ControlData.currEvalTypeIndex == 4){
			if (mds.exMap.containsKey(ControlData.currEvalName)){
				External ex=mds.exMap.get(ControlData.currEvalName);
				sourceLocation="("+ex.fromWresl+":"+ex.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in external function definition: "+error);
		}else if (ControlData.currEvalTypeIndex == 5){
			if (mds.tsMap.containsKey(ControlData.currEvalName)){
				Timeseries ts=mds.tsMap.get(ControlData.currEvalName);
				sourceLocation="("+ts.fromWresl+":"+ts.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in timeseries definition: "+error);
		}else if (ControlData.currEvalTypeIndex==6){
			if (mds.tsMap.containsKey(ControlData.currEvalName)){
				Timeseries ts=mds.tsMap.get(ControlData.currEvalName);
				sourceLocation="("+ts.fromWresl+":"+ts.line+")";
			}
			error_evaluation.add(ControlData.currEvalName+" in timeseries reading: "+error);
		}else if (ControlData.currEvalTypeIndex==7){
			if (mds.wtMap.containsKey(ControlData.currEvalName)){
				WeightElement wt=mds.wtMap.get(ControlData.currEvalName);
				sourceLocation="("+wt.fromWresl+":"+wt.line+")";
			}
			error_evaluation.add(getCurrentDateCycleModel()+", "+ControlData.currEvalName+" in weight definition: "+error);
		}else if (ControlData.currEvalTypeIndex==8){
			if (mds.svMap.containsKey(ControlData.currEvalName)){
				Svar sv=mds.svMap.get(ControlData.currEvalName);
				sourceLocation="("+sv.fromWresl+":"+sv.line+")";
			}
			error_evaluation.add("Initial variable "+ControlData.currEvalName+": "+error);
		}else if (ControlData.currEvalTypeIndex==9){
			String msg = "Conditional include (if, elseif) in file: "+ControlData.currEvalName+": "+error;
			error_evaluation.add(msg);
			System.err.println("# Error :"+msg);
		}
		if (ControlData.currEvalTypeIndex<100) System.err.println(sourceLocation+" # Error :"+ControlData.currEvalName+":"+error);
	}
	
	public static void addSolvingError(String error){
		error_solving.add(getCurrentDateCycleModel()+": "+error);
		System.err.println("# Error :"+error);
	}

	public static void addInfeasibleHint(String goalName, String moreMsg){
		ModelDataSet mds = ControlData.currModelDataSet;
		String sourceLocation = "";
		if (mds.gMap.containsKey(goalName)){
			Goal goal=mds.gMap.get(goalName);
			sourceLocation="("+goal.fromWresl+":"+goal.line+")";
		}
		error_solving.add(sourceLocation+ " "+moreMsg);
		System.err.println(sourceLocation+ " "+goalName);
	}
	
	public static void addEngineError(String error){
		error_engine.add("Engine Error: "+error);
		System.err.println("# Engine Error: "+error);
	}
	
	public static void addConfigError(String error){
		error_config.add(error);
		System.err.println("# Error: "+error);
	}
	public static void addInitialError(String error){
		error_initial.add(error);
		System.err.println("# Error: "+error);
	}
	public static void addDeviationError(String error){
		error_deviation.add(getCurrentDateCycleModel()+": "+error);
		System.err.println("# Error: "+error);
	}
	
	public static int getTotalError(){
		return Error.error_engine.size()+
				 Error.error_evaluation.size()+
				 Error.error_grammer.size()+
				 Error.error_solving.size()+
				 Error.error_config.size()+
				 Error.error_initial.size()+
				 Error.error_deviation.size();
	}
	
	public static void clear(){
		Error.error_engine = new ArrayList<String>();
		Error.error_evaluation = new ArrayList<String>();
		Error.error_grammer = new ArrayList<String>();
		Error.error_solving = new ArrayList<String>();
		Error.error_config = new ArrayList<String>();
		Error.error_initial = new ArrayList<String>();
		Error.error_deviation = new ArrayList<String>();
	}

	public static String getCurrentDateCycleModel(){
		
		ArrayList<String> modelList=ControlData.currStudyDataSet.getModelList();		
		String modelName = modelList.get(ControlData.currCycleIndex);
		int cycleIndex = ControlData.currCycleIndex + 1 ;
		
		return ControlData.currYear+"/"+ControlData.currMonth+"/"+ControlData.currDay+ " cycle "+cycleIndex+ " ("+modelName +")";

	}
}

