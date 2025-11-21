package wrimsv2.external;

import java.util.*;

public class Functionadvancegwstate extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionadvancegwstate(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param1 = stack.pop();

		//cast params to correct types:
		int iDummy = ((Number) param1).intValue();

		float result = advancegwstate(iDummy);

		// push the result on the Stack
		stack.push(new Float(result));
	}

	public native float advancegwstate(int iDummy);
}
