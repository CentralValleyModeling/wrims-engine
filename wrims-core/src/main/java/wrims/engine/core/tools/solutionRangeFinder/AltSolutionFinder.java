package wrims.engine.core.tools.solutionRangeFinder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import wrims.engine.core.solver.mpmodel.MPModel;
import wrims.engine.core.solver.mpmodel.MPModelUtils;
import wrims.engine.core.solver.ortools.OrToolsSolver;
import wrims.engine.core.wreslplus.elements.Tools;

public class AltSolutionFinder {

	
	// required
	private String id;
	private MPModel asfModel = null;
	private List<String> searchVars; // will be modified (reduced) in the searching process ..
	private int searchObjSign; // 1 for + and -1 for -
	private ArrayList<String> reportVars; // only report these vars 
	
	
	
	// optional
	private boolean lpsLogging = false;
	private String lpsDir = "";
	private String lpsFileNamePrepend = "";
	
	
	public AltSolutionFinder(String id, MPModel inModel, List<String> searchVars, int searchObjSign, ArrayList<String> reportVars) {
		
		this.asfModel = inModel;
		this.id = id;
		this.searchVars = searchVars;
		this.searchObjSign = searchObjSign;
		this.reportVars = reportVars;

	}


	// optional. This is for lps file logging. if not set then no logging.
	public void setLpsLoggingPath(String dirName, String lpsFileNamePrepend){
		
		this.lpsLogging = true;
		this.lpsDir = dirName;
		this.lpsFileNamePrepend = lpsFileNamePrepend+"_"+id;
		
	}

	public ArrayList<LinkedHashMap<String, Double>> go() {

		OrToolsSolver sSolver = new OrToolsSolver("CBC_MIXED_INTEGER_PROGRAMMING");
		sSolver.setModel(asfModel);
		
		ArrayList<LinkedHashMap<String, Double>> altSolutions = new ArrayList<LinkedHashMap<String, Double>>();

		int i = 0;
		
		for ( String sv: searchVars) {
			i++;
			System.out.println(id+" seach: "+i+": "+sv);
									
			LinkedHashMap<String, Double> searchObjFunc = new LinkedHashMap<String, Double>();
			searchObjFunc.put(sv, (double)searchObjSign);
			double searchObjOffset = asfModel.solution.get(sv)*searchObjSign;
						
			
			System.out.println("Var to search:  "+sv);

			System.out.println("searchObjFunc: "+searchObjFunc);
			System.out.println("searchObjOffset: "+searchObjOffset);
			
			sSolver.refreshObjFunc(searchObjFunc);
			
			// log search model
			
			//TODO: sSolver.model searchObjFunc is wrong
			if (DetectorParam.lpsLogging) MPModelUtils.toLpSolve(sSolver.model, lpsDir, lpsFileNamePrepend+"_search_"+i+".lps");
	
			// TODO: if solve fail then need to skip current obj function and continue to next search
			// TODO: need to log solver fail for that specific variable
			if (sSolver.solve()!=0) {
				System.err.println("# Error ... no optimal solution for search variable: "+sv);
				if (DetectorParam.continueOnErrors) continue;
			}

			System.out.println("obj value: "+sSolver.solver.objectiveValue());
			if (DetectorParam.showSolutionInConsole) System.out.println("solution: "+sSolver.solution);

			// TODO: simplify this
			boolean hasNewObjValue = false;

			
			if (searchObjSign>0) {
				hasNewObjValue = sSolver.solver.objectiveValue() > searchObjOffset;
			} else if (searchObjSign<0) {
				hasNewObjValue = sSolver.solver.objectiveValue() * searchObjSign < searchObjOffset * searchObjSign;
			}
			
			System.out.println("New solution found? "+hasNewObjValue);	
			
			if (hasNewObjValue) {
				
				// post new solution
				
				LinkedHashMap<String, Double> report_solution = new LinkedHashMap<String, Double>(sSolver.solution);
				Tools.mapRetainAll(report_solution, reportVars);
				altSolutions.add(report_solution);
				
			} 	
			
		}
		
		sSolver.delete();
		sSolver = null;
		return altSolutions;
		
	}


	public static double createObjFunc(LinkedHashMap<String, Double> out_searchObjFunc, ArrayList<String> vars, int searchObjSign, LinkedHashMap<String, Double> baseSolution){
	
	
		double out_searchObjOffset = 0;
		
		for (String key: vars){
			
			out_searchObjFunc.put(key, (double)searchObjSign);
			out_searchObjOffset = out_searchObjOffset + baseSolution.get(key)*searchObjSign;
			
		}
		
		return out_searchObjOffset;
			
	}	
	
}
