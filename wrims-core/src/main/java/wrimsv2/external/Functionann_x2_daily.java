package wrimsv2.external;

import java.util.*;

public class Functionann_x2_daily extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionann_x2_daily(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param5 = stack.pop();
		Object param4 = stack.pop();
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int currYear = ((Number) param5).intValue();
		int currMonth = ((Number) param4).intValue();
		int currDay = ((Number) param3).intValue();
		Number[] DO_prv_Arr = (Number[])param2;
		Number[] X2_prv_Arr = (Number[])param1;

		int size1=X2_prv_Arr.length;
		float[] X2_prv=new float[size1];
		for (int i=0; i<size1; i++){
			X2_prv[i]=X2_prv_Arr[i].floatValue();
		}
		
		int size2=DO_prv_Arr.length;
		float[] DO_prv=new float[size2];
		for (int i=0; i<size2; i++){
			DO_prv[i]=DO_prv_Arr[i].floatValue();
		}
				
		float result = ann_x2_daily(X2_prv, DO_prv, currDay, currMonth, currYear);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float ann_x2_daily(float[] X2_prv, float[] DO_prv, int currDay, int currMonth, int currYear);
}
