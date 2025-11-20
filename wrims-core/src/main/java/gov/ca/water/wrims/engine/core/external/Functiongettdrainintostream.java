package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongettdrainintostream extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongettdrainintostream(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int istrmnode = ((Number) param1).intValue();

		float result = gettdrainintostream(istrmnode);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float gettdrainintostream(int istrmnode);
}
