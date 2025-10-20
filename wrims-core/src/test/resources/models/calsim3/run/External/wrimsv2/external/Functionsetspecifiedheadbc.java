package wrimsv2.external;

import java.util.*;

public class Functionsetspecifiedheadbc extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsetspecifiedheadbc(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		float Head = ((Number) param3).floatValue();
		int Layer = ((Number) param2).intValue();
		int Node = ((Number) param1).intValue();

		int result = setspecifiedheadbc(Node, Layer, Head);

		// push the result on the Stack
		stack.push(new Integer(result));

	}

	public native int setspecifiedheadbc(int Node, int Layer, float Head);
}
