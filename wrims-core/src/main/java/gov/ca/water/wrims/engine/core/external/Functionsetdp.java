package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functionsetdp extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsetdp(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int WBAIndex = ((Number) param2).intValue();
		float Value = ((Number) param1).floatValue();

		float result = setdp(Value, WBAIndex);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float setdp(float Value, int WBAIndex);
}
