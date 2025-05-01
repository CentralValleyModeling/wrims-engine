package wrimsv2.tools.solutionRangeFinder;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.apache.commons.io.FilenameUtils;

import wrimsv2.solver.mpmodel.MPModel;
import wrimsv2.solver.mpmodel.MPModelUtils;
import wrimsv2.solver.ortools.OrToolsSolver;

public class Detector {

	private DetectorWorkflow dwf = null;
	
	public static void main(String[] args) throws Exception {

				
		OrToolsSolver.initialize();

		
		String mpmPath = "examples\\CL_SharingFix_Shortage_121212\\Run\\=ILP=\\Existing_BO_121212_CBC.config\\mpmodel\\\\1921_10_c01.mpm";

		DetectorParam.lpsLogging = true;
		File f = new File(mpmPath);
		DetectorParam.lpsFileNamePrepend = FilenameUtils.removeExtension(f.getName());
		DetectorParam.lpsDir = new File(f.getParent()).getAbsolutePath();
		
		MPModel mpm = null;
		
		try {
			mpm = MPModelUtils.load(mpmPath);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Detector d = new Detector(mpm);
		
		d.validateBase();
		
		LinkedHashMap<String, double[]> varsRange = d.detect(DetectorParam.searchVarList);
		
		// write report
		if (varsRange!=null && varsRange.size()>0) {
			String reportPath = FilenameUtils.removeExtension(mpmPath) + "_variable_range.csv";
			PrintWriter rp = Misc.openReportFile(reportPath);
			Misc.writeReport(varsRange, rp);
			rp.close();
		} else {
			
			System.out.println("no alternative solution detected.");
		}

		// detect("examples\\simple1\\run\\=ILP=\\simple1_cbc.config\\mpmodel\\1921_10_c01.mpm");

	}

	
	public Detector(MPModel mpm) {

		if (mpm==null) System.err.println("# Error: original model is not valid (mpm==null).");
		
		dwf = new DetectorWorkflow(mpm);

		boolean OK_original = dwf.validateOriginalModel();
		
		if (!OK_original) System.err.println("# Error: original model is not valid (!OK_original).");

	}

	public void validateBase() {

		validateBase(DetectorParam.obj_constraint_relax_ratio);

	}
	
	public void validateBase(double obj_constraint_relax_ratio) {

		
		boolean OK_base = dwf.validateBaseModel(obj_constraint_relax_ratio);
		
		if (!OK_base) System.err.println("# Error: base model is not valid.");

	}

	public LinkedHashMap<String, double[]> detect(ArrayList<String> searchVarList) {


		LinkedHashMap<String, double[]> varsRange = null;

		if (dwf.findAltSolutions(searchVarList) == DetectorParam.altSolutionFound) {
			varsRange = dwf.getVarsRange();
		}
		
		return varsRange;
	}
	
//	public LinkedHashMap<String, double[]> detect(MPModel mpm, ArrayList<String> searchVarList) {
//
//		if (mpm==null) return null;
//		
//		DetectorWorkflow dw = new DetectorWorkflow(mpm);
//
//		boolean OK_original = dw.validateOriginalModel();
//		boolean OK_base = dw.validateBaseModel();
//
//		LinkedHashMap<String, double[]> varsRange = null;
//
//		if (dw.findAltSolutions(searchVarList) == DetectorParam.altSolutionFound) {
//			varsRange = dw.getVarsRange();
//		}
//		
//		return varsRange;
//	}	
//	
//	
//	public LinkedHashMap<String, double[]> detect(String mpmPath, ArrayList<String> searchVarList) {
//
//		MPModel mpm = null;
//		
//		try {
//			mpm = MPModelUtils.load(mpmPath);
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		
//		return detect(mpm, searchVarList);
//	}

}
