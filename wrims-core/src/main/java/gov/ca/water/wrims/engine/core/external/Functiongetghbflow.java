package gov.ca.water.wrims.engine.core.external;

import java.util.*;

public class Functiongetghbflow extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetghbflow(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int iLayer = ((Number) param2).intValue();
		int iNode = ((Number) param1).intValue();

		float result = getghbflow(iNode, iLayer);

		// push the result on the Stack
		stack.push(new Float(result));

	}

	public native float getghbflow(int iNode, int iLayer);
}
