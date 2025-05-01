package wrimsv2.commondata.wresldata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.evaluator.EvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorParser;


public class Goal implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public String scope;
	public String lhs;
	public ArrayList<String> caseName;
	
	//     ArrayList< Map< dvarName, Weight > >
	public ArrayList<Map<String,String>> dvarWeightMapList;
	public ArrayList<ArrayList<String>> dvarSlackSurplusList;	
	public ArrayList<String> dvarName; // from slack or surplus
	public ArrayList<String> dvarWeight; // for the slack or surplus. Negative penalty leads to positive weight
	public ArrayList<String> caseCondition;
	public ArrayList<ValueEvaluatorParser> caseConditionParsers;
	public ArrayList<String> caseExpression;
	public Set<String> expressionDependants;
	public ArrayList<EvaluatorParser> caseExpressionParsers;
	public String fromWresl;
	public int line=1;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;
	
	// default is zero
	public String timeArraySize;
	public ValueEvaluatorParser timeArraySizeParser;
	
	
	public Goal(){
		scope=Param.undefined;
		lhs=Param.undefined;
		caseName       = new ArrayList<String>();
		dvarWeightMapList = new ArrayList<Map<String, String>>(); // ArrayList< Map< dvarName, Weight > >
		dvarSlackSurplusList = new ArrayList<ArrayList<String>>(); // ArrayList< ArrayList< dvarName > >
		dvarName       = new ArrayList<String>();
		dvarWeight     = new ArrayList<String>();		
		caseCondition  = new ArrayList<String>();
		caseExpression = new ArrayList<String>();
		expressionDependants = new HashSet<String>();
		fromWresl = Param.undefined;
		neededVarInCycleSet = new HashSet<String>();
		needVarFromEarlierCycle = false;
		
		timeArraySize = "0"; // default has no future time array
	}
}
	
