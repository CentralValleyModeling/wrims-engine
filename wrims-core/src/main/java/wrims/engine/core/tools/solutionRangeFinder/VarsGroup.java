package wrims.engine.core.tools.solutionRangeFinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import wrims.engine.core.solver.mpmodel.MPModel;
import wrims.engine.core.wreslplus.elements.Tools;

public class VarsGroup {

	public List<String> lowerVertexVars_number;
	public List<String> lowerVertexVars_integer;

	public List<String> notLowerVertexVars_number;
	public List<String> notLowerVertexVars_integer;
	
	public List<String> notUpperVertexVars_number;
	public List<String> notUpperVertexVars_integer;
	
	// required
	private final double vertax_tolerance;  // tolerance for determining if a var is at vertex
	private MPModel baseModel;	
	
	// optional 
	private ArrayList<String> excludeStrings = null; // if not set then exclude none
	private ArrayList<String> includeStrings = null; // if not set then include all
	public ArrayList<String> varsPool;	// default is base solution
	
	// processed
	public ArrayList<String> varsPool_number;	// subset of varsPool
	public ArrayList<String> varsPool_integer;	// subset of varsPool
	
	public VarsGroup(MPModel in, double vertax_tolerance) {

		lowerVertexVars_number = new ArrayList<String>();
		lowerVertexVars_integer = new ArrayList<String>();
		
		excludeStrings = null;
		includeStrings = null;
		
		baseModel = in;
		this.vertax_tolerance = vertax_tolerance;
		this.varsPool = new ArrayList<String>(baseModel.solution.keySet());
	}

	public void setIncludeStrings(ArrayList<String> in){
		
		includeStrings = in;
		
	}
	
	public void setExcludeStrings(ArrayList<String> in){
		
		excludeStrings = in;
		
	}	
	
	
	public void setVarsPool(ArrayList<String> varsToSearch_in){
		
		
		ArrayList<String> p = new ArrayList<String>(Tools.allToLowerCase(varsToSearch_in));

		if (p.retainAll(baseModel.solution.keySet())){
			// unknown vars...
			ArrayList<String> unknownVars = new ArrayList<String>(Tools.allToLowerCase(varsToSearch_in));
			unknownVars.removeAll(p);
			System.err.println("# Error: unknown vars in varsPool:"+ unknownVars);
		
		} else {
			
			varsPool = p;
			
			varsPool_integer = new ArrayList<String>(varsPool);
			varsPool_integer.retainAll(baseModel.varMap_integer.keySet());

			varsPool_number = new ArrayList<String>(varsPool);
			varsPool_number.retainAll(baseModel.varMap_number.keySet());
			
		}		
	
	}
	
	public void filterVarsPool(ArrayList<String> includeStrings, ArrayList<String> excludeStrings ){
		
		
		if (includeStrings!=null && includeStrings.size()>0) {
			
			this.includeStrings = new ArrayList<String>(Tools.allToLowerCase(includeStrings));
			
			
			ArrayList<String> varsPool_t = new ArrayList<String>(varsPool);
			for (String incStr: this.includeStrings){
				
				for (String key: varsPool_t){
					
					if (!key.contains(incStr)) varsPool.remove(key);		
					
				}
				
			}
			
		}
		
		if (excludeStrings!=null && excludeStrings.size()>0) {
			
			this.excludeStrings = new ArrayList<String>(Tools.allToLowerCase(excludeStrings));
			
			
			ArrayList<String> varsPool_t = new ArrayList<String>(varsPool);
			for (String excStr: this.excludeStrings){
				
				for (String key: varsPool_t){
					
					if (key.contains(excStr)) varsPool.remove(key);		
					
				}
				
			}
			
		}
		
	}	

	public void findVarsAtVertex(){		
		
		lowerVertexVars_number  = findLowerVertaxVars(varsPool_number,  baseModel.solution, baseModel.varMap_number);
		lowerVertexVars_integer = findLowerVertaxVars(varsPool_integer, baseModel.solution, baseModel.varMap_integer);

		notLowerVertexVars_number  = findNotLowerVertaxVars(varsPool_number,  baseModel.solution, baseModel.varMap_number);
		notLowerVertexVars_integer = findNotLowerVertaxVars(varsPool_integer, baseModel.solution, baseModel.varMap_integer);

		notUpperVertexVars_number  = findNotUpperVertaxVars(varsPool_number,  baseModel.solution, baseModel.varMap_number);
		notUpperVertexVars_integer = findNotUpperVertaxVars(varsPool_integer, baseModel.solution, baseModel.varMap_integer);
		
	}

	private ArrayList<String> findNotUpperVertaxVars(ArrayList<String> vars, LinkedHashMap<String, Double> solutionMap, LinkedHashMap<String, double[]> varBounds) {

		ArrayList<String> out = new ArrayList<String>();

		for (String key : vars) {

			if (solutionMap.get(key) < varBounds.get(key)[1]) {

				out.add(key);

			}
		}

		return out;
	}
	
	private ArrayList<String> findNotLowerVertaxVars(ArrayList<String> vars, LinkedHashMap<String, Double> solutionMap, LinkedHashMap<String, double[]> varBounds) {

		ArrayList<String> out = new ArrayList<String>();

		for (String key : vars) {

			if (solutionMap.get(key) > varBounds.get(key)[0]) {

				out.add(key);

			}
		}

		return out;
	}
	
	private ArrayList<String> findLowerVertaxVars(ArrayList<String> vars, LinkedHashMap<String, Double> solutionMap, LinkedHashMap<String, double[]> varBounds) {

		ArrayList<String> out = new ArrayList<String>();

		for (String key : vars) {

			if (solutionMap.get(key) < varBounds.get(key)[0] + vertax_tolerance) {

				out.add(key);

			}
		}

		return out;
	}
	
}
