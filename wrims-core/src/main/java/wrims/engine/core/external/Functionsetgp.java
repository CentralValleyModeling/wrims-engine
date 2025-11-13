package wrims.engine.core.external;

import java.util.*;

public class Functionsetgp extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsetgp(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int WBAIndex = ((Number) param2).intValue();
		float Value = ((Number) param1).floatValue();

		float result = setgp(Value, WBAIndex);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float setgp(float Value, int WBAIndex);
}
