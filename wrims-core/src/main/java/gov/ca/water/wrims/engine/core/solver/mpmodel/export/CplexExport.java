package gov.ca.water.wrims.engine.core.solver.mpmodel.export;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;



public class CplexExport {

	private CplexExport() {

	}

	public static void writeComment(PrintWriter outFile, String msg) {
		

		outFile.println("\\ " + msg );

	}	
	
	public static void writeObjValue() {
		

	}
	

	protected static void writeObj(PrintWriter outFile, Map<String, WeightElement> activeWeightMap) {
		
		outFile.println("\\ objective function ");
		outFile.println("Maximize");


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

		//outFile.println(";");

	}

	protected static void writeConstraint(PrintWriter outFile) {

		outFile.println("");
		outFile.println("\\ constraint");
		outFile.println("Subject To");

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

			if (sign.equals(">")) sign = ">=";
			if (sign.equals("<")) sign = "<=";
			
			if (val == 0) {
				lhs = constraintName + ": " + lhs + " " + sign + " " + "0";
			}
			else {
				lhs = constraintName + ": " + lhs + " " + sign + " " + val * -1;
			}

			outFile.println(lhs);

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
	
		outFile.println("");		
		outFile.println("\\ dvar");
		outFile.println("Bounds");
		
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
			if (lower<Param.lower_unbounded_double && upper>Param.upper_unbounded_double) {
				freeList.add(key);  //TODO: test what happen if it's a free integer ???
				continue;  
			}
			else if (lowerStr.equalsIgnoreCase(Param.lower_unbounded) || lower<Param.lower_unbounded_double) {
				outFile.print(" -inf <= " + key + " <= " + upper + " \n");
			}
			else if (upperStr.equalsIgnoreCase(Param.upper_unbounded) || upper>Param.upper_unbounded_double){

				if (lower != 0) outFile.print(key + " >= " + lower + " \n");
			}
			else {
				if (lower != 0) {	
					outFile.print(lower + " <= " + key + " <= " + upper + " \n");
				} else {
					outFile.print(key + " <= " + upper + " \n");
				}
			}
		}
		
		if (freeList.size() > 0) {
			//outFile.print("free ");
	
			for (int i = 0; i < freeList.size(); i++) {
				String term = freeList.get(i);
	
				outFile.print(" -inf <= " + term + " \n");

			}
	
		}
		
		if (intList.size() > 0) {
			outFile.println("");
			outFile.println("\\ Integer");
			outFile.println("Generals");
	
			for (int i = 0; i < intList.size(); i++) {
				String term = intList.get(i);
	
				//if (i == 0) {
					outFile.println(term);
				//}
				//else {
				//	outFile.print(", " + term);
				//}
			}
	
			//outFile.print(" ;\n");
		}
	

	}	

}
