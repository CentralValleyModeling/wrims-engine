package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.javatuples.Triplet;

public class ModelTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String absPath;
	public String parentAbsPath;
	public String pathRelativeToRunDir; //processed
	
	public ArrayList<String>  itemList;
	public ArrayList<Integer>  itemTypeList;
	public ArrayList<String>  svIncFileList;   // processed 
	public ArrayList<String>  svIncFileList_post;   // post processed 

	public ArrayList<String>  asIncFileList;   // processed 
	public ArrayList<String>  asIncFileList_post;   // post processed 

	public ArrayList<String>  exIncFileList;   // processed 
	public ArrayList<String>  exIncFileList_post;   // post processed 
	
	public ArrayList<String>  dvIncFileList;   // processed 
	public ArrayList<String>  dvIncFileList_post;   // post processed 

	public ArrayList<String>  glIncFileList;   // processed 
	public ArrayList<String>  glIncFileList_post;   // post processed 	
	
	//<svName, relativePath, operationName>
	//public ArrayList<Triplet<String,String,String>>  t_svList;   // processed 
	//public ArrayList<Triplet<String,String,String>>  t_svList_post;   // post processed 
	
	//TODO: use more compact storage. i.e., Map<String fileID, Pair ( relativePath, absPath)>
	// then converts to Map<String relativePath, String absPath>
	
	public ArrayList<String> incFileIDList;
	public ArrayList<String> incFileRelativePathList; // processed
	public ArrayList<String> incFileRelativePathList_post; // post processed
	public ArrayList<String> incFileAbsPathList;      // processed 
	public ArrayList<String> incFileAbsPathList_post;      // post processed 
	public Map<String,IncFileTemp> incFileMap;

	public ArrayList<String> ifIncItemGroupIDList;
	public Map<String,IfIncItemGroup> ifIncItemGroupMap;
	
	public ArrayList<String> incModelList;
	
	public ArrayList<String>  wvList_post; // added after processed
	public ArrayList<WeightTable> wTableObjList;  // raw data
	public ArrayList<String>  asList_backup;  // backup raw lowercased
	public ArrayList<String>  asList;
	//public ArrayList<String>  asList_reduced; // added after processed
	public LinkedHashMap<String,AliasTemp> asMap;		
	public ArrayList<String>  svList;
	public Map<String,SvarTemp> svMap;
	public ArrayList<String>  dvList;
	public Map<String,DvarTemp> dvMap;	
	public ArrayList<String>  dvList_fromAlias;  // processed
	public ArrayList<String>  dvList_deviationSlackSurplus;  // give warning or error if greater than a tolerance
	public Map<String,Double> deviationSlackSurplus_toleranceMap; // < deviationSS, tolerance>
	public ArrayList<String>  tsList;
	public Map<String,TimeseriesTemp> tsMap;
	public ArrayList<String>  exList;
	public Map<String,ExternalTemp> exMap;	
	public ArrayList<String>  glList_backup;
	public ArrayList<String>  glList;
	public Map<String,GoalTemp> glMap;
	public ArrayList<String>  gl2List;
	public ArrayList<String>  glList_fromAlias;  // processed
 	public ArrayList<String>  ssList_hasCase;    // processed
	public Map<String,DvarTemp> ssMap_hasCase;   // processed
	public Map<String,WeightTemp> ssWeightMap_hasCase;  // processed
 	public ArrayList<String>  ssList_noCase;            // processed
	public Map<String,DvarTemp> ssMap_noCase;           // processed
	public Map<String,WeightTemp> ssWeightMap_noCase;   // processed
	
	public Map<String,WeightTemp> groupWeightMap;   // processed
	
	public Map<String, HashSet<String>> neededCycleVarMap;
	
//	public Set<String> varUsedByLaterCycle; 
//	public Set<String> dvarUsedByLaterCycle;
//	public Set<String> svarUsedByLaterCycle;
//	public Set<String> aliasUsedByLaterCycle; 
	
	public ModelTemp(){
		
		//t_svList = new ArrayList<Triplet<String,String,String>>();
		//t_svList_post = new ArrayList<Triplet<String,String,String>>();
		itemList = new ArrayList<String>();
		itemTypeList = new ArrayList<Integer>();
		//svIncFileList = new ArrayList<String>();
		svIncFileList_post = new ArrayList<String>();
		asIncFileList_post = new ArrayList<String>();
		exIncFileList_post = new ArrayList<String>();
		dvIncFileList_post = new ArrayList<String>();
		glIncFileList_post = new ArrayList<String>();
		incFileIDList = new ArrayList<String>();
		incFileRelativePathList = new ArrayList<String>();
		incFileRelativePathList_post = new ArrayList<String>();
		incFileAbsPathList = new ArrayList<String>();
		incFileAbsPathList_post = new ArrayList<String>();
		
		incFileMap= new LinkedHashMap<String, IncFileTemp>();
		
		
		ifIncItemGroupIDList = new ArrayList<String>();
		ifIncItemGroupMap = new HashMap<String, IfIncItemGroup>();
		
		incModelList = new ArrayList<String>();
		
		wvList_post = new ArrayList<String>();   // type obj
		wTableObjList = new ArrayList<WeightTable>();
		svList = new ArrayList<String>();  //raw data
		svMap = new HashMap<String, SvarTemp>();  // includes processed data	
		asList = new ArrayList<String>();
		//asList_reduced = new ArrayList<String>();
		asMap = new LinkedHashMap<String, AliasTemp>();
		dvList = new ArrayList<String>();
		dvMap = new HashMap<String, DvarTemp>();
		dvList_fromAlias = new ArrayList<String>();
		dvList_deviationSlackSurplus = new ArrayList<String>();
		deviationSlackSurplus_toleranceMap = new HashMap<String, Double>();
		tsList = new ArrayList<String>();
		tsMap = new HashMap<String, TimeseriesTemp>();	
		exList = new ArrayList<String>();
		exMap = new HashMap<String, ExternalTemp>();		
		glList = new ArrayList<String>();
		glMap = new HashMap<String, GoalTemp>();	
		gl2List = new ArrayList<String>();
		glList_fromAlias = new ArrayList<String>();
		ssList_hasCase = new ArrayList<String>();
		ssMap_hasCase = new LinkedHashMap<String, DvarTemp>();	
		ssWeightMap_hasCase = new LinkedHashMap<String, WeightTemp>();	
		ssList_noCase = new ArrayList<String>();
		ssMap_noCase = new LinkedHashMap<String, DvarTemp>();	
		ssWeightMap_noCase = new LinkedHashMap<String, WeightTemp>();		
		
		groupWeightMap = new HashMap<String, WeightTemp>();
		
		neededCycleVarMap = new HashMap<String, HashSet<String>>();
		
//		varUsedByLaterCycle = new HashSet<String>();	
//		dvarUsedByLaterCycle = new HashSet<String>();
//		svarUsedByLaterCycle = new HashSet<String>();
//		aliasUsedByLaterCycle = new HashSet<String>();
	}
	
}
	
