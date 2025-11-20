package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetstrm_h extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetstrm_h(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int indxStrm = ((Number) param1).intValue();

		float result = getstrm_h(indxStrm);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getstrm_h(int indxStrm);
}
