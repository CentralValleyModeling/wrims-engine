package gov.ca.water.wrims.engine.core.external;

import java.util.*;

import gov.ca.water.wrims.engine.core.components.ControlData;

public class Functiongetndo_x2_curmonndosplit extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetndo_x2_curmonndosplit(){

	}

	public void execute(Stack stack) {

		long t1 = Calendar.getInstance().getTimeInMillis();
		
		//values in reverse order:
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
		int EndDay = ((Number) param24).intValue();
		int BeginDay = ((Number) param23).intValue();
		int currYear = ((Number) param22).intValue();
		int currMonth = ((Number) param21).intValue();
		int ave_type = ((Number) param20).intValue();
		int mon4_2 = ((Number) param19).intValue();
		int mon4_1 = ((Number) param18).intValue();
		int mon4 = ((Number) param17).intValue();
		int mon3 = ((Number) param16).intValue();
		int mon2 = ((Number) param15).intValue();
		int mon1 = ((Number) param14).intValue();
		int mon0 = ((Number) param13).intValue();
		float DO_prv4_2 = ((Number) param12).floatValue();
		float DO_prv4_1 = ((Number) param11).floatValue();
		float DO_prv3 = ((Number) param10).floatValue();
		float DO_prv2 = ((Number) param9).floatValue();
		float DO_prv1 = ((Number) param8).floatValue();
		float DO_prv0 = ((Number) param7).floatValue();
		float X2_prv4 = ((Number) param6).floatValue();
		float X2_prv3 = ((Number) param5).floatValue();
		float X2_prv2 = ((Number) param4).floatValue();
		float X2_prv1 = ((Number) param3).floatValue();
		float X2_prv0 = ((Number) param2).floatValue();
		float X2 = ((Number) param1).floatValue();

		float result = getndo_x2_curmonndosplit(X2, X2_prv0, X2_prv1, X2_prv2, X2_prv3, X2_prv4, DO_prv0, DO_prv1, DO_prv2, DO_prv3, DO_prv4_1, DO_prv4_2, mon0, mon1, mon2, mon3, mon4, mon4_1, mon4_2, ave_type, currMonth, currYear, BeginDay, EndDay);

		// push the result on the Stack
		stack.push(new Float(result));
		
		long t2 = Calendar.getInstance().getTimeInMillis();
		ControlData.t_ann=ControlData.t_ann+(int) (t2-t1);
		ControlData.t_anngetndo_x2_curmonndosplit=ControlData.t_anngetndo_x2_curmonndosplit+(int) (t2-t1);
		ControlData.n_ann=ControlData.n_ann+1;
		ControlData.n_anngetndo_x2_curmonndosplit=ControlData.n_anngetndo_x2_curmonndosplit+1;
	}

	public native float getndo_x2_curmonndosplit(float X2, float X2_prv0, float X2_prv1, float X2_prv2, float X2_prv3, float X2_prv4, float DO_prv0, float DO_prv1, float DO_prv2, float DO_prv3, float DO_prv4_1, float DO_prv4_2, int mon0, int mon1, int mon2, int mon3, int mon4, int mon4_1, int mon4_2, int ave_type, int currMonth, int currYear, int BeginDay, int EndDay);
}
