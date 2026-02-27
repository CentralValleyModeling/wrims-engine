package gov.ca.water.wrims.engine.core.debug;

import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Timeseries;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.evaluator.CondensedReferenceCacheAndRead;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ReLoadSVDss {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReLoadSVDss.class);

    public ReLoadSVDss(StudyDataSet sds) {
        ControlData.currStudyDataSet = sds;

        if (!(new File(FilePaths.fullSvarFilePath)).exists()) {
            LOGGER.atInfo().setMessage("Error: Svar file " + FilePaths.fullSvarFilePath + " doesn't exist.").log();
            LOGGER.atInfo().setMessage("=======Run Complete Unsuccessfully=======").log();
            System.exit(0);
        }
        try {
            ControlData.cacheSvar = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFilePath);
            if (!FilePaths.fullSvarFile2Path.equals("")) {
                ControlData.cacheSvar2 = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFile2Path);
            }
            ControlData.allTsMap = sds.getTimeseriesMap();
            readTimeseries();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readTimeseries() {
        Map<String, Timeseries> tsMap = ControlData.currStudyDataSet.getTimeseriesMap();
        Map<String, ArrayList<String>> tsTimeStepMap = ControlData.currStudyDataSet.getTimeseriesTimeStepMap();
        ControlData.currEvalTypeIndex = 6;
        Set tsKeySet = tsMap.keySet();
        Iterator iterator = tsKeySet.iterator();
        while (iterator.hasNext()) {
            String tsName = (String) iterator.next();

            //To Do: in the svar class, add flag to see if svTS has been loaded
            if (!DataTimeSeries.lookSvDss.contains(tsName)) {
                ArrayList<String> timeStepList = tsTimeStepMap.get(tsName);
                for (String timeStep : timeStepList) {
                    DssOperation.getSVTimeseries(tsName, FilePaths.fullSvarFilePath, timeStep, 1);
                    if (!FilePaths.fullSvarFile2Path.equals("")) {
                        DssOperation.getSVTimeseries(tsName, FilePaths.fullSvarFile2Path, timeStep, 2);
                    }
                    String entryNameTS = DssOperation.entryNameTS(tsName, timeStep);
                    DataTimeSeries.lookSvDss.add(entryNameTS);
                }
            }
        }
        LOGGER.atInfo().setMessage("Timeseries Reading Done.").log();
    }
}