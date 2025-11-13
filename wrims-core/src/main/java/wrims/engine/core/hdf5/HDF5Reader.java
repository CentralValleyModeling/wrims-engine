package wrims.engine.core.hdf5;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import wrims.engine.core.commondata.wresldata.Alias;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.Timeseries;
import wrims.engine.core.components.ControlData;
import wrims.engine.core.components.FilePaths;
import wrims.engine.core.evaluator.DataTimeSeries;
import wrims.engine.core.evaluator.DssDataSet;
import wrims.engine.core.evaluator.DssOperation;
import wrims.engine.core.evaluator.Evaluation;
import wrims.engine.core.evaluator.TimeOperation;
import wrims.engine.core.parallel.ParallelVars;

public class HDF5Reader {
	
	private static int svFid=-1;
	private static int initFid=-1;
	private static int svGidPartA=-1;
	private static int initGidPartA=-1;
	private static int svGidPartF=-1;
	private static int initGidPartF=-1;
	private static int svGidIOData=-1;
	private static int initGidIOData=-1;
	private static String gIOData="IO Data";
	private static String gInfo="Info";
	private static int svGidInfo=-1;
	private static int initGidInfo=-1;
	private static String gData="Data";
	private static int svGidData=-1;
	private static int initGidData=-1;
	private static String gMonthly="Monthly";
	private static int svGidMonthly=-1;
	private static int initGidMonthly=-1;
	private static String gDaily="Daily";
	private static int svGidDaily=-1;
	private static int initGidDaily=-1;
	private static Date svMonthlyStartDate=new Date(21,9,31,24,0);
	private static Date svDailyStartDate=new Date(21,9,1,24,0);
	private static Date initMonthlyStartDate=new Date(21,9,31,24,0);;
	private static Date initDailyStartDate=new Date(21,9,1,24,0);;
	private static double[][] svMonthlyData=new double[0][0];
	private static double[][] svDailyData=new double[0][0];
	private static double[][] initMonthlyData=new double[0][0];
	private static double[][] initDailyData=new double[0][0];
	private static String[] svMonthlyListName= new String[0];
	private static String[] svMonthlyListKind= new String[0];
	private static String[] svDailyListName= new String[0];
	private static String[] svDailyListKind= new String[0];
	private static String[] initMonthlyListName= new String[0];
	private static String[] initMonthlyListKind= new String[0];
	private static String[] initDailyListName= new String[0];
	private static String[] initDailyListKind= new String[0];
	private static String[] svLookupName=new String[0];
	private static String[] svLookupKind=new String[0];
	private static String[] svLookupUnit=new String[0];
	private static String[] svLookupTimestep=new String[0];
	private static String[] initLookupName=new String[0];
	private static String[] initLookupKind=new String[0];
	private static String[] initLookupUnit=new String[0];
	private static String[] initLookupTimestep=new String[0];
	
	public static void readTimeseries(){
		if (openFileAndGroup(1)>=0){
			readLookupTable(1);
			readTimestepList(1);
			readTimestepData(1);
			assignTimeseries();
			closeFileAndGroup(1);
			System.out.println("Timeseries Reading Done.");
		}
	}
	
	public static void readInitialData(){
		if (openFileAndGroup(2)>=0){
			readLookupTable(2);
			readTimestepList(2);
			readTimestepData(2);
			closeFileAndGroup(2);
		}
	}
	
