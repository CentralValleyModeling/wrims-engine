package wrimsv2.evaluator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.components.ControlData;
import wrimsv2.components.Error;
import wrimsv2.components.IntDouble;

public class AssignPastCycleVariable {
	
	private ArrayList<String> modelList;
	private Map<String, ModelDataSet> mdsMap;
	private Map<String, Map<String, IntDouble>> varCycleValueMap;
	private String cycleName;
	

	public AssignPastCycleVariable(){
		StudyDataSet sds = ControlData.currStudyDataSet;
		mdsMap=sds.getModelDataSetMap();
		modelList=sds.getModelList();
		varCycleValueMap=sds.getVarCycleValueMap();
		cycleName=ControlData.currCycleName;
		
		assignSvar();
		assignAlias();
		assignDvar();
	}
	
	public void assignSvar(){
		Set<String> svarUsedByLaterCycle = ControlData.currModelDataSet.svarUsedByLaterCycle;
		Iterator iterator = svarUsedByLaterCycle.iterator();
		while (iterator.hasNext()){
			String svName=(String)iterator.next();
			boolean isAssigned=false;
			int i=ControlData.currCycleIndex-1;
			while (!isAssigned && i>=0){
				ModelDataSet mds = mdsMap.get(modelList.get(i));
				Map<String, Svar> svMap = mds.svMap;
				if (svMap.containsKey(svName)){
					IntDouble id=svMap.get(svName).getData();
					if (id !=null){
						varCycleValueMap.get(svName).put(cycleName, id);
						isAssigned=true;
					}
				}
				i=i-1;
			}
			if (!isAssigned){
				varCycleValueMap.get(svName).put(cycleName, null);
			}
		}
	}
	
	public void assignAlias(){
		Set<String> aliasUsedByLaterCycle = ControlData.currModelDataSet.aliasUsedByLaterCycle;
		Iterator iterator = aliasUsedByLaterCycle.iterator();
		while (iterator.hasNext()){
			String asName=(String)iterator.next();
			boolean isAssigned=false;
			int i=ControlData.currCycleIndex-1;
			while (!isAssigned && i>=0){
				ModelDataSet mds = mdsMap.get(modelList.get(i));
				Map<String, Alias> asMap = mds.asMap;
				if (asMap.containsKey(asName)){
					IntDouble id=asMap.get(asName).getData();
					if (id !=null){
						varCycleValueMap.get(asName).put(cycleName, id);
						isAssigned=true;
					}
				}
				i=i-1;
			}
			if (!isAssigned){
				varCycleValueMap.get(asName).put(cycleName, null);
			}
		}
	}
	
	public void assignDvar(){
		Set<String> dvarUsedByLaterCycle = ControlData.currModelDataSet.dvarUsedByLaterCycle;
		Iterator iterator = dvarUsedByLaterCycle.iterator();
		while (iterator.hasNext()){
			String dvName=(String)iterator.next();
			boolean isAssigned=false;
			int i=ControlData.currCycleIndex-1;
			while (!isAssigned && i>=0){
				ModelDataSet mds = mdsMap.get(modelList.get(i));
				Map<String, Dvar> dvMap = mds.dvMap;
				if (dvMap.containsKey(dvName)){
					IntDouble id=dvMap.get(dvName).getData();
					if (id !=null){
						varCycleValueMap.get(dvName).put(cycleName, id);
						isAssigned=true;
					}
				}
				i=i-1;
			}
			if (!isAssigned){
				varCycleValueMap.get(dvName).put(cycleName, null);
			}
		}
	}
}
