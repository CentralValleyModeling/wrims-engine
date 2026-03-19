package gov.ca.water.wrims.engine.core.hdf5;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import gov.ca.water.wrims.engine.core.commondata.wresldata.ModelDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.StudyDataSet;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import gov.ca.water.wrims.engine.core.evaluator.DataTimeSeries;
import gov.ca.water.wrims.engine.core.evaluator.DssDataSetFixLength;
import gov.ca.water.wrims.engine.core.evaluator.TimeOperation;
import gov.ca.water.wrims.engine.core.tools.General;

public class HDF5Writer {
	
	private static boolean hasDvarAliasLookup=false;
	private static String[] dvAsLookupNames;
	private static String[] dvAsLookupKinds;
	private static String[] dvAsLookupUnits;
	private static String[] dvAsLookupTimestep;
	private static int[] dvAsLookupIndex;
	private static ArrayList<String> monthlyDvarAliasList = new ArrayList<String>();
	private static ArrayList<String> dailyDvarAliasList =  new ArrayList<String>();
	private static Map<String, String> monthlyDvarAliasMap = new HashMap<String, String>();
	private static Map<String, String> dailyDvarAliasMap =  new HashMap<String, String>();
	private static Map<String, String> monthlyDvarAliasKindMap = new HashMap<String, String>();
	private static Map<String, String> dailyDvarAliasKindMap =  new HashMap<String, String>();
	private static String h5FileName="";
	private static int fid=-1;
	private static String gPartA=ControlData.partA;
	private static int gidPartA=-1;
	private static String gPartF=ControlData.svDvPartF;
	private static int gidPartF=-1;
	private static String gIOData="IO Data";
	private static int gidIOData=-1;
	private static String gInfo="Info";
	private static int gidInfo=-1;
	private static String gData="Data";
	private static int gidData=-1;
	private static String gMonthly="Monthly";
	private static int gidMonthly=-1;
	private static String gDaily="Daily";
	private static int gidDaily=-1;
	private static String gSC="Static Cycle SV";
	private static int gidSC=-1;
	private static String gDC="Dynamic Cycle SV";
	private static int gidDC=-1;
	private static String gCS="Cycles DV Alias";
	private static int gidCS=-1;
	private static String gSCMonthly="Monthly";
	private static int gidSCMonthly=-1;
	private static String gSCDaily="Daily";
	private static int gidSCDaily=-1;
	private static String gDCMonthly="Monthly";
	private static int gidDCMonthly=-1;
	private static String gDCDaily="Daily";
	private static int gidDCDaily=-1;
	private static String gCSMonthly="Monthly";
	private static int gidCSMonthly=-1;
	private static String gCSDaily="Daily";
	private static int gidCSDaily=-1;
	private static String[] monthlyCycleVarNames;
	private static String[] dailyCycleVarNames;
	private static double[][] monthlyCycleData;
	private static double[][] dailyCycleData;
	private static int monthlyDim=0;
	private static int dailyDim=0;
	
