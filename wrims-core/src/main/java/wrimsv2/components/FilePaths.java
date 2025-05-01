package wrimsv2.components;

import java.io.File;

public class FilePaths {
	public static String groundwaterDir="";  //for groundwater use.
	public static String fullMainPath="";
	public static String mainFile="";
	public static String mainDirectory="";
	public static String fullSvarFilePath="";
	public static String fullSvarFile2Path="";
	public static String svarFile="";
	public static String svarFileDirectory="";
	public static String fullDvarDssPath="";
	public static String dvarDssFile="";
	public static String dvarDssDirectory="";
	public static String fullDvarHDF5Path="";
	public static String fullCsvPath="";
	public static String fullInitFilePath="";
	public static String initFile="";
	public static String initFileDirectory="";
	public static String fullIlpPath="";
	public static String ilpFile="";
	public static String ilpFileDirectory="";
	public static String csvFolderName="";
	public static String lookupSubDirectory="";
	public static String sqlScenarioName="";
	public static String genTableDir="";


	public static void setMainFilePaths(String fullPath){
		fullMainPath=fullPath;
		int index=fullPath.lastIndexOf(File.separator);
		mainDirectory=fullPath.substring(0,index+1);
		mainFile=fullPath.substring(index+1);
	}
	
	public static void setSvarFilePaths(String fullPath){
		fullSvarFilePath=fullPath;
		int index=fullPath.lastIndexOf(File.separator);
		svarFileDirectory=fullPath.substring(0,index+1);
		svarFile=fullPath.substring(index+1);
	}
	
	public static void setSvarFile2Paths(String fullPath){
		fullSvarFile2Path=fullPath;
	}
	
	public static void setDvarFilePaths(String fullPath){
		if (fullPath.toLowerCase().endsWith(".h5")){
			ControlData.outputType=1;
			fullDvarHDF5Path=fullPath;
			int index=fullPath.lastIndexOf(".");
			fullDvarDssPath=fullPath.substring(0, index+1)+"dss";
		}else if (fullPath.toLowerCase().endsWith(".mysqlc")){
			ControlData.outputType=2;
			int index1=fullPath.lastIndexOf(File.separator);
			int index2=fullPath.lastIndexOf(".");
			sqlScenarioName=fullPath.substring(index1+1,index2);
			fullDvarDssPath=fullPath.substring(0, index2+1)+"dss";
		}else if (fullPath.toLowerCase().endsWith(".mysqlr")){
			ControlData.outputType=3;
			int index1=fullPath.lastIndexOf(File.separator);
			int index2=fullPath.lastIndexOf(".");
			sqlScenarioName=fullPath.substring(index1+1,index2);
			fullDvarDssPath=fullPath.substring(0, index2+1)+"dss";
		}else if (fullPath.toLowerCase().endsWith(".sqlsvr")){
			ControlData.outputType=4;
			int index1=fullPath.lastIndexOf(File.separator);
			int index2=fullPath.lastIndexOf(".");
			sqlScenarioName=fullPath.substring(index1+1,index2);
			fullDvarDssPath=fullPath.substring(0, index2+1)+"dss";
		}else if (fullPath.toLowerCase().endsWith(".csv")){
			ControlData.outputType=5;
			int index1=fullPath.lastIndexOf(File.separator);
			int index2=fullPath.lastIndexOf(".");
			sqlScenarioName=fullPath.substring(index1+1,index2);
			fullDvarDssPath=fullPath.substring(0, index2+1)+"dss";
			fullCsvPath=fullPath;
		}else{
			ControlData.outputType=0;
			fullDvarDssPath=fullPath;
		}
		int index=fullPath.lastIndexOf(File.separator);
		dvarDssDirectory=fullPath.substring(0,index+1);
		dvarDssFile=fullPath.substring(index+1);
	}
	
	public static void setInitFilePaths(String fullPath){
		fullInitFilePath=fullPath;
		int index=fullPath.lastIndexOf(File.separator);
		initFileDirectory=fullPath.substring(0,index+1);
		initFile=fullPath.substring(index+1);
	}
	
	public static void clear(){
		
		groundwaterDir=""; 
		fullMainPath="";
		mainFile="";
		mainDirectory="";
		fullSvarFilePath="";
		svarFile="";
		svarFileDirectory="";
		fullDvarDssPath="";
		dvarDssFile="";
		dvarDssDirectory="";
		fullInitFilePath="";
		initFile="";
		initFileDirectory="";
		fullIlpPath="";
		ilpFile="";
		ilpFileDirectory="";
		csvFolderName="";
		genTableDir="";
	}

}
