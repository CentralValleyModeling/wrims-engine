package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.config.ConfigUtils;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.AliasTemp;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.IfIncItemGroup;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.ParamTemp;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.SvarTemp;
import wrimsv2.wreslplus.elements.Tools;
import wrimsv2.wreslplus.elements.WeightSubgroup;
import wrimsv2.wreslplus.elements.WeightTable;

public class ErrorCheck {
	
	
	private ErrorCheck(){}

//	// these dvars are from slack and surplus of weight group deviation penalty
//	public static boolean checkDeviationSlackSurplus(ArrayList<String> dvList_monitored, Map<String, Dvar> dvMap) {
//		
//		ArrayList<String> errorList = new ArrayList<String>();
//		
//		for (String x : dvList_monitored){
//			
//			double v = (Double) dvMap.get(x).getData().getData();
//			
//			if (v > Param.deviationSlackSurplusTolerance) {
//				
//				errorList.add(x);
//				
//			}			
//			
//		}
//		
//		if (errorList.size()>0) {
//			
//			Error.addDeviationError( " Deviation slack and surplus are not zero: "+errorList); 
//			Error.writeDeviationErrorFile("Error_deviation.txt");
//			return true;
//			
//		}
//		
//		return false;
//			
//	}	

	// these dvars are from slack and surplus of weight group deviation penalty
	public static boolean checkDeviationSlackSurplus(Map<String,Double> deviationSS_toleranceMap, Map<String, Dvar> dvMap) {
		
		ArrayList<String> errorList = new ArrayList<String>();
		
		for (String x : deviationSS_toleranceMap.keySet()){
			
			double v = (Double) dvMap.get(x).getData().getData();
			
			if (v > deviationSS_toleranceMap.get(x)) {
				
				errorList.add(x);
				Error.addDeviationError( "Tolerance of ["+ deviationSS_toleranceMap.get(x) +"] exceeded by deviation slack and surplus: ["+x+"]"); 
				
			}			
			
		}
		
		if (errorList.size()>0) {
			
			Error.writeDeviationErrorFile("Error_deviation.txt");
			Error.writeErrorLog();
			return true;
			
		}
		
		return false;
			
	}
	
	// TODO: this process alias only. need to expand to other types
	public static void checkVarUsedBeforeDefined(StudyTemp s) {
		
		for (String se : s.seqList){
			
			SequenceTemp seqObj = s.seqMap.get(se); 

			checkVarUsedBeforeDefined(seqObj);						
		}
	}
	
