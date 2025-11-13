package wrims.engine.core.commondata.wresldata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import wrims.engine.core.components.IntDouble;
import wrims.engine.core.evaluator.ValueEvaluatorParser;
import wrims.engine.core.evaluator.ValueEvaluatorTreeWalker;


public class Svar implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	// These properties are the same for all Svar time array
	public String fromWresl;
	public int line=1;
	public String scope;
	public String dssBPart;
	public String format;
	public String kind;
	public String units;
	public String convertToUnits;
	public Set<String> dependants;
	public Set<String> neededVarInCycleSet;
	public boolean needVarFromEarlierCycle;

	
	private IntDouble data;
	
	
	// default is zero
	public String timeArraySize;
	public ValueEvaluatorParser timeArraySizeParser;
	
	// These are for: 
	// (1) normal Svar if timeArraySize=0
	//		e.g., define someSvar { value 1 }
	//
	// (2) Svar time array if future expressions are the same
	//      e.g.,  define(3) someSvar { value $m }
	public ArrayList<String> caseName;
	public ArrayList<String> caseCondition;
	public ArrayList<ValueEvaluatorParser> caseConditionParsers;
	public ArrayList<ValueEvaluatorTreeWalker> caseConditionWalkers;
	public ArrayList<String> caseExpression;
	public ArrayList<ValueEvaluatorParser> caseExpressionParsers;
	public ArrayList<ValueEvaluatorTreeWalker> caseExpressionWalkers;
	

	// These maps are for time array of Svar if future definitions have different expressions
	//      example 1:  define(3) someSvar { 
	//                                      (0) { value 99   }
	//                                      (1) { value a+b  }
	//                                      (2) { value 7    }
	//                                      (3) { value 2*k  }   }
	//      example 2:  define(3) someSvar { 
	//                                      (0:2) { value 99   }
	//                                      (3)   { value 2*k  }   }
	public Map<Integer, ArrayList<String>> timeMap_caseName;
	public Map<Integer, ArrayList<String>> timeMap_caseCondition;
	public Map<Integer, ArrayList<String>> timeMap_caseExpression;


	
	public Svar(){
				
		scope=Param.undefined;
		dssBPart=Param.undefined;
		format=Param.undefined;
		kind=Param.undefined;
		units=Param.undefined;
		convertToUnits =Param.undefined;
		caseName       = new ArrayList<String>();
		caseCondition  = new ArrayList<String>();
		caseConditionParsers = new ArrayList<ValueEvaluatorParser>();
		caseConditionWalkers = new ArrayList<ValueEvaluatorTreeWalker>();
		caseExpression = new ArrayList<String>();
		caseExpressionParsers = new ArrayList<ValueEvaluatorParser>();
		caseExpressionWalkers = new ArrayList<ValueEvaluatorTreeWalker>();
		fromWresl = Param.undefined;
		dependants = new HashSet<String>();
		data = null;
		neededVarInCycleSet = new HashSet<String>();
		needVarFromEarlierCycle = false;
		
		timeArraySize = "0"; // default has no future time array
		
		timeMap_caseName = new HashMap<Integer, ArrayList<String>>();
		timeMap_caseCondition = new HashMap<Integer, ArrayList<String>>();
		timeMap_caseExpression = new HashMap<Integer, ArrayList<String>>();
	}	
	
	public void setData(IntDouble data){
		this.data=data;
	}
	
	public IntDouble getData(){
		return data;
	}
}
	
