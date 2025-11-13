package wrims.engine.core.external;

import java.util.*;

public class Functionsimgw extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsimgw(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		int Month = ((Number) param2).intValue();
		int WaterYear = ((Number) param1).intValue();

		float result = simgw(WaterYear, Month);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float simgw(int WaterYear, int Month);
}
