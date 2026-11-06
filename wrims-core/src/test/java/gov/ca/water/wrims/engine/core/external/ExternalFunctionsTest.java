package gov.ca.water.wrims.engine.core.external;

import org.junit.jupiter.api.Test;

import java.util.Stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExternalFunctionsTest {

	@Test
	void returnsEngineExternalFunctionsUnchanged() throws ReflectiveOperationException {
		assertEquals(EngineFunction.class, ExternalFunctions.newInstance(EngineFunction.class).getClass());
	}

	@Test
	void adaptsAndExecutesLegacyFunctions() throws ReflectiveOperationException {
		ExternalFunction function = ExternalFunctions.newInstance(LegacyFunction.class);
		Stack<Object> stack = new Stack<>();

		function.execute(stack);

		assertEquals("legacy", stack.pop());
	}

	@Test
	void propagatesRuntimeExceptionsFromLegacyFunctions() throws ReflectiveOperationException {
		ExternalFunction function = ExternalFunctions.newInstance(ThrowingLegacyFunction.class);

		assertThrows(IllegalArgumentException.class, () -> function.execute(new Stack<>()));
	}

	public static class EngineFunction extends ExternalFunction {
		@Override
		public void execute(Stack stack) {
		}
	}

	public static class LegacyFunction {
		public void execute(Stack stack) {
			stack.push("legacy");
		}
	}

	public static class ThrowingLegacyFunction {
		public void execute(Stack stack) {
			throw new IllegalArgumentException("legacy failure");
		}
	}
}
