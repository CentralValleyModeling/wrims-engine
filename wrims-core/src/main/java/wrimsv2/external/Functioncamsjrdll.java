package wrimsv2.external;

import java.util.*;

import wrimsv2.components.ControlData;

public class Functioncamsjrdll extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functioncamsjrdll(){

	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		if (stack.size()==2){
			Object param2 = stack.pop();
			Object param1 = stack.pop();
			
			int ICODE = ((Number) param2).intValue();
			int ICALC = ((Number) param1).intValue();
			
			float result = camsjrdll(ICALC, ICODE, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0, (float) 0.0);
			
			// push the result on the Stack
			stack.push(new Float(result));
		}else{
			//values in reverse order:
			Object param80 = stack.pop();
			Object param79 = stack.pop();
			Object param78 = stack.pop();
			Object param77 = stack.pop();
			Object param76 = stack.pop();
			Object param75 = stack.pop();
			Object param74 = stack.pop();
			Object param73 = stack.pop();
			Object param72 = stack.pop();
			Object param71 = stack.pop();
			Object param70 = stack.pop();
			Object param69 = stack.pop();
			Object param68 = stack.pop();
			Object param67 = stack.pop();
			Object param66 = stack.pop();
			Object param65 = stack.pop();
			Object param64 = stack.pop();
			Object param63 = stack.pop();
			Object param62 = stack.pop();
			Object param61 = stack.pop();
			Object param60 = stack.pop();
			Object param59 = stack.pop();
			Object param58 = stack.pop();
			Object param57 = stack.pop();
			Object param56 = stack.pop();
			Object param55 = stack.pop();
			Object param54 = stack.pop();
			Object param53 = stack.pop();
			Object param52 = stack.pop();
			Object param51 = stack.pop();
			Object param50 = stack.pop();
			Object param49 = stack.pop();
			Object param48 = stack.pop();
			Object param47 = stack.pop();
			Object param46 = stack.pop();
			Object param45 = stack.pop();
			Object param44 = stack.pop();
			Object param43 = stack.pop();
			Object param42 = stack.pop();
			Object param41 = stack.pop();
			Object param40 = stack.pop();
			Object param39 = stack.pop();
			Object param38 = stack.pop();
			Object param37 = stack.pop();
			Object param36 = stack.pop();
			Object param35 = stack.pop();
			Object param34 = stack.pop();
			Object param33 = stack.pop();
			Object param32 = stack.pop();
			Object param31 = stack.pop();
			Object param30 = stack.pop();
			Object param29 = stack.pop();
			Object param28 = stack.pop();
			Object param27 = stack.pop();
			Object param26 = stack.pop();
			Object param25 = stack.pop();
			Object param24 = stack.pop();
			Object param23 = stack.pop();
			Object param22 = stack.pop();
			Object param21 = stack.pop();
			Object param20 = stack.pop();
			Object param19 = stack.pop();
			Object param18 = stack.pop();
			Object param17 = stack.pop();
			Object param16 = stack.pop();
			Object param15 = stack.pop();
			Object param14 = stack.pop();
			Object param13 = stack.pop();
			Object param12 = stack.pop();
			Object param11 = stack.pop();
			Object param10 = stack.pop();
			Object param9 = stack.pop();
			Object param8 = stack.pop();
			Object param7 = stack.pop();
			Object param6 = stack.pop();
			Object param5 = stack.pop();
			Object param4 = stack.pop();
			Object param3 = stack.pop();
			Object param2 = stack.pop();
			Object param1 = stack.pop();
				
			//cast params to correct types:
			float PSJ_1_12 = ((Number) param80).floatValue();
			float PSJ_1_11 = ((Number) param79).floatValue();
			float PSJ_1_10 = ((Number) param78).floatValue();
			float PSJ_0_4 = ((Number) param77).floatValue();
			float PSJ_0_3 = ((Number) param76).floatValue();
			float PSJ_0_2 = ((Number) param75).floatValue();
			float PSJ_0_1 = ((Number) param74).floatValue();
			float PME_1_12 = ((Number) param73).floatValue();
			float PME_1_11 = ((Number) param72).floatValue();
			float PME_1_10 = ((Number) param71).floatValue();
			float PME_0_4 = ((Number) param70).floatValue();
			float PME_0_3 = ((Number) param69).floatValue();
			float PME_0_2 = ((Number) param68).floatValue();
			float PME_0_1 = ((Number) param67).floatValue();
			float PTU_1_12 = ((Number) param66).floatValue();
			float PTU_1_11 = ((Number) param65).floatValue();
			float PTU_1_10 = ((Number) param64).floatValue();
			float PTU_0_4 = ((Number) param63).floatValue();
			float PTU_0_3 = ((Number) param62).floatValue();
			float PTU_0_2 = ((Number) param61).floatValue();
			float PTU_0_1 = ((Number) param60).floatValue();
			float PST_1_12 = ((Number) param59).floatValue();
			float PST_1_11 = ((Number) param58).floatValue();
			float PST_1_10 = ((Number) param57).floatValue();
			float PST_0_4 = ((Number) param56).floatValue();
			float PST_0_3 = ((Number) param55).floatValue();
			float PST_0_2 = ((Number) param54).floatValue();
			float PST_0_1 = ((Number) param53).floatValue();
			float QSJ_1_12 = ((Number) param52).floatValue();
			float QSJ_1_11 = ((Number) param51).floatValue();
			float QSJ_1_10 = ((Number) param50).floatValue();
			float QSJ_0_4 = ((Number) param49).floatValue();
			float QSJ_0_3 = ((Number) param48).floatValue();
			float QSJ_0_2 = ((Number) param47).floatValue();
			float QSJ_0_1 = ((Number) param46).floatValue();
			float QME_1_12 = ((Number) param45).floatValue();
			float QME_1_11 = ((Number) param44).floatValue();
			float QME_1_10 = ((Number) param43).floatValue();
			float QME_0_4 = ((Number) param42).floatValue();
			float QME_0_3 = ((Number) param41).floatValue();
			float QME_0_2 = ((Number) param40).floatValue();
			float QME_0_1 = ((Number) param39).floatValue();
			float QTU_1_12 = ((Number) param38).floatValue();
			float QTU_1_11 = ((Number) param37).floatValue();
			float QTU_1_10 = ((Number) param36).floatValue();
			float QTU_0_4 = ((Number) param35).floatValue();
			float QTU_0_3 = ((Number) param34).floatValue();
			float QTU_0_2 = ((Number) param33).floatValue();
			float QTU_0_1 = ((Number) param32).floatValue();
			float QST_2_12 = ((Number) param31).floatValue();
			float QST_2_11 = ((Number) param30).floatValue();
			float QST_2_10 = ((Number) param29).floatValue();
			float QST_1_12 = ((Number) param28).floatValue();
			float QST_1_11 = ((Number) param27).floatValue();
			float QST_1_10 = ((Number) param26).floatValue();
			float QST_1_9 = ((Number) param25).floatValue();
			float QST_1_8 = ((Number) param24).floatValue();
			float QST_1_7 = ((Number) param23).floatValue();
			float QST_1_6 = ((Number) param22).floatValue();
			float QST_1_5 = ((Number) param21).floatValue();
			float QST_1_4 = ((Number) param20).floatValue();
			float QST_1_3 = ((Number) param19).floatValue();
			float QST_1_2 = ((Number) param18).floatValue();
			float QST_1_1 = ((Number) param17).floatValue();
			float QST_0_4 = ((Number) param16).floatValue();
			float QST_0_3 = ((Number) param15).floatValue();
			float QST_0_2 = ((Number) param14).floatValue();
			float QST_0_1 = ((Number) param13).floatValue();
			float IFREQ5X = ((Number) param12).floatValue();
			float IFREQ4X = ((Number) param11).floatValue();
			float IFREQ3X = ((Number) param10).floatValue();
			float IFREQ2X = ((Number) param9).floatValue();
			float IFREQ1X = ((Number) param8).floatValue();
			float IFREQODX = ((Number) param7).floatValue();
			float IMX = ((Number) param6).floatValue();
			float IYEARX = ((Number) param5).floatValue();
			float IMPOSX = ((Number) param4).floatValue();
			float IYPOSX = ((Number) param3).floatValue();
			int ICODE = ((Number) param2).intValue();
			int ICALC = ((Number) param1).intValue();

			float result = camsjrdll(ICALC, ICODE, IYPOSX, IMPOSX, IYEARX, IMX, IFREQODX, IFREQ1X, IFREQ2X, IFREQ3X, IFREQ4X, IFREQ5X, QST_0_1, QST_0_2, QST_0_3, QST_0_4, QST_1_1, QST_1_2, QST_1_3, QST_1_4, QST_1_5, QST_1_6, QST_1_7, QST_1_8, QST_1_9, QST_1_10, QST_1_11, QST_1_12, QST_2_10, QST_2_11, QST_2_12, QTU_0_1, QTU_0_2, QTU_0_3, QTU_0_4, QTU_1_10, QTU_1_11, QTU_1_12, QME_0_1, QME_0_2, QME_0_3, QME_0_4, QME_1_10, QME_1_11, QME_1_12, QSJ_0_1, QSJ_0_2, QSJ_0_3, QSJ_0_4, QSJ_1_10, QSJ_1_11, QSJ_1_12, PST_0_1, PST_0_2, PST_0_3, PST_0_4, PST_1_10, PST_1_11, PST_1_12, PTU_0_1, PTU_0_2, PTU_0_3, PTU_0_4, PTU_1_10, PTU_1_11, PTU_1_12, PME_0_1, PME_0_2, PME_0_3, PME_0_4, PME_1_10, PME_1_11, PME_1_12, PSJ_0_1, PSJ_0_2, PSJ_0_3, PSJ_0_4, PSJ_1_10, PSJ_1_11, PSJ_1_12);

			// push the result on the Stack
			stack.push(new Float(result));
		}
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_cam=ControlData.t_cam+(int) (t2-t1);
	}

	public native float camsjrdll(int ICALC, int ICODE, float IYPOSX, float IMPOSX, float IYEARX, float IMX, float IFREQODX, float IFREQ1X, float IFREQ2X, float IFREQ3X, float IFREQ4X, float IFREQ5X, float QST_0_1, float QST_0_2, float QST_0_3, float QST_0_4, float QST_1_1, float QST_1_2, float QST_1_3, float QST_1_4, float QST_1_5, float QST_1_6, float QST_1_7, float QST_1_8, float QST_1_9, float QST_1_10, float QST_1_11, float QST_1_12, float QST_2_10, float QST_2_11, float QST_2_12, float QTU_0_1, float QTU_0_2, float QTU_0_3, float QTU_0_4, float QTU_1_10, float QTU_1_11, float QTU_1_12, float QME_0_1, float QME_0_2, float QME_0_3, float QME_0_4, float QME_1_10, float QME_1_11, float QME_1_12, float QSJ_0_1, float QSJ_0_2, float QSJ_0_3, float QSJ_0_4, float QSJ_1_10, float QSJ_1_11, float QSJ_1_12, float PST_0_1, float PST_0_2, float PST_0_3, float PST_0_4, float PST_1_10, float PST_1_11, float PST_1_12, float PTU_0_1, float PTU_0_2, float PTU_0_3, float PTU_0_4, float PTU_1_10, float PTU_1_11, float PTU_1_12, float PME_0_1, float PME_0_2, float PME_0_3, float PME_0_4, float PME_1_10, float PME_1_11, float PME_1_12, float PSJ_0_1, float PSJ_0_2, float PSJ_0_3, float PSJ_0_4, float PSJ_1_10, float PSJ_1_11, float PSJ_1_12);
}
