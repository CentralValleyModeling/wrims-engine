package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetpercentpumpshort extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetpercentpumpshort(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int iDummy = ((Number) param1).intValue();

		float result = getpercentpumpshort(iDummy);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getpercentpumpshort(int iDummy);
}
