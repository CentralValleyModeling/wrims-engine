package wrims.engine.core.external;

import java.util.*;

public class Functionsetstrmflow extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsetstrmflow(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int StrmNodeIndex = ((Number) param2).intValue();
		float Value = ((Number) param1).floatValue();

		float result = setstrmflow(Value, StrmNodeIndex);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float setstrmflow(float Value, int StrmNodeIndex);
}
