package wrims.engine.core.ilp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import wrims.engine.core.commondata.solverdata.SolverData;
import wrims.engine.core.commondata.wresldata.Dvar;
import wrims.engine.core.commondata.wresldata.WeightElement;
import wrims.engine.core.commondata.wresldata.Param;
import wrims.engine.core.evaluator.EvalConstraint;

// for LpSolve select 1.Rows 2.Cols 3.Elimeq2 in Presolve

public class LpSolveWriter {

	private LpSolveWriter() {

	}

//	public static Set<String> output() {
//		
//		Set<String> usedDvar = new HashSet<String>();
//		
//		writeInfo();
//		Set<String> usedDvarInWeight = writeObj();
//		_ilpFile.print("\n");
//		Set<String> usedDvarInConstraint = writeConstraint();
//		_ilpFile.print("\n");
//		usedDvar.addAll(usedDvarInWeight);
//		usedDvar.addAll(usedDvarInConstraint);
//		writeDvar(usedDvar);
//		_ilpFile.flush();
//		
//		// write to _svarFile
//		writeSvarValue();
//		return usedDvar;
//	}

	public static void writeComment(PrintWriter outFile, String msg) {
		

		outFile.println("// " + msg );

	}	
	
	public static void writeObjValue() {
		

	}
	

	protected static void writeObj(PrintWriter outFile, Map<String, WeightElement> activeWeightMap) {
		
		outFile.println("// objective function ");
		outFile.println("max: ");


		String toPrint = "";

		ArrayList<String> sortedTerm = new ArrayList<String>(activeWeightMap.keySet());
		
		Collections.sort(sortedTerm);

		for (String dvar : sortedTerm) {

			double weight = activeWeightMap.get(dvar).getValue();

			if (weight > 0) {

				toPrint = toPrint + "+ " + weight + " " + dvar + " ";
				outFile.println("+ " + weight + " " + dvar);

			}
			else if (weight < 0) {
				toPrint = toPrint + weight + " " + dvar + " ";
				outFile.println(weight + " " + dvar);
			}

		}

		// _ilpFile.println(toPrint);
		outFile.println(";");

	}

	protected static void writeConstraint(PrintWriter outFile) {

		outFile.println("/* constraint */");

		Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();

		ArrayList<String> sortedConstraint = new ArrayList<String>(constraintMap.keySet());
		Collections.sort(sortedConstraint);

		for (String constraintName : sortedConstraint) {

			String lhs = "";

			if (!constraintMap.get(constraintName).getEvalExpression().isNumeric()) {

				ArrayList<String> sortedTerm = new ArrayList<String>(constraintMap.get(constraintName)
						.getEvalExpression().getMultiplier().keySet());
				Collections.sort(sortedTerm);

				for (String var : sortedTerm) {

					Number coef = constraintMap.get(constraintName).getEvalExpression().getMultiplier().get(var)
							.getData();
					double coefDouble = coef.doubleValue();
					String coefStr = coef.toString();
					String term;

					if (coefDouble == 1.0) {
						term = " + " + var;
					}
					else if (coefDouble == -1.0) {
						term = " - " + var;
					}
					else if (coefDouble < 0) {
						term = " " + coefStr + " " + var;
					}
					else { // coefDouble >= 0
						term = " + " + coefStr + " " + var;
					}
					lhs = lhs + term;
				}

			}
			else {

				lhs = "0";
			}

			String sign = constraintMap.get(constraintName).getSign();
			double val = constraintMap.get(constraintName).getEvalExpression().getValue().getData().doubleValue();

			if (val == 0) {
				lhs = constraintName + ": " + lhs + " " + sign + " " + "0";
			}
			else {
				lhs = constraintName + ": " + lhs + " " + sign + " " + val * -1;
			}

			outFile.println(lhs + " ;");

		}

	}

