package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.collect.HashBasedTable;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.Goal;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.commondata.wresldata.WeightElement;
import wrimsv2.components.ControlData;
import wrimsv2.wreslplus.elements.VarCycleIndex;
import wrimsv2.wreslplus.elements.AliasTemp;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.ExternalTemp;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.SvarTemp;
import wrimsv2.wreslplus.elements.TimeseriesTemp;
import wrimsv2.wreslplus.elements.Tools;
import wrimsv2.wreslplus.elements.WeightTable;
import wrimsv2.wreslplus.elements.WeightTemp;

public class ToWreslData {
	
	// < relative file path, svar name, svar object >
	private static HashBasedTable<String,String,Svar> fileSvMap; 
	private static HashBasedTable<String,String,Dvar> fileDvMap; 
	private static HashBasedTable<String,String,Timeseries> fileTsMap; 
	private static HashBasedTable<String,String,Alias> fileAsMap; 
	private static HashBasedTable<String,String,Goal> fileGlMap;
	private static HashBasedTable<String,String,Dvar> fileSlackSurplusNoCaseMap;
	private static HashBasedTable<String,String,WeightElement> fileGroupWeightMap; 
	private static HashBasedTable<String,String,WeightElement> fileSsWeightMap_noCase; 
	private static HashBasedTable<String,String,WeightElement> fileWeightMap; 
	private static HashBasedTable<String,String,HashMap<String,WeightElement>> fileWTableObjMap; 
	private static HashBasedTable<String,String,HashMap<String,WeightElement>> mainfileModelWTableObjMap;
		
	private static HashBasedTable<String,Integer,Svar> StringIntegerSvarMap; 
	
	private ToWreslData(){}

	private static void initialize() {
		fileSvMap = HashBasedTable.create(); 
		fileDvMap = HashBasedTable.create(); 
		fileTsMap = HashBasedTable.create(); 
		fileAsMap = HashBasedTable.create(); 
		fileGlMap = HashBasedTable.create();
		fileSlackSurplusNoCaseMap = HashBasedTable.create();
		fileGroupWeightMap = HashBasedTable.create(); 
		fileSsWeightMap_noCase = HashBasedTable.create(); 
		fileWeightMap = HashBasedTable.create(); 
		fileWTableObjMap = HashBasedTable.create(); 
		mainfileModelWTableObjMap = HashBasedTable.create();		
		StringIntegerSvarMap = HashBasedTable.create(); 
	}
	
	//TODO: model must be in sequence obj, also condition not linked yet
	//TODO: remember to lowercase all evaluation strings and var names
	public static StudyDataSet convertStudy (StudyTemp s){
		
		if (s==null) return null;
		
		initialize();
		
		StudyDataSet o = new StudyDataSet();
		o.setModelList(s.modelList_effective);
		
		o.setVarCycleValueMap(s.varCycleValueMap);
		o.setTimeseriesTimeStepMap(s.timeseriesTimeStepMap);
		o.setModelTimeStepList(s.seqTimeStepList);		
		
		Map<String, ModelDataSet> modelDataSetMap = new HashMap<String, ModelDataSet>();
		ArrayList<String> modelConditionList   = new ArrayList<String>();
		Map<String, Timeseries> allTimeseriesMap = new LinkedHashMap<String, Timeseries>();
		
		o.setParameterList(s.parameterList);
		//o.setParameterMap(convertParameterMap(s.parameterMap));
		o.setParameterMap(s.controlDataParameterMap);
		
		
		//for (String k: s.modelList_effective){
		for (String se : s.seqList){
			
			SequenceTemp seqObj = s.seqMap.get(se); 
			
			String modelName = seqObj.model;

			ModelTemp mt = s.modelMap.get(modelName);	
			
			modelConditionList.add(seqObj.condition); 
			ModelDataSet md = convertModel(seqObj, s);
			
			// insert parameters into svar map
			//md.svList.addAll(0, o.getParameterList());
			//md.svMap.putAll(o.getParameterMap());
			
			modelDataSetMap.put(modelName, md);
		
			// add ts into all ts map. TODO: check name duplications
			allTimeseriesMap.putAll(md.tsMap);
		}
		
		o.setModelDataSetMap(modelDataSetMap);
		o.setModelConditionList(modelConditionList);
		o.setTimeseriesMap(allTimeseriesMap);
		o.setVarCycleIndexList(VarCycleIndex.varCycleIndexList);

		return o;

	}	
	
