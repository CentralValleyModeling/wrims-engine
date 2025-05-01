package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import wrimsv2.commondata.wresldata.Svar;
import wrimsv2.components.IntDouble;

import com.google.common.collect.HashBasedTable;

public class StudyTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	//public boolean isSort = true;
	public String runDir = null;
	public String absPath = null;
	public String relativePath = null;
	
	public ArrayList<String> parameterList;
	public ArrayList<String> parameterConstList;  // subset of parameterList
	//public Map<String, String> parameterMap; 
	public LinkedHashMap<String, SvarTemp> parameterMap; 
	public LinkedHashMap<String, Svar> controlDataParameterMap;
	
	public ArrayList<String> seqList;
	public ArrayList<String> seqTimeStepList;
	public Map<String, SequenceTemp> seqMap;
	public ArrayList<String> modelList_effective;  //processed	// defined in sequence 
	public ArrayList<String> incModelList_effective;  //processed // models included in models	
	public ArrayList<String> modelList;		
	public Map<String, ModelTemp> modelMap;
	public Map<String, ArrayList<String>> fileModelNameMap;  
	public HashBasedTable<String,String,ModelTemp> fileModelDataTable; // <file,modelName,model>
	
	// include models
	public Map<String, HashSet<String>>kidMap_incModel; // processed	
	public HashSet<String> noKid_incModel; // processed
	public Map<String, HashSet<String>>allOffspringMap_incModel; // all offspring map. processed
	public ArrayList<HashSet<String>>fileGroupOrder_incModel; // processed after kidMap and AOMap
	
	// files
	public Map<String, HashSet<String>>kidMap; // processed
	public HashSet<String> noKid; // processed
	public Map<String, HashSet<String>>allOffspringMap; // all offspring map. processed
	public ArrayList<HashSet<String>>fileGroupOrder; // processed after kidMap and AOMap
	
	//public String objectiveType="obj";
	
	/// this map contains value of vars needed for WRESL syntax: varName[cycleName] 
	/// < VarName, < CycleName, Value >>		
	public Map<String, Map<String, IntDouble>> varCycleValueMap;
	
	///  < timeseries name, time steps > 
	public Map<String, ArrayList<String>> timeseriesTimeStepMap;

	
	public StudyTemp(){
		
		parameterList = new ArrayList<String>();
		parameterConstList = new ArrayList<String>();
		parameterMap = new LinkedHashMap<String, SvarTemp>();
		controlDataParameterMap = new LinkedHashMap<String, Svar>();
		
		seqTimeStepList = new ArrayList<String>();
		modelList = new ArrayList<String>();
		modelList_effective = new ArrayList<String>();
		incModelList_effective = new ArrayList<String>();
		modelMap = new HashMap<String, ModelTemp>();	
		seqList = new ArrayList<String>();
		seqMap = new HashMap<String, SequenceTemp>();	
		fileModelNameMap = new LinkedHashMap<String, ArrayList<String>>();
		fileModelDataTable = HashBasedTable.create();

		kidMap_incModel = new HashMap<String, HashSet<String>>();
		noKid_incModel = new HashSet<String>();
		allOffspringMap_incModel = new HashMap<String, HashSet<String>>();
		fileGroupOrder_incModel = new ArrayList<HashSet<String>>();
		
		kidMap = new HashMap<String, HashSet<String>>();
		//kidssMap = HashBasedTable.create();
		noKid = new HashSet<String>();
		allOffspringMap = new HashMap<String, HashSet<String>>();
		fileGroupOrder = new ArrayList<HashSet<String>>();
		
		varCycleValueMap = new HashMap<String, Map<String,IntDouble>>();
		
		timeseriesTimeStepMap = new HashMap<String, ArrayList<String>>();


	}
	
}
