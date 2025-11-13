package wrims.engine.core.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrims.engine.core.commondata.wresldata.Param;


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
	
