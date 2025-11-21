package wrimsv2.external;

import java.util.*;

public class Functionsetgeneralheadbc extends ExternalFunction{
	private final boolean DEBUG = false;


	public Functionsetgeneralheadbc(){

	}

	public void execute(Stack stack) {

		//values in reverse order:
		Object param4 = stack.pop();
		Object param3 = stack.pop();
		Object param2 = stack.pop();
		Object param1 = stack.pop();

		//cast params to correct types:
		float GHead = ((Number) param4).floatValue();
		float Conductance = ((Number) param3).floatValue();
		int Layer = ((Number) param2).intValue();
		int Node = ((Number) param1).intValue();

		int result = setgeneralheadbc(Node, Layer, Conductance, GHead);

		// push the result on the Stack
		stack.push(new Integer(result));

	}

	public native int setgeneralheadbc(int Node, int Layer, float Conductance, float GHead);
}
