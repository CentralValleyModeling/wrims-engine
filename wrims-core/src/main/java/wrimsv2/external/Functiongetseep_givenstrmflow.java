package wrimsv2.external;

import java.util.*;

public class Functiongetseep_givenstrmflow extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functiongetseep_givenstrmflow(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int iGWHeadBeginOrEndOfTimeStep = ((Number) param3).intValue();
		float rStrmFlow = ((Number) param2).floatValue();
		int iStrmNode = ((Number) param1).intValue();

		float result = getseep_givenstrmflow(iStrmNode, rStrmFlow, iGWHeadBeginOrEndOfTimeStep);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float getseep_givenstrmflow(int iStrmNode, float rStrmFlow, int iGWHeadBeginOrEndOfTimeStep);
}