	public static void checkVarUsedBeforeDefined(SequenceTemp seqObj) {
		
/*		for (String key : seqObj.asMap.keySet()) {

			AliasTemp asObj = seqObj.asMap.get(key);
			
			Set<String> temp = new HashSet<String>(asObj.dependants);
			temp.removeAll(seqObj.tsList);
			temp.removeAll(seqObj.asMap.keySet());
			temp.removeAll(seqObj.dvList);
			temp.removeAll(seqObj.svMap.keySet());
			temp.removeAll(asObj.dependants_parameter);
			asObj.dependants_unknown = temp;	
			
			if (asObj.dependants_unknown.size()>0){
				String msg = "In model ["+seqObj.model+"] variable(s) not defined before use: "+asObj.dependants_unknown+" in Alias ["+asObj.id+"]";
				LogUtils.errMsgLocation(asObj.fromWresl, asObj.line, msg);
			}
	
		}	
		
		
		for (String key : seqObj.glMap.keySet()) {

			GoalTemp glObj = seqObj.glMap.get(key);
			
			Set<String> temp = new HashSet<String>(glObj.dependants);
			temp.removeAll(seqObj.tsList);
			temp.removeAll(seqObj.asMap.keySet());
			temp.removeAll(seqObj.dvList);
			temp.removeAll(seqObj.svMap.keySet());
			temp.removeAll(seqObj.exList);
			temp.removeAll(glObj.dependants_parameter);
			glObj.dependants_unknown = temp;
				
			if (glObj.dependants_unknown.size()>0){
				String msg = "In model ["+seqObj.model+"] variable(s) not defined before use: "+glObj.dependants_unknown+" in Goal ["+glObj.id+"]";
				LogUtils.errMsgLocation(glObj.fromWresl, glObj.line, msg);
			}
	
		}
		*/

		for (int i=0; i<seqObj.svIncFileList_post.size(); i++) {

			String key = seqObj.svIncFileList_post.get(i);

			SvarTemp svObj = seqObj.svMap.get(key);
			
			Set<String> temp = new HashSet<String>(svObj.dependants);
			temp.removeAll(seqObj.tsList);
			temp.removeAll(seqObj.asMap.keySet());
			temp.removeAll(seqObj.dvList);
			temp.removeAll(seqObj.svMap.keySet());
			temp.removeAll(seqObj.exList);
			temp.removeAll(svObj.dependants_parameter);
			svObj.dependants_unknown = temp;
			
				
			if (svObj.dependants_unknown.size()>0){
				String msg = "In Sequence: ["+seqObj.id+"] Svar: ["+ svObj.id + "] has unknown variable(s): "+svObj.dependants_unknown;
				LogUtils.errMsgLocation(svObj.fromWresl, svObj.line, msg);
			
			
			} else {
				
				for (String dep: svObj.dependants_svar){
					
					if ( seqObj.svIncFileList_post.indexOf(dep)> i){
						String msg = "In Sequence: ["+seqObj.id+"] Svar: ["+ svObj.id + "] has variable(s) not defined before use: "+dep;
						LogUtils.errMsgLocation(svObj.fromWresl, svObj.line, msg);
					}
					
				}
				
			}
	
		}		
		
	}

	public static boolean checkIncModelNotExistIgnoreCase (StudyTemp s){
		
		ArrayList<String> modelList_lowercase = new ArrayList<String>();
		
		modelList_lowercase.addAll(Tools.allToLowerCase(s.modelList));
		
		//System.out.println("modelList_lowercase:"+modelList_lowercase);
		
		for (ModelTemp mt: s.modelMap.values()){
			
			for ( String incM: mt.incModelList) {
				
				if ( !modelList_lowercase.contains(incM.toLowerCase())) {
					
					LogUtils.errMsg("Include model ["+ incM +"] not exist in model ["+mt.id+"].");
					
					return true;
				}
						
			}
			
		}
		
		return false;
		
	}

	public static int checkSequenceHasUniqueModel (StudyTemp s){
		
		ArrayList<String> modelList = new ArrayList<String>();
		
		//modelList_lowercase.addAll(Tools.allToLowerCase(s.modelList));
		
		
		for (SequenceTemp seq: s.seqMap.values()){
			
			modelList.add(seq.model.toLowerCase());
			
		}
		
		ArrayList<String> modelDup = findDuplicatesIgnoreCase(modelList);
		
		for (String m:modelDup){
			LogUtils.errMsg("Each sequence must define unique model. ["+ m +"] is used in multiple sequences.");
		}
		return modelDup.size();
		

		
	}
	
	public static int checkModelRedefinedIgnoreCase (StudyTemp s){
		
		ArrayList<String> modelDup = findDuplicatesIgnoreCase(s.modelList);
		
		//System.out.println("modelDup:"+modelDup);
		
		for (String m:modelDup){
			LogUtils.errMsg("Model ["+ m +"] redefined in main file.");
		}
		return modelDup.size();
		
	}
	
	public static int checkModelRedefined (StudyTemp s){
		
		ArrayList<String> modelDup = findDuplicates(s.modelList);
		
		//System.out.println("modelDup:"+modelDup);
		
		for (String m:modelDup){
			LogUtils.errMsg("Model ["+ s.modelMap.get(m).id +"] redefined in main file.");
		}
		return modelDup.size();
		
	}
	
