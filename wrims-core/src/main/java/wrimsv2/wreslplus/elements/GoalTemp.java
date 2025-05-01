package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.EvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorTreeWalker;


public class GoalTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public String condition;
	public String lhs;
	
	public Set<String> dependants;
	public Set<String> dependants_timeseries;
	public Set<String> dependants_svar;
	public Set<String> dependants_dvar;
	public Set<String> dependants_alias;
	public Set<String> dependants_external;
	public Set<String> dependants_parameter;
	public Set<String> dependants_unknown;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;

	public boolean hasCase;
	public boolean hasLhs;
	public boolean isFromAlias;
	public boolean isProcessed;
	public ArrayList<String> caseName;
	public Map<String, GoalCase> caseMap;
	public ArrayList<String> caseCondition;
	public ArrayList<String> caseExpression;
	//     ArrayList< Map< dvarName, Weight > > index is case number
	public ArrayList<Map<String, String>> dvarWeightMapList;
	public ArrayList<ArrayList<String>> dvarSlackSurplusList;	
	public ArrayList<String> slackList;
	public ArrayList<String> surplusList;
	
	// default is 0
	public String timeArraySize;
	public String arraySize;
	
	public GoalTemp(){
		
		lhs=null;
		hasCase=false;
		hasLhs=false;
		isFromAlias=false;
		isProcessed=false;
		condition = Param.always;
		dependants = new LinkedHashSet<String>();
//		dependants_timeseries = new LinkedHashSet<String>();
//		dependants_svar = new LinkedHashSet<String>();
//		dependants_dvar = new LinkedHashSet<String>();
//		dependants_alias = new LinkedHashSet<String>();
//		dependants_external = new LinkedHashSet<String>();
//		dependants_unknown = new LinkedHashSet<String>();
		neededVarInCycleSet = new LinkedHashSet<String>();
		needVarFromEarlierCycle = false;
		caseName=new ArrayList<String>();
		caseMap=new LinkedHashMap<String, GoalCase>();
		caseCondition=new ArrayList<String>();
		caseExpression=new ArrayList<String>();
		dvarWeightMapList = new ArrayList<Map<String,String>>();
		dvarSlackSurplusList = new ArrayList<ArrayList<String>>();
		slackList=new ArrayList<String>();
		surplusList=new ArrayList<String>();
		
		timeArraySize="0";
		arraySize="0";
	}
	
}
	
