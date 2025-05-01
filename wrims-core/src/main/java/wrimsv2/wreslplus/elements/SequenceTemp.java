package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;

public class SequenceTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String fromWresl;
	public int line=1;
	public String model;
	public String condition;
	public String order;	
	public String timeStep;

	public Set<String> dependants;
	
	public ArrayList<String>  svIncFileList_post;   // copied from included model 
	public ArrayList<String>  asIncFileList_post;   // copied from included model 
	public ArrayList<String>  exIncFileList_post;   // copied from included model 
	public ArrayList<String>  dvIncFileList_post;   // copied from included model 
	public ArrayList<String>  glIncFileList_post;   // copied from included model 
	public ArrayList<String> incFileAbsPathList_post;  // copied from included model  
	public ArrayList<String> incFileRelativePathList_post; // copied from included model  
	// does not reflect user's input order
	public ArrayList<String> exList; 
	public ArrayList<String> dvList;
	public ArrayList<String> dvList_fromAlias; 
	public ArrayList<String> dvList_deviationSlackSurplus; 
	public Map<String,Double> deviationSlackSurplus_toleranceMap; // < deviationSS, tolerance>
	public ArrayList<String> asList;
	public ArrayList<String> glList;
	public ArrayList<String> gl2List;
	public ArrayList<String> glList_fromAlias;
	public ArrayList<String> ssList_hasCase;
	public ArrayList<String> ssList_noCase;
	
	public ArrayList<String>  tsList;
	public Map<String,TimeseriesTemp> tsMap;
	
	public Map<String,SvarTemp> svMap;
	public Map<String,DvarTemp> dvMap;
	public LinkedHashMap<String,AliasTemp> asMap;		
	public LinkedHashMap<String,GoalTemp> glMap;		
	public Map<String,ExternalTemp> exMap;	
	
	
	public Map<String,DvarTemp> ssMap_hasCase;   // processed
	public Map<String,WeightTemp> ssWeightMap_hasCase;  // processed

	public Map<String,DvarTemp> ssMap_noCase;           // processed
	public Map<String,WeightTemp> ssWeightMap_noCase;   // processed
	
	public Map<String,WeightTemp> groupWeightMap;   // copid from model
	
	public ArrayList<String> wvList;
	public ArrayList<WeightTable> wTableObjList;
	
	public Map<String, HashSet<String>> neededCycleVarMap;
	
	public Set<String> varUsedByLaterCycle; 
	public Set<String> dvarUsedByLaterCycle;
	public Set<String> svarUsedByLaterCycle;
	public Set<String> aliasUsedByLaterCycle; 
	
	
	public SequenceTemp(){
		
		fromWresl ="";
		dependants = new LinkedHashSet<String>();
		
		condition = Param.always;
		timeStep = Param.undefined;
		//svIncFileList_post = new ArrayList<String>();
		exList = new ArrayList<String>();
		dvList = new ArrayList<String>();
		dvList_fromAlias = new ArrayList<String>();
		dvList_deviationSlackSurplus = new ArrayList<String>();
		deviationSlackSurplus_toleranceMap = new HashMap<String, Double>();
		asList = new ArrayList<String>();
		glList = new ArrayList<String>();
		gl2List = new ArrayList<String>();
		glList_fromAlias = new ArrayList<String>();
		tsList = new ArrayList<String>();
		ssList_hasCase = new ArrayList<String>();
		ssList_noCase = new ArrayList<String>();
		
		tsMap = new HashMap<String, TimeseriesTemp>();
		svMap = new HashMap<String, SvarTemp>();
		dvMap = new HashMap<String, DvarTemp>();
		asMap = new LinkedHashMap<String, AliasTemp>();
		glMap = new LinkedHashMap<String, GoalTemp>();
		exMap = new HashMap<String, ExternalTemp>();
		
		ssMap_hasCase = new HashMap<String, DvarTemp>();
		ssWeightMap_hasCase = new HashMap<String, WeightTemp>();

		ssMap_noCase = new HashMap<String, DvarTemp>();
		ssWeightMap_noCase = new HashMap<String, WeightTemp>();
		
		groupWeightMap = new HashMap<String, WeightTemp>();
		
		wvList = new ArrayList<String>();
		wTableObjList = new ArrayList<WeightTable>();
		
		neededCycleVarMap = new HashMap<String, HashSet<String>>();
		
		varUsedByLaterCycle = new HashSet<String>();	
		dvarUsedByLaterCycle = new HashSet<String>();
		svarUsedByLaterCycle = new HashSet<String>();
		aliasUsedByLaterCycle = new HashSet<String>();
		
	}
	
}
	
