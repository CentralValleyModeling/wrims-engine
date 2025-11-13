package wrims.engine.core.components;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import wrims.engine.core.ilp.ILP;

public class TimeUsage {

	public static Map<String, Integer> cpuTimeMap = new HashMap<String, Integer>();
	public static Map<String, Integer> nCallsMap = new HashMap<String, Integer>();
	
	public static void showTimeUsage(){
		System.out.println("Parse Time Usage: "+ControlData.t_parse/60000+"min "+Math.round((ControlData.t_parse/60000.0-ControlData.t_parse/60000)*60)+"sec");
		System.out.println("Read Timeseries Time Usage: "+ControlData.t_readTs/60000+"min "+Math.round((ControlData.t_readTs/60000.0-ControlData.t_readTs/60000)*60)+"sec");
		System.out.println("Process Timesereis Time Usage: "+ControlData.t_ts/60000+"min "+Math.round((ControlData.t_ts/60000.0-ControlData.t_ts/60000)*60)+"sec");
		System.out.println("Process Svar Time Usage: "+ControlData.t_svar/60000+"min "+Math.round((ControlData.t_svar/60000.0-ControlData.t_svar/60000)*60)+"sec");
		System.out.println("Process Dvar Time Usage: "+ControlData.t_dvar/60000+"min "+Math.round((ControlData.t_dvar/60000.0-ControlData.t_dvar/60000)*60)+"sec");
		System.out.println("Process Constraint Time Usage: "+ControlData.t_goal/60000+"min "+Math.round((ControlData.t_goal/60000.0-ControlData.t_goal/60000)*60)+"sec");
		System.out.println("Process Weight Time Usage: "+ControlData.t_wt/60000+"min "+Math.round((ControlData.t_wt/60000.0-ControlData.t_wt/60000)*60)+"sec");
		System.out.println("Process Weight Surplus Slack Time Usage: "+ControlData.t_wtss/60000+"min "+Math.round((ControlData.t_wtss/60000.0-ControlData.t_wtss/60000)*60)+"sec");
		System.out.println("Cbc Time Usage: "+ControlData.t_cbc/60000+"min "+Math.round((ControlData.t_cbc/60000.0-ControlData.t_cbc/60000)*60)+"sec");
		System.out.println("XA Time Usage: "+ControlData.t_xa/60000+"min "+Math.round((ControlData.t_xa/60000.0-ControlData.t_xa/60000)*60)+"sec");
		System.out.println("Process Alias Time Usage: "+ControlData.t_as/60000+"min "+Math.round((ControlData.t_as/60000.0-ControlData.t_as/60000)*60)+"sec");
		System.out.println("Write Dss Time Usage: "+ControlData.t_writeDss/60000+"min "+Math.round((ControlData.t_writeDss/60000.0-ControlData.t_writeDss/60000)*60)+"sec");
		System.out.println("CAM Time Usage: "+ControlData.t_cam/60000+"min "+Math.round((ControlData.t_cam/60000.0-ControlData.t_cam/60000)*60)+"sec");
		System.out.println("ANN Time Usage: "+ControlData.t_ann/60000+"min "+Math.round((ControlData.t_ann/60000.0-ControlData.t_ann/60000)*60)+"sec");
		System.out.println("ANN Number of Calls: "+ControlData.n_ann);
		System.out.println("ANN EC Time Usage: "+ControlData.t_annec/60000+"min "+Math.round((ControlData.t_annec/60000.0-ControlData.t_annec/60000)*60)+"sec");
		System.out.println("ANN EC Number of Calls: "+ControlData.n_annec);
		System.out.println("ANN Linegen Time Usage: "+ControlData.t_annlinegen/60000+"min "+Math.round((ControlData.t_annlinegen/60000.0-ControlData.t_annlinegen/60000)*60)+"sec");
		System.out.println("ANN Linegen Number of Calls: "+ControlData.n_annlinegen);
		System.out.println("ANN EC Match DSM2 Time Usage: "+ControlData.t_annec_matchdsm2/60000+"min "+Math.round((ControlData.t_annec_matchdsm2/60000.0-ControlData.t_annec_matchdsm2/60000)*60)+"sec");
		System.out.println("ANN EC Match DSM2 Number of Calls: "+ControlData.n_annec_matchdsm2);
		System.out.println("ANN X2 Time Usage: "+ControlData.t_annx2/60000+"min "+Math.round((ControlData.t_annx2/60000.0-ControlData.t_annx2/60000)*60)+"sec");
		System.out.println("ANN X2 Number of Calls: "+ControlData.n_annx2);
		System.out.println("ANN Get NDO X2 Time Usage: "+ControlData.t_anngetndo_x2/60000+"min "+Math.round((ControlData.t_anngetndo_x2/60000.0-ControlData.t_anngetndo_x2/60000)*60)+"sec");
		System.out.println("ANN Get NDO X2 Number of Calls: "+ControlData.n_anngetndo_x2);
		System.out.println("ANN Get NDO X2 Split Time Usage: "+ControlData.t_anngetndo_x2_curmonndosplit/60000+"min "+Math.round((ControlData.t_anngetndo_x2_curmonndosplit/60000.0-ControlData.t_anngetndo_x2_curmonndosplit/60000)*60)+"sec");
		System.out.println("ANN Get NDO X2 Split Number of Calls: "+ControlData.n_anngetndo_x2_curmonndosplit);
		Iterator<String> it = cpuTimeMap.keySet().iterator();
		while (it.hasNext()){
			String pi=it.next();
			int cpuTime=cpuTimeMap.get(pi);
			System.out.println(pi+" Time Usage: "+cpuTime/60000+"min "+Math.round((cpuTime/60000.0-cpuTime/60000)*60)+"sec");
			if (nCallsMap.containsKey(pi)){
				int nCalls = nCallsMap.get(pi);
				System.out.println(pi+" Number of Calls: "+ nCalls);
			}
		}
			
		ILP.writeNoteLn("Parse Time Usage", ControlData.t_parse/60000+"min "+Math.round((ControlData.t_parse/60000.0-ControlData.t_parse/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Read Timeseries Time Usage", ControlData.t_readTs/60000+"min "+Math.round((ControlData.t_readTs/60000.0-ControlData.t_readTs/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Timesereis Time Usage", ControlData.t_ts/60000+"min "+Math.round((ControlData.t_ts/60000.0-ControlData.t_ts/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Svar Time Usage", ControlData.t_svar/60000+"min "+Math.round((ControlData.t_svar/60000.0-ControlData.t_svar/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Dvar Time Usage", ControlData.t_dvar/60000+"min "+Math.round((ControlData.t_dvar/60000.0-ControlData.t_dvar/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Constraint Time Usage", ControlData.t_goal/60000+"min "+Math.round((ControlData.t_goal/60000.0-ControlData.t_goal/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Weight Time Usage", ControlData.t_wt/60000+"min "+Math.round((ControlData.t_wt/60000.0-ControlData.t_wt/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Weight Surplus Slack Time Usage", ControlData.t_wtss/60000+"min "+Math.round((ControlData.t_wtss/60000.0-ControlData.t_wtss/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Cbc Time Usage", ControlData.t_cbc/60000+"min "+Math.round((ControlData.t_cbc/60000.0-ControlData.t_cbc/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("XA Time Usage", ControlData.t_xa/60000+"min "+Math.round((ControlData.t_xa/60000.0-ControlData.t_xa/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Process Alias Time Usage", ControlData.t_as/60000+"min "+Math.round((ControlData.t_as/60000.0-ControlData.t_as/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("Write Dss Time Usage", ControlData.t_writeDss/60000+"min "+Math.round((ControlData.t_writeDss/60000.0-ControlData.t_writeDss/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("CAM Time Usage", ControlData.t_cam/60000+"min "+Math.round((ControlData.t_cam/60000.0-ControlData.t_cam/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Time Usage", ControlData.t_ann/60000+"min "+Math.round((ControlData.t_ann/60000.0-ControlData.t_ann/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Number of Calls", String.valueOf(ControlData.n_ann), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Time Usage", ControlData.t_annec/60000+"min "+Math.round((ControlData.t_annec/60000.0-ControlData.t_annec/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Number of Calls", String.valueOf(ControlData.n_annec), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Linegen Time Usage", ControlData.t_annlinegen/60000+"min "+Math.round((ControlData.t_annlinegen/60000.0-ControlData.t_annlinegen/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Linegen Number of Calls", String.valueOf(ControlData.n_annlinegen), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Match DSM2 Time Usage", ControlData.t_annec_matchdsm2/60000+"min "+Math.round((ControlData.t_annec_matchdsm2/60000.0-ControlData.t_annec_matchdsm2/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN EC Match DSM2 Number of Calls", String.valueOf(ControlData.n_annec_matchdsm2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN X2 Time Usage", ControlData.t_annx2/60000+"min "+Math.round((ControlData.t_annx2/60000.0-ControlData.t_annx2/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN X2 Number of Calls", String.valueOf(ControlData.n_annx2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Time Usage", ControlData.t_anngetndo_x2/60000+"min "+Math.round((ControlData.t_anngetndo_x2/60000.0-ControlData.t_anngetndo_x2/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Number of Calls", String.valueOf(ControlData.n_anngetndo_x2), ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Split Time Usage", ControlData.t_anngetndo_x2_curmonndosplit/60000+"min "+Math.round((ControlData.t_anngetndo_x2_curmonndosplit/60000.0-ControlData.t_anngetndo_x2_curmonndosplit/60000)*60)+"sec", ILP._noteFile_timeusage);
		ILP.writeNoteLn("ANN Get NDO X2 Split Number of Calls", String.valueOf(ControlData.n_anngetndo_x2_curmonndosplit), ILP._noteFile_timeusage);
		it = cpuTimeMap.keySet().iterator();
		while (it.hasNext()){
			String pi=it.next();
			int cpuTime=cpuTimeMap.get(pi);
			ILP.writeNoteLn(pi+" Time Usage", cpuTime/60000+"min "+Math.round((cpuTime/60000.0-cpuTime/60000)*60)+"sec", ILP._noteFile_timeusage);
			if (nCallsMap.containsKey(pi)){
				int nCalls = nCallsMap.get(pi);
				ILP.writeNoteLn(pi+" Number of Calls",  String.valueOf(nCalls), ILP._noteFile_timeusage);
			}
		}
	}
	
}