	public static int openFileAndGroup(int flag){
		String h5FileName="";
		String gPartF="";
		Date monthlyStartDate=new Date(21,9,31,24,0);
		Date dailyStartDate=new Date(21,9,1,24,0);
		int gidPartA=-1;
		int gidPartF=-1;
		int gidIOData=-1;
		int gidInfo=-1;
		int gidData=-1;
		int gidMonthly=-1;
		int gidDaily=-1;
		
		if (flag==1){
			h5FileName=FilePaths.fullSvarFilePath;
			gPartF=ControlData.svDvPartF;
		}else{
			h5FileName=FilePaths.fullInitFilePath;
			gPartF=ControlData.initPartF;
		}
		
		int fid=-1;
		try {
			fid = H5.H5Fopen(h5FileName, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
		} catch (Exception e1) {
			if (flag==1){
				System.err.println("Failed to open Svar Timesereis HDF5 file.");
				return -1;
			}else{
				System.err.println("Failed to open Initial HDF5 file.");
				return -1;
			}
		} 									
		
		if (fid<0){
			if (flag==1){
				System.err.println("Failed to open Svar Timesereis HDF5 file.");
				return -1;
			}else{
				System.err.println("Failed to open Initial HDF5 file.");
				return -1;
			}
		}else{
			try {
				gidPartA=HDF5Util.locateGroup(fid, ControlData.partA);
				if (gidPartA>=0){
					gidPartF = HDF5Util.locateGroup(gidPartA, gPartF);
					if (gidPartF>=0){	
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
										if (aidMonthly>=0){
											String monthlyDateStr = HDF5Util.readStringAttr(aidMonthly, tidAttr, 30);
											int year = Integer.parseInt(monthlyDateStr.substring(5, 9));
											int month = TimeOperation.monthValue(monthlyDateStr.substring(2, 5));
											int day = Integer.parseInt(monthlyDateStr.substring(0, 2));
											monthlyStartDate=new Date(year-1900, month-1, day, 24, 0);
											H5.H5Aclose(aidMonthly);
										}
									}catch (Exception e){
									}
								}
								
								gidDaily = HDF5Util.locateGroup(gidData, gDaily);
								if (gidDaily>=0 && tidAttr>=0 && sidAttr>=0){
									int aidDaily=-1;
									try{
										aidDaily=H5.H5Aopen(gidDaily, "Starting Time", HDF5Constants.H5P_DEFAULT);
										
										if (aidDaily>=0){
											String dailyDateStr = HDF5Util.readStringAttr(aidDaily, tidAttr, 30);
											int year = Integer.parseInt(dailyDateStr.substring(5, 9));
											int month = TimeOperation.monthValue(dailyDateStr.substring(2, 5));
											int day = Integer.parseInt(dailyDateStr.substring(0, 2));
											dailyStartDate=new Date(year-1900, month-1, day, 24, 0);
											H5.H5Aclose(aidDaily);
										}
									}catch (Exception e){	
									}
								}
								
								H5.H5Tclose(tidAttr);
								H5.H5Sclose(sidAttr);
							}
						}
					}
				}
			} catch (Exception e) {
				if (flag==1){
					System.err.println("Failed to open Svar Timesereis HDF5 file.");
					return -1;
				}else{
					System.err.println("Failed to open Initial HDF5 file.");
					return -1;
				}
			}
		}
		
		if (flag==1){
			svFid=fid;
			svGidPartA=gidPartA;
			svGidPartF=gidPartF;
			svGidIOData=gidIOData;
			svGidInfo=gidInfo;
			svGidData=gidData;
			svGidMonthly=gidMonthly;
			svGidDaily=-gidDaily;
			svMonthlyStartDate=monthlyStartDate;
			svDailyStartDate=dailyStartDate;
		}else{
			initFid=fid;
			initGidPartA=gidPartA;
			initGidPartF=gidPartF;
			initGidIOData=gidIOData;
			initGidInfo=gidInfo;
			initGidData=gidData;
			initGidMonthly=gidMonthly;
			initGidDaily=-gidDaily;
			initMonthlyStartDate=monthlyStartDate;
			initDailyStartDate=dailyStartDate;
		}
		
		return 1;
	}
	
	public static void closeFileAndGroup(int flag){
		try {		
			if (flag==1){
				if (svGidDaily>=0)
					H5.H5Gclose(svGidDaily);
				if (svGidMonthly>=0) 
					H5.H5Gclose(svGidMonthly);
				if (svGidData>=0) 
					H5.H5Gclose(svGidData);
				if (svGidInfo>=0) 
					H5.H5Gclose(svGidInfo);
				if (svGidIOData>=0) 
					H5.H5Gclose(svGidIOData);
				if (svGidPartF>=0) 
					H5.H5Gclose(svGidPartF);
				if (svGidPartA>=0) 
					H5.H5Gclose(svGidPartA);
				if (svFid>=0)
					H5.H5Fclose(svFid);
			}else{
				if (initGidDaily>=0)
					H5.H5Gclose(initGidDaily);
				if (initGidMonthly>=0) 
					H5.H5Gclose(initGidMonthly);
				if (initGidData>=0) 
					H5.H5Gclose(initGidData);
				if (initGidInfo>=0) 
					H5.H5Gclose(initGidInfo);
				if (initGidIOData>=0) 
					H5.H5Gclose(initGidIOData);
				if (initGidPartF>=0) 
					H5.H5Gclose(initGidPartF);
				if (initGidPartA>=0) 
					H5.H5Gclose(initGidPartA);
				if (initFid>=0)
					H5.H5Fclose(initFid);
			}
		} catch (HDF5LibraryException e) {	
			e.printStackTrace();
		}
	}
	
	public static void readLookupTable(int flag){
		
		int gidInfo=-1;
		if (flag==1){
			gidInfo=svGidInfo;
		}else{
			gidInfo=initGidInfo;
		}
		
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
			
			int didLookup=-1;
			try{
				didLookup = H5.H5Dopen(gidInfo, "IO Variable Lookup");
			}catch(Exception e){
				didLookup =-1;
			}
				
			if (didLookup >= 0){
				
				int sidLookup=-1;
				sidLookup = H5.H5Dget_space(didLookup);
				if (sidLookup>=0){
					long[] dims={1};
					H5.H5Sget_simple_extent_dims(sidLookup, dims, null);
					int size=(int)dims[0];
					
					String[] nameList=new String[size];
					String[] kindList=new String[size];
					String[] unitList=new String[size];
					String[] timestepList=new String[size];
					int[] indexList=new int[size]; 
					    
					offset=0;
					
					int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
					H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
					nameList=HDF5Util.readStringData(didLookup, tidCompoundTmp, size, 256);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
					H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
					kindList=HDF5Util.readStringData(didLookup, tidCompoundTmp, size, 80);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 20);
					H5.H5Tinsert(tidCompoundTmp, "Unit", offset, tidStringUnit);
					unitList=HDF5Util.readStringData(didLookup, tidCompoundTmp, size, 20);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 4);
					H5.H5Tinsert(tidCompoundTmp, "Timestep", offset, tidStringTimestep);
					timestepList=HDF5Util.readStringData(didLookup, tidCompoundTmp, size, 4);
					
					tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 4);
					H5.H5Tinsert(tidCompoundTmp, "Index", offset, HDF5Constants.H5T_NATIVE_INT32);
					H5.H5Dread(didLookup, tidCompoundTmp,
						HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
		        		indexList);
				
					H5.H5Tclose(tidCompoundTmp);
		            H5.H5Sclose(sidLookup);
		            
		            if (flag==1){
						svLookupName=new String[size];
						svLookupName=nameList;
						svLookupKind=new String[size];
						svLookupKind=kindList;
						svLookupUnit=new String[size];
						svLookupUnit=unitList;
						svLookupTimestep=new String[size];
						svLookupTimestep=timestepList;
					}else{
						initLookupName=new String[size];
						initLookupName=nameList;
						initLookupKind=new String[size];
						initLookupKind=kindList;
						initLookupUnit=new String[size];
						initLookupUnit=unitList;
						initLookupTimestep=new String[size];
						initLookupTimestep=timestepList;
					}
				}
			}
				
            H5.H5Tclose(tidStringName);
            H5.H5Tclose(tidStringKind);
            H5.H5Tclose(tidStringUnit);
            H5.H5Tclose(tidStringTimestep);
            H5.H5Tclose(tidCompound);
            H5.H5Dclose(didLookup);

		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void readTimestepList(int flag){
		readMonthlyTimestepList(flag);
		readDailyTimestepList(flag);
	}
	
	public static void readMonthlyTimestepList(int flag){
		int gidMonthly=-1;
		
		if (flag==1){
			gidMonthly=svGidMonthly;
		}else{
			gidMonthly=initGidMonthly;
		}
		
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
							
			int didList=-1;
				try{
					didList = H5.H5Dopen(gidMonthly, "Timestep List");
				}catch(Exception e){
					didList=-1;
				}
				
				if (didList >= 0){
					int sidList=-1;
					sidList = H5.H5Dget_space(didList);
					if (sidList>=0){
						long[] dims={1};
						H5.H5Sget_simple_extent_dims(sidList, dims, null);
						int size=(int)dims[0];
						
						String[] nameList=new String[size];
						String[] kindList=new String[size];
											
						offset=0;
					
						int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
						H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
						nameList=HDF5Util.readStringData(didList, tidCompoundTmp, size, 256);
					
						tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
						H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
						kindList=HDF5Util.readStringData(didList, tidCompoundTmp, size, 80);
					
						H5.H5Tclose(tidCompoundTmp);
						H5.H5Sclose(sidList);
						
						if (flag==1){
							svMonthlyListName=new String[size];
							svMonthlyListName=nameList;
							svMonthlyListKind=new String[size];
							svMonthlyListKind=kindList;
						}else{
							initMonthlyListName=new String[size];
							initMonthlyListName=nameList;
							initMonthlyListKind=new String[size];
							initMonthlyListKind=kindList;
						}
					}
				}
				
	            H5.H5Tclose(tidStringName);
	            H5.H5Tclose(tidStringKind);
	            H5.H5Tclose(tidCompound);
	            H5.H5Dclose(didList);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void readDailyTimestepList(int flag){
		int gidDaily=-1;
		
		if (flag==1){
			gidDaily=svGidDaily;
		}else{
			gidDaily=initGidDaily;
		}
		
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
							
			int didList=-1;
				try{
					didList = H5.H5Dopen(gidDaily, "Timestep List");
				}catch(Exception e){
					didList=-1;
				}
				
				if (didList >= 0){
					int sidList=-1;
					sidList = H5.H5Dget_space(didList);
					if (sidList>=0){
						long[] dims={1};
						H5.H5Sget_simple_extent_dims(sidList, dims, null);
						int size=(int)dims[0];
						
						String[] nameList=new String[size];
						String[] kindList=new String[size];
											
						offset=0;
					
						int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);
						H5.H5Tinsert(tidCompoundTmp, "Name", offset, tidStringName);
						nameList=HDF5Util.readStringData(didList, tidCompoundTmp, size, 256);
					
						tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 80);
						H5.H5Tinsert(tidCompoundTmp, "Kind", offset, tidStringKind);
						kindList=HDF5Util.readStringData(didList, tidCompoundTmp, size, 80);
					
						H5.H5Tclose(tidCompoundTmp);
						H5.H5Sclose(sidList);
						
						if (flag==1){
							svDailyListName=new String[size];
							svDailyListName=nameList;
							svDailyListKind=new String[size];
							svDailyListKind=kindList;
						}else{
							initDailyListName=new String[size];
							initDailyListName=nameList;
							initDailyListKind=new String[size];
							initDailyListKind=kindList;
						}
					}
				}
				
	            H5.H5Tclose(tidStringName);
	            H5.H5Tclose(tidStringKind);
	            H5.H5Tclose(tidCompound);
	            H5.H5Dclose(didList);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void readTimestepData(int flag){
		readMonthlyTimestepData(flag);
		readDailyTimestepData(flag);
	}
	
	public static void readMonthlyTimestepData(int flag){
		int gidMonthly=-1;
		
		if (flag==1){
			gidMonthly=svGidMonthly;
		}else{
			gidMonthly=initGidMonthly;
		}
		
		if (gidMonthly>=0){
			try {				
				String dName="Timestep Table";
					
				int didTDA=-1;
				try{
					didTDA = H5.H5Dopen(gidMonthly, dName);
				}catch (Exception e){
					didTDA=-1;
				}
				
				int sidTDA=-1;
				try{
					sidTDA = H5.H5Dget_space(didTDA);
					if (sidTDA>=0){
						long[] dims={1,1};
						H5.H5Sget_simple_extent_dims(sidTDA, dims, null);
						
						double[][] read_data=new double[(int) dims[0]][(int) dims[1]];
						if (didTDA >= 0){			
							H5.H5Dread(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
							HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, read_data);
							
							H5.H5Dclose(didTDA);
							
							if (flag==1){
								svMonthlyData=new double[(int) dims[0]][(int) dims[1]];
								svMonthlyData=read_data;
							}else{
								initMonthlyData=new double[(int) dims[0]][(int) dims[1]];
								initMonthlyData=read_data;
							}
						}
					}
				}catch(Exception e){
					sidTDA=-1;
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void readDailyTimestepData(int flag){
		int gidDaily=-1;
		
		if (flag==1){
			gidDaily=svGidDaily;
		}else{
			gidDaily=initGidDaily;
		}
		
		if (gidDaily>=0){
			try {				
				String dName="Timestep Table";
					
				int didTDA=-1;
				try{
					didTDA = H5.H5Dopen(gidDaily, dName);
				}catch (Exception e){
					didTDA=-1;
				}
			
				int sidTDA=-1;
				try{
					sidTDA = H5.H5Dget_space(didTDA);
					if (sidTDA>=0){
						long[] dims={1,1};
						H5.H5Sget_simple_extent_dims(sidTDA, dims, null);
						
						double[][] read_data=new double[(int) dims[0]][(int) dims[1]];
						if (didTDA >= 0){			
							H5.H5Dread(didTDA, HDF5Constants.H5T_NATIVE_DOUBLE, 
							HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, read_data);
							
							H5.H5Dclose(didTDA);
							
							if (flag==1){
								svDailyData=new double[(int) dims[0]][(int) dims[1]];
								svDailyData=read_data;
							}else{
								initDailyData=new double[(int) dims[0]][(int) dims[1]];
								initDailyData=read_data;
							}
						}
					}
				}catch(Exception e){
					sidTDA=-1;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	public static void assignTimeseries(){
		Map<String, Timeseries> tsMap=ControlData.currStudyDataSet.getTimeseriesMap();
		Map<String, ArrayList<String>> tsTimeStepMap=ControlData.currStudyDataSet.getTimeseriesTimeStepMap(); 
		ControlData.currEvalTypeIndex=6;
		Set tsKeySet=tsMap.keySet();
		Iterator iterator=tsKeySet.iterator();
		while(iterator.hasNext()){
			String tsName=(String)iterator.next();
			if (!DataTimeSeries.lookSvDss.contains(tsName)){ 
				ArrayList<String> timeStepList=tsTimeStepMap.get(tsName);
				for (String timeStep:timeStepList){
					getSVTimeseries(tsName, timeStep);
					String entryNameTS=DssOperation.entryNameTS(tsName, timeStep);
					DataTimeSeries.lookSvDss.add(entryNameTS);
				}
			}
		}
		
		svLookupName=new String[0];
		svLookupKind=new String[0];
		svLookupUnit=new String[0];
		svLookupTimestep=new String[0];
		svMonthlyData=new double[0][0];
		svDailyData=new double[0][0];
		svMonthlyListName= new String[0];
		svMonthlyListKind= new String[0];
		svDailyListName= new String[0];
		svDailyListKind= new String[0];
	}
	
	public static boolean getSVTimeseries(String name, String timeStep){		
		ControlData.timeStep=timeStep;
		ControlData.partE=timeStep;
		String timeStepLow=timeStep.toLowerCase();
		Timeseries ts=ControlData.allTsMap.get(name);
		String dssBPart=ts.dssBPart;
		String partC=ts.kind;
		boolean found=false;
		
		int i=0;
		while (i<svLookupName.length && !found){
			if (svLookupName[i].equals(dssBPart) && svLookupTimestep[i].equals(timeStepLow) && svLookupKind[i].equals(partC) && svLookupUnit[i].equals(ts.units)){
				found=true;
			}
			i++;
		}
		
		if (!found) return false;
		
		String[] listName=new String[0];
		String[] listKind=new String[0];
		double[][] data=new double[0][0];
		Date startDate=new Date(21, 9, 31, 24, 0);
		
		if (TimeOperation.isMonthlyInterval(timeStep)){
			listName = svMonthlyListName;
			listKind = svMonthlyListKind;
			data = svMonthlyData;
			startDate=svMonthlyStartDate;
		}else if (timeStep.equals("1DAY")){
			listName = svDailyListName;
			listKind = svDailyListKind;
			data = svDailyData;
			startDate=svDailyStartDate;
		}
		
		i=0; 
		int index=-1;
		while(i<listName.length && index==-1){
			if (listName[i].equals(dssBPart) && listKind[i].equals(partC)){
				index=i;
			}
			i++;
		}
		
		if (index==-1) return false;
		
		DssDataSet dds= new DssDataSet();
		ArrayList<Double> dataArray= new ArrayList<Double>();
		
		if (ts.units.equals("taf") && ts.convertToUnits.equals("cfs")){
			for (int j=0; j<data.length; j++){
				double dataEntry = data[j][index];
				if (dataEntry==-901.0){
					dataArray.add(-901.0);
				}else if (dataEntry==-902.0){
					dataArray.add(-902.0);
				}else{
					ParallelVars prvs = TimeOperation.findTime(j, startDate.getYear()+1900, startDate.getMonth(), startDate.getDate());
					double dataEntryValue=dataEntry*Evaluation.tafcfs("taf_cfs", prvs);
					dataArray.add(dataEntryValue);
				}
			}
		}else if (ts.units.equals("cfs") && ts.convertToUnits.equals("taf")){
			for (int j=0; j<data.length; j++){
				double dataEntry=data[j][index];
				if (dataEntry==-901.0){
					dataArray.add(-901.0);
				}else if (dataEntry==-902.0){
					dataArray.add(-902.0);
				}else{
					ParallelVars prvs = TimeOperation.findTime(j, startDate.getYear()+1900, startDate.getMonth(), startDate.getDate());
					double dataEntryValue=dataEntry*Evaluation.tafcfs("cfs_taf", prvs);
					dataArray.add(dataEntryValue);
				}
			}
		}else{
			for (int j=0; j<data.length; j++){
				dataArray.add(data[j][index]);
			}
		}
		dds.setUnits(ts.units);
		dds.setKind(partC);
        dds.setData(dataArray);
        dds.setTimeStep(timeStep);
        dds.setStartTime(startDate);
        dds.setFromDssFile(true);
        dds.generateStudyStartIndex();
        String entryNameTS=DssOperation.entryNameTS(name, timeStep);
        DataTimeSeries.svTS.put(entryNameTS, dds);
		return true;
	}
	
	public static boolean getDVAliasInitTimeseries(String name){	
		String units;
		String partC;
		if (ControlData.currDvMap.containsKey(name)){
			Dvar dvar=ControlData.currDvMap.get(name);
			partC=dvar.kind;
			units=dvar.units;
		}else{
			Alias alias=ControlData.currAliasMap.get(name);
			partC=alias.kind;
			units=alias.units;
		}
		
		boolean found=false;
		String timeStep=ControlData.timeStep;
		String timeStepLow=timeStep.toLowerCase();
		
		int i=0;
		while (i<initLookupName.length && !found){
			if (initLookupName[i].equals(name) && initLookupTimestep[i].equals(timeStepLow) && initLookupKind[i].equals(partC) && initLookupUnit[i].equals(units)){
				found=true;
			}
			i++;
		}
		
		if (!found) return false;
		
		String[] listName=new String[0];
		String[] listKind=new String[0];
		double[][] data=new double[0][0];
		Date startDate=new Date(21, 9, 31, 24, 0);
		
		if (TimeOperation.isMonthlyInterval(timeStep)){
			listName = initMonthlyListName;
			listKind = initMonthlyListKind;
			data = initMonthlyData;
			startDate=initMonthlyStartDate;
		}else if (timeStep.equals("1DAY")){
			listName = initDailyListName;
			listKind = initDailyListKind;
			data = initDailyData;
			startDate= initDailyStartDate;
		}
		
		i=0; 
		int index=-1;
		while(i<listName.length && index==-1){
			if (listName[i].equals(name) && listKind[i].equals(partC)){
				index=i;
			}
			i++;
		}
		
		if (index==-1) return false;
		
		DssDataSet dds= new DssDataSet();
		ArrayList<Double> dataArray= new ArrayList<Double>();
		
		for (int j=0; j<data.length; j++){
			dataArray.add(data[j][index]);
		}

		dds.setUnits(units);
		dds.setKind(partC);
        dds.setData(dataArray);
        dds.setTimeStep(timeStep);
        dds.setStartTime(startDate);
        dds.setFromDssFile(true);
        dds.generateStudyStartIndex();
        String entryNameTS=DssOperation.entryNameTS(name, timeStep);
        DataTimeSeries.dvAliasInit.put(entryNameTS, dds);
		return true;
	}
	
	public static boolean getSVInitTimeseries(String name){	
		Timeseries ts=ControlData.currTsMap.get(name);
		String partC=ts.kind;
		String units=ts.units;
		
		boolean found=false;
		String timeStep=ControlData.timeStep;
		String timeStepLow=timeStep.toLowerCase();
		
		int i=0;
		while (i<initLookupName.length && !found){
			if (initLookupName[i].equals(name) && initLookupTimestep[i].equals(timeStepLow) && initLookupKind[i].equals(partC) && initLookupUnit[i].equals(units)){
				found=true;
			}
			i++;
		}
		
		if (!found) return false;
		
		String[] listName=new String[0];
		String[] listKind=new String[0];
		double[][] data=new double[0][0];
		Date startDate=new Date(21, 9, 31, 24, 0);
		
		if (TimeOperation.isMonthlyInterval(timeStep)){
			listName = initMonthlyListName;
			listKind = initMonthlyListKind;
			data = initMonthlyData;
			startDate=initMonthlyStartDate;
		}else if (timeStep.equals("1DAY")){
			listName = initDailyListName;
			listKind = initDailyListKind;
			data = initDailyData;
			startDate= initDailyStartDate;
		}
		
		i=0; 
		int index=-1;
		while(i<listName.length && index==-1){
			if (listName[i].equals(name) && listKind[i].equals(partC)){
				index=i;
			}
			i++;
		}
		
		if (index==-1) return false;
		
		DssDataSet dds= new DssDataSet();
		ArrayList<Double> dataArray= new ArrayList<Double>();
		
		if (ts.units.equals("taf") && ts.convertToUnits.equals("cfs")){
			for (int j=0; j<data.length; j++){
				double dataEntry = data[j][index];
				if (dataEntry==-901.0){
					dataArray.add(-901.0);
				}else if (dataEntry==-902.0){
					dataArray.add(-902.0);
				}else{
					ParallelVars prvs = TimeOperation.findTime(j, startDate.getYear()+1900, startDate.getMonth(), startDate.getDate());
					double dataEntryValue=dataEntry*Evaluation.tafcfs("taf_cfs", prvs);
					dataArray.add(dataEntryValue);
				}
			}
		}else if (ts.units.equals("cfs") && ts.convertToUnits.equals("taf")){
			for (int j=0; j<data.length; j++){
				double dataEntry=data[j][index];
				if (dataEntry==-901.0){
					dataArray.add(-901.0);
				}else if (dataEntry==-902.0){
					dataArray.add(-902.0);
				}else{
					ParallelVars prvs = TimeOperation.findTime(j, startDate.getYear()+1900, startDate.getMonth(), startDate.getDate());
					double dataEntryValue=dataEntry*Evaluation.tafcfs("cfs_taf", prvs);
					dataArray.add(dataEntryValue);
				}
			}
		}else{
			for (int j=0; j<data.length; j++){
				dataArray.add(data[j][index]);
			}
		}
		dds.setUnits(ts.units);
		dds.setKind(partC);
        dds.setData(dataArray);
        dds.setTimeStep(timeStep);
        dds.setStartTime(startDate);
        dds.setFromDssFile(true);
        dds.generateStudyStartIndex();
        String entryNameTS=DssOperation.entryNameTS(name, timeStep);
        DataTimeSeries.svInit.put(entryNameTS, dds);
		return true;
	}
}
