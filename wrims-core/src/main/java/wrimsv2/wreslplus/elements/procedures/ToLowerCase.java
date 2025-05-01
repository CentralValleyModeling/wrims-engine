package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.wreslplus.elements.AliasTemp;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.ExternalTemp;
import wrimsv2.wreslplus.elements.GoalCase;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.IfIncItemGroup;
import wrimsv2.wreslplus.elements.IncFileTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.ParamTemp;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.SvarTemp;
import wrimsv2.wreslplus.elements.TimeseriesTemp;
import wrimsv2.wreslplus.elements.Tools;
import wrimsv2.wreslplus.elements.WeightTable;
import wrimsv2.wreslplus.elements.WeightTemp;

public class ToLowerCase {
	
	
	private ToLowerCase(){}

	
	//TODO: model must be in sequence obj, also condition not linked yet
	public static void convert (StudyTemp s){		

		// study
		for (String key : s.parameterList) {

			SvarTemp o = svar(s.parameterMap.get(key));
			s.parameterMap.remove(key);
			s.parameterMap.put( key.toLowerCase(), o);
			
//			ParamTemp pt = new ParamTemp();
//			pt.id = s.parameterMap.get(key).id;
//			pt.expression = s.parameterMap.get(key).expression.toLowerCase();
//			pt.dependants = Tools.allToLowerCase(s.parameterMap.get(key).dependants);
			
//			s.parameterMap.remove(key);
//			s.parameterMap.put( key.toLowerCase(), o);
		}
		s.parameterList = Tools.allToLowerCase(s.parameterList);
		s.parameterConstList = Tools.allToLowerCase(s.parameterConstList);
		
		// sequence
		for (String key : s.seqList) {
			SequenceTemp n = s.seqMap.get(key);
			s.seqMap.remove(key);
			s.seqMap.put( key.toLowerCase(), sequence(n));
		}
		s.seqList = Tools.allToLowerCase(s.seqList);
		// model
		for (String key : s.modelList) {
			ModelTemp n = s.modelMap.get(key);
			s.modelMap.remove( key);
			convert(n);
			s.modelMap.put( key.toLowerCase(), n);
		}
		s.modelList = Tools.allToLowerCase(s.modelList);
	}	
	
	public static SequenceTemp sequence (SequenceTemp w){
		
		SequenceTemp o = new SequenceTemp();
		
		o.id = w.id;
		o.fromWresl = w.fromWresl.toLowerCase();
		o.line = w.line;
		o.model = w.model.toLowerCase();
		o.condition = w.condition.toLowerCase();
		o.order = w.order.toLowerCase();
		o.timeStep = w.timeStep;
		o.dependants = Tools.allToLowerCase(w.dependants);
		o.neededCycleVarMap = Tools.allToLowerCase_string_set(w.neededCycleVarMap);
		
		return o;
	
	}


