package wrimsv2.debug;

import hec.heclib.dss.HecDss;
import hec.heclib.dss.HecDssCatalog;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.CondensedReferenceCacheAndRead;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.external.LoadAllDll;

public class ReLoadSVDss {
	public ReLoadSVDss(StudyDataSet sds){
		ControlData.currStudyDataSet=sds;
				
		if (!(new File(FilePaths.fullSvarFilePath)).exists()){ 
			System.out.println("Error: Svar file "+ FilePaths.fullSvarFilePath+" doesn't exist.");
			System.out.println("=======Run Complete Unsuccessfully=======");
			System.exit(0);
		}
		try {
	        ControlData.cacheSvar = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFilePath);
			if (!FilePaths.fullSvarFile2Path.equals("")){
		        ControlData.cacheSvar2 = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFile2Path);
			}
			ControlData.allTsMap=sds.getTimeseriesMap();
			readTimeseries();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readTimeseries(){
		Map<String, Timeseries> tsMap=ControlData.currStudyDataSet.getTimeseriesMap();
		Map<String, ArrayList<String>> tsTimeStepMap=ControlData.currStudyDataSet.getTimeseriesTimeStepMap();
		ControlData.currEvalTypeIndex=6;
		Set tsKeySet=tsMap.keySet();
		Iterator iterator=tsKeySet.iterator();
		while(iterator.hasNext()){
			String tsName=(String)iterator.next();
			//System.out.println("Reading svar timeseries "+tsName);
			//To Do: in the svar class, add flag to see if svTS has been loaded
			if (!DataTimeSeries.lookSvDss.contains(tsName)){
				ArrayList<String> timeStepList=tsTimeStepMap.get(tsName);
				for (String timeStep:timeStepList){
					DssOperation.getSVTimeseries(tsName, FilePaths.fullSvarFilePath, timeStep, 1);
					if (!FilePaths.fullSvarFile2Path.equals("")){
						DssOperation.getSVTimeseries(tsName, FilePaths.fullSvarFile2Path, timeStep, 2);
					}
					String entryNameTS=DssOperation.entryNameTS(tsName, timeStep);
					DataTimeSeries.lookSvDss.add(entryNameTS);
				}
			}
		}
		System.out.println("Timeseries Reading Done.");
	}
}