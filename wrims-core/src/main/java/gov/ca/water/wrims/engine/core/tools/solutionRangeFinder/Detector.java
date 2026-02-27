package gov.ca.water.wrims.engine.core.tools.solutionRangeFinder;

import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModel;
import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModelUtils;
import gov.ca.water.wrims.engine.core.solver.ortools.OrToolsSolver;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class Detector {
    private static final Logger LOGGER = LoggerFactory.getLogger(Detector.class);
    private DetectorWorkflow dwf = null;

    public Detector(MPModel mpm) {

        if (mpm == null) LOGGER.atError().setMessage("# Error: original model is not valid (mpm==null).").log();

        dwf = new DetectorWorkflow(mpm);

        boolean OK_original = dwf.validateOriginalModel();

        if (!OK_original) LOGGER.atError().setMessage("# Error: original model is not valid (!OK_original).").log();

    }

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
        if (varsRange != null && varsRange.size() > 0) {
            String reportPath = FilenameUtils.removeExtension(mpmPath) + "_variable_range.csv";
            PrintWriter rp = Misc.openReportFile(reportPath);
            Misc.writeReport(varsRange, rp);
            rp.close();
        } else {

            LOGGER.atInfo().setMessage("no alternative solution detected.").log();
        }

        // detect("examples\\simple1\\run\\=ILP=\\simple1_cbc.config\\mpmodel\\1921_10_c01.mpm");

    }

    public void validateBase() {

        validateBase(DetectorParam.obj_constraint_relax_ratio);

    }

    public LinkedHashMap<String, double[]> detect(ArrayList<String> searchVarList) {

        LinkedHashMap<String, double[]> varsRange = null;

        if (dwf.findAltSolutions(searchVarList) == DetectorParam.altSolutionFound) {
            varsRange = dwf.getVarsRange();
        }

        return varsRange;
    }

    public void validateBase(double obj_constraint_relax_ratio) {

        boolean OK_base = dwf.validateBaseModel(obj_constraint_relax_ratio);

        if (!OK_base) LOGGER.atError().setMessage("# Error: base model is not valid.").log();

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
