package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import gov.ca.water.wrims.engine.core.components.ControlData;

public class Functionannlinegen extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionannlinegen(){

	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		//values in reverse order:
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
		int ForceOption = ((Number) param52).intValue();
		int currYear = ((Number) param51).intValue();
		int currMonth = ((Number) param50).intValue();
		int ave_type = ((Number) param49).intValue();
		int variable = ((Number) param48).intValue();
		int location = ((Number) param47).intValue();
		float linear2 = ((Number) param46).floatValue();
		float linear1 = ((Number) param45).floatValue();
		float ECTARGET = ((Number) param44).floatValue();
		int mon4 = ((Number) param43).intValue();
		int mon3 = ((Number) param42).intValue();
		int mon2 = ((Number) param41).intValue();
		int mon1 = ((Number) param40).intValue();
		int mon0 = ((Number) param39).intValue();
		float VernEC_fut = ((Number) param38).floatValue();
		float VernEC_prv3 = ((Number) param37).floatValue();
		float VernEC_prv2 = ((Number) param36).floatValue();
		float VernEC_prv1 = ((Number) param35).floatValue();
		float VernEC_prv0 = ((Number) param34).floatValue();
		float Qexp_oth_fut = ((Number) param33).floatValue();
		float Qexp_oth_prv3 = ((Number) param32).floatValue();
		float Qexp_oth_prv2 = ((Number) param31).floatValue();
		float Qexp_oth_prv1 = ((Number) param30).floatValue();
		float Qexp_oth_prv0 = ((Number) param29).floatValue();
		float Qsac_oth_fut = ((Number) param28).floatValue();
		float Qsac_oth_prv3 = ((Number) param27).floatValue();
		float Qsac_oth_prv2 = ((Number) param26).floatValue();
		float Qsac_oth_prv1 = ((Number) param25).floatValue();
		float Qsac_oth_prv0 = ((Number) param24).floatValue();
		float DICU_fut = ((Number) param23).floatValue();
		float DICU_prv3 = ((Number) param22).floatValue();
		float DICU_prv2 = ((Number) param21).floatValue();
		float DICU_prv1 = ((Number) param20).floatValue();
		float DICU_prv0 = ((Number) param19).floatValue();
		float DXC_fut = ((Number) param18).floatValue();
		float DXC_prv3 = ((Number) param17).floatValue();
		float DXC_prv2 = ((Number) param16).floatValue();
		float DXC_prv1 = ((Number) param15).floatValue();
		float DXC_prv0 = ((Number) param14).floatValue();
		float Qsjr_fut = ((Number) param13).floatValue();
		float Qsjr_prv3 = ((Number) param12).floatValue();
		float Qsjr_prv2 = ((Number) param11).floatValue();
		float Qsjr_prv1 = ((Number) param10).floatValue();
		float Qsjr_prv0 = ((Number) param9).floatValue();
		float Qexp_prv3 = ((Number) param8).floatValue();
		float Qexp_prv2 = ((Number) param7).floatValue();
		float Qexp_prv1 = ((Number) param6).floatValue();
		float Qexp_prv0 = ((Number) param5).floatValue();
		float Qsac_prv3 = ((Number) param4).floatValue();
		float Qsac_prv2 = ((Number) param3).floatValue();
		float Qsac_prv1 = ((Number) param2).floatValue();
		float Qsac_prv0 = ((Number) param1).floatValue();

		float result = annlinegen(Qsac_prv0, Qsac_prv1, Qsac_prv2, Qsac_prv3, Qexp_prv0, Qexp_prv1, Qexp_prv2, Qexp_prv3, Qsjr_prv0, Qsjr_prv1, Qsjr_prv2, Qsjr_prv3, Qsjr_fut, DXC_prv0, DXC_prv1, DXC_prv2, DXC_prv3, DXC_fut, DICU_prv0, DICU_prv1, DICU_prv2, DICU_prv3, DICU_fut, Qsac_oth_prv0, Qsac_oth_prv1, Qsac_oth_prv2, Qsac_oth_prv3, Qsac_oth_fut, Qexp_oth_prv0, Qexp_oth_prv1, Qexp_oth_prv2, Qexp_oth_prv3, Qexp_oth_fut, VernEC_prv0, VernEC_prv1, VernEC_prv2, VernEC_prv3, VernEC_fut, mon0, mon1, mon2, mon3, mon4, ECTARGET, linear1, linear2, location, variable, ave_type, currMonth, currYear, ForceOption);

		// push the result on the Stack
		stack.push(new Float(result));
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_ann=ControlData.t_ann+(int) (t2-t1);
		ControlData.t_annlinegen=ControlData.t_annlinegen+(int) (t2-t1);
		ControlData.n_ann=ControlData.n_ann+1;
		ControlData.n_annlinegen=ControlData.n_annlinegen+1;
	}

	public native float annlinegen(float Qsac_prv0, float Qsac_prv1, float Qsac_prv2, float Qsac_prv3, float Qexp_prv0, float Qexp_prv1, float Qexp_prv2, float Qexp_prv3, float Qsjr_prv0, float Qsjr_prv1, float Qsjr_prv2, float Qsjr_prv3, float Qsjr_fut, float DXC_prv0, float DXC_prv1, float DXC_prv2, float DXC_prv3, float DXC_fut, float DICU_prv0, float DICU_prv1, float DICU_prv2, float DICU_prv3, float DICU_fut, float Qsac_oth_prv0, float Qsac_oth_prv1, float Qsac_oth_prv2, float Qsac_oth_prv3, float Qsac_oth_fut, float Qexp_oth_prv0, float Qexp_oth_prv1, float Qexp_oth_prv2, float Qexp_oth_prv3, float Qexp_oth_fut, float VernEC_prv0, float VernEC_prv1, float VernEC_prv2, float VernEC_prv3, float VernEC_fut, int mon0, int mon1, int mon2, int mon3, int mon4, float ECTARGET, float linear1, float linear2, int location, int variable, int ave_type, int currMonth, int currYear, int ForceOption);
}
