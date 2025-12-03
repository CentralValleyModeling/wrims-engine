package gov.ca.water.wrims.engine.core.wreslplus.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.config.ConfigUtils;
import gov.ca.water.wrims.engine.core.wreslparser.elements.LogUtils;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyParser;
import gov.ca.water.wrims.engine.core.wreslparser.elements.StudyUtils;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ErrorCheck;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcGoal;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcIfIncItemGroup;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcMainFile;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcIncFile;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcParameter;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcVarIncFileList;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ProcWeight;
import gov.ca.water.wrims.engine.core.wreslplus.elements.procedures.ToLowerCase;

public class Workflow {
	
	
	private Workflow(){}


	public static StudyTemp checkStudy(String mainFilePath) {
		
		StudyParser.reset();
		
		String canonicalMainFilePath = Tools.getCanonicalLowCasePath(mainFilePath);
		
		ParserUtils.setRunDir(new File(canonicalMainFilePath).getParent()); 
		
		VarCycleIndex.clearVarCycleIndexList();
		
		StudyTemp st = ParserUtils.parseWreslMain(canonicalMainFilePath);
		// TODO: need to check sequence error here
		// check study
		
		if (StudyParser.total_errors>0) {
			
			LogUtils.parsingSummaryMsg("Wresl+ parsing aborted", StudyParser.total_errors);
			
			return null;
		}
		

		if (ErrorCheck.checkIncModelNotExistIgnoreCase(st)) return null;	
		if (ErrorCheck.checkModelRedefinedIgnoreCase(st)>0) return null;
		if (ErrorCheck.checkSequenceHasUniqueModel(st)>0) return null;
		if (ErrorCheck.checkVarRedefined(st)>0) return null;		
		
		ToLowerCase.convert(st);
		
		if (ErrorCheck.checkVarRedefined(st)>0) return null;
		
		// for Error.log header
		ControlData.currEvalTypeIndex=8;
		
		// check config param not declared in wresl main file
		if (ErrorCheck.checkInitialVarInConfigNotDeclaredInWresl(st)) return null;		
		
		// overwrite main wresl parameter with config file parameter
		if (ConfigUtils.paramMap.size()>0){
			
			for (String k: ConfigUtils.paramMap.keySet()){
				ParamTemp pt = ConfigUtils.paramMap.get(k);
				SvarTemp svObj = new SvarTemp();
	            svObj.id = pt.id;
	            svObj.caseName.add(Param.defaultCaseName);
	            svObj.caseCondition.add(Param.always);
	            svObj.caseExpression.add(pt.expression);
	            svObj.dependants = pt.dependants;
	            
	            LogUtils.importantMsg("Overwrite initial variable ["+ k +"] in main wresl file.");
				st.parameterMap.put(k,svObj);
			}

		}
		
		// check param dependents unknown
		if (ErrorCheck.checkInitialConst(st)) return null;
		if (ErrorCheck.checkParameterHasUnknownDepedants(st)) return null;
		
		ProcParameter.process(st);

		if (ErrorCheck.checkSeqConditionHasUnknownDepedants(st)) return null;
		
		// check unknown dependants in if statement
		ErrorCheck.checkIfStatementHasUnknownDependants(st);
		
		// process "if include file group"
		ProcIfIncItemGroup.process(st);
		
		
		ProcMainFile.findEffectiveModel(st); 

		ProcMainFile.findKidMap_incModel(st);
		ProcMainFile.findAllOffSpring_incModel(st);
		ProcMainFile.findGroupOrder_incModel(st);
		
		
		ProcMainFile.findEffectiveIncludeModel(st); 
		
		
		ProcIncFile.processPath(st);
			

		ProcVarIncFileList.replaceIncFile(st);
		
		
		Procedures.processDependants(st);
		

		
		
		/// put effective models into fileModelDataTable

		for (String modelName: st.modelList_effective){
				
			ModelTemp mt = st.modelMap.get(modelName);  // this is model in main file
			
			if (st.fileModelNameMap.keySet().contains(mt.pathRelativeToRunDir)) {
				
				st.fileModelNameMap.get(mt.pathRelativeToRunDir).add(modelName);
				
			} else {
				ArrayList<String> modelNameList = new ArrayList<String>();
				modelNameList.add(modelName);
				st.fileModelNameMap.put(mt.pathRelativeToRunDir, modelNameList);
			}
			
			st.fileModelDataTable.put(mt.pathRelativeToRunDir, modelName, mt);			
			
		}

		/// put effective include models into fileModelDataTable
		for (String incM: st.incModelList_effective){
			
			ModelTemp incMt = st.modelMap.get(incM);  // this is model in main file
				
			if (st.fileModelNameMap.keySet().contains(incMt.pathRelativeToRunDir)) {
				
				st.fileModelNameMap.get(incMt.pathRelativeToRunDir).add(incM);
				
			} else {
				ArrayList<String> modelNameList = new ArrayList<String>();
				modelNameList.add(incM);
				st.fileModelNameMap.put(incMt.pathRelativeToRunDir, modelNameList);
			}
			
			st.fileModelDataTable.put(incMt.pathRelativeToRunDir, incM, incMt);			
			
		}
		
		// parse modelList_effective all included files		
		// store results to a map using relativePath as key
		for (String se : st.seqList){
			
			SequenceTemp seqObj = st.seqMap.get(se); 
			String m = seqObj.model;

			ModelTemp mt = st.modelMap.get(m);

			ParserUtils.parseAllIncFile(mt.incFileRelativePathList, st);					
			
		}
		
		// parse incModelList_effective all included files		
		// store results to a map using relativePath as key
		for (String incM : st.incModelList_effective){

			ModelTemp mt = st.modelMap.get(incM);

			ParserUtils.parseAllIncFile(mt.incFileRelativePathList, st);					
			
		}
		

		if (StudyParser.total_errors>0) {
			
			LogUtils.parsingSummaryMsg("Wresl+ parsing stopped", StudyParser.total_errors);
			if (ControlData.showWreslLog) {
				for (String e:StudyParser.error_summary){
					System.err.println( e);
				}
			}
			for (String e:StudyParser.error_summary){
				 LogUtils._logFile.println(e);
			}
			LogUtils._logFile.flush();
			
			return null;
		}
		
		// find all offspring
		// store results to kidMap and AOMap, fileGroupOrder
		Procedures.findKidMap(st);
		
		
		Procedures.findAllOffSpring(st);
		
		
		Procedures.findFileGroupOrder(st);
		

		Procedures.postProcessIncFileList(st);
		
		
		Procedures.postProcessVarListinIncFile(st);

		ProcGoal.processGoalHS2(st); 
		
		ProcWeight.collectWeightVar(st);
		
		
		// filter weight groups from the weight Object list and then process them
		// TODO: need to rewrite

		ProcWeight.processWeightGroup(st);
						
		if(Procedures.copyModelVarMapToSequenceVarMap(st)) return null;
		
				
		if (ErrorCheck.checkWeightObjList(st)) return null;
		
		ErrorCheck.checkVarRedefined(st);	
		
		Procedures.classifyDependants(st);  // TODO: has bug. need to fix. also a bottleneck.
		
		if (StudyUtils.parserCheckVarUndefined) {
			LogUtils.importantMsg("Check variables used before defined ...");
			ErrorCheck.checkVarUsedBeforeDefined(st);
		}
		
		LogUtils.importantMsg("Convert aliases to goals ...");
		Procedures.convertAliasToGoal(st);   // TODO: bottleneck. need to optimize.
		
		ErrorCheck.checkWeightVarNotInDvar(st);
		
		Procedures.analyzeVarNeededFromCycle(st);
		Procedures.createSpaceInVarCycleValueMap(st);
		
		
		Procedures.collectTimeStep(st);
		
				
		LogUtils.parsingSummaryMsg("Wresl+ parsing completed",StudyParser.total_errors);
		
		if (StudyParser.total_errors>0) return null;
		
		return st;
		
	}


