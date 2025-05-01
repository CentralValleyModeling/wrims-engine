package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenStream;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.ControlData;
import wrimsv2.evaluator.ValueEvaluatorLexer;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.wreslplus.elements.AliasTemp;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.IfIncItemGroup;
import wrimsv2.wreslplus.elements.IncFileTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.SvarTemp;
import wrimsv2.wreslplus.elements.TimeseriesTemp;
import wrimsv2.wreslplus.elements.WeightTable;
import wrimsv2.components.Error;

public class ProcIfIncItemGroup {

	private ProcIfIncItemGroup() {
	}

	public static void process(StudyTemp st){
		
		for (String q: st.modelList){
			
			ModelTemp m =  st.modelMap.get(q);
			
			process(m);
			
		}
		
	}

	public static void process(ModelTemp m){
		
		for ( String k : m.ifIncItemGroupIDList){
			
			IfIncItemGroup gObj = m.ifIncItemGroupMap.get(k);
			
			// good for debug
			ControlData.currEvalName=gObj.fromWresl;
			ControlData.currEvalTypeIndex=9;
			gObj.conditionValueList = evaluateConditions(gObj.conditionList);	
			
			// find index
			int indexOfFirstTrue = gObj.conditionValueList.indexOf(true);			
			int index_ItemList = m.itemList.indexOf(gObj.id);
			int index_IncFileIDList = m.incFileIDList.indexOf(gObj.id);
			int index_svList = m.svList.indexOf(gObj.id);
			int index_dvList = m.dvList.indexOf(gObj.id);
			int index_asList = m.asList.indexOf(gObj.id);
			int index_tsList = m.tsList.indexOf(gObj.id);
			int index_glList = m.glList.indexOf(gObj.id);
			int index_gl2List = m.gl2List.indexOf(gObj.id);
			
			// when at least one condition is true
			if (indexOfFirstTrue>-1) {
			
				ArrayList<String> inc_item_list  = gObj.inc_item_list.get(indexOfFirstTrue);
				HashMap<String, IncFileTemp> incFMap = gObj.inc_files_map_list.get(indexOfFirstTrue);
				HashMap<String, SvarTemp> svarMap = gObj.inc_svar_map_list.get(indexOfFirstTrue);
				HashMap<String, DvarTemp> dvarMap = gObj.inc_dvar_map_list.get(indexOfFirstTrue);
				LinkedHashMap<String, AliasTemp> aliasMap = gObj.inc_alias_map_list.get(indexOfFirstTrue);	
				HashMap<String, TimeseriesTemp> timeseriesMap = gObj.inc_timeseries_map_list.get(indexOfFirstTrue);	
				HashMap<String, GoalTemp> goalSimpleMap = gObj.inc_goalSimple_map_list.get(indexOfFirstTrue);
				HashMap<String, GoalTemp> goalComplexMap = gObj.inc_goalComplex_map_list.get(indexOfFirstTrue);
				HashMap<String, WeightTable> weightTableMap = gObj.inc_weightTable_map_list.get(indexOfFirstTrue);
				
				// svar replace 
				m.svList.remove(index_svList);
				m.svMap.remove(gObj.id);				
				ArrayList<String> svList = new ArrayList<String>(inc_item_list);
				svList.retainAll(svarMap.keySet());
				if (svList.size()>0){
					m.svList.addAll(index_svList,svList);
					m.svMap.putAll(svarMap);
				}

				// dvar replace 
				m.dvList.remove(index_dvList);
				m.dvMap.remove(gObj.id);
				ArrayList<String> dvList = new ArrayList<String>(inc_item_list);
				dvList.retainAll(dvarMap.keySet());
				if (dvList.size()>0){
					m.dvList.addAll(index_dvList,dvList);
					m.dvMap.putAll(dvarMap);
				}				

				// alias replace 
				m.asList.remove(index_asList);
				m.asMap.remove(gObj.id);
				ArrayList<String> asList = new ArrayList<String>(inc_item_list);
				asList.retainAll(aliasMap.keySet());
				if (asList.size()>0){
					m.asList.addAll(index_asList,asList);
					m.asMap.putAll(aliasMap);
				}	
				
				// timeseries replace 
				m.tsList.remove(index_tsList);
				m.tsMap.remove(gObj.id);
				ArrayList<String> tsList = new ArrayList<String>(inc_item_list);
				tsList.retainAll(timeseriesMap.keySet());
				if (tsList.size()>0){
					m.tsList.addAll(index_tsList,tsList);
					m.tsMap.putAll(timeseriesMap);
				}

				// goalSimple and goalComplex replace 
				m.glList.remove(index_glList);
				m.glMap.remove(gObj.id);
				ArrayList<String> glList = new ArrayList<String>(inc_item_list);
				Set<String> allGoalKeys = new HashSet<String>();
				allGoalKeys.addAll(goalSimpleMap.keySet());
				allGoalKeys.addAll(goalComplexMap.keySet());
				glList.retainAll(allGoalKeys);
				if (glList.size()>0){
					m.glList.addAll(index_glList,glList);
					m.glMap.putAll(goalSimpleMap);
					m.glMap.putAll(goalComplexMap);
				}
				
				// goalComplex replace 
				m.gl2List.remove(index_gl2List);
				ArrayList<String> gl2List = new ArrayList<String>(inc_item_list);
				gl2List.retainAll(goalComplexMap.keySet());
				if (gl2List.size()>0){
					m.gl2List.addAll(index_gl2List,gl2List);
				}				

				// weightTable replace 				
				for (String wk: weightTableMap.keySet()){
					m.wTableObjList.add(weightTableMap.get(wk));
				}
				
				// item replace
				m.itemList.remove(index_ItemList);
				m.itemTypeList.remove(index_ItemList);
				m.itemList.addAll(index_ItemList, inc_item_list);

				// TODO: improve this by prepare itemTypeList before here
				for (String dummy: gObj.inc_item_list.get(indexOfFirstTrue)) {
				
					if (incFMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.incFileType);
					} 
					else if (svarMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.svType);
					} 
					else if (dvarMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.dvType);
					} 
					else if (aliasMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.asType);
					} 
					else if (timeseriesMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.tsType);
					}
					else if (goalSimpleMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.gl1Type);
					}
					else if (goalComplexMap.keySet().contains(dummy)){
						m.itemTypeList.add(index_ItemList, Param.gl2Type);
					}
					else {
						m.itemTypeList.add(index_ItemList, -999997);
					}
				}
				
				
				// inc file replace
				m.incFileIDList.removeAll(Collections.singleton(gObj.id));
				m.incFileMap.remove(gObj.id);
				
				ArrayList<String> fileIdList = new ArrayList<String>(inc_item_list);				
				fileIdList.retainAll(incFMap.keySet());
				
				if (fileIdList.size()>0){
					m.incFileIDList.addAll(index_IncFileIDList, fileIdList);
					m.incFileMap.putAll(incFMap);
				}



			}
			// no condition is true
			else { 

				// svar
				m.svList.remove(gObj.id);
				m.svMap.remove(gObj.id);

				// dvar
				m.dvList.remove(gObj.id);
				m.dvMap.remove(gObj.id);

				// alias
				m.asList.remove(gObj.id);
				m.asMap.remove(gObj.id);

				// timeseries
				m.tsList.remove(gObj.id);
				m.tsMap.remove(gObj.id);
				
				// goal
				m.glList.remove(gObj.id);
				m.gl2List.remove(gObj.id);
				m.glMap.remove(gObj.id);
				
				m.itemList.remove(index_ItemList);
				m.itemTypeList.remove(index_ItemList);
				
				// inc file
				m.incFileIDList.removeAll(Collections.singleton(gObj.id));
				m.incFileMap.remove(gObj.id);

			}

		}
		
	}

	// TODO: return the index that contains the first "true" value
	public static ArrayList<Boolean> evaluateConditions(ArrayList<String> conditionList){
		
		ArrayList<Boolean> ret = new ArrayList<Boolean>();
		
		for (String conditionExpression : conditionList) {
		
			String evalString="c: "+conditionExpression;
			ANTLRStringStream stream = new ANTLRStringStream(evalString);
			ValueEvaluatorLexer lexer = new ValueEvaluatorLexer(stream);
			TokenStream tokenStream = new CommonTokenStream(lexer);

			ValueEvaluatorParser vep = new ValueEvaluatorParser(tokenStream);
		
			try {
				
				vep.evaluator();
				ret.add(vep.evalCondition);
				//if (vep.evalCondition) return ret;
				
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(" Error in processing conditional include ...");
				
				ret.add(false);
				
			} finally {
				
				if (Error.getTotalError()>0) Error.writeErrorLog();
			}
						
		}
		
		return ret;
		
	}	
	
}