	public static int checkVarRedefined (StudyTemp s){
		
		// check modelList itself
		// check item duplicates with model names
		
		
		// 
		int totalDup=0;
		for (String k: s.modelList){
			
			ModelTemp m = s.modelMap.get(k);
			
			totalDup += checkVarRedefined(m, s);
			
		}
		
		return totalDup;

	}	

	public static int checkVarRedefined (ModelTemp m, StudyTemp st){
				
		
		// check dvar list duplicates
		ArrayList<String> dvDup = findDuplicates(m.dvList);
		
		if (dvDup.size()>0) {
			m.dvList = removeDuplicates(m.dvList);
		
			for (String s: dvDup){
				DvarTemp dvO = m.dvMap.get(s);
				//LogUtils.errMsg("Dvar ["+dvO.id+"] redefined in file ["+ dvO.fromWresl +"].");
				String msg = "Dvar ["+dvO.id+"] redefined";
				LogUtils.errMsgLocation(dvO.fromWresl, dvO.line, msg);
			}
		}		

		// check svar defined in initial statement
		
		ArrayList<String> svDup_initialStatement = new ArrayList<String>(st.controlDataParameterMap.keySet());
		
		svDup_initialStatement.retainAll(m.svList);

		if (svDup_initialStatement.size()>0) {
		
			for (String s: svDup_initialStatement){
				SvarTemp svO = m.svMap.get(s);
				String msg = "Svar ["+svO.id+"] redefined in initial statement and model statement";
				LogUtils.errMsgLocation(svO.fromWresl, svO.line, msg);
			}
		}		
		
		// check svar list duplicates
		ArrayList<String> svDup = findDuplicates(m.svList);
		
		if (svDup.size()>0) {
			m.svList = removeDuplicates(m.svList);
		
			for (String s: svDup){
				SvarTemp svO = m.svMap.get(s);
				//LogUtils.errMsg("Svar ["+svO.id+"] redefined in file ["+ svO.fromWresl +"].");
				String msg = "Svar ["+svO.id+"] redefined";
				LogUtils.errMsgLocation(svO.fromWresl, svO.line, msg);
			}
		}
		
		// TODO: check incFile list duplicates

		
		// check wTable var duplicates
		ArrayList<String> wvDup = findDuplicates(m.wvList_post);
		
		if (wvDup.size()>0) {
			m.wvList_post = removeDuplicates(m.wvList_post);
		
			for (String s: wvDup){
				LogUtils.errMsg("Weight redefined: "+s+" in file: unknown.");
			}
		}	

		// check item list duplicates
		ArrayList<String> itemDup = findDuplicates(m.itemList);
		
		if (itemDup.size()>0) {
			m.itemList = removeDuplicates(m.itemList);
		
			for (String s: itemDup){
				String item = "Item";
				int line = 0;
				if (m.dvList.contains(s)) {line = m.dvMap.get(s).line;item="dvar";}
				else if (m.svList.contains(s)) {line = m.svMap.get(s).line;item="svar";}
				else if (m.tsList.contains(s)) {line = m.tsMap.get(s).line;item="timeseries";}
				else if (m.glList.contains(s)) {line = m.glMap.get(s).line;item="goal";}
				else if (m.asList.contains(s)) {line = m.asMap.get(s).line;item="alias";}
				else if (m.exList.contains(s)) {line = m.exMap.get(s).line;item="external";}
				//else if (m.incFileIDList.contains(s)) {line = -1;item="include file";}
				//LogUtils.errMsg("Item ["+s+"] redefined in file ["+ m.absPath +"].");
				String msg = item+" ["+s+"] redefined";
				LogUtils.errMsgLocation(m.absPath, line, msg);
			}
		}
		
		return dvDup.size()+svDup.size()+wvDup.size()+itemDup.size();
	
	}

