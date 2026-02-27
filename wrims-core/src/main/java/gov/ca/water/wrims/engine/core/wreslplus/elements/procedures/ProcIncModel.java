package gov.ca.water.wrims.engine.core.wreslplus.elements.procedures;

import gov.ca.water.wrims.engine.core.wreslplus.elements.StudyTemp;
import gov.ca.water.wrims.engine.core.wreslplus.elements.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ProcIncModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcIncModel.class);

    private ProcIncModel() {
    }

    public static void findKidMap(StudyTemp st) {

        for (String f : st.modelMap.keySet()) {

            ArrayList<String> kids = st.modelMap.get(f).incModelList;

            if (kids == null) {
                st.noKid_incModel.add(f);
            } else if (kids.isEmpty()) {
                st.noKid_incModel.add(f);
            } else {
                //st.kidssMap.put(f, modelName, new HashSet<String>(kids));
                st.kidMap_incModel.put(f, new HashSet<String>(kids));
            }

        }

        LOGGER.atInfo().setMessage("st.kidMap_incModel" + st.kidMap_incModel).log();

    }

    public static void findAllOffSpring(StudyTemp st) {

        for (String f : st.kidMap_incModel.keySet()) {

            HashSet<String> a = Tools.findAllOffspring(f, st.kidMap_incModel);

            st.allOffspringMap_incModel.put(f, a);

        }

        LOGGER.atInfo().setMessage("st.allOffspringMap_incModel" + st.allOffspringMap_incModel).log();
    }

    public static void findFileGroupOrder(StudyTemp st) {

        Map<String, HashSet<String>> toBeSorted = new HashMap<String, HashSet<String>>(st.allOffspringMap_incModel);

        st.fileGroupOrder_incModel.add(st.noKid_incModel);

        Tools.findFileHierarchy(st.fileGroupOrder_incModel, toBeSorted);

        LOGGER.atInfo().setMessage("st.fileGroupOrder_incModel" + st.fileGroupOrder_incModel).log();

    }

    public static void findEffectiveIncludeModel(StudyTemp st) {

        HashSet<String> t = new HashSet<String>();
        for (HashSet<String> e : st.fileGroupOrder_incModel) {
            t.addAll(e);
        }

        st.incModelList_effective = new ArrayList<String>(t);

    }

}
