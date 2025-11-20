package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetseep extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetseep(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int indxStrm = ((Number) param1).intValue();

		float result = getseep(indxStrm);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getseep(int indxStrm);
}
