package wrimsv2.wreslplus.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.EvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorParser;
import wrimsv2.evaluator.ValueEvaluatorTreeWalker;


public class GoalCase implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public String id;
	public String condition;
	public String rhs;
	public String lhs_gt_rhs;	
	public String lhs_lt_rhs;
	//public Set<String> dependants;	
	
	public GoalCase(){
		
		//id=Param.undefined;
		condition = Param.always;		
		//dependants = new LinkedHashSet<String>();
		lhs_gt_rhs = Param.constrain; 
		lhs_lt_rhs = Param.constrain; 

	}
}
	
