package gov.ca.water.wrims.engine.core.components;

import gov.ca.water.wrims.engine.core.ilp.ILP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TimeUsage {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeUsage.class);
    public static Map<String, Integer> cpuTimeMap = new HashMap<>();
    public static Map<String, Integer> nCallsMap = new HashMap<>();

    public static void showTimeUsage() {
        LOGGER.atInfo().setMessage("Parse Time Usage: " + getTimeString(ControlData.t_parse)).log();
        LOGGER.atInfo().setMessage("Read Timeseries Time Usage: " + getTimeString(ControlData.t_readTs)).log();
        LOGGER.atInfo().setMessage("Process Timesereis Time Usage: " + getTimeString(ControlData.t_ts)).log();
        LOGGER.atInfo().setMessage("Process Svar Time Usage: " + getTimeString(ControlData.t_svar)).log();
        LOGGER.atInfo().setMessage("Process Dvar Time Usage: " + getTimeString(ControlData.t_dvar)).log();
        LOGGER.atInfo().setMessage("Process Constraint Time Usage: " + getTimeString(ControlData.t_goal)).log();
        LOGGER.atInfo().setMessage("Process Weight Time Usage: " + getTimeString(ControlData.t_wt)).log();
        LOGGER.atInfo()
              .setMessage("Process Weight Surplus Slack Time Usage: " + getTimeString(ControlData.t_wtss))
              .log();
        LOGGER.atInfo().setMessage("Cbc Time Usage: " + getTimeString(ControlData.t_cbc)).log();
        LOGGER.atInfo().setMessage("XA Time Usage: " + getTimeString(ControlData.t_xa)).log();
        LOGGER.atInfo().setMessage("Process Alias Time Usage: " + getTimeString(ControlData.t_as)).log();
        LOGGER.atInfo().setMessage("Write Dss Time Usage: " + getTimeString(ControlData.t_writeDss)).log();
        LOGGER.atInfo().setMessage("CAM Time Usage: " + getTimeString(ControlData.t_cam)).log();
        LOGGER.atInfo().setMessage("ANN Time Usage: " + getTimeString(ControlData.t_ann)).log();
        LOGGER.atInfo().setMessage("ANN Number of Calls: " + ControlData.n_ann).log();
        LOGGER.atInfo().setMessage("ANN EC Time Usage: " + getTimeString(ControlData.t_annec)).log();
        LOGGER.atInfo().setMessage("ANN EC Number of Calls: " + ControlData.n_annec).log();
        LOGGER.atInfo().setMessage("ANN Linegen Time Usage: " + getTimeString(ControlData.t_annlinegen)).log();
        LOGGER.atInfo().setMessage("ANN Linegen Number of Calls: " + ControlData.n_annlinegen).log();
        System.out.println("ANN EC Match DSM2 Time Usage: " + ControlData.t_annec_matchdsm2 / 60000 + "min " + Math.round(
                (ControlData.t_annec_matchdsm2 / 60000.0 - ControlData.t_annec_matchdsm2 / 60000) * 60) + "sec");
        LOGGER.atInfo().setMessage("ANN EC Match DSM2 Number of Calls: " + ControlData.n_annec_matchdsm2).log();
        LOGGER.atInfo().setMessage("ANN X2 Time Usage: " + getTimeString(ControlData.t_annx2)).log();
        LOGGER.atInfo().setMessage("ANN X2 Number of Calls: " + ControlData.n_annx2).log();
        LOGGER.atInfo().setMessage("ANN Get NDO X2 Time Usage: " + getTimeString(ControlData.t_anngetndo_x2)).log();
        LOGGER.atInfo().setMessage("ANN Get NDO X2 Number of Calls: " + ControlData.n_anngetndo_x2).log();
        LOGGER.atInfo().setMessage("ANN Get NDO X2 Split Time Usage: " + getTimeString(ControlData.t_anngetndo_x2_curmonndosplit)).log();
        LOGGER.atInfo().setMessage("ANN Get NDO X2 Split Number of Calls: " + ControlData.n_anngetndo_x2_curmonndosplit).log();

        Iterator<String> it = cpuTimeMap.keySet().iterator();
        while (it.hasNext()) {
            String pi = it.next();
            int cpuTime = cpuTimeMap.get(pi);
            LOGGER.atInfo().setMessage(pi + " Time Usage: " + getTimeString(cpuTime)).log();
            if (nCallsMap.containsKey(pi)) {
                int nCalls = nCallsMap.get(pi);
                LOGGER.atInfo().setMessage(pi + " Number of Calls: " + nCalls).log();
            }
        }

        ILP.writeNoteLn("Parse Time Usage", getTimeString(ControlData.t_parse), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Read Timeseries Time Usage", getTimeString(ControlData.t_readTs), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Timesereis Time Usage", getTimeString(ControlData.t_ts), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Svar Time Usage", getTimeString(ControlData.t_svar), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Dvar Time Usage", getTimeString(ControlData.t_dvar), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Constraint Time Usage", getTimeString(ControlData.t_goal), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Weight Time Usage", getTimeString(ControlData.t_wt), ILP._noteFile_timeusage);
        ILP.writeNoteLn(
                "Process Weight Surplus Slack Time Usage",
                getTimeString(ControlData.t_wtss),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn("Cbc Time Usage", getTimeString(ControlData.t_cbc), ILP._noteFile_timeusage);
        ILP.writeNoteLn("XA Time Usage", getTimeString(ControlData.t_xa), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Process Alias Time Usage", getTimeString(ControlData.t_as), ILP._noteFile_timeusage);
        ILP.writeNoteLn("Write Dss Time Usage", getTimeString(ControlData.t_writeDss), ILP._noteFile_timeusage);
        ILP.writeNoteLn("CAM Time Usage", getTimeString(ControlData.t_cam), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN Time Usage", getTimeString(ControlData.t_ann), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN Number of Calls", String.valueOf(ControlData.n_ann), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN EC Time Usage", getTimeString(ControlData.t_annec), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN EC Number of Calls", String.valueOf(ControlData.n_annec), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN Linegen Time Usage", getTimeString(ControlData.t_annlinegen), ILP._noteFile_timeusage);
        ILP.writeNoteLn(
                "ANN Linegen Number of Calls",
                String.valueOf(ControlData.n_annlinegen),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn(
                "ANN EC Match DSM2 Time Usage",
                getTimeString(ControlData.t_annec_matchdsm2),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn(
                "ANN EC Match DSM2 Number of Calls",
                String.valueOf(ControlData.n_annec_matchdsm2),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn("ANN X2 Time Usage", getTimeString(ControlData.t_annx2), ILP._noteFile_timeusage);
        ILP.writeNoteLn("ANN X2 Number of Calls", String.valueOf(ControlData.n_annx2), ILP._noteFile_timeusage);
        ILP.writeNoteLn(
                "ANN Get NDO X2 Time Usage",
                getTimeString(ControlData.t_anngetndo_x2),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn(
                "ANN Get NDO X2 Number of Calls",
                String.valueOf(ControlData.n_anngetndo_x2),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn(
                "ANN Get NDO X2 Split Time Usage",
                getTimeString(ControlData.t_anngetndo_x2_curmonndosplit),
                ILP._noteFile_timeusage
        );
        ILP.writeNoteLn(
                "ANN Get NDO X2 Split Number of Calls",
                String.valueOf(ControlData.n_anngetndo_x2_curmonndosplit),
                ILP._noteFile_timeusage
        );
        it = cpuTimeMap.keySet().iterator();
        while (it.hasNext()) {
            String pi = it.next();
            int cpuTime = cpuTimeMap.get(pi);
            ILP.writeNoteLn(pi + " Time Usage", getTimeString(cpuTime), ILP._noteFile_timeusage);
            if (nCallsMap.containsKey(pi)) {
                int nCalls = nCallsMap.get(pi);
                ILP.writeNoteLn(pi + " Number of Calls", String.valueOf(nCalls), ILP._noteFile_timeusage);
            }
        }
    }

    private static String getTimeString(int millis) {
        return millis / 60000 + "min " + Math.round((millis / 60000.0 - millis / 60000) * 60) + "sec";
    }

}