	public static void convert (ModelTemp j){
		
		for (String key : j.svList) {
			if (!j.svMap.keySet().contains(key)) continue;
			SvarTemp o = svar(j.svMap.get(key));
			j.svMap.remove(key);
			j.svMap.put( key.toLowerCase(), o);
		}
		for (String key : j.dvList) {
			if (!j.dvMap.keySet().contains(key)) continue;
			DvarTemp o = dvar(j.dvMap.get(key));
			j.dvMap.remove(key);
			j.dvMap.put( key.toLowerCase(), o);
		}
		for (String key : j.tsList) {
			if (!j.tsMap.keySet().contains(key)) continue;
			TimeseriesTemp o = timeseries(j.tsMap.get(key));
			j.tsMap.remove(key);
			j.tsMap.put( key.toLowerCase(), o);
		}
		for (String key : j.exList) {
			if (!j.exMap.keySet().contains(key)) continue;
			ExternalTemp o = external(j.exMap.get(key));
			j.exMap.remove(key);
			j.exMap.put( key.toLowerCase(), o);
		}
		for (String key : j.glList) {
			if (!j.glMap.keySet().contains(key)) continue;
			GoalTemp o = goal(j.glMap.get(key));
			j.glMap.remove(key);
			j.glMap.put( key.toLowerCase(), o);
		}
//		for (String key : j.ssList) {
//			DvarTemp o = dvar(j.ssMap.get(key));
//			j.ssMap.remove(key);
//			j.ssMap.put( key.toLowerCase(), o);
//		}
//		for (String key : j.ssList) {
//			WeightTemp o = weight(j.ssWeightMap_hasCase.get(key));
//			j.ssWeightMap_hasCase.remove(key);
//			j.ssWeightMap_hasCase.put( key.toLowerCase(), o);
//		}
		for (String key : j.asList) {	
			if (!j.asMap.keySet().contains(key)) continue;
			AliasTemp o = alias(j.asMap.get(key));
			j.asMap.remove(key);
			j.asMap.put( key.toLowerCase(), o);
		}		
		for (int i=0; i<j.wTableObjList.size();i++) {	
			WeightTable o = weightTable(j.wTableObjList.get(i));
			j.wTableObjList.remove(i);
			j.wTableObjList.add(i, o);
		}
		for (String key : j.incFileIDList) {	
			if (!j.incFileMap.keySet().contains(key)) continue;
			IncFileTemp o = incFile(j.incFileMap.get(key));
			j.incFileMap.remove(key);
			j.incFileMap.put( key.toLowerCase(), o);
		}	

		for (String key : j.ifIncItemGroupIDList) {	
			if (!j.ifIncItemGroupMap.keySet().contains(key)) continue;
			IfIncItemGroup o = ifIncFileGroup(j.ifIncItemGroupMap.get(key));
			j.ifIncItemGroupMap.remove(key);
			j.ifIncItemGroupMap.put( key.toLowerCase(), o);
		}
		
		//j.svIncFileList = Tools.allToLowerCase(j.svIncFileList);
		j.incModelList = Tools.allToLowerCase(j.incModelList);
		j.incFileIDList=Tools.allToLowerCase(j.incFileIDList);
		j.ifIncItemGroupIDList=Tools.allToLowerCase(j.ifIncItemGroupIDList);
		j.itemList = Tools.allToLowerCase(j.itemList);
		j.svList = Tools.allToLowerCase(j.svList);
		j.dvList = Tools.allToLowerCase(j.dvList);
		j.dvList_fromAlias = Tools.allToLowerCase(j.dvList_fromAlias);
		j.glList = Tools.allToLowerCase(j.glList);
		j.gl2List = Tools.allToLowerCase(j.gl2List);
		j.glList_fromAlias = Tools.allToLowerCase(j.glList_fromAlias);
		j.tsList = Tools.allToLowerCase(j.tsList);
		//j.ssList = Tools.allToLowerCase(j.ssList);
		j.exList = Tools.allToLowerCase(j.exList);
		j.asList = Tools.allToLowerCase(j.asList);
		//j.asList_reduced = Tools.allToLowerCase(j.asList_reduced);
//		j.wvList_post = Tools.allToLowerCase(j.wvList_post); 
//		// there should be nothing here
//		if (j.wvList_post.size()>0) LogUtils.errMsg(" j.wvList.size()>0 ");
		
	}

	public static SvarTemp svar (SvarTemp s){
		
		if (s==null) return null;
		
		SvarTemp o = new SvarTemp();
		
		o.id = s.id;
		o.fromWresl = s.fromWresl.toLowerCase();
		o.line = s.line;
		o.kind = s.kind.toLowerCase();
		o.units = s.units.toLowerCase();
		o.caseName = Tools.allToLowerCase(s.caseName);
		o.dependants = Tools.allToLowerCase(s.dependants);
		o.dependants_notAllowed = s.dependants_notAllowed; // not converted to lowercase
		o.neededVarInCycleSet = Tools.allToLowerCase(s.neededVarInCycleSet);
		o.needVarFromEarlierCycle = s.needVarFromEarlierCycle;
		
		o.caseCondition = Tools.allToLowerCase(s.caseCondition);
		o.caseCondition = Tools.replace_with_space(o.caseCondition);
		//o.caseCondition = Tools.replace_seperator(o.caseCondition);
		o.caseCondition = Tools.add_space_between_logical(o.caseCondition);
		
		o.caseExpression = Tools.allToLowerCase(s.caseExpression);
		o.caseExpression = Tools.replace_with_space(o.caseExpression);
		//o.caseExpression = Tools.replace_seperator(o.caseExpression);
		o.caseExpression = Tools.add_space_between_logical(o.caseExpression);
		
		o.arraySize = s.arraySize.toLowerCase();
		o.timeArraySize = s.timeArraySize.toLowerCase();
		
		return o;
		
	}	
	
	public static TimeseriesTemp timeseries (TimeseriesTemp t){

		if (t==null) return null;
		
		TimeseriesTemp o = new TimeseriesTemp();
		
		o.id = t.id;
		o.fromWresl = t.fromWresl.toLowerCase();
		o.line = t.line;
		o.dssBPart = t.dssBPart.toLowerCase();
		o.convertToUnits = t.convertToUnits.toLowerCase();
		o.kind = t.kind.toLowerCase();
		o.units = t.units.toLowerCase();
		
		return o;
		
	}
	
