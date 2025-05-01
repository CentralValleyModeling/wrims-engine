package wrimsv2.evaluator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.components.ControlData;

public class DataTimeSeries {
	public static HashMap<String, DssDataSet> svTS = new HashMap<String, DssDataSet> ();
	public static HashMap<String, DssDataSetFixLength> dvAliasTS = new HashMap<String, DssDataSetFixLength> ();
	public static ArrayList<HashMap<String, DssDataSetFixLength>> dvAliasTSCycles = new ArrayList<HashMap<String, DssDataSetFixLength>> ();
	public static HashMap<String, DssDataSet> svInit = new HashMap<String, DssDataSet> ();
	public static HashMap<String, DssDataSet> dvAliasInit = new HashMap<String, DssDataSet> ();
	public static ArrayList<String> lookSvDss=new ArrayList<String>();
	public static ArrayList<String> lookInitDss=new ArrayList<String>();
	
	public static void saveDataToTimeSeries(String dvName, String entryNameTS, double value, Dvar dvar){
		saveDataToTimeSeries(entryNameTS, value, dvar, 0);
		if (dvName.contains("__fut__")){
			String[] dvNameParts = dvName.split("__fut__");
			if (dvNameParts.length==2){
				try{
					int offset=Integer.parseInt(dvNameParts[1]);
					String newEntryNameTS=DssOperation.entryNameTS(dvNameParts[0], ControlData.timeStep);
					saveDataToTimeSeries(newEntryNameTS, value, dvar, offset);
				}catch(NumberFormatException e){
				}
			}
		}
	}
	
	public static void saveDataToTimeSeries(String entryNameTS, double value, Dvar dvar, int offset){
		if (!dvAliasTS.containsKey(entryNameTS)){
			DssDataSetFixLength dds=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
			//double[] data=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
			//dds.setData(data);
			dds.setTimeStep(ControlData.partE);
			dds.setStartTime(ControlData.memStartDate);
			dds.setUnits(dvar.units);
			dds.setKind(dvar.kind);
			dvAliasTS.put(entryNameTS,dds);
		}
		DssDataSetFixLength ddsfl = dvAliasTS.get(entryNameTS);
		double[] dataList=ddsfl.getData();
		//dataList[ControlData.currTimeStep.get(ControlData.currCycleIndex)]=id.getData().doubleValue();
		Date memStartDate = ddsfl.getStartTime();
		Date currDate =  new Date(ControlData.currYear-1900, ControlData.currMonth-1, ControlData.currDay);
		int index=TimeOperation.getNumberOfTimestep(memStartDate, currDate, ddsfl.getTimeStep())-1+offset;
		if (index<dataList.length)	dataList[index]=value;
		
		//if (ControlData.outputCycleToDss){
		int cycleIndex=ControlData.currCycleIndex;
		HashMap<String, DssDataSetFixLength> dvAliasTSCycle = dvAliasTSCycles.get(cycleIndex);
		if (!dvAliasTSCycle.containsKey(entryNameTS)){
			DssDataSetFixLength dds1=new DssDataSetFixLength(ControlData.totalTimeStep.get(ControlData.currCycleIndex));
			//double[] data1=new double[ControlData.totalTimeStep.get(ControlData.currCycleIndex)];
			//dds1.setData(data1);
			dds1.setTimeStep(ControlData.partE);
			dds1.setStartTime(ControlData.memStartDate);
			dds1.setUnits(dvar.units);
			dds1.setKind(dvar.kind);
			dvAliasTSCycle.put(entryNameTS,dds1);
		}
		double[] dataList1=dvAliasTSCycle.get(entryNameTS).getData();
		if (index<dataList1.length) dataList1[index]=value;
		//}
	}
}
