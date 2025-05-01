package wrimsv2.solver.mpmodel;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;

import wrimsv2.commondata.wresldata.Param;

public class MPModel implements Serializable {

	public String modelName = "";
	public LinkedHashMap<String, double[]> varMap_number = null;
	public HashSet<String> var_general = null;
	public HashSet<String> var_standard = null;  // warning: is a subset of all standard vars
	public HashSet<String> var_free = null;

	public LinkedHashMap<String, double[]> varMap_integer = null;
	public HashSet<String> var_int_binary = null;
	public HashSet<String> var_int_nonnegative = null; // has upper bound

	// <constraintName, <varName, varCoefficient>>
	public LinkedHashMap<String, LinkedHashMap<String, Double>> constraintLhs = null;
	public LinkedHashMap<String, double[]> constraintRhs = null;

	public LinkedHashMap<String, Double> objFunction = null;
	public LinkedHashMap<String, Double> solution = null;
	public double objValue;

	public LinkedHashMap<String, Double> originalObjFunction = null;
	public LinkedHashMap<String, Double> restoredSolution = null;
	public double restoredObjValue;

	//
	public LinkedHashMap<String, Double> tvarOffsetMap = null;
	public LinkedHashMap<String, LinkedHashMap<String, Double>> tvarEquationMap = null;
	public LinkedHashMap<String, Double> solution_original = null;

	public MPModel(String modelName) {

		this.modelName = modelName;
		varMap_number = new LinkedHashMap<String, double[]>();
		var_general = new HashSet<String>();
		var_standard = new HashSet<String>();
		var_free = new HashSet<String>();
		varMap_integer = new LinkedHashMap<String, double[]>();
		var_int_binary = new HashSet<String>();
		var_int_nonnegative = new HashSet<String>();
		constraintLhs = new LinkedHashMap<String, LinkedHashMap<String, Double>>();
		constraintRhs = new LinkedHashMap<String, double[]>();
		objFunction = new LinkedHashMap<String, Double>();
		objValue = 0;

		//
		tvarOffsetMap = new LinkedHashMap<String, Double>();
		tvarEquationMap = new LinkedHashMap<String, LinkedHashMap<String, Double>>();
		solution_original = new LinkedHashMap<String, Double>();
	}

	public MPModel(MPModel in, String modelName) {

		this.modelName = modelName;
		varMap_number = new LinkedHashMap<String, double[]>(in.varMap_number);
		var_general = new HashSet<String>(in.var_general);
		var_standard = new HashSet<String>(in.var_standard);
		var_free = new HashSet<String>(in.var_free);
		varMap_integer = new LinkedHashMap<String, double[]>(in.varMap_integer);
		var_int_binary = new HashSet<String>(in.var_int_binary);
		var_int_nonnegative = new HashSet<String>(in.var_int_nonnegative);
		constraintLhs = new LinkedHashMap<String, LinkedHashMap<String, Double>>(in.constraintLhs);
		constraintRhs = new LinkedHashMap<String, double[]>(in.constraintRhs);
		objFunction = new LinkedHashMap<String, Double>(in.objFunction);
		objValue = 0;

		//
		tvarOffsetMap = new LinkedHashMap<String, Double>();
		tvarEquationMap = new LinkedHashMap<String, LinkedHashMap<String, Double>>();
		solution_original = new LinkedHashMap<String, Double>();
	}

	// number: non-binary & non-integer
	public void addGeneralVar(String varName, double lb, double ub) {

		double[] lb_ub = new double[] { lb, ub };
		varMap_number.put(varName, lb_ub);

		if (lb == 0 && ub > Param.inf_assumed) {
			var_standard.add(varName);
		} else if (lb < -Param.inf_assumed && ub > Param.inf_assumed) {
			var_free.add(varName);
		} else {
			var_general.add(varName);
		}

	}

	// number: standard var (0,inf)
	public void addStdVar(String varName) {

		double[] lb_ub = new double[] { 0, Param.inf };
		varMap_number.put(varName, lb_ub);
		var_standard.add(varName);

	}

	// number: free var (-inf, inf)
	public void addFreeVar(String varName) {

		double[] lb_ub = new double[] { -Param.inf, Param.inf };
		varMap_number.put(varName, lb_ub);
		var_free.add(varName);

	}

	// integer: binary var (0,1)
	public void addBinaryVar(String varName) {

		// warning, can't set {0, 1}
		double[] lb_ub = new double[] { 0, 1.0 };
		varMap_integer.put(varName, lb_ub);
		var_int_binary.add(varName);

	}

	// TODO: general integer var is not complete yet.

	public void addIntVar(String varName, double lb, double ub) {

		// with upper bound (0, ub)
		if (lb == 0) {
			// warning, need to add tolerance
			double[] lb_ub = new double[] { 0, ub };

			varMap_integer.put(varName, lb_ub);
			
			if (ub == 1) {
				var_int_binary.add(varName);
			} else {
				var_int_nonnegative.add(varName);
			}
		
		} else {
			
			System.err.println(" Error in MPModel.");
			throw new java.lang.Error();

		}

	}

	public void createConstraint(String constraintName, LinkedHashMap<String, Double> varCoefMap, double lb, double ub) {

		constraintLhs.put(constraintName, varCoefMap);
		double[] lb_ub = new double[] { lb, ub };
		constraintRhs.put(constraintName, lb_ub);

	}

}
