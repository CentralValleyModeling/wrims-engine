package gov.ca.water.wrims.engine.core.tools.solutionRangeFinder;

import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModel;
import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModelUtils;
import gov.ca.water.wrims.engine.core.solver.ortools.OrToolsSolver;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        String searchVarsFilePath = "D:\\search_vars.txt";
        String mpmPath_prepend = "D:\\cvwrsm\\trunk\\wrims_v2\\wrims_v2\\examples\\CL_SharingFix_Shortage_121212\\Run\\=ILP=\\Existing_BO_121212_CBC.config\\mpmodel\\";

        String date = "1932_10";
        String mpmPath = mpmPath_prepend + date + "_c03.mpm";
        DetectorParam.nonunique_min_abs_diff = 0.5; // minimum difference for
        // reporting solution range
        DetectorParam.nonunique_min_ratio_diff = 0.04;
        DetectorParam.obj_constraint_relax_ratio = 100. / Math.pow(10, 9); //part per billion
        DetectorParam.continueOnErrors = true;
        DetectorParam.lpsLogging = false; // TODO: this option has error

        ArrayList<String> variablesList = new ArrayList<String>();

        BufferedReader reader;
        if (searchVarsFilePath.length() > 0) {
            try {
                reader = new BufferedReader(new FileReader(searchVarsFilePath));

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    String[] vars = line.split(",");
                    Collections.addAll(variablesList, vars);
                }
                DetectorParam.searchVarList = variablesList;
                LOGGER.atInfo().setMessage("{}").addArgument(variablesList).log();

            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();

            }
        }

        OrToolsSolver.initialize();

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

    }

}
