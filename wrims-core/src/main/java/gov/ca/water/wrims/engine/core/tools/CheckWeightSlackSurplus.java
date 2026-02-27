package gov.ca.water.wrims.engine.core.tools;

import gov.ca.water.wrims.engine.core.commondata.solverdata.SolverData;
import gov.ca.water.wrims.engine.core.commondata.wresldata.WeightElement;
import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.IntDouble;
import gov.ca.water.wrims.engine.core.evaluator.EvalConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class CheckWeightSlackSurplus {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckWeightSlackSurplus.class);

    public CheckWeightSlackSurplus() {
        CopyOnWriteArrayList<String> usedWeightSlackSurplusCollection = ControlData.currModelDataSet.usedWtSlackSurplusList;
        Map<String, WeightElement> weightSlackSurplusMap = SolverData.getWeightSlackSurplusMap();

        Map<String, EvalConstraint> constraintMap = SolverData.getConstraintDataMap();
        ArrayList<String> constraintCollection = new ArrayList<String>(ControlData.currModelDataSet.gList);
        constraintCollection.retainAll(constraintMap.keySet());
        Iterator<String> constraintIterator = constraintCollection.iterator();

        while (constraintIterator.hasNext()) {
            String constraintName = constraintIterator.next();
            EvalConstraint ec = constraintMap.get(constraintName);

            Map<String, IntDouble> multipliers = ec.getEvalExpression().getMultiplier();
            Set keySet = multipliers.keySet();
            Iterator multipliersIterator = keySet.iterator();
            while (multipliersIterator.hasNext()) {
                String multiplierName = (String) multipliersIterator.next();
                if (usedWeightSlackSurplusCollection.contains(multiplierName)) {
                    usedWeightSlackSurplusCollection.remove(multiplierName);
                } else {
                    if (weightSlackSurplusMap.containsKey(multiplierName)) {
                        if (multiplierName.startsWith("surlus__") || multiplierName.startsWith("slack__")) {
                            LOGGER.atInfo()
                                  .setMessage(multiplierName + " is not in weight table!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                                  .log();
                        }
                    }
                }
            }
        }
        if (usedWeightSlackSurplusCollection.size() > 0) {
            LOGGER.atInfo()
                  .setMessage(usedWeightSlackSurplusCollection + " is not used in constraint!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
                  .log();
        }
    }
}
