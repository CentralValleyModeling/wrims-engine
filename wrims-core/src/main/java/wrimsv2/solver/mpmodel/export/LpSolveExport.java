package wrimsv2.solver.mpmodel.export;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import wrimsv2.commondata.wresldata.Param;
import wrimsv2.solver.mpmodel.MPModel;


public class LpSolveExport {

	
	private LpSolveExport() {

	}

	public static void writeComment(PrintWriter outFile, String msg) {
		

		outFile.println("// " + msg );

	}	
	
	public static void writeObjValue() {
		

	}
	

	public static String writeObj(LinkedHashMap<String, Double> activeWeightMap) {
		
		String out = "// objective function\n";

		out = out + "max:\n";

		ArrayList<String> sortedTerm = new ArrayList<String>(activeWeightMap.keySet());
		
		Collections.sort(sortedTerm);

		for (String dvar : sortedTerm) {

			double weight = activeWeightMap.get(dvar);

			if (weight > 0) {

				out = out + "+ " + weight + " " + dvar + "\n";

			}
			else if (weight < 0) {
				out = out + weight + " " + dvar + "\n";

			}

		}
		
		out = out + ";\n";
		return out;

	}

	public static String writeConstraint(LinkedHashMap<String, LinkedHashMap<String, Double>> constraintLhs, LinkedHashMap<String, double[]> constraintRhs) {


		String out ="/* constraint */\n";


		for (String constraintName : constraintLhs.keySet()) {

			LinkedHashMap<String, Double> lhs_map = constraintLhs.get(constraintName);
			
			String lhs = "";
			
			// TODO:  this constraint might be always true or always false
			if (lhs_map.keySet().size()==0) {
				System.err.println("# Error: check constraint named: "+constraintName);
			}
			
			for (String key: lhs_map.keySet()){
				
				lhs =  lhs + " + " + lhs_map.get(key) + " " + key ;
				
			}
			

			double lb = constraintRhs.get(constraintName)[0];
			double ub = constraintRhs.get(constraintName)[1];
			
			String lbStr = "constraint lb error";
			String ubStr = "constraint ub error";
			
			if ( lb < -Param.inf_assumed ) {
				lbStr = "";
			} else {
				lbStr = lb + " <= "; 
			}

			if ( ub > Param.inf_assumed ) {
				ubStr = "";
			} else {
				ubStr = " <= " + ub; 
			}
			
			if (lb == ub) {
				lbStr = "";
				ubStr = " = " + ub;
			}
			
			// TODO: what if unbounded
			
			out = out + constraintName + ": "+ lbStr + lhs + ubStr + ";\n";

		}
		
		return out;

	}

	public static String writeDvar(MPModel in) {
		
		String out = "/* dvar */\n";
		

		// number nonstd nonfree
		ArrayList<String> number_general = new ArrayList<String>(in.var_general);
		Collections.sort(number_general);
		
		// integer nonbinary
		ArrayList<String> integer_nonbinary = new ArrayList<String>(in.varMap_integer.keySet());
		integer_nonbinary.removeAll(in.var_int_binary);
		Collections.sort(integer_nonbinary);

		double lb = Param.inf_assumed;
		double ub = -Param.inf_assumed;
		
		String lbStr = "lb error";
		String ubStr = "ub error";
		
		
		for (String key : number_general) {
			
			lb = in.varMap_number.get(key)[0];
			ub = in.varMap_number.get(key)[1];
			
			if ( lb < -Param.inf_assumed ) {
				lbStr = "";
			} else {
				lbStr = lb + " <= "; 
			}

			if ( ub > Param.inf_assumed ) {
				ubStr = "";
			} else {
				ubStr = " <= " + ub; 
			}
			
			// TODO: what if unbounded
			
			out = out + lbStr + key + ubStr + ";\n";
		}
		
		
		

		for (String key : integer_nonbinary) {

			lb = in.varMap_integer.get(key)[0];
			ub = in.varMap_integer.get(key)[1];
			
			if ( lb < -Param.inf_assumed ) {
				lbStr = "";
			} else {
				lbStr = lb + " <= "; 
			}

			if ( ub > Param.inf_assumed ) {
				ubStr = "";
			} else {
				ubStr = " <= " + ub; 
			}
			
			// TODO: what if unbounded
			
			out = out + lbStr + key + ubStr + ";\n";
		}
			
		if (in.var_free.size() > 0) {
			out = out + "free\n";

			ArrayList<String> var_free_list = new ArrayList<String>(in.var_free);
			Collections.sort(var_free_list);
			
			for (int i = 0; i < var_free_list.size(); i++) {
				String term = var_free_list.get(i);

				if (i == 0) {
					out = out + term;
				}
				else {
					out = out + ", "+term;
				}
			}

			out = out + ";\n";
		}
		
		if (in.varMap_integer.size() > 0) {
			
			if (in.var_int_binary.size()>0) {
				
				out = out + "bin\n";
				
				ArrayList<String> var_binary_list = new ArrayList<String>(in.var_int_binary);
				Collections.sort(var_binary_list);				
				
				for (int i = 0; i < var_binary_list.size(); i++) {
					String term = var_binary_list.get(i);

					if (i == 0) {
						out = out + term;
					}
					else {
						out = out + ", "+term;
					}
				}

				out = out + ";\n";					
			
			}

			
			if (in.varMap_integer.size() - in.var_int_binary.size()>0) {
				
				out = out + "int\n";
				
				ArrayList<String> var_int_list = new ArrayList<String>(in.varMap_integer.keySet());
				var_int_list.removeAll(in.var_int_binary);
				Collections.sort(var_int_list);				
				
				for (int i = 0; i < var_int_list.size(); i++) {
					String term = var_int_list.get(i);

					if (i == 0) {
						out = out + term;
					}
					else {
						out = out + ", "+term;
					}
				}

				out = out + ";\n";					
			
			}

		}
		return out;


	}	

}