	public static void createDataStructure(){
		h5FileName=FilePaths.fullDvarHDF5Path;
		fid=HDF5Util.locateFile(h5FileName);
		
		Date date = ControlData.monthlyStartTime;
		String[] monthlyDateStr={TimeOperation.dssTimeEndDay(date.getYear()+1900, date.getMonth()+1, date.getDate())};	
		
		date = ControlData.dailyStartTime;
		String[] dailyDateStr={TimeOperation.dssTimeEndDay(date.getYear()+1900, date.getMonth()+1, date.getDate())};										
		
		if (fid<0){
			System.out.println("Failed to generate HDF5 file.");
		}else{
			try {
				gidPartA=HDF5Util.locateGroup(fid, gPartA);
				if (gidPartA>=0){
					gidPartF = HDF5Util.locateGroup(gidPartA, gPartF);
					if (gidPartF>=0){	
						gidSC=HDF5Util.locateGroup(gidPartF, gSC);
						if (gidSC>=0){	
							int tidAttr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
							long[] dims= {1};
							int sidAttr = H5.H5Screate_simple(1, dims, null);
							H5.H5Tset_size(tidAttr, 30);
							
							gidSCMonthly=HDF5Util.locateGroup(gidSC, gSCMonthly);
							if (gidSCMonthly>=0 && tidAttr>=0 && sidAttr>=0){
								int aidMonthly=-1;
								try{
									aidMonthly=H5.H5Aopen(gidSCMonthly, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidMonthly=H5.H5Acreate(gidSCMonthly, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidMonthly>=0){
																		
									HDF5Util.writeStringAttr(aidMonthly, tidAttr, monthlyDateStr, 30);
									H5.H5Aclose(aidMonthly);
								}
							}
							
							gidSCDaily=HDF5Util.locateGroup(gidSC, gSCDaily);
							if (gidSCDaily>=0 && tidAttr>=0 && sidAttr>=0){
								int aidDaily=-1;
								try{
									aidDaily=H5.H5Aopen(gidSCDaily, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidDaily=H5.H5Acreate(gidSCDaily, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidDaily>=0){
									
									HDF5Util.writeStringAttr(aidDaily, tidAttr, dailyDateStr, 30);
									H5.H5Aclose(aidDaily);
								}
							}
							
							H5.H5Tclose(tidAttr);
							H5.H5Sclose(sidAttr);
						}
						
						gidDC=HDF5Util.locateGroup(gidPartF, gDC);
						if (gidDC>=0){			
							int tidAttr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
							long[] dims= {1};
							int sidAttr = H5.H5Screate_simple(1, dims, null);
							H5.H5Tset_size(tidAttr, 30);
							
							gidDCMonthly=HDF5Util.locateGroup(gidDC, gDCMonthly);
							if (gidDCMonthly>=0 && tidAttr>=0 && sidAttr>=0){
								int aidMonthly=-1;
								try{
									aidMonthly=H5.H5Aopen(gidDCMonthly, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidMonthly=H5.H5Acreate(gidDCMonthly, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidMonthly>=0){
																		
									HDF5Util.writeStringAttr(aidMonthly, tidAttr, monthlyDateStr, 30);
									H5.H5Aclose(aidMonthly);
								}
							}
							
							gidDCDaily=HDF5Util.locateGroup(gidDC, gDCDaily);
							if (gidDCDaily>=0 && tidAttr>=0 && sidAttr>=0){
								int aidDaily=-1;
								try{
									aidDaily=H5.H5Aopen(gidDCDaily, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidDaily=H5.H5Acreate(gidDCDaily, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidDaily>=0){
									
									HDF5Util.writeStringAttr(aidDaily, tidAttr, dailyDateStr, 30);
									H5.H5Aclose(aidDaily);
								}
							}
							
							H5.H5Tclose(tidAttr);
							H5.H5Sclose(sidAttr);
						}
						
						gidCS=HDF5Util.locateGroup(gidPartF, gCS);
						if (gidCS>=0){	
							int tidAttr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
							long[] dims= {1};
							int sidAttr = H5.H5Screate_simple(1, dims, null);
							H5.H5Tset_size(tidAttr, 30);
							
							gidCSMonthly=HDF5Util.locateGroup(gidCS, gCSMonthly);
							if (gidCSMonthly>=0 && tidAttr>=0 && sidAttr>=0){
								int aidMonthly=-1;
								try{
									aidMonthly=H5.H5Aopen(gidCSMonthly, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidMonthly=H5.H5Acreate(gidCSMonthly, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidMonthly>=0){
																		
									HDF5Util.writeStringAttr(aidMonthly, tidAttr, monthlyDateStr, 30);
									H5.H5Aclose(aidMonthly);
								}
							}
							
							gidCSDaily=HDF5Util.locateGroup(gidCS, gCSDaily);
							if (gidCSDaily>=0 && tidAttr>=0 && sidAttr>=0){
								int aidDaily=-1;
								try{
									aidDaily=H5.H5Aopen(gidCSDaily, "Starting Time", HDF5Constants.H5P_DEFAULT);
								}catch (Exception e){
									aidDaily=H5.H5Acreate(gidCSDaily, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
								}
								if (aidDaily>=0){
									
									HDF5Util.writeStringAttr(aidDaily, tidAttr, dailyDateStr, 30);
									H5.H5Aclose(aidDaily);
								}
							}
							
							H5.H5Tclose(tidAttr);
							H5.H5Sclose(sidAttr);
						}
						
						gidIOData = HDF5Util.locateGroup(gidPartF, gIOData);
						if (gidIOData>=0){
							gidInfo = HDF5Util.locateGroup(gidIOData, gInfo);							
							gidData = HDF5Util.locateGroup(gidIOData, gData);
							if(gidData>=0){
								int tidAttr = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
								long[] dims= {1};
								int sidAttr = H5.H5Screate_simple(1, dims, null);
								H5.H5Tset_size(tidAttr, 30);
								
								gidMonthly = HDF5Util.locateGroup(gidData, gMonthly);
								if (gidMonthly>=0 && tidAttr>=0 && sidAttr>=0){
									int aidMonthly=-1;
									try{
										aidMonthly=H5.H5Aopen(gidMonthly, "Starting Time", HDF5Constants.H5P_DEFAULT);
									}catch (Exception e){
										aidMonthly=H5.H5Acreate(gidMonthly, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
									}
									if (aidMonthly>=0){
																			
										HDF5Util.writeStringAttr(aidMonthly, tidAttr, monthlyDateStr, 30);
										H5.H5Aclose(aidMonthly);
									}
								}
								
								gidDaily = HDF5Util.locateGroup(gidData, gDaily);
								if (gidDaily>=0 && tidAttr>=0 && sidAttr>=0){
									int aidDaily=-1;
									try{
										aidDaily=H5.H5Aopen(gidDaily, "Starting Time", HDF5Constants.H5P_DEFAULT);
									}catch (Exception e){
										aidDaily=H5.H5Acreate(gidDaily, "Starting Time", tidAttr, sidAttr, HDF5Constants.H5P_DEFAULT);
									}
									if (aidDaily>=0){
										
										HDF5Util.writeStringAttr(aidDaily, tidAttr, dailyDateStr, 30);
										H5.H5Aclose(aidDaily);
									}
								}
								
								H5.H5Tclose(tidAttr);
								H5.H5Sclose(sidAttr);
							}
						}
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void createDvarAliasLookup(){
		HashMap<String, DssDataSetFixLength> dvAliasTs = DataTimeSeries.dvAliasTS;
		int size=dvAliasTs.size();
		dvAsLookupNames=new String[size];
		dvAsLookupKinds=new String[size];
		dvAsLookupUnits=new String[size];
		dvAsLookupTimestep=new String[size];
		dvAsLookupIndex=new int[size];
		Set dvAliasCollection = dvAliasTs.keySet();
		Iterator dvarIterator = dvAliasCollection.iterator();
		int i=0;
		while(dvarIterator.hasNext()){ 
			String dvAliasName=(String)dvarIterator.next();
			DssDataSetFixLength dds = dvAliasTs.get(dvAliasName);
			String dvAliasNameMod=dvAliasName.substring(0, dvAliasName.length()-5);
			dvAsLookupNames[i]=dvAliasNameMod;
			String kind = dds.getKind();
			dvAsLookupKinds[i]=kind;
			dvAsLookupUnits[i]=dds.getUnits();
			String timestep=dds.getTimeStep();
			dvAsLookupTimestep[i]=timestep;
			int j=i+1;
			dvAsLookupIndex[i]=j;
			if (TimeOperation.isMonthlyInterval(timestep)){
				monthlyDvarAliasMap.put(dvAliasName, dvAliasNameMod);
				monthlyDvarAliasKindMap.put(dvAliasName, kind);
				monthlyDvarAliasList.add(dvAliasName);
			}else if (timestep.equals("1DAY")){
				dailyDvarAliasMap.put(dvAliasName, dvAliasNameMod);
				dailyDvarAliasKindMap.put(dvAliasName, kind);
				dailyDvarAliasList.add(dvAliasName);
			}
			i=i+1;
		}
		
		long[] dims = {size};
		try {
			int tidStringName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringName, 256);
			
			int tidStringKind = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringKind, 80);
			
			int tidStringUnit = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringUnit, 20);
			
			int tidStringTimestep = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringTimestep, 4);
			
			int tidCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, (256+80+20+4+4));
			int offset=0;
			int tidName = H5.H5Tinsert(tidCompound, "Name", offset, tidStringName);
			offset=offset+256;
			int tidKind = H5.H5Tinsert(tidCompound, "Kind", offset, tidStringKind);
			offset=offset+80;
			int tidUnit = H5.H5Tinsert(tidCompound, "Unit", offset, tidStringUnit);
			offset=offset+20;
			int tidTimestep = H5.H5Tinsert(tidCompound, "Timestep", offset, tidStringTimestep);
			offset=offset+4;
			int tidIndex = H5.H5Tinsert(tidCompound, "Index", offset, HDF5Constants.H5T_NATIVE_INT32);
			
			int sidLookup = H5.H5Screate_simple(1, dims, null);
			if (gidInfo >= 0 && sidLookup >= 0){
				int didLookup=-1;
				try{
					didLookup = H5.H5Dopen(gidInfo, "IO Variable Lookup");
				}catch(Exception e){
					didLookup = H5.H5Dcreate(gidInfo,
							"IO Variable Lookup", tidCompound,
							sidLookup, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didLookup >= 0){
					offset=0;
					
					int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
					H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
					HDF5Util.writeStringData(didLookup, tidCompoundTmp, dvAsLookupNames, 256);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
					H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
					HDF5Util.writeStringData(didLookup, tidCompoundTmp, dvAsLookupKinds, 80);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 20);
					H5.H5Tinsert(tidCompoundTmp, "Unit", offset, tidStringUnit);
					HDF5Util.writeStringData(didLookup, tidCompoundTmp, dvAsLookupUnits, 20);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 4);
					H5.H5Tinsert(tidCompoundTmp, "Timestep", offset, tidStringTimestep);
					HDF5Util.writeStringData(didLookup, tidCompoundTmp, dvAsLookupTimestep, 4);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 4);
					H5.H5Tinsert(tidCompoundTmp, "Index", offset, HDF5Constants.H5T_NATIVE_INT32);
					H5.H5Dwrite(didLookup, tidCompoundTmp,
							HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
			        		dvAsLookupIndex);
					
					H5.H5Tclose(tidCompoundTmp);
				}
				
	            H5.H5Sclose(sidLookup);
	            H5.H5Tclose(tidStringName);
	            H5.H5Tclose(tidStringKind);
	            H5.H5Tclose(tidStringUnit);
	            H5.H5Tclose(tidStringTimestep);
	            H5.H5Tclose(tidCompound);
	            H5.H5Dclose(didLookup);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void writeTimestepData(){
		if (fid>0){
			writeTimestepDvarAlias();
		}
	}
	
	public static void writeCyclesDvAlias(){
		if (fid>0 && ControlData.isOutputCycle){
			int size = DataTimeSeries.dvAliasTSCycles.size();
			for (int i=0; i<size; i++){
				int cycleI=i+1;
				String strCycleI=cycleI+"";
				if (General.isSelectedCycleOutput(strCycleI)){
					HashMap<String, DssDataSetFixLength> dvAliasCycle = DataTimeSeries.dvAliasTSCycles.get(i);
					differMonthlyDailyCycle(dvAliasCycle);
					listCycleDvAliasVariables(strCycleI);
					writeMonthlyCycleDvarAlias(strCycleI);
					writeDailyCycleDvarAlias(strCycleI);
				}
			}
		}
	}
	
	public static void writeTimestepDvarAlias(){
		System.out.println("write dvar and alias to HDF5 file");
		listMonthlyTimestepDvarAlias();
		writeMonthlyTimestepDvarAlias();
		listDailyTimestepDvarAlias();
		writeDailyTimestepDvarAlias();
	}

	public static void writeMonthlyTimestepDvarAlias_deprecated(){
		
		long[] dims={0};
		
		if (gidMonthly>=0){
			try {
				String name=monthlyDvarAliasList.get(0);
				int dim = DataTimeSeries.dvAliasTS.get(name).getData().length;
				dims[0]=dim;
				
				int size=monthlyDvarAliasList.size();
				int tableCount=(size-1)/1000+1;
				int lastTableColCount=size%1000;
				if (lastTableColCount==0) lastTableColCount=1000;
	
				for (int k=0; k<tableCount; k++){
					int offset=0;
					int endi, colCount;
					
					int kk=k+1;
					if (kk<tableCount){
						endi=1000*kk;
						colCount=1000;
					}else{
						endi=size;
						colCount=lastTableColCount;
					}
					String dName="Timestep Table "+kk;
					
					int tidCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8*colCount);
					for (int i=k*1000; i<endi; i++){
						name=monthlyDvarAliasList.get(i);
						int j=i-k*1000;
					
						int tidValue = H5.H5Tinsert(tidCompound, String.valueOf(j), offset, HDF5Constants.H5T_NATIVE_DOUBLE);
						offset=offset+8;
					}
					H5.H5Tclose(tidCompound);
					
					int sidTDA = H5.H5Screate_simple(1, dims, null);
					if (sidTDA >= 0){
						int didTDA = H5.H5Dcreate(gidMonthly, dName, tidCompound, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				
						if (didTDA >= 0){
							offset=0;
							for (int i=k*1000; i<endi; i++){
								name=monthlyDvarAliasList.get(i);
								int j=i=k*1000;
								
								int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);						
								H5.H5Tinsert(tidCompoundTmp, String.valueOf(j), offset, HDF5Constants.H5T_NATIVE_DOUBLE);
						
								double[] data = DataTimeSeries.dvAliasTS.get(name).getData();
								
								H5.H5Dwrite(didTDA, tidCompoundTmp, 
							        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, data);
							
								H5.H5Tclose(tidCompoundTmp);
							}
						}
						H5.H5Dclose(didTDA);
					}	
					H5.H5Sclose(sidTDA);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
public static void writeMonthlyTimestepDvarAlias(){
		
		long[] dims={0,0};
		
		if (gidMonthly>=0 && monthlyDvarAliasList.size()>0){
			try {		
				String name=monthlyDvarAliasList.get(0);
				int dim = DataTimeSeries.dvAliasTS.get(name).getData().length;
				
				int size=monthlyDvarAliasList.size();
				
				dims[0]=dim;
				dims[1]=size;
				double[][] write_data=new double[dim][size];
				
				String dName="Timestep Table";
					
				int sidTDA = H5.H5Screate_simple(2, dims, null);
				if (sidTDA >= 0){
					int didTDA=-1;
					try{
						didTDA = H5.H5Dopen(gidMonthly, dName);
					}catch (Exception e){
						didTDA = H5.H5Dcreate(gidMonthly, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
					}
					
					//int didTDA = H5.H5Dcreate(gidMonthly, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				
					if (didTDA >= 0){
						for (int i=0; i<size; i++){
								name=monthlyDvarAliasList.get(i);
											
								double[] data = DataTimeSeries.dvAliasTS.get(name).getData();
								
								for (int j=0; j<dim; j++){
									write_data[j][i]=data[j];
								}
								
							}
						}
				
						H5.H5Dwrite(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
						HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, write_data);
					
						H5.H5Dclose(didTDA);
					}	
					H5.H5Sclose(sidTDA);

			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void writeDailyTimestepDvarAlias(){
		
		long[] dims={0,0};
		
		if (gidDaily>=0 && dailyDvarAliasList.size()>0){
			try {		
				String name=dailyDvarAliasList.get(0);
				int dim = DataTimeSeries.dvAliasTS.get(name).getData().length;
				
				int size=dailyDvarAliasList.size();
				
				dims[0]=dim;
				dims[1]=size;
				double[][] write_data=new double[dim][size];
				
				String dName="Timestep Table";
					
				int sidTDA = H5.H5Screate_simple(2, dims, null);
				if (sidTDA >= 0){
					int didTDA = -1;
					try{
						didTDA = H5.H5Dopen(gidDaily, dName);
					}catch (Exception e){
						didTDA = H5.H5Dcreate(gidDaily, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
					}
				
					if (didTDA >= 0){
						for (int i=0; i<size; i++){
								name=dailyDvarAliasList.get(i);
											
								double[] data = DataTimeSeries.dvAliasTS.get(name).getData();
								
								for (int j=0; j<dim; j++){
									write_data[j][i]=data[j];
								}
								
							}
						}
				
						H5.H5Dwrite(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
						HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, write_data);
					
						H5.H5Dclose(didTDA);
					}	
					H5.H5Sclose(sidTDA);

			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void listMonthlyTimestepDvarAlias(){
		int size=monthlyDvarAliasList.size();
		long[] dims = {size};
		try {
			int tidStringName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringName, 256);
			
			int tidStringKind = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringKind, 80);
			
			int tidCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, (256+80));
			int offset=0;
			int tidName = H5.H5Tinsert(tidCompound, "Name", offset, tidStringName);
			offset=offset+256;
			int tidKind = H5.H5Tinsert(tidCompound, "Kind", offset, tidStringKind);
							
			int sidList = H5.H5Screate_simple(1, dims, null);
			if (gidInfo >= 0 && sidList >= 0 && size>0){
				int didList=-1;
				try{
					didList = H5.H5Dopen(gidMonthly, "Timestep List");
				}catch(Exception e){
					didList = H5.H5Dcreate(gidMonthly,
							"Timestep List", tidCompound,
							sidList, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didList >= 0){
					String[] nameList=new String[size];
					String[] kindList=new String[size];
					for (int i=0; i<size; i++){
						nameList[i]=monthlyDvarAliasMap.get(monthlyDvarAliasList.get(i));
						kindList[i]=monthlyDvarAliasKindMap.get(monthlyDvarAliasList.get(i));
					}
					
					offset=0;
					
					int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
					H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
					HDF5Util.writeStringData(didList, tidCompoundTmp, nameList, 256);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
					H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
					HDF5Util.writeStringData(didList, tidCompoundTmp, kindList, 80);
					
					H5.H5Tclose(tidCompoundTmp);
				}
				
	            H5.H5Sclose(sidList);
	            H5.H5Tclose(tidStringName);
	            H5.H5Tclose(tidStringKind);
	            H5.H5Tclose(tidCompound);
	            H5.H5Dclose(didList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void listDailyTimestepDvarAlias(){
		int size=dailyDvarAliasList.size();
		long[] dims = {size};
		try {
			int tidStringName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringName, 256);
			
			int tidStringKind = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidStringKind, 80);
			
			int tidCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, (256+80));
			int offset=0;
			int tidName = H5.H5Tinsert(tidCompound, "Name", offset, tidStringName);
			offset=offset+256;
			int tidKind = H5.H5Tinsert(tidCompound, "Kind", offset, tidStringKind);
							
			int sidList = H5.H5Screate_simple(1, dims, null);
			if (gidInfo >= 0 && sidList >= 0 && size>0){
				int didList=-1;
				try{
					didList = H5.H5Dopen(gidDaily, "Timestep List");
				}catch(Exception e){
					didList = H5.H5Dcreate(gidDaily,
							"Timestep List", tidName,
							sidList, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didList >= 0){
					String[] nameList=new String[size];
					String[] kindList=new String[size];
					for (int i=0; i<size; i++){
						nameList[i]=dailyDvarAliasMap.get(dailyDvarAliasList.get(i));
						kindList[i]=dailyDvarAliasKindMap.get(dailyDvarAliasList.get(i));
					}
					
					int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
					H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
					HDF5Util.writeStringData(didList, tidCompoundTmp, nameList, 256);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
					H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
					HDF5Util.writeStringData(didList, tidCompoundTmp, kindList, 80);
					
					H5.H5Tclose(tidCompoundTmp);
				}
				
	            H5.H5Sclose(sidList);
	            H5.H5Tclose(tidStringName);
	            H5.H5Tclose(tidStringKind);
	            H5.H5Tclose(tidCompound);
	            H5.H5Dclose(didList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void writeOneCycleSv(ModelDataSet mds, int index){
		if (fid>0){
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)) {
				writeMonthlyCycleSv(mds, index);
			}else if (ControlData.timeStep.equals("1DAY")){
				writeDailyCycleSv(mds, index);
			}
		}
	}
	
	public static void writeMonthlyCycleSv(ModelDataSet mds, int index){
		
		if (gidSCMonthly>=0) writeMonthlyCycleStaticSv(mds, index);
		if (gidDCMonthly>=0) writeMonthlyCycleDynamicSv(mds, index);
		
	}
	
	public static void writeDailyCycleSv(ModelDataSet mds, int index){
		if (gidSCDaily>=0) writeDailyCycleStaticSv(mds, index);
		if (gidDCDaily>=0) writeDailyCycleDynamicSv(mds, index);
	}
	
	public static void listCycleStaticSv(StudyDataSet sds){
		ArrayList<String> ml = sds.getModelList();
		Map<String, ModelDataSet> mm = sds.getModelDataSetMap();
		ArrayList<String> mtsl = sds.getModelTimeStepList();
		int size=ml.size();
		for (int i=0; i<size; i++){
			String model=ml.get(i);
			ModelDataSet mds = mm.get(model);
			String timestep = mtsl.get(i);
			int cycleI=i+1;
			String strCycleI=cycleI+"";
			
			if (ControlData.isOutputCycle && General.isSelectedCycleOutput(strCycleI)){
				if (TimeOperation.isMonthlyInterval(timestep)){
					if (gidSCMonthly>=0) HDF5Util.writeCycleStaticSvNames(mds, gidSCMonthly, i);
				}else if (timestep.equals("1DAY")){
					if (gidSCDaily>=0) HDF5Util.writeCycleStaticSvNames(mds, gidSCDaily, i);
				}
			}
		}
	}
	
	public static void differMonthlyDailyCycle(HashMap<String, DssDataSetFixLength> dvAliasCycle){
		ArrayList<String> monthlyCycleVarNamesArr = new ArrayList<String>();
		ArrayList<String> dailyCycleVarNamesArr = new ArrayList<String>();
		ArrayList<double[]> monthlyCycleDataArr = new ArrayList<double[]>();
		ArrayList<double[]> dailyCycleDataArr = new ArrayList<double[]>();
		Set dvAliasCollection = dvAliasCycle.keySet();
		Iterator dvarIterator = dvAliasCollection.iterator();
		while(dvarIterator.hasNext()){ 
			String dvAliasName=(String)dvarIterator.next();
			DssDataSetFixLength dds = dvAliasCycle.get(dvAliasName);
			String dvAliasNameMod=dvAliasName.substring(0, dvAliasName.length()-5);
			String timestep = dds.getTimeStep();
			if (TimeOperation.isMonthlyInterval(timestep)){
				monthlyCycleVarNamesArr.add(dvAliasNameMod);
				monthlyCycleDataArr.add(dds.getData());
			}else{
				dailyCycleVarNamesArr.add(dvAliasNameMod);
				dailyCycleDataArr.add(dds.getData());
			}
		}
		
		int monthlySize=monthlyCycleVarNamesArr.size();
		if (monthlySize==0){
			monthlyDim=0;
			monthlyCycleVarNames = new String[monthlySize];
			monthlyCycleData=new double[monthlyDim][monthlySize];
		}else{
			monthlyDim=monthlyCycleDataArr.get(0).length;
			monthlyCycleVarNames = new String[monthlySize];
			monthlyCycleData=new double[monthlyDim][monthlySize];
			for (int i=0; i<monthlySize; i++){
				monthlyCycleVarNames[i]=monthlyCycleVarNamesArr.get(i);
				double[] data = monthlyCycleDataArr.get(i);
				for (int j=0; j<monthlyDim; j++){
					monthlyCycleData[j][i]=data[j];
				}
			}
		}
		
		int dailySize=dailyCycleVarNamesArr.size();
		if (dailySize==0){
			dailyDim=0;
			dailyCycleVarNames = new String[dailySize];
			dailyCycleData=new double[dailyDim][dailySize];
		}else{
			dailyDim = dailyCycleDataArr.get(0).length;
			dailyCycleVarNames = new String[dailySize];
			dailyCycleData=new double[dailyDim][dailySize];
			for (int i=0; i<dailySize; i++){
				dailyCycleVarNames[i]=dailyCycleVarNamesArr.get(i);
				double[] data = dailyCycleDataArr.get(i);
				for (int j=0; j<dailyDim; j++){
					dailyCycleData[j][i]=data[j];
				}
			}
		}
	}
	
	public static void listCycleDvAliasVariables(String strCycleI){
		if (gidCSMonthly>=0) HDF5Util.writeCycleVariableNames(monthlyCycleVarNames, gidCSMonthly, strCycleI);
		if (gidCSDaily>=0) HDF5Util.writeCycleVariableNames(dailyCycleVarNames, gidCSDaily, strCycleI);
	}
	
	public static void writeMonthlyCycleDvarAlias(String strCycleI){
		
		long[] dims={0,0};
		int size=monthlyCycleVarNames.length;
		
		if (gidCSMonthly>=0 && size>0){
			try {
				double[][] write_data=monthlyCycleData;
				dims[0]=monthlyDim;
				dims[1]=size;
				
				String dName="Cycle "+strCycleI+" Table";
					
				int sidTDA = H5.H5Screate_simple(2, dims, null);
				if (sidTDA >= 0){
					int didTDA=-1;
					try{
						didTDA = H5.H5Dopen(gidCSMonthly, dName);
					}catch (Exception e){
						didTDA = H5.H5Dcreate(gidCSMonthly, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
					}
					
					//int didTDA = H5.H5Dcreate(gidMonthly, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				
				
					if (didTDA >= 0){		
						H5.H5Dwrite(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
						HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, write_data);
					
						H5.H5Dclose(didTDA);
					}	
					H5.H5Sclose(sidTDA);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void writeDailyCycleDvarAlias(String strCycleI){
		
		long[] dims={0,0};
		int size=dailyCycleVarNames.length;
		
		if (gidCSDaily>=0 && size>0){
			try {
				double[][] write_data=dailyCycleData;
				dims[0]=dailyDim;
				dims[1]=size;
				
				String dName="Cycle "+strCycleI+" Table";
					
				int sidTDA = H5.H5Screate_simple(2, dims, null);
				if (sidTDA >= 0){
					int didTDA=-1;
					try{
						didTDA = H5.H5Dopen(gidCSDaily, dName);
					}catch (Exception e){
						didTDA = H5.H5Dcreate(gidCSDaily, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
					}
					
					//int didTDA = H5.H5Dcreate(gidMonthly, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidTDA, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				
				
					if (didTDA >= 0){		
						H5.H5Dwrite(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
						HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, write_data);
					
						H5.H5Dclose(didTDA);
					}	
					H5.H5Sclose(sidTDA);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void writeMonthlyCycleStaticSv(ModelDataSet mds, int index){
		
		//ArrayList<String> dvList = mds.dvList;
		ArrayList<String> svList = mds.svList;
		//ArrayList<String> asList = mds.asList;
		//Map<String, Dvar> dvMap = mds.dvMap;
		Map<String, Svar> svMap = mds.svMap;
		//Map<String, Alias> asMap = mds.asMap;
		//int dvSize = dvList.size();
		int svSize = svList.size();
		//int asSize = asList.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		double[][] write_data = new double[1][size];
		
		/*
		for (int i=0; i<dvSize; i++){
			String name=dvList.get(i);
						
			double data = dvMap.get(name).data.getData().doubleValue();
			write_data[0][i]=data;						
		}
		
		int offset=dvSize;
		for (int i=0; i<asSize; i++){
			String name=asList.get(i);
						
			double data = asMap.get(name).data.getData().doubleValue();
			write_data[0][i+offset]=data;						
		}
		offset=dvSize+asSize;
		*/
		
		for (int i=0; i<svSize; i++){
			String name=svList.get(i);
						
			double data = svMap.get(name).getData().getData().doubleValue();
			write_data[0][i]=data;						
		}
		
		HDF5Util.writeCycleStaticSv(index, size, gidSCMonthly, write_data);
	}
	
	public static void writeDailyCycleStaticSv(ModelDataSet mds, int index){
		
		//ArrayList<String> dvList = mds.dvList;
		ArrayList<String> svList = mds.svList;
		//ArrayList<String> asList = mds.asList;
		//Map<String, Dvar> dvMap = mds.dvMap;
		Map<String, Svar> svMap = mds.svMap;
		//Map<String, Alias> asMap = mds.asMap;
		//int dvSize = dvList.size();
		int svSize = svList.size();
		//int asSize = asList.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		double[][] write_data = new double[1][size];
		
		/*
		for (int i=0; i<dvSize; i++){
			String name=dvList.get(i);
						
			double data = dvMap.get(name).data.getData().doubleValue();
			write_data[0][i]=data;						
		}
		
		int offset=dvSize;
		for (int i=0; i<asSize; i++){
			String name=asList.get(i);
						
			double data = asMap.get(name).data.getData().doubleValue();
			write_data[0][i+offset]=data;						
		}
		offset=dvSize+asSize;
		*/
		
		for (int i=0; i<svSize; i++){
			String name=svList.get(i);
						
			double data = svMap.get(name).getData().getData().doubleValue();
			write_data[0][i]=data;						
		}
		
		HDF5Util.writeCycleStaticSv(index, size, gidSCDaily, write_data);
	}

	
	public static void writeMonthlyCycleDynamicSv(ModelDataSet mds, int index){
		String dName=+ControlData.currYear+"-"+ControlData.currMonth+" Cycle "+index+" Table";
		HDF5Util.writeCycleDynamicSv(mds, gidDCMonthly, dName);
	}
	
	public static void writeDailyCycleDynamicSv(ModelDataSet mds, int index){
		String dName=+ControlData.currYear+"-"+ControlData.currMonth+"-"+ControlData.currDay+" Cycle "+index+" Table";
		HDF5Util.writeCycleDynamicSv(mds, gidDCDaily, dName);
	}
	
	public static void skipOneCycle(ModelDataSet mds, int index){
		if (fid>0){
			if (TimeOperation.isMonthlyInterval(ControlData.timeStep)) {
				skipMonthlyCycle(mds, index);
			}else if (ControlData.timeStep.equals("1DAY")){
				skipDailyCycle(mds, index);
			}
		}
	}
	
	public static void skipMonthlyCycle(ModelDataSet mds, int index){
		
		if (gidSCMonthly>=0) skipMonthlyCycleStaticSv(mds, index);
		
	}
	
	public static void skipDailyCycle(ModelDataSet mds, int index){
		
		if (gidSCDaily>=0) skipDailyCycleStaticSv(mds, index);
		
	}
	
	public static void skipMonthlyCycleStaticSv(ModelDataSet mds, int index){
		//ArrayList<String> dvList = mds.dvList;
		ArrayList<String> svList = mds.svList;
		//ArrayList<String> asList = mds.asList;
		//int dvSize = dvList.size();
		int svSize = svList.size();
		//int asSize = asList.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		double[][] write_data = new double[1][size];
		
		for (int i=0; i<size; i++){
			write_data[0][i]=0.0;						
		}
		
		HDF5Util.writeCycleStaticSv(index, size, gidSCMonthly, write_data);
	}
	
	public static void skipDailyCycleStaticSv(ModelDataSet mds, int index){
		//ArrayList<String> dvList = mds.dvList;
		ArrayList<String> svList = mds.svList;
		//ArrayList<String> asList = mds.asList;
		//int dvSize = dvList.size();
		int svSize = svList.size();
		//int asSize = asList.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		double[][] write_data = new double[1][size];
		
		for (int i=0; i<size; i++){
			write_data[0][i]=0.0;						
		}
		
		HDF5Util.writeCycleStaticSv(index, size, gidSCDaily, write_data);
	}
	
	public static void closeDataStructure(){
			try {
				if (gidCSDaily>=0)
					H5.H5Gclose(gidCSDaily);
				if (gidCSMonthly>=0) 
					H5.H5Gclose(gidCSMonthly);
				if (gidCS>=0)
					H5.H5Gclose(gidCS);
				if (gidSCDaily>=0)
					H5.H5Gclose(gidSCDaily);
				if (gidSCMonthly>=0) 
					H5.H5Gclose(gidSCMonthly);
				if (gidSC>=0)
					H5.H5Gclose(gidSC);
				if (gidDCDaily>=0)
					H5.H5Gclose(gidDCDaily);
				if (gidDCMonthly>=0) 
					H5.H5Gclose(gidDCMonthly);
				if (gidDC>=0)
					H5.H5Gclose(gidDC);
				if (gidDaily>=0)
					H5.H5Gclose(gidDaily);
				if (gidMonthly>=0) 
					H5.H5Gclose(gidMonthly);
				if (gidData>=0) 
					H5.H5Gclose(gidData);
				if (gidInfo>=0) 
					H5.H5Gclose(gidInfo);
				if (gidIOData>=0) 
					H5.H5Gclose(gidIOData);
				if (gidPartF>=0) 
					H5.H5Gclose(gidPartF);
				if (gidPartA>=0) 
					H5.H5Gclose(gidPartA);
				if (fid>=0)
					H5.H5Fclose(fid);
			} catch (HDF5LibraryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
}
