package gov.ca.water.wrims.engine.core.solver.mpmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;


public class TransformModel {

	private static final String slack_append   = "___tslac";
	private static final String surplus_append = "___tsurp";

	// original var and transformed var mapping
	// <trans var, trans var - orig var> e.g., x_new, x_new = x_old + 5 =>
	// <x_new, 5>
	public static LinkedHashMap<String, Double> tvarOffsetMap = null;
	// <trans var, equation of transformation> e.g., x_new = 3* x_old + 2* y_old
	// => <x_new, { x_old=3, y_old=2 }>
	public static LinkedHashMap<String, LinkedHashMap<String, Double>> tvarEquationMap = null;

	public static MPModel transform(MPModel in) {

		tvarOffsetMap = new LinkedHashMap<String, Double>();
		tvarEquationMap = new LinkedHashMap<String, LinkedHashMap<String, Double>>();

		MPModel out = new MPModel("Transformed");
		out.constraintLhs = new LinkedHashMap<String, LinkedHashMap<String, Double>>(
				in.constraintLhs);

		// updated with var shift
		LinkedHashMap<String, double[]> transConstraintRhs = new LinkedHashMap<String, double[]>();
		LinkedHashMap<String, LinkedHashMap<String, Double>> tranConstraintLhs = new LinkedHashMap<String, LinkedHashMap<String, Double>>();

		// original var and transformed var mapping
		// <trans var, trans var - orig var> e.g., x_new, x_new = x_old + 5 =>
		// <x_new, 5>
		LinkedHashMap<String, Double> tvarOffsetMap = new LinkedHashMap<String, Double>();
		// <trans var, equation of transformation> e.g., x_new = 3* x_old + 2*
		// y_old => <x_new, { x_old=3, y_old=2 }>
		LinkedHashMap<String, LinkedHashMap<String, Double>> tvarEquationMap = new LinkedHashMap<String, LinkedHashMap<String, Double>>();

		// add binary var (0, 1)
		for (String key : in.var_int_binary) {

			out.addBinaryVar(key);
		}

		// add nonnegative integer var (0, ub)
		for (String key : in.var_int_nonnegative) {

			double ub = in.varMap_integer.get(key)[1];

			out.addIntVar(key, 0, ub);
		}

		// TODO: add general integer var

		// add std num var (0, inf)
		for (String key : in.var_standard) {

			out.addStdVar(key);
		}

		// replace free num var (-inf, inf) with slack - surplus
		for (String key : in.var_free) {

			String slack = key + slack_append;
			String surpl = key + surplus_append;

			out.addStdVar(slack);
			out.addStdVar(surpl);

			// transform equation x = x___surplus - x___slack
			LinkedHashMap<String, Double> equ = new LinkedHashMap<String, Double>();
			equ.put(slack, -1.0);
			equ.put(surpl, 1.0);

			tvarEquationMap.put(key, equ);
		}

		// replace general vars
		for (String key : in.var_general) {

			double lb = in.varMap_number.get(key)[0];
			double ub = in.varMap_number.get(key)[1];

			// / case 1
			// (-inf, inf) => surplus(std) - slack(std)

			// / case 2
			// (-inf, 0) => shift( 0) => - slack(std)
			// (-inf, b) => shift(-b) => - slack(std)
			// (-inf, -a) => shift( a) => - slack(std)

			// / case 3
			// (-a, 0) => shift(a) => surplus(0, a)
			// (-a, b) => shift(a) => surplus(0, a+b)
			// (-a, inf) => shift(a) => surplus(std)
			// (-a, -b) => shift(a) => surplus(a-b)

			// / case 4
			// (0, inf) => no change
			// (0, b) => no change

			// / case 5
			// (a, b) => shift(-a) => surplus(b-a)
			// (a, inf) => shift(-a) => surplus(std)

			if (lb < -Param.inf_assumed) {

				if (ub > Param.inf_assumed) {
					// / case 1
					// (-inf, inf) => surplus(std) - slack(std)

					String slack = key + slack_append;
					String surpl = key + surplus_append;

					out.addStdVar(slack);
					out.addStdVar(surpl);

					// transform equation x = x___surplus - x___slack
					LinkedHashMap<String, Double> equ = new LinkedHashMap<String, Double>();
					equ.put(slack, -1.0);
					equ.put(surpl, 1.0);

					tvarEquationMap.put(key, equ);

				} else {
					// / case 2
					// (-inf, 0) => shift( 0) => - slack(std)
					// (-inf, b) => shift(-b) => - slack(std)
					// (-inf, -a) => shift( a) => - slack(std)

					// shift , slack
					tvarOffsetMap.put(key, -ub);
					String slack = key + slack_append;

					out.addStdVar(slack);

					LinkedHashMap<String, Double> equ = new LinkedHashMap<String, Double>();
					equ.put(slack, -1.0);

					tvarEquationMap.put(key, equ);
				}

			} else if (lb < 0) {
				// / case 3
				// (-a, 0) => shift(a) => surplus(0, a)
				// (-a, b) => shift(a) => surplus(0, a+b)
				// (-a, inf) => shift(a) => surplus(std)
				// (-a, -b) => shift(a) => surplus(a-b)

				// shift, surplus
				tvarOffsetMap.put(key, -lb);
				String surpl = key + surplus_append;

				out.addGeneralVar(surpl, 0, -lb + ub);

				LinkedHashMap<String, Double> equ = new LinkedHashMap<String, Double>();
				equ.put(surpl, 1.0);

				tvarEquationMap.put(key, equ);

			} else if (lb == 0) {
				// / case 4
				// (0, inf) => no change
				// (0, b) => no change

				out.addGeneralVar(key, 0, ub);

			} else if (lb > 0) {

				// / case 5
				// (a, b) => shift(-a) => surplus(b-a)
				// (a, inf) => shift(-a) => surplus(std)

				// shift, surplus
				tvarOffsetMap.put(key, -lb);
				String surpl = key + surplus_append;

				out.addGeneralVar(surpl, 0, -lb + ub);

				LinkedHashMap<String, Double> equ = new LinkedHashMap<String, Double>();
				equ.put(surpl, 1.0);

				tvarEquationMap.put(key, equ);

			}
		}

		// find transformed constraint
		for (String constName : in.constraintLhs.keySet()) {

			double[] rhs = in.constraintRhs.get(constName);
			LinkedHashMap<String, Double> varCoef = in.constraintLhs
					.get(constName);
			LinkedHashMap<String, Double> newVarCoef = new LinkedHashMap<String, Double>();

			for (String term : varCoef.keySet()) {

				double coef = varCoef.get(term);

				// use equ terms instead of term in lhs
				if (tvarEquationMap.keySet().contains(term)) {

					LinkedHashMap<String, Double> equ = tvarEquationMap
							.get(term);

					for (String newTerm : equ.keySet()) {
						newVarCoef.put(newTerm, equ.get(newTerm) * coef);
					}

					// adjust rhs //TODO: put objectFunction offset here?
					if (tvarOffsetMap.keySet().contains(term)) {

						double offset = tvarOffsetMap.get(term);

						rhs[0] = rhs[0] + offset;
						rhs[1] = rhs[1] + offset;
					}
				} else {

					newVarCoef.put(term, coef);
				}
			}

			out.createConstraint(constName, newVarCoef, rhs[0], rhs[1]);
		}


		// transform obj function

		LinkedHashMap<String, Double> newObjFunction = new LinkedHashMap<String, Double>();

		for (String o : in.objFunction.keySet()) {

			double oCoef = in.objFunction.get(o);

			// use equ terms instead of term in lhs
			if (tvarEquationMap.keySet().contains(o)) {

				LinkedHashMap<String, Double> equ = tvarEquationMap.get(o);

				for (String newTerm : equ.keySet()) {
					out.objFunction.put(newTerm, equ.get(newTerm) * oCoef);
				}

			} else {

				out.objFunction.put(o, oCoef);
			}

		}

		out.tvarEquationMap = tvarEquationMap;
		out.tvarOffsetMap = tvarOffsetMap;

		// solve transformed lp problem // in Detector

		// shift back solution // in Detector
		// LinkedHashMap<String, Double> revertedSolution =
		// shiftSolution(transM.solution, varOffsetMap);

		return out;
	}

