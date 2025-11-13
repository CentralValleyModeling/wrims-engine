
package wrims.engine.core.tools.solutionRangeFinder;

import java.util.ArrayList;


public class DetectorParam {

	// constant
	public static final int altSolutionFound = 1;
	public static final int altSolutionNotFound = 0;	
	public static final int modelNotValid = -1;
	public static final int otherErrors = -2;

	/// user input
	// required
	public static double nonunique_min_abs_diff = 10.0;     // minimum difference for reporting solution range 
	public static double nonunique_min_ratio_diff = 0.2;     // minimum difference ratio for reporting solution range. nonunique_min_ratio_diff > difference/max(larger, 1.0)
	public static double vertax_tolerance = 0.001;  // tolerance for determining if a var is at vertex
	public static double obj_constraint_relax_ratio = 0.00001; // relax constraint by this ratio to avoid solution infeasible 
	
	public static int searchLevel = 2;  
	public static boolean continueOnErrors = false;  
	public static boolean showSolutionInConsole = false; 
	// 1: max for vars at lowerbound. need to scan all vars in solution to get better result. 
	// 2: max and min for vars at bound. default.  
	// 3: max and min for vars at or not at bound. best result.   
	
	
	// optional
	public static ArrayList<String> searchVarList = null;
	public static ArrayList<String> excludeStrings = null;
	public static ArrayList<String> includeStrings = null;
	
	public static boolean lpsLogging = false;
	public static String lpsDir = "";
	public static String lpsFileNamePrepend = "";
	
	
}