	public static DvarTemp dvar (DvarTemp d){
		
		if (d==null) return null;
		
		DvarTemp o = new DvarTemp();
		
		o.id = d.id;
		o.fromWresl = d.fromWresl.toLowerCase();
		o.line = d.line;
		o.lowerBound = d.lowerBound.toLowerCase();
		o.upperBound = d.upperBound.toLowerCase();
		o.kind = d.kind.toLowerCase();
		o.units = d.units.toLowerCase();
		o.condition = d.condition.toLowerCase();
		
		o.isInteger = d.isInteger;
		o.isFromAlias = d.isFromAlias;
		
		o.arraySize = d.arraySize.toLowerCase();
		o.timeArraySize = d.timeArraySize.toLowerCase();
		
		return o;
	}


	public static WeightTemp weight (WeightTemp w){
		
		WeightTemp o = new WeightTemp();
		
		o.id = w.id;
		o.fromWresl = w.fromWresl.toLowerCase();
		o.line = w.line;
		o.condition = w.condition.toLowerCase();
		o.weight = w.weight.toLowerCase();
		o.timeArraySize = w.timeArraySize.toLowerCase();
		
		return o;

	}

	
	public static WeightTable weightTable (WeightTable w){
		
		WeightTable o = new WeightTable();
		
		o.id_lowcase = w.id_lowcase.toLowerCase();
		o.id_raw = w.id_raw;
		o.line = w.line;
		o.fromWresl = w.fromWresl.toLowerCase();
		o.line = w.line;
		o.condition = w.condition.toLowerCase();
		o.varList = Tools.allToLowerCase(w.varList);
		o.varWeightMap = Tools.allToLowerCase_string(w.varWeightMap);
		o.varLineMap = Tools.allToLowerCaseString(w.varLineMap);
		o.subgroupMap = Tools.allToLowerCase_weightSubgroup(w.subgroupMap);
		o.dependants = Tools.allToLowerCase(w.dependants);
		o.commonWeight = w.commonWeight.toLowerCase();
		o.deviationPenalty = w.deviationPenalty.toLowerCase();
		o.deviationTolerance = w.deviationTolerance.toLowerCase();
		o.isWeightGroup = w.isWeightGroup;
		o.varTimeArraySizeMap = Tools.allToLowerCase_string(w.varTimeArraySizeMap);
		
		return o;

	}
	

	public static ExternalTemp external (ExternalTemp e){
		
		ExternalTemp o = new ExternalTemp();
		
		o.id = e.id;
		o.fromWresl = e.fromWresl.toLowerCase();
		o.line = e.line;
		o.fileName = e.fileName.toLowerCase();
		
		return o;
		
	}

	// GoalCase is not converted to lowercase
	public static GoalTemp goal (GoalTemp g){
		
		if (g==null) return null;

		// TODO: convert fields in class GoalCase to lowercase 
		
		GoalTemp o = new GoalTemp();

		ArrayList<String> cn = new ArrayList<String>(g.caseMap.keySet());
		for (String key: cn){		
			GoalCase gc = goalCase(g.caseMap.get(key));
			//g.caseMap.remove(key);
			o.caseMap.put( key.toLowerCase(), gc);	
		}
			
		o.id = g.id;
		o.fromWresl = g.fromWresl.toLowerCase();
		o.line = g.line;
		o.caseName = Tools.allToLowerCase(g.caseName);
		o.dependants = Tools.allToLowerCase(g.dependants);
		o.neededVarInCycleSet = Tools.allToLowerCase(g.neededVarInCycleSet);
		o.needVarFromEarlierCycle = g.needVarFromEarlierCycle;

		o.hasCase = g.hasCase;
		o.hasLhs = g.hasLhs;
		
		if (g.hasLhs) {
			
			o.lhs = g.lhs.toLowerCase();
		
		} else {  // simple goal
		
			o.caseCondition = Tools.allToLowerCase(g.caseCondition);
			o.caseCondition = Tools.replace_with_space(o.caseCondition);
			//o.caseCondition = Tools.replace_seperator(o.caseCondition);
			o.caseCondition = Tools.add_space_between_logical(o.caseCondition);
			
			o.caseExpression = Tools.allToLowerCase(g.caseExpression);
			o.caseExpression = Tools.replace_with_space(o.caseExpression);
			//o.caseExpression = Tools.replace_seperator(o.caseExpression);
			o.caseExpression = Tools.add_space_between_logical(o.caseExpression);
		
		}
		
		o.arraySize = g.arraySize.toLowerCase();
		o.timeArraySize = g.timeArraySize.toLowerCase();
		
		return o;
				
	}


