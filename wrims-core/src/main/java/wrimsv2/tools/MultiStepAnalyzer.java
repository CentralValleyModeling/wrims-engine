package wrimsv2.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import wrimsv2.commondata.solverdata.SolverData;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.components.IntDouble;
import wrimsv2.evaluator.EvalConstraint;
import wrimsv2.evaluator.EvalExpression;

public class MultiStepAnalyzer {
	private Map<String, EvalConstraint> constraintMap;
	private int fammonths=4;
	private BufferedWriter out;
	private ArrayList<String> sortedConstraintKeys;
	private int ifut;

	public MultiStepAnalyzer(){
		constraintMap=SolverData.getConstraintDataMap();
		Set constraintKeys=constraintMap.keySet();
	    sortedConstraintKeys = new ArrayList<String>(constraintKeys);
		Collections.sort(sortedConstraintKeys);
		
		for (ifut=0; ifut<=fammonths; ifut++){
			new File(FilePaths.mainDirectory+"=MultiStepAnalyzer=").mkdir();
			String outPath=FilePaths.mainDirectory+"=MultiStepAnalyzer=\\"+ControlData.currYear+"_"+ControlData.currMonth+"_"+ControlData.currCycleIndex+"_"+"fut"+ifut+".txt";
			FileWriter outstream;
			try {
				outstream = new FileWriter(outPath);
				out = new BufferedWriter(outstream);
				
				analyzeConstraints();				
				
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void analyzeConstraints(){
		for (int j=0; j<sortedConstraintKeys.size(); j++){
			String constraintKey=sortedConstraintKeys.get(j);
			EvalConstraint ec=constraintMap.get(constraintKey);
			if (ifut==0){
				if (!constraintKey.contains("__fut__")){
					analyzeOneConstraint(constraintKey, ec);
				}
			}else{
				String tokens="__fut__"+ifut;
				if (constraintKey.endsWith(tokens)){
					analyzeOneConstraint(constraintKey, ec);
				}
			}
		}
	}
	
	private void analyzeOneConstraint(String constraintKey, EvalConstraint ec){
		String[] procConstraintKey=constraintKey.split("__fut__");
		String line=procConstraintKey[0]+": ";
		EvalExpression ee=ec.getEvalExpression();
		HashMap<String, IntDouble> multipliers = ee.getMultiplier();
		Set<String> multiplierKeys = multipliers.keySet();
		ArrayList<String> sortedMultiKeys = new ArrayList(multiplierKeys);
		Collections.sort(sortedMultiKeys);
		for (int k=0; k<sortedMultiKeys.size(); k++){
			String variable=sortedMultiKeys.get(k);
			double value=multipliers.get(variable).getData().doubleValue();
			String procVariable=processVariable(variable);
			if (value>=0){
				line=line+"+"+value+"*"+procVariable;
			}else{
				line=line+value+"*"+procVariable;
			}
		}
		line=line+ec.getSign()+(-ee.getValue().getData().doubleValue())+"\n";
		try {
			out.write(line);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String processVariable(String variable){
		if (variable.contains("__fut__")){
			String[] procVariable=variable.split("__fut__");
			int timeStep=Integer.parseInt(procVariable[1])-ifut;
			return procVariable[0]+"("+timeStep+")";
		}else{
			return variable;
		}
	}
}