	public static StudyTemp checkStudy_compileLog(String mainFilePath) {
		
		StudyParser.reset();		
		VarCycleIndex.clearVarCycleIndexList();
		
		
		String canonicalMainFilePath = Tools.getCanonicalLowCasePath(mainFilePath);
		
		String runDir = new File(canonicalMainFilePath).getParent();
		String mainFileName = new File(canonicalMainFilePath).getName();
		
		ParserUtils.setRunDir(runDir); 
		
		String compileLogDir = "z:\\compileLog\\"+FilenameUtils.getPath(runDir);
		
		
		new File(compileLogDir).mkdirs();
		
		StudyTemp st = ParserUtils.parseWreslMain(canonicalMainFilePath);
		
		LinkedHashMap<String,Integer> fileMap_reverse = new LinkedHashMap<String, Integer>();
		
		String mainString = CompileLog.createMainWreslLog(st, fileMap_reverse);
		CompileLog.writeLog(mainString, new File (compileLogDir, mainFileName).getAbsolutePath());
		
		
		for (String filePath : fileMap_reverse.keySet()) {

			String absFilePath = st.runDir + "\\" + filePath;
			
			ModelTemp mt = ParserUtils.parseWreslFile(absFilePath);
			
			
			Set<String> deps = new LinkedHashSet<String>();
			String w = CompileLog.createWreslFileLog(mt, deps);
			
			// write dependents
			w= "\n" + w;
			for (String d : deps){ w= d +" " + w; }
			w = "@dep: "+ w; 
			
			
			File f = new File (compileLogDir, filePath);
			File dir = f.getParentFile();			
			if (! dir.exists()) dir.mkdirs();
			
			CompileLog.writeLog(w, f.getAbsolutePath());

		}
		
		

		
		return st;
		
	}	



}
	
