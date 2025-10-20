package wrimsv2.components;

import hec.heclib.dss.HecDss;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.Heclib;
import wrimsv2.commondata.wresldata.External;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.StudyDataSet;
import wrimsv2.commondata.wresldata.Timeseries;
import wrimsv2.evaluator.*;
import wrimsv2.external.LoadAllDll;
import wrimsv2.hdf5.HDF5Reader;
import wrimsv2.hdf5.HDF5Writer;
import wrimsv2.wreslparser.elements.StudyUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class PreRunModel {
    public PreRunModel(StudyDataSet sds) {
        setGenTableDir();
        setSelectedOutputCycles();
        if (ControlData.ovOption != 0) {
            procOVFile();
        }

        ControlData.currStudyDataSet = sds;
        VariableTimeStep.procCycleTimeStep(sds);
        ControlData.totalTimeStep = VariableTimeStep.getTotalTimeStep(sds);
        ArrayList<String> modelList = sds.getModelList();
        Map<String, ModelDataSet> modelDataSetMap = sds.getModelDataSetMap();
        ControlData.monthlyStartTime = new Date(ControlData.startYear - 1900, ControlData.startMonth - 1, TimeOperation.numberOfDays(ControlData.startMonth, ControlData.startYear));
        ControlData.dailyStartTime = new Date(ControlData.startYear - 1900, ControlData.startMonth - 1, ControlData.startDay);

		/*
		ControlData.writer = new DSSDataWriter(FilePaths.fullDvarDssPath);
		try {
			ControlData.writer.openDSSFile();
		} catch (Exception e) {
			ControlData.writer.closeDSSFile();
			Error.addEngineError("Could not open dv file. "+e);
			return;
		}
		*/

        try {
            Heclib.zset("ALLV", "", ControlData.vHecLib);
            ControlData.dvDss = HecDss.open(FilePaths.fullDvarDssPath);
        } catch (Exception e) {
            Error.addEngineError("Could not open dv file. " + e);
            ControlData.dvDss.close();
            return;
        }

        if (!(new File(FilePaths.fullInitFilePath)).exists()) {
            System.out.println("Error: Initial file " + FilePaths.fullInitFilePath + " doesn't exist.");
            System.out.println("=======Run Complete Unsuccessfully=======");
            System.exit(1);
        }
        if (!(new File(FilePaths.fullSvarFilePath)).exists()) {
            System.out.println("Error: Svar file " + FilePaths.fullSvarFilePath + " doesn't exist.");
            System.out.println("=======Run Complete Unsuccessfully=======");
            System.exit(1);
        }
        ControlData.allTsMap = sds.getTimeseriesMap();

        HecTimeSeries.setMessageLevel(0);
        long t1 = Calendar.getInstance().getTimeInMillis();
        if (FilePaths.svarFile.toLowerCase().endsWith(".h5")) {
            HDF5Reader.readTimeseries();
        } else {
            ControlData.cacheSvar = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFilePath, "*");
            if (!FilePaths.fullSvarFile2Path.equals("")) {
                ControlData.cacheSvar2 = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullSvarFile2Path, "*");
            }
            readTimeseries();
        }
        long t2 = Calendar.getInstance().getTimeInMillis();
        ControlData.t_readTs = ControlData.t_readTs + (int) (t2 - t1);

        if (FilePaths.initFile.toLowerCase().endsWith(".h5")) {
            ControlData.initHDF5 = true;
            HDF5Reader.readInitialData();
        } else {
            ControlData.initHDF5 = false;
            ControlData.cacheInit = CondensedReferenceCacheAndRead.createCondensedCache(FilePaths.fullInitFilePath, "*");
        }
        initialDvarAliasTS();

        for (int i = 0; i < modelList.size(); i++) {
            String model = modelList.get(i);
            ModelDataSet mds = modelDataSetMap.get(model);
            ControlData.currModelDataSet = mds;
            ControlData.currCycleIndex = i;
            processExternal();
        }
        new LoadAllDll(ControlData.allDll);
        System.out.println("Loading dlls done");

        if (ControlData.outputType == 1) {
            System.out.println("Create HDF5 output data structure.");
            HDF5Writer.createDataStructure();
            HDF5Writer.listCycleStaticSv(sds);
            System.out.println("HDF5 output data structure is created.");
        }

        if (!ControlData.unchangeGWRestart) setGroundwaterInitFile();
    }

    public void readTimeseries() {
        Map<String, Timeseries> tsMap = ControlData.currStudyDataSet.getTimeseriesMap();
        Map<String, ArrayList<String>> tsTimeStepMap = ControlData.currStudyDataSet.getTimeseriesTimeStepMap();
        ControlData.currEvalTypeIndex = 6;
        Set tsKeySet = tsMap.keySet();
        Iterator iterator = tsKeySet.iterator();
        while (iterator.hasNext()) {
            String tsName = (String) iterator.next();
            //System.out.println("Reading svar timeseries "+tsName);
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
        System.out.println("Timeseries Reading Done.");
    }

    public void initialDvarAliasTS() {
        DataTimeSeries.dvAliasTS = new HashMap<String, DssDataSetFixLength>();
        //if (ControlData.outputCycleToDss) {
        ControlData.cycleDataStartYear = ControlData.startYear;
        ControlData.cycleDataStartMonth = ControlData.startMonth;
        ControlData.cycleDataStartDay = ControlData.startDay;
        int totalCycleNumber = ControlData.currStudyDataSet.getModelList().size();
        DataTimeSeries.dvAliasTSCycles = new ArrayList<HashMap<String, DssDataSetFixLength>>(totalCycleNumber);
        for (int i = 0; i < totalCycleNumber; i++) {
            HashMap<String, DssDataSetFixLength> dvAliasTSCycle = new HashMap<String, DssDataSetFixLength>();
            DataTimeSeries.dvAliasTSCycles.add(dvAliasTSCycle);
        }
        //}
    }

    public void processExternal() {
        ModelDataSet mds = ControlData.currModelDataSet;
        ArrayList<String> exList = mds.exList;
        Map<String, External> exMap = mds.exMap;
        ControlData.currExMap = exMap;
        ControlData.currEvalTypeIndex = 4;
        for (String exName : exList) {
            if (!ControlData.allExternalFunction.containsKey(exName)) {
                ControlData.currEvalName = exName;
                External external = exMap.get(exName);
                ControlData.allExternalFunction.put(exName, external.type);
                if (external.type.endsWith(".dll") && !ControlData.allDll.contains(external.type)) {
                    ControlData.allDll.add(external.type);
                }
            }
        }
        //System.out.println("Load dlls for Cycle "+(ControlData.currCycleIndex+1)+" done");
    }

    public void setSelectedOutputCycles() {
        String strSelectedCycleOutput = ControlData.selectedCycleOutput.replace("'", "");
        ControlData.selectedCycles = strSelectedCycleOutput.split(",");
    }

    public void setGroundwaterInitFile() {
        if (FilePaths.groundwaterDir != null || !FilePaths.groundwaterDir.equals("")) {
            File orig = new File(FilePaths.groundwaterDir + "\\CVGroundwater.in");
            if (orig.exists()) {
                File des = new File(FilePaths.groundwaterDir + "\\CVGroundwater_bk.in");
                try {
                    Files.copy(orig.toPath(), des.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                setGroundwaterInitFilePath(des, orig);
            }
        }
    }

    public void setGroundwaterInitFilePath(File backup, File input) {
        try {
            FileReader fr = new FileReader(backup);
            BufferedReader br = new BufferedReader(fr);
            FileWriter fw = new FileWriter(input);
            BufferedWriter bw = new BufferedWriter(fw);
            String line;
            int lineCount = 1;
            while ((line = br.readLine()) != null) {
                if (line.toUpperCase().contains("INITIAL CONDITIONS DATA FILE")) {
                    if (ControlData.startMonth == 1) {
                        String initMonth = "DEC";
                        int initYear = ControlData.startYear - 1;
                        line = "Restart\\CVInitial_Restart_" + initMonth + initYear + ".dat                /6: INITIAL CONDITIONS DATA FILE (INPUT, REQUIRED)";
                    } else {
                        String initMonth = TimeOperation.monthName(ControlData.startMonth - 1);
                        int initYear = ControlData.startYear;
                        line = "Restart\\CVInitial_Restart_" + initMonth + initYear + ".dat                /6: INITIAL CONDITIONS DATA FILE (INPUT, REQUIRED)";
                    }
                }
                bw.write(line + "\n");
                lineCount++;
            }
            br.close();
            bw.close();
            fr.close();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGenTableDir() {
        String configFileName = new File(StudyUtils.configFilePath).getName();
        FilePaths.genTableDir = FilePaths.mainDirectory + "lookup" + File.separator + "gen" + File.separator + configFileName + File.separator;

        File folder = new File(FilePaths.genTableDir);
        if (folder.exists()) {
			/*
			try {
				FileUtils.cleanDirectory(folder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
        } else {
            folder.mkdirs();
        }

    }

    public void procOVFile() {
        ControlData.ovPartBC = new HashMap<String, String>();
        File ovFile = new File(ControlData.ovFile);
        if (!ovFile.exists()) {
            System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
            ControlData.ovOption = 0;
            return;
        }
        try {
            FileInputStream fs = new FileInputStream(ovFile.getAbsolutePath());
            BufferedReader br = new BufferedReader(new InputStreamReader(fs));
            String line = br.readLine();
            if (br == null) {
                System.out.println("Output variable file doesn't contain data. All the timeseries will be written to the csv file.");
            }
            while ((line = br.readLine()) != null) {
                line = line.replace(" ", "").replace("\t", "").toUpperCase();
                if (line.equals("")) return;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    ControlData.ovPartBC.put(parts[0], parts[1]);
                }
            }
            br.close();
            fs.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
            ControlData.ovOption = 0;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Output variable file has errors. All the timeseries will be written to the csv file.");
            ControlData.ovOption = 0;
        }
    }
}