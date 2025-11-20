package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetgw_h extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetgw_h(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int Layer = ((Number) param2).intValue();
		int indxGW = ((Number) param1).intValue();

		float result = getgw_h(indxGW, Layer);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getgw_h(int indxGW, int Layer);
}
