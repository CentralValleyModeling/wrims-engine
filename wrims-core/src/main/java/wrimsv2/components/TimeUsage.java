package wrimsv2.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wrimsv2.ilp.ILP;

public class TimeUsage {
    private static final Logger logger = LoggerFactory.getLogger(TimeUsage.class);
	public static Map<String, Integer> cpuTimeMap = new HashMap<String, Integer>();
	public static Map<String, Integer> nCallsMap = new HashMap<String, Integer>();

    private static int getFullMinutes(int milliseconds) {
        return milliseconds / 60000;
    }

    private static double getLeftoverSeconds(int milliseconds) {
        double seconds = milliseconds / 1000.0;
        return seconds % 60;
    }

    private static String getTimeReport(int milliseconds) {
        return getFullMinutes(milliseconds) + "min " + getLeftoverSeconds(milliseconds) + "sec";
    }

	public static void showTimeUsage(){
        logger.info("Parse Time Usage: {}", getTimeReport(ControlData.t_parse));
        logger.info("Read Timeseries Time Usage: {}", getTimeReport(ControlData.t_readTs));
        logger.info("Process Timesereis Time Usage: {}", getTimeReport(ControlData.t_ts));
        logger.info("Process Svar Time Usage: {}", getTimeReport(ControlData.t_svar));
        logger.info("Process Dvar Time Usage: {}", getTimeReport(ControlData.t_dvar));
        logger.info("Process Constraint Time Usage: {}", getTimeReport(ControlData.t_goal));
        logger.info("Process Weight Time Usage: {}", getTimeReport(ControlData.t_wt));
        logger.info("Process Weight Surplus Slack Time Usage: {}", getTimeReport(ControlData.t_wtss));
        logger.info("Cbc Time Usage: {}", getTimeReport(ControlData.t_cbc));
        logger.info("XA Time Usage: {}", getTimeReport(ControlData.t_xa));
        logger.info("Process Alias Time Usage: {}", getTimeReport(ControlData.t_as));
        logger.info("Write Dss Time Usage: {}", getTimeReport(ControlData.t_writeDss));
        logger.info("CAM Time Usage: {}", getTimeReport(ControlData.t_cam));
        logger.info("ANN Time Usage: {}", getTimeReport(ControlData.t_ann));
        logger.info("ANN Number of Calls: {}", ControlData.n_ann);
        logger.info("ANN EC Time Usage: {}", getTimeReport(ControlData.t_annec));
        logger.info("ANN EC Number of Calls: {}", ControlData.n_annec);
        logger.info("ANN Linegen Time Usage: {}", getTimeReport(ControlData.t_annlinegen));
        logger.info("ANN Linegen Number of Calls: {}", ControlData.n_annlinegen);
        logger.info("ANN EC Match DSM2 Time Usage: {}", getTimeReport(ControlData.t_annec_matchdsm2));
        logger.info("ANN EC Match DSM2 Number of Calls: {}", ControlData.n_annec_matchdsm2);
        logger.info("ANN X2 Time Usage: {}", getTimeReport(ControlData.t_annx2));
        logger.info("ANN X2 Number of Calls: {}", ControlData.n_annx2);
        logger.info("ANN Get NDO X2 Time Usage: {}", getTimeReport(ControlData.t_anngetndo_x2));
        logger.info("ANN Get NDO X2 Number of Calls: {}", ControlData.n_anngetndo_x2);
        logger.info("ANN Get NDO X2 Split Time Usage: {}", getTimeReport(ControlData.t_anngetndo_x2_curmonndosplit));
        logger.info("ANN Get NDO X2 Split Number of Calls: {}", ControlData.n_anngetndo_x2_curmonndosplit);
		Iterator<String> it = cpuTimeMap.keySet().iterator();
		while (it.hasNext()){
			String pi=it.next();
			int cpuTime=cpuTimeMap.get(pi);
			logger.info(pi+" Time Usage: "+getTimeReport(cpuTime));
			if (nCallsMap.containsKey(pi)){
				int nCalls = nCallsMap.get(pi);
				logger.info(pi+" Number of Calls: "+ nCalls);
			}
		}
			
		ILP.writeNoteLn("Parse Time Usage", getTimeReport( ControlData.t_parse), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Read Timeseries Time Usage", getTimeReport( ControlData.t_readTs), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Timesereis Time Usage", getTimeReport( ControlData.t_ts), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Svar Time Usage", getTimeReport( ControlData.t_svar), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Dvar Time Usage", getTimeReport( ControlData.t_dvar), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Constraint Time Usage", getTimeReport( ControlData.t_goal), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Weight Time Usage", getTimeReport( ControlData.t_wt), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Weight Surplus Slack Time Usage", getTimeReport( ControlData.t_wtss), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Cbc Time Usage", getTimeReport( ControlData.t_cbc), ILP._noteFile_timeusage);
		ILP.writeNoteLn("XA Time Usage", getTimeReport( ControlData.t_xa), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Alias Time Usage", getTimeReport( ControlData.t_as), ILP._noteFile_timeusage);
		ILP.writeNoteLn("Write Dss Time Usage", getTimeReport( ControlData.t_writeDss), ILP._noteFile_timeusage);
		ILP.writeNoteLn("CAM Time Usage", getTimeReport( ControlData.t_cam), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Time Usage", getTimeReport( ControlData.t_ann), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Number of Calls", String.valueOf(ControlData.n_ann), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Time Usage", getTimeReport( ControlData.t_annec), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Number of Calls", String.valueOf(ControlData.n_annec), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Linegen Time Usage", getTimeReport( ControlData.t_annlinegen), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Linegen Number of Calls", String.valueOf(ControlData.n_annlinegen), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Match DSM2 Time Usage", getTimeReport( ControlData.t_annec_matchdsm2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Match DSM2 Number of Calls", String.valueOf(ControlData.n_annec_matchdsm2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN X2 Time Usage", getTimeReport(ControlData.t_annx2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN X2 Number of Calls", String.valueOf(ControlData.n_annx2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Time Usage", getTimeReport(ControlData.t_anngetndo_x2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Number of Calls", String.valueOf(ControlData.n_anngetndo_x2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Split Time Usage", getTimeReport(ControlData.t_anngetndo_x2_curmonndosplit), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Split Number of Calls", String.valueOf(ControlData.n_anngetndo_x2_curmonndosplit), ILP._noteFile_timeusage);
		it = cpuTimeMap.keySet().iterator();
		while (it.hasNext()){
			String pi=it.next();
			int cpuTime=cpuTimeMap.get(pi);
			ILP.writeNoteLn(pi+" Time Usage", getTimeReport(cpuTime), ILP._noteFile_timeusage);
			if (nCallsMap.containsKey(pi)){
				int nCalls = nCallsMap.get(pi);
				ILP.writeNoteLn(pi+" Number of Calls",  String.valueOf(nCalls), ILP._noteFile_timeusage);
			}
		}
	}
	
}
