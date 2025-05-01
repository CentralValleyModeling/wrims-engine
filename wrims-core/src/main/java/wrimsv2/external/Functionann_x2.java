package wrimsv2.external;

import java.util.*;

import wrimsv2.components.ControlData;

public class Functionann_x2 extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionann_x2(){

	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		if (stack.size()==20){
			//values in reverse order:
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
			int EndDay = ((Number) param20).intValue();
			int BeginDay = ((Number) param19).intValue();
			int currYear = ((Number) param18).intValue();
			int currMonth = ((Number) param17).intValue();
			int ave_type = ((Number) param16).intValue();
			int mon4 = ((Number) param15).intValue();
			int mon3 = ((Number) param14).intValue();
			int mon2 = ((Number) param13).intValue();
			int mon1 = ((Number) param12).intValue();
			int mon0 = ((Number) param11).intValue();
			float DO_prv4 = ((Number) param10).floatValue();
			float DO_prv3 = ((Number) param9).floatValue();
			float DO_prv2 = ((Number) param8).floatValue();
			float DO_prv1 = ((Number) param7).floatValue();
			float DO_prv0 = ((Number) param6).floatValue();
			float X2_prv4 = ((Number) param5).floatValue();
			float X2_prv3 = ((Number) param4).floatValue();
			float X2_prv2 = ((Number) param3).floatValue();
			float X2_prv1 = ((Number) param2).floatValue();
			float X2_prv0 = ((Number) param1).floatValue();
			
			float result = ann_x2(X2_prv0, X2_prv1, X2_prv2, X2_prv3, X2_prv4, DO_prv0, DO_prv1, DO_prv2, DO_prv3, DO_prv4, mon0, mon1, mon2, mon3, mon4, ave_type, currMonth, currYear, BeginDay, EndDay);

			// push the result on the Stack
			stack.push(new Float(result));
		}else{
			//values in reverse order:
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
			int EndDay = 28;
			int BeginDay = 1;
			int currYear = ((Number) param18).intValue();
			int currMonth = ((Number) param17).intValue();
			int ave_type = ((Number) param16).intValue();
			int mon4 = ((Number) param15).intValue();
			int mon3 = ((Number) param14).intValue();
			int mon2 = ((Number) param13).intValue();
			int mon1 = ((Number) param12).intValue();
			int mon0 = ((Number) param11).intValue();
			float DO_prv4 = ((Number) param10).floatValue();
			float DO_prv3 = ((Number) param9).floatValue();
			float DO_prv2 = ((Number) param8).floatValue();
			float DO_prv1 = ((Number) param7).floatValue();
			float DO_prv0 = ((Number) param6).floatValue();
			float X2_prv4 = ((Number) param5).floatValue();
			float X2_prv3 = ((Number) param4).floatValue();
			float X2_prv2 = ((Number) param3).floatValue();
			float X2_prv1 = ((Number) param2).floatValue();
			float X2_prv0 = ((Number) param1).floatValue();
			
			float result = ann_x2(X2_prv0, X2_prv1, X2_prv2, X2_prv3, X2_prv4, DO_prv0, DO_prv1, DO_prv2, DO_prv3, DO_prv4, mon0, mon1, mon2, mon3, mon4, ave_type, currMonth, currYear, BeginDay, EndDay);

			// push the result on the Stack
			stack.push(new Float(result));

		}
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_ann=ControlData.t_ann+(int) (t2-t1);
		ControlData.t_annx2=ControlData.t_annx2+(int) (t2-t1);
		ControlData.n_ann=ControlData.n_ann+1;
		ControlData.n_annx2=ControlData.n_annx2+1;
	}

	public native float ann_x2(float X2_prv0, float X2_prv1, float X2_prv2, float X2_prv3, float X2_prv4, float DO_prv0, float DO_prv1, float DO_prv2, float DO_prv3, float DO_prv4, int mon0, int mon1, int mon2, int mon3, int mon4, int ave_type, int currMonth, int currYear, int BeginDay, int EndDay);
}
