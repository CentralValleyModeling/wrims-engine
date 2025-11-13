package wrims.engine.core.wreslplus.elements;

import java.io.Serializable;

import wrims.engine.core.commondata.wresldata.Param;


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
	
