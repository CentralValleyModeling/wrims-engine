package wrims.engine.core.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import wrims.engine.core.commondata.wresldata.Param;


public class SvarTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;
	public String id;
	public String kind;
	public String units;
	public String condition;
	//public String expression;
	public Set<String> dependants;
	public Set<String> dependants_timeseries;
	public Set<String> dependants_external;
	public Set<String> dependants_parameter;
	public Set<String> dependants_alias;
	public Set<String> dependants_dvar;
	public Set<String> dependants_svar;
	public Set<String> dependants_unknown;
	public Set<String> dependants_notAllowed;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;
	public boolean isProcessed;


	
	public ArrayList<String> caseName;
	public ArrayList<String> caseCondition;
	public ArrayList<String> caseExpression;
		
	
	// default is 0
	public String timeArraySize;
	public String arraySize;
	
	public SvarTemp(){
		
		fromWresl ="";
		kind=Param.undefined;
		units=Param.undefined;
		condition = Param.always;
		//expression=Param.undefined;
		dependants = new LinkedHashSet<String>();
		dependants_notAllowed = new LinkedHashSet<String>();
//		dependants_timeseries = new LinkedHashSet<String>();
//		dependants_external = new LinkedHashSet<String>();
		//dependants_svar = new LinkedHashSet<String>();
		neededVarInCycleSet = new LinkedHashSet<String>();
		needVarFromEarlierCycle = false;
		isProcessed = false;
		
		timeArraySize="0";
		arraySize="0";
		
		caseName=new ArrayList<String>();
		caseCondition=new ArrayList<String>();
		caseExpression=new ArrayList<String>();
	}
	
}
	
