package wrimsv2.wreslplus.elements.procedures;

import java.util.Set;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.wreslplus.elements.DvarTemp;
import wrimsv2.wreslplus.elements.GoalTemp;
import wrimsv2.wreslplus.elements.ModelTemp;
import wrimsv2.wreslplus.elements.WeightTemp;

public class Misc {

	private Misc() {
	}

	public static void addWeightInGroupWeightMap(String id, String fromWresl, String weight, ModelTemp mObj) {
		
		WeightTemp w1 = new WeightTemp();
		w1.id = id;
		w1.fromWresl = fromWresl;
		w1.weight = weight;

		mObj.groupWeightMap.put(id.toLowerCase(), w1);
		mObj.wvList_post.add(id.toLowerCase());
		
	}
	
	public static void createDvarInModelObj(String id, String ub, String lb, String kind, String units, 
			String fromWresl, ModelTemp mObj) {
		
		DvarTemp d = new DvarTemp();
		d.id = id;
		d.upperBound = ub;
		d.lowerBound = lb;
		d.kind = kind;
		d.units = units;
		d.fromWresl = fromWresl;
		//d.condition = condition;

		mObj.dvList.add(d.id.toLowerCase());
		mObj.dvMap.put(d.id.toLowerCase(), d);
		
	}
	
	public static void createGoalInModelObj(String id, String lhs, String relation, String rhs,  
			Set<String> dependants, String fromWresl, String condition, ModelTemp mObj) {
		
		GoalTemp g = new GoalTemp();
		g.fromWresl = fromWresl;
		g.id = id;

		g.caseExpression.add(lhs + relation + rhs);
		g.condition = condition;
		g.caseName.add(Param.defaultCaseName);
		g.caseCondition.add(Param.always);
		g.dependants = dependants;

		mObj.glList.add(g.id.toLowerCase());
		mObj.glMap.put(g.id.toLowerCase(), g);
		
	}
	
}
