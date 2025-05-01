package wrimsv2.wreslparser.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Alias;
import wrimsv2.commondata.wresldata.Param;
import wrimsv2.commondata.wresldata.Svar;

public class Sort {
	
	private Map<String, Set<String>> varDependentMap;
	private Map<String, String> varTypeMap = new HashMap<String, String>();
	private Set<String> previousSet = new HashSet<String>();;
	
	// for iteration only
	private Set<String> outSet = new HashSet<String>();



	//sorting
	
	// Group dv: previous step dv that will be needed. 
	// TODO: this might need more work to get the number of prior steps are needed for dv.
	
	// Group 1: No dependents or ONLY dependent on timeseries, and control data like month or water year   ( dependant.size ==0 )
	
	// NextGroup : recursive

	public Sort(Map<String, SimulationDataSet> file_data_map ){
		
		varDependentMap = new HashMap<String, Set<String>>();
		
		for (Map.Entry<String, SimulationDataSet> e: file_data_map.entrySet()){
			
			Set<String> d = e.getValue().incFileSet;
			varDependentMap.put(e.getKey(), d);
			varTypeMap.put(e.getKey(), "Include File");
		}	
	}

	// sorting based on union of svar and alias
	public Sort(Map<String, Svar> in_svMap, Map<String, Alias> in_asMap, Set<String>dvSet, Set<String>tsSet, Set<String>exSet){
		
		varDependentMap = new HashMap<String, Set<String>>();
		
		for (String key : exSet){
			Set<String> d = new HashSet<String>(); // empty set
			varDependentMap.put(key, d);
			varTypeMap.put(key, "External");		
		}
		
		for (Map.Entry<String, Svar> e: in_svMap.entrySet()){
			
			Set<String> d = e.getValue().dependants;
			d.removeAll(Param.reservedSet);
			d.removeAll(dvSet);d.removeAll(tsSet);//d.removeAll(externalSet);
			varDependentMap.put(e.getKey(), d);
			varTypeMap.put(e.getKey(), "Svar");
		}

		for (Map.Entry<String, Alias> e: in_asMap.entrySet()){
			
			Set<String> d = e.getValue().dependants;
			d.removeAll(Param.reservedSet);
			d.removeAll(dvSet);d.removeAll(tsSet);//d.removeAll(externalSet);
			varDependentMap.put(e.getKey(), d);
			varTypeMap.put(e.getKey(), "Alias");
		}
		
	}	
	
	
	public Set<String> sort(ArrayList<String> toBeSortedList){
		
		toBeSortedList.clear();
		
		// get first group
		toBeSortedList.addAll(getGroup1());
		
		// recursive
		while (getNextGroup(outSet)) {
			
			ArrayList<String> outList = new ArrayList<String>(outSet);
			Collections.sort(outList);
			
			toBeSortedList.addAll(outList);
		}
		
		// var depend on unknowns
		if (varDependentMap.keySet().size()>0) {
			
			//allows recursive svar definition for future array
			//LogUtils.errMsg("Variables with unknown dependants: "+ varDependentMap.keySet()); 
		    LogUtils.warningMsg("Variables with unknown dependants: "+ varDependentMap.keySet());
			if (Param.debug){
				for (String key:varDependentMap.keySet() ){
					LogUtils.warningMsg("Variables type of "+ key +": "+ varTypeMap.get(key)); 
				}
			}
		}
		
		// var depend on unknowns
		return varDependentMap.keySet();
	}
	
  
	// Group 1 : dependant.size ==0 
	private Set<String> getGroup1() {
		
		Set<String> out = new HashSet<String>();
		
		for ( Map.Entry<String, Set<String>>  e : varDependentMap.entrySet() ) {
			
			//System.out.println(" *** before: " + e.getKey());
			
			if (e.getValue().size()==0) {
				
				out.add(e.getKey());
			}	                                  	
		}

		for ( String o : out ) {
				varDependentMap.remove(o);	                                  	
		}		
		previousSet.addAll(out);
		return  out;
	}

	
	private boolean getNextGroup( Set<String> out ) {
		
		boolean OK = false;
		
		out.clear();
		
		for ( Map.Entry<String, Set<String>>  e : varDependentMap.entrySet() ) {
			
			if (previousSet.containsAll(e.getValue()) ) {
				
				out.add(e.getKey());	
			}
		}

		for ( String o : out ) {
			varDependentMap.remove(o);	                                  	
		}
		
		previousSet.addAll(out);	
		
		if (out.size()>0) OK = true;
		
		return OK;
	}	
}
