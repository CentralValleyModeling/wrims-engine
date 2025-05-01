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


public class GoalHS implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public String condition;
	public String lhs;
	
	public Set<String> dependants;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;
	
	public ArrayList<String> caseName;
	public Map<String,GoalCase> caseMap;
	
	public GoalHS(){
		
		condition = Param.always;
		dependants = new LinkedHashSet<String>();
		neededVarInCycleSet = new LinkedHashSet<String>();
		needVarFromEarlierCycle = false;
		caseName=new ArrayList<String>();
		caseMap=new LinkedHashMap<String, GoalCase>();
	}
}
	
