package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetpumpshort extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetpumpshort(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int iDummy = ((Number) param1).intValue();

		float result = getpumpshort(iDummy);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getpumpshort(int iDummy);
}
