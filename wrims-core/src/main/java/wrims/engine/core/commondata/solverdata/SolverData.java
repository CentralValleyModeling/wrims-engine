package wrims.engine.core.commondata.solverdata;

import java.util.concurrent.ConcurrentHashMap;

import wrims.engine.core.evaluator.EvalConstraint;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.WeightElement;

public class SolverData {
	private static ConcurrentHashMap<String, EvalConstraint> constraintDataMap=new ConcurrentHashMap<String, EvalConstraint>();
	private static ConcurrentHashMap<String, Dvar> dvarMap= new ConcurrentHashMap<String, Dvar>();
	private static ConcurrentHashMap<String, WeightElement> weightMap = new ConcurrentHashMap<String, WeightElement>();
	private static  ConcurrentHashMap<String, WeightElement> weightSlackSurplusMap = new  ConcurrentHashMap<String, WeightElement>();
		
	public static ConcurrentHashMap<String, WeightElement> getWeightMap(){
		return weightMap;
	}
	
	public static void clearWeightMap(){
		weightMap=new ConcurrentHashMap<String, WeightElement>();
	}
		
	public static  ConcurrentHashMap<String, WeightElement> getWeightSlackSurplusMap(){
		return weightSlackSurplusMap;
	}
	
	public static void clearWeightSlackSurplusMap(){
		weightSlackSurplusMap=new  ConcurrentHashMap<String, WeightElement>();
	}

	public static ConcurrentHashMap<String, Dvar> getDvarMap(){
		return dvarMap;
	}

	public static void clearDvarMap(){
		dvarMap=new ConcurrentHashMap<String, Dvar>();
	}
	
	public static ConcurrentHashMap<String, EvalConstraint> getConstraintDataMap(){
		return constraintDataMap;
	}
	
	public static void clearConstraintDataMap(){
		constraintDataMap=new ConcurrentHashMap<String, EvalConstraint>();
	}
}
