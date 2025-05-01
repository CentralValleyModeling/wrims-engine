
package wrimsv2.tools.solutionRangeFinder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import wrimsv2.wreslplus.elements.Tools;


public class Misc {

	static PrintWriter openReportFile(String filePath) {
		
		File f = new File(filePath);
		String file = f.getName();
		String dir = new File(f.getParent()).getAbsolutePath();
		
		PrintWriter reportFile = null;
		
		try {
			reportFile = Tools.openFile(dir, file);
			
			reportFile.println("VariableName,LowerBound,UpperBound,Difference,PercentageDifference");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return reportFile;
		
	}

	
//	protected static void writeReport(LinkedHashMap<String, double[]> varRange, String filePath) {
//		
//		File f = new File(filePath);
//		String file = f.getName();
//		String dir = new File(f.getParent()).getAbsolutePath();
//		
//		writeReport(varRange, dir, file);
//		
//	}

	protected static void writeReport(LinkedHashMap<String, double[]> varRange, PrintWriter reportFile) {
		


			
			for (String varName: varRange.keySet()){
				
				double[] range = varRange.get(varName);
				double diff = range[1] - range[0];
				double diff_percent = 100*diff/Math.max(Math.abs(range[1]), Math.abs(range[0]));
				
				// for excel view
				reportFile.println(varName+","+(float)range[0] + ","+(float)range[1]+ ","+(float)diff+ ","+(float)diff_percent+"%");
				reportFile.flush();
			
			
			}
			

		
	}
	
	protected static void writeReport(LinkedHashMap<String, double[]> varRange, String dir, String fileName) {
		
		try {
			PrintWriter reportFile = Tools.openFile(dir, fileName);
			
			reportFile.println("VariableName,LowerBound,UpperBound,Difference,PercentageDifference");
			
			for (String varName: varRange.keySet()){
				
				double[] range = varRange.get(varName);
				double diff = range[1] - range[0];
				double diff_percent = 100*diff/Math.max(Math.abs(range[1]), Math.abs(range[0]));
				
				// for excel view
				reportFile.println(varName+","+(float)range[0] + ","+(float)range[1]+ ","+(float)diff+ ","+(float)diff_percent+"%");
				reportFile.flush();
			}
			
			reportFile.close();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}

	protected static LinkedHashMap<String, double[]> filterVarsRange(LinkedHashMap<String, double[]> varsRange, double min_abs_diff, double min_ratio_diff) {
		
		LinkedHashMap<String, double[]> out = new LinkedHashMap<String, double[]>();
		
		
		for (String varName: varsRange.keySet()){
			
			double lower = varsRange.get(varName)[0];
			double upper = varsRange.get(varName)[1];
			double absMax = Math.max(Math.abs(lower), Math.abs(upper)); 
			
			double difference = Math.abs(upper-lower);
			
			boolean check1 = difference > min_abs_diff;
			
			boolean check2 = difference / Math.max(absMax, 1.0) > min_ratio_diff;  // assign 1.0 in case both lower and uppers are zero.
			
			if ( check1 && check2 ) out.put(varName, new double[]{lower, upper});
			//if ( (upper!=0) && ( (upper-lower)/upper > min_abs_diff ) ) out.put(varName, new double[]{lower, upper});
			


			
		}
		
		return out;		
		
	}

	protected static LinkedHashMap<String, double[]> findVarsRange(ArrayList<LinkedHashMap<String, Double>> allSearchSolutions) {
		
		
		// initialize		
		LinkedHashMap<String, Double> varUpper = new LinkedHashMap<String, Double>(allSearchSolutions.get(0));
		LinkedHashMap<String, Double> varLower = new LinkedHashMap<String, Double>(allSearchSolutions.get(0));
		
		
		for (LinkedHashMap<String, Double> sol : allSearchSolutions){
			
			for (String varName: sol.keySet()){
				if (sol.get(varName) > varUpper.get(varName) ) varUpper.put(varName, sol.get(varName)); 
				if (sol.get(varName) < varLower.get(varName) ) varLower.put(varName, sol.get(varName)); 
			}
		}
		
		

		LinkedHashMap<String, double[]> varRange = new LinkedHashMap<String, double[]>();
		
		for (String varName: allSearchSolutions.get(0).keySet()){
			
			double upper = varUpper.get(varName);
			double lower = varLower.get(varName);
			
			varRange.put(varName, new double[]{lower, upper});
			
		}
		
		return varRange;		
		
	}

	protected static LinkedHashMap<String, double[]> updateVarsRange(List<LinkedHashMap<String, double[]>> varsRangeList) {
		
		LinkedHashMap<String, double[]> out = new LinkedHashMap<String, double[]>(varsRangeList.get(0));
		List<String> keySet = new ArrayList<String>(out.keySet());
		
		for (LinkedHashMap<String, double[]> varsRange : varsRangeList) {

			for (String key : keySet) {

				double upper = out.get(key)[1];
				double lower = out.get(key)[0];

				if (varsRange.get(key)[0] < lower)
					lower = varsRange.get(key)[0];
				if (varsRange.get(key)[1] > upper)
					upper = varsRange.get(key)[1];

				out.put(key, new double[] { lower, upper });

			}
		}
		return out;		
		
	}	
	
	protected static LinkedHashMap<String, double[]> updateVarsRange(LinkedHashMap<String, double[]> varsRange1, LinkedHashMap<String, double[]> varsRange2) {
		
		LinkedHashMap<String, double[]> out = new LinkedHashMap<String, double[]>(varsRange1);
		
		
		for (String key: varsRange1.keySet()){
			
			double upper = varsRange1.get(key)[1];
			double lower = varsRange1.get(key)[0];
			
			if (varsRange2.get(key)[0] < lower) lower = varsRange2.get(key)[0];
			if (varsRange2.get(key)[1] > upper) upper = varsRange2.get(key)[1];
			
			out.put(key, new double[]{lower, upper});
				
		}
		
		return out;		
		
	}
}
