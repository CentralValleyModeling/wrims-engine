package wrims.engine.core.external;

import java.util.*;

import wrims.engine.core.components.ControlData;

public class Functionannec_matchdsm2_a extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionannec_matchdsm2_a(){

	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();

		//values in reverse order:
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
		int currYear = ((Number) param13).intValue();
		int currMonth = ((Number) param12).intValue();
		int ave_type = ((Number) param11).intValue();
		int location = ((Number) param10).intValue();
		Number[] mon_Arr = (Number[])param9;
		int size_mon=mon_Arr.length;
		float[] mon=new float[size_mon];
		for (int i=0; i<size_mon; i++){
			mon[i]=mon_Arr[i].floatValue();
		}
		Number[] VernEC_prv_Arr = (Number[])param8;
		int size_VernEC_prv=VernEC_prv_Arr.length;
		float[] VernEC_prv=new float[size_VernEC_prv];
		for (int i=0; i<size_VernEC_prv; i++){
			VernEC_prv[i]=VernEC_prv_Arr[i].floatValue();
		}
		Number[] Qexp_oth_prv_Arr = (Number[])param7;
		int size_Qexp_oth_prv=Qexp_oth_prv_Arr.length;
		float[] Qexp_oth_prv=new float[size_Qexp_oth_prv];
		for (int i=0; i<size_Qexp_oth_prv; i++){
			Qexp_oth_prv[i]=Qexp_oth_prv_Arr[i].floatValue();
		}
		Number[] Qsac_oth_prv_Arr = (Number[])param6;
		int size_Qsac_oth_prv=Qsac_oth_prv_Arr.length;
		float[] Qsac_oth_prv=new float[size_Qsac_oth_prv];
		for (int i=0; i<size_Qsac_oth_prv; i++){
			Qsac_oth_prv[i]=Qsac_oth_prv_Arr[i].floatValue();
		}
		Number[] DICU_prv_Arr = (Number[])param5;
		int size_DICU_prv=DICU_prv_Arr.length;
		float[] DICU_prv=new float[size_DICU_prv];
		for (int i=0; i<size_DICU_prv; i++){
			DICU_prv[i]=DICU_prv_Arr[i].floatValue();
		}
		Number[] DXC_prv_Arr = (Number[])param4;
		int size_DXC_prv=DXC_prv_Arr.length;
		float[] DXC_prv=new float[size_DXC_prv];
		for (int i=0; i<size_DXC_prv; i++){
			DXC_prv[i]=DXC_prv_Arr[i].floatValue();
		}
		Number[] Qsjr_prv_Arr = (Number[])param3;
		int size_Qsjr_prv=Qsjr_prv_Arr.length;
		float[] Qsjr_prv=new float[size_Qsjr_prv];
		for (int i=0; i<size_Qsjr_prv; i++){
			Qsjr_prv[i]=Qsjr_prv_Arr[i].floatValue();
		}
		Number[] Qexp_prv_Arr = (Number[])param2;
		int size_Qexp_prv=Qexp_prv_Arr.length;
		float[] Qexp_prv=new float[size_Qexp_prv];
		for (int i=0; i<size_Qexp_prv; i++){
			Qexp_prv[i]=Qexp_prv_Arr[i].floatValue();
		}
		Number[] Qsac_prv_Arr = (Number[])param1;
		int size_Qsac_prv=Qsac_prv_Arr.length;
		float[] Qsac_prv=new float[size_Qsac_prv];
		for (int i=0; i<size_Qsac_prv; i++){
			Qsac_prv[i]=Qsac_prv_Arr[i].floatValue();
		}

		float result = annec_matchdsm2_a(Qsac_prv, Qexp_prv, Qsjr_prv, DXC_prv, DICU_prv, Qsac_oth_prv, Qexp_oth_prv, VernEC_prv, mon, location, ave_type, currMonth, currYear);

		// push the result on the Stack
		stack.push(new Float(result));

		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_ann=ControlData.t_ann+(int) (t2-t1);
		ControlData.t_annec_matchdsm2=ControlData.t_annec_matchdsm2+(int) (t2-t1);
		ControlData.n_ann=ControlData.n_ann+1;
		ControlData.n_annec_matchdsm2=ControlData.n_annec_matchdsm2+1;

	}

	public native float annec_matchdsm2_a(float[] Qsac_prv, float[] Qexp_prv, float[] Qsjr_prv, float[] DXC_prv, float[] DICU_prv, float[] Qsac_oth_prv, float[] Qexp_oth_prv, float[] VernEC_prv, float[] mon, int location, int ave_type, int currMonth, int currYear);
}
