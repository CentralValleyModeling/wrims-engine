package gov.ca.water.wrims.engine.core.ilp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Dvar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Svar;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.Param;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;

// for LpSolve select 1.Rows 2.Cols 3.Elimeq2 in Presolve

public class AmplWriter {

	private AmplWriter() {

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

	protected static void writeDvarValue(PrintWriter dvarFile, Set<String> dvar_effective) {
		
		Map<String, Dvar> dvMap = SolverData.getDvarMap();
		Map<String, WeightElement> wtMap = SolverData.getWeightMap();
		//Map<String, WeightElement> wtSlackSurplusMap = SolverData.getWeightSlackSurplusMap();
		
		ArrayList<String> dvar_weighted = new ArrayList<String>(wtMap.keySet());
		dvar_weighted.addAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		
		ArrayList<String> dvar_unweighted = new ArrayList<String>(dvMap.keySet());
		dvar_unweighted.removeAll(wtMap.keySet());
		dvar_unweighted.removeAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		
		dvar_unweighted.retainAll(dvar_effective);
		Collections.sort(dvar_weighted);
		Collections.sort(dvar_unweighted);
		
		
		dvarFile.println("/* Weighted Dvar    */");
		for (String s : dvar_weighted){
			String dvName = String.format("%-35s", s);
			dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
		}
		dvarFile.println();
		dvarFile.println("/* Unweighted Dvar    */");	
		for (String s : dvar_unweighted){
			String dvName = String.format("%-35s", s);
			dvarFile.print(dvName + ":  " + ControlData.xasolver.getColumnActivity(s) +"\n"  );
		}
	}	
	
	protected static void writeSvarValue(PrintWriter svarFile) {
		
		Map<String, Svar> svMap = ControlData.currSvMap;
		
		ArrayList<String> sortedTerm = new ArrayList<String>(svMap.keySet());
		Collections.sort(sortedTerm);
		
		for (String s : sortedTerm){
			String svName = String.format("%-35s", s);
			svarFile.print(svName + ":  " + svMap.get(s).getData().getData() +"\n"  );
			
		}
	}
	
	public static void writeComment(PrintWriter outFile, String msg) {
		

		outFile.println("// " + msg );

	}	
	
	public static void writeObjValue() {
		

	}
	

	protected static void writeObj(PrintWriter outFile, Map<String, WeightElement> activeWeightMap) {
		

		String ObjString = "";
		
		ObjString = "# objective function \n";
		ObjString = ObjString + "maximize Obj:  \n";


		String toPrint = "";

		ArrayList<String> sortedTerm = new ArrayList<String>(SolverData.getWeightMap().keySet());
		sortedTerm.addAll(ControlData.currModelDataSet.usedWtSlackSurplusList);
		Collections.sort(sortedTerm);

		for (String dvar : sortedTerm) {

			double weight = activeWeightMap.get(dvar).getValue();

			if (weight > 0) {

				toPrint = toPrint + "+ " + weight + " * " + dvar.toUpperCase() + " ";
				ObjString = ObjString + "+ " + weight + " * " + dvar.toUpperCase() + "\n";
				//_amplFile.println("+ " + weight + " * " + dvar.toUpperCase());

			}
			else if (weight < 0) {
				toPrint = toPrint + weight + " * " + dvar.toUpperCase() + " ";
				ObjString = ObjString + weight + " * " + dvar.toUpperCase() + "\n";
				//_amplFile.println(weight + " * " + dvar.toUpperCase());
			}

		}

		// _ilpFile.println(toPrint);
		ObjString = ObjString + ";";
		
		outFile.print(ObjString);

	}

	protected static void writeConstraint(PrintWriter outFile) {

		//outFile.println("/* constraint */");

		String constraintString = "";
		
		constraintString = "# constraint \n";
		
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
						term = " + " + var.toUpperCase();
					}
					else if (coefDouble == -1.0) {
						term = " - " + var.toUpperCase();
					}
					else if (coefDouble < 0) {
						term = " " + coefStr + " * " + var.toUpperCase();
					}
					else { // coefDouble >= 0
						term = " + " + coefStr + " * " + var.toUpperCase();
					}
					lhs = lhs + term;
				}

			}
			else {

				lhs = "0";
			}

			// TODO: improve this
			String sign = constraintMap.get(constraintName).getSign()+"=";
			sign= sign.replace("==", "=");
			double val = constraintMap.get(constraintName).getEvalExpression().getValue().getData().doubleValue();

			if (val == 0) {
				lhs = constraintName.toUpperCase() + ": " + lhs + " " + sign + " " + "0";
			}
			else {
				lhs = constraintName.toUpperCase() + ": " + lhs + " " + sign + " " + val * -1;
			}

			//_amplFile.println("subject to "+lhs + " ;");
			constraintString = constraintString + "subject to "+lhs + " ;\n";

		}
		
		outFile.print(constraintString);

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

		outFile.println("# dvar ");

		Map<String, Dvar> dvarMap = SolverData.getDvarMap();
		ArrayList<String> sortedDvar = new ArrayList<String>(dvarMap.keySet());
		sortedDvar.retainAll(dvar_effective);
		Collections.sort(sortedDvar);

//		ArrayList<String> intList = new ArrayList<String>();
//		ArrayList<String> freeList = new ArrayList<String>();

		for (String key : sortedDvar) {

			double lower = dvarMap.get(key).lowerBoundValue.doubleValue();
			double upper = dvarMap.get(key).upperBoundValue.doubleValue();
			String lowerStr = dvarMap.get(key).lowerBound;
			String upperStr = dvarMap.get(key).upperBound;
			String toPrint = null;
			String freeTag = "";

			if (lowerStr.equalsIgnoreCase(Param.lower_unbounded) && upperStr.equalsIgnoreCase(Param.upper_unbounded)) {
				// free
				outFile.print("var "+key.toUpperCase());
				freeTag = "  # free";
			}
			else if (lowerStr.equalsIgnoreCase(Param.lower_unbounded)) {
				toPrint = key.toUpperCase() + " <= " + upper;
				outFile.print("var "+toPrint );
			}
			else if (upperStr.equalsIgnoreCase(Param.upper_unbounded)) {

				outFile.print("var "+key.toUpperCase() + " >= " + lower );
			}
			else {

				outFile.print("var "+key.toUpperCase() + " >= " + lower + " <= " + upper );
			}
			

			if (dvarMap.get(key).integer.equalsIgnoreCase(Param.yes)) {
				//intList.add(key);
				outFile.print(" integer ");
			} 
			
			outFile.print(" ; "+freeTag+"\n");
			
		}
		
		outFile.println("# dynamic slack and surplus ");
		
		// for dynamic slack surplus vars
		for (String ss :ControlData.currModelDataSet.usedWtSlackSurplusList) {			
			
			//System.out.println("ss:"+ss);
			outFile.print("var "+ss.toUpperCase() + " >= 0 ; \n");
			
		}

	}

}
