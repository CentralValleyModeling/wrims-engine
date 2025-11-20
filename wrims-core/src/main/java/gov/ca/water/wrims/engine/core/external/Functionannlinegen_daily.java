package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functionannlinegen_daily extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionannlinegen_daily(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
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
		int ForceOption = ((Number) param23).intValue();
		int currYear = ((Number) param22).intValue();
		int currMonth = ((Number) param21).intValue();
		int currDay = ((Number) param20).intValue();
		int variable = ((Number) param19).intValue();
		int location = ((Number) param18).intValue();
		float linear2 = ((Number) param17).floatValue();
		float linear1 = ((Number) param16).floatValue();
		float ECTARGET = ((Number) param15).floatValue();
		float VernEC_est = ((Number) param14).floatValue();
		Number[] VernEC_prv_Arr = (Number[])param13;
		float Qexp_oth_est = ((Number) param12).floatValue();
		Number[] Qexp_oth_prv_Arr = (Number[])param11;
		float Qsac_oth_est = ((Number) param10).floatValue();
		Number[] Qsac_oth_prv_Arr = (Number[])param9;
		float DICU_est = ((Number) param8).floatValue();
		Number[] DICU_prv_Arr = (Number[])param7;
		float DXC_est = ((Number) param6).floatValue();
		Number[] DXC_prv_Arr = (Number[])param5;
		float Qsjr_est = ((Number) param4).floatValue();
		Number[] Qsjr_prv_Arr = (Number[])param3;
		Number[] Qexp_prv_Arr = (Number[])param2;
		Number[] Qsac_prv_Arr = (Number[])param1;

		int size1=Qsac_prv_Arr.length;
		float[] Qsac_prv=new float[size1];
		for (int i=0; i<size1; i++){
			Qsac_prv[i]=Qsac_prv_Arr[i].floatValue();
		}
		
		int size2=Qexp_prv_Arr.length;
		float[] Qexp_prv=new float[size2];
		for (int i=0; i<size2; i++){
			Qexp_prv[i]=Qexp_prv_Arr[i].floatValue();
		}
		
		int size3=Qsjr_prv_Arr.length;
		float[] Qsjr_prv=new float[size3];
		for (int i=0; i<size3; i++){
			Qsjr_prv[i]=Qsjr_prv_Arr[i].floatValue();
		}
		
		int size4=DXC_prv_Arr.length;
		float[] DXC_prv=new float[size4];
		for (int i=0; i<size4; i++){
			DXC_prv[i]=DXC_prv_Arr[i].floatValue();
		}
		
		int size5=DICU_prv_Arr.length;
		float[] DICU_prv=new float[size5];
		for (int i=0; i<size5; i++){
			DICU_prv[i]=DICU_prv_Arr[i].floatValue();
		}
		
		int size6=Qsac_oth_prv_Arr.length;
		float[] Qsac_oth_prv=new float[size6];
		for (int i=0; i<size6; i++){
			Qsac_oth_prv[i]=Qsac_oth_prv_Arr[i].floatValue();
		}
		
		int size7=Qexp_oth_prv_Arr.length;
		float[] Qexp_oth_prv=new float[size7];
		for (int i=0; i<size7; i++){
			Qexp_oth_prv[i]=Qexp_oth_prv_Arr[i].floatValue();
		}
		
		int size8=VernEC_prv_Arr.length;
		float[] VernEC_prv=new float[size8];
		for (int i=0; i<size8; i++){
			VernEC_prv[i]=VernEC_prv_Arr[i].floatValue();
		}
		
		float result = annlinegen_daily(Qsac_prv, Qexp_prv,			     
				 Qsjr_prv,Qsjr_est,
			     DXC_prv, DXC_est,
			     DICU_prv,DICU_est,      
			     Qsac_oth_prv,Qsac_oth_est, 
			     Qexp_oth_prv,Qexp_oth_est, 
			     VernEC_prv,VernEC_est,
			     ECTARGET,linear1,linear2,location,variable,
			     currDay,currMonth,currYear,ForceOption);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float annlinegen_daily(float[] Qsac_prv, float[] Qexp_prv,			     
			 float[] Qsjr_prv, float Qsjr_est,
		     float[] DXC_prv, float DXC_est,
		     float[] DICU_prv, float DICU_est,      
		     float[] Qsac_oth_prv, float Qsac_oth_est, 
		     float[] Qexp_oth_prv, float Qexp_oth_est, 
		     float[] VernEC_prv, float VernEC_est,
		     float ECTARGET, float linear1, float linear2, int location, int variable,
		     int currDay, int currMonth, int currYear, int ForceOption);
}
