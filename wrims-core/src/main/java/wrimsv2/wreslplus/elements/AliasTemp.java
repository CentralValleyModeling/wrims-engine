package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorTreeWalker;



public class AliasTemp implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String fromWresl; // for test only
	public int line=1;	
	public String id;
	public String kind;
	public String units;
	public String condition;
	public String expression;
	public Set<String> dependants;
	public Set<String> dependants_timeseries;
	public Set<String> dependants_dvar;
	public Set<String> dependants_svar;
	public Set<String> dependants_alias;
	public Set<String> dependants_parameter;
	public Set<String> dependants_unknown;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;
	public boolean isMovedToDvar;	
	public boolean isProcessed;	
	public boolean noSolver;
	
	// default is 0
	public String timeArraySize;
	public String arraySize;
	
	public AliasTemp(){

		kind=Param.undefined;
		units=Param.undefined;
		condition = Param.always;
		expression=Param.undefined;
		dependants = new LinkedHashSet<String>();
//		dependants_timeseries = new LinkedHashSet<String>();
//		dependants_dvar = new LinkedHashSet<String>();
//		dependants_svar = new LinkedHashSet<String>();
//		dependants_alias = new LinkedHashSet<String>();
		neededVarInCycleSet = new LinkedHashSet<String>();
		needVarFromEarlierCycle = false;
		isMovedToDvar = false;
		isProcessed = false;
		noSolver = false;

		timeArraySize="0";
		arraySize="0";
	}
	
}
	