	public static ArrayList<String> findDuplicatesIgnoreCase(ArrayList<String> a){
		
		ArrayList<String> duplicates = new ArrayList<String>();
		
		if (a.size()<1) return duplicates;
		
		ArrayList<String> a_lowercase = Tools.allToLowerCase(a);
		
		duplicates.addAll(a_lowercase);
		Set<String> varSet = new LinkedHashSet<String>();
		
		varSet.addAll(a_lowercase);
		
		for (String s: varSet) {
			duplicates.remove(s);
		}
		
		return duplicates;
	}
	
	public static ArrayList<String> findDuplicates(ArrayList<String> a){
		
		ArrayList<String> duplicates = new ArrayList<String>();
		
		if (a.size()<1) return duplicates;
		
		duplicates.addAll(a);
		Set<String> varSet = new LinkedHashSet<String>();
		
		varSet.addAll(a);
		
		for (String s: varSet) {
			duplicates.remove(s);
		}
		
		return duplicates;
	}
	
	public static ArrayList<String> removeDuplicates(ArrayList<String> a){

		Set<String> varSet = new LinkedHashSet<String>();
		
		varSet.addAll(a);
		
		return new ArrayList<String>(varSet);

	}



	public static boolean checkWeightObjList(StudyTemp st) {

		boolean ret = false;
		
		for (String seqName : st.seqList) {
			
			SequenceTemp seqObj = st.seqMap.get(seqName);

			
		//// check duplicate weight group (objective) names
			ArrayList<String> WeightObjIdList = new ArrayList<String>();
			
			for (WeightTable wt: seqObj.wTableObjList){
				
				WeightObjIdList.add(wt.id_lowcase);
				
			}
			if (findDuplicates(WeightObjIdList).size()>0) {
				
				for (String id : findDuplicates(WeightObjIdList)) {
					LogUtils.errMsg(" Objective ["+id+"] is redefined in Sequence ["+ seqName +"]");
				
				}
				
				return true;
				
			}
			
		//// check deviation penalty and tolerance "Not a number" in weight group	

			for (WeightTable wt: seqObj.wTableObjList){
				
				if (wt.isWeightGroup) {
					
					try {
						
						double p = Double.parseDouble(wt.deviationPenalty);
						
						if (p<0) {
							LogUtils.errMsg(" Deviation Penalty ["+ wt.deviationPenalty +"] in Objective ["+ wt.id_raw +"] must be a nonnegative number." );	
							ret = true;
						}
						
					} catch (Exception e) {
						
						LogUtils.errMsg(" Deviation Penalty ["+ wt.deviationPenalty +"] in Objective ["+ wt.id_raw +"] must be a pure number." );	
						
						ret = true;
					}

					try {
						
						double p = Double.parseDouble(wt.deviationTolerance);
						
						if (p<0) {
							LogUtils.errMsg(" Deviation Tolerance ["+ wt.deviationTolerance +"] in Objective ["+ wt.id_raw +"] must be a nonnegative number." );	
							ret = true;
						}
						
					} catch (Exception e) {
						
						LogUtils.errMsg(" Deviation Tolerance ["+ wt.deviationTolerance +"] in Objective ["+ wt.id_raw +"] must be a pure number." );	
						
						ret = true;
					}
					
					for (WeightSubgroup ws: wt.subgroupMap.values()){
						
						
						try {
							
							double p = Double.parseDouble(ws.deviationPenalty);
							
							if (p<0) {
								LogUtils.errMsg(" Deviation Penalty ["+ ws.deviationPenalty +"] in Objective ["+ wt.id_raw +"] must be a nonnegative number." );	
								ret = true;
							}
							
						} catch (Exception e) {
							
							LogUtils.errMsg(" Deviation Penalty ["+ ws.deviationPenalty +"] in Objective ["+ wt.id_raw +"] must be a pure number." );	
							
							ret = true;
						}
						
						try {
							
							double p = Double.parseDouble(ws.deviationTolerance);
							
							if (p<0) {
								LogUtils.errMsg(" Deviation Tolerance ["+ ws.deviationTolerance +"] in Objective ["+ wt.id_raw +"] must be a nonnegative number." );	
								ret = true;
							}
							
						} catch (Exception e) {
							
							LogUtils.errMsg(" Deviation Tolerance ["+ ws.deviationTolerance +"] in Objective ["+ wt.id_raw +"] must be a pure number." );	
							
							ret = true;
						}
						
					}

				}				

			}
			
			if (ret) return true;
			
			
		//// check duplicate variables inside the same weight group (objective)
			
			for (WeightTable wt: seqObj.wTableObjList){
				
				ArrayList<String> varList = new ArrayList<String>();
				
				if (wt.isWeightGroup) {
					varList.addAll(wt.varList);
					varList.addAll(wt.subgroupMap.keySet());
					
					for (WeightSubgroup ws: wt.subgroupMap.values()){
						
						varList.addAll(ws.varList);
					
					}
					
				}
				else {
					varList.addAll(wt.varList);

				}
				
				if (findDuplicates(varList).size()>0) {
					
					for (String var : findDuplicates(varList)) {
						LogUtils.errMsg(" Weight variable ["+var+"] in Objective ["+ wt.id_raw +"] is redefined in Sequence ["+ seqName +"]");	
					}
										
					ret = true;
				}
			}
			
			if (ret) return true;
			
	//// check duplicates variables inside the sequence
		
			ArrayList<String> varList = new ArrayList<String>();
			
			for (WeightTable wt: seqObj.wTableObjList){
				
				if (wt.isWeightGroup) {
					varList.addAll(wt.varList);
					varList.addAll(wt.subgroupMap.keySet());
					
					for (WeightSubgroup ws: wt.subgroupMap.values()){
						
						varList.addAll(ws.varList);
					
					}
					
				}
				else {
					varList.addAll(wt.varList);

				}
				
			}
			
			if (findDuplicates(varList).size()>0) {
				
				for (String var : findDuplicates(varList)) {
					LogUtils.errMsg(" Weight variable ["+var+"] is redefined in Sequence ["+ seqName +"]");	
				}
									
				ret = true;
			}
			
			if (ret) return true;
			
			
		}
		
		return false;
		
	}

