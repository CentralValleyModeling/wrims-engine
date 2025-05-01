package wrimsv2.wreslplus.elements.procedures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.wreslparser.elements.LogUtils;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.SequenceTemp;
import wrimsv2.wreslplus.elements.StudyTemp;
import wrimsv2.wreslplus.elements.Tools;
import wrimsv2.wreslplus.elements.WeightSubgroup;
import wrimsv2.wreslplus.elements.WeightTable;

public class ProcIncModel {

	private ProcIncModel() {
	}


	public static void findKidMap(StudyTemp st) {
		
		for (String f: st.modelMap.keySet()){
			
			ArrayList<String> kids = st.modelMap.get(f).incModelList;
			
			
			if (kids==null){
				st.noKid_incModel.add(f);			
			} else if (kids.isEmpty()){
				st.noKid_incModel.add(f);
			} else {
				//st.kidssMap.put(f, modelName, new HashSet<String>(kids));
				st.kidMap_incModel.put(f, new HashSet<String>(kids));
			}
			
		}
		
		System.out.println("st.kidMap_incModel"+st.kidMap_incModel);
		
	}
	
	
	public static void findAllOffSpring(StudyTemp st) {


		for (String f: st.kidMap_incModel.keySet()) { 
				
			HashSet<String> a = Tools.findAllOffspring(f, st.kidMap_incModel);
			
			st.allOffspringMap_incModel.put(f, a);			
			
		}
	
		System.out.println("st.allOffspringMap_incModel"+st.allOffspringMap_incModel);
	}

	
	public static void findFileGroupOrder(StudyTemp st) {
		
		Map<String,HashSet<String>> toBeSorted = new HashMap<String, HashSet<String>>(st.allOffspringMap_incModel);
		

		st.fileGroupOrder_incModel.add(st.noKid_incModel);
		
		
		Tools.findFileHierarchy(st.fileGroupOrder_incModel, toBeSorted);
		
		
		System.out.println("st.fileGroupOrder_incModel"+st.fileGroupOrder_incModel);
		
	}

	public static void findEffectiveIncludeModel(StudyTemp st) {

		
		HashSet<String> t = new HashSet<String>();
		for (HashSet<String> e : st.fileGroupOrder_incModel) {
			t.addAll(e);
		}
		
		st.incModelList_effective = new ArrayList<String>(t);
		
		
	}





	
}
