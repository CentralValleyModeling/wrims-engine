package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class IfIncItemGroup implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String id;	
	public String fromWresl;
	public int line=1;
	
	public ArrayList<ArrayList<String>> inc_item_list;
	public ArrayList<HashMap<String, IncFileTemp>> inc_files_map_list;
	public ArrayList<HashMap<String, SvarTemp>> inc_svar_map_list;
	public ArrayList<HashMap<String, DvarTemp>> inc_dvar_map_list;
	public ArrayList<LinkedHashMap<String, AliasTemp>> inc_alias_map_list;
	public ArrayList<HashMap<String, TimeseriesTemp>> inc_timeseries_map_list;
	public ArrayList<HashMap<String, GoalTemp>> inc_goalSimple_map_list;
	public ArrayList<HashMap<String, GoalTemp>> inc_goalComplex_map_list;
	public ArrayList<HashMap<String, WeightTable>> inc_weightTable_map_list;
	public ArrayList<String> conditionList;
	public ArrayList<Boolean> conditionValueList;
	public Set<String> dependants;
	
	public IfIncItemGroup(){

		inc_item_list = new ArrayList<ArrayList<String>>();
		inc_files_map_list = new ArrayList<HashMap<String,IncFileTemp>>();
		inc_svar_map_list =  new ArrayList<HashMap<String,SvarTemp>>();
		inc_dvar_map_list = new ArrayList<HashMap<String,DvarTemp>>();
		inc_alias_map_list = new ArrayList<LinkedHashMap<String,AliasTemp>>();
		inc_timeseries_map_list = new ArrayList<HashMap<String,TimeseriesTemp>>();
		inc_goalSimple_map_list = new ArrayList<HashMap<String,GoalTemp>>();
		inc_goalComplex_map_list = new ArrayList<HashMap<String,GoalTemp>>();
		inc_weightTable_map_list = new ArrayList<HashMap<String,WeightTable>>();
		conditionList = new ArrayList<String>();
		dependants = new LinkedHashSet<String>();

	}
	
}
	
