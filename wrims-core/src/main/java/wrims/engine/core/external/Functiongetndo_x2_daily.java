package wrims.engine.core.external;

import java.util.*;

public class Functiongetndo_x2_daily extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetndo_x2_daily(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param7 = stack.pop();
		Object param6 = stack.pop();
		Object param5 = stack.pop();
		Object param4 = stack.pop();
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int currYear = ((Number) param7).intValue();
		int currMonth = ((Number) param6).intValue();
		int currDay = ((Number) param5).intValue();
		Number[] DO_prv_Arr = (Number[]) param4;
		float X2_last = ((Number) param3).floatValue();
		Number[] X2_prv_Arr = (Number[]) param2;
		float X2 = ((Number) param1).floatValue();

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
				
		float result = getndo_x2_daily(X2, X2_prv, X2_last, DO_prv, currDay, currMonth, currYear);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getndo_x2_daily(float X2, float[] X2_prv, float X2_last, float[] DO_prv, int currDay, int currMonth, int currYear);
}
