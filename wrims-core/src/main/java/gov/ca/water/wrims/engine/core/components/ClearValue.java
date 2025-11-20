package gov.ca.water.wrims.engine.core.components;

import java.util.ArrayList;
import java.util.Map;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Alias;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;

public class ClearValue {

	public static void clearCycleLoopValue(ArrayList<String> modelList, Map<String, ModelDataSet> modelDataSetMap){
			String model=modelList.get(ControlData.currCycleIndex);
			ModelDataSet mds=modelDataSetMap.get(model);
			ArrayList<String> dvList = mds.dvList;
			Map<String, Dvar> dvMap =mds.dvMap;
			for (String dvName: dvList){
				Dvar dvar=dvMap.get(dvName);
				dvar.setData(null);
			}
			ArrayList<String> svList = mds.svList;
			Map<String, Svar> svMap =mds.svMap;
			for (String svName: svList){
				Svar svar=svMap.get(svName);
				svar.setData(null);
			}
			ArrayList<String> asList = mds.asList;
			Map<String, Alias> asMap =mds.asMap;
			for (String asName: asList){
				Alias alias=asMap.get(asName);
				alias.setData(null);
			}
	}
	
	public static void 	clearValues(ArrayList<String> modelList, Map<String, ModelDataSet> modelDataSetMap){
		for (int i=0; i<modelList.size(); i++){
			String model=modelList.get(i);
			ModelDataSet mds=modelDataSetMap.get(model);
			ArrayList<String> dvList = mds.dvList;
			Map<String, Dvar> dvMap =mds.dvMap;
			for (String dvName: dvList){
				Dvar dvar=dvMap.get(dvName);
				dvar.setData(null);
			}
			ArrayList<String> svList = mds.svList;
			Map<String, Svar> svMap =mds.svMap;
			for (String svName: svList){
				Svar svar=svMap.get(svName);
				svar.setData(null);
			}
			ArrayList<String> asList = mds.asList;
			Map<String, Alias> asMap =mds.asMap;
			for (String asName: asList){
				Alias alias=asMap.get(asName);
				alias.setData(null);
			}
			
			mds.clearFutureSvMap();
			mds.clearFutureAsMap();
		}
	}
}
