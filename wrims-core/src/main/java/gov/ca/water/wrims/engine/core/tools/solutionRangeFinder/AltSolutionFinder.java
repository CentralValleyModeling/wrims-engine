package gov.ca.water.wrims.engine.core.tools.solutionRangeFinder;

import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModel;
import gov.ca.water.wrims.engine.core.solver.mpmodel.MPModelUtils;
import gov.ca.water.wrims.engine.core.solver.ortools.OrToolsSolver;
import gov.ca.water.wrims.engine.core.wreslplus.elements.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class AltSolutionFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AltSolutionFinder.class);

    // required
    private final String id;
    private MPModel asfModel = null;
    private final List<String> searchVars; // will be modified (reduced) in the searching process ..
    private final int searchObjSign; // 1 for + and -1 for -
    private final ArrayList<String> reportVars; // only report these vars

    // optional
    private boolean lpsLogging = false;
    private String lpsDir = "";
    private String lpsFileNamePrepend = "";

    public AltSolutionFinder(
            String id,
            MPModel inModel,
            List<String> searchVars,
            int searchObjSign,
            ArrayList<String> reportVars
    ) {

        this.asfModel = inModel;
        this.id = id;
        this.searchVars = searchVars;
        this.searchObjSign = searchObjSign;
        this.reportVars = reportVars;

    }

    public static double createObjFunc(
            LinkedHashMap<String, Double> out_searchObjFunc,
            ArrayList<String> vars,
            int searchObjSign,
            LinkedHashMap<String, Double> baseSolution
    ) {

        double out_searchObjOffset = 0;

        for (String key : vars) {

            out_searchObjFunc.put(key, (double) searchObjSign);
            out_searchObjOffset = out_searchObjOffset + baseSolution.get(key) * searchObjSign;

        }

        return out_searchObjOffset;

    }

    // optional. This is for lps file logging. if not set then no logging.
    public void setLpsLoggingPath(String dirName, String lpsFileNamePrepend) {

        this.lpsLogging = true;
        this.lpsDir = dirName;
        this.lpsFileNamePrepend = lpsFileNamePrepend + "_" + id;

    }

    public ArrayList<LinkedHashMap<String, Double>> go() {

        OrToolsSolver sSolver = new OrToolsSolver("CBC_MIXED_INTEGER_PROGRAMMING");
        sSolver.setModel(asfModel);

        ArrayList<LinkedHashMap<String, Double>> altSolutions = new ArrayList<LinkedHashMap<String, Double>>();

        int i = 0;

        for (String sv : searchVars) {
            i++;
            LOGGER.atInfo().setMessage(id + " seach: " + i + ": " + sv).log();

            LinkedHashMap<String, Double> searchObjFunc = new LinkedHashMap<String, Double>();
            searchObjFunc.put(sv, (double) searchObjSign);
            double searchObjOffset = asfModel.solution.get(sv) * searchObjSign;

            LOGGER.atInfo().setMessage("Var to search:  " + sv).log();

            LOGGER.atInfo().setMessage("searchObjFunc: " + searchObjFunc).log();
            LOGGER.atInfo().setMessage("searchObjOffset: " + searchObjOffset).log();

            sSolver.refreshObjFunc(searchObjFunc);

            // log search model

            //TODO: sSolver.model searchObjFunc is wrong
            if (DetectorParam.lpsLogging)
                MPModelUtils.toLpSolve(sSolver.model, lpsDir, lpsFileNamePrepend + "_search_" + i + ".lps");

            // TODO: if solve fail then need to skip current obj function and continue to next search
            // TODO: need to log solver fail for that specific variable
            if (sSolver.solve() != 0) {
                LOGGER.atError().setMessage("# Error ... no optimal solution for search variable: " + sv).log();
                if (DetectorParam.continueOnErrors) continue;
            }

            LOGGER.atInfo().setMessage("obj value: " + sSolver.solver.objectiveValue()).log();
            if (DetectorParam.showSolutionInConsole) LOGGER.atInfo().setMessage("solution: " + sSolver.solution).log();

            // TODO: simplify this
            boolean hasNewObjValue = false;

            if (searchObjSign > 0) {
                hasNewObjValue = sSolver.solver.objectiveValue() > searchObjOffset;
            } else if (searchObjSign < 0) {
                hasNewObjValue = sSolver.solver.objectiveValue() * searchObjSign < searchObjOffset * searchObjSign;
            }

            LOGGER.atInfo().setMessage("New solution found? " + hasNewObjValue).log();

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

}