	public static AliasTemp alias (AliasTemp d){
		
		if (d==null) return null;
		
		AliasTemp o = new AliasTemp();
		
		o.id = d.id;
		o.fromWresl = d.fromWresl.toLowerCase();
		o.line = d.line;
		o.expression = d.expression.toLowerCase();
		o.kind = d.kind.toLowerCase();
		o.units = d.units.toLowerCase();
		o.noSolver = d.noSolver;
		o.condition = d.condition.toLowerCase();
		o.dependants = Tools.allToLowerCase(d.dependants);
		o.neededVarInCycleSet = Tools.allToLowerCase(d.neededVarInCycleSet);
		o.needVarFromEarlierCycle = d.needVarFromEarlierCycle;
		o.isMovedToDvar = d.isMovedToDvar;

		o.arraySize = d.arraySize.toLowerCase();
		o.timeArraySize = d.timeArraySize.toLowerCase();
		
		return o;
	}


	public static GoalCase goalCase (GoalCase d){
		
		GoalCase o = new GoalCase();
		
		o.id = d.id;
		o.rhs = d.rhs.toLowerCase();
		o.lhs_gt_rhs = d.lhs_gt_rhs.toLowerCase();
		o.lhs_lt_rhs = d.lhs_lt_rhs.toLowerCase();
		o.condition = d.condition.toLowerCase();

		
		return o;
	}


	public static IncFileTemp incFile (IncFileTemp w){
		
		if (w==null) return null;
		
		IncFileTemp o = new IncFileTemp();
		
		o.id = w.id;
		o.rawPath = w.rawPath.toLowerCase();
		
		return o;
	
	}

	public static IfIncItemGroup ifIncFileGroup (IfIncItemGroup w){
		
		if (w==null) return null;
		
		IfIncItemGroup o = new IfIncItemGroup();
		
		o.id = w.id;
		o.fromWresl = w.fromWresl.toLowerCase();
		o.line = w.line;
		o.dependants = Tools.allToLowerCase(w.dependants);
		o.conditionList =  Tools.allToLowerCase(w.conditionList);

		o.inc_item_list = Tools.allToLowerCase2(w.inc_item_list);
		
		for ( Map<String,IncFileTemp> mi : w.inc_files_map_list){
			
			for (String key: mi.keySet()){
				
				mi.put(key, incFile(mi.get(key)));							
			}
		}
		o.inc_files_map_list = w.inc_files_map_list;		

		for ( HashMap<String,SvarTemp> mi : w.inc_svar_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				SvarTemp svObj = svar(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), svObj);				
			}
		}
		o.inc_svar_map_list = w.inc_svar_map_list;		

		for ( HashMap<String,DvarTemp> mi : w.inc_dvar_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				DvarTemp dvObj = dvar(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), dvObj);				
			}
		}
		o.inc_dvar_map_list = w.inc_dvar_map_list;		
		
		for ( HashMap<String,AliasTemp> mi : w.inc_alias_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				AliasTemp asObj = alias(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), asObj);				
			}
		}
		o.inc_alias_map_list = w.inc_alias_map_list;		

		for ( HashMap<String,TimeseriesTemp> mi : w.inc_timeseries_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				TimeseriesTemp tsObj = timeseries(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), tsObj);				
			}
		}
		o.inc_timeseries_map_list = w.inc_timeseries_map_list;
		
		for ( HashMap<String,GoalTemp> mi : w.inc_goalSimple_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				GoalTemp glObj = goal(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), glObj);				
			}
		}
		o.inc_goalSimple_map_list = w.inc_goalSimple_map_list;
		
		for ( HashMap<String,GoalTemp> mi : w.inc_goalComplex_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				GoalTemp glObj = goal(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), glObj);				
			}
		}
		o.inc_goalComplex_map_list = w.inc_goalComplex_map_list;
		
		for ( HashMap<String,WeightTable> mi : w.inc_weightTable_map_list){
			
			Set<String> ks = new HashSet<String>(mi.keySet());
			
			for (String key: ks){
				
				WeightTable wtObj = weightTable(mi.get(key));			
				mi.remove(key);
				mi.put(key.toLowerCase(), wtObj);				
			}
		}
		o.inc_weightTable_map_list = w.inc_weightTable_map_list;
		
		
		
		return o;
	}
}
	