	private static LinkedHashMap<String, Svar> convertParameterMap(
			Map<String, String> simpleMap) {
		
		LinkedHashMap<String, Svar> pm = new LinkedHashMap<String, Svar>();
		
		for (String key: simpleMap.keySet()){
			
			Svar svObj = new Svar();
			
			svObj.caseName.add(Param.defaultCaseName);
			svObj.caseCondition.add(Param.always);
			svObj.caseExpression.add(simpleMap.get(key));
		    
			pm.put(key, svObj);
			
		}
		
		return pm;
	}


	public static ModelDataSet convertModel (SequenceTemp seq, StudyTemp st){
		
		ModelDataSet o = new ModelDataSet();
		
		o.varUsedByLaterCycle = seq.varUsedByLaterCycle;
		o.dvarUsedByLaterCycle = seq.dvarUsedByLaterCycle;
		o.svarUsedByLaterCycle = seq.svarUsedByLaterCycle;
		o.aliasUsedByLaterCycle = seq.aliasUsedByLaterCycle;
		
		// TODO: give pre-sorted var list

		o.dvSlackSurplusList = new ArrayList<String>(seq.ssList_hasCase);
		if (ControlData.isNameSorting) Collections.sort(o.dvSlackSurplusList,String.CASE_INSENSITIVE_ORDER);

		o.wtSlackSurplusList = new ArrayList<String>(seq.ssList_hasCase);
		if (ControlData.isNameSorting) Collections.sort(o.wtSlackSurplusList,String.CASE_INSENSITIVE_ORDER);
	
		o.dvList = new ArrayList<String>(seq.dvIncFileList_post);
		o.dvList.addAll(seq.dvList_fromAlias);
		o.dvList_deviationSlackSurplus.addAll(seq.dvList_deviationSlackSurplus);
		o.deviationSlackSurplus_toleranceMap.putAll(seq.deviationSlackSurplus_toleranceMap);
		//o.dvList.addAll(seq.ssList_noCase);
		if (ControlData.isNameSorting) Collections.sort(o.dvList,String.CASE_INSENSITIVE_ORDER);
		
		o.tsList = new ArrayList<String>(seq.tsList);
		if (ControlData.isNameSorting) Collections.sort(o.tsList,String.CASE_INSENSITIVE_ORDER);
		
		// don't sort svList. order matters in evaluator
		o.svList = new ArrayList<String>(seq.svIncFileList_post);
		//System.out.println("ToWreslData => m.svIncFileList_post"+m.svIncFileList_post);
		//System.out.println("ToWreslData => m.svList"+m.svList);
		//o.svList = new ArrayList<String>(m.svList);

		o.gList = new ArrayList<String>(seq.glIncFileList_post);
		o.gList.addAll(seq.glList_fromAlias);
		if (ControlData.isNameSorting) Collections.sort(o.gList,String.CASE_INSENSITIVE_ORDER);
		

		// don't sort exList. order matters in evaluator		
		o.exList = new ArrayList<String>(seq.exIncFileList_post);
		//Collections.sort(o.exList,String.CASE_INSENSITIVE_ORDER);
		
		// don't sort asList. order matters in evaluator
		o.asList = new ArrayList<String>(seq.asIncFileList_post);

		o.wtList = new ArrayList<String>(seq.wvList);
		o.wtList.addAll(seq.ssList_noCase);
		if (ControlData.isNameSorting) Collections.sort(o.wtList,String.CASE_INSENSITIVE_ORDER);
		
		o.incFileList = new ArrayList<String>(seq.incFileAbsPathList_post);
	
		
//		for (String k: m.incFileMap.keySet()){			
//			o.incFileList.add(m.incFileMap.get(k).absPath);
//		}
		
		
		
		
		for (String k: o.dvSlackSurplusList){			
			o.dvSlackSurplusMap.put(k, convertDvar(seq.ssMap_hasCase.get(k)));
		}
		for (String k: o.wtSlackSurplusList){			
			o.wtSlackSurplusMap.put(k, convertWeight(seq.ssWeightMap_hasCase.get(k)));
		}
		
		
//		for (String k: seq.tsMap.keySet()){			
//			o.tsMap.put(k, convertTimeseries(seq.tsMap.get(k)));
//		}	
		processTimeseries(seq, st, o); 
	
		

//		for (String k: seq.glMap.keySet()){			
//			o.gMap.put(k, convertGoal(seq.glMap.get(k)));
//		}
		processGoal(seq, st, o); 

		
		
		for (String k: o.exList){			
			o.exMap.put(k, convertExternal(seq.exMap.get(k)));
		}
		
		
		processSvar(seq, st, o); 
		
		
//		for (String k: o.dvList){			
//			o.dvMap.put(k, convertDvar(seq.dvMap.get(k)));
//		}
		processDvar(seq, st, o); 
		
		
		
//		for (String k: seq.ssList_noCase){			
//			o.dvMap.put(k, convertDvar(seq.ssMap_noCase.get(k)));
//		}
		o.dvList.addAll(seq.ssList_noCase);
		processSsList_noCase(seq, st, o); 
	
		
		
//		for (String k: o.asList){			
//			o.asMap.put(k, convertAlias(seq.asMap.get(k)));
//		}
		processAlias(seq, st, o); 

		
		
		
//// TODO: be careful. this one is special. need to revisit after
//// using Map<String,WeightTable> instead of ArrayList<WeightTable>	
//		for (WeightTable w : seq.wTableObjList){				
//			if (w.isWeightGroup){
//				o.wtMap.putAll(convertWeightTableGroup(w));
//			} else {
//				o.wtMap.putAll(convertWeightTable(w));
//			}
//		}
		processWeightTableObjList(seq, st, o); // need to revive this one.
		
		

		
//		for (String k: seq.ssList_noCase){				
//			o.wtMap.put(k, convertWeight(seq.ssWeightMap_noCase.get(k)));
//		}				
		processSsWeight_noCase(seq, st, o);
		
////    slack and surplus from group weight		
//		for (String k: seq.groupWeightMap.keySet()){				
//			o.wtMap.put(k, convertWeight(seq.groupWeightMap.get(k)));
//		}
		processGroupWeight(seq, st, o);

		return o;
		
	}
	
	
	private static void processWeightTableObjList(SequenceTemp seq, StudyTemp st, ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (WeightTable w: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).wTableObjList) {
				
				String objGroupName = w.id_lowcase;
				
				
				//TODO: rewrite this based on f check. no need to check each obj group.
				if (fileWeightMap.contains(f, objGroupName)){
					
					out.wtMap.putAll(fileWTableObjMap.get(f, objGroupName));
				
				} else {
				
					Map<String,WeightElement> v = null;
					
					if (w.isWeightGroup) {
						v= convertWeightTableGroup(w);
					} else {
						v= convertWeightTable(w);
					}
					
					fileWTableObjMap.put(f, objGroupName,(HashMap<String, WeightElement>)v);
					
					out.wtMap.putAll(v);
					
				}	
				
			}
		}
		
		ArrayList<String> allUsedModels = new ArrayList<String>();
		allUsedModels.add(seq.model);
		if (st.allOffspringMap_incModel.keySet().contains(seq.model)) allUsedModels.addAll(st.allOffspringMap_incModel.get(seq.model));

		
		for (String e: allUsedModels){
			

				
					ModelTemp incModel = st.modelMap.get(e);
				
					for (WeightTable w: incModel.wTableObjList) {
						
						String objGroupName = w.id_lowcase;
						
						if (mainfileModelWTableObjMap.contains(e, objGroupName)){
							
							out.wtMap.putAll(mainfileModelWTableObjMap.get(e, objGroupName));
						
						} else {
						
							Map<String,WeightElement> v = null;
							
							if (w.isWeightGroup) {
								v= convertWeightTableGroup(w);
							} else {
								v= convertWeightTable(w);
							}
							
							mainfileModelWTableObjMap.put(e, objGroupName,(HashMap<String, WeightElement>)v);
							
							out.wtMap.putAll(v);
							
						}	
						
					}
					

		
		}
