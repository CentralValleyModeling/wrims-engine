package wrims.engine.core.tools.solutionRangeFinder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import wrims.engine.core.commondata.wresldata.Param;
import wrims.engine.core.solver.mpmodel.MPModelUtils;
import wrims.engine.core.solver.mpmodel.MPModel;
import wrims.engine.core.solver.ortools.OrToolsSolver;
import wrims.engine.core.wreslplus.elements.Tools;

public class DetectorWorkflow {

	private MPModel originalModel = null;
	// private String mpmodelDir = null;
	// private String mpmodelFile_withoutExt = "";

	private MPModel baseModel = null;

	private boolean isOriginalValidated;
	private boolean isBaseValidated;

	private LinkedHashMap<String, double[]> varsRange_output;

	public DetectorWorkflow(MPModel mpm) {

		this.isOriginalValidated = false;
		this.isBaseValidated = false;
		this.varsRange_output = new LinkedHashMap<String, double[]>();

		originalModel = new MPModel(mpm, "original");

	}

	public DetectorWorkflow(String mpmPath) {

		this.isOriginalValidated = false;
		this.isBaseValidated = false;
		this.varsRange_output = new LinkedHashMap<String, double[]>();

		try {
			originalModel = MPModelUtils.load(mpmPath);

			// File f = new File(mpmPath);
			// mpmodelFile_withoutExt =
			// FilenameUtils.removeExtension(f.getName());
			// mpmodelDir = new File(f.getParent()).getAbsolutePath();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public LinkedHashMap<String, double[]> getVarsRange() {

		return varsRange_output;
	}

	protected boolean validateOriginalModel() {

		OrToolsSolver aSolver = new OrToolsSolver("CBC_MIXED_INTEGER_PROGRAMMING");
		aSolver.setVerbose(1);

		// log original model
		if (DetectorParam.lpsLogging)
			MPModelUtils.toLpSolve(originalModel, DetectorParam.lpsDir, DetectorParam.lpsFileNamePrepend
					+ "_original.lps");
		// solve original model
		System.out.println("solve original model...");
		int originalModelReturnCode = aSolver.solve(originalModel);
		if (originalModelReturnCode != 0) {
			aSolver.delete();
			isOriginalValidated = false;
			return isOriginalValidated;
		} else {
			System.out.println("solving success for original model!");
			isOriginalValidated = true;
		}

		originalModel.solution = aSolver.solution;
		originalModel.objValue = aSolver.solver.objectiveValue();
		// System.out.println("Original:   "+originalModel.solution);

		aSolver.delete();
		return isOriginalValidated;

	}

//	protected boolean validateBaseModel() {
//		
//		return validateBaseModel(DetectorParam.obj_constraint_relax_ratio);
//		
//	}
	
	protected boolean validateBaseModel(double obj_constraint_relax_ratio) {

		if (!isOriginalValidated)
			return false;

		OrToolsSolver aSolver = new OrToolsSolver("CBC_MIXED_INTEGER_PROGRAMMING");
		aSolver.setVerbose(1);

		// base model
		baseModel = new MPModel(originalModel, "base");
		baseModel.createConstraint("OBJ_CONSTRAINT", originalModel.objFunction, originalModel.objValue
				* (1.0 - obj_constraint_relax_ratio), Param.inf);

		if (DetectorParam.lpsLogging)
			MPModelUtils.toLpSolve(baseModel, DetectorParam.lpsDir, DetectorParam.lpsFileNamePrepend + "_base.lps");

		System.out.println("solve base model...");
		int baseModelReturnCode = aSolver.solve(baseModel);
		if (baseModelReturnCode != 0) {
			aSolver.delete();
			isBaseValidated = false;
			return isBaseValidated;
		} else {
			System.out.println("solving success for base model!");
		}

		baseModel.solution = aSolver.solution;
		baseModel.objValue = aSolver.solver.objectiveValue();
		// System.out.println("SearchBase: "+baseModel.solution);

		aSolver.delete();

		isBaseValidated = true;
		return isBaseValidated;

	}

	public int findAltSolutions(ArrayList<String> searchVarList) {

		// return code
		// -1: model not validated
		// -2: other errors
		// 0: no alt solution found
		// 1: alt solution found

		if (!(isBaseValidated && isOriginalValidated))
			return DetectorParam.modelNotValid;

		// use all vars in solution to search if null
		if (searchVarList == null)
			searchVarList = new ArrayList<String>(baseModel.solution.keySet());

		// find vars that equals its own bound
		VarsGroup varsGroup = new VarsGroup(baseModel, DetectorParam.vertax_tolerance);
		varsGroup.setVarsPool(searchVarList);

		// TODO: this should be done before calling detect() method
		varsGroup.filterVarsPool(DetectorParam.includeStrings, DetectorParam.excludeStrings);

		varsGroup.findVarsAtVertex();

		// TODO: find vars that equals its own upper bound
		// TODO: find vars not at either bound

		// initialize var range using original and base solutions

		ArrayList<String> all_vars_to_report = varsGroup.varsPool;

		LinkedHashMap<String, Double> reportBaseSolution = new LinkedHashMap<String, Double>(baseModel.solution);
		Tools.mapRetainAll(reportBaseSolution, all_vars_to_report);

		LinkedHashMap<String, Double> reportOriginalSolution = new LinkedHashMap<String, Double>(originalModel.solution);
		Tools.mapRetainAll(reportOriginalSolution, all_vars_to_report);

		ArrayList<LinkedHashMap<String, Double>> group0Solutions = new ArrayList<LinkedHashMap<String, Double>>();
		group0Solutions.add(reportBaseSolution);
		group0Solutions.add(reportOriginalSolution);

		LinkedHashMap<String, double[]> varRange_group0 = Misc.findVarsRange(group0Solutions);
		List<LinkedHashMap<String, double[]>> varRange_allGroups = new ArrayList<LinkedHashMap<String, double[]>>();
		varRange_allGroups.add(varRange_group0);

		// setup vars to search and vars to report

		// List<String> all_vars_to_search = new ArrayList<String>();

		// quick search. might miss some.
		// all_vars_to_search.addAll(varsGroup.lowerVertexVars_integer);
		// all_vars_to_search.addAll(varsGroup.lowerVertexVars_number);

		if (DetectorParam.searchLevel != 2)
			return DetectorParam.otherErrors; // not integrated

		// all_vars_to_search = varsGroup.varsPool;
		//
		// System.out.println("all_vars_to_search:" + all_vars_to_search);

		//List<String> g1searchVars = new ArrayList<String>(varsGroup.notUpperVertexVars_number.subList(0, 3));
		//List<String> g2searchVars = new ArrayList<String>(varsGroup.notLowerVertexVars_number.subList(0, 3));
		List<String> g1searchVars = new ArrayList<String>(varsGroup.notUpperVertexVars_number);
		List<String> g2searchVars = new ArrayList<String>(varsGroup.notLowerVertexVars_number);
		
		// TODO: parallel threads for multiple solver instances

		// ================ begin group 1 search ==============

		// positive search
		// input
		int searchObjSign1 = 1; // for lower vertex vars

		// return
		LinkedHashMap<String, double[]> varRange_group1 = new LinkedHashMap<String, double[]>();

		AltSolutionFinder altFinder1 = new AltSolutionFinder("group1", baseModel, g1searchVars, searchObjSign1,
				all_vars_to_report);
		if (DetectorParam.lpsLogging)
			altFinder1.setLpsLoggingPath(DetectorParam.lpsDir, DetectorParam.lpsFileNamePrepend);
		ArrayList<LinkedHashMap<String, Double>> group1Solutions = altFinder1.go();

		// find solution ranges
		if (group1Solutions.size() == 0) {
			System.out.println(" no alt solution found in group1");
		} else {
			System.out.println(" alt solution found in group1");
			varRange_group1 = Misc.findVarsRange(group1Solutions);
		}

		// returns varRange_group*
		if (varRange_group1.size() > 0) {
			varRange_allGroups.add(varRange_group1);
		}
		// ============= end group 1 search ==============

		System.err.println("begin group2");
		// ================ begin group 2 search ==============

		// negative search
		// input
		int searchObjSign2 = -1; // for upper vertex vars

		// return
		LinkedHashMap<String, double[]> varRange_group2 = new LinkedHashMap<String, double[]>();

		AltSolutionFinder altFinder2 = new AltSolutionFinder("group2", baseModel, g2searchVars, searchObjSign2,
				all_vars_to_report);
		if (DetectorParam.lpsLogging)
			altFinder2.setLpsLoggingPath(DetectorParam.lpsDir, DetectorParam.lpsFileNamePrepend);
		ArrayList<LinkedHashMap<String, Double>> group2Solutions = altFinder2.go();

		// find solution ranges
		if (group2Solutions.size() == 0) {
			System.out.println(" no alt solution found in group2");
		} else {
			varRange_group2 = Misc.findVarsRange(group2Solutions);
		}
		if (varRange_group2.size() > 0) {
			System.out.println(" alt solution found in group2");
			varRange_allGroups.add(varRange_group2);
		}
		// ============= end group 2 search ==============

		// update ranges
		LinkedHashMap<String, double[]> varRange_all = null;

		if (varRange_allGroups.size() > 0) {
			System.out.println("update vars range...");
			varRange_all = Misc.updateVarsRange(varRange_allGroups);
		} else {
			return DetectorParam.altSolutionNotFound; // no alt solutions found
		}

		// report only vars that have big difference
		varsRange_output = Misc.filterVarsRange(varRange_all, DetectorParam.nonunique_min_abs_diff, DetectorParam.nonunique_min_ratio_diff);

		// write report
		// Misc.writeReport(varsRange_output, mpmodelDir,
		// mpmodelFile_withoutExt);

		return DetectorParam.altSolutionFound; // alt solutions found

	}

}
