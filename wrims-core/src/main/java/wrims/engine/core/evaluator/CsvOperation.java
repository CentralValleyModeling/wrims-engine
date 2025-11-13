package wrims.engine.core.evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import wrims.engine.core.components.ControlData;

public class CsvOperation {
	
	private String slackPrefix="slack__";
	private String surplusPrefix="surplus__";
	private Map<String, String> ovPartBC=new HashMap<String, String>();
	
	public void ouputCSV(String csvLocalPath, int scenarioIndex){
		if (ControlData.ovOption != 0){
			procOVFile();
		}
		
		try {
			System.out.println("Writing data to CSV file...");
			
			File csvFile= new File(csvLocalPath);
			csvFile.getParentFile().mkdirs();
			FileWriter fw = new FileWriter(csvFile);
			BufferedWriter bw = new BufferedWriter(fw, 8192);
			String line="id,PartA,PartF,Timestep,Units,Date_Time,Variable,Kind,Value\n";
			bw.write(line);
			Set<String> keys = DataTimeSeries.dvAliasTS.keySet();
			Iterator<String> it = keys.iterator();
			while (it.hasNext()){
				String name=it.next();
				String nameUp=DssOperation.getTSName(name).toUpperCase();
				DssDataSetFixLength dds = DataTimeSeries.dvAliasTS.get(name);
				String origKindName=dds.getKind();
				boolean isWritten=false;
				if (ControlData.ovOption==0){
					isWritten=true;
				}else{
					if (ovPartBC.containsKey(nameUp)){
						if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
							isWritten = true;
						}
					}
				}
				if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
					String timestep=dds.getTimeStep().toUpperCase();
					Date date = dds.getStartTime();
					String unitsName=formUnitsName(dds.getUnits());
					String variableName=formVariableName(nameUp);
					String kindName=formKindName(origKindName);
					double[] data = dds.getData();
					if (timestep.equals("1DAY")){
						if (!ControlData.isSimOutput) date=TimeOperation.backOneDay(date);
						for (int i=0; i<data.length; i++){
							double value = data[i];
							if (value != -901.0 && value !=-902.0){
								line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
								bw.write(line);
							}else{
								if (ControlData.isSimOutput){
									line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
									bw.write(line);
								}
							}
							date=TimeOperation.addOneDay(date);
						}
					}else{
						if (!ControlData.isSimOutput) date=TimeOperation.backOneMonth(date);
						for (int i=0; i<data.length; i++){
							double value = data[i];
							if (value != -901.0 && value !=-902.0){
								line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
								bw.write(line);
							}else{
								if (ControlData.isSimOutput){
									line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ value +"\n";
									bw.write(line);
								}
							}
							date=TimeOperation.addOneMonth(date);
						}
					}
				}
			}
			Set<String> svKeys = DataTimeSeries.svTS.keySet();
			it = svKeys.iterator();
			while (it.hasNext()){
				String name=it.next();
				String nameUp=DssOperation.getTSName(name).toUpperCase();
				DssDataSet dds = DataTimeSeries.svTS.get(name);
				String origKindName = dds.getKind();
				boolean isWritten=false;
				if (ControlData.ovOption==0){
					isWritten=true;
				}else{
					if (ovPartBC.containsKey(nameUp)){
						if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
							isWritten = true;
						}
					}
				}
				if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
					String timestep=dds.getTimeStep().toUpperCase();
					String units=dds.getUnits();
					String unitsName=formUnitsName(units);
					String convertToUnits = dds.getConvertToUnits();
					Date date = dds.getStartTime();
					String variableName=formVariableName(nameUp);
					String kindName=formKindName(origKindName);
					ArrayList<Double> data = dds.getData();
					if (timestep.equals("1DAY")){
						//date=TimeOperation.backOneDay(date);
						for (int i=0; i<data.size(); i++){
							double value = data.get(i);
							if (value != -901.0 && value !=-902.0){
								line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+ convertValue(value, units, convertToUnits, date, timestep) +"\n";
								bw.write(line);
							}
							date=TimeOperation.addOneDay(date);
						}
					}else{
						//date=TimeOperation.backOneMonth(date);
						for (int i=0; i<data.size(); i++){
							double value = data.get(i);
							if (value != -901.0 && value !=-902.0){
								line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
								bw.write(line);
							}
							date=TimeOperation.addOneMonth(date);
						}
					}
				}
			}
			if (ControlData.writeInitToDVOutput){
				keys = DataTimeSeries.dvAliasInit.keySet();
				it = keys.iterator();
				while (it.hasNext()){
					String name=it.next();
					if (ControlData.isSimOutput || svKeys.contains(name)){
						String nameUp=DssOperation.getTSName(name).toUpperCase();
						DssDataSet dds = DataTimeSeries.dvAliasInit.get(name);
						String origKindName=dds.getKind();
						boolean isWritten=false;
						if (ControlData.ovOption==0){
							isWritten=true;
						}else{
							if (ovPartBC.containsKey(nameUp)){
								if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
									isWritten = true;
								}
							}
						}
						if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
							String timestep=dds.getTimeStep().toUpperCase();
							Date date = dds.getStartTime();
							String units=dds.getUnits();
							String unitsName=formUnitsName(units);
							String convertToUnits=dds.getConvertToUnits();
							String variableName=formVariableName(nameUp);
							String kindName=formKindName(origKindName);
							ArrayList<Double> data = dds.getData();
							if (timestep.equals("1DAY")){
								date=TimeOperation.backOneDay(date);
								for (int i=0; i<data.size(); i++){
									double value = data.get(i);
									if (value != -901.0 && value !=-902.0){
										line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
										bw.write(line);
									}
									date=TimeOperation.addOneDay(date);
								}
							}else{
								date=TimeOperation.backOneMonth(date);
								for (int i=0; i<data.size(); i++){
									double value = data.get(i);
									if (value != -901.0 && value !=-902.0){
										line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
										bw.write(line);
									}
									date=TimeOperation.addOneMonth(date);
								}
							}
						}
					}
				}
			}
			if (ControlData.isSimOutput && ControlData.writeInitToDVOutput){
				keys = DataTimeSeries.svInit.keySet();
				it = keys.iterator();
				while (it.hasNext()){
					String name=it.next();
					String nameUp=DssOperation.getTSName(name).toUpperCase();
					DssDataSet dds = DataTimeSeries.svInit.get(name);
					String origKindName=dds.getKind();
					boolean isWritten=false;
					if (ControlData.ovOption==0){
						isWritten=true;
					}else{
						if (ovPartBC.containsKey(nameUp)){
							if (ovPartBC.get(nameUp).equals(origKindName.toUpperCase())){
								isWritten = true;
							}
						}
					}
					if (isWritten && !nameUp.startsWith(slackPrefix) && !nameUp.startsWith(surplusPrefix)){
						String timestep=dds.getTimeStep().toUpperCase();
						Date date = dds.getStartTime();
						String units=dds.getUnits();
						String unitsName=formUnitsName(units);
						String convertToUnits=dds.getConvertToUnits();
						String variableName=formVariableName(nameUp);
						String kindName=formKindName(origKindName);
						ArrayList<Double> data = dds.getData();
						if (timestep.equals("1DAY")){
							date=TimeOperation.backOneDay(date);
							for (int i=0; i<data.size(); i++){
								double value = data.get(i);
								if (value != -901.0 && value !=-902.0){
									line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1DAY,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
									bw.write(line);
								}
								date=TimeOperation.addOneDay(date);
							}
						}else{
							date=TimeOperation.backOneMonth(date);
							for (int i=0; i<data.size(); i++){
								double value = data.get(i);
								if (value != -901.0 && value !=-902.0){
									line = scenarioIndex+","+ControlData.partA+","+ControlData.svDvPartF+",1MON,"+unitsName+","+formDateData(date)+","+variableName+","+kindName+","+convertValue(value, units, convertToUnits, date, timestep)+"\n";
									bw.write(line);
								}
								date=TimeOperation.addOneMonth(date);
							}
						}
					}
				}
			}
			bw.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Wrote data to CSV file");
	}
	
	public void procOVFile(){
		ovPartBC=new HashMap<String, String>();
		File ovFile = new File (ControlData.ovFile);
		if (!ovFile.exists()){
			System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
			ControlData.ovOption=0;
			return;
		}
		try {
			FileInputStream fs = new FileInputStream(ovFile.getAbsolutePath());
			BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		    String line=br.readLine();
		    if (br == null) {
		    	System.out.println("Output variable file doesn't contain data. All the timeseries will be written to the csv file.");
		    };
			while((line=br.readLine()) !=null){
		    	line=line.replace(" ", "").replace("\t",  "").toUpperCase();
		    	if (line.equals("")) return;
		    	String[] parts = line.split(",");
		    	if (parts.length>=2){
		    		ovPartBC.put(parts[0], parts[1]);
		    	}	
		    }
		    br.close();
		    fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Output variable file doesn't exist. All the timeseries will be written to the csv file.");
			ControlData.ovOption=0;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Output variable file has errors. All the timeseries will be written to the csv file.");
			ControlData.ovOption=0;
		}
	}
	
	public double convertValue(double value, String units, String convertToUnits, Date date, String timestep){
		if (units.equalsIgnoreCase("cfs") && convertToUnits.equalsIgnoreCase("taf")){
			return value*factorTafToCfs(date, timestep);
		}else if (units.equalsIgnoreCase("taf") && convertToUnits.equalsIgnoreCase("cfs")){
			return value*factorCfsToTaf(date, timestep);
		}else{
			return value;
		}
	}
	
	public double factorTafToCfs(Date date, String timestep){
		if (TimeOperation.isMonthlyInterval(timestep)){
			int year=date.getYear()+1900;
			int month=date.getMonth()+1;
			int daysInMonth=TimeOperation.numberOfDays(month, year);
			return 504.1666667 / daysInMonth;
		}else{
			return 504.1666667;
		}
	}
	
	public double factorCfsToTaf(Date date, String timestep){
		if (TimeOperation.isMonthlyInterval(timestep)){
			int year=date.getYear()+1900;
			int month=date.getMonth()+1;
			int daysInMonth=TimeOperation.numberOfDays(month, year);
			return daysInMonth / 504.1666667;
		}else{
			return 1 / 504.1666667;
		}
	}
	
	public String formUnitsName(String units){
		String newUnits=units.replaceAll("/", "_").replaceAll("-", "_");
		return newUnits;
	}
	
	public String formVariableName(String name){
		String variableName = name.replaceAll("-", "_");
		return variableName;
	}
	
	public String formKindName(String name){
		String kindName = name.replaceAll("-", "_");
		return kindName;
	}
	
	public String formDateData(Date date){
		int year=date.getYear()+1900;
		int month=date.getMonth()+1;
		int day = date.getDate();
		return year+"-"+TimeOperation.monthNameNumeric(month)+"-"+TimeOperation.dayName(day)+" 00:00:00";
	}
}
