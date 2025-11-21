package wrimsv2.wreslparser.elements;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.components.ControlData;



public class LogUtils {

    private static final Logger logger = LoggerFactory.getLogger(LogUtils.class);
	public static PrintWriter _logFile;
	
	public static void closeLogFile(){
		
		_logFile.close();		
	}

	public static void setLogFile(String parentDir, String logFileName){
		
		try {
			_logFile = Tools.openFile(parentDir, logFileName);
			
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}	
	
	public static void setLogFile(String logFileName){
				
		try {
			if (logFileName.contains(":")) {
				_logFile = Tools.openFile(logFileName);
			} else {
				_logFile = Tools.openFile(System.getProperty("user.dir"), logFileName);
			}
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
	}
	
	public static void dvarsList(String msg, ArrayList<String> list_all, ArrayList<String> list_g, ArrayList<String> list_l, Map<String, Dvar> dvMap){
		
		String description = "Dvars";
		
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_all.size()+" "+description+":");
		LogUtils.importantMsg(msg, list_all, dvMap);
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_g.size()+" global "+description+":");
		LogUtils.importantMsg(msg, list_g, dvMap);
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_l.size()+" local "+description+":");
		LogUtils.importantMsg(msg, list_l, dvMap);
		LogUtils.importantMsg("------------------------------");
		
	}
	
	public static void dvarsList(String msg, ArrayList<String> list_all, Map<String, Dvar> dvMap){
		
		String description = "Dvars";
		
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_all.size()+" "+description+":");
		LogUtils.importantMsg(msg, list_all, dvMap);
		LogUtils.importantMsg("------------------------------");
		
	}
	
	public static void varsList(String msg, ArrayList<String> list_all, ArrayList<String> list_g, ArrayList<String> list_l, String description){
		
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_all.size()+" "+description+":");
		LogUtils.importantMsg(list_all);
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_g.size()+" global "+description+":");
		LogUtils.importantMsg(list_g);
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_l.size()+" local "+description+":");
		LogUtils.importantMsg(list_l);
		LogUtils.importantMsg("------------------------------");
		
	}
	public static void varsList(String msg, ArrayList<String> list_all, String description){
		
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg(msg+"Include total "+list_all.size()+" "+description+":");
		LogUtils.importantMsg(list_all);
		LogUtils.importantMsg("------------------------------");
	
	}

	public static void seqList(ArrayList<String> list,  Map<Integer, Sequence> seqMap){
		
		LogUtils.importantMsg("------------------------------");
		LogUtils.importantMsg("Include total "+list.size()+" sequences:");
		for (int i: seqMap.keySet()){
			LogUtils.importantMsg("Order: "+i+"  Sequence: "+seqMap.get(i).sequenceName+"  Model: "+seqMap.get(i).modelName);
		}
		LogUtils.importantMsg("------------------------------");
	
	}	
	
	public static void fileSummary(SimulationDataSet S){

		//seqList(S.seqList, S.seqMap);
		//varsList(S.model_list, "models");
		varsList("", S.incFileList, S.incFileList_global, S.incFileList_local, "files");
		varsList("", S.dvList, S.dvList_global, S.dvList_local, "Dvars");
		varsList("", S.svList, S.svList_global, S.svList_local, "Svars");
		
	}	

	public static void mainFileSummary(StudyConfig studyConfig){


		seqList(studyConfig.sequenceList, studyConfig.sequenceMap);
		//varsList(mainDataSet.model_list, "models");
		
		for (Integer i: studyConfig.sequenceMap.keySet()){
			String modelName = studyConfig.sequenceMap.get(i).modelName;
			SimulationDataSet M = studyConfig.modelDataMap.get(modelName);
			LogUtils.importantMsg("#####  Model: "+ modelName);
			varsList("", M.incFileList, M.incFileList_global, M.incFileList_local, "files");
			varsList("", M.dvList, M.dvList_global, M.dvList_local, "Dvars");
			varsList("", M.svList, M.svList_global, M.svList_local, "Svars");
		}
	}		

	public static void studySummary_details(StudyConfig studyConfig, Map<String, SimulationDataSet> modelDataMap){

		seqList(studyConfig.sequenceList, studyConfig.sequenceMap);

		
		for (String key: studyConfig.modelList){
			SimulationDataSet M = modelDataMap.get(key);
			LogUtils.importantMsg("#####  Model: "+ key);
			
			String msg = "Model "+key+" ";
			varsList(msg, M.incFileList, M.incFileList_global, M.incFileList_local, "files");
			dvarsList(msg, M.dvList, M.dvList_global, M.dvList_local, M.dvMap);
			varsList(msg, M.svList, M.svList_global, M.svList_local, "Svars");
		}
	}	
	
	public static void studySummary(StudyConfig studyConfig, Map<String, SimulationDataSet> modelDataMap){

		seqList(studyConfig.sequenceList, studyConfig.sequenceMap);

		
		for (String key: studyConfig.modelList){
			SimulationDataSet M = modelDataMap.get(key);
			LogUtils.importantMsg("#####  Model: "+ key);
			
			String msg = "Model "+key+" ";
			varsList(msg, M.incFileList, M.incFileList_global, M.incFileList_local, "files");
			varsList(msg, M.dvList, M.dvList_global, M.dvList_local, "Dvars");
			varsList(msg, M.svList, M.svList_global, M.svList_local, "Svars");
		}
	}	
	
	public static void studySummary_details(StudyDataSet sd){
		
		for (String key: sd.getModelList()){
			ModelDataSet M = sd.getModelDataSetMap().get(key);
			LogUtils.importantMsg("#####  Model: "+ key);
			
			String msg = "Model "+key+" ";

			dvarsList(msg, M.dvList, M.dvMap);

		}
	}	

	public static void titleMsg(String msg){

		logger.info("============================================");
		logger.info(msg);
		logger.info("============================================");
		
		_logFile.println("============================================");
		_logFile.println(msg);
		_logFile.println("============================================");
		
		_logFile.flush();
	}	

	public static void parsingSummaryMsg(String msg, int errors){

		logger.info("============================================");
		logger.info(msg);
		logger.info("Total errors: "+errors);
		logger.info("============================================");
		
		_logFile.println("============================================");
		_logFile.println(msg);
		_logFile.println("Total errors: "+errors);
		_logFile.println("============================================");
		
		_logFile.flush();
	}
	
	public static void criticalMsg(String msg){
        Marker mark = MarkerFactory.getMarker("CRITICAL");
		logger.info(mark, msg);
		_logFile.println(msg);
		_logFile.flush();
	}	
	
	public static void importantMsg(String msg){
        Marker mark = MarkerFactory.getMarker("IMPORTANT");
		if (ControlData.showWreslLog) logger.info(mark, msg);
		_logFile.println(msg);
		_logFile.flush();
	}

	public static void importantMsg(ArrayList<String> msg){
		
		for(String e: msg){
			importantMsg(e+"\n");
		}
	}

	public static void importantMsg(String msg, ArrayList<String> dvList, Map<String, Dvar> dvMap){
		
		for(String e: dvList){
			importantMsg(msg + e + "  kind: "+dvMap.get(e).kind +"\n");
		}
	}
	
	public static void normalMsg(String msg){
		if (Param.printLevel>1){
			if (ControlData.showWreslLog) logger.info(msg);
			_logFile.println(msg);

		}
	}

	public static void consoleMsgOnly(String msg){

		if (ControlData.showWreslLog) logger.info(msg); // TODO, setup specific appender for console only
		
	}	

	public static void warningMsg(String msg){
		
		 StudyParser.total_warnings++;

		if (ControlData.showWreslLog) logger.warn("# Warning: "+msg);
		 _logFile.println("# Warning: "+msg);
		 _logFile.flush();
		
	}
	
	public static void warningMsgLocation(String filePath, int lineNumber, String msg){

		if (ControlData.showWreslLog) logger.info( "("+filePath+":"+lineNumber+") "+msg );
		 _logFile.println("# Warning: " + "("+filePath+":"+lineNumber+") "+msg );
		 _logFile.flush();
		
	}

	public static void typeRedefinedErrMsg(String msg) throws TypeRedefinedException {
		
		 StudyParser.total_errors++;

		if (ControlData.showWreslLog) logger.info("# Error: "+msg);
		 _logFile.println("# Error: "+msg);
		 _logFile.flush();
		 
		 throw new TypeRedefinedException();
		 //if (!Param.debug) System.exit(0);
	}	
	
	public static void errMsgLocation(String filePath, int lineNumber, String msg){
		
		 StudyParser.total_errors++;
		 StudyParser.error_summary.add("# Error: ("+filePath+":"+lineNumber+") "+msg );
		if (ControlData.showWreslLog) System.err.println( "# Error: ("+filePath+":"+lineNumber+") "+msg );
		 _logFile.println("# Error: " + "("+filePath+":"+lineNumber+") "+msg );
		 _logFile.flush();
		
	}
	
	public static void errMsg(String msg){
		
		 StudyParser.total_errors++;

		if (ControlData.showWreslLog) System.err.println("# Error: "+msg);
		 _logFile.println("# Error: "+msg);
		 _logFile.flush();
		
	}	

	public static void errMsg(String msg, ArrayList<String> list){
		 
		 for (String e: list){
			 errMsg(msg+e); 
		 }
		
	}	
	
	public static void errMsg(String msg, String file){

		errMsg(msg+" in file: "+file);
		
	}

	public static void errMsg(String msg, String file1, String file2, Map<String, Set<String>> reverseMap) {

		errMsg(msg + " in files: ");

		String sp = "  ";
		if (ControlData.showWreslLog) logger.error(sp + file1);
		_logFile.println(sp + file1);
		printTree(file1, reverseMap, sp);
		if (ControlData.showWreslLog) logger.error(sp + file2);
		_logFile.println(sp + file2);
		printTree(file2, reverseMap, sp);
		
		 _logFile.flush();

	}

	private static void printTree(String f, Map<String, Set<String>> reverseMap, String level) {

		// String arrow = ">";
		if (reverseMap.get(f) != null) {
			level = level + "--";
			Set<String> parents = reverseMap.get(f);
			for (String s : parents) {
				if (ControlData.showWreslLog) logger.info(" "+level + "> "+ s);
				_logFile.println(" "+level + "> "+ s);

				printTree(s, reverseMap, level);

			}
		}
	}
}
