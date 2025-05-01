package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.Tools;

public class ProcMainFile {

	private ProcMainFile() {
	}


	public static void findKidMap_incModel(StudyTemp st) {
		
		for (String f: st.modelMap.keySet()){
			
			ArrayList<String> kids = st.modelMap.get(f).incModelList;
			
			//System.out.println("st.modelMap.get(f).incModelList: "+st.modelMap.get(f).incModelList);
			
			if (kids==null){
				st.noKid_incModel.add(f);			
			} else if (kids.isEmpty()){
				st.noKid_incModel.add(f);
			} else {
				st.kidMap_incModel.put(f, new HashSet<String>(kids));
			}
			
		}

		
	}
	
	
	public static void findAllOffSpring_incModel(StudyTemp st) {


		for (String f: st.kidMap_incModel.keySet()) { 
				
			HashSet<String> a = Tools.findAllOffspring(f, st.kidMap_incModel);
			
			st.allOffspringMap_incModel.put(f, a);			
			
		}
	
	}

	
	public static void findGroupOrder_incModel(StudyTemp st) {
		
		Map<String,HashSet<String>> toBeSorted = new HashMap<String, HashSet<String>>(st.allOffspringMap_incModel);
		

		st.fileGroupOrder_incModel.add(st.noKid_incModel);
		
		
		Tools.findFileHierarchy(st.fileGroupOrder_incModel, toBeSorted);
		
		
	}

	public static void findEffectiveIncludeModel(StudyTemp st) {

		
		HashSet<String> t = new HashSet<String>();
		for (HashSet<String> e : st.kidMap_incModel.values()) {
			t.addAll(e);
		}
		
		st.incModelList_effective = new ArrayList<String>(t);
		
		
	}

	public static void findEffectiveModel(StudyTemp st) {
		
		for (String s: st.seqList){
			String modelName = st.seqMap.get(s).model;
			
			if (!st.modelList.contains(modelName)){
			
				LogUtils.errMsg("model name ["+ modelName +"] not found in sequence ["+ s +"].");
			
			} else {
	
				st.modelList_effective.add(modelName);
			}
			
			// add included model list
			for (String incM: st.modelMap.get(modelName).incModelList) {
				
				incM = incM.toLowerCase();
				
				//System.out.println("inc models: "+incM);
				
				if (!st.modelList.contains(incM)){
					
					LogUtils.errMsg("included model ["+ incM +"] not found in model ["+ modelName +"].");
				
				} else {
					
					st.incModelList_effective.add(incM);
					
				}
				
			}
			
		}
	}





	
}