	protected static void writeDvar(PrintWriter outFile, Set<String> dvar_weighted, Set<String> dvar_unWeighted) {
		
		
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		
		ArrayList<String> sortedDvar_weighted = new ArrayList<String>(dvarMap.keySet());
		sortedDvar_weighted.retainAll(dvar_weighted);
		Collections.sort(sortedDvar_weighted);
		
		ArrayList<String> sortedDvar_unWeighted = new ArrayList<String>(dvarMap.keySet());
		sortedDvar_unWeighted.retainAll(dvar_unWeighted);
		Collections.sort(sortedDvar_unWeighted);

		// TODO: separate integer list in the parsing stage for efficiency
		ArrayList<String> intList = new ArrayList<String>();
		ArrayList<String> freeList = new ArrayList<String>();

		
	    /////// weighted
		outFile.println("/* dvar weighted */");
		
		for (String key : sortedDvar_weighted) {
			double lower = dvarMap.get(key).lowerBoundValue.doubleValue();
			double upper = dvarMap.get(key).upperBoundValue.doubleValue();
			String lowerStr = dvarMap.get(key).lowerBound;
			String upperStr = dvarMap.get(key).upperBound;

			if (dvarMap.get(key).integer.equalsIgnoreCase(Param.yes)) intList.add(key);
			
			if (lowerStr.equalsIgnoreCase(Param.lower_unbounded) && upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				freeList.add(key);  //TODO: test what happen if it's a free integer ???
				continue;  
			}
			else if (lowerStr.equalsIgnoreCase(Param.lower_unbounded)) {
				outFile.print(key + " < " + upper + " ;\n");
			}
			else if (upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				// TODO: be careful, by default LpSolve treat this as standard so we don't write to ilp file
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
			}
			else {
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
				outFile.print(key + " < " + upper + " ;\n");
			}
		}

		/////// unweighted
		outFile.println("/* dvar unweighted in constraint*/");
		
		for (String key : sortedDvar_unWeighted) {
			double lower = dvarMap.get(key).lowerBoundValue.doubleValue();
			double upper = dvarMap.get(key).upperBoundValue.doubleValue();
			String lowerStr = dvarMap.get(key).lowerBound;
			String upperStr = dvarMap.get(key).upperBound;

			if (dvarMap.get(key).integer.equalsIgnoreCase(Param.yes)) intList.add(key);
			
			if (lowerStr.equalsIgnoreCase(Param.lower_unbounded) && upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				freeList.add(key);  //TODO: test what happen if it's a free integer ???
				continue;  
			}
			else if (lowerStr.equalsIgnoreCase(Param.lower_unbounded)) {
				outFile.print(key + " < " + upper + " ;\n");
			}
			else if (upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				// TODO: be careful, by default LpSolve treat this as standard so we don't write to ilp file
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
			}
			else {
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
				outFile.print(key + " < " + upper + " ;\n");
			}
		}		
		
			
		
		
		if (intList.size() > 0) {
			outFile.print("int ");

			for (int i = 0; i < intList.size(); i++) {
				String term = intList.get(i);

				if (i == 0) {
					outFile.print(term);
				}
				else {
					outFile.print(", " + term);
				}
			}

			outFile.print(" ;\n");
		}

		if (freeList.size() > 0) {
			outFile.print("free ");

			for (int i = 0; i < freeList.size(); i++) {
				String term = freeList.get(i);

				if (i == 0) {
					outFile.print(term);
				}
				else {
					outFile.print(", " + term);
				}
			}

			outFile.print(" ;\n");
		}
	}

	protected static void writeDvar(PrintWriter outFile, Set<String> dvar_effective) {
		
		
		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		
		ArrayList<String> sortedDvar_effective = new ArrayList<String>(dvarMap.keySet());
		sortedDvar_effective.retainAll(dvar_effective);
		Collections.sort(sortedDvar_effective);
		
	
		// TODO: separate integer list in the parsing stage for efficiency
		ArrayList<String> intList = new ArrayList<String>();
		ArrayList<String> freeList = new ArrayList<String>();
	
		
		outFile.println("/* dvar */");
		
		for (String key : sortedDvar_effective) {
			double lower = dvarMap.get(key).lowerBoundValue.doubleValue();
			double upper = dvarMap.get(key).upperBoundValue.doubleValue();
			String lowerStr = dvarMap.get(key).lowerBound;
			String upperStr = dvarMap.get(key).upperBound;
	
			if (dvarMap.get(key).integer.equalsIgnoreCase(Param.yes)) intList.add(key);
			
			if (lowerStr.equalsIgnoreCase(Param.lower_unbounded) && upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				freeList.add(key);  //TODO: test what happen if it's a free integer ???
				continue;  
			}
			else if (lowerStr.equalsIgnoreCase(Param.lower_unbounded)) {
				outFile.print(key + " < " + upper + " ;\n");
			}
			else if (upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				// TODO: be careful, by default LpSolve treat this as standard so we don't write to ilp file
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
			}
			else {
				if (lower != 0) outFile.print(key + " > " + lower + " ;\n");
				outFile.print(key + " < " + upper + " ;\n");
			}
		}
			
		
		if (intList.size() > 0) {
			outFile.print("int ");
	
			for (int i = 0; i < intList.size(); i++) {
				String term = intList.get(i);
	
				if (i == 0) {
					outFile.print(term);
				}
				else {
					outFile.print(", \n" + term);
				}
			}
	
			outFile.print(" ;\n");
		}
	
		if (freeList.size() > 0) {
			outFile.print("free ");
	
			for (int i = 0; i < freeList.size(); i++) {
				String term = freeList.get(i);
	
				if (i == 0) {
					outFile.print(term);
				}
				else {
					outFile.print(", \n" + term);
				}
			}
	
			outFile.print(" ;\n");
		}
	}	

}
