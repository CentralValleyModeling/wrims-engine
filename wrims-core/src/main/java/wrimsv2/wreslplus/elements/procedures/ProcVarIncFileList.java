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

public class ProcVarIncFileList {

	private ProcVarIncFileList() {
	}

	public static void replaceIncFile( StudyTemp st) {
		
		
		for (HashSet<String> modSet : st.fileGroupOrder_incModel ){
			
			for (String modName: modSet ){
				
				ModelTemp mObj = st.modelMap.get(modName);
				
				replaceIncFile(mObj, st);
				
			}
			
		}	
	}

	// for main file (st!=null), for included file (st==null)
	public static void replaceIncFile( ModelTemp mt, StudyTemp st) {
		
		// TODO: use retain Set to simplify codes
		mt.svIncFileList = new ArrayList<String>(mt.itemList);
		mt.svIncFileList.removeAll(mt.tsList);
		mt.svIncFileList.removeAll(mt.asList);
		mt.svIncFileList.removeAll(mt.dvList);
		mt.svIncFileList.removeAll(mt.glList);
		mt.svIncFileList.removeAll(mt.exList);
	
		mt.asIncFileList = new ArrayList<String>(mt.itemList);
		mt.asIncFileList.removeAll(mt.tsList);
		mt.asIncFileList.removeAll(mt.svList);
		mt.asIncFileList.removeAll(mt.dvList);
		mt.asIncFileList.removeAll(mt.glList);
		mt.asIncFileList.removeAll(mt.exList);
		
		mt.exIncFileList = new ArrayList<String>(mt.itemList);
		mt.exIncFileList.removeAll(mt.tsList);
		mt.exIncFileList.removeAll(mt.svList);
		mt.exIncFileList.removeAll(mt.dvList);
		mt.exIncFileList.removeAll(mt.glList);
		mt.exIncFileList.removeAll(mt.asList);
		
		mt.dvIncFileList = new ArrayList<String>(mt.itemList);
		mt.dvIncFileList.removeAll(mt.tsList);
		mt.dvIncFileList.removeAll(mt.svList);
		mt.dvIncFileList.removeAll(mt.exList);
		mt.dvIncFileList.removeAll(mt.glList);
		mt.dvIncFileList.removeAll(mt.asList);
	
		mt.glIncFileList = new ArrayList<String>(mt.itemList);
		mt.glIncFileList.removeAll(mt.tsList);
		mt.glIncFileList.removeAll(mt.svList);
		mt.glIncFileList.removeAll(mt.exList);
		mt.glIncFileList.removeAll(mt.dvList);
		mt.glIncFileList.removeAll(mt.asList);
		
		for (String f : mt.incFileMap.keySet()){
			
			int index_1;
			int index_2;
			int index_3;
			int index_4;
			int index_5;
			
			
			// f is a include model
			if (st!=null && st.incModelList_effective.contains(f)) {
			
				//System.out.println("# before:"+mt.svIncFileList);
				
				String model_label = Param.model_label+f;
				
				index_1 = mt.svIncFileList.indexOf(model_label);
				index_2 = mt.asIncFileList.indexOf(model_label);
				index_3 = mt.exIncFileList.indexOf(model_label);
				index_4 = mt.dvIncFileList.indexOf(model_label);
				index_5 = mt.glIncFileList.indexOf(model_label);
				
				mt.svIncFileList.remove(index_1);	
				mt.svIncFileList.addAll(index_1, st.modelMap.get(f).svIncFileList_post);	

				mt.asIncFileList.remove(index_2);
				mt.asIncFileList.addAll(index_2, st.modelMap.get(f).asIncFileList_post);

				mt.exIncFileList.remove(index_3);
				mt.exIncFileList.addAll(index_3, st.modelMap.get(f).exIncFileList_post);
				
				mt.dvIncFileList.remove(index_4);
				mt.dvIncFileList.addAll(index_4, st.modelMap.get(f).dvIncFileList_post);
				
				mt.glIncFileList.remove(index_5);
				mt.glIncFileList.addAll(index_5, st.modelMap.get(f).glIncFileList_post);
				//System.out.println("# after:"+mt.svIncFileList);
				
			} 
			
			// f is a include file
			else {						
				
				index_1 = mt.svIncFileList.indexOf(f);
				index_2 = mt.asIncFileList.indexOf(f);
				index_3 = mt.exIncFileList.indexOf(f);
				index_4 = mt.dvIncFileList.indexOf(f);
				index_5 = mt.glIncFileList.indexOf(f);

				mt.svIncFileList.set(index_1, mt.incFileMap.get(f).pathRelativeToRunDir);			
				mt.asIncFileList.set(index_2, mt.incFileMap.get(f).pathRelativeToRunDir);
				mt.exIncFileList.set(index_3, mt.incFileMap.get(f).pathRelativeToRunDir);
				mt.dvIncFileList.set(index_4, mt.incFileMap.get(f).pathRelativeToRunDir);
				mt.glIncFileList.set(index_5, mt.incFileMap.get(f).pathRelativeToRunDir);
				
			}
		}
		
		mt.svIncFileList_post = new ArrayList<String>(mt.svIncFileList);	
		mt.asIncFileList_post = new ArrayList<String>(mt.asIncFileList);
		mt.exIncFileList_post = new ArrayList<String>(mt.exIncFileList);
		mt.dvIncFileList_post = new ArrayList<String>(mt.dvIncFileList);
		mt.glIncFileList_post = new ArrayList<String>(mt.glIncFileList);
		
	}
	
//	public static void replaceIncFile_( ModelTemp mt) {
//		
//		// TODO: use retain Set to simplify codes
//		mt.svIncFileList = new ArrayList<String>(mt.itemList);
//		mt.svIncFileList.removeAll(mt.tsList);
//		mt.svIncFileList.removeAll(mt.asList);
//		mt.svIncFileList.removeAll(mt.dvList);
//		mt.svIncFileList.removeAll(mt.glList);
//		mt.svIncFileList.removeAll(mt.exList);
//	
//		mt.asIncFileList = new ArrayList<String>(mt.itemList);
//		mt.asIncFileList.removeAll(mt.tsList);
//		mt.asIncFileList.removeAll(mt.svList);
//		mt.asIncFileList.removeAll(mt.dvList);
//		mt.asIncFileList.removeAll(mt.glList);
//		mt.asIncFileList.removeAll(mt.exList);
//		
//		mt.exIncFileList = new ArrayList<String>(mt.itemList);
//		mt.exIncFileList.removeAll(mt.tsList);
//		mt.exIncFileList.removeAll(mt.svList);
//		mt.exIncFileList.removeAll(mt.dvList);
//		mt.exIncFileList.removeAll(mt.glList);
//		mt.exIncFileList.removeAll(mt.asList);
//		
//		for (String f : mt.incFileMap.keySet()){
//			
//			int index = mt.svIncFileList.indexOf(f);
//			mt.svIncFileList.set(index, mt.incFileMap.get(f).pathRelativeToRunDir);
//			
//			index = mt.asIncFileList.indexOf(f);
//			mt.asIncFileList.set(index, mt.incFileMap.get(f).pathRelativeToRunDir);
//			
//			index = mt.exIncFileList.indexOf(f);
//			mt.exIncFileList.set(index, mt.incFileMap.get(f).pathRelativeToRunDir);
//		}
//		
//		mt.svIncFileList_post = new ArrayList<String>(mt.svIncFileList);	
//		mt.asIncFileList_post = new ArrayList<String>(mt.asIncFileList);
//		mt.exIncFileList_post = new ArrayList<String>(mt.exIncFileList);
//		
//	}

//	public static void replaceIncModel( StudyTemp st) {
//		
//		
//		for (HashSet<String> modSet : st.fileGroupOrder_incModel ){
//			
//			for (String modName: modSet ){
//				
//				ModelTemp mObj = st.modelMap.get(modName);
//				
//				replaceIncModel(mObj, st);
//				
//			}
//			
//		}	
//		
//	}
//	
//	public static void replaceIncModel( ModelTemp mt, StudyTemp st) {
//
//		
//		for (String incM : mt.incModelList){
//			
//			int index = -1;
//			
//			ModelTemp incMObj = st.modelMap.get(incM);
//						
//			
//			
//			index = mt.svIncFileList_post.indexOf(incM);
//			mt.svIncFileList_post.remove(index);
//			mt.svIncFileList_post.addAll(index, incMObj.svIncFileList_post);
//			
//			index = mt.asIncFileList_post.indexOf(incM);
//			mt.asIncFileList_post.remove(index);
//			mt.asIncFileList_post.addAll(index, incMObj.asIncFileList_post);
//			
//			index = mt.exIncFileList_post.indexOf(incM);
//			mt.exIncFileList_post.remove(index);
//			mt.exIncFileList_post.addAll(index, incMObj.exIncFileList_post);
//		}
//				
//		
//	}
	
	
}