//		
//		for (WeightTable w: seq.wTableObjList){	
//			
//			if (! fileWTableObjMap.columnKeySet().contains(w.id.toLowerCase())) {
//				
//				Map<String,WeightElement> v = null;
//				
//				if (w.isWeightGroup) {
//					v= convertWeightTableGroup(w);
//				} else {
//					v= convertWeightTable(w);
//				}
//				
//				out.wtMap.putAll(v);
//			
//			}	
//		}
		
	}	

	private static void processTimeseries(SequenceTemp seq, StudyTemp st,
			ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).tsList) {
				
				if (fileTsMap.contains(f, k)){

					out.tsMap.put(k, fileTsMap.get(f, k));
				
				} else {
				
					Timeseries v = convertTimeseries(seq.tsMap.get(k));
					fileTsMap.put(f,k,v);
					out.tsMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: seq.tsMap.keySet()){		
			if (!out.tsMap.containsKey(k)) { out.tsMap.put(k, convertTimeseries(seq.tsMap.get(k)));
			}
		}
		
	}


	private static void processGoal(SequenceTemp seq, StudyTemp st,
			ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).glList) {
				
				if (fileGlMap.contains(f, k)){

					out.gMap.put(k, fileGlMap.get(f, k));
				
				} else {
				
					Goal v = convertGoal(seq.glMap.get(k));
					fileGlMap.put(f,k,v);
					out.gMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: seq.glMap.keySet()){		
			if (!out.gMap.containsKey(k)) out.gMap.put(k, convertGoal(seq.glMap.get(k)));
		}
		
	}


	private static void processSvar(SequenceTemp seq, StudyTemp st,
			ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).svList) {
				
				if (fileSvMap.contains(f, k)){

					out.svMap.put(k, fileSvMap.get(f, k));
				
				} else {
				
					Svar v = convertSvar(seq.svMap.get(k));
					fileSvMap.put(f,k,v);
					out.svMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: out.svList){		
			if (!out.svMap.containsKey(k)) out.svMap.put(k, convertSvar(seq.svMap.get(k)));
		}
		
	}


	private static void processDvar(SequenceTemp seq, StudyTemp st,
			ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).dvList) {
				
				if (fileDvMap.contains(f, k)){

					out.dvMap.put(k, fileDvMap.get(f, k));
				
				} else {
				
					Dvar v = convertDvar(seq.dvMap.get(k));
					fileDvMap.put(f,k,v);
					out.dvMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: out.dvList){		
			if (!out.dvMap.containsKey(k)) out.dvMap.put(k, convertDvar(seq.dvMap.get(k)));
		}	
		
	}


	private static void processSsList_noCase(SequenceTemp seq, StudyTemp st,
			ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).ssList_noCase) {
				
				if (fileSlackSurplusNoCaseMap.contains(f, k)){

					out.dvMap.put(k, fileSlackSurplusNoCaseMap.get(f, k));
				
				} else {
				
					Dvar v = convertDvar(seq.ssMap_noCase.get(k));
					fileSlackSurplusNoCaseMap.put(f,k,v);
					out.dvMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: seq.ssList_noCase){		
			if (!out.dvMap.containsKey(k)) out.dvMap.put(k, convertDvar(seq.ssMap_noCase.get(k)));
		}	
		
	}

	private static void processSsWeight_noCase(SequenceTemp seq, StudyTemp st, ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).ssWeightMap_noCase.keySet()) {
				
				if (fileSsWeightMap_noCase.contains(f, k)){

					out.wtMap.put(k, fileSsWeightMap_noCase.get(f, k));
				
				} else {
				
					WeightElement v = convertWeight(seq.ssWeightMap_noCase.get(k));
					fileSsWeightMap_noCase.put(f,k,v);
					out.wtMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: seq.ssWeightMap_noCase.keySet()){	
			if (!out.wtMap.containsKey(k)) out.wtMap.put(k, convertWeight(seq.ssWeightMap_noCase.get(k)));
		}
		
		
	}
	
	private static void processGroupWeight(SequenceTemp seq, StudyTemp st, ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).groupWeightMap.keySet()) {
				
				if (fileGroupWeightMap.contains(f, k)){

					out.wtMap.put(k, fileGroupWeightMap.get(f, k));
				
				} else {
				
					WeightElement v = convertWeight(seq.groupWeightMap.get(k));
					fileGroupWeightMap.put(f,k,v);
					out.wtMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: seq.groupWeightMap.keySet()){	
			if (!out.wtMap.containsKey(k)) out.wtMap.put(k, convertWeight(seq.groupWeightMap.get(k)));
		}
		
		
	}

	private static void processAlias(SequenceTemp seq, StudyTemp st, ModelDataSet out) {
		for (String f: seq.incFileRelativePathList_post){
			
			for (String k: st.fileModelDataTable.get(f, st.fileModelNameMap.get(f).get(0)).asList) {
				
				if (fileAsMap.contains(f, k)){

					out.asMap.put(k, fileAsMap.get(f, k));
				
				} else {
				
					Alias v = convertAlias(seq.asMap.get(k));
					fileAsMap.put(f,k,v);
					out.asMap.put(k, v);
					
				}			
			}
		}
		// TODO: can be improved...
		for (String k: out.asList){	
			if (!out.asMap.containsKey(k)) out.asMap.put(k, convertAlias(seq.asMap.get(k)));
		}
		
	}


	public static Svar processVar (SvarTemp s){
		
		Svar o = new Svar();
		try {
			o.fromWresl = s.fromWresl;
			o.line = s.line;
		} catch (Exception e) {
			System.out.println("#### error");
			System.out.println("svarName: "+s.id);
		}
		o.kind = s.kind;
		o.units = s.units;
		o.timeArraySize = s.timeArraySize;
		o.caseName = s.caseName;
		o.dependants = s.dependants;
		o.neededVarInCycleSet = s.neededVarInCycleSet;
		o.needVarFromEarlierCycle = s.needVarFromEarlierCycle;
		//o.dependants.removeAll(Param.reservedSet);
		//System.out.println(s.dependants);
		
		o.caseCondition = s.caseCondition;
		//o.caseCondition = Tools.replace_with_space(o.caseCondition);
		o.caseCondition = Tools.replace_seperator(o.caseCondition);
		//o.caseCondition = Tools.add_space_between_logical(o.caseCondition);
		
		o.caseExpression = s.caseExpression;
		//o.caseExpression = Tools.replace_with_space(o.caseExpression);
		o.caseExpression = Tools.replace_seperator(o.caseExpression);
		//o.caseExpression = Tools.add_space_between_logical(o.caseExpression);
		
		//System.out.println(o.caseExpression);
		return o;
		
	}	
	
	public static Svar convertSvar (SvarTemp s){
		
		Svar o = new Svar();
		try {
			o.fromWresl = s.fromWresl;
			o.line = s.line;
		} catch (Exception e) {
			System.out.println("#### error");
			System.out.println("svarName: "+s.id);
		}
		o.kind = s.kind;
		o.units = s.units;
		o.timeArraySize = s.timeArraySize;
		o.caseName = s.caseName;
		o.dependants = s.dependants;
		o.neededVarInCycleSet = s.neededVarInCycleSet;
		o.needVarFromEarlierCycle = s.needVarFromEarlierCycle;
		//o.dependants.removeAll(Param.reservedSet);
		//System.out.println(s.dependants);
		
		o.caseCondition = s.caseCondition;
		//o.caseCondition = Tools.replace_with_space(o.caseCondition);
		o.caseCondition = Tools.replace_seperator(o.caseCondition);
		//o.caseCondition = Tools.add_space_between_logical(o.caseCondition);
		
		o.caseExpression = s.caseExpression;
		//o.caseExpression = Tools.replace_with_space(o.caseExpression);
		o.caseExpression = Tools.replace_seperator(o.caseExpression);
		//o.caseExpression = Tools.add_space_between_logical(o.caseExpression);
		
		//System.out.println(o.caseExpression);
		//o.timeArraySize = s.timeArraySize;
		
		return o;
		
	}	
	
	public static Timeseries convertTimeseries (TimeseriesTemp t){
		
		Timeseries o = new Timeseries();
		
		o.fromWresl = t.fromWresl;
		o.line = t.line;
		o.dssBPart = t.dssBPart;
		o.convertToUnits = t.convertToUnits;
		o.kind = t.kind;
		o.units = t.units;
		
		return o;
		
	}
	
	public static Dvar convertDvar (DvarTemp d){
		
		Dvar o = new Dvar();
		
		o.fromWresl = d.fromWresl;
		o.line = d.line;
		o.lowerBound = d.lowerBound;
		o.upperBound = d.upperBound;
		o.kind = d.kind;
		o.units = d.units;
		o.condition = d.condition;
		if (d.isInteger) o.integer=Param.yes;
		
		o.timeArraySize = d.timeArraySize;
		
		return o;
		
	}


	public static WeightElement convertWeight (WeightTemp w){
		
		WeightElement o = new WeightElement();
		
		o.fromWresl = w.fromWresl;
		o.line = w.line;
		o.condition = w.condition;
		o.weight = w.weight;
		o.timeArraySize = w.timeArraySize;
		
		o.weight = Tools.replace_with_space(o.weight);
		o.weight = Tools.replace_seperator(o.weight);
		
		return o;
		
	}


	public static Map<String,WeightElement> convertWeightTable (WeightTable w){
		
		Map<String,WeightElement> om = new LinkedHashMap<String, WeightElement>();
		
		for (String s : w.varList) {
		
			WeightElement o = new WeightElement();

			o.fromWresl = w.fromWresl;
			o.line = w.varLineMap.get(s);
			o.condition = w.condition;
			o.weight = w.varWeightMap.get(s);
			
			if (w.varTimeArraySizeMap.containsKey(s)){
				o.timeArraySize = w.varTimeArraySizeMap.get(s);
			}else{
				o.timeArraySize = "0";
			}
			
			om.put(s,o);
		}

		
		return om;
		
	}

	public static Map<String,WeightElement> convertWeightTableGroup (WeightTable w){
		
		Map<String,WeightElement> om = new LinkedHashMap<String, WeightElement>();
		
		for (String s : w.varList) {
		
			WeightElement o = new WeightElement();
		
			o.fromWresl = w.fromWresl;
			o.line = w.line;
			o.condition = w.condition;
			o.weight = w.commonWeight;
			
			om.put(s,o);
		}

		//System.out.println("ToWreslData: w.subgroupMap.keySet(): "+w.subgroupMap.keySet());
		for (String k : w.subgroupMap.keySet()) {
			
			for (String s: w.subgroupMap.get(k).varList) {
				
				System.out.println("ToWreslData: w.subgroupMap.varList: "+w.subgroupMap.get(k).varList);
				
				WeightElement o = new WeightElement();
			
				o.fromWresl = w.fromWresl;
				o.line = w.line;
				o.condition = w.condition;
				o.weight = w.commonWeight;
				
				om.put(s,o);
			}
		}

		return om;
		
	}
	
	public static External convertExternal (ExternalTemp e){
		
		External o = new External();
		
		o.fromWresl = e.fromWresl;
		o.line = e.line;
		o.type = e.fileName;
		
		return o;
		
	}


	public static Goal convertGoal (GoalTemp g){
		
		Goal o = new Goal();
		
		o.fromWresl = g.fromWresl;
		o.line = g.line;
		o.caseName = g.caseName;
		o.expressionDependants = g.dependants;
		o.neededVarInCycleSet = g.neededVarInCycleSet;
		o.needVarFromEarlierCycle = g.needVarFromEarlierCycle;
		o.expressionDependants.removeAll(Param.reservedSet);
		//System.out.println(s.dependants);
		
		o.caseCondition = g.caseCondition;
		//o.caseCondition = Tools.replace_with_space(o.caseCondition);
		o.caseCondition = Tools.replace_seperator(o.caseCondition);
		//o.caseCondition = Tools.add_space_between_logical(o.caseCondition);
		
		o.caseExpression = g.caseExpression;
		//o.caseExpression = Tools.replace_with_space(o.caseExpression);
		o.caseExpression = Tools.replace_seperator(o.caseExpression);
		//o.caseExpression = Tools.add_space_between_logical(o.caseExpression);
		
		o.dvarWeightMapList = g.dvarWeightMapList;
		o.dvarSlackSurplusList = g.dvarSlackSurplusList;
		//System.out.println(o.caseExpression);
		
		o.timeArraySize = g.timeArraySize;
		return o;
		
	}


	public static Alias convertAlias (AliasTemp d){
		
		Alias o = new Alias();
		
		o.fromWresl = d.fromWresl;
		o.line = d.line;
		o.expression = Tools.replace_seperator(d.expression);
		o.kind = d.kind;
		o.units = d.units;
		o.noSolver = d.noSolver;
		o.condition = d.condition;
		o.dependants = d.dependants;
		o.neededVarInCycleSet = d.neededVarInCycleSet;
		o.needVarFromEarlierCycle = d.needVarFromEarlierCycle;
		
		o.timeArraySize = d.timeArraySize;

		return o;
		
	}

}
	