	public static void checkWeightVarNotInDvar(StudyTemp s) {
		
		for (String se : s.seqList){
			
			SequenceTemp seqObj = s.seqMap.get(se); 

			checkWeightVarNotInDvar(seqObj);						
		}
	}
	
	public static void checkWeightVarNotInDvar(SequenceTemp seqObj) {
		
		Set<String> unknowns = new HashSet<String>();
		unknowns.addAll(seqObj.wvList);
		unknowns.removeAll(seqObj.dvList);
		unknowns.removeAll(seqObj.dvList_fromAlias);
		
//		System.out.println("###  seqObj.wvList:"+seqObj.wvList);
//		System.out.println("###  seqObj.dvList:"+seqObj.dvList);
//		System.out.println("###  seqObj.dvList_fromAlias:"+seqObj.dvList_fromAlias);
		
		if (unknowns.size()>0) {
			
			LogUtils.errMsg(" Weight variables "+unknowns+" are not defined as Dvar; or are not defined as Alias and show up as terms in Goal.");	
			
			
		}
		
	}

	public static boolean checkInitialVarInConfigNotDeclaredInWresl(StudyTemp st) {
		
		Set<String> undeclared = new HashSet<String>();
		
		undeclared.addAll(ConfigUtils.paramMap.keySet());
		
		undeclared.removeAll(st.parameterList);
		
		if (undeclared.size()>0) {
			
			Error.addInitialError("Initial variable(s) in Config file not declared in main wresl file:  "+undeclared);
		
			Error.writeErrorLog();
			return true;
		}
		
		return false;
	}

