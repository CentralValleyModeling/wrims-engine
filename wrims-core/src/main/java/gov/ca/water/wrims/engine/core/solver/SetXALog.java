package gov.ca.water.wrims.engine.core.solver;

import gov.ca.water.wrims.engine.core.components.ControlData;
import gov.ca.water.wrims.engine.core.components.FilePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SetXALog {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetXALog.class);
    static boolean isConfigRead = false;
    static String line = "set sortName YES MPSX Yes Set FreqLog :01";

    public static void enableXALog() {
        File config = new File("xa_config.dat");
        if (config.exists() && !isConfigRead) {
            BufferedReader br;
            try {
                FileReader fr = new FileReader(config);
                br = new BufferedReader(fr);
                line = br.readLine();
                isConfigRead = true;
                LOGGER.atInfo().setMessage("Retrieve XA configuation from xa_config.dat file").log();
                ControlData.xasolver.setCommand("set debug no ToRcc Yes FileName  " + FilePaths.mainDirectory + "  Output " + FilePaths.mainDirectory + "\\xa.log " + line);
                br.close();
                fr.close();
            } catch (Exception e) {
                e.printStackTrace();
                ControlData.xasolver.setCommand("set debug no ToRcc Yes FileName  " + FilePaths.mainDirectory + "  Output " + FilePaths.mainDirectory + "\\xa.log set sortName YES MPSX Yes Set FreqLog :01");
            }
        } else {
            ControlData.xasolver.setCommand("set debug no ToRcc Yes FileName  " + FilePaths.mainDirectory + "  Output " + FilePaths.mainDirectory + "\\xa.log " + line);
            //ControlData.xasolver.setCommand("When +10920 FileName d:\\xatemp\\xa%d ToRcc Yes Output xa%d set sortName YES MPSX Yes");
        }
    }

    public static void disableXALog() {
        ControlData.xasolver.setCommand("set debug no");
    }
}
