package gov.ca.water.wrims.engine.core.external;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

/**
 * Factory for user-supplied external functions ({@code wrimsv2.external.Function*}).
 * <p>
 * Studies built for WRIMS 2 commonly ship their own copy of
 * {@code wrimsv2.external.ExternalFunction} in {@code Run/External}. When that folder is
 * placed ahead of the engine on the classpath, the study's base class shadows the engine's
 * compatibility facade and does not extend {@link ExternalFunction}; a direct cast would then
 * fail with a {@link ClassCastException}. Such instances are wrapped in an adapter that invokes
 * {@code execute(Stack)} reflectively so the engine can treat them uniformly.
 */
public final class ExternalFunctions {

	private ExternalFunctions() {
	}

	public static ExternalFunction newInstance(Class<?> functionClass)
			throws ReflectiveOperationException {
		Object instance = functionClass.getDeclaredConstructor().newInstance();
		if (instance instanceof ExternalFunction) {
			return (ExternalFunction) instance;
		}
		return new LegacyAdapter(instance);
	}

	private static final class LegacyAdapter extends ExternalFunction {
		private final Object target;
		private final Method execute;

		LegacyAdapter(Object target) throws NoSuchMethodException {
			this.target = target;
			this.execute = target.getClass().getMethod("execute", Stack.class);
		}

		@Override
		public void execute(Stack stack) {
			try {
				execute.invoke(target, stack);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException) {
					throw (RuntimeException) cause;
				}
				if (cause instanceof Error) {
					throw (Error) cause;
				}
				throw new IllegalStateException(cause);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}