	public static ArrayList<LinkedHashMap<String, Double>> restoreSolutions(
			ArrayList<LinkedHashMap<String, Double>> inSolutions,
			LinkedHashMap<String, Double> varOffsetMap,
			LinkedHashMap<String, LinkedHashMap<String, Double>> varEquationMap) {

		ArrayList<LinkedHashMap<String, Double>> restoredSolutions = new ArrayList<LinkedHashMap<String, Double>>();

		for (LinkedHashMap<String, Double> solution : inSolutions) {

			restoredSolutions.add(restoreSolution(solution, varOffsetMap,
					varEquationMap));

		}

		return restoredSolutions;

	}

	// pre transform solution
	public static LinkedHashMap<String, Double> restoreSolution(
			LinkedHashMap<String, Double> inSolution,
			LinkedHashMap<String, Double> varOffsetMap,
			LinkedHashMap<String, LinkedHashMap<String, Double>> varEquationMap) {

		LinkedHashMap<String, Double> outSolution = new LinkedHashMap<String, Double>(
				inSolution);

		Set<String> removeTheseFromSolution = new HashSet<String>();

		for (String k : varEquationMap.keySet()) {

			double v = 0;
			LinkedHashMap<String, Double> equ = varEquationMap.get(k);

			for (String term : equ.keySet()) {

				v = v + inSolution.get(term) * equ.get(term);
				outSolution.put(k, v);

				removeTheseFromSolution.add(term);

			}

		}

		// restore offset
		for (String k : varOffsetMap.keySet()) {

			double offset = varOffsetMap.get(k);

			double newValue = outSolution.get(k) - offset;

			outSolution.put(k, newValue);

		}

		for (String k : removeTheseFromSolution) {

			outSolution.remove(k);

		}

		return outSolution;
	}

	public static double restoreObjValue(
			LinkedHashMap<String, Double> originalObjFunction,
			LinkedHashMap<String, Double> restoredSolution) {

		double outObjValue = 0;

		for (String k : originalObjFunction.keySet()) {

			outObjValue = outObjValue + restoredSolution.get(k)
					* originalObjFunction.get(k);

		}

		return outObjValue;
	}

}
