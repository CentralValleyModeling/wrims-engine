package wrimsv2.hdf5;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Dvar;
import wrimsv2.commondata.wresldata.ModelDataSet;
import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.components.ControlData;
import wrimsv2.evaluator.DssDataSetFixLength;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

public class HDF5Util {
	
	public static int locateFile(String fileName){
		
		try {
			return H5.H5Fopen(fileName, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT);
		} catch (Exception e) {
		
			try {
				return H5.H5Fcreate(fileName, HDF5Constants.H5F_ACC_TRUNC,
							HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		}
	}
	
	public static int locateGroup(int id, String group){

		try {
			int gid = H5.H5Gopen(id, group, 0);
			if (gid<0){
				gid=H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
			}
			return gid;
		} catch (HDF5LibraryException e) {
			try {
				int gid = H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				return gid;
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		} catch (NullPointerException e) {
			try {
				int gid = H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				return gid;
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		}	
	}

	public static void writeStringData(int did, int tid, String[] stringArray, int stringLength){
		try {
			int size =stringArray.length;
			byte[][] write_data =  new byte[size][stringLength];
			for (int indx = 0; indx <size ; indx++) {
				for (int jndx = 0; jndx < stringLength; jndx++) {
					if (jndx < stringArray[indx].length())
						write_data[indx][jndx] = (byte) stringArray[indx].charAt(jndx);
					else
						write_data[indx][jndx] = 0;
				}
			}

			if ((did >= 0) && (tid >= 0))
				H5.H5Dwrite(did, tid, 
				        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
				        write_data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String[] readStringData(int did, int tid, int size, int stringLength){
		try {
			byte[] read_data =  new byte[size*stringLength];
			String[] stringData=new String[size];
			
			if ((did >= 0) && (tid >= 0))
				H5.H5Dread(did, tid, 
				        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
				        read_data);
			
			for (int indx = 0; indx <size ; indx++) {
				byte[] buff=new byte[stringLength];
				for (int jndx = 0; jndx < stringLength; jndx++) {
					buff[jndx]=read_data[indx*stringLength+jndx];
				}
				stringData[indx]=(new String(buff)).trim().toLowerCase();
			}
			return stringData;
		}
		catch (Exception e) {
			String[] stringData=new String[0];
			e.printStackTrace();
			return stringData;
		}
	}
	
	public static void writeStringAttr(int aid, int tid, String[] stringArray, int stringLength){
		try {
			int size =stringArray.length;
			byte[][] write_data =  new byte[size][stringLength];
			for (int indx = 0; indx <size ; indx++) {
				for (int jndx = 0; jndx < stringLength; jndx++) {
					if (jndx < stringArray[indx].length())
						write_data[indx][jndx] = (byte) stringArray[indx].charAt(jndx);
					else
						write_data[indx][jndx] = 0;
				}
			}

			if ((aid >= 0) && (tid >= 0))
				H5.H5Awrite(aid, tid, write_data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String readStringAttr(int aid, int tid, int stringLength){
		try {
			byte[] readData=new byte[stringLength];
			if ((aid >= 0) && (tid >= 0))
				H5.H5Aread(aid, tid, readData);
			
			String stringAttr=(new String(readData)).toLowerCase().trim();
			return stringAttr;
		}catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void writeCycleVariableNames(String[] write_data, int gid, String strCycleI){
		
		String dName="Cycle "+strCycleI+" List";
		
		int size = write_data.length;
		long[] dims = {size};
		
		try {
			int tidName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidName, 256);
				
			int sidList = H5.H5Screate_simple(1, dims, null);
			if (sidList >= 0 && size>0){
				int didList=-1;
				try{
					didList = H5.H5Dopen(gid, dName);
				}catch(Exception e){
					didList = H5.H5Dcreate(gid,
							dName, tidName,
							sidList, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didList >= 0){
					HDF5Util.writeStringData(didList, tidName, write_data, 256);
				}
				
	            H5.H5Sclose(sidList);
	            H5.H5Tclose(tidName);
	            H5.H5Dclose(didList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void writeCycleStaticSv(int index, int size, int gid, double[][] write_data){
		String dName="Cycle "+index+" Table";
		long[] dims = {1, size};
		long[] dims1 = {1, size};
		long[] maxDims={HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED};
		long[] chunkDims={1,5};
		
		int sidVs;
		try {				
			int didVs =-1;
			try{
				didVs = H5.H5Dopen(gid, dName);
				Integer currTimestep = ControlData.currTimeStep.get(index-1);
				dims[0]=currTimestep+1;
				H5.H5Dextend(didVs, dims);
				
				int fsidVs = H5.H5Dget_space (didVs);
				long[] offset1={0,0};
			    offset1[0] = currTimestep;
			    offset1[1] = 0;
			    H5.H5Sselect_hyperslab(fsidVs, HDF5Constants.H5S_SELECT_SET, offset1, null,
			                                  dims1, null); 
			    sidVs = H5.H5Screate_simple (2, dims1, null); 
			    
				H5.H5Dwrite(didVs, HDF5Constants.H5T_NATIVE_DOUBLE, 
					sidVs, fsidVs, HDF5Constants.H5P_DEFAULT, write_data);
				
				H5.H5Sclose(fsidVs);
				H5.H5Dclose(didVs);
				H5.H5Sclose(sidVs);
				
			}catch (Exception e){			
				sidVs = H5.H5Screate_simple(2, dims, maxDims);
				if (sidVs >= 0){
					int cparms = H5.H5Pcreate (HDF5Constants.H5P_DATASET_CREATE);
					H5.H5Pset_chunk ( cparms, 2, chunkDims);
					didVs = H5.H5Dcreate(gid, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidVs, cparms);
				
					if (didVs >= 0){		
						H5.H5Dextend(didVs, dims);
					
						int fsidVs = H5.H5Dget_space (didVs);
						long[] offset1={0,0};
						offset1[0] = 0;
						offset1[1] = 0;
						H5.H5Sselect_hyperslab(fsidVs, HDF5Constants.H5S_SELECT_SET, offset1, null,
								dims1, null); 
				    
						H5.H5Dwrite(didVs, HDF5Constants.H5T_NATIVE_DOUBLE, 
								sidVs, fsidVs, HDF5Constants.H5P_DEFAULT, write_data);
						
						H5.H5Sclose(fsidVs);
					}
				}
				H5.H5Dclose(didVs);
				H5.H5Sclose(sidVs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public static void writeCycleStaticSvNames(ModelDataSet mds, int gid, int index){
		
		//ArrayList<String> dvList = mds.dvList;
		ArrayList<String> svList = mds.svList;
		//ArrayList<String> asList = mds.asList;
		//int dvSize = dvList.size();
		int svSize = svList.size();
		//int asSize = asList.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		String[] write_data = new String[size];
		
		/*
		for (int i=0; i<dvSize; i++){
			write_data[i]=dvList.get(i);						
		}
		
		int offset=dvSize;
		for (int i=0; i<asSize; i++){
			write_data[i+offset]=asList.get(i);					
		}
		offset=dvSize+asSize;
		*/
		
		for (int i=0; i<svSize; i++){
			write_data[i]=svList.get(i);						
		}
		
		int j=index+1;
		String dName="Cycle "+j+" List";
		
		long[] dims = {size};
		try {
			int tidName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidName, 256);
				
			int sidList = H5.H5Screate_simple(1, dims, null);
			if (sidList >= 0 && size>0){
				int didList=-1;
				try{
					didList = H5.H5Dopen(gid, dName);
				}catch(Exception e){
					didList = H5.H5Dcreate(gid,
							dName, tidName,
							sidList, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didList >= 0){
					HDF5Util.writeStringData(didList, tidName, write_data, 256);
				}
				
	            H5.H5Sclose(sidList);
	            H5.H5Tclose(tidName);
	            H5.H5Dclose(didList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void writeCycleDynamicSv(ModelDataSet mds, int gid, String dName){
		
		//Map<String, Dvar> solverDvMap = SolverData.getDvarMap();
		//ArrayList<String> dvTimeArrayList = mds.dvTimeArrayList;
		Map<String, Svar> svFutMap = mds.svFutMap;
		//Map<String, Alias> asFutMap = mds.asFutMap;

		//int dvSize=dvTimeArrayList.size();
		//int asSize=asFutMap.size();
		int svSize=svFutMap.size();
		
		//int size=dvSize+svSize+asSize;
		int size=svSize;
		
		String[] vNames = new String[size];
		double[] vValues = new double[size];
		
		/*
		for (int i=0; i<dvSize; i++){
			String name=dvTimeArrayList.get(i);
			vNames[i]=name;
			Dvar dvar = solverDvMap.get(name);
			vValues[i]=dvar.getData().getData().doubleValue();
		}
		
		int offset=dvSize;
		Set<String> asKeys = asFutMap.keySet();
		Iterator<String> asIter = asKeys.iterator();
		while (asIter.hasNext()){
			String name=asIter.next();
			vNames[offset]=name;
			Alias as = asFutMap.get(name);
			vValues[offset]=as.getData().getData().doubleValue();
			offset=offset+1;
		}
		
		offset=dvSize+asSize;
		*/
		
		int offset=0;
		Set<String> svKeys = svFutMap.keySet();
		Iterator<String> svIter = svKeys.iterator();
		while (svIter.hasNext()){
			String name=svIter.next();
			vNames[offset]=name;
			Svar sv = svFutMap.get(name);
			vValues[offset]=sv.getData().getData().doubleValue();
			offset=offset+1;
		}
		
		if (size>0){
			long[] dims = {size};
			try {
				int tidName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
				H5.H5Tset_size(tidName, 256);
			
				int tidCompound = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256+8);	
				int offset1=0;
				H5.H5Tinsert(tidCompound, "Name", offset1, tidName);
				offset1=256;
				H5.H5Tinsert(tidCompound, "Value", offset1, HDF5Constants.H5T_NATIVE_DOUBLE);
				
				int sidTable = H5.H5Screate_simple(1, dims, null);
				if (sidTable >= 0 && size>0){
					int didTable=-1;
					try{
						didTable = H5.H5Dopen(gid, dName);
					}catch(Exception e){
						didTable = H5.H5Dcreate(gid,
							dName, tidCompound,
							sidTable, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
					}
				
					if (didTable >= 0){
						offset1=0;
						int tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 256);						
						H5.H5Tinsert(tidCompoundTmp, "Name", offset1, tidName);
						HDF5Util.writeStringData(didTable, tidCompoundTmp, vNames, 256);
						H5.H5Tclose(tidCompoundTmp);
					
						tidCompoundTmp = H5.H5Tcreate(HDF5Constants.H5T_COMPOUND, 8);						
						H5.H5Tinsert(tidCompoundTmp, "Value", offset1, HDF5Constants.H5T_NATIVE_DOUBLE);
						H5.H5Dwrite(didTable, tidCompoundTmp, 
					        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, vValues);
						H5.H5Tclose(tidCompoundTmp);
					}
				
					H5.H5Sclose(sidTable);
					H5.H5Tclose(tidName);
					H5.H5Dclose(didTable);
				}
				H5.H5Tclose(tidCompound);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