	public static boolean checkInitialConst(StudyTemp st) {
		
		boolean hasError = false;
		
		for (String k: st.parameterConstList){
			
			SvarTemp pt = st.parameterMap.get(k);
			String expression = pt.caseExpression.get(0);
			try {
				
				Float.parseFloat(expression);
				
			} catch (Exception e) {
				
				String msg = "Variable ["+ k +"] declared as Const type must be a number, but it's either defined or overwritten by Config file as ["+expression+"]";
				
				Error.addInitialError(msg);
				LogUtils.errMsg(msg);
				
				hasError = true;
			}	
			
		}

		if (hasError) Error.writeErrorLog();
		return hasError;
		
	}
	
	public static boolean checkParameterHasUnknownDepedants(StudyTemp st) {
		
		boolean hasError = false;
		
		//for (String k : Lists.reverse(parameterList))
		
		
		for (int i=st.parameterList.size()-1; i>=0; i-- ){
			
			String k = st.parameterList.get(i);
			
			SvarTemp pt = st.parameterMap.get(k);
			
			pt.dependants_unknown = new HashSet<String>();
			pt.dependants_unknown.addAll(pt.dependants);
			
			pt.dependants_unknown.removeAll(st.parameterList.subList(0, i));
			
			if(pt.dependants_notAllowed.size()>0){
				
				String msg = "Initial svar ["+ pt.id +"] has dependent(s) not allowed: "+pt.dependants_notAllowed;
				
				Error.addInitialError(msg);
				//LogUtils.errMsg(msg);
				LogUtils.errMsgLocation(pt.fromWresl, pt.line, msg);
				
				hasError = true;				
				
			}
			if (pt.dependants_unknown.size()>0){
				
				String msg = "Initial svar ["+ pt.id +"] has unknown dependent(s): "+pt.dependants_unknown;
				
				Error.addInitialError(msg);
				//LogUtils.errMsg(msg);
				LogUtils.errMsgLocation(pt.fromWresl, pt.line, msg);
				
				hasError = true;
						
			}
			
		}
		
		if (hasError) Error.writeErrorLog();
		return hasError;
		
	}

	public static void checkIfStatementHasUnknownDependants(StudyTemp st) {

		for (String q: st.modelList){
			
			ModelTemp m =  st.modelMap.get(q);
			
			checkIfStatementHasUnknownDependants(m, st.parameterMap.keySet());
			
			
		}
		
	}	
	
	public static boolean checkIfStatementHasUnknownDependants(ModelTemp m, Set<String> parameters) {

			
			for ( String k : m.ifIncItemGroupIDList){
				
				IfIncItemGroup iObj = m.ifIncItemGroupMap.get(k);
				
				Set<String> dependants_unknown = iObj.dependants;
				
				dependants_unknown.removeAll(parameters);
				
				if (dependants_unknown.size()>0) {
					
					String msg = "Conditional include (if, elseif) has dependant(s) not defined in the initial block "+dependants_unknown + " in file: "+iObj.fromWresl;
					//LogUtils.errMsg(msg);
					Error.addInitialError(msg);
					Error.writeErrorLog();
					return true;
					
				}
				
			}
			
		return false;
		
	}

	public static boolean checkSeqConditionHasUnknownDepedants(StudyTemp st) {
		
		boolean hasError = false;
		
		
		for (String k: st.seqList){
			
			SequenceTemp seqObj = st.seqMap.get(k);
			ArrayList<String> unknown_deps = new ArrayList<String>(seqObj.dependants);
			
			unknown_deps.removeAll(st.controlDataParameterMap.keySet());
			unknown_deps.removeAll(Param.reservedSet);


			if (unknown_deps.size()>0){
				
				String msg = "Sequence ["+ k +"] has unknown dependent(s): "+unknown_deps;
				
				Error.addInitialError(msg);
				LogUtils.errMsgLocation(seqObj.fromWresl, seqObj.line, msg);
				
				hasError = true;
						
			}
			
		}
		
		if (hasError) Error.writeErrorLog();
		return hasError;
		
	}


}
	
