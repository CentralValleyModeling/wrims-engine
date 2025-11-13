package wrims.engine.core.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import wrims.engine.core.commondata.wresldata.StudyDataSet;
import wrims.engine.core.commondata.wresldata.Timeseries;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.evaluator.CondensedReferenceCacheAndRead;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssOperation;

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